package com.darkness.jujutsukaisenzone.player;

import com.darkness.jujutsukaisenzone.compat.EpicFightCompat;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

/**
 * 咒术等级对战斗属性的加成（仅服务端应用）。
 */
public final class CursedGradeBonuses
{
    private static final UUID ATTACK_UUID = UUID.fromString("6b2c9a01-0001-4000-8000-000000000001");
    private static final UUID SPEED_UUID = UUID.fromString("6b2c9a01-0002-4000-8000-000000000002");
    private static final UUID HEALTH_UUID = UUID.fromString("6b2c9a01-0003-4000-8000-000000000003");
    private static final UUID ARMOR_UUID = UUID.fromString("6b2c9a01-0004-4000-8000-000000000004");
    private static final UUID HEAVENLY_ATTACK_UUID = UUID.fromString("6b2c9a01-1001-4000-8000-000000000001");
    private static final UUID HEAVENLY_SPEED_UUID = UUID.fromString("6b2c9a01-1002-4000-8000-000000000002");
    private static final UUID HEAVENLY_BURST_SPEED_UUID = UUID.fromString("6b2c9a01-1005-4000-8000-000000000005");
    private static final UUID HEAVENLY_HEALTH_UUID = UUID.fromString("6b2c9a01-1003-4000-8000-000000000003");
    private static final UUID HEAVENLY_ARMOR_UUID = UUID.fromString("6b2c9a01-1004-4000-8000-000000000004");
    private static final UUID HEAVENLY_KNOCKBACK_RESISTANCE_UUID = UUID.fromString("6b2c9a01-1006-4000-8000-000000000006");
    private static final String TAG_HEAVENLY_WAS_SPRINTING = "jujutsukaisenzone_heavenly_was_sprinting";
    private static final String TAG_HEAVENLY_BURST_REMAINING = "jujutsukaisenzone_heavenly_burst_remaining";
    private static final String TAG_HEAVENLY_BURST_ORIGIN_X = "jujutsukaisenzone_heavenly_burst_origin_x";
    private static final String TAG_HEAVENLY_BURST_ORIGIN_Y = "jujutsukaisenzone_heavenly_burst_origin_y";
    private static final String TAG_HEAVENLY_BURST_ORIGIN_Z = "jujutsukaisenzone_heavenly_burst_origin_z";

    /** 咒力总量额外上限：四级~特级 */
    private static final int[] MAX_ENERGY_BONUS = {0, 200, 500, 1000, 1999, 2999};

    /** 原版基础上限，属性修饰符在此基础上叠加 */
    public static final double BASE_MAX_HEALTH = 20.0;
    /** 生命上限绝对 cap（含基础上限与所有加成） */
    public static final double ABSOLUTE_MAX_HEALTH = 500.0;

    /**
     * 四级~特级累计生命加成（index = {@link PlayerCursedGradeData#getRank}）。
     * 四级 +20 → 三级累计 +50 → 二级 +90 → 一级 +170 → 特级 +270。
     */
    private static final double[] HEALTH_BONUS_BY_RANK = {0.0, 20.0, 50.0, 90.0, 170.0, 270.0};

    /** 攻击：每级 +1.1（中幅，随 rank 累加） */
    private static final double ATTACK_PER_RANK = 1.1;
    /** 仅疾跑时生效：每级移动速度 ADDITION（略高于旧常驻值） */
    private static final double SPRINT_SPEED_PER_RANK = 0.026;
    private static final double HEAVENLY_ATTACK_BONUS = 14.0;
    private static final double HEAVENLY_SPRINT_SPEED_BONUS = 0.24;
    private static final double HEAVENLY_SPRINT_BURST_BONUS = 0.85;
    private static final int HEAVENLY_SPRINT_BURST_TICKS = 20;
    /** 天与咒缚：基础上限 20 + 300 = 320 */
    private static final double HEAVENLY_HEALTH_BONUS = 300.0;
    private static final double HEAVENLY_ARMOR_BONUS = 30.0;

    private CursedGradeBonuses()
    {
    }

    public static int getMaxEnergyBonus(int rank)
    {
        if (rank < 0 || rank >= MAX_ENERGY_BONUS.length)
        {
            return 0;
        }
        return MAX_ENERGY_BONUS[rank];
    }

    /** 咒术等级对应的累计生命加成（不含基础上限 20） */
    public static double getHealthBonusForRank(int rank)
    {
        if (rank < 0 || rank >= HEALTH_BONUS_BY_RANK.length)
        {
            return 0.0;
        }
        return HEALTH_BONUS_BY_RANK[rank];
    }

    /** 在 ABSOLUTE_MAX_HEALTH cap 下，实际施加到 MAX_HEALTH 属性上的加成值 */
    public static double getEffectiveHealthBonus(net.minecraft.world.entity.player.Player player)
    {
        double bonus = PlayerCursedEnergyData.isHeavenlyRestricted(player)
                ? HEAVENLY_HEALTH_BONUS
                : getHealthBonusForRank(PlayerCursedGradeData.getRank(player));
        return Math.min(bonus, ABSOLUTE_MAX_HEALTH - BASE_MAX_HEALTH);
    }

