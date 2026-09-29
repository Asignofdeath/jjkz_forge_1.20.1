package com.darkness.jujutsukaisenzone.technique.skill;

import com.darkness.jujutsukaisenzone.compat.EpicFightCompat;
import com.darkness.jujutsukaisenzone.player.PlayerStarMarkData;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import com.darkness.jujutsukaisenzone.player.StarMark;
import com.darkness.jujutsukaisenzone.player.StarMarkType;
import com.darkness.jujutsukaisenzone.technique.TechniqueDamageSources;
import com.darkness.jujutsukaisenzone.technique.skillset.SkillDamageType;
import com.darkness.jujutsukaisenzone.technique.development.SkillUnlockLogic;
import com.darkness.jujutsukaisenzone.technique.skillset.LoveRendezvousTechniqueSkillSet;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 「飞行，穿越」——星间飞行的终结技：一次高速追踪的折线穿越。
 * <p>
 * <b>前置条件两条</b>（缺一不可，全部在扣咒力<b>之前</b>校验，见
 * {@code LoveRendezvousTechniqueSkillSet#useSkill}）：
 * <ol>
 *   <li>施术者<b>自己身上有一颗标记</b>——它是本次相会的「出发之星」，也是最后判定弹飞的基准；</li>
 *   <li>目标身上有一颗<b>星位叠满</b>（{@link StarMark#MAX_STACKS} 级）的实体标记，
 *       且施术者与它相距不超过 {@link #CAST_RANGE} 格。</li>
 * </ol>
 * 注意这里<b>不要求两颗星跨序</b>——目标身上那颗只负责「够格挨这一脚」，弹不弹飞是打完才决定的，见下。
 * <p>
 * <b>一次施放 = 五个阶段</b>：
 * <ol>
 *   <li><b>取快照</b>——若此刻正站在自己的星域里（{@link StarMarkRules#insideOwnGate}），
 *       先把「这一轮解锁了哪些星位」读出来，见下；</li>
 *   <li><b>收域</b>——立刻关掉施术者自己的全部星域（跟随 / 固定一并解除）。本技能<b>不需要星域</b>，
 *       星域只是它的素材来源，出手前一律清场；</li>
 *   <li><b>依次穿越</b>（仅当取到快照）——按星序扫过快照里解锁的每一颗星位，每位取<b>最近的</b>已标记目标
 *       作为一段路标（方块 / 实体都可以，够不着的位直接跳过，找到几颗算几颗），逐段飞过去。
 *       每穿过一颗，这一脚的踢击伤害 +{@link #PASS_BONUS_PER_STAR}，封顶 +{@link #PASS_BONUS_CAP}；</li>
 *   <li><b>追踪</b>——每一段都以高速<b>平移</b>推进，每 tick 按锚点的<b>当前位置</b>重算方向与步长，
 *       所以目标跑动时这一脚会拐着弯追上去；</li>
 *   <li><b>抵达结算</b>（{@link #arrive}）——引爆旧印记 → 冲撞伤害（受穿越加成放大）+ 补上「当前选中」印记
 *       → 看两颗星是否互为克制（{@link #isCounter}），是才把目标按克制弹飞
 *       （{@link StarMarkRules#flightKick}，<b>永远由施术者弹飞目标</b>，不看双方生命值）。</li>
 * </ol>
 * <p>
 * <b>为什么必须取快照</b>：星位的解锁进度（通行证）存在<b>那一轮星域</b>里，而本技能必然关域——
 * {@link StarMarkRules#deactivateGate} 会把整份记录删掉，所以位图只能在关域<b>之前</b>读
 * （{@link StarMarkRules#unlockMaskOf}）。这就是「一份独立的记录」：它只活在 {@link Flight} 里，
 * 随这次飞行一起消失，不落盘。读的是<b>自身标记</b>那一份，与「星星标记」状态提示里那句
 * 「自身标记本轮已解锁 ★…」同源，玩家自己就能核对这一趟会穿几颗。
 * <p>
 * <b>为什么弹飞是条件触发</b>：这一脚踢出去的那颗新印记，正是目标接下来当「球」的身份——
 * 同星制导与撞击伤害都要求它留在施术者的标记池里（{@link StarMarkRules#apply} 的成员池就是这张表）。
 * 引爆必然吃掉旧标记，所以补打的这一颗既是新印记、也是重新获得的「球籍」。
 * 弹不弹则取决于两颗星的顺序关系（{@link #isCounter}）：玩家可以靠切换当前标记来决定「这一脚要不要踢飞」，
 * 而不是被前置条件锁死。
 * <p>
 * <b>为什么是「平移」而不是瞬移</b>：瞬移只有起点与终点两帧，撞上的一瞬间没有任何铺垫；
 * 这里每 tick 推进一小段，方向随锚点实时修正，于是「冲过去 → 追上 → 撞上」是一个连续的过程，
 * 起手动画正好铺满整段飞行。位移全部由服务端结算（{@link #tick}），客户端不参与。
 * <p>
 * <b>飞行状态是内存态</b>（{@link #FLIGHTS}，按玩家 UUID 索引），不落盘：它描述的是「正在进行中的一次动画」，
 * 而不是玩家属性。因此死亡、换维度、掉线都不会留下残留——每一段重挂的 {@code deadline} 是最后的兜底。
 */
public final class StarFlightSkillLogic
{
    /** 施术者与目标的最大距离（格）：超出就连「相会」也无从谈起。 */
    public static final double CAST_RANGE = 20.0D;

    /** 撞上目标那一刻的冲撞伤害（点）。引爆的爆心伤害另计，见 {@link StarMarkDetonation#DETONATION_DAMAGE}。 */
    public static final float RAM_DAMAGE = 12.0F;

    /**
     * 「依次穿越」每穿过一颗星位给踢击伤害的加成（比例）。
     * <p>
     * 加的是<b>最后那一脚</b>（{@link #RAM_DAMAGE}），不是路径伤害——路径伤害是沿途的顺带一下，
     * 与穿了几颗无关。
     */
    public static final double PASS_BONUS_PER_STAR = 0.40D;

    /**
     * 上述加成的封顶（比例）。
     * <p>
     * 0.40 × 5 = 2.00：星位一共只有 {@link StarMarkType#count()} 颗，所以这个上限天然等于
     * 「把这一轮解锁的星位全穿一遍」。真触顶时踢击是基础值的 3 倍。
     */
    public static final double PASS_BONUS_CAP = 2.00D;

    /** 飞行途中撞到挡路实体时结算的伤害（点）。同一个实体整趟只挨一次。 */
    public static final float PATH_DAMAGE = 6.0F;

    /** 路径击退的力度：刻意小于弹射初速，这是「绕路撞人」，不是「踢飞」。 */
    private static final double PATH_KNOCK = 0.80D;

    /** 路径撞击的判定半径（格）：在扫掠包围盒之外再向外放宽这么多。 */
    private static final double PATH_RADIUS = 1.5D;

    /** 到达判定（格）：锚点距离进到这里就结算，不必真贴到零距离。 */
    private static final double ARRIVE_DISTANCE = 1.6D;

    /**
     * 一次穿越的飞行时长（tick）：按距离在 {@link #MIN_FLIGHT_TICKS} ~ {@link #MAX_FLIGHT_TICKS} 之间取值。
     * <p>
     * ⚠️ <b>硬约束</b>：{@link #MAX_FLIGHT_TICKS} 必须 ≥ {@link #CAST_RANGE} / {@link #TICKS_PER_BLOCK}
     * （当前 20 / 1.25 = 16）。一旦上限偏低，满射程时反推出的步长就会大于 {@link #ARRIVE_DISTANCE}：
     * 位移本身不会越过目标（{@link #tick} 用 {@code Math.min(step, distance)} 收尾），但每一跳都跨过
     * 大半个身位，「逼近 → 撞上」的连续感就没了，观感退化成两三段闪现。改射程时这里要一起改。
     */
    private static final int MIN_FLIGHT_TICKS = 4;
    private static final int MAX_FLIGHT_TICKS = 16;

    /**
     * 一个 tick 期望飞过的距离（格），用来把「距离」换算成「时长」。
     * <p>
     * 取 1.25：20 格满射程飞 16 tick（0.8 秒），即约 25 格/秒——比弹射初速（1.4 格/tick）略缓，
     * 因为这里的位移是<b>持续</b>的而不是一次抛出，同样的观感需要更小的单 tick 步长。
     * <p>
     * 它同时决定<b>单步的连续性</b>：这个值要留在 {@link #ARRIVE_DISTANCE} 之下，位移才是贴着目标推进的
     * 滑行，而不是大跳着闪过去。射程变远时优先抬 {@link #MAX_FLIGHT_TICKS}，不要抬这个值。
     */
    private static final double TICKS_PER_BLOCK = 1.25D;

    /**
     * 飞行状态的兜底寿命（tick）：到期无论到没到都强制收尾。
     * <p>
     * 正常飞行最多 {@link #MAX_FLIGHT_TICKS}，取 32 留了一倍余量。它同时也是「玩家掉线 / 服务端卡顿」
     * 这类异常的唯一出口——{@link #FLIGHTS} 不落盘，所以除它以外没有别的机会把残留状态抹掉。
     */
    private static final int FLIGHT_TIMEOUT_TICKS = 32;

    /** 粒子尾迹的生成间隔（tick）：每 tick 一个。 */
    private static final float TRAIL_SCALE = 0.4F;

    /** 进行中的穿越，按施术者 UUID 索引。 */
    private static final Map<UUID, Flight> FLIGHTS = new HashMap<>();

    private StarFlightSkillLogic()
    {
    }

    /** 一次穿越锁定的目标：目标身上那颗满级标记，以及它对应的实体。 */
    public record Target(StarMark mark, LivingEntity entity)
    {
    }

    /**
     * 飞行折线上的一段。
     * <p>
     * 段目标可以是方块或实体标记——「依次穿越」吃的正是这两类，
     * 所以这里存 {@link StarMark} 加一个可空实体：方块只有坐标，实体每 tick 现查位置
     * （见 {@link #legAnchor(ServerLevel, Leg, boolean)}）。折线的最后一段恒为 {@link #pickTarget} 选出的真实目标。
     */
    private record Leg(StarMark mark, @Nullable Entity entity)
    {
    }

    /**
     * 进行中的一次穿越。
     * <p>
     * 只存「接下来怎么走」需要的量：折线（{@link #legs}）、当前走到第几段（{@link #index}）、
     * 单 tick 步长（把这一段距离均摊到 {@link #remaining} 个 tick 上），以及兜底寿命。
     * <p>
     * {@link #passes} 是已经穿过的星位数，只用来算踢击加成；{@link #rammed} 是路径撞击的去重表。
     * 两者都只描述「这一次飞行」，所以随 {@link Flight} 一起躺在内存里、不落盘。
     */
    private static final class Flight
    {
        private final List<Leg> legs;
        /** 最终目标的实体 id：路径撞击要放过它，它由 {@link #arrive} 单独结算。 */
        private final int finalEntityId;
        /** 已被路径撞过的实体 id：步长小、半径大，同一个实体注定连着几 tick 都落在判定盒里。 */
        private final Set<Integer> rammed = new HashSet<>();
        private int index;
        private int passes;
        private int remaining;
        private double step;
        private long deadline;

        private Flight(List<Leg> legs, int finalEntityId, long now)
        {
            this.legs = legs;
            this.finalEntityId = finalEntityId;
            this.deadline = now + FLIGHT_TIMEOUT_TICKS;
        }
    }

    // ======================== 前置条件与目标选择 ========================

    /**
     * 两颗星是否<b>互为克制之星</b>——即星序上相隔至少一颗（{@code |序号差| >= 2}）。
     * <p>
     * 判据复用星域的解锁位图：{@code a} 的初始位图是「自己 + 顺序相邻的两颗」，
     * 所以 {@code b} 不在其中 ⟺ 两颗星隔了至少一颗。于是「克制」在这里与「禁行域里必须逐级经过
     * 才能接近」是同一件事，不需要另建一张克制表。
     * <p>
     * 反过来说：<b>同星与相邻星都不算克制</b>，它们本来就能自由靠近，弹不出也该弹不开。
     */
    public static boolean isCounter(@Nullable StarMarkType a, @Nullable StarMarkType b)
    {
        return a != null && b != null && !b.isUnlockedIn(a.initialUnlockMask());
    }

    /** 施术者是否正在穿越途中（穿越期间不允许再次施放）。 */
    public static boolean isFlying(ServerPlayer player)
    {
        return FLIGHTS.containsKey(player.getUUID());
    }

    /**
     * 选取本次穿越的目标。
     * <p>
     * 候选 = 施术者自己持有的、<b>实体类型</b>的标记，逐个过两道闸：星位叠满 →
     * 距离不超过 {@link #CAST_RANGE}。全部通过之后再择优：<b>准星命中的优先</b>（与清除标记同一套
     * 准星判定，允许穿墙、不限距离），其次取最近的。
     * <p>
     * 这里<b>不筛克制关系</b>——目标原本挂的是哪颗星无所谓，弹不弹飞由 {@link #arrive} 补上
     * 新印记之后才判（见类注释）。
     * <p>
     * 不满足时返回 {@code null}，并在这里把<b>具体是哪一道闸没过</b>报在动作栏里——调用方据此在扣费前
     * 提前 return，所以失败永远不消耗咒力、不上冷却。
     */
    @Nullable
    public static Target pickTarget(ServerPlayer player)
    {
        if (isFlying(player))
        {
            notifyPlayer(player, "★飞行，穿越 · 你正在穿越途中");
            return null;
        }

        /* 自身标记是最后判定弹飞的基准；没有它这一脚就无从谈起。它是施术者身上任一颗星，与当前选中无关 */
        if (PlayerStarMarkData.findTarget(player, StarMarkSkillLogic.markOfEntity(player, player)) == null)
        {
            notifyPlayer(player, "★飞行，穿越 · 需要先有一颗属于自己的标记（Shift + 技能键打在自己身上）");
            return null;
        }

        List<StarMarkSkillLogic.ResolvedMark> marks = StarMarkSkillLogic.resolveAll(player);
        StarMark crosshair = StarMarkSkillLogic.pickMarkedTarget(player);
        long now = player.level().getGameTime();
        Vec3 origin = player.position();

        /* 留一个计数：最后没选出目标时，靠它区分「没有满级的」与「有、但都在射程外」 */
        int fullStacks = 0;
        Target best = null;
        double bestDistance = Double.MAX_VALUE;
        boolean bestIsCrosshair = false;

        for (StarMarkSkillLogic.ResolvedMark resolved : marks)
        {
            Entity entity = resolved.entity();
            /* 方块锚点弹不动、非 LivingEntity 都不是「相会」的对象；自己也不是 */
            if (!(entity instanceof LivingEntity living) || entity == player || !living.isAlive())
            {
                continue;
            }
            StarMark mark = resolved.mark();
            if (StarMarkRules.decayedStacks(mark, now) < StarMark.MAX_STACKS)
            {
                continue;
            }
            fullStacks++;

            double distance = origin.distanceTo(resolved.position());
            if (distance > CAST_RANGE)
            {
                continue;
            }

            boolean crosshairHit = crosshair != null && crosshair.sameTargetAs(mark);
            if (best != null)
            {
                /* 准星命中的目标压过一切；同优先级时取更近的那个 */
                if (bestIsCrosshair && !crosshairHit)
                {
                    continue;
                }
                if (bestIsCrosshair == crosshairHit && distance >= bestDistance)
                {
                    continue;
                }
            }
            best = new Target(mark, living);
            bestDistance = distance;
            bestIsCrosshair = crosshairHit;
        }

        if (best != null)
        {
            return best;
        }
        /* 报原因：先说最靠前的那一道，玩家才知道该补什么 */
        if (fullStacks == 0)
        {
            notifyPlayer(player, "★飞行，穿越 · 没有星位叠满（" + StarMark.MAX_STACKS + " 级）的实体标记");
        }
        else
        {
            notifyPlayer(player, "★飞行，穿越 · 星位叠满的目标都在 " + (int) CAST_RANGE + " 格之外");
        }
        return null;
    }

    // ======================== 释放 ========================

    /**
     * 开始一次穿越：取快照 → 收域 → 排折线 → 播起手动画 → 把飞行状态挂上去。
     * <p>
     * 咒力 / 冷却 / 解锁 / 等级由调用方在 {@link #pickTarget} 通过之后按技能经济结算完毕，
     * 本方法只管演出与状态——因此它<b>只可能发生在扣费之后</b>，这是 {@code useSkill} 里的先后顺序，
     * 不要在这里补前置校验（那样失败就已经白扣一次了）。
     */
    public static void cast(ServerPlayer player, Target target)
    {
        LivingEntity entity = target.entity();
        if (!entity.isAlive() || !entity.level().dimension().equals(player.level().dimension()))
        {
            notifyPlayer(player, "★飞行，穿越 · 目标已失");
            return;
        }

        /*
         * ① 快照必须在关域之前取：星位的解锁进度（通行证）存在那一轮星域里，deactivateGate 会把整份
         * 记录删掉，而本技能必然关域——晚一步就永远读不到了。快照只活在 Flight 里，不落盘。
         * 读的是自身标记那一份，与「星星标记」状态提示里那句「自身标记本轮已解锁 ★…」是同一份数据。
         * <p>
         * 「在星间穿梭」是这条快照的<b>唯一消费者</b>：未解锁时干脆不读（mask 恒 0），于是折线不排、
         * 这一脚退化成直取目标的单段突进。门控刻意放在<b>读取口</b>而不是排折线那一行——这样
         * 「没有折线」只有一个成因，也不必白查一次星域。
         */
        int mask = 0;
        if (isChainUnlocked(player) && StarMarkRules.insideOwnGate(player))
        {
            StarMark self = PlayerStarMarkData.findTarget(player,
                    StarMarkSkillLogic.markOfEntity(player, player));
            if (self != null)
            {
                mask = StarMarkRules.unlockMaskOf(player, self);
            }
        }

        /* ② 「无需星域」：本技能与域无关，出手前把施术者自己的域全收掉，避免穿越途中还在结算域内斥力 */
        StarMarkRules.deactivateGate(player);

        /* ③ 折线：先依次穿越已解锁的星位（找到几颗算几颗），最后一段才是真实目标 */
        List<Leg> legs = new ArrayList<>();
        if (mask != 0)
        {
            legs.addAll(planChain(player, mask, target.mark()));
        }
        legs.add(new Leg(target.mark(), entity));

        Flight flight = new Flight(legs, entity.getId(), player.level().getGameTime());
        FLIGHTS.put(player.getUUID(), flight);
        beginLeg(player, flight, player.position());

        EpicFightCompat.playFlyingTurnAnimation(player);
        String name = entity.getName().getString();
        notifyPlayer(player, legs.size() > 1
                ? "★飞行，穿越 · 依次穿越 " + (legs.size() - 1) + " 颗星，最后向 " + name + " 飞去"
                : "★飞行，穿越 · 向 " + name + " 飞去");
    }

    /**
     * 排定「依次穿越」的星位顺序。
     * <p>
     * 按星序从 {@code ordinal 0} 一路走到<b>位图里解锁的最大星位</b>（位图本身是逐级推进的，
     * 所以逐位判一遍就是「从第一颗走到解锁的最远那一颗」），每一位取<b>最近的</b>已标记目标作为一段；
     * 某一位找不到合用的目标就跳过它——找到几颗算几颗。
     * <p>
     * 目标只要求「已被标记」，<b>不</b>要求星位叠满（满级是最后那一段的门槛，不是路过星位的门槛），
     * 也不分方块 / 实体：穿越段吃的正是这两类。
     * <p>
     * 三处排除：
     * <ul>
     *   <li><b>施术者自己</b>——自身标记是这套判定的基准，穿自己没有意义；</li>
     *   <li><b>最终目标</b>——它排在最后一段，先穿过去再回头踢很怪；</li>
     *   <li><b>超过 {@link #CAST_RANGE} 的目标</b>——不是「没找到」，而是够不着。这条同时是
     *       {@link #MAX_FLIGHT_TICKS} 那条连续性硬约束的前提：每段都被夹在射程内，单步长才留得住。</li>
     * </ul>
     * 距离按<b>上一段的锚点</b>逐段推进计算，所以整条折线是「就近串起来」，而不是「离起点最近的几颗」。
     */
    private static List<Leg> planChain(ServerPlayer player, int mask, StarMark finalMark)
    {
        List<StarMarkSkillLogic.ResolvedMark> marks = StarMarkSkillLogic.resolveAll(player);
        List<Leg> legs = new ArrayList<>();
        Vec3 cursor = player.position();

        for (int ordinal = 0; ordinal < StarMarkType.count(); ordinal++)
        {
            if ((mask & (1 << ordinal)) == 0)
            {
                continue;
            }
            StarMarkType type = StarMarkType.values()[ordinal];
            StarMarkSkillLogic.ResolvedMark best = null;
            double bestDistance = Double.MAX_VALUE;
            for (StarMarkSkillLogic.ResolvedMark resolved : marks)
            {
                if (resolved.mark().type() != type)
                {
                    continue;
                }
                /* 自身标记与最终目标都不参与穿越；同一目标至多一条标记，所以不会重复串到同一段 */
                if (resolved.entity() == player || resolved.mark().sameTargetAs(finalMark))
                {
                    continue;
                }
                double distance = cursor.distanceTo(resolved.position());
                if (distance > CAST_RANGE || distance >= bestDistance)
                {
                    continue;
                }
                best = resolved;
                bestDistance = distance;
            }
            if (best == null)
            {
                continue;
            }
            legs.add(new Leg(best.mark(), best.entity()));
            cursor = best.position();
        }
        return legs;
    }

    /**
     * 施术者是否已解锁「在星间穿梭」这个<b>机制型技能</b>。
     * <p>
     * 机制型技能不占槽位、没有自己的咒力与冷却，唯一的状态就是那个解锁标记，所以运行时门控只看这一条
     * （技能定义见 {@code LoveRendezvousTechniqueSkillSet#SKILL_STAR_CHAIN}）。术式 id 写死星间飞行——
     * 本类就是它的终结技实现。
     * <p>
     * 与 {@code StarMarkRules#isSlingUnlocked} 同一写法：走 {@link SkillUnlockLogic#isSkillUnlockedById}
     * 而不是自己读 tag，把「节点 -&gt; 技能定义」的解析统一收口在那边，这里就少一份「tag 名写错却没人发现」的机会。
     */
    private static boolean isChainUnlocked(ServerPlayer player)
    {
        return SkillUnlockLogic.isSkillUnlockedById(
                player,
                PlayerTechniqueData.TECHNIQUE_LOVE_RENDEZVOUS,
                LoveRendezvousTechniqueSkillSet.SKILL_STAR_CHAIN);
    }

    /**
     * 每 tick 推进一次穿越；由 {@code StarMarkEvents#onPlayerTick} 调用。
     * <p>
     * 方向与步长<b>每 tick 现算</b>——这就是「带追踪」的全部实现：锚点挪了，下一 tick 的朝向与落点
     * 跟着挪，不需要任何预测。位移走 {@link ServerPlayer#teleportTo(double, double, double)}：
     * 服务端玩家的位置由客户端权威，直接改 {@code deltaMovement} 会被客户端的移动包覆盖掉，
     * 所以「高速平移」只能用每 tick 的位置包来推。
     * <p>
     * 走到一段的尽头就交给 {@link #resolveLeg}：中间段是「穿越」（给加成、接着飞下一段），
     * 末段才是踢击。三种「走不到」的处置各不相同：
     * <ul>
     *   <li><b>段目标失效</b>（方块被挖 / 实体消失）——中间段跳过，末段判中断；</li>
     *   <li><b>跑出两倍射程</b>——同上；</li>
     *   <li><b>这一段飞完了还没贴上锚点</b>——末段照旧就地结算（与单段时代一致），
     *       中间段则算「没穿过去」，跳过、不给加成，这才对得起「穿越」两个字。</li>
     * </ul>
     */
    public static void tick(ServerPlayer player)
    {
        Flight flight = FLIGHTS.get(player.getUUID());
        if (flight == null)
        {
            return;
        }
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (now > flight.deadline || !player.isAlive() || player.isSpectator())
        {
            abort(player, "穿越中断");
            return;
        }

        boolean last = flight.index == flight.legs.size() - 1;
        Leg leg = flight.legs.get(flight.index);
        Vec3 anchor = legAnchor(level, leg, last);
        if (anchor == null)
        {
            if (last)
            {
                abort(player, "目标已失，穿越中断");
            }
            else
            {
                advance(player, flight, "★" + leg.mark().type().starName() + " 已失，跳过");
            }
            return;
        }

        Vec3 from = player.position();
        Vec3 delta = anchor.subtract(from);
        double distance = delta.length();

        if (distance <= ARRIVE_DISTANCE)
        {
            resolveLeg(player, flight, anchor, last);
            return;
        }
        if (distance > CAST_RANGE * 2.0D)
        {
            if (last)
            {
                abort(player, "目标跑得太远，穿越中断");
            }
            else
            {
                advance(player, flight, "★" + leg.mark().type().starName() + " 已跑远，跳过");
            }
            return;
        }
        if (flight.remaining <= 0)
        {
            if (last)
            {
                resolveLeg(player, flight, anchor, true);
            }
            else
            {
                advance(player, flight, "★" + leg.mark().type().starName() + " 未及穿越，跳过");
            }
            return;
        }

        Vec3 next = from.add(delta.scale(Math.min(flight.step, distance) / distance));
        /* 先转头再传送：位置包会把当前朝向一并带过去，观感上就是一路盯着锚点飞 */
        player.lookAt(EntityAnchorArgument.Anchor.EYES, anchor);
        player.teleportTo(next.x, next.y, next.z);
        /* 空中连传送不吃坠落伤害；尾迹用这一段那颗星的星色，与标记火花同源 */
        player.resetFallDistance();
        ramPath(player, flight, from, next);
        StarMarkSkillLogic.spawnSpark(level, player.position().add(0.0D, 1.0D, 0.0D), leg.mark().type(), TRAIL_SCALE);
        flight.remaining--;
    }

    // ======================== 折线的段 ========================

    /**
     * 现算某一段的锚点；段目标已失效时返回 {@code null}（方块被挖 / 区块未加载、实体消失）。
     * <p>
     * <b>末段与穿越段的锚点不是同一个点</b>，这是刻意的：
     * <ul>
     *   <li><b>穿越段</b>瞄 {@link StarMarkSkillLogic#anchorOf}——也就是火花所在的位置（方块正上方 / 实体头顶）。
     *       「穿过那颗星」在视觉上就是穿过火花中心；</li>
     *   <li><b>末段</b>瞄实体的 {@link Entity#position()}，即脚下。这一脚是要贴到身上去踢的，
     *       瞄头顶会让施术者悬在目标上方结算，落点差出两格多——这是从单段时代沿用的手感，不改。</li>
     * </ul>
     */
    @Nullable
    private static Vec3 legAnchor(ServerLevel level, Leg leg, boolean last)
    {
        Entity entity = leg.entity();
        if (entity != null)
        {
            if (entity.isRemoved() || !entity.isAlive())
            {
                return null;
            }
            return last ? entity.position() : StarMarkSkillLogic.anchorOf(leg.mark(), entity);
        }
        BlockPos pos = leg.mark().pos();
        if (!level.isLoaded(pos) || level.getBlockState(pos).isAir())
        {
            return null;
        }
        return StarMarkSkillLogic.anchorOf(leg.mark(), null);
    }

    /**
     * 开始第 {@code flight.index} 段：按「当前位置 → 该段锚点」重算步长与时长，并把兜底寿命重挂一次。
     * <p>
     * 每一段都重挂 {@link #FLIGHT_TIMEOUT_TICKS}，所以多段飞行的总时长上限是「段数 × 该值」——
     * 它既是「这一段追不上」的出口，也是掉线 / 服务端卡顿之后清理残留状态的唯一机会
     * （{@link #FLIGHTS} 不落盘，没有别的机会）。
     */
    private static void beginLeg(ServerPlayer player, Flight flight, Vec3 from)
    {
        Leg leg = flight.legs.get(flight.index);
        boolean last = flight.index == flight.legs.size() - 1;
        Vec3 anchor = legAnchor(player.serverLevel(), leg, last);
        double distance = anchor == null ? 0.0D : from.distanceTo(anchor);
        int ticks = flightTicks(distance);
        flight.remaining = ticks;
        flight.step = distance / ticks;
        flight.deadline = player.level().getGameTime() + FLIGHT_TIMEOUT_TICKS;
    }

    /**
     * 推进到下一段（丢弃当前段）。{@code note} 非空时把「为什么跳过」报在动作栏里。
     * <p>
     * 位置不动——跳过的段只是不作数，不补飞也不补伤害。
     */
    private static void advance(ServerPlayer player, Flight flight, @Nullable String note)
    {
        flight.index++;
        beginLeg(player, flight, player.position());
        if (note != null)
        {
            notifyPlayer(player, note);
        }
    }

    /**
     * 一段走到头时的结算：中间段是「穿越」，末段是踢击。
     * <p>
     * 穿越段<b>不消耗、也不引爆</b>路过的那颗标记：它只是路标，改动的只有最后那一脚的力度。
     * 结算完立刻推进到下一段，位置不动——「穿越」是贴着星擦过去，不是停下来看。
     */
    private static void resolveLeg(ServerPlayer player, Flight flight, Vec3 anchor, boolean last)
    {
        Leg leg = flight.legs.get(flight.index);

        if (last)
        {
            if (leg.entity() instanceof LivingEntity living)
            {
                arrive(player, living, flight.passes);
            }
            else
            {
                abort(player, "目标已失，穿越中断");
            }
            return;
        }

        flight.passes++;
        StarMarkSkillLogic.spawnBurst(player.serverLevel(), anchor, leg.mark().type(), 2.0D);
        notifyPlayer(player, "★穿越 · " + leg.mark().type().displayName()
                + "（第 " + flight.passes + " 颗）踢击 "
                + Math.round(damageScale(flight.passes) * 100.0D) + "%");
        advance(player, flight, null);
    }

    /**
     * 路径撞击：把这一小段位移<b>扫过</b>的实体撞开，并结算 {@link #PATH_DAMAGE}。
     * <p>
     * 用<b>扫掠包围盒</b>（起点 → 终点的 AABB 再向外放宽 {@link #PATH_RADIUS}）而不是「以当前位置为圆心」：
     * 单步 1.25 格虽然没有撞击伤害那种 2.8 格/tick 的隧穿风险，但按点判定会让「擦身而过」变得随机，
     * 扫掠则稳定地把整段路径都算进去。
     * <p>
     * 排除两处：施术者自己（由 {@code getEntities} 的 except 参数挡掉），以及<b>本次飞行的最终目标</b>
     * ——后者由 {@link #arrive} 的踢击单独结算，不该在撞上去的前一 tick 先吃一记路径伤害。
     * 路过的星位目标照打不误（它们只有这一段）。
     * <p>
     * 每实体<b>整趟只结算一次</b>（{@link Flight#rammed}）：步长小、半径大，同一个实体注定连着几 tick
     * 都落在盒子里。
     */
    private static void ramPath(ServerPlayer player, Flight flight, Vec3 from, Vec3 to)
    {
        AABB sweep = new AABB(from, to).inflate(PATH_RADIUS);
        for (Entity entity : player.serverLevel().getEntities(player, sweep,
                victim -> victim.isAlive() && victim.getId() != flight.finalEntityId))
        {
            if (!flight.rammed.add(entity.getId()))
            {
                continue;
            }
            if (entity instanceof LivingEntity living)
            {
                TechniqueDamageSources.deal(living, player, PATH_DAMAGE, SkillDamageType.MARTIAL,
                        LoveRendezvousTechniqueSkillSet.SKILL_STAR_FLIGHT);
            }
            StarMarkRules.flightRam(player, entity, from, PATH_KNOCK);
        }
    }

    /** 踢击伤害倍率：每穿越一颗星位 +{@link #PASS_BONUS_PER_STAR}，封顶 +{@link #PASS_BONUS_CAP}。 */
    private static double damageScale(int passes)
    {
        return 1.0D + Math.min(passes * PASS_BONUS_PER_STAR, PASS_BONUS_CAP);
    }

    /**
     * 抵达结算：踢击演出 → 引爆旧印记 → 冲撞伤害 + 补上当前选中印记 → 条件弹飞。
     * <p>
     * <b>顺序就是这条链的全部</b>，四步都不能换位：
     * <ol>
     *   <li>爆心必须在引爆<b>之前</b>取——{@link StarMarkDetonation#detonate} 一进去就消耗掉那条标记；</li>
     *   <li>引爆必须排在补印记<b>之前</b>——旧印记先清场，{@link #applySelectedMark} 打上的才是干净的
     *       1 级新印记（否则会变成「在旧印记上叠一层」）；</li>
     *   <li>补印记必须排在弹飞<b>之前</b>——弹飞要先知道新印记与自身标记的星序关系
     *       （{@link #isCounter}），这正是「这一脚踢不踢得动」的判据；</li>
     *   <li>弹飞必须排在引爆<b>之后</b>——它盖的 {@code TAG_BURST_UNTIL} 只管「球」的身份，
     *       而目标当球的前提是身上得有标记，也就是第 2 步补上的那一颗。</li>
     * </ol>
     * 顺带一提：补上的新印记把被踢飞的目标<b>重新送回了施术者的标记池</b>
     * （{@link StarMarkRules#apply} 的成员池就是这张表），所以它飞出去之后仍然是「一颗球」——
     * 会被同星制导，撞上别的标记照常结算撞击伤害。
     *
     * @param passes 本次飞行已经穿过的星位数，只用来放大冲撞那一下（{@link #damageScale(int)}）
     */
    private static void arrive(ServerPlayer player, LivingEntity target, int passes)
    {
        FLIGHTS.remove(player.getUUID());
        EpicFightCompat.stopFlyingTurnAnimation(player);
        EpicFightCompat.playFlyingTurnKickAnimation(player);
        /* 位置每 tick 被传送覆盖，速度分量可能残留；停下来时清零，免得落地还滑一段 */
        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();

        /* 穿越加成只作用于冲撞这一脚；引爆的爆心伤害是标记自己的账，与飞了几段无关 */
        float damage = (float) (RAM_DAMAGE * damageScale(passes));
        String name = target.getName().getString();
        StarMark mark = PlayerStarMarkData.findTarget(player,
                StarMarkSkillLogic.markOfEntity(player, target));
        if (mark == null)
        {
            /* 飞行途中标记被清掉了：只兑现冲撞这一脚 */
            boolean hit = TechniqueDamageSources.deal(target, player, damage, SkillDamageType.MARTIAL,
                    LoveRendezvousTechniqueSkillSet.SKILL_STAR_FLIGHT);
            notifyPlayer(player, "★飞行，穿越 · " + name
                    + (hit ? " 冲撞 " + Math.round(damage) : " 已被拦下"));
            return;
        }

        /* ① 引爆旧印记：爆心先取，detonate 会把这条标记消耗掉 */
        Vec3 center = StarMarkSkillLogic.anchorOf(mark, target);
        StarMarkDetonation.Result result = StarMarkDetonation.detonate(player, mark, center);

        /* ② 冲撞伤害（按穿越段数放大），并给目标补上「当前选中」的那颗印记 */
        boolean hit = TechniqueDamageSources.deal(target, player, damage, SkillDamageType.MARTIAL,
                LoveRendezvousTechniqueSkillSet.SKILL_STAR_FLIGHT);
        StarMarkType applied = applySelectedMark(player, target);

        /*
         * ③ 弹飞：只看新印记与施术者自身标记是否互为克制。自身标记在飞行途中可能已被清掉，所以现取。
         * 永远是施术者弹飞目标——不看生命值，这是「克制之星」的相会，不是两个球互撞。
         */
        StarMark self = PlayerStarMarkData.findTarget(player, StarMarkSkillLogic.markOfEntity(player, player));
        boolean kicked = self != null && isCounter(self.type(), applied);
        if (kicked)
        {
            StarMarkRules.flightKick(player, target, player.position());
        }

        StringBuilder message = new StringBuilder("★飞行，穿越 · ").append(name).append("：");
        if (passes > 0)
        {
            message.append("穿越 ").append(passes).append(" 颗（")
                    .append(Math.round(damageScale(passes) * 100.0D)).append("%）；");
        }
        message.append(mark.type().displayName()).append(" 引爆");
        if (result.damaged())
        {
            message.append(" 单体 ").append(Math.round(StarMarkDetonation.DETONATION_DAMAGE));
        }
        if (hit)
        {
            message.append("；冲撞 ").append(Math.round(damage));
        }
        message.append("；").append(applied.displayName()).append(" 印记叠加");
        if (self == null)
        {
            message.append("（自身标记已失，未弹飞）");
        }
        else
        {
            message.append(kicked
                    ? "，与 " + self.type().displayName() + " 跨序 · 弹飞"
                    : "，与 " + self.type().displayName() + " 非跨序 · 未弹飞");
        }
        if (result.affected() > 0)
        {
            message.append("；波及 ").append(result.affected());
        }
        if (result.spread() > 0)
        {
            message.append("，传染 ").append(result.spread());
        }
        if (result.evicted() > 0)
        {
            message.append("，顶掉最旧 ").append(result.evicted());
        }
        notifyPlayer(player, message.toString());
    }

    /**
     * 补印记：把施术者<b>当前选中</b>的那颗星打到目标身上，返回打的是哪颗星。
     * <p>
     * <b>不走</b> {@code LoveRendezvousTechniqueSkillSet#tryApplyMark}——那条路会再结算一次咒力
     * 与打标冷却，而这里只是「飞行，穿越」的附加效果，钱在施放技能那一刻就付过了。这与引爆的
     * 「传染」是同一个口径：{@link StarMarkDetonation} 内部也直接走 {@link PlayerStarMarkData#put}，
     * 技能自身产生的标记不经经济。
     * <p>
     * 由于 {@link #arrive} 在这一步之前刚把目标的旧印记引爆消耗掉，落到这里的一定是 1 级新印记
     * （{@link StarMarkSkillLogic#markOfEntity(ServerPlayer, Entity, StarMarkType)}），
     * 不存在「叠在旧印记上」的情形。
     */
    private static StarMarkType applySelectedMark(ServerPlayer player, LivingEntity target)
    {
        StarMarkType selected = PlayerStarMarkData.getSelected(player);
        PlayerStarMarkData.put(player, StarMarkSkillLogic.markOfEntity(player, target, selected));
        return selected;
    }

    /**
     * 中止穿越：清掉状态与起手动画，并告诉施术者发生了什么。
     * <p>
     * 与 {@link #arrive} 的区别是<b>不结算任何效果</b>——没撞上就是没撞上，不补伤害也不补弹射。
     */
    private static void abort(ServerPlayer player, String reason)
    {
        FLIGHTS.remove(player.getUUID());
        EpicFightCompat.stopFlyingTurnAnimation(player);
        notifyPlayer(player, "★飞行，穿越 · " + reason);
    }

    /** 飞行时长：把距离按 {@link #TICKS_PER_BLOCK} 换算成 tick，再夹在上下限之间。 */
    private static int flightTicks(double distance)
    {
        int ticks = (int) Math.round(distance / TICKS_PER_BLOCK);
        return Math.max(MIN_FLIGHT_TICKS, Math.min(MAX_FLIGHT_TICKS, ticks));
    }

    /** 动作栏提示。穿越的每一步都在同一条线上更新，所以一律走 overlay（{@code actionBar=true}）。 */
    private static void notifyPlayer(ServerPlayer player, String text)
    {
        player.displayClientMessage(Component.literal(text), true);
    }
}
