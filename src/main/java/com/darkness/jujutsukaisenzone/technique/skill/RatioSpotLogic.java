package com.darkness.jujutsukaisenzone.technique.skill;

import com.darkness.jujutsukaisenzone.player.PlayerRatioBreakData;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import com.darkness.jujutsukaisenzone.technique.development.SkillUpgradeLogic;
import com.darkness.jujutsukaisenzone.technique.skillset.RatioTechniqueSkillSet;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * 十划基础技：对目标挂随机部位光点；瞄准光点的近战命中结算击破与强制暴击。
 */
public final class RatioSpotLogic
{
    public static final double APPLY_RANGE = 8.0D;
    /** 光点存活时长（tick）。 */
    public static final int SPOT_DURATION_TICKS = 20 * 8;
    /** 视线到光点的最大垂距（格），越小越吃瞄准。 */
    public static final double HIT_RADIUS = 0.55D;
    /** 打中光点时的强制暴击倍率（相对当前伤害）。 */
    public static final float FORCED_CRIT_MULT = 1.5F;

    private static final String TAG_ROOT = "jujutsukaisenzone_ratio_spot";
    private static final String TAG_CASTER = "caster";
    private static final String TAG_PART = "part";
    private static final String TAG_EXPIRE = "expire";

    private RatioSpotLogic()
    {
    }