    private static double resolveHealthBonus(ServerPlayer player)
    {
        return getEffectiveHealthBonus(player);
    }

    /** 自然恢复间隔（刻）：等级越高间隔越短，小幅提升 */
    public static int getRegenIntervalTicks(int rank)
    {
        return Math.max(10, 20 - rank * 2);
    }

    public static void applyAll(ServerPlayer player)
    {
        float oldMax = player.getMaxHealth();
        float oldHealth = player.getHealth();

        if (PlayerCursedEnergyData.isHeavenlyRestricted(player))
        {
            clearNormalModifiers(player);
            applyAttribute(player, Attributes.ATTACK_DAMAGE, HEAVENLY_ATTACK_UUID, "jjkz_heavenly_attack", HEAVENLY_ATTACK_BONUS, AttributeModifier.Operation.ADDITION);
            clearSprintSpeedModifier(player);
            applyAttribute(player, Attributes.MAX_HEALTH, HEAVENLY_HEALTH_UUID, "jjkz_heavenly_health", resolveHealthBonus(player), AttributeModifier.Operation.ADDITION);
            applyAttribute(player, Attributes.ARMOR, HEAVENLY_ARMOR_UUID, "jjkz_heavenly_armor", HEAVENLY_ARMOR_BONUS, AttributeModifier.Operation.ADDITION);
            // 天与咒缚常驻满额击退抗性，保证在任何时刻都不会被击退。
            applyAttribute(player, Attributes.KNOCKBACK_RESISTANCE, HEAVENLY_KNOCKBACK_RESISTANCE_UUID, "jjkz_heavenly_knockback_resistance", 1.0, AttributeModifier.Operation.ADDITION);
        }
        else
        {
            clearHeavenlyModifiers(player);
            int rank = PlayerCursedGradeData.getRank(player);

            applyAttribute(player, Attributes.ATTACK_DAMAGE, ATTACK_UUID, "jjkz_grade_attack", ATTACK_PER_RANK * rank, AttributeModifier.Operation.ADDITION);
            clearSprintSpeedModifier(player);
            applyAttribute(player, Attributes.MAX_HEALTH, HEALTH_UUID, "jjkz_grade_health", resolveHealthBonus(player), AttributeModifier.Operation.ADDITION);
            applyAttribute(player, Attributes.ARMOR, ARMOR_UUID, "jjkz_grade_armor", 4.0 * rank, AttributeModifier.Operation.ADDITION);
        }

        PlayerHealthData.scaleHealthAfterMaxChange(player, oldMax, oldHealth);
    }

    /**
     * 每刻调用：仅在疾跑时施加咒术等级移速加成（瞬态修饰符，非疾跑时移除）。
     */
    public static void updateSprintSpeedBonus(ServerPlayer player)
    {
        var movement = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement == null)
        {
            return;
        }
        movement.removeModifier(SPEED_UUID);
        movement.removeModifier(HEAVENLY_SPEED_UUID);
        movement.removeModifier(HEAVENLY_BURST_SPEED_UUID);

        boolean sprinting = player.isSprinting();
        if (!sprinting)
        {
            player.getPersistentData().putBoolean(TAG_HEAVENLY_WAS_SPRINTING, false);
            return;
        }

        if (PlayerCursedEnergyData.isHeavenlyRestricted(player))
        {
            boolean wasSprinting = player.getPersistentData().getBoolean(TAG_HEAVENLY_WAS_SPRINTING);
            int burstRemaining = player.getPersistentData().getInt(TAG_HEAVENLY_BURST_REMAINING);
            if (!wasSprinting)
            {
                // 仅在 Epic Fight 体力足够并扣除成功时，才允许触发这次 20 tick 爆发冲刺。
                if (!EpicFightCompat.tryConsumeHeavenlyBurstStamina(player))
                {
                    player.getPersistentData().putBoolean(TAG_HEAVENLY_WAS_SPRINTING, true);
                    player.getPersistentData().putInt(TAG_HEAVENLY_BURST_REMAINING, 0);
                    return;
                }
                // 刚进入疾跑时触发一次高加速度爆发，持续 20 tick。
                burstRemaining = HEAVENLY_SPRINT_BURST_TICKS;
                // 锁定爆发起点，在后续 20 tick 持续于该位置生成烟尘，营造伪瞬移残影。
                player.getPersistentData().putDouble(TAG_HEAVENLY_BURST_ORIGIN_X, player.getX());
                player.getPersistentData().putDouble(TAG_HEAVENLY_BURST_ORIGIN_Y, player.getY());
                player.getPersistentData().putDouble(TAG_HEAVENLY_BURST_ORIGIN_Z, player.getZ());
            }

            movement.addTransientModifier(new AttributeModifier(
                    HEAVENLY_SPEED_UUID,
                    "jjkz_heavenly_speed_sprint",
                    HEAVENLY_SPRINT_SPEED_BONUS,
                    AttributeModifier.Operation.ADDITION
            ));
            if (burstRemaining > 0)
            {
                movement.addTransientModifier(new AttributeModifier(
                        HEAVENLY_BURST_SPEED_UUID,
                        "jjkz_heavenly_speed_burst",
                        HEAVENLY_SPRINT_BURST_BONUS,
                        AttributeModifier.Operation.ADDITION
                ));
                applyHeavenlyBurstStealthAndSmoke(player);
                burstRemaining--;
            }
            player.getPersistentData().putInt(TAG_HEAVENLY_BURST_REMAINING, burstRemaining);
            player.getPersistentData().putBoolean(TAG_HEAVENLY_WAS_SPRINTING, true);
            return;
        }

