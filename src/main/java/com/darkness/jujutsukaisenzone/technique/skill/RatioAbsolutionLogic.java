package com.darkness.jujutsukaisenzone.technique.skill;

import com.darkness.jujutsukaisenzone.player.PlayerRatioOvertimeData;
import com.darkness.jujutsukaisenzone.technique.TechniqueDamageSources;
import com.darkness.jujutsukaisenzone.technique.skillset.SkillDamageType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 无可谅解：仅加班时段可用。重击击飞、可穿软方块并多段撞击，随后进入「工作效率」。
 */
public final class RatioAbsolutionLogic
{
    public static final float DAMAGE = 18.0F;
    public static final float IMPACT_DAMAGE = 6.0F;
    public static final double RANGE = 8.0D;
    public static final double HALF_WIDTH = 1.2D;
    public static final double KNOCKBACK = 1.35D;
    public static final double LAUNCH_Y = 0.45D;
    public static final int MAX_BLOCK_BREAKS = 12;
    public static final float MAX_BLOCK_HARDNESS = 5.0F;

    private RatioAbsolutionLogic()
    {
    }

    public static boolean tryCast(ServerPlayer player)
    {
        if (!RatioOvertimeLogic.canUseAbsolution(player))
        {
            if (!RatioOvertimeLogic.hasVowUnlocked(player))
            {
                player.displayClientMessage(
                        Component.literal("尚未立下加班契约").withStyle(ChatFormatting.YELLOW),
                        true);
            }
            else if (PlayerRatioOvertimeData.isVowBroken(player))
            {
                player.displayClientMessage(
                        Component.literal("今日已违约，无法使用无可谅解").withStyle(ChatFormatting.YELLOW),
                        true);
            }
            else
            {
                player.displayClientMessage(
                        Component.literal("仅加班时段（夜晚）可用").withStyle(ChatFormatting.YELLOW),
                        true);
            }
            return false;
        }

        if (!(player.level() instanceof ServerLevel level))
        {
            return false;
        }

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        AABB box = player.getBoundingBox()
                .expandTowards(look.scale(RANGE))
                .inflate(HALF_WIDTH, 0.8D, HALF_WIDTH);

        Set<UUID> hit = new HashSet<>();
        int livingHits = 0;
        for (LivingEntity living : level.getEntitiesOfClass(
                LivingEntity.class,
                box,
                entity -> entity.isAlive()
                        && !entity.isRemoved()
                        && entity.isAttackable()
                        && !entity.getUUID().equals(player.getUUID())
                        && !entity.isSpectator()
                        && entity.isPickable()))
        {
            Vec3 to = living.getEyePosition().subtract(eye);
            double dist = to.length();
            if (dist < 0.01D || dist > RANGE + living.getBbWidth())
            {
                continue;
            }
            if (to.normalize().dot(look) < 0.35D)
            {
                continue;
            }
            if (TechniqueDamageSources.deal(living, player, DAMAGE, SkillDamageType.MARTIAL))
            {
                livingHits++;
                hit.add(living.getUUID());
                Vec3 kb = look.scale(KNOCKBACK).add(0.0D, LAUNCH_Y, 0.0D);
                living.setDeltaMovement(living.getDeltaMovement().add(kb));
                living.hurtMarked = true;
            }
        }

        int blocksBroken = smashBlocks(player, level, eye, look);

        // 穿墙后对落点附近再砸一次（多重撞击）
        Vec3 tip = eye.add(look.scale(RANGE * 0.85D));
        AABB tipBox = new AABB(tip, tip).inflate(1.6D);
        int impactHits = 0;
        for (LivingEntity living : level.getEntitiesOfClass(
                LivingEntity.class,
                tipBox,
                entity -> entity.isAlive()
                        && !entity.isRemoved()
                        && entity.isAttackable()
                        && !entity.getUUID().equals(player.getUUID())
                        && !hit.contains(entity.getUUID())))
        {
            if (TechniqueDamageSources.deal(living, player, IMPACT_DAMAGE, SkillDamageType.MARTIAL))
            {
                impactHits++;
                living.setDeltaMovement(living.getDeltaMovement().add(look.scale(0.55D).add(0.0D, 0.25D, 0.0D)));
                living.hurtMarked = true;
            }
        }

        player.setDeltaMovement(player.getDeltaMovement().add(look.scale(0.9D).add(0.0D, 0.15D, 0.0D)));
        player.hurtMarked = true;

        PlayerRatioOvertimeData.startEfficiency(player);

        level.playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.ANVIL_LAND,
                SoundSource.PLAYERS,
                0.7F,
                0.75F);
        level.sendParticles(
                ParticleTypes.EXPLOSION,
                tip.x, tip.y, tip.z,
                3,
                0.4D, 0.3D, 0.4D,
                0.01D);
        player.displayClientMessage(
                Component.literal("无可谅解！"
                                + (livingHits > 0 ? " 命中" + livingHits : "")
                                + (impactHits > 0 ? " 撞击" + impactHits : "")
                                + (blocksBroken > 0 ? " 破块" + blocksBroken : "")
                                + " · 工作效率")
                        .withStyle(ChatFormatting.GOLD),
                true);
        return true;
    }

    private static int smashBlocks(ServerPlayer player, ServerLevel level, Vec3 eye, Vec3 look)
    {
        int broken = 0;
        for (double t = 0.5D; t <= RANGE && broken < MAX_BLOCK_BREAKS; t += 0.55D)
        {
            BlockPos pos = BlockPos.containing(eye.add(look.scale(t)));
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || !state.getFluidState().isEmpty() || state.hasBlockEntity())
            {
                continue;
            }
            float hardness = state.getDestroySpeed(level, pos);
            if (hardness < 0.0F || hardness > MAX_BLOCK_HARDNESS)
            {
                continue;
            }
            if (!player.mayInteract(level, pos))
            {
                continue;
            }
            if (level.destroyBlock(pos, true, player))
            {
                broken++;
            }
        }
        return broken;
    }
}
