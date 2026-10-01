package com.darkness.jujutsukaisenzone.technique.skill;

import com.darkness.jujutsukaisenzone.player.PlayerRatioBreakData;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import com.darkness.jujutsukaisenzone.technique.TechniqueDamageSources;
import com.darkness.jujutsukaisenzone.technique.development.SkillUpgradeLogic;
import com.darkness.jujutsukaisenzone.technique.skillset.RatioTechniqueSkillSet;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 瓦落瓦落：把十划扩张到周围无机物——碎出带咒力的坠落方块乱撞。
 * <p>
 * MVP：球形采集可发射方块 → {@link FallingBlockEntity}，数量 = 基础 + 消耗的击破计数。
 */
public final class RatioCollapseLogic
{
    /** 不消耗击破时至少飞出的碎块数。 */
    public static final int BASE_DEBRIS = 3;
    /** 单次最多另吃多少击破（每点多 1 块）。 */
    public static final int MAX_BREAKS_CONSUMED = 8;
    public static final double BREAK_RADIUS = 4.0D;
    public static final float HURT_DAMAGE = 0.25F;
    public static final int HURT_MAX = 16;
    public static final double LAUNCH_SPEED = 0.55D;
    public static final double LAUNCH_UP = 0.42D;

    private RatioCollapseLogic()
    {
    }

    public static boolean tryCast(ServerPlayer player)
    {
        if (!(player.level() instanceof ServerLevel level))
        {
            return false;
        }

        int consumed = PlayerRatioBreakData.consume(player, MAX_BREAKS_CONSUMED);
        int debrisCount = BASE_DEBRIS + consumed;

        BlockPos origin = BlockPos.containing(player.getEyePosition().add(player.getLookAngle().scale(1.5D)));
        List<BlockPos> candidates = collectLaunchable(level, player, origin, BREAK_RADIUS);
        if (candidates.isEmpty())
        {
            // 退还击破：没有可碎方块时不白吃粮
            if (consumed > 0)
            {
                PlayerRatioBreakData.add(player, consumed);
            }
            player.displayClientMessage(
                    Component.literal("附近没有可切削的方块").withStyle(ChatFormatting.YELLOW),
                    true);
            return false;
        }

        Collections.shuffle(candidates);
        int launched = 0;
        Vec3 center = Vec3.atCenterOf(origin);

        for (BlockPos pos : candidates)
        {
            if (launched >= debrisCount)
            {
                break;
            }
            BlockState state = level.getBlockState(pos);
            if (!isLaunchable(level, player, pos, state))
            {
                continue;
            }

            FallingBlockEntity flying = FallingBlockEntity.fall(level, pos, state);
            if (flying == null)
            {
                continue;
            }

            flying.dropItem = false;
            flying.getPersistentData().putBoolean(TechniqueDamageSources.TAG_RATIO_DEBRIS, true);
            flying.getPersistentData().putUUID("jujutsukaisenzone_ratio_debris_caster", player.getUUID());
            float hurt = SkillUpgradeLogic.scaleRatio(
                    player,
                    PlayerTechniqueData.TECHNIQUE_RATIO,
                    RatioTechniqueSkillSet.SKILL_COLLAPSE,
                    HURT_DAMAGE);
            flying.setHurtsEntities(hurt, HURT_MAX);
            flying.setDeltaMovement(randomLaunch(level, center, Vec3.atCenterOf(pos)));
            launched++;
        }

        if (launched <= 0)
        {
            if (consumed > 0)
            {
                PlayerRatioBreakData.add(player, consumed);
            }
            player.displayClientMessage(
                    Component.literal("瓦落瓦落未能掀起方块").withStyle(ChatFormatting.YELLOW),
                    true);
            return false;
        }

        // 吃了击破却块数不够时，按实际块数退还差额（可选宽松：不退）
        int unusedBreakBudget = Math.max(0, debrisCount - launched - BASE_DEBRIS);
        if (unusedBreakBudget > 0 && consumed > 0)
        {
            int refund = Math.min(unusedBreakBudget, consumed);
            PlayerRatioBreakData.add(player, refund);
            consumed -= refund;
        }

        level.playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_EXPLODE,
                SoundSource.PLAYERS,
                0.55F,
                1.25F);
        level.sendParticles(
                ParticleTypes.CLOUD,
                center.x, center.y, center.z,
                24,
                1.2D, 0.8D, 1.2D,
                0.05D);
        player.displayClientMessage(
                Component.literal("瓦落瓦落 ×" + launched
                                + (consumed > 0 ? "（击破 -" + consumed + "）" : ""))
                        .withStyle(ChatFormatting.GOLD),
                true);
        return true;
    }

    private static List<BlockPos> collectLaunchable(
            ServerLevel level, ServerPlayer player, BlockPos origin, double radius)
    {
        List<BlockPos> list = new ArrayList<>();
        int r = Mth.ceil(radius);
        double radiusSqr = radius * radius;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; dx++)
        {
            for (int dy = -r; dy <= r; dy++)
            {
                for (int dz = -r; dz <= r; dz++)
                {
                    if (dx * dx + dy * dy + dz * dz > radiusSqr)
                    {
                        continue;
                    }
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    BlockState state = level.getBlockState(cursor);
                    if (isLaunchable(level, player, cursor, state))
                    {
                        list.add(cursor.immutable());
                    }
                }
            }
        }
        return list;
    }

    private static boolean isLaunchable(
            ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state)
    {
        if (state.isAir() || !state.getFluidState().isEmpty() || state.hasBlockEntity())
        {
            return false;
        }
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0.0F || hardness > 50.0F)
        {
            return false;
        }
        return player.mayInteract(level, pos);
    }

    private static Vec3 randomLaunch(ServerLevel level, Vec3 center, Vec3 blockCenter)
    {
        Vec3 offset = blockCenter.subtract(center);
        double horiz = offset.horizontalDistance();
        Vec3 dir;
        if (horiz < 0.2D)
        {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            dir = new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
        }
        else
        {
            dir = new Vec3(offset.x / horiz, 0.0D, offset.z / horiz);
        }
        double spread = 0.22D;
        double speed = LAUNCH_SPEED * (0.75D + level.random.nextDouble() * 0.5D);
        return new Vec3(
                dir.x * speed + Mth.nextDouble(level.random, -spread, spread),
                LAUNCH_UP + Mth.nextDouble(level.random, 0.05D, 0.25D),
                dir.z * speed + Mth.nextDouble(level.random, -spread, spread));
    }
}
