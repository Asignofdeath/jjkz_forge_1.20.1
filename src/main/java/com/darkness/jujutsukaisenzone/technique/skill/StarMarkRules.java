package com.darkness.jujutsukaisenzone.technique.skill;

import com.darkness.jujutsukaisenzone.player.PlayerStarMarkData;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import com.darkness.jujutsukaisenzone.player.StarMark;
import com.darkness.jujutsukaisenzone.player.StarMarkType;
import com.darkness.jujutsukaisenzone.technique.BarrierTechniqueServerLogic;
import com.darkness.jujutsukaisenzone.technique.TechniqueDamageSources;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 星间飞行的三态标记规则，只在服务端结算。
 * <p>
 * <b>同标记</b>：互相吸引（常态向质心靠拢；牵引期间改为向施术者靠拢）。被弹飞出去的标记在落回地面之前，
 * 还会额外吃一份「制导」横向加力，朝同星拐一点——「踢足球」的手感来源，见 {@link #GUIDANCE_IMPULSE}。
 * <b>撞击伤害</b>：被弹飞的球撞上另一条标记时双方各受一次「速度 × 系数」的术式伤害
 * （见 {@link #resolveImpacts}）——这才是弹射机制的收益点，缓慢靠拢的同星相吸撞不痛。<br>
 * <b>相邻</b>：无效果。<br>
 * <b>跨序</b>：仅当「禁行」星域生效时无法靠近；但可以「逐级经过」化解，见下。
 * <p>
 * <b>累积解锁（跨序的核心状态）</b>：每个玩家持一份「星域实例通行证」，按标记逐条索引。
 * 每条标记的位图初始 = 自己的星 + 顺序相邻的星；与另一个标记<b>接触</b>后，把对方那颗星及其相邻并入。
 * 于是 ① 只能先碰上 ②，踩过 ② 才解锁 ③……逐级推进即可走完整条链。
 * 判据是「对方那颗星在不在我的位图里」，而不是「两颗星的序号差」——后者与进展无关，
 * 会让玩家按顺序走也永远被挡在第三颗星之前。
 * <p>
 * <b>接触只对「本来就够得着的那颗星」作数</b>：接触的星必须已经在自己的位图里（也就是这一侧本来
 * 就不吃斥力）。否则被排斥的星只是「硬挤进去」就能白拿，跨序链条会被一次接触直接跳过一级。
 * 于是「经过」与「被推开」用的是同一条判据，两侧各自判定——真经过的那一侧才解锁。
 * <p>
 * <b>但解锁有位阶寿命</b>：接触挣来的位只活 {@link #UNLOCK_DECAY_TICKS}（{@code / 20} 秒），
 * 过期即回落到初始位图；初始位（自己的星 + 相邻星）则永不过期。所以「走完顺序」拿到的是一个
 * <b>有期限的窗口</b>，而不是永久豁免——敌人散开、停下、或被拉开一会儿，跨序斥力就自己回来了。
 * 而且<b>一轮实例内每颗星只授予一次</b>：过期就是真的失去，贴着目标站着也不会被补发，
 * 否则「粘住不放」等于永久破解。想重新拿回窗口，只能解域后重新展开（那本来就是新的一轮）。
 * <p>
 * <b>通行证的生命周期 = 一次星域展开</b>：{@link #activateGate} / {@link #activateFixedGate} 会把它清零，
 * 解域也会丢弃，所以重新展开总是从初始位图起步。
 * 代价是「解锁」与「推开」在同一个实例内互斥：位图越满，能被推开的标记越少。
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
    /**
     * 标记的最大存活距离（格）；超出即解除。
     * <p>
     * 只是<b>兜底</b>，不是玩法尺度——标记的终结方式本该是「目标消失 / 方块被破坏 / 换维度 / 主动清除」，
     * 距离剪除仅用于防止长期满世界打标导致 NBT 无限膨胀。因此必须明显大于星域半径上限
     * （{@link BarrierTechniqueServerLogic#staticGetMaxRadius()} = 64 格），否则域开到最大时，
     * 域边的标记会在域生效前就被剪掉、域内凭空少几颗星。
     * <p>
     * 取 128 = 上限的两倍：域能罩住的范围一定留得住标记。
     * 注意参与力学结算的数量另由 {@link #MAX_SIMULATED} 按距离截断，与本剪除无关；
     * 「同时铺太多」则由持有上限（{@code PlayerStarMarkData#MAX_MARKS}）兜底，两者性质不同。
     */
    public static final double MARK_RANGE = 128.0D;

    /**
     * 星位等级的衰减间隔（tick）：距最后一次打标每满这么久，星位等级掉 1 级（最低停在 1 级）。
     * <p>
     * 衰减只吃掉「叠上去的部分」，标记本身不会因为时间消失——所以它不违反 {@link #MARK_RANGE}
     * 那套「标记该由目标存亡决定」的立场，只是让「叠满 5 级」变成有时限的成就：
     * 想引爆就得在窗口内连续叠满，而不是提前叠好放着。
     */
    public static final int STACK_DECAY_TICKS = 8 * 20;

    /**
     * 跨序解锁的存活时间（tick）：接触授予的解锁位只在这一段内有效，过期即回落到初始位图。
     * <p>
     * 这是「累积解锁」从<b>只增不减</b>改为<b>会掉</b>的关键：敌人走完顺序只换来一个有期限的窗口，
     * 窗口一过跨序斥力自动恢复——不存在「走完就永久破解」。
     * <p>
     * <b>过期不会续期</b>（见 {@link #grantUnlock}）：一轮星域实例内每颗星只授予一次，
     * 贴着目标不动也一样会掉，掉了就得重新展开星域才有。所以「经过」是一次性的通行，
     * 不是「粘住就永久有效」——否则敌人只要贴着你站住，这个窗口就永远不过期。
     */
    public static final int UNLOCK_DECAY_TICKS = 10 * 20;

    /**
     * 星位等级的时间衰减：距 {@link StarMark#lastMarkedAt()} 每满 {@link #STACK_DECAY_TICKS} 掉 1 级，
     * 最低停在 1 级。
     * <p>
     * 纯函数、不写回——由 {@code StarMarkSkillLogic#resolveAll} 每 tick 调用一次，
     * 返回值与存档里的值不同时才落盘，所以静止不动时没有 NBT 写入。
     *
     * @param now 当前游戏刻（{@code Level#getGameTime()}）
     */
    public static int decayedStacks(StarMark mark, long now)
    {
        long elapsed = now - mark.lastMarkedAt();
        if (elapsed < STACK_DECAY_TICKS)
        {
            return mark.stacks();
        }
        return Math.max(1, mark.stacks() - (int) (elapsed / STACK_DECAY_TICKS));
    }

    /** 牵引持续时间（5 秒）。 */
    public static final int PULL_DURATION_TICKS = 100;
    /** 牵引的最大作用距离（格）。 */
    public static final double PULL_RANGE = 24.0D;

    /**
     * 禁行星域半径（格）——<b>不写死</b>，托管给「结界尺寸调节器」。
     * <p>
     * 中键切到「结界半径」调节模式后用侧键 4 / 5 调档（默认 8 格，最高 64 格），
     * 结界本体与禁行星域共用同一份档位，因此两者尺寸同步变化。
     * <p>
     * 半径是**每 tick 现取**的：域展开期间调档立刻生效，跟随版与原地版同理；
     * 原地版只固定圆心，不固定半径。
     */
    public static double gateRadius(ServerPlayer owner)
    {
        return BarrierTechniqueServerLogic.staticGetBarrierRadius(owner);
    }

    /** 同标记常态吸引的有效半径（格）。 */
    private static final double ATTRACT_RANGE = 16.0D;
    /** 同标记常态吸引的每 tick 增量速度。 */
    private static final double ATTRACT_IMPULSE = 0.010D;
    /**
     * 「制导」：同星吸引给<b>飞行途中</b>（离地）的标记的额外横向增量速度。
     * <p>
     * 玩法定位是「踢足球」：把目标踢出去之后，如果它落进另一颗同类星的
     * {@link #ATTRACT_RANGE}（16 格）内，就会朝那颗星拐一点——这是<b>辅助</b>，不是必中，
     * 玩家自己的射门手感仍然是主导。
     * <p>
     * <b>刻意用「加力」而不是「转向」</b>：横向冲量恒定 ⇒ 前向速度越大，偏角
     * {@code atan(横向速度 / 前向速度)} 越小 ⇒ <b>飞得越快越难命中</b>。这条曲线是免费得来的，
     * 不需要任何额外的速度衰减代码；反过来若改成「每 tick 转固定角度」，速度再高也照转不误，
     * 就得手写一套衰减去补。
     * <p>
     * 0.045 的体感：半径 8 档、飞约 9.5 格的一脚，侧移约 1.6~1.9 格，即约 <b>2 格的容错窗口</b>
     * （猪的碰撞箱约 0.7 格宽）。偏差 2 格内救得回来，超出就靠手感——这就是「辅助而非必中」的边界。
     * <p>
     * ⚠️ <b>不要乘以 {@link #burstStackScale(int)} 或 {@link #burstImpulse(double)}。</b>
     * 制导是一份<b>固定额度</b>的补偿，速度带来的难度<b>必须保留</b>——设计上要求
     * 「域越小 / 星位越高的重炮越难打准」。一旦让它跟着速度一起放大，满级星飞得远 2 倍、
     * 补偿也大 2 倍，容错占比不变，整个难度梯度当场被抵消。这是设计决策，不是遗漏。
     */
    private static final double GUIDANCE_IMPULSE = 0.045D;
    /** 牵引的每 tick 增量速度。 */
    private static final double PULL_IMPULSE = 0.070D;
    /** 跨序斥力的作用距离（格）。 */
    private static final double REPULSION_RANGE = 6.0D;
    /** 跨序斥力的每 tick 增量速度。 */
    private static final double REPULSION_IMPULSE = 0.075D;
    /**
     * 「接触」判定距离（格）：标记光点贴近到这一距离即视为「经过」，
     * 把对方那颗星及其顺序相邻并入自己的解锁位图。
     * <p>
     * 必须明显小于 {@link #REPULSION_RANGE}，否则「一进入斥力范围就解锁」会让跨序形同虚设。
     * <p>
     * 判的是<b>锚点</b>距离（方块在方块上方 1.15 格、实体在头顶再上 0.45 格），所以两名同地面
     * 站立的目标水平相隔约 2.3 格时锚点距离就已经到 2.5——看着还隔着一格半，其实已经算贴上了。
     * <p>
     * 另外「接触」本身还有一道门槛：只有<b>本来就够得着</b>的星才吃接触，见类注释的累积解锁说明。
     */
    private static final double CONTACT_RANGE = 2.5D;
    /** 最小间隔（格）：小于它则不再吸引，斥力直接翻倍（最小距离闸，防穿模）。 */
    private static final double MIN_SEPARATION = 1.5D;
    /** 单 tick 参与力学计算的最大标记数（按与施术者的距离取近者）。 */
    private static final int MAX_SIMULATED = 16;

    /**
     * 星域展开瞬间触发「弹开」的距离（格）。<b>没有下限</b>——只要够得着就弹。
     * <p>
     * 判的是<b>锚点</b>距离，而锚点比目标本体高（方块在方块上方 1.15 格、实体在头顶再上 0.45 格），
     * 所以「看上去挨着」的两个目标，锚点距离往往已经有一两格，窗口必须从 0 起步而不是从
     * {@link #CONTACT_RANGE} 起步——贴在一起的两颗星恰恰是最该被分开的那一对。
     * 贴进 {@link #CONTACT_RANGE} 的一对本来已经贴死了（常态的接触判定就落在那个距离内），
     * 展开的冲击把它们当场掀出去，连「挤在一起」都做不到——这正是禁行域该有的效果。
     */
    private static final double BURST_RANGE = 4.0D;

    /**
     * 展开瞬间的弹开初速（格/tick），取在<b>默认半径</b>档上。
     * <p>
     * 与星域半径<b>反相关</b>：域越小，标记越容易被塞进同一小片地方，越需要一次有力的分离，
     * 所以小域给大初速、大域给小初速（见 {@link #burstImpulse(double)}）。
     * <p>
     * 量级按「踢足球」定。目标会被 {@link #BURST_LIFT} 抬起来飞约 10 tick，空中每 tick 只衰减 9%、
     * 地面要衰减 45%，所以同样的初速离地后能飞出约 5 倍的距离：1.4 大致是 9~10 格的抛物线，
     * 落地后再滑一小段。
     */
    private static final double BURST_IMPULSE_NEAR = 1.40D;
    /**
     * 展开瞬间的弹开初速（格/tick），取在<b>最大半径</b>档上。
     * <p>
     * 0.6 也不是「小幅度」——同样会被抬起来飞约 4 格。滞空时长由上抛量决定（固定），
     * 所以位移只由初速决定：星域尺寸影响的确实只是「发射初速」这一个量。
     */
    private static final double BURST_IMPULSE_FAR = 0.60D;

    /**
     * 展开弹射的<b>固定上抛速度</b>（格/tick）。
     * <p>
     * 「踢足球」的观感就来自这一项：纯水平推只会让目标贴着地面蹭出去——落地状态下摩擦每 tick
     * 吃掉约 45%（方块摩擦 0.6 × 0.91），0.9 的初速在实机上也就是滑出两格。掀起来之后目标离地，
     * 空中每 tick 只衰减 9%，同样初速能飞出约 5 倍远，并且落地前有一段真正的弧线。
     * <p>
     * 取<b>固定值</b>（不随初速缩放）是刻意的：这样滞空时长一致，位移就只由初速决定——
     * 「域越小初速越大、弹得越远」于是是同一个机制的两个表现，而不是两条互相干扰的规则。
     */
    private static final double BURST_LIFT = 0.42D;

    /**
     * 星位等级的弹射加成：目标星位每高 1 级，初速再乘 {@code 1 + 本值 × (等级 - 1)}。
     * <p>
     * 1 级 = ×1.00，5 级（叠满）= ×{@code 1 + 4 × 本值}。取 0.25 时正好是「满级翻倍」：
     * 配合固定上抛（滞空时长一致、位移正比于初速，见 {@link #BURST_LIFT}），
     * 飞行距离也就同步翻倍。
     * <p>
     * 与半径项 {@link #burstImpulse(double)} 是<b>相乘</b>关系，各管一头：等级决定「这颗星有多重」，
     * 半径决定「这片域有多挤」，互不覆盖。
     * <p>
     * 这就是唯一的手感旋钮——嫌不够猛就调大（读到的百分比就是每级加成）。注意调大后
     * 满级目标会飞得很远，别把怪直接送出渲染距离。
     */
    private static final double BURST_STACK_BONUS = 0.25D;

    /**
     * <b>撞击伤害</b>：「被弹飞的球」撞上另一条标记时的伤害 = 撞击速度 × 本值。
     * <p>
     * 这是「踢足球」玩法的<b>收益点</b>，也是它存在的理由：常态的同星相吸只是每 tick 加
     * {@link #ATTRACT_IMPULSE} 的力、缓慢靠拢，撞上去也不痛；只有弹射给出的那种力道才谈得上撞击。
     * 于是「速度」这一个量同时管三件事——飞多远、好不好打准、撞上去多痛。
     * <p>
     * 速度自带星位倍率（{@link #burstStackScale}）与半径档（{@link #burstImpulse(double)}），
     * 所以<b>不需要</b>在这里再乘一次等级：满级重炮一次撞击的伤害天然是轻推的数倍。
     * <p>
     * 12 的体感：半径 8 档，刚出膛（1.4）撞上 = 17 点，滑行到阈值边缘（0.45）撞上 = 5 点。
     * 猪（10 血）一击毙命，玩家（20 血）挨一记重炮去掉大半条命。
     */
    private static final double IMPACT_DAMAGE_PER_SPEED = 12.0D;
    /**
     * 撞击的最低速度（格/tick）：低于它视为「蹭到」，不造成伤害。
     * <p>
     * 它同时也是「缓慢靠近不痛」这条设定的执行者——常态同星相吸的稳态漂移只有
     * {@link #ATTRACT_IMPULSE} ÷ (1 − 0.91) ≈ 0.11 格/tick，永远够不到这条线。
     */
    private static final double IMPACT_MIN_SPEED = 0.45D;
    /** 撞击判定的膨胀量（格）：吸收「本 tick 位移」推算的微小误差与贴边掠过。 */
    private static final double IMPACT_MARGIN = 0.35D;
    /** 同一颗球两次撞击的最小间隔（tick）：防止贴着目标逐 tick 反复结算。 */
    private static final int IMPACT_COOLDOWN_TICKS = 10;
    /**
     * 发射后多久内算「被弹飞的球」（tick）。
     * <p>
     * 只在 {@link #burstNeighbors} 里盖这个标签，所以「这次撞击是弹射造成的」是精确成立的：
     * 平时被同星吸引蹭过去的标记身上没有标签，撞了也不结算伤害。
     * 40 tick（2 秒）比滞空（约 10 tick）宽裕得多，让落地后带着余速滑出去撞上的那一记也算数。
     */
    private static final int BURST_FLIGHT_TICKS = 40;

    /** 竖直方向的力度衰减，避免把目标顶上天。 */
    private static final double ATTRACT_VERTICAL_SCALE = 0.35D;
    private static final double REPULSION_VERTICAL_SCALE = 0.5D;

    private static final String TAG_PULL_UNTIL = "jujutsukaisenzone_love_rendezvous_pull_until";
    /**
     * 跟随星域的开 / 关标记。
     * <p>
     * <b>星域没有时限</b>：展开后一直生效，直到主动解除（{@link #deactivateGate}）或改换生得术式，
     * 因此这里存的是布尔开关而不是「到期时刻」。
     */
    private static final String TAG_GATE_ACTIVE = "jujutsukaisenzone_love_rendezvous_gate_active";
    /** 固定星域（原地展开）的锚点记录；一个玩家同时只保留一个。 */
    private static final String TAG_GATE_ANCHOR = "jujutsukaisenzone_love_rendezvous_gate_anchor";
    private static final String KEY_ANCHOR_X = "x";
    /**
     * 星域实例的通行证：CompoundTag，键 = 标记目标的身份，值 = <b>长度为 5 的 long 数组</b>，
     * 下标 = 星序 {@code ordinal()}，元素 = 该位解锁到期的那一刻（游戏刻）。
     * <p>
     * 元素是「授予到期时刻」：{@code 0} = 从未授予；非 {@code 0} = 授予过（值即使已过期也仍非 {@code 0}，
     * 用来保证<b>不再补发</b>——见 {@link #grantUnlock}）。因此条目要保留到本轮结束，<b>过期不清零</b>。
     * <p>
     * 用「到期时刻」而不是「布尔位图」，是因为解锁会<b>过期</b>：接触授予的位只活
     * {@link #UNLOCK_DECAY_TICKS}，过期即自动回落到初始位图（自己的星 + 顺序相邻的星）。
     * 位图本身不再单独存——它就是「初始位 ∪ 尚未过期的接触位」，每次读取现算。
     * <p>
     * 展开星域时整个清掉（见 {@link #resetGateUnlocks}），因此它仍然是「一次展开的生命周期」，
     * 而不是挂在 {@link StarMark} 上的字段——同一个目标身上的标记，在两轮展开里进度互不相干。
     * 旧存档里这个键存的是 int（曾经是布尔位图），{@code getLongArray} 读出来是空数组、
     * 按「没有尚未过期的接触位」处理，下一次写入自然覆盖——<b>不需要迁移</b>。
     */
    private static final String TAG_GATE_UNLOCKS = "jujutsukaisenzone_love_rendezvous_gate_unlocks";
    private static final String KEY_ANCHOR_Y = "y";
    private static final String KEY_ANCHOR_Z = "z";
    private static final String KEY_ANCHOR_DIM = "dim";
    /*
     * 下面两个是**实体**身上的键（Forge 会把实体的 persistentData 存成 ForgeData 一起落盘），
     * 不是玩家数据：撞击状态属于「被踢出去的那颗球」，而球通常是生物，不是施术者。
     */
    /** 该实体是哪一次弹射踢出去的：到期刻。超出即不再是「球」，见 {@link #BURST_FLIGHT_TICKS}。 */
    private static final String TAG_BURST_UNTIL = "jujutsukaisenzone_love_rendezvous_burst_until";
    /** 该实体上一次撞击结算的游戏刻，用于 {@link #IMPACT_COOLDOWN_TICKS} 去抖。 */
    private static final String TAG_IMPACT_AT = "jujutsukaisenzone_love_rendezvous_impact_at";

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

    /**
     * 展开禁行星域：域内跨序之星无法靠近。
     * <p>
     * <b>没有时限</b>——展开后一直生效，直到主动解除（Ctrl + 技能键）。重复施放只是刷新，
     * 并照常结算一次咒力与冷却。
     * <p>
     * 每次展开都开启<b>新一轮通行证</b>：所有标记的解锁位图清零，重新从「自己的星 + 顺序相邻的星」起步。
     * 于是「走完顺序」只豁免本次展开——想重新拿回封锁能力，解域后重新展开即可。
     */
    public static void activateGate(ServerPlayer player)
    {
        resetGateUnlocks(player);
        player.getPersistentData().putBoolean(TAG_GATE_ACTIVE, true);
        /* 通行证刚清零，此刻读到的位图就是初始位图——弹射必须卡在这个时点结算 */
        int kicked = burstNeighbors(player);
        player.displayClientMessage(
                Component.literal("★禁行 · 星域展开，半径 " + (int) gateRadius(player)
                        + " 格内之星必须逐级经过才能靠近下一颗（Ctrl+技能键解除）"
                        + burstNote(kicked)),
                true);
    }

    /**
     * 展开<b>固定</b>禁行星域：锚在释放点，不随施术者移动。
     * <p>
     * 与跟随版完全同源——同样的半径（{@link #gateRadius(ServerPlayer)}，随「结界半径」调节）、
     * 同样的「无时限」，差别只在圆心：跟随版每 tick 取施术者脚下，固定版取释放那一刻的坐标。
     * 同一时间只保留一个固定域，再次释放即覆盖旧锚点；换维度也会失效。
     * 两个域可以同时存在（跟随 + 固定），判定时取并集。
     * <p>
     * 与跟随版一样，每次展开都开启新一轮通行证（清零所有标记的解锁位图）。
     * 通行证是<b>每个玩家一份</b>，所以跟随域与固定域并存时共用同一轮进度。
     */
    public static void activateFixedGate(ServerPlayer player)
    {
        resetGateUnlocks(player);
        Vec3 anchor = player.position();
        CompoundTag tag = new CompoundTag();
        tag.putDouble(KEY_ANCHOR_X, anchor.x);
        tag.putDouble(KEY_ANCHOR_Y, anchor.y);
        tag.putDouble(KEY_ANCHOR_Z, anchor.z);
        tag.putString(KEY_ANCHOR_DIM, player.level().dimension().location().toString());
        player.getPersistentData().put(TAG_GATE_ANCHOR, tag);
        /* 同跟随版：通行证已清零，弹射在初始位图上结算 */
        int kicked = burstNeighbors(player);
        player.displayClientMessage(
                Component.literal("★禁行 · 原地星域展开，半径 " + (int) gateRadius(player)
                        + " 格内之星必须逐级经过才能靠近下一颗（锚定此处不随你移动，Ctrl+技能键解除）"
                        + burstNote(kicked)),
                true);
    }

    // ======================== 展开瞬间的弹射 ========================

    /**
     * 星域展开瞬间的一次性冲击：把「已经挨得太近的跨序之星」当场弹开。
     * <p>
     * 与常态斥力（每 tick 的持续推力、双向）是两回事：这里只在展开那一刻结算一次，而且是
     * <b>单向</b>的——施力方纹丝不动，只有被弹的一方获得初速，观感上像「域一开就被顶出去」。
     * <p>
     * 强度与星域半径反相关（见 {@link #burstImpulse(double)}）：小域里标记本就挤，需要更大初速；
     * 大域里标记天然分散，给小初速即可。此外还按<b>被弹那一方的星位等级</b>放大
     * （见 {@link #burstStackScale(int)}）：星位越高飞得越远，叠满的目标会被轰出老远。
     * <p>
     * <b>谁弹开谁</b>（与「方块是静态锚点」的立场一致）：
     * <ul>
     *   <li>方块 vs 实体 → 方块弹开实体，方块不动；</li>
     *   <li>方块 vs 方块 → 不弹（连常态斥力都不判定）；</li>
     *   <li>实体 vs 实体 → 总生命值高者弹开低者，相同则谁也不弹。</li>
     * </ul>
     * 「总生命值」取生物的最大生命值（{@link LivingEntity#getMaxHealth()}）；掉落物等非生物记 0，
     * 于是生物总能弹开掉落物。
     * <p>
     * 被弹的一方还必须对施力方<b>处于排斥状态</b>——相邻星本来就允许靠近，展开时不该被炸开。
     * <p>
     * 没有距离下限：贴在一起的两颗星正是最该被分开的，展开的冲击会当场打断它们「接触 → 经过」
     * 的贴合状态。方向与初速见 {@link #burstKick(Vec3, Vec3, double)}（水平甩出 + 一记上抛）。
     *
     * @return 实际弹开的颗数，供展开提示使用（见 {@link #burstNote(int)}）
     */
    private static int burstNeighbors(ServerPlayer player)
    {
        List<StarMarkSkillLogic.ResolvedMark> marks = StarMarkSkillLogic.resolveAll(player);
        if (marks.size() < 2)
        {
            return 0;
        }
        /* 本次展开的圆心：固定版刚把锚点定在脚下，跟随版本来就是脚下——一个圆心就够 */
        List<Vec3> centers = List.of(player.position());
        double radius = gateRadius(player);
        double radiusSqr = radius * radius;
        double baseImpulse = burstImpulse(radius);
        int kicked = 0;
        long now = player.level().getGameTime();

        for (int i = 0; i < marks.size(); i++)
        {
            StarMarkSkillLogic.ResolvedMark a = marks.get(i);
            for (int j = i + 1; j < marks.size(); j++)
            {
                StarMarkSkillLogic.ResolvedMark b = marks.get(j);
                /* 方块之间互相判定毫无意义 */
                if (!a.mark().mobile() && !b.mark().mobile())
                {
                    continue;
                }

                double distance = a.position().distanceTo(b.position());
                if (distance > BURST_RANGE)
                {
                    continue;
                }

                /* 与常态斥力同一口径：任一方落在域内即算「域内」 */
                if (!withinAnyGate(centers, a.position(), radiusSqr)
                        && !withinAnyGate(centers, b.position(), radiusSqr))
                {
                    continue;
                }

                StarMarkSkillLogic.ResolvedMark shooter;
                StarMarkSkillLogic.ResolvedMark mover;
                if (a.mark().mobile() == b.mark().mobile())
                {
                    /* 双方都是实体：总生命值高者弹开低者，相同则作罢 */
                    double lifeA = lifePoints(a);
                    double lifeB = lifePoints(b);
                    if (lifeA == lifeB)
                    {
                        continue;
                    }
                    shooter = lifeA > lifeB ? a : b;
                    mover = lifeA > lifeB ? b : a;
                }
                else
                {
                    /* 一方是方块：方块弹开实体，自己不动 */
                    shooter = a.mark().mobile() ? b : a;
                    mover = a.mark().mobile() ? a : b;
                }

                /* 被弹的一方必须对施力方处于排斥状态（初始位图里没有对方那颗星） */
                if (shooter.mark().type().isUnlockedIn(unlockMaskOf(player, mover.mark())))
                {
                    continue;
                }

                /* 半径定「域有多挤」，星位定「这颗星有多重」，两者相乘 */
                double impulse = baseImpulse * burstStackScale(mover.mark().stacks());
                push(mover.entity(), burstKick(shooter.position(), mover.position(), impulse), player, true);
                /*
                 * 盖上「这是被踢出去的球」的标签。撞击伤害只认它——平时被同星吸引蹭过去的标记
                 * 身上没有这个标签，撞了也不结算，于是「缓慢靠近不痛、弹开猛撞才痛」自然成立。
                 */
                if (mover.entity() != null)
                {
                    mover.entity().getPersistentData().putLong(TAG_BURST_UNTIL, now + BURST_FLIGHT_TICKS);
                }
                kicked++;
            }
        }
        return kicked;
    }

    /**
     * 「飞行，穿越」撞上目标那一瞬间的弹射：把 {@code mover} 沿「施术者 → 目标」的方向踢出去。
     * <p>
     * 与 {@link #burstNeighbors} 的展开弹射共用同一套方向与上抛（{@link #burstKick}），
     * 所以被踢出去的目标同样是「一颗球」——会盖上 {@link #TAG_BURST_UNTIL}，
     * 接下来 {@link #BURST_FLIGHT_TICKS} 内撞上别的标记照常结算撞击伤害。
     * <p>
     * <b>力度是现算的</b>：{@link #BURST_IMPULSE_NEAR}（最近的域档）× {@code burstStackScale(MAX_STACKS)}
     * （满级倍率）。目标恒为满级（这是技能的前置条件），所以这两项合起来给出的就是这套机制里最猛的一脚；
     * 也正因为恒为满级，这里<b>不按半径插值</b>——星域在这一刻已经被收掉了，半径根本没有意义。
     * <p>
     * 尺度上是刻意的：{@code burstNeighbors} 是「把挤在一起的星分开」，而这里是<b>终结技的最后一下</b>，
     * 目标该被真正踢飞出去（约 18 ~ 20 格的抛物线），而不是挪一格。
     *
     * @param owner 施术者，也是这一脚的发力方（永远是施术者弹飞目标，不看双方生命值）
     * @param mover 被弹飞的一方；为 {@code null}（方块锚点）时什么都不做
     * @param from  发力点，取施术者脚下的位置
     */
    public static void flightKick(ServerPlayer owner, @Nullable Entity mover, Vec3 from)
    {
        if (mover == null || mover.isRemoved() || !mover.isAlive())
        {
            return;
        }
        double impulse = BURST_IMPULSE_NEAR * burstStackScale(StarMark.MAX_STACKS);
        push(mover, burstKick(from, mover.position(), impulse), owner, true);
        mover.getPersistentData().putLong(TAG_BURST_UNTIL, gameTime(owner) + BURST_FLIGHT_TICKS);
    }

    /**
     * 展开提示里的弹射计数后缀。
     * <p>
     * 必须与展开提示拼成同一条——动作栏只有一个槽位，单发一条会当场被紧随其后的
     * 「★禁行 · 星域展开」覆盖掉，那样就白报了。
     */
    private static String burstNote(int kicked)
    {
        return kicked == 0 ? "" : "；冲击弹开 " + kicked + " 颗";
    }

    /**
     * 「踢足球」的收尾：被弹飞的球撞上另一条标记时，双方各挨一记术式伤害。
     * <p>
     * 这是整个弹射机制存在的<b>理由</b>——常态的同星相吸只是每 tick 加
     * {@link #ATTRACT_IMPULSE} 的力、缓慢靠拢，撞上去不痛；只有弹射给出的那种力道才谈得上撞击。
     * 于是「速度」这一个量同时决定三件事：飞多远（{@link #burstImpulse(double)}）、
     * 好不好打准（{@link #GUIDANCE_IMPULSE} 那份固定补偿能不能追上）、撞上去多痛（本方法）。
     * <p>
     * <b>触发条件三件套</b>：
     * <ol>
     *   <li>球身上有 {@link #TAG_BURST_UNTIL} 且未到期——即「这次撞击是被弹射踢出来的」，
     *       由 {@link #burstNeighbors} 在推出去的同时盖上；</li>
     *   <li>水平速度 ≥ {@link #IMPACT_MIN_SPEED}——同星吸引的缓慢靠拢永远够不到这条线；</li>
     *   <li>扫掠判定命中对方碰撞箱——为什么必须扫掠见下。</li>
     * </ol>
     * <p>
     * <b>为什么必须扫掠而不是逐点判定</b>：球速可达 2.8 格/tick，而猪只有约 0.7 格宽，
     * 且原版实体之间本来就不做碰撞（会互相穿过）。每 tick 只取一个点来判，高速球会直接从目标
     * 身上「跳」过去、一次都判不到——越用力越打不中，正好与设计意图相反。这里改用
     * 「上一刻 → 这一刻」的线段（tick END 时 {@link Entity#getDeltaMovement()} 就是本 tick
     * 实际走过的位移）与目标膨胀后的碰撞箱求交（{@link AABB#clip(Vec3, Vec3)}），
     * 一次位移只判一次，快慢都不漏。
     * <p>
     * <b>谁受伤</b>：双方各挨一记。球是力道来源、目标是被砸的那个，两边都不白挨。
     * 方块没有生命值也拿不到实体，所以「立一颗同星方块当球门」只负责吸引与瞄准，
     * 撞上去不结算伤害；掉落物同理（非 {@link LivingEntity}）。
     * <p>
     * 伤害走 {@link TechniqueDamageSources#deal}，因此术式击杀照常给咒术经验、掉术式情报、
     * 记罪恶者、判黑闪——与另外几处术式伤害同一个口径。
     */
    private static void resolveImpacts(ServerPlayer owner, List<StarMarkSkillLogic.ResolvedMark> marks)
    {
        if (marks.size() < 2)
        {
            return;
        }
        long now = owner.level().getGameTime();

        for (StarMarkSkillLogic.ResolvedMark ballMark : marks)
        {
            Entity ball = ballMark.entity();
            /* 方块锚点没有实体、掉落物不是 LivingEntity——都当不了球 */
            if (!(ball instanceof LivingEntity ballLiving))
            {
                continue;
            }
            if (ball.getPersistentData().getLong(TAG_BURST_UNTIL) <= now)
            {
                continue;
            }

            double speed = horizontalSpeed(ball);
            if (speed < IMPACT_MIN_SPEED)
            {
                continue;
            }
            if (ball.getPersistentData().getLong(TAG_IMPACT_AT) + IMPACT_COOLDOWN_TICKS > now)
            {
                continue;
            }

            Vec3 head = ball.position();
            Vec3 tail = head.subtract(ball.getDeltaMovement());

            for (StarMarkSkillLogic.ResolvedMark other : marks)
            {
                Entity victim = other.entity();
                if (victim == null || victim == ball || !(victim instanceof LivingEntity living))
                {
                    continue;
                }
                if (victim.getBoundingBox().inflate(IMPACT_MARGIN).clip(tail, head).isEmpty())
                {
                    continue;
                }

                float damage = (float) (speed * IMPACT_DAMAGE_PER_SPEED);
                /* 两边都要结算，所以用 | 而不是 ||（后者会在第一记生效时短路跳过第二记） */
                boolean dealt = TechniqueDamageSources.deal(living, owner, damage);
                dealt |= TechniqueDamageSources.deal(ballLiving, owner, damage);

                ball.getPersistentData().putLong(TAG_IMPACT_AT, now);
                if (dealt)
                {
                    owner.displayClientMessage(Component.literal("★撞击 · "
                            + ballMark.mark().type().displayName() + " ⇄ "
                            + other.mark().type().displayName()
                            + " 各受 " + Math.round(damage) + " 伤害"), true);
                }
                /* 一颗球一次只撞一个目标；贴近不散的情况交给 IMPACT_COOLDOWN_TICKS 去抖 */
                break;
            }
        }
    }

    /** 水平速度（格/tick）：撞击只看「甩出去的那股劲」，竖直方向的坠落与上抛都不计入。 */
    private static double horizontalSpeed(Entity entity)
    {
        Vec3 velocity = entity.getDeltaMovement();
        return Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
    }

    /**
     * 展开瞬间的弹开初速：半径越小越大。
     * <p>
     * 在「默认档（{@link BarrierTechniqueServerLogic#staticGetDefaultRadius()}）→ 最大档
     * （{@link BarrierTechniqueServerLogic#staticGetMaxRadius()}）」之间线性插值，
     * 于是 8 / 16 / 32 / 64 格这四档依次落在 {@link #BURST_IMPULSE_NEAR} 与
     * {@link #BURST_IMPULSE_FAR} 之间等距递减。
     */
    private static double burstImpulse(double radius)
    {
        double near = BarrierTechniqueServerLogic.staticGetDefaultRadius();
        double far = BarrierTechniqueServerLogic.staticGetMaxRadius();
        if (far <= near)
        {
            return BURST_IMPULSE_NEAR;
        }
        double trimmed = Math.max(0.0D, Math.min(1.0D, (far - radius) / (far - near)));
        return BURST_IMPULSE_FAR + (BURST_IMPULSE_NEAR - BURST_IMPULSE_FAR) * trimmed;
    }

    /**
     * 被弹那一方的星位等级带来的初速倍率：等级越高，弹得越远。
     * <p>
     * {@code 1 + BURST_STACK_BONUS × (等级 - 1)}，即 1 级 ×1.00 → 5 级 ×2.00（默认系数下）。
     * 上抛量是固定的（{@link #BURST_LIFT}），滞空时长不随等级变，所以位移严格正比于初速——
     * 「星位越高飞得越远」在观感上就是抛物线被原样拉长，弧线高度不变、落点更远。
     * <p>
     * 等级由 {@code StarMarkSkillLogic#resolveAll} 结算衰减后给出（{@link StarMark#stacks()} 已是
     * 「距上次打标每过 {@link #STACK_DECAY_TICKS} 掉 1 级」之后的当前值），所以衰减过的目标
     * 弹得自然就近，不需要在这里再算一次时间。
     */
    private static double burstStackScale(int stacks)
    {
        int level = Math.max(1, Math.min(StarMark.MAX_STACKS, stacks));
        return 1.0D + BURST_STACK_BONUS * (level - 1);
    }

    /**
     * 弹开的方向与初速：<b>水平分量 + 一记固定上抛</b>。
     * <p>
     * 水平方向取锚点差的水平投影（「分开」就是水平甩出去），不能沿用常态斥力那套
     * 「保留一半垂直分量」：锚点比本体高，一方正处在另一方正上方（锚点水平重合）时方向会退化成
     * 纯垂直、水平冲量恰好为 0，看起来就是「开了域什么都没发生」。水平重合时给一个
     * 固定方向兜底，保证一定踢得动。
     * <p>
     * 再补上 {@link #BURST_LIFT} 把目标从地面上掀起来——离地才飞得远，见那儿的说明。
     * 上抛量与初速无关（固定），因此滞空时长一致，位移只由初速决定。
     */
    private static Vec3 burstKick(Vec3 from, Vec3 to, double impulse)
    {
        Vec3 flat = new Vec3(to.x - from.x, 0.0D, to.z - from.z);
        Vec3 dir = flat.lengthSqr() < 1.0E-8D
                ? new Vec3(1.0D, 0.0D, 0.0D)
                : flat.normalize();
        return dir.scale(impulse).add(0.0D, BURST_LIFT, 0.0D);
    }

    /**
     * 「总生命值」：生物的最大生命值；方块没有生命值可言，掉落物等非生物记 0。
     * <p>
     * 拿它当「谁弹开谁」的秤——生物必定弹得动掉落物，血量高的生物必定弹得动血量低的。
     */
    private static double lifePoints(StarMarkSkillLogic.ResolvedMark mark)
    {
        return mark.entity() instanceof LivingEntity living ? living.getMaxHealth() : 0.0D;
    }

    public static boolean isPullActive(ServerPlayer player)
    {
        return player.getPersistentData().getLong(TAG_PULL_UNTIL) > gameTime(player);
    }

    public static boolean isGateActive(ServerPlayer player)
    {
        return player.getPersistentData().getBoolean(TAG_GATE_ACTIVE);
    }

    /** 固定星域是否生效（同维度）。 */
    public static boolean isFixedGateActive(ServerPlayer player)
    {
        return fixedGateCenter(player) != null;
    }

    /**
     * 固定星域的锚点；未生效时返回 {@code null}。
     * <p>
     * 唯一的失效条件是换维度——星域没有时限。换维度时顺手清掉记录，
     * 避免回到原维度后星域突然复活。
     */
    @Nullable
    private static Vec3 fixedGateCenter(ServerPlayer player)
    {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(TAG_GATE_ANCHOR))
        {
            return null;
        }
        CompoundTag tag = data.getCompound(TAG_GATE_ANCHOR);
        if (!tag.getString(KEY_ANCHOR_DIM).equals(player.level().dimension().location().toString()))
        {
            data.remove(TAG_GATE_ANCHOR);
            return null;
        }
        return new Vec3(tag.getDouble(KEY_ANCHOR_X), tag.getDouble(KEY_ANCHOR_Y), tag.getDouble(KEY_ANCHOR_Z));
    }

    /**
     * 中断牵引。定向清除「当前标记」时调用——牵引本就以当前标记为引，引被清掉后它只会空转。
     *
     * @return 调用前是否处于牵引中
     */
    public static boolean clearPull(ServerPlayer player)
    {
        boolean active = isPullActive(player);
        player.getPersistentData().remove(TAG_PULL_UNTIL);
        return active;
    }

    /**
     * 解除星域：跟随域与固定域一并清掉，两者都没有时限，这是唯一的主动关闭手段。
     * <p>
     * 顺带丢弃本轮通行证——域都没了，解锁进度没有意义；重新展开本来就是新的一轮。
     *
     * @return 调用前是否处于「任一星域生效」状态
     */
    public static boolean deactivateGate(ServerPlayer player)
    {
        boolean active = isGateActive(player) || isFixedGateActive(player);
        player.getPersistentData().remove(TAG_GATE_ACTIVE);
        player.getPersistentData().remove(TAG_GATE_ANCHOR);
        resetGateUnlocks(player);
        return active;
    }

    // ======================== 星域实例通行证 ========================

    /**
     * 开启一轮新的星域实例：丢弃全部通行证。
     * <p>
     * 通行证的粒度是「每个玩家一份、按标记索引」，因此清的是施术者自己那一份；
     * 被标记的实体没有独立进度，它们能否接近某颗星完全由施术者这一份决定。
     */
    private static void resetGateUnlocks(ServerPlayer player)
    {
        player.getPersistentData().remove(TAG_GATE_UNLOCKS);
    }

    /**
     * 该标记在<b>本次星域展开</b>中当前的解锁位图 = 初始位 ∪ 尚未过期的接触位。
     * <p>
     * 本轮还没解锁过、或解锁已全部过期时，结果就是初始值 = 自己的星 + 顺序相邻的星，
     * 所以展开时不需要给每条标记预先写一条记录。
     */
    private static int unlockMaskOf(ServerPlayer player, StarMark mark)
    {
        return effectiveMask(mark, expiryOf(player, mark), gameTime(player));
    }

    /** 读取某条标记的解锁到期表（长度 = 星序长度；全 0 表示没被接触授予过任何位）。 */
    private static long[] expiryOf(ServerPlayer player, StarMark mark)
    {
        CompoundTag store = player.getPersistentData().getCompound(TAG_GATE_UNLOCKS);
        long[] expiry = new long[StarMarkType.count()];
        long[] stored = store.getLongArray(unlockKey(mark));
        System.arraycopy(stored, 0, expiry, 0, Math.min(stored.length, expiry.length));
        return expiry;
    }

    /**
     * 由「标记的星 + 到期表」现算当前解锁位图。
     * <p>
     * 初始位（自己的星 + 顺序相邻的星）<b>永不过期</b>——那是这条标记与生俱来的通行权；
     * 只有靠接触挣来的位才有寿命。
     */
    private static int effectiveMask(StarMark mark, @Nullable long[] expiry, long now)
    {
        int mask = mark.type().initialUnlockMask();
        if (expiry == null)
        {
            return mask;
        }
        for (StarMarkType type : StarMarkType.values())
        {
            if (expiry[type.ordinal()] > now)
            {
                mask |= 1 << type.ordinal();
            }
        }
        return mask;
    }

    /**
     * 到期表里是否记录过任何授予（<b>含已经过期的</b>）。
     * <p>
     * 不能改成「是否还有没过期的位」——那样一过期就把记录删了，下一 tick 同一颗星会被当成
     * 「从未授予」重新补发，{@link #UNLOCK_DECAY_TICKS} 就白设了。
     */
    private static boolean hasAnyGrant(long[] expiry)
    {
        for (long value : expiry)
        {
            if (value != 0L)
            {
                return true;
            }
        }
        return false;
    }

    /**
     * 通行证的索引键。
     * <p>
     * 方块取坐标、实体取 uuid，两者在维度内已经唯一（{@link PlayerStarMarkData} 也只统计当前维度），
     * 所以不必再拼维度。
     */
    private static String unlockKey(StarMark mark)
    {
        return mark.kind() == StarMark.Kind.BLOCK
                ? "b" + mark.pos().asLong()
                : "e" + mark.entityId();
    }

    /**
     * 自身标记的跨序进度读条，挂在「没瞄到方块时按技能键」的状态提示后面，方便核对走到哪一档。
     *
     * @return 形如「；自身标记本轮已解锁 ★Imai 薄荷青、★Acrux 樱粉」；自身没有标记时返回空串
     */
    public static String unlockStatus(ServerPlayer owner)
    {
        StarMark own = PlayerStarMarkData.findTarget(owner, StarMarkSkillLogic.markOfEntity(owner, owner));
        return own == null ? "" : "；自身标记本轮已解锁 " + starNames(unlockMaskOf(owner, own));
    }

    // ======================== 每 tick 力学 ========================

    /**
     * 对该玩家身上全部有效标记结算一次力学，并把结果写成增量速度。
     * <p>
     * 表现部分与标记解耦：星域的环只由「域是否生效」决定，因此 {@code marks} 为空时也会画
     * （见 {@link #renderGates}）；标记火花则归 {@link StarMarkSkillLogic#resolveAndRender(ServerPlayer)} 管。
     *
     * @param marks {@link StarMarkSkillLogic#resolveAndRender(ServerPlayer)} 的返回值
     */
    public static void apply(ServerPlayer owner, List<StarMarkSkillLogic.ResolvedMark> marks)
    {
        ServerLevel level = owner.serverLevel();
        long now = level.getGameTime();
        Vec3 ownerPos = owner.position();
        /* 星域圆心：跟随域取施术者脚下，固定域取释放点；两者可并存，判定取并集 */
        List<Vec3> gateCenters = gateCenters(owner, ownerPos);

        /*
         * 没有标记：吸引 / 斥力 / 连线都无从结算，但星域外环照画。
         * 否则「域已展开、身上却没挂任何标记」时画面毫无反馈，按了 Shift 像是没生效。
         */
        if (marks.isEmpty())
        {
            renderGates(level, owner, gateCenters, now);
            return;
        }

        boolean render = now % StarMarkSkillLogic.RENDER_INTERVAL_TICKS == 0L;
        StarMarkType selected = PlayerStarMarkData.getSelected(owner);
        boolean pulling = isPullActive(owner);

        /* 参与计算的目标数截断：按与施术者的距离取近者 */
        List<StarMarkSkillLogic.ResolvedMark> simulated = marks;
        if (marks.size() > MAX_SIMULATED)
        {
            simulated = new ArrayList<>(marks);
            simulated.sort(Comparator.comparingDouble(mark -> mark.position().distanceToSqr(ownerPos)));
            simulated = simulated.subList(0, MAX_SIMULATED);
        }

        /* 同标记：按质心吸引；牵引期间改为向施术者吸引。飞行中的成员另加一份制导加力（见 GUIDANCE_IMPULSE） */
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

                /*
                 * 制导：飞行途中（离地）再吃一层横向加力，让球朝同星拐一点。
                 * 「在不在飞」是**成员级**的，所以只能在这里现算——组级的 strength / verticalScale
                 * 在组循环外一次定下，管不了这一点。
                 */
                double memberStrength = strength;
                double memberVertical = verticalScale;
                if (!pullThisGroup && isAirborne(member))
                {
                    memberStrength += GUIDANCE_IMPULSE;
                    /*
                     * 垂直分量清零：留着 ATTRACT_VERTICAL_SCALE 会让空中的球被地面目标往下拽，
                     * 滞空变短、落点提前——弧线高度只该由 BURST_LIFT 决定，制导纯横向。
                     */
                    memberVertical = 0.0D;
                }

                Vec3 impulse = dir.scale(memberStrength * (0.35D + falloff))
                        .multiply(1.0D, memberVertical, 1.0D);
                push(member.entity(), impulse, owner, false);

                if (render && pullThisGroup)
                {
                    StarMarkSkillLogic.spawnLine(level, pos, attractor, entry.getKey());
                }
            }
        }

        /*
         * 撞击结算：传完整的 marks 而不是上面那份 simulated。
         * 球是往远处飞的，而 simulated 按「离施术者最近」截断到 MAX_SIMULATED 条，
         * 恰恰最可能把飞远的球剔出去。开销不成问题——外层只对「正在飞的球」进入内层，
         * 平时一次内层循环都不会开。
         */
        resolveImpacts(owner, marks);

        /*
         * 跨序：只在禁行星域生效（标记落在任意一个域内都算「域内」），判据是「累积解锁」而不是两颗星的序号差。
         *
         * 每个玩家持一份**本轮通行证**，按标记逐条索引（初始 = 自己的星 + 顺序相邻的星）。两个标记**接触**
         * 且接触的那颗星**本来就在自己位图里**（即这一侧本来就不吃斥力）才算「经过」，把对方那颗星及其
         * 相邻并入自己的位图；否则被排斥的星挤进去也什么都不发生，跨序链条不会被一次接触跳过一级。
         *
         * 并入的位带寿命（UNLOCK_DECAY_TICKS）且**一轮内只授予一次**：过期就真的失去，贴着目标站着也不会
         * 被补发——所以「走完顺序」换到的是一个一次性窗口，而不是「粘住就永久有效」。
         *
         * 通行证随展开重生（activateGate 清零），解域也会丢弃，所以重新展开总是从初始位图起步。
         */
        if (!gateCenters.isEmpty())
        {
            /* 半径每 tick 现取，域展开期间调档立刻生效 */
            double radius = gateRadius(owner);
            double gateRadiusSqr = radius * radius;
            /*
             * 本轮通行证快照：先从玩家数据一次性读出「到期表」，循环里只动内存，结束后统一写回。
             * 键是标记本身，因此同一 tick 内一条标记接触多个目标时可以连续累加。
             */
            Map<StarMark, long[]> unlocks = new HashMap<>();
            for (StarMarkSkillLogic.ResolvedMark resolved : simulated)
            {
                unlocks.put(resolved.mark(), expiryOf(owner, resolved.mark()));
            }
            /*
             * 本 tick 每条标记各自新解锁了哪些星。按标记分开收集，提示里才能说清「谁解锁了什么」——
             * 合成一张位图会出现「我只碰了一颗星，却报我解锁了三颗」的错觉。
             */
            Map<StarMark, Integer> gains = new LinkedHashMap<>();
            int passedMask = 0;

            for (int i = 0; i < simulated.size(); i++)
            {
                StarMarkSkillLogic.ResolvedMark a = simulated.get(i);
                for (int j = i + 1; j < simulated.size(); j++)
                {
                    StarMarkSkillLogic.ResolvedMark b = simulated.get(j);
                    /* 方块之间互相判定毫无意义 */
                    if (!a.mark().mobile() && !b.mark().mobile())
                    {
                        continue;
                    }

                    Vec3 posA = a.position();
                    Vec3 posB = b.position();
                    if (!withinAnyGate(gateCenters, posA, gateRadiusSqr)
                            && !withinAnyGate(gateCenters, posB, gateRadiusSqr))
                    {
                        continue;
                    }

                    double distance = posA.distanceTo(posB);

                    /*
                     * 接触 = 经过，但只对「本来就够得着的那颗星」作数：接触的星必须在自己的位图里
                     * （也就是这一侧本来就不吃斥力）。否则「硬挤进去」就能白拿一颗被排斥的星，
                     * 跨序链条会被一次接触跳过一级——本来要逐级走四步的链，两步就走完了。
                     * 两侧各自判定：谁经过，谁才解锁。
                     */
                    if (distance <= CONTACT_RANGE)
                    {
                        boolean passedA = b.mark().type().isUnlockedIn(
                                effectiveMask(a.mark(), unlocks.get(a.mark()), now));
                        boolean passedB = a.mark().type().isUnlockedIn(
                                effectiveMask(b.mark(), unlocks.get(b.mark()), now));
                        if (passedA)
                        {
                            passedMask |= 1 << a.mark().type().ordinal();
                            mergeUnlock(gains, unlocks, a.mark(), b.mark().type(), now);
                        }
                        if (passedB)
                        {
                            passedMask |= 1 << b.mark().type().ordinal();
                            mergeUnlock(gains, unlocks, b.mark(), a.mark().type(), now);
                        }
                        /*
                         * 刻意不 continue：若只有一侧经过，另一侧仍在排斥，本 tick 照常把它推开。
                         * 排斥力该守的正是「接触距离之内」这一档——原先在这里直接 continue，
                         * 等于在最需要挡人的地方把力关掉了。
                         */
                    }

                    /* 靠近：只有「本轮位图里没有对方那颗星」的一侧被推开 */
                    boolean pushA = a.mark().mobile()
                            && !b.mark().type().isUnlockedIn(effectiveMask(a.mark(), unlocks.get(a.mark()), now));
                    boolean pushB = b.mark().mobile()
                            && !a.mark().type().isUnlockedIn(effectiveMask(b.mark(), unlocks.get(b.mark()), now));
                    if ((!pushA && !pushB) || distance > REPULSION_RANGE)
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

                    /*
                     * 双向施力：施术者本人若也是这两颗星之一（自身标记），同样要吃推力——
                     * 否则跨序只表现为「对方被推开、自己纹丝不动」，测不出排斥是否成立。
                     */
                    Vec3 impulse = dir.scale(strength).multiply(1.0D, REPULSION_VERTICAL_SCALE, 1.0D);
                    if (pushA)
                    {
                        push(a.entity(), impulse, owner, true);
                    }
                    if (pushB)
                    {
                        push(b.entity(), impulse.scale(-1.0D), owner, true);
                    }
                }
            }

            flushUnlocks(owner, unlocks, gains, passedMask);
            renderGates(level, owner, gateCenters, now);
        }
    }

    // ======================== 内部 ========================

    /**
     * 画星域外环：每个圆心各一圈（跟随 + 固定并存时就是两圈）。
     * <p>
     * 与标记数量无关——只要域生效就画，所以身上没挂标记时也能看见「域已展开」。
     * 半径现取「结界尺寸调节器」的当前档位，因此调档时环会当场跟着涨缩。
     * 受 {@link StarMarkSkillLogic#RENDER_INTERVAL_TICKS} 节流，与标记火花的节奏一致。
     */
    private static void renderGates(ServerLevel level, ServerPlayer owner, List<Vec3> gateCenters, long now)
    {
        if (gateCenters.isEmpty() || now % StarMarkSkillLogic.RENDER_INTERVAL_TICKS != 0L)
        {
            return;
        }
        StarMarkType selected = PlayerStarMarkData.getSelected(owner);
        double radius = gateRadius(owner);
        for (Vec3 center : gateCenters)
        {
            StarMarkSkillLogic.spawnRing(level, center, radius, selected);
        }
    }

    /**
     * 本 tick 生效的星域圆心。跟随版以施术者当下位置为圆心，固定版锚在释放点。
     * <p>
     * 星域没有时限，因此这里兼作「该不该继续存在」的检查点：生得术式不再是星间飞行就当场解除，
     * 否则无限期的域会残留到别的术式里去。
     *
     * @return 可能为空（没有星域）、一个或两个圆心
     */
    private static List<Vec3> gateCenters(ServerPlayer owner, Vec3 ownerPos)
    {
        if (!PlayerTechniqueData.isLoveRendezvous(owner))
        {
            deactivateGate(owner);
            return List.of();
        }
        boolean following = isGateActive(owner);
        Vec3 fixed = fixedGateCenter(owner);
        if (fixed == null)
        {
            return following ? List.of(ownerPos) : List.of();
        }
        return following ? List.of(ownerPos, fixed) : List.of(fixed);
    }

    private static boolean withinAnyGate(List<Vec3> centers, Vec3 pos, double radiusSqr)
    {
        for (Vec3 center : centers)
        {
            if (pos.distanceToSqr(center) <= radiusSqr)
            {
                return true;
            }
        }
        return false;
    }

    /**
     * 把「接触到的星」并入某条标记在本轮星域实例中的解锁位，并把新增的位记进 {@code gains}。
     * <p>
     * 授予的位是有寿命的（{@link #UNLOCK_DECAY_TICKS}），而且<b>一轮内每颗星只授予一次</b>：
     * 过期即真正失去，贴着目标不动也不会被重新授予（见 {@link #grantUnlock}）。
     * <p>
     * 同一条标记一 tick 内可能接触多个目标，所以以 {@code unlocks} 里的中间数组为准累加，
     * 新增的位并入同一条标记在 {@code gains} 里的那一组。
     */
    private static void mergeUnlock(Map<StarMark, Integer> gains, Map<StarMark, long[]> unlocks,
                                    StarMark mark, StarMarkType contacted, long now)
    {
        long[] expiry = unlocks.get(mark);
        if (expiry == null)
        {
            return;
        }
        int before = effectiveMask(mark, expiry, now);
        grantUnlock(expiry, contacted, mark.type().initialUnlockMask(), now);
        int gained = effectiveMask(mark, expiry, now) & ~before;
        if (gained != 0)
        {
            gains.merge(mark, gained, (left, right) -> left | right);
        }
    }

    /**
     * 授予「某颗星及其顺序相邻」一个限时解锁；<b>一轮星域实例内只授予一次</b>。
     * <p>
     * 判定依据是「这位上是否曾经授予过」（{@code expiry[bit] != 0}，<b>含已过期</b>），
     * 而不是「是否已经过期」：过期就真的失去，不会因为目标还贴着你而被补发。
     * 否则「粘住不放」等于永久破解，{@link #UNLOCK_DECAY_TICKS} 这个窗口就形同虚设。
     * <p>
     * 初始位图里本来就有的星（自己的星 + 顺序相邻）不记录——那是与生俱来的通行权，
     * 记下来只会让存档里多一堆永远用不上的到期时刻。
     */
    private static void grantUnlock(long[] expiry, StarMarkType contacted, int initialMask, long now)
    {
        int first = Math.max(0, contacted.ordinal() - 1);
        int last = Math.min(StarMarkType.count() - 1, contacted.ordinal() + 1);
        for (int bit = first; bit <= last; bit++)
        {
            if ((initialMask & (1 << bit)) != 0 || expiry[bit] != 0L)
            {
                continue;
            }
            expiry[bit] = now + UNLOCK_DECAY_TICKS;
        }
    }

    /**
     * 把本 tick 累积的解锁写回星域实例，并在确实解锁了新东西时给一句动作栏反馈。
     * <p>
     * 落盘条件是「这位上曾经授予过」而不是「还有没过期的位」——过期记录必须留下，
     * 否则下一 tick 就会把同一颗星当成「从未授予」重新补发（见 {@link #grantUnlock}）。
     * 完全没有授予过的条目照旧删掉（顺带铲掉旧格式残留的 int 值），
     * 所以一旦授予完成就没有后续 NBT 写入，静止不动时是零写入。
     * <p>
     * 提示按<b>标记</b>分列「谁解锁了什么」，不把几条标记的新增合成一句话：合并之后
     * 「你」与「对方」的账会混在一起，看起来像自己凭空多解锁了几颗星。
     * <p>
     * 只在星域生效时被调用，因此条目清理也只在域展开期间发生——域解除时整个标签被丢弃。
     */
    private static void flushUnlocks(ServerPlayer owner, Map<StarMark, long[]> unlocks,
                                     Map<StarMark, Integer> gains, int passedMask)
    {
        CompoundTag data = owner.getPersistentData();
        CompoundTag store = data.contains(TAG_GATE_UNLOCKS) ? data.getCompound(TAG_GATE_UNLOCKS) : new CompoundTag();
        boolean dirty = false;
        for (Map.Entry<StarMark, long[]> entry : unlocks.entrySet())
        {
            String key = unlockKey(entry.getKey());
            if (hasAnyGrant(entry.getValue()))
            {
                if (!Arrays.equals(store.getLongArray(key), entry.getValue()))
                {
                    store.putLongArray(key, entry.getValue());
                    dirty = true;
                }
            }
            else if (store.contains(key))
            {
                store.remove(key);
                dirty = true;
            }
        }
        if (dirty)
        {
            data.put(TAG_GATE_UNLOCKS, store);
        }
        if (gains.isEmpty())
        {
            return;
        }
        owner.displayClientMessage(
                Component.literal("★经过 " + starNames(passedMask) + " · " + gainText(gains)
                        + "（" + (UNLOCK_DECAY_TICKS / 20) + " 秒）"), true);
    }

    /**
     * 「谁解锁了什么」：把每条标记新解锁的星按标记分列，形如
     * {@code ★Imai 解锁 ★Mimosa、★Ginan ｜ ★Mimosa 解锁 ★Imai}。
     */
    private static String gainText(Map<StarMark, Integer> gains)
    {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<StarMark, Integer> entry : gains.entrySet())
        {
            if (builder.length() > 0)
            {
                builder.append(" ｜ ");
            }
            builder.append(entry.getKey().type().displayName())
                    .append(" 解锁 ").append(starNames(entry.getValue()));
        }
        return builder.toString();
    }

    /** 位图里为 1 的星名，按顺序列举，如「★Mimosa 柠檬黄、★Ginan 薰衣草紫」。 */
    private static String starNames(int mask)
    {
        StringBuilder builder = new StringBuilder();
        for (StarMarkType type : StarMarkType.values())
        {
            if (!type.isUnlockedIn(mask))
            {
                continue;
            }
            if (builder.length() > 0)
            {
                builder.append('、');
            }
            builder.append(type.displayName());
        }
        return builder.toString();
    }

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

    /**
     * 该标记是否正处在「飞行途中」：实体存在且离地。
     * <p>
     * 制导的开关就用它，不另设计时器——被弹飞的球落地即结束制导，天然只覆盖「弹飞后那一段」，
     * 既不会出现「在地上跑还追着拐弯」，也不需要保存任何窗口状态（发射方无需记账）。
     * <p>
     * 方块锚点没有实体，恒为 {@code false}（本来也不参与移动）。
     */
    private static boolean isAirborne(StarMarkSkillLogic.ResolvedMark mark)
    {
        return mark.entity() != null && !mark.entity().onGround();
    }

    /**
     * 以增量速度推动目标；方块锚点（entity == null）与旁观 / 创造模式玩家跳过。
     *
     * @param allowOwner 是否允许推动施术者本人。
     *                   自身标记同样是星域里的一颗星，跨序斥力必须双向成立，所以跨序传 {@code true}；
     *                   同标记的质心吸引传 {@code false}——否则施术者会被自己挂的星拽着走。
     */
    private static void push(@Nullable Entity entity, Vec3 impulse, ServerPlayer owner, boolean allowOwner)
    {
        if (entity == null || entity.isRemoved() || !entity.isAlive())
        {
            return;
        }
        if (entity == owner && !allowOwner)
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
