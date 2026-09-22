package com.darkness.jujutsukaisenzone.technique.skill;

import com.darkness.jujutsukaisenzone.player.PlayerStarMarkData;
import com.darkness.jujutsukaisenzone.player.StarMarkType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 星间飞行的三态标记规则，只在服务端结算。
 * <p>
 * <b>同标记</b>：互相吸引（常态向质心靠拢；牵引期间改为向施术者靠拢）。<br>
 * <b>相邻</b>：无效果。<br>
 * <b>跨序</b>：仅当「禁行」星域生效时，域内产生斥力，越近越强。
 * <p>
 * 三条实现纪律：
 * <ul>
 *   <li>力度一律用**增量**速度（{@code setDeltaMovement(dm.add(impulse))}），不覆盖既有速度，
 *       否则会和 EpicFight 的移动 / 闪避互相吃掉。</li>
 *   <li>同标记用**质心**而不是两两成对，天然收敛、不会自激。</li>
 *   <li>方块是静态锚点，只对「会动的一侧」施力；方块之间不参与判定。</li>
 * </ul>
 */
public final class StarMarkRules
{
    /** 标记的最大存活距离（格）；超出即解除。 */
    public static final double MARK_RANGE = 32.0D;

    /** 牵引持续时间（5 秒）。 */
    public static final int PULL_DURATION_TICKS = 100;
    /** 牵引的最大作用距离（格）。 */
    public static final double PULL_RANGE = 24.0D;
    /** 禁行持续时间（15 秒）。 */
    public static final int GATE_DURATION_TICKS = 300;
    /** 禁行星域半径（格）。 */
    public static final double GATE_RADIUS = 8.0D;

    /** 同标记常态吸引的有效半径（格）。 */
    private static final double ATTRACT_RANGE = 16.0D;
    /** 同标记常态吸引的每 tick 增量速度。 */
    private static final double ATTRACT_IMPULSE = 0.010D;
    /** 牵引的每 tick 增量速度。 */
    private static final double PULL_IMPULSE = 0.070D;
    /** 跨序斥力的作用距离（格）。 */
    private static final double REPULSION_RANGE = 6.0D;
    /** 跨序斥力的每 tick 增量速度。 */
    private static final double REPULSION_IMPULSE = 0.075D;
    /** 最小间隔（格）：小于它则不再吸引，斥力直接翻倍（最小距离闸，防穿模）。 */
    private static final double MIN_SEPARATION = 1.5D;
    /** 单 tick 参与力学计算的最大标记数（按与施术者的距离取近者）。 */
    private static final int MAX_SIMULATED = 16;

    /** 竖直方向的力度衰减，避免把目标顶上天。 */
    private static final double ATTRACT_VERTICAL_SCALE = 0.35D;
    private static final double REPULSION_VERTICAL_SCALE = 0.5D;

    private static final String TAG_PULL_UNTIL = "jujutsukaisenzone_love_rendezvous_pull_until";
    private static final String TAG_GATE_UNTIL = "jujutsukaisenzone_love_rendezvous_gate_until";

    private StarMarkRules()
    {
    }

    // ======================== 技能状态 ========================

    /** 开启牵引：把当前标记的所有目标拉向施术者。 */
    public static void activatePull(ServerPlayer player)
    {
        StarMarkType selected = PlayerStarMarkData.getSelected(player);
        int targets = PlayerStarMarkData.countOfType(player, selected);
        player.getPersistentData().putLong(TAG_PULL_UNTIL, gameTime(player) + PULL_DURATION_TICKS);
        player.displayClientMessage(
                Component.literal("★牵引 · 以 " + selected.displayName() + " 为引，"
                        + targets + " 个目标正被拉向你（" + (PULL_DURATION_TICKS / 20) + " 秒）"),
                true);
    }

    /** 展开禁行星域：域内跨序之星无法靠近。 */
    public static void activateGate(ServerPlayer player)
    {
        player.getPersistentData().putLong(TAG_GATE_UNTIL, gameTime(player) + GATE_DURATION_TICKS);
        player.displayClientMessage(
                Component.literal("★禁行 · 星域展开，半径 " + (int) GATE_RADIUS
                        + " 格内跨序之星无法靠近（" + (GATE_DURATION_TICKS / 20) + " 秒）"),
                true);
    }

    public static boolean isPullActive(ServerPlayer player)
    {
        return player.getPersistentData().getLong(TAG_PULL_UNTIL) > gameTime(player);
    }

    public static boolean isGateActive(ServerPlayer player)
    {
        return player.getPersistentData().getLong(TAG_GATE_UNTIL) > gameTime(player);
    }

    // ======================== 每 tick 力学 ========================

