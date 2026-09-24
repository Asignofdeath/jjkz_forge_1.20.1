package com.darkness.jujutsukaisenzone.technique.skill;

import com.darkness.jujutsukaisenzone.compat.EpicFightCompat;
import com.darkness.jujutsukaisenzone.player.PlayerStarMarkData;
import com.darkness.jujutsukaisenzone.player.StarMark;
import com.darkness.jujutsukaisenzone.player.StarMarkType;
import com.darkness.jujutsukaisenzone.technique.TechniqueDamageSources;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 「飞行，穿越」——星间飞行的终结技：一次高速追踪的直线穿越。
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
 * <b>一次施放 = 四件事</b>：
 * <ol>
 *   <li><b>收域</b>——立刻关掉施术者自己的全部星域（跟随 / 固定一并解除）。本技能<b>不需要星域</b>，
 *       星域只是它的素材来源，出手前一律清场；</li>
 *   <li><b>穿越</b>——播放起手动画（{@code flying_turn}）后，以高速<b>平移</b>冲向目标，每 tick
 *       按目标的<b>当前位置</b>重算方向与步长，因此目标跑动时这一脚会拐着弯追上去；</li>
 *   <li><b>引爆</b>——抵达瞬间先引爆目标身上那颗满级标记（{@link StarMarkDetonation#detonate}，
 *       标记被消耗、爆心单体伤害照常结算）。<b>顺序是有意的</b>：旧印记必须先清场，
 *       紧接着补上的那颗才是干净的新印记；</li>
 *   <li><b>冲撞 + 补印记 + 弹飞</b>——播放踢击动画（{@code flying_turn_kick}）并结算
 *       {@link #RAM_DAMAGE} 点冲撞伤害，<b>同时</b>把施术者<b>当前选中</b>的那颗印记打到目标身上
 *       （{@link #applySelectedMark}）；再看这颗新印记与施术者自身那颗标记<b>是否互为克制</b>
 *       （{@link #isCounter}）——是就把它踢飞出去（{@link StarMarkRules#flightKick}，
 *       <b>永远由施术者弹飞目标</b>，不看双方生命值），不是就不弹。</li>
 * </ol>
 * <p>
 * <b>为什么弹飞改成条件触发</b>：这一脚踢出去的那颗新印记，正是目标接下来当「球」的身份——
 * 同星制导与撞击伤害都要求它留在施术者的标记池里（{@link StarMarkRules#apply} 的成员池就是这张表）。
 * 引爆必然吃掉旧标记，所以补打的这一颗既是新印记、也是重新获得的「球籍」。
 * 弹不弹则取决于两颗星的顺序关系（{@link #isCounter}）：玩家可以靠切换当前标记来决定「这一脚要不要踢飞」，
 * 而不是被前置条件锁死。
 * <p>
 * <b>为什么是「平移」而不是瞬移</b>：瞬移只有起点与终点两帧，撞上的一瞬间没有任何铺垫；
 * 这里每 tick 推进一小段，方向随目标实时修正，于是「冲过去 → 追上 → 撞上」是一个连续的过程，
 * 起手动画正好铺满整段飞行。位移全部由服务端结算（{@link #tick}），客户端不参与。
 * <p>
 * <b>飞行状态是内存态</b>（{@link #FLIGHTS}，按玩家 UUID 索引），不落盘：它描述的是「正在进行中的一次动画」，
 * 而不是玩家属性。因此死亡、换维度、掉线都不会留下残留——{@link Flight#deadline} 是最后的兜底。
 */
public final class StarFlightSkillLogic
{
    /** 施术者与目标的最大距离（格）：超出就连「相会」也无从谈起。 */
    public static final double CAST_RANGE = 20.0D;

    /** 撞上目标那一刻的冲撞伤害（点）。引爆的爆心伤害另计，见 {@link StarMarkDetonation#DETONATION_DAMAGE}。 */
    public static final float RAM_DAMAGE = 12.0F;

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

    /** 一次穿越锁定的目标：目标身上那颗满级、跨序、够得着的标记，以及它对应的实体。 */
    public record Target(StarMark mark, LivingEntity entity)
    {
    }

    /**
     * 进行中的一次穿越。
     * <p>
     * 只存「接下来怎么走」需要的量：目标实体 id（每 tick 现查，目标换维度 / 消失即失效）、
     * 单 tick 步长（把总距离均摊到 {@link #remaining} 个 tick 上）、以及兜底寿命。
     */
    private static final class Flight
    {
        private final int targetEntityId;
        private final double step;
        private final StarMarkType star;
        private final long deadline;
        private int remaining;

        private Flight(ServerPlayer player, LivingEntity target, int ticks, double step, StarMarkType star)
        {
            this.targetEntityId = target.getId();
            this.step = step;
            this.remaining = ticks;
            this.star = star;
            this.deadline = player.level().getGameTime() + FLIGHT_TIMEOUT_TICKS;
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
            /* 方块锚点弹不动、掉落物不是 LivingEntity，都不是「相会」的对象；自己也不是 */
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
     * 开始一次穿越：收掉星域、播起手动画、把飞行状态挂上去。
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

        /* 「无需星域」：本技能与域无关，出手前把施术者自己的域全收掉，避免穿越途中还在结算域内的斥力 */
        StarMarkRules.deactivateGate(player);

        double distance = player.position().distanceTo(entity.position());
        int ticks = flightTicks(distance);
        FLIGHTS.put(player.getUUID(),
                new Flight(player, entity, ticks, distance / ticks, target.mark().type()));

        EpicFightCompat.playFlyingTurnAnimation(player);
        notifyPlayer(player, "★飞行，穿越 · 向 " + entity.getName().getString() + " 飞去");
    }

    /**
     * 每 tick 推进一次穿越；由 {@code StarMarkEvents#onPlayerTick} 调用。
     * <p>
     * 方向与步长<b>每 tick 现算</b>——这就是「带追踪」的全部实现：目标挪了，下一 tick 的朝向与落点
     * 跟着挪，不需要任何预测。位移走 {@link ServerPlayer#teleportTo(double, double, double)}：
     * 服务端玩家的位置由客户端权威，直接改 {@code deltaMovement} 会被客户端的移动包覆盖掉，
     * 所以「高速平移」只能用每 tick 的位置包来推。
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

        Entity target = level.getEntity(flight.targetEntityId);
        if (!(target instanceof LivingEntity living) || !living.isAlive())
        {
            abort(player, "目标已失，穿越中断");
            return;
        }

        Vec3 from = player.position();
        Vec3 delta = living.position().subtract(from);
        double distance = delta.length();

        if (distance <= ARRIVE_DISTANCE)
        {
            FLIGHTS.remove(player.getUUID());
            arrive(player, living);
            return;
        }
        if (flight.remaining <= 0 || distance > CAST_RANGE * 2.0D)
        {
            FLIGHTS.remove(player.getUUID());
            if (distance <= CAST_RANGE * 2.0D)
            {
                arrive(player, living);
            }
            else
            {
                abort(player, "目标跑得太远，穿越中断");
            }
            return;
        }

        Vec3 next = from.add(delta.scale(Math.min(flight.step, distance) / distance));
        /* 先转头再传送：位置包会把当前朝向一并带过去，观感上就是一路盯着目标飞 */
        player.lookAt(EntityAnchorArgument.Anchor.EYES, living.position());
        player.teleportTo(next.x, next.y, next.z);
        /* 空中连传送不吃坠落伤害；尾迹用自身那颗星的星色，与标记火花同源 */
        player.resetFallDistance();
        StarMarkSkillLogic.spawnSpark(level, player.position().add(0.0D, 1.0D, 0.0D), flight.star, TRAIL_SCALE);
        flight.remaining--;
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
     */
    private static void arrive(ServerPlayer player, LivingEntity target)
    {
        EpicFightCompat.stopFlyingTurnAnimation(player);
        EpicFightCompat.playFlyingTurnKickAnimation(player);
        /* 位置每 tick 被传送覆盖，速度分量可能残留；停下来时清零，免得落地还滑一段 */
        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();

        String name = target.getName().getString();
        StarMark mark = PlayerStarMarkData.findTarget(player,
                StarMarkSkillLogic.markOfEntity(player, target));
        if (mark == null)
        {
            /* 飞行途中标记被清掉了：只兑现冲撞这一脚 */
            boolean hit = TechniqueDamageSources.deal(target, player, RAM_DAMAGE);
            notifyPlayer(player, "★飞行，穿越 · " + name
                    + (hit ? " 冲撞 " + Math.round(RAM_DAMAGE) : " 已被拦下"));
            return;
        }

        /* ① 引爆旧印记：爆心先取，detonate 会把这条标记消耗掉 */
        Vec3 center = StarMarkSkillLogic.anchorOf(mark, target);
        StarMarkDetonation.Result result = StarMarkDetonation.detonate(player, mark, center);

        /* ② 冲撞伤害，并给目标补上「当前选中」的那颗印记 */
        boolean hit = TechniqueDamageSources.deal(target, player, RAM_DAMAGE);
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

        StringBuilder message = new StringBuilder("★飞行，穿越 · ").append(name).append("：")
                .append(mark.type().displayName()).append(" 引爆");
        if (result.damaged())
        {
            message.append(" 单体 ").append(Math.round(StarMarkDetonation.DETONATION_DAMAGE));
        }
        if (hit)
        {
            message.append("；冲撞 ").append(Math.round(RAM_DAMAGE));
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