    public static LivingEntity resolveCrosshairTarget(ServerPlayer player, double maxDistance)
    {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(maxDistance));
        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(maxDistance)).inflate(1.0D);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                player,
                eye,
                end,
                searchBox,
                entity -> entity instanceof LivingEntity living
                        && living.isAlive()
                        && !living.isRemoved()
                        && !living.getUUID().equals(player.getUUID())
                        && living.isAttackable()
                        && !living.isSpectator()
                        && entity.isPickable(),
                maxDistance * maxDistance);
        if (hit != null && hit.getEntity() instanceof LivingEntity living)
        {
            return living;
        }
        return null;
    }

    /**
     * 对准准星生物挂点；成功返回 true（供技能集在扣费前校验）。
     */
    public static boolean tryApplySpot(ServerPlayer caster, LivingEntity target)
    {
        if (target == null || !target.isAlive() || target.isRemoved())
        {
            return false;
        }
        if (caster.distanceToSqr(target) > APPLY_RANGE * APPLY_RANGE)
        {
            caster.displayClientMessage(Component.literal("目标太远").withStyle(ChatFormatting.YELLOW), true);
            return false;
        }

        RatioBodyPart part = RatioBodyPart.random(caster.getRandom());
        long expire = caster.level().getGameTime() + spotDurationTicks(caster);
        CompoundTag spot = new CompoundTag();
        spot.putUUID(TAG_CASTER, caster.getUUID());
        spot.putString(TAG_PART, part.name());
        spot.putLong(TAG_EXPIRE, expire);
        target.getPersistentData().put(TAG_ROOT, spot);

        caster.displayClientMessage(
                Component.literal("七三弱点：" + part.displayName())
                        .withStyle(ChatFormatting.GOLD),
                true);
        spawnSpotBurst((ServerLevel) target.level(), part.worldAnchor(target));
        return true;
    }

    public static boolean hasActiveSpot(LivingEntity target, UUID casterUuid, long now)
    {
        CompoundTag spot = getSpotTag(target);
        if (spot == null)
        {
            return false;
        }
        if (!casterUuid.equals(spot.getUUID(TAG_CASTER)))
        {
            return false;
        }
        if (now > spot.getLong(TAG_EXPIRE))
        {
            clearSpot(target);
            return false;
        }
        return true;
    }

    public static RatioBodyPart getPart(LivingEntity target)
    {
        CompoundTag spot = getSpotTag(target);
        if (spot == null)
        {
            return null;
        }
        return RatioBodyPart.byName(spot.getString(TAG_PART));
    }

    public static void clearSpot(LivingEntity target)
    {
        target.getPersistentData().remove(TAG_ROOT);
    }

    /**
     * 攻击者视线是否足够贴近该目标当前光点。
     */
    public static boolean isAimingAtSpot(Player attacker, LivingEntity target)
    {
        RatioBodyPart part = getPart(target);
        if (part == null)
        {
            return false;
        }
        Vec3 spot = part.worldAnchor(target);
        Vec3 eye = attacker.getEyePosition();
        Vec3 look = attacker.getLookAngle().normalize();
        Vec3 toSpot = spot.subtract(eye);
        double along = toSpot.dot(look);
        if (along < 0.0D)
        {
            return false;
        }
        // 超过准星合理触及也不算（防隔空点名）
        if (along > APPLY_RANGE + 2.0D)
        {
            return false;
        }
        Vec3 closest = eye.add(look.scale(along));
        return closest.distanceToSqr(spot) <= HIT_RADIUS * HIT_RADIUS;
    }

    /**
     * 结算一次弱点击破：强制暴击、击破计数 +1、清除光点。
     *
     * @return 是否成功击破（由乘区暴击消费强制标记）
     */
    public static boolean tryBreakSpot(ServerPlayer attacker, LivingEntity target)
    {
        long now = attacker.level().getGameTime();
        if (!hasActiveSpot(target, attacker.getUUID(), now))
        {
            return false;
        }
        if (!isAimingAtSpot(attacker, target))
        {
            return false;
        }

        RatioBodyPart part = getPart(target);
        clearSpot(target);
        int breaks = PlayerRatioBreakData.add(attacker, 1);

        attacker.displayClientMessage(
                Component.literal("弱点击破！"
                                + (part != null ? part.displayName() : "")
                                + "  计数 "
                                + breaks
                                + "/"
                                + PlayerRatioBreakData.MAX_BREAKS)
                        .withStyle(ChatFormatting.RED),
                true);

        if (target.level() instanceof ServerLevel serverLevel && part != null)
        {
            spawnSpotBurst(serverLevel, part.worldAnchor(target));
        }

        return true;
    }

    /** 光点持续：Lv1=8s，每级 +1s。 */
    public static int spotDurationTicks(ServerPlayer caster)
    {
        return SPOT_DURATION_TICKS
                + 20 * SkillUpgradeLogic.upgradeSteps(
                caster, PlayerTechniqueData.TECHNIQUE_RATIO, RatioTechniqueSkillSet.SKILL_RATIO_MARK);
    }

    /** 破点暴击倍率：Lv1=1.50，每级 +0.05。 */
    public static float forcedCritMult(ServerPlayer caster)
    {
        return FORCED_CRIT_MULT
                + 0.05F * SkillUpgradeLogic.upgradeSteps(
                caster, PlayerTechniqueData.TECHNIQUE_RATIO, RatioTechniqueSkillSet.SKILL_RATIO_MARK);
    }

    /**
     * @deprecated 改用 {@link #tryBreakSpot}；倍率由 {@link #forcedCritMult} 注入强制暴击。
     */
    @Deprecated
    public static float resolveSpotBreak(ServerPlayer attacker, LivingEntity target, float currentDamage)
    {
        return tryBreakSpot(attacker, target) ? forcedCritMult(attacker) : 1.0F;
    }

    public static void tickSpotParticles(LivingEntity target)
    {
        CompoundTag spot = getSpotTag(target);
        if (spot == null)
        {
            return;
        }
        long now = target.level().getGameTime();
        if (now > spot.getLong(TAG_EXPIRE))
        {
            clearSpot(target);
            return;
        }
        if ((now % 4L) != 0L)
        {
            return;
        }
        if (!(target.level() instanceof ServerLevel serverLevel))
        {
            return;
        }
        RatioBodyPart part = RatioBodyPart.byName(spot.getString(TAG_PART));
        Vec3 pos = part.worldAnchor(target);
        serverLevel.sendParticles(
                ParticleTypes.END_ROD,
                pos.x, pos.y, pos.z,
                1,
                0.02D, 0.02D, 0.02D,
                0.0D);
        serverLevel.sendParticles(
                ParticleTypes.ELECTRIC_SPARK,
                pos.x, pos.y, pos.z,
                2,
                0.05D, 0.05D, 0.05D,
                0.0D);
    }

    private static CompoundTag getSpotTag(LivingEntity target)
    {
        CompoundTag data = target.getPersistentData();
        if (!data.contains(TAG_ROOT))
        {
            return null;
        }
        return data.getCompound(TAG_ROOT);
    }

    private static void spawnSpotBurst(ServerLevel level, Vec3 pos)
    {
        level.sendParticles(
                ParticleTypes.FLASH,
                pos.x, pos.y, pos.z,
                1,
                0.0D, 0.0D, 0.0D,
                0.0D);
        level.sendParticles(
                ParticleTypes.END_ROD,
                pos.x, pos.y, pos.z,
                12,
                0.15D, 0.15D, 0.15D,
                0.02D);
    }
}
