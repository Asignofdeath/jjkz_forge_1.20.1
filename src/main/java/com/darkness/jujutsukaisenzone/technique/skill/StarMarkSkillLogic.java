package com.darkness.jujutsukaisenzone.technique.skill;

import com.darkness.jujutsukaisenzone.player.PlayerStarMarkData;
import com.darkness.jujutsukaisenzone.player.StarMark;
import com.darkness.jujutsukaisenzone.player.StarMarkType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * 「星星标记」：把当前的星打到接触到的目标身上，并负责标记的解析、失效清理与粒子表现。
 * <p>
 * 与原作一致，施加方式是**接触**而不是远距离视线点选：近战命中实体（由 {@code StarMarkEvents} 挂载）
 * / 看向方块后按「星星标记」技能键（触及距离 {@link #LOOK_REACH} 格）。
 * 两条入口最终都汇入 {@code LoveRendezvousTechniqueSkillSet#tryApplyMark}。
 * 力学的三态规则不在这里，见 {@link StarMarkRules}。
 */
public final class StarMarkSkillLogic
{
    /** 粒子重绘间隔（tick）。 */
    public static final int RENDER_INTERVAL_TICKS = 5;

    /** 标记火花相对目标的高度偏移。 */
    private static final double BLOCK_MARK_HEIGHT = 1.15D;
    private static final double ITEM_MARK_HEIGHT = 0.35D;
    private static final double ENTITY_MARK_HEIGHT = 0.45D;

    /** 火花默认尺寸。 */
    private static final float SPARK_SCALE = 0.45F;

    /** 技能键打方块时的最大可及距离（格）。沿用右键年代的方块触及距离，手感不变。 */
    private static final double LOOK_REACH = 4.5D;

    private StarMarkSkillLogic()
    {
    }

    /** 一个已解析到世界坐标的标记。{@code entity} 为 null 表示方块锚点。 */
    public record ResolvedMark(StarMark mark, Vec3 position, @Nullable Entity entity)
    {
    }

    // ======================== 打标 ========================

    /** 以当前选中的星，构造某个实体身上的标记（掉落物归 ITEM，其余归 ENTITY）。 */
    public static StarMark markOfEntity(ServerPlayer player, Entity entity)
    {
        StarMark.Kind kind = entity instanceof ItemEntity ? StarMark.Kind.ITEM : StarMark.Kind.ENTITY;
        return new StarMark(
                kind,
                player.level().dimension(),
                entity.blockPosition(),
                entity.getUUID(),
                PlayerStarMarkData.getSelected(player));
    }

    /** 以当前选中的星，构造某个方块上的标记。 */
    public static StarMark markOfBlock(ServerPlayer player, BlockPos pos)
    {
        return new StarMark(
                StarMark.Kind.BLOCK,
                player.level().dimension(),
                pos.immutable(),
                null,
                PlayerStarMarkData.getSelected(player));
    }

    /**
     * 取玩家视线命中的方块，供「技能键打方块」使用。
     * <p>
     * 完全在服务端自行射线结算，客户端只上报「按了技能键」，因此不受客户端视线与包顺序影响；
     * 触及距离见 {@link #LOOK_REACH}，命中空气或未命中方块时返回 {@code null}。
     */
    @Nullable
    public static BlockPos lookedAtBlock(ServerPlayer player)
    {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1.0F).scale(LOOK_REACH));
        BlockHitResult hit = player.level().clip(
                new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK)
        {
            return null;
        }
        BlockPos pos = hit.getBlockPos();
        return player.level().getBlockState(pos).isAir() ? null : pos.immutable();
    }

    /** 目标的中文描述，用于动作栏提示。 */
    public static String describeTarget(Entity entity)
    {
        return entity instanceof ItemEntity ? "掉落物" : "实体「" + entity.getName().getString() + "」";
    }

    /** 目标方块的中文描述，用于动作栏提示。 */
    public static String describeBlock(BlockPos pos)
    {
        return "方块 (" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")";
    }

    // ======================== 解析与表现 ========================

    /**
     * 解析该玩家所有仍有效的标记、重绘火花，并顺手解除失效的标记。
     * <p>
     * 解除条件：维度改变、方块被破坏、实体确认已消失、超出 {@link StarMarkRules#MARK_RANGE} 格。
     * 批量失效只写一次 NBT。
     *
     * @return 本 tick 参与力学计算与表现的有效标记
     */
    public static List<ResolvedMark> resolveAndRender(ServerPlayer player)
    {
        List<StarMark> stored = PlayerStarMarkData.getAll(player);
        if (stored.isEmpty())
        {
            return List.of();
        }

        ServerLevel level = player.serverLevel();
        Vec3 ownerPos = player.position();
        double rangeSqr = StarMarkRules.MARK_RANGE * StarMarkRules.MARK_RANGE;

        List<ResolvedMark> resolved = new ArrayList<>(stored.size());
        List<StarMark> kept = new ArrayList<>(stored.size());
        boolean dirty = false;

        for (StarMark mark : stored)
        {
            /* 换维度即解除 */
            if (!mark.dimension().equals(level.dimension()))
            {
                dirty = true;
                continue;
            }

            if (!mark.mobile())
            {
                BlockPos pos = mark.pos();
                if (!level.isLoaded(pos))
                {
                    /* 区块未加载：既不算失效也不参与表现，原样保留 */
                    kept.add(mark);
                    continue;
                }
                if (level.getBlockState(pos).isAir())
                {
                    dirty = true;
                    continue;
                }
                Vec3 anchor = new Vec3(pos.getX() + 0.5D, pos.getY() + BLOCK_MARK_HEIGHT, pos.getZ() + 0.5D);
                if (anchor.distanceToSqr(ownerPos) > rangeSqr)
                {
                    dirty = true;
                    continue;
                }
                kept.add(mark);
                resolved.add(new ResolvedMark(mark, anchor, null));
                continue;
            }

            Entity entity = mark.entityId() == null ? null : level.getEntity(mark.entityId());
            if (entity == null || entity.isRemoved() || !entity.isAlive())
            {
                /* 仅当缓存坐标所在区块已加载时才能确认「实体已消失」，否则可能只是区块未加载 */
                if (level.isLoaded(mark.pos()))
                {
                    dirty = true;
                }
                else
                {
                    kept.add(mark);
                }
                continue;
            }

            StarMark refreshed = mark;
            BlockPos currentPos = entity.blockPosition();
            if (!currentPos.equals(mark.pos()))
            {
                refreshed = mark.withPos(currentPos);
                dirty = true;
            }

            double top = entity.getY() + entity.getBbHeight();
            double height = mark.kind() == StarMark.Kind.ITEM ? ITEM_MARK_HEIGHT : ENTITY_MARK_HEIGHT;
            Vec3 anchor = new Vec3(entity.getX(), top + height, entity.getZ());
            if (anchor.distanceToSqr(ownerPos) > rangeSqr)
            {
                dirty = true;
                continue;
            }
            kept.add(refreshed);
            resolved.add(new ResolvedMark(refreshed, anchor, entity));
        }

        if (dirty)
        {
            PlayerStarMarkData.replaceAll(player, kept);
        }

        if (level.getGameTime() % RENDER_INTERVAL_TICKS == 0L)
        {
            for (ResolvedMark mark : resolved)
            {
                spawnSpark(level, mark.position(), mark.mark().type(), SPARK_SCALE);
            }
        }
        return resolved;
    }

    /** 标记火花：随星色的尘土 + 缓慢上下浮动，隔一段时间补一颗闪光。 */
    public static void spawnSpark(ServerLevel level, Vec3 pos, StarMarkType type, float scale)
    {
        long time = level.getGameTime();
        double y = pos.y + Math.sin(time * 0.25D) * 0.06D;
        level.sendParticles(dust(type, scale), pos.x, y, pos.z, 1, 0.03D, 0.03D, 0.03D, 0.0D);
        if (time % (RENDER_INTERVAL_TICKS * 2L) == 0L)
        {
            level.sendParticles(ParticleTypes.END_ROD, pos.x, y, pos.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    /** 两点之间的星色粒子线（牵引用）。 */
    public static void spawnLine(ServerLevel level, Vec3 from, Vec3 to, StarMarkType type)
    {
        int steps = 8;
        DustParticleOptions dust = dust(type, 0.35F);
        for (int i = 1; i < steps; i++)
        {
            Vec3 point = from.lerp(to, i / (double) steps);
            level.sendParticles(dust, point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    /** 以中心为心的水平星色粒子环（禁行域用）。 */
    public static void spawnRing(ServerLevel level, Vec3 center, double radius, StarMarkType type)
    {
        int points = 18;
        double phase = level.getGameTime() * 0.04D;
        DustParticleOptions dust = dust(type, 0.6F);
        for (int i = 0; i < points; i++)
        {
            double angle = phase + Math.PI * 2.0D * i / points;
            level.sendParticles(
                    dust,
                    center.x + Math.cos(angle) * radius,
                    center.y,
                    center.z + Math.sin(angle) * radius,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private static DustParticleOptions dust(StarMarkType type, float scale)
    {
        return new DustParticleOptions(new Vector3f(type.red(), type.green(), type.blue()), scale);
    }
}
