package com.darkness.jujutsukaisenzone.player;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

/**
 * 强化术开启期间的属性加成（仅服务端）。
 */
public final class StrengthenTechniqueBonuses
{
    private static final UUID HEALTH_UUID = UUID.fromString("6b2c9a01-2001-4000-8000-000000000001");
    private static final UUID SPEED_UUID = UUID.fromString("6b2c9a01-2002-4000-8000-000000000002");
    private static final UUID ATTACK_UUID = UUID.fromString("6b2c9a01-2003-4000-8000-000000000003");

    /** 初习开启时约 5 格跳高的起跳 Y 速度（原版约 0.42）。 */
    public static final double BASE_JUMP_Y_VELOCITY = 0.85D;
    /** 原版玩家基础起跳 Y 速度，用于换算跳跃提升效果等级。 */
    private static final double VANILLA_JUMP_Y_VELOCITY = 0.42D;
    public static final double ACTIVE_SPEED_BONUS = 0.20D;
    public static final double ACTIVE_ATTACK_BONUS = 0.40D;

    private StrengthenTechniqueBonuses()
    {
    }

    public static void applyActiveModifiers(ServerPlayer player)
    {
        clearModifiers(player);
        if (!PlayerStrengthenTechniqueData.isActive(player))
        {
            return;
        }

        StrengthenTechniqueLevel level = PlayerStrengthenTechniqueData.getLevel(player);
        double healthBonus = level.getActiveHealthBonusMultiplier();
        if (healthBonus > 0.0D)
        {
            applyAttribute(player, Attributes.MAX_HEALTH, HEALTH_UUID, "jjkz_strengthen_health",
                    healthBonus, AttributeModifier.Operation.MULTIPLY_TOTAL);
        }
        applyAttribute(player, Attributes.MOVEMENT_SPEED, SPEED_UUID, "jjkz_strengthen_speed",
                ACTIVE_SPEED_BONUS, AttributeModifier.Operation.MULTIPLY_TOTAL);
        applyAttribute(player, Attributes.ATTACK_DAMAGE, ATTACK_UUID, "jjkz_strengthen_attack",
                ACTIVE_ATTACK_BONUS, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    public static double resolveJumpYVelocity(ServerPlayer player)
    {
        StrengthenTechniqueLevel level = PlayerStrengthenTechniqueData.getLevel(player);
        return BASE_JUMP_Y_VELOCITY * level.getJumpVelocityMultiplier();
    }

    /** 初习约 5 格跳高；每升一级再 +10%。 */
    public static double resolveJumpHeightBlocks(StrengthenTechniqueLevel level)
    {
        return 5.0D * level.getJumpVelocityMultiplier();
    }

    public static void applyJumpBoostTick(ServerPlayer player)
    {
        if (!PlayerStrengthenTechniqueData.isActive(player))
        {
            return;
        }
        int amplifier = resolveJumpAmplifier(PlayerStrengthenTechniqueData.getLevel(player));
        player.addEffect(new MobEffectInstance(MobEffects.JUMP, 10, amplifier, true, false, true));
    }

    static int resolveJumpAmplifier(StrengthenTechniqueLevel level)
    {
        double targetY = BASE_JUMP_Y_VELOCITY * level.getJumpVelocityMultiplier();
        return Math.max(0, (int) Math.ceil((targetY - VANILLA_JUMP_Y_VELOCITY) / 0.1D) - 1);
    }

    private static void clearModifiers(ServerPlayer player)
    {
        removeModifier(player, Attributes.MAX_HEALTH, HEALTH_UUID);
        removeModifier(player, Attributes.MOVEMENT_SPEED, SPEED_UUID);
        removeModifier(player, Attributes.ATTACK_DAMAGE, ATTACK_UUID);
    }

    private static void applyAttribute(
            ServerPlayer player,
            net.minecraft.world.entity.ai.attributes.Attribute attribute,
            UUID uuid,
            String name,
            double amount,
            AttributeModifier.Operation operation)
    {
        var instance = player.getAttribute(attribute);
        if (instance == null)
        {
            return;
        }
        instance.removeModifier(uuid);
        instance.addPermanentModifier(new AttributeModifier(uuid, name, amount, operation));
    }

    private static void removeModifier(ServerPlayer player, net.minecraft.world.entity.ai.attributes.Attribute attribute, UUID uuid)
    {
        var instance = player.getAttribute(attribute);
        if (instance != null)
        {
            instance.removeModifier(uuid);
        }
    }
}