        player.getPersistentData().putBoolean(TAG_HEAVENLY_WAS_SPRINTING, true);
        int rank = PlayerCursedGradeData.getRank(player);
        if (rank <= 0)
        {
            return;
        }
        double amount = SPRINT_SPEED_PER_RANK * rank;
        movement.addTransientModifier(new AttributeModifier(SPEED_UUID, "jjkz_grade_speed_sprint", amount, AttributeModifier.Operation.ADDITION));
    }

    /**
     * 天与咒缚冲刺爆发期（20 tick）附带隐身与起点烟尘，用于制造“伪瞬移”视觉。
     */
    private static void applyHeavenlyBurstStealthAndSmoke(ServerPlayer player)
    {
        // 持续短时刷新，保证爆发期间稳定隐身，结束后会自然消失。
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 6, 0, true, false, true));

        if (player.level() instanceof ServerLevel serverLevel)
        {
            double originX = player.getPersistentData().getDouble(TAG_HEAVENLY_BURST_ORIGIN_X);
            double originY = player.getPersistentData().getDouble(TAG_HEAVENLY_BURST_ORIGIN_Y);
            double originZ = player.getPersistentData().getDouble(TAG_HEAVENLY_BURST_ORIGIN_Z);
            serverLevel.sendParticles(ParticleTypes.CLOUD, originX, originY + 0.1D, originZ, 12, 0.25D, 0.08D, 0.25D, 0.01D);
            serverLevel.sendParticles(ParticleTypes.POOF, originX, originY + 0.15D, originZ, 10, 0.2D, 0.1D, 0.2D, 0.01D);
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, originX, originY + 0.2D, originZ, 4, 0.15D, 0.05D, 0.15D, 0.002D);
        }
    }

    private static void clearSprintSpeedModifier(ServerPlayer player)
    {
        var movement = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement != null)
        {
            movement.removeModifier(SPEED_UUID);
            movement.removeModifier(HEAVENLY_SPEED_UUID);
            movement.removeModifier(HEAVENLY_BURST_SPEED_UUID);
        }
    }

    /** 每刻调用：维持抗性效果（短持续时间叠加以避免误清其它来源时尽量温和） */
    public static void applyResistanceTick(ServerPlayer player, int rank)
    {
        if (PlayerCursedEnergyData.isHeavenlyRestricted(player))
        {
            // 天与咒缚：高常驻减伤与机动性，并附带间歇性生命恢复。
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 50, 1, true, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.JUMP, 50, 1, true, false, true));
            // 恢复间隔与持续时间游戏刻
            if (player.tickCount % 620 == 0)
            {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 256, 0, true, true, true));
            }
            return;
        }
        if (rank >= PlayerCursedGradeData.RANK_SPECIAL)
        {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 45, 1, true, false, true));
        }
        else if (rank >= PlayerCursedGradeData.RANK_GRADE_1)
        {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 45, 0, true, false, true));
        }
    }

    private static void applyAttribute(
            ServerPlayer player,
            net.minecraft.world.entity.ai.attributes.Attribute attribute,
            UUID id,
            String name,
            double amount,
            AttributeModifier.Operation operation
    )
    {
        var inst = player.getAttribute(attribute);
        if (inst == null)
        {
            return;
        }
        inst.removeModifier(id);
        if (amount != 0.0)
        {
            inst.addPermanentModifier(new AttributeModifier(id, name, amount, operation));
        }
    }

    private static void clearNormalModifiers(ServerPlayer player)
    {
        var attack = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null)
        {
            attack.removeModifier(ATTACK_UUID);
        }
        var health = player.getAttribute(Attributes.MAX_HEALTH);
        if (health != null)
        {
            health.removeModifier(HEALTH_UUID);
        }
        var armor = player.getAttribute(Attributes.ARMOR);
        if (armor != null)
        {
            armor.removeModifier(ARMOR_UUID);
        }
    }

    private static void clearHeavenlyModifiers(ServerPlayer player)
    {
        var attack = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null)
        {
            attack.removeModifier(HEAVENLY_ATTACK_UUID);
        }
        var health = player.getAttribute(Attributes.MAX_HEALTH);
        if (health != null)
        {
            health.removeModifier(HEAVENLY_HEALTH_UUID);
        }
        var armor = player.getAttribute(Attributes.ARMOR);
        if (armor != null)
        {
            armor.removeModifier(HEAVENLY_ARMOR_UUID);
        }
        var knockbackResistance = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (knockbackResistance != null)
        {
            knockbackResistance.removeModifier(HEAVENLY_KNOCKBACK_RESISTANCE_UUID);
        }
    }
}