    /**
     * 对该玩家身上全部有效标记结算一次力学，并把结果写成增量速度。
     *
     * @param marks {@link StarMarkSkillLogic#resolveAndRender(ServerPlayer)} 的返回值
     */
    public static void apply(ServerPlayer owner, List<StarMarkSkillLogic.ResolvedMark> marks)
    {
        if (marks.isEmpty())
        {
            return;
        }

        ServerLevel level = owner.serverLevel();
        long now = level.getGameTime();
        boolean render = now % StarMarkSkillLogic.RENDER_INTERVAL_TICKS == 0L;
        StarMarkType selected = PlayerStarMarkData.getSelected(owner);
        boolean pulling = isPullActive(owner);
        boolean gating = isGateActive(owner);
        Vec3 ownerPos = owner.position();

        /* 参与计算的目标数截断：按与施术者的距离取近者 */
        List<StarMarkSkillLogic.ResolvedMark> simulated = marks;
        if (marks.size() > MAX_SIMULATED)
        {
            simulated = new ArrayList<>(marks);
            simulated.sort(Comparator.comparingDouble(mark -> mark.position().distanceToSqr(ownerPos)));
            simulated = simulated.subList(0, MAX_SIMULATED);
        }

        /* 同标记：按质心吸引；牵引期间改为向施术者吸引 */
        Map<StarMarkType, List<StarMarkSkillLogic.ResolvedMark>> groups = new EnumMap<>(StarMarkType.class);
        for (StarMarkSkillLogic.ResolvedMark mark : simulated)
        {
            groups.computeIfAbsent(mark.mark().type(), key -> new ArrayList<>()).add(mark);
        }

        for (Map.Entry<StarMarkType, List<StarMarkSkillLogic.ResolvedMark>> entry : groups.entrySet())
        {
            List<StarMarkSkillLogic.ResolvedMark> group = entry.getValue();
            if (group.size() < 2)
            {
                continue;
            }

            boolean pullThisGroup = pulling && entry.getKey() == selected;
            Vec3 attractor = pullThisGroup ? ownerPos.add(0.0D, 1.0D, 0.0D) : centroid(group);
            double strength = pullThisGroup ? PULL_IMPULSE : ATTRACT_IMPULSE;
            double range = pullThisGroup ? PULL_RANGE : ATTRACT_RANGE;
            double verticalScale = pullThisGroup ? 1.0D : ATTRACT_VERTICAL_SCALE;

            for (StarMarkSkillLogic.ResolvedMark member : group)
            {
                Vec3 pos = member.position();
                double distance = pos.distanceTo(attractor);
                if (distance < 1.0E-4D || distance > range)
                {
                    continue;
                }
                /* 最小距离闸：已经贴上就不再吸，避免互相穿模 */
                if (!pullThisGroup && distance < MIN_SEPARATION)
                {
                    continue;
                }

                double falloff = 1.0D - Math.min(1.0D, distance / range);
                Vec3 dir = attractor.subtract(pos).scale(1.0D / distance);
                Vec3 impulse = dir.scale(strength * (0.35D + falloff))
                        .multiply(1.0D, verticalScale, 1.0D);
                push(member.entity(), impulse, owner);

                if (render && pullThisGroup)
                {
                    StarMarkSkillLogic.spawnLine(level, pos, attractor, entry.getKey());
                }
            }
        }

        /* 跨序：只在禁行星域生效 */
        if (gating)
        {
            double gateRadiusSqr = GATE_RADIUS * GATE_RADIUS;
            for (int i = 0; i < simulated.size(); i++)
            {
                StarMarkSkillLogic.ResolvedMark a = simulated.get(i);
                for (int j = i + 1; j < simulated.size(); j++)
                {
                    StarMarkSkillLogic.ResolvedMark b = simulated.get(j);
                    if (!a.mark().type().isNonAdjacentTo(b.mark().type()))
                    {
                        continue;
                    }
                    /* 方块之间互相判定毫无意义 */
                    if (!a.mark().mobile() && !b.mark().mobile())
                    {
                        continue;
                    }

                    Vec3 posA = a.position();
                    Vec3 posB = b.position();
                    if (posA.distanceToSqr(ownerPos) > gateRadiusSqr
                            && posB.distanceToSqr(ownerPos) > gateRadiusSqr)
                    {
                        continue;
                    }

                    double distance = posA.distanceTo(posB);
                    if (distance > REPULSION_RANGE)
                    {
                        continue;
                    }

                    Vec3 dir;
                    if (distance < 1.0E-4D)
                    {
                        dir = new Vec3(1.0D, 0.0D, 0.0D);
                        distance = 1.0E-4D;
                    }
                    else
                    {
                        dir = posA.subtract(posB).scale(1.0D / distance);
                    }

                    double falloff = 1.0D - Math.min(1.0D, distance / REPULSION_RANGE);
                    double strength = REPULSION_IMPULSE * (0.30D + falloff);
                    if (distance < MIN_SEPARATION)
                    {
                        strength *= 2.0D;
                    }

                    Vec3 impulse = dir.scale(strength).multiply(1.0D, REPULSION_VERTICAL_SCALE, 1.0D);
                    if (a.mark().mobile())
                    {
                        push(a.entity(), impulse, owner);
                    }
                    if (b.mark().mobile())
                    {
                        push(b.entity(), impulse.scale(-1.0D), owner);
                    }
                }
            }

            if (render)
            {
                StarMarkSkillLogic.spawnRing(level, ownerPos, GATE_RADIUS, selected);
            }
        }
    }

    // ======================== 内部 ========================

    private static Vec3 centroid(List<StarMarkSkillLogic.ResolvedMark> group)
    {
        double x = 0.0D;
        double y = 0.0D;
        double z = 0.0D;
        for (StarMarkSkillLogic.ResolvedMark member : group)
        {
            x += member.position().x;
            y += member.position().y;
            z += member.position().z;
        }
        double size = group.size();
        return new Vec3(x / size, y / size, z / size);
    }

    /** 以增量速度推动目标；方块锚点（entity == null）与旁观 / 创造模式玩家跳过。 */
    private static void push(@Nullable Entity entity, Vec3 impulse, ServerPlayer owner)
    {
        if (entity == null || entity == owner || entity.isRemoved() || !entity.isAlive())
        {
            return;
        }
        if (entity instanceof Player player && (player.isSpectator() || player.isCreative()))
        {
            return;
        }

        entity.setDeltaMovement(entity.getDeltaMovement().add(impulse));
        entity.hurtMarked = true;
        entity.hasImpulse = true;
        /* 被拖拽期间不吃坠落伤害 */
        entity.fallDistance = 0.0F;
    }

    private static long gameTime(ServerPlayer player)
    {
        return player.level().getGameTime();
    }
}
