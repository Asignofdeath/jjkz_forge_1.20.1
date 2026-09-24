package com.darkness.jujutsukaisenzone.technique.skill;

import com.darkness.jujutsukaisenzone.player.PlayerStarMarkData;
import com.darkness.jujutsukaisenzone.player.StarMark;
import com.darkness.jujutsukaisenzone.player.StarMarkType;
import com.darkness.jujutsukaisenzone.technique.BarrierTechniqueServerLogic;
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

    /**
     * 以当前选中的星，构造某个实体身上的标记（掉落物归 ITEM，其余归 ENTITY）。
     * <p>
     * 这里造出的永远是<b>全新的 1 级标记</b>，并带上「此刻」作为最后打标时刻
     * （{@link StarMark#lastMarkedAt()}）——叠加与换星都走这条路径重新构造，所以每次打标都会
     * 刷新星位等级的衰减计时。跨序的「经过」进度不在标记上（它属于星域实例，展开时统一重置，
     * 见 {@code StarMarkRules}），星位等级的叠加由打标入口负责
     * （见 {@code LoveRendezvousTechniqueSkillSet#tryApplyMark}）。
     */
    public static StarMark markOfEntity(ServerPlayer player, Entity entity)
    {
        return markOfEntity(player, entity, PlayerStarMarkData.getSelected(player));
    }

    /**
     * 以<b>指定</b>的星构造某个实体身上的标记。
     * <p>
     * 供「引爆传染」使用——传染过去的是<b>被引爆的那颗星</b>，而不是施术者此刻选中的星，
     * 所以不能走 {@link #markOfEntity(ServerPlayer, Entity)}（那个读的是当前选中标记）。
     */
    public static StarMark markOfEntity(ServerPlayer player, Entity entity, StarMarkType type)
    {
        StarMark.Kind kind = entity instanceof ItemEntity ? StarMark.Kind.ITEM : StarMark.Kind.ENTITY;
        return new StarMark(
                kind,
                player.level().dimension(),
                entity.blockPosition(),
                entity.getUUID(),
                type,
                1,
                player.level().getGameTime());
    }

    /** 以当前选中的星，构造某个方块上的标记（同为 1 级，规则见 {@link #markOfEntity}）。 */
    public static StarMark markOfBlock(ServerPlayer player, BlockPos pos)
    {
        StarMarkType selected = PlayerStarMarkData.getSelected(player);
        return new StarMark(
                StarMark.Kind.BLOCK,
                player.level().dimension(),
                pos.immutable(),
                null,
                selected,
                1,
                player.level().getGameTime());
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
     * 解析该玩家所有仍有效的标记，并顺手解除失效的标记（不做任何表现）。
     * <p>
     * 解除条件：维度改变、方块被破坏、实体确认已消失、超出 {@link StarMarkRules#MARK_RANGE} 格，
     * 或超出持有上限 {@link PlayerStarMarkData#MAX_MARKS}（按「最久没被打标」顶掉）。
     * 区块未加载时既不算失效也不参与结算，原样保留；批量失效只写一次 NBT。
     * <p>
     * 这里同时是<b>星位等级衰减</b>的唯一结算点：距最后一次打标每满
     * {@link StarMarkRules#STACK_DECAY_TICKS} 掉 1 级，最低停在 1 级（见
     * {@link StarMarkRules#decayedStacks(StarMark, long)}）。放在这里是因为本方法本来就每 tick
     * 走一遍全部标记、并且只在真有变化时才写 NBT——静止不动时仍然是零写入。
     *
     * @return 本 tick 有效、可参与力学与准星判定的标记
     */
    public static List<ResolvedMark> resolveAll(ServerPlayer player)
    {
        List<StarMark> stored = PlayerStarMarkData.getAll(player);
        if (stored.isEmpty())
        {
            return List.of();
        }

        ServerLevel level = player.serverLevel();
        Vec3 ownerPos = player.position();
        long now = level.getGameTime();
        double rangeSqr = StarMarkRules.MARK_RANGE * StarMarkRules.MARK_RANGE;

        /* 持有上限是本轮才引入的，老存档可能残留超出上限的条目——先修剪，后面就不会再见到它们 */
        boolean dirty = !PlayerStarMarkData.evictOverflow(stored, null).isEmpty();

        List<ResolvedMark> resolved = new ArrayList<>(stored.size());
        List<StarMark> kept = new ArrayList<>(stored.size());

        for (StarMark mark : stored)
        {
            /* 换维度即解除 */
            if (!mark.dimension().equals(level.dimension()))
            {
                dirty = true;
                continue;
            }

            /* 星位衰减：只吃掉「叠上去的部分」，最低停在 1 级，标记本身不因时间消失 */
            int decayed = StarMarkRules.decayedStacks(mark, now);
            StarMark settled = decayed == mark.stacks() ? mark : mark.withStacks(decayed);
            if (settled != mark)
            {
                dirty = true;
            }

            if (!settled.mobile())
            {
                BlockPos pos = settled.pos();
                if (!level.isLoaded(pos))
                {
                    /* 区块未加载：既不算失效也不参与表现，原样保留 */
                    kept.add(settled);
                    continue;
                }
                if (level.getBlockState(pos).isAir())
                {
                    dirty = true;
                    continue;
                }
                Vec3 anchor = anchorOf(settled, null);
                if (anchor.distanceToSqr(ownerPos) > rangeSqr)
                {
                    dirty = true;
                    continue;
                }
                kept.add(settled);
                resolved.add(new ResolvedMark(settled, anchor, null));
                continue;
            }

            Entity entity = settled.entityId() == null ? null : level.getEntity(settled.entityId());
            if (entity == null || entity.isRemoved() || !entity.isAlive())
            {
                /* 仅当缓存坐标所在区块已加载时才能确认「实体已消失」，否则可能只是区块未加载 */
                if (level.isLoaded(settled.pos()))
                {
                    dirty = true;
                }
                else
                {
                    kept.add(settled);
                }
                continue;
            }

            StarMark refreshed = settled;
            BlockPos currentPos = entity.blockPosition();
            if (!currentPos.equals(settled.pos()))
            {
                refreshed = settled.withPos(currentPos);
                dirty = true;
            }

            Vec3 anchor = anchorOf(settled, entity);
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
        return resolved;
    }

    /** 解析 + 重绘火花；每 tick 的力学结算与表现入口。 */
    public static List<ResolvedMark> resolveAndRender(ServerPlayer player)
    {
        List<ResolvedMark> resolved = resolveAll(player);
        if (resolved.isEmpty())
        {
            return resolved;
        }
        ServerLevel level = player.serverLevel();
        if (level.getGameTime() % RENDER_INTERVAL_TICKS == 0L)
        {
            for (ResolvedMark mark : resolved)
            {
                spawnSpark(level, mark.position(), mark.mark().type(), SPARK_SCALE);
            }
        }
        return resolved;
    }

    /**
     * 标记的锚点：方块为方块正上方 {@link #BLOCK_MARK_HEIGHT} 格，实体为头顶之上。
     * <p>
     * 施术者给自己打的标记同样走 {@code ENTITY} 分支——锚点落在自己头顶，与打给别人的完全一致。
     */
    public static Vec3 anchorOf(StarMark mark, @Nullable Entity entity)
    {
        if (entity == null)
        {
            BlockPos pos = mark.pos();
            return new Vec3(pos.getX() + 0.5D, pos.getY() + BLOCK_MARK_HEIGHT, pos.getZ() + 0.5D);
        }
        double height = mark.kind() == StarMark.Kind.ITEM ? ITEM_MARK_HEIGHT : ENTITY_MARK_HEIGHT;
        return new Vec3(entity.getX(), entity.getY() + entity.getBbHeight() + height, entity.getZ());
    }

    // ======================== 准星判定（清除用） ========================

    /** 准星判定容差（格）：实体按碰撞箱半宽 + 该值，方块按方块半宽 + 该值。 */
    private static final double CROSSHAIR_TOLERANCE = 0.4D;

    /**
     * 取准星命中的「已标记目标」，供双击 Shift 的单独清除使用。
     * <p>
     * <b>距离无限、允许穿墙</b>，并且候选集只有施术者自己持有的标记——因为按定义，
     * 「目标」就是已经带上标记的方块 / 掉落物 / 实体，所以这里不需要对世界做射线，
     * 也不受区块是否加载影响。判定方式：把锚点投到视线上，在「不在背后」且「垂距落在容差内」
     * 的候选里取沿视线最近的一个。
     *
     * @return 命中的标记；未命中返回 {@code null}
     */
    @Nullable
    public static StarMark pickMarkedTarget(ServerPlayer player)
    {
        Vec3 eye = player.getEyePosition();
        Vec3 dir = player.getViewVector(1.0F);
        if (dir.lengthSqr() < 1.0E-8D)
        {
            return null;
        }
        dir = dir.normalize();

        double bestDistance = Double.MAX_VALUE;
        StarMark best = null;

        List<ResolvedMark> resolved = resolveAll(player);
        for (ResolvedMark mark : resolved)
        {
            double distance = crosshairDistance(eye, dir, mark.position(),
                    crosshairRadius(mark.mark(), mark.entity()));
            if (distance >= 0.0D && distance < bestDistance)
            {
                bestDistance = distance;
                best = mark.mark();
            }
        }

        /* 兜底：区块未加载而解析不出的标记，用最后一次已知坐标继续参与判定，
           否则「无限距离 + 穿墙」在远处目标上会直接失效。 */
        for (StarMark mark : PlayerStarMarkData.getAll(player))
        {
            if (!mark.dimension().equals(player.level().dimension()) || containsTarget(resolved, mark))
            {
                continue;
            }
            double distance = crosshairDistance(eye, dir, anchorOf(mark, null), CROSSHAIR_TOLERANCE + 0.5D);
            if (distance >= 0.0D && distance < bestDistance)
            {
                bestDistance = distance;
                best = mark;
            }
        }
        return best;
    }

    /** 目标在准星上的命中半径：实体取碰撞箱半宽，方块取方块半宽，各自再加一点容差。 */
    private static double crosshairRadius(StarMark mark, @Nullable Entity entity)
    {
        double halfSize = entity != null ? entity.getBbWidth() * 0.5D : 0.5D;
        return halfSize + CROSSHAIR_TOLERANCE;
    }

    /**
     * 点到视线的判定。
     *
     * @return 命中时返回沿视线的投影距离（越小越近）；在背后或垂距超出容差时返回 {@code -1}
     */
    private static double crosshairDistance(Vec3 eye, Vec3 dir, Vec3 anchor, double radius)
    {
        Vec3 toTarget = anchor.subtract(eye);
        double along = toTarget.dot(dir);
        if (along <= 0.0D)
        {
            return -1.0D;
        }
        double perpendicularSqr = toTarget.lengthSqr() - along * along;
        return perpendicularSqr <= radius * radius ? along : -1.0D;
    }

    private static boolean containsTarget(List<ResolvedMark> resolved, StarMark mark)
    {
        for (ResolvedMark candidate : resolved)
        {
            if (candidate.mark().sameTargetAs(mark))
            {
                return true;
            }
        }
        return false;
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

    /** 星域外环的点数下限（也是「基础档位半径」下的点数，即最初的手感）。 */
    private static final int RING_MIN_POINTS = 18;

    /** 以中心为心的水平星色粒子环（禁行域用）。半径由调用方给出（现取「结界尺寸调节器」的档位）。 */
    public static void spawnRing(ServerLevel level, Vec3 center, double radius, StarMarkType type)
    {
        /*
         * 点数随半径等比增长，保持弧上间距恒定：半径托管给「结界尺寸调节器」后最大可到 64 格，
         * 若固定 18 点会散成十几个孤立光点。基础档位（半径 8）仍是 18 点，观感与最初一致。
         */
        double baseRadius = Math.max(1.0D, BarrierTechniqueServerLogic.staticGetDefaultRadius());
        int points = Math.max(RING_MIN_POINTS, (int) Math.round(radius * RING_MIN_POINTS / baseRadius));
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

    /**
     * 引爆的爆发表现：星色尘土向外扩散 + 一圈末影火花。
     * <p>
     * 刻意不用原版爆炸粒子与爆炸音效——引爆不造成任何伤害，画面不该给出「挨了一下爆炸」的误导。
     * 粒子数随半径等比（与 {@link #spawnRing} 同理），免得把作用半径调大后爆心看着只是「多了一小团雾」。
     */
    public static void spawnBurst(ServerLevel level, Vec3 center, StarMarkType type, double radius)
    {
        int count = Math.max(RING_MIN_POINTS, (int) Math.round(radius * 8.0D));
        level.sendParticles(
                dust(type, 1.0F),
                center.x, center.y, center.z,
                count, radius * 0.35D, radius * 0.35D, radius * 0.35D, 0.05D);
        level.sendParticles(
                ParticleTypes.END_ROD,
                center.x, center.y, center.z,
                Math.max(6, count / 2), radius * 0.25D, radius * 0.25D, radius * 0.25D, 0.12D);
    }

    private static DustParticleOptions dust(StarMarkType type, float scale)
    {
        return new DustParticleOptions(new Vector3f(type.red(), type.green(), type.blue()), scale);
    }
}
