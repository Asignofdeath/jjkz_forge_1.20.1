package com.darkness.jujutsukaisenzone.technique.skill;

import com.darkness.jujutsukaisenzone.player.PlayerStarMarkData;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import com.darkness.jujutsukaisenzone.player.StarMark;
import com.darkness.jujutsukaisenzone.player.StarMarkType;
import com.darkness.jujutsukaisenzone.technique.BarrierTechniqueServerLogic;
import com.darkness.jujutsukaisenzone.technique.TechniqueDamageSources;
import com.darkness.jujutsukaisenzone.technique.development.SkillUnlockLogic;
import com.darkness.jujutsukaisenzone.technique.skillset.LoveRendezvousTechniqueSkillSet;
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
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 星间飞行的三态标记规则，只在服务端结算。
 * <p>
 * <b>同标记</b>：互相吸引（常态向质心靠拢；牵引期间改为向施术者靠拢）。被弹飞出去的标记在落回地面之前，
 * 还会额外吃一份「制导」横向加力，朝同星拐一点——「踢足球」的手感来源，见 {@link #GUIDANCE_IMPULSE}。
 * <b>撞击伤害</b>：被弹飞的球撞上另一条标记时双方各受一次「速度 × 系数」的术式伤害
 * （见 {@link #resolveImpacts}）——这才是弹射机制的收益点，缓慢靠拢的同星相吸撞不痛。<br>
 * <b>引力弹弓</b>：被弹飞的球掠过<b>同星方块</b>时被那颗「大质量天体」加速并刷新球身份
 * （见 {@link #slingBalls}）。方块撞不出伤害，本该是条死路，这里把它改写成加速带。<br>
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
 * <b>跨序斥力 = 芝诺墙</b>（见 {@link #zenoInwardCap}）。它约束的对象是<b>位移</b>而不是力：
 * 每 tick 把「朝对方靠拢的那一段位移」压进一个上限，上限随距离线性趋零，于是距离的演化是
 * {@code dd/dn = -k·(d - ZENO_CORE)}——指数逼近、<b>永远差最后 {@link #ZENO_CORE} 格</b>。
 * 无论对方多快（僵尸寻路、玩家疾跑、鞘翅俯冲）都压得住，因为服务端放行的从来不是速度，
 * 而是「这一 tick 允许靠近多少」。这与「把推力调大」是两回事：调大只是让力气大的一方赢，
 * 且对位置由客户端权威的玩家根本不起作用。
 * <p>
 * <b>「爱与幽会」领域展开</b>（{@code LoveRendezvousDomainExpansion}）不改上面这三态本身，
 * 而是在其之上加一条领域级重定义：
 * <ul>
 *   <li><b>引爆的传播升级成连锁</b>——规则与切换点都在 {@code StarMarkDetonation#spreadMark}
 *       （按爆心是否落在域内自动分两套，域外行为完全不变），本类只提供
 *       {@link #isRendezvousDomainActive} / {@link #insideRendezvousDomain} 这两个判据。</li>
 *   <li><b>星星大碰撞改为完全制导</b>（见 {@link #homeBalls}：每 tick 改写速度方向 ⇒ 必中）。
 *       它存在的意义是让连锁的每一环都不落空——链条靠一次成功的撞击推进，命中率若是随机的，
 *       雪球会自己散掉。（球在飞行中豁免芝诺墙，所以必中不会被跨序斥力吃掉。）</li>
 * </ul>
 * 两者都由 {@link #activateRendezvousDomain} 登记的球体界定，域一关就整体失效——它们不是新的常态规则。
 * <p>
 * 三条实现纪律：
 * <ul>
 *   <li>力度一律用**增量**速度（{@code setDeltaMovement(dm.add(impulse))}），不覆盖既有速度，
 *       否则会和 EpicFight 的移动 / 闪避互相吃掉。<b>三处例外</b>：跨序斥力压的是位移
 *       （见 {@link #zenoWall}），「爱与幽会」域内的完全制导直接改写速度方向
 *       （见 {@link #homeBalls}），「引力弹弓」也改写水平速度方向并夹上速限
 *       （见 {@link #slingBalls}）——三者都是结构性动作，不是常规施力。</li>
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
    /**
     * 「爱与幽会」领域（领域展开）下，完全制导的<b>兜底水平航速</b>（格/tick）。
     * <p>
     * 域内飞行的球每 tick 被<b>直接改写水平速度的方向</b>去对准目标（见 {@link #homeBalls}），
     * 速度取「当前水平速度」与本值的较大者。之所以必须有下限：地面摩擦每 tick 吃掉约 45%，
     * 球在地上滑三四 tick 就会掉到 {@link #IMPACT_MIN_SPEED} 之下——那时连撞击都不再结算，
     * 「必中」就成了空话。取 0.60：略高于撞击门槛，又不至于变成瞬移。
     * <p>
     * 域外不受影响：常态的制导依旧只是 {@link #GUIDANCE_IMPULSE} 那份固定横向加力，
     * 「辅助而非必中」的手感在域外原样保留。
     */
    private static final double DOMAIN_HOMING_SPEED = 0.60D;
    /**
     * 「引力弹弓」的影响球半径（格）：球与同星方块锚点的距离进到这个范围内即触发。
     * <p>
     * 类比航天器借力：方块是静态的大质量天体，被弹飞的球是航天器。撞击结算只认
     * {@link LivingEntity}（方块撞不出伤害，见 {@link #resolveImpacts}），所以球掠过方块本该是条死路——
     * {@link #nearestStrikeable} 甚至刻意把方块排除在制导目标之外。本机制把这条死路改写成加速带。
     * <p>
     * 取 2.5：比 {@link #BURST_RANGE}（4.0，弹射分家）小、比 {@link #MIN_SEPARATION}（1.5）大。
     * 再大就变成「到处是自动加速带」，失去「贴着天体掠过」的观感。
     * <p>
     * 下面四个 {@code SLING_*} 常量取 {@code public} 只为让技能描述能注入它们
     * （见 {@code TechniqueSkillDescriptions#registerLoveRendezvousSkills}），
     * 与 {@link #MARK_RANGE} 同一立场——不是给外部改的旋钮。
     */
    public static final double SLING_RANGE = 2.5D;
    /**
     * 「引力弹弓」的加速量（格/tick，<b>加法</b>）。
     * <p>
     * 为什么用加法：满级初速已经是 {@code BURST_IMPULSE_NEAR(1.40) × burstStackScale(MAX_STACKS)(2.0) = 2.8}，
     * 而速度是撞击伤害<b>唯一</b>的乘数（{@link #IMPACT_DAMAGE_PER_SPEED}）。乘法会让一次借力
     * 直接把伤害推到失控；加法可控，且对「本来就慢的球」帮助更大。
     */
    public static final double SLING_BOOST = 0.6D;
    /**
     * 弹弓加速后的水平速度上限（格/tick）——<b>硬要求</b>。
     * <p>
     * 速度直接换算伤害（{@code 速度 × 12}，见 {@link #IMPACT_DAMAGE_PER_SPEED}），不封顶就不是
     * 「更疼」而是「一记即死」。取 3.4：比满级初速 2.8 略高，正好留一次借力的余量（约 41 点）。
     */
    public static final double MAX_BALL_SPEED = 3.4D;
    /**
     * 弹弓附带的轻微上抛（格/tick）。
     * <p>
     * 「续航」不只是刷新 {@link #TAG_BURST_UNTIL}：只刷新标签、球却贴地磨停，那只是延长了一段
     * 「趴着不动」。这一记上抛把球重新掀离地面（落地摩擦每 tick 吃掉约 45%，离地后只衰减 9%）。
     * 比 {@link #BURST_LIFT}（0.42）更轻——借力是中途补给，不是又一次发射。
     */
    private static final double SLING_LIFT = 0.30D;
    /**
     * 同一颗球整段飞行里能被借力的次数上限。
     * <p>
     * 防「几颗同星方块接力、球永远不落地」。它与「同一方块只弹一次」（见 {@link #TAG_SLING_USED}）
     * 是两道独立的闸：前者限总量，后者防球停在影响球里每 tick 重弹。
     */
    public static final int SLING_MAX_PER_BALL = 2;
    /** 牵引的每 tick 增量速度。 */
    private static final double PULL_IMPULSE = 0.070D;
    /** 跨序斥力的作用距离（格）：同时是芝诺墙的外沿，见 {@link #ZENO_INWARD_CAP}。 */
    private static final double REPULSION_RANGE = 6.0D;
    /**
     * 芝诺墙的硬底线（格）——可接近量的收敛目标：双方<b>永远差这一点</b>。
     * <p>
     * 距离的演化是 {@code d(n) - ZENO_CORE = (d(0) - ZENO_CORE)·(1-k)ⁿ}，指数逼近、永不归零，
     * 所以它不是一堵会被撞开的墙，而是一道「越靠近越硬、硬到无穷」的墙。
     * <p>
     * ⚠️ <b>必须明显大于 {@link #CONTACT_RANGE}（2.5），并且留出「单 tick 位移」的余量。</b>
     * 接触判定读的是<b>本 tick 修正之前</b>的位置（锚点在 {@code resolveAll} 里就算好了），
     * 而位移是每 tick 只削一次的离散量：只要一步能跨过余量，那一 tick 就会先擦到接触线、
     * 下一 tick 才被推回去——「磨蹭着贴上去」正是这样磨出来的。取 3.0 = 接触线 + 0.5：
     * 疾跑跳跃的水平位移约 0.36 格/tick，跨不过这条余量；能跨过去的只有碰撞击飞 / 爆炸 /
     * 鞘翅俯冲那个量级，而那些场合本来也不该由墙来管。
     * <p>
     * <b>调高不会锁死跨序链</b>：接触判定要求「对方那颗星本来就在自己的位图里」
     * （见 {@link #effectiveMask}），也就是这一侧本来就不吃墙，所以「逐级经过」永远够得着目标，
     * 「飞行，穿越」的依次穿越也不经过接触判定。墙真正封死的只有<b>被排斥的那一侧</b>——
     * 那正是「禁行」的字面意思。
     * <p>
     * 这个旋钮就是「禁行有多严」：调高 = 挡得越远；调到 {@link #CONTACT_RANGE} 以下
     * 等于默许接触（旧值 2.0 的毛病）。
     */
    private static final double ZENO_CORE = 3.0D;
    /**
     * 芝诺墙在外沿（{@link #REPULSION_RANGE}）处允许的最大朝内速度（格/tick）。
     * <p>
     * 取 0.20：略低于僵尸等地面生物的寻路速度，所以「一进斥力圈就被拖住」。
     * 但它只是<b>外沿</b>的上限——越往里上限越低，到 {@link #ZENO_CORE} 处归零。
     * 玩家步行（约 0.11 格/tick）在外沿几乎无感、越走越涩；疾跑（约 0.28）从外侧就被压住。
     * <p>
     * 圈厚 = {@code REPULSION_RANGE - ZENO_CORE}，当前 3.0 格；核心取大后这一段变短，
     * 于是「被拖住」的手感出现得更早、也更陡——这是 {@link #ZENO_CORE} 那两个格的直接后果，
     * 不是这里的系数变了。
     */
    private static final double ZENO_INWARD_CAP = 0.20D;
    /**
     * 芝诺墙的逼近阶：1.0 = 一阶线性衰减，即上面那条微分方程。
     * <p>
     * 调大会让「越靠近越涩」更陡（贴着边滑行也更难），调小则墙更软、卡点更靠外。
     */
    private static final double ZENO_EXP = 1.0D;
    /**
     * 单 tick 最多退回多少格，防止「被末影珍珠 / 瞬移塞进核心」时出现一次暴力弹射。
     * <p>
     * 正常推挤的越界量只有零点几格，这个上限根本碰不到；它只兜极端穿越。
     */
    private static final double ZENO_MAX_CORRECT = 1.0D;
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
    /**
     * 这颗球已经借过力的方块（{@code long[]}，元素为 {@code BlockPos#asLong()}）。
     * <p>
     * 与 {@link #TAG_BURST_UNTIL} / {@link #TAG_IMPACT_AT} 一样存在<b>球自己</b>身上。两个作用：
     * ①「同一方块只弹一次」——否则球停在影响球内会每 tick 被弹一次；
     * ② 数组长度天然就是「借了几次」的计数，无需另存一个 int。
     * <p>
     * 每次被重新踢出（见 {@link #markAsBall}）即整个清空——新的一脚是新的额度，
     * 否则同一只生物第二次飞行会因为「方块都用过」而借不到力。
     */
    private static final String TAG_SLING_USED = "jujutsukaisenzone_love_rendezvous_sling_used";

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
                    markAsBall(mover.entity(), now + BURST_FLIGHT_TICKS);
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
        markAsBall(mover, gameTime(owner) + BURST_FLIGHT_TICKS);
    }

    /**
     * 给实体盖上「被弹飞出去的球」的身份，并清空它上一轮的弹弓记账。
     * <p>
     * 两件事必须一起做：{@link #TAG_BURST_UNTIL} 是球身份（撞击结算与域内制导都认它），
     * {@link #TAG_SLING_USED} 是这颗球的借力账本。同一只生物会被反复踢（先被展开弹开、
     * 再被终结技踢飞），若沿用旧账，第二次飞行会因为「方块都用过 / 次数已满」而借不到力。
     * 新的一脚 = 新份额度。
     */
    private static void markAsBall(Entity entity, long until)
    {
        CompoundTag data = entity.getPersistentData();
        data.putLong(TAG_BURST_UNTIL, until);
        data.remove(TAG_SLING_USED);
    }

    /**
     * 「飞行，穿越」<b>路径上</b>的一次撞击：把挡路的实体沿飞行方向撞开。
     * <p>
     * 与 {@link #flightKick} 的两点差别都是刻意的：
     * <ul>
     *   <li><b>力度由调用方给</b>——路径撞击是位移途中的顺带一下，用的是固定的小额度，
     *       而不是「终结技最后一下」那种 {@code BURST_IMPULSE_NEAR × 满级倍率}；</li>
     *   <li><b>不盖 {@link #TAG_BURST_UNTIL}</b>——「球」的身份只由 {@link #flightKick} 授予。
     *       挡路的倒霉蛋只是被撞飞，不该因此获得撞击伤害的资格。</li>
     * </ul>
     * 方向与上抛沿用 {@link #burstKick}，所以观感与两套弹射同源：水平撞飞 + 一记固定上抛。
     *
     * @param from 这一撞的发力点（上一 tick 施术者所在的位置）
     */
    public static void flightRam(ServerPlayer owner, @Nullable Entity victim, Vec3 from, double impulse)
    {
        if (victim == null || victim.isRemoved() || !victim.isAlive())
        {
            return;
        }
        push(victim, burstKick(from, victim.position(), impulse), owner, true);
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
     * <b>「爱与幽会」域内还会多一步：引爆「被撞的那一个」身上的标记</b>（见方法内的 {@code detonating}），
     * 连锁由这一下展开——引爆会把新的满级可燃点传染到周围，玩家再去踢下一个。
     * 球自己<b>不炸</b>：它是这一脚踢出去的武器，不是挂在球上的炸药；撞完就摘掉球身份，
     * 一颗球只换一击。
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

            /*
             * 「爱与幽会」领域：这一记撞击额外引爆「被撞的那一个」身上的标记，连锁由此展开。
             * 判据与完全制导同一口径（域生效 + 球落在域内），所以「球是被域内那一脚踢出来的」这点是精确成立的。
             */
            boolean detonating = isRendezvousDomainActive(owner)
                    && insideRendezvousDomain(owner, ball.position());

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
                if (detonating)
                {
                    /*
                     * 只引爆「被撞的那一个」——球是这一脚踢出去的武器，不是挂在球上的炸药，
                     * 所以球自己身上的标记留着（它下一 tick 就摘掉球身份，回到普通目标）。
                     * 连锁靠「引爆把新的满级可燃点传染出去、玩家再踢下一个」推进，不靠一颗球连环炸。
                     * 摘掉标签 = 结束这一颗球的使命（不再被制导、不再触发撞击）：一颗球只换一击。
                     */
                    detonateIfMarked(owner, other);
                    ball.getPersistentData().remove(TAG_BURST_UNTIL);
                }
                if (dealt)
                {
                    owner.displayClientMessage(Component.literal("★撞击 · "
                            + ballMark.mark().type().displayName() + " ⇄ "
                            + other.mark().type().displayName()
                            + " 各受 " + Math.round(damage) + " 伤害"
                            + (detonating ? "，「爱与幽会」引爆该标记并传染" : "")), true);
                }
                /* 一颗球一次只撞一个目标；贴近不散的情况交给 IMPACT_COOLDOWN_TICKS 去抖 */
                break;
            }
        }
    }

    /**
     * 引爆一条标记——先确认它还在施术者名下，再交给 {@link StarMarkDetonation}。
     * <p>
     * 这道确认是必要的：{@code marks} 是本 tick 开头的解析快照，而同一 tick 里另一颗球完全可能
     * 已经把这条标记引爆掉了（它是那颗球的撞击目标）。{@link StarMarkDetonation#detonate} 并不校验
     * 标记是否仍然存在——重复调用会把伤害、效果与传染再放一遍。
     */
    private static void detonateIfMarked(ServerPlayer owner, StarMarkSkillLogic.ResolvedMark mark)
    {
        if (PlayerStarMarkData.findTarget(owner, mark.mark()) == null)
        {
            return;
        }
        StarMarkDetonation.detonate(owner, mark.mark(), mark.position());
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

    /**
     * 施术者此刻是否<b>站在自己的星域里</b>（跟随域或固定域任一覆盖到脚下）。
     * <p>
     * 「飞行，穿越」靠它决定要不要走「依次穿越已解锁星位」那条强化链路：域是那套进度的载体，
     * 只判「域是否生效」并不够——固定域可以留在原地，人跑开了就不该再算「在自己星域中」。
     * 半径口径与域内的禁行判定完全一致（{@link #gateRadius(ServerPlayer)}）。
     * <p>
     * 跟随域恒为真（它的圆心就是施术者脚下），所以这条判定真正过滤的只有「固定域留在别处」那一种。
     */
    public static boolean insideOwnGate(ServerPlayer player)
    {
        if (!isGateActive(player) && !isFixedGateActive(player))
        {
            return false;
        }
        Vec3 pos = player.position();
        double radius = gateRadius(player);
        return withinAnyGate(gateCenters(player, pos), pos, radius * radius);
    }

    // ======================== 「爱与幽会」领域展开 ========================

    /**
     * 「爱与幽会」领域（领域展开）的作用球体：施术者 UUID → 球心与半径。
     * <p>
     * 球心取<b>展开那一刻</b>的领域方块中心，半径取领域固定半径
     * （{@code AbstractDomainExpansion.DOMAIN_RADIUS}）。领域本身不移动，登记一次就够。
     * <p>
     * <b>刻意放在内存里而不是玩家 NBT</b>：领域实例本身就是内存态（掉线、崩服都会消失），
     * 强化跟着它同生共死才不会留下「没有领域、却仍在必中 / 仍在连锁」的幽灵状态——这一点与
     * {@link #ZENO_PREV} / {@code StarFlightSkillLogic#FLIGHTS} 同一立场。
     * <p>
     * 读取它的有两处：本类的「星星大碰撞改走完全制导」（{@link #homeBalls} / {@link #resolveImpacts}），
     * 以及 {@code StarMarkDetonation} 的引爆传染（判断这一场爆炸该走域内连锁规则还是域外默认规则）。
     * 两处都只服务施术者自己那一份数据（{@link #apply} 与引爆本就按施术者跑），因此天然只有一项。
     */
    private static final Map<UUID, RendezvousDomain> RENDEZVOUS_DOMAINS = new ConcurrentHashMap<>();

    /** 领域的球形作用域：球心 + 半径（格）。 */
    private record RendezvousDomain(Vec3 center, double radius)
    {
    }

    /**
     * 「爱与幽会」领域展开完成：登记作用球体。
     * <p>
     * 登记之后域内两套规则随之生效，它们分别在各自的读取方落地：
     * <ul>
     *   <li><b>引爆传染升级成连锁</b>——{@code StarMarkDetonation#detonate} 每次引爆都会来查
     *       {@link #isRendezvousDomainActive} + {@link #insideRendezvousDomain} 决定走哪套；</li>
     *   <li><b>星星大碰撞完全制导</b>——{@link #homeBalls} 在 {@link #apply} 里每 tick 读一次。</li>
     * </ul>
     */
    public static void activateRendezvousDomain(ServerPlayer owner, Vec3 center, double radius)
    {
        RENDEZVOUS_DOMAINS.put(owner.getUUID(), new RendezvousDomain(center, radius));
    }

    /** 领域关闭（或换术式）：撤销强化。 */
    public static void deactivateRendezvousDomain(ServerPlayer owner)
    {
        RENDEZVOUS_DOMAINS.remove(owner.getUUID());
    }

    /**
     * 施术者的「爱与幽会」领域是否生效。
     * <p>
     * 顺带做一次术式校验：换了生得术式就不再是星间飞行的领域，与 {@link #gateCenters} 同一个立场。
     */
    public static boolean isRendezvousDomainActive(ServerPlayer owner)
    {
        if (!RENDEZVOUS_DOMAINS.containsKey(owner.getUUID()))
        {
            return false;
        }
        if (!PlayerTechniqueData.isLoveRendezvous(owner))
        {
            deactivateRendezvousDomain(owner);
            return false;
        }
        return true;
    }

    /**
     * 某点是否落在施术者的「爱与幽会」领域球内；未展开时恒为 {@code false}。
     * <p>
     * <b>公开给引爆侧</b>：{@code StarMarkDetonation#detonate} 靠它决定这一场爆炸走域外的默认
     * 传染规则、还是域内的连锁规则。刻意与 {@link #isRendezvousDomainActive} 分成两个方法——
     * 前者管「有没有域」（并顺带做术式校验），后者管「这一点在不在域里」；凡是需要<b>按位置</b>判定的
     * 调用点（制导、撞击、引爆）都两个一起调。
     */
    public static boolean insideRendezvousDomain(ServerPlayer owner, Vec3 pos)
    {
        RendezvousDomain domain = RENDEZVOUS_DOMAINS.get(owner.getUUID());
        if (domain == null)
        {
            return false;
        }
        return pos.distanceToSqr(domain.center()) <= domain.radius() * domain.radius();
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
     * <p>
     * 对外只被「飞行，穿越」用一次，而且必须在 {@link #deactivateGate} <b>之前</b>读——
     * 通行证就存在那一轮星域里，关域会把整份记录删掉。读一次就取到快照，之后与星域无关。
     */
    public static int unlockMaskOf(ServerPlayer player, StarMark mark)
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
         * 「引力弹弓」：域外飞行的球掠过同星方块时被加速 + 续航。同样必须排在撞击结算之前——
         * 这一 tick 改写过的速度就是撞击扫掠判定读到的那一段位移。
         *
         * 与下面的域内制导是<b>互斥分支</b>：域内的定位是「必中终结」，与「续航」语义互斥
         * （域内撞击成功即摘掉球身份、一颗球只换一击，见 resolveImpacts），两者同时生效会互相抵消。
         * 做成 if/else 而不是两个独立 if，是为了让「球只会被其中一边接管」在代码结构上就成立。
         */
        if (isRendezvousDomainActive(owner))
        {
            /*
             * 「爱与幽会」领域：域内飞行的球改为完全制导（必中）。
             */
            homeBalls(owner, marks);
        }
        else if (isSlingUnlocked(owner))
        {
            /*
             * 域外弹弓是<b>机制型技能</b>：没解锁就整条分支不进，球照旧只会被同星吸引慢慢拽过去。
             * 判定放在调用点而不是 {@link #slingBalls} 内部——没解锁时连「遍历球、找同星方块」这一趟
             * 扫描都不必做。
             */
            slingBalls(owner, marks);
        }

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

                    /*
                     * 双向约束：施术者本人若也是这两颗星之一（自身标记），同样要吃约束——
                     * 否则跨序只表现为「对方被挡住、自己纹丝不动」，测不出排斥是否成立。
                     * dir 由 A 减 B 得到，所以各自拿到的都是「远离对方」的外向向量。
                     */
                    if (pushA)
                    {
                        zenoWall(a.entity(), dir, distance, now);
                    }
                    if (pushB)
                    {
                        zenoWall(b.entity(), dir.scale(-1.0D), distance, now);
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
     * 「爱与幽会」领域的<b>完全制导</b>：域内正在飞的球，每 tick 把水平速度直接掰向最近的另一条标记。
     * <p>
     * 与常态 {@link #GUIDANCE_IMPULSE} 的差别是<b>性质</b>而非程度：常态是「加一份固定的横向力」，
     * 于是速度越快偏角越小、本质是辅助；这里直接<b>改写速度的方向</b>，球飞出多远、多快，每 tick 都
     * 重新对准一次——所以是必中，而不是「更容易打中」。这正是领域该有的量级差。
     * <p>
     * 三条边界：
     * <ul>
     *   <li><b>只制导「球」</b>——判据与撞击结算完全一致（{@link #TAG_BURST_UNTIL} 未过期）。
     *       平时被同星吸引慢慢蹭的标记不会被抽走，域外的手感不受影响；</li>
     *   <li><b>球必须在域内</b>——领域是那片场地。球飞出球面即失去制导，不会满世界追人；</li>
     *   <li><b>只瞄「能挨撞击的目标」</b>——见 {@link #nearestStrikeable}。把球导向一块石头
     *       等于让它提前失速（方块撞不出伤害），所以方块锚点与掉落物都不作数。</li>
     * </ul>
     * 竖直分量原样保留：上抛（{@link #BURST_LIFT}）与重力照旧，制导只负责「往哪儿去」。
     * 水平速度取「当前值」与 {@link #DOMAIN_HOMING_SPEED} 的较大者——在地面上被摩擦拖慢时兜住速度，
     * 否则球会滑到 {@link #IMPACT_MIN_SPEED} 之下、连撞击都不再结算。
     * <p>
     * <b>球是玩家时这条制导形同虚设</b>：服务端改不动玩家的 {@code deltaMovement}（位置由客户端权威），
     * 与 {@link #push} 的立场一致。这不是缺口——被弹飞的是玩家时，他本来就该自己决定往哪儿走。
     */
    private static void homeBalls(ServerPlayer owner, List<StarMarkSkillLogic.ResolvedMark> marks)
    {
        long now = owner.level().getGameTime();
        for (StarMarkSkillLogic.ResolvedMark ball : marks)
        {
            Entity entity = ball.entity();
            /* 方块锚点没有实体、掉落物不是 LivingEntity——都当不了球 */
            if (!(entity instanceof LivingEntity))
            {
                continue;
            }
            if (entity.getPersistentData().getLong(TAG_BURST_UNTIL) <= now)
            {
                continue;
            }
            if (!insideRendezvousDomain(owner, entity.position()))
            {
                continue;
            }

            StarMarkSkillLogic.ResolvedMark goal = nearestStrikeable(ball, marks);
            if (goal == null)
            {
                continue;
            }
            Vec3 to = goal.position().subtract(entity.position());
            Vec3 flat = new Vec3(to.x, 0.0D, to.z);
            double length = flat.length();
            if (length < 1.0E-6D)
            {
                /* 已经正对着（水平重合）：这一 tick 不需要拐，让它自己撞上去 */
                continue;
            }
            Vec3 direction = flat.scale(1.0D / length);
            Vec3 velocity = entity.getDeltaMovement();
            double speed = Math.max(
                    Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z),
                    DOMAIN_HOMING_SPEED);
            entity.setDeltaMovement(direction.x * speed, velocity.y, direction.z * speed);
            entity.hurtMarked = true;
            entity.hasImpulse = true;
            entity.fallDistance = 0.0F;
        }
    }

    /**
     * 完全制导的目标：离球最近的、<b>撞得响</b>的另一条标记。
     * <p>
     * 只要 {@link LivingEntity}：撞击判定本身就跳过方块与掉落物（没有生命值、拿不到实体），
     * 把球导向它们只会让它提前失速。与星种无关——撞击本来就不看双方是不是同一颗星，
     * 「同星相吸」是另一套机制（制导只负责把这一脚送到最近的靶子上）。
     */
    @Nullable
    private static StarMarkSkillLogic.ResolvedMark nearestStrikeable(
            StarMarkSkillLogic.ResolvedMark ball, List<StarMarkSkillLogic.ResolvedMark> marks)
    {
        return nearestStrikeableFrom(ball.position(), ball.entity(), marks);
    }

    /**
     * 从 {@code origin} 出发，找最近的、<b>撞得响</b>的另一条标记。
     * <p>
     * 两个调用方共用同一口径：{@link #homeBalls} 拿球自己的位置当原点（域内必中），
     * {@link #slingExit} 拿方块锚点当原点（域外借力后要挑「下一个靶子」）。
     *
     * @param exclude 要排除的实体（球自己）；方块锚点传 {@code null} 即不排除任何实体
     */
    @Nullable
    private static StarMarkSkillLogic.ResolvedMark nearestStrikeableFrom(
            Vec3 origin, @Nullable Entity exclude, List<StarMarkSkillLogic.ResolvedMark> marks)
    {
        StarMarkSkillLogic.ResolvedMark nearest = null;
        double best = Double.MAX_VALUE;
        for (StarMarkSkillLogic.ResolvedMark other : marks)
        {
            if (other.entity() == exclude || !(other.entity() instanceof LivingEntity))
            {
                continue;
            }
            double distance = other.position().distanceToSqr(origin);
            if (distance < best)
            {
                best = distance;
                nearest = other;
            }
        }
        return nearest;
    }

    /**
     * 施术者是否已解锁「引力弹弓」这个<b>机制型技能</b>。
     * <p>
     * 机制型技能不占槽位、没有自己的咒力与冷却，唯一的状态就是那个解锁标记，所以运行时门控只看这一条
     * （技能定义见 {@code LoveRendezvousTechniqueSkillSet#SKILL_STAR_SLING}）。术式 id 写死星间飞行——
     * 本类是它的专属力学实现，{@code apply} 收到的就是这一套标记。
     * <p>
     * 走 {@link SkillUnlockLogic#isSkillUnlockedById} 而不是自己读 tag：节点 -&gt; 技能定义的解析在那边
     * 统一收口，这里就少一份「tag 名写错却没人发现」的机会。
     */
    private static boolean isSlingUnlocked(ServerPlayer owner)
    {
        return SkillUnlockLogic.isSkillUnlockedById(
                owner,
                PlayerTechniqueData.TECHNIQUE_LOVE_RENDEZVOUS,
                LoveRendezvousTechniqueSkillSet.SKILL_STAR_SLING);
    }

    /**
     * 「引力弹弓」：域外飞行的球掠过<b>同星方块</b>时，被那颗「大质量天体」加速并续命。
     * <p>
     * 动机是把死路改写成玩法：撞击结算只认 {@link LivingEntity}，方块撞不出伤害，
     * 所以被同星吸过去的球只会停在方块旁干等标签过期。本方法给方块补上它缺失的角色——
     * <b>方块不是靶子，是加速带</b>。
     * <p>
     * 触发要同时满足四条：球身份未过期、借力次数没用完、有<b>同星</b>方块落在
     * {@link #SLING_RANGE} 内、且那颗方块这一轮还没被这颗球借过。结算三件事：
     * 水平速度按 {@link #SLING_BOOST} 加并夹到 {@link #MAX_BALL_SPEED}，方向朝「下一个能撞的目标」
     * （见 {@link #slingExit}），再补一记 {@link #SLING_LIFT} 上抛，最后刷新 {@link #TAG_BURST_UNTIL}。
     * <p>
     * <b>只认同星</b>：方块是你主动摆下的天体，摆哪颗星由你决定；若任意方块都作数，
     * 全图都会变成加速带，这个机制就失去了「刻意布场」的意味。
     * <p>
     * <b>解锁门槛</b>：这是<b>机制型技能</b>，未解锁时本方法根本不会被调用（门控见 {@link #isSlingUnlocked}）；
     * 解锁之后永久生效，不再消耗咒力、也没有冷却——所以玩家的学习顺序是「先学会弹射，再学会借力」，
     * 而不是一上手就默认拥有。
     * <p>
     * ⚠️ <b>前提待验收</b>：方块锚点在方块正上方 {@code BLOCK_MARK_HEIGHT}（1.15）格，球瞄的是这个
     * 空中点，所以「掠过而不撞上」在<b>锚点意义上</b>成立；但原版物理碰撞仍在——方块若是实心块、
     * 球又贴地飞，球箱会怼在方块侧面停住。这是本机制唯一需要在实机上确认的前提。
     */
    private static void slingBalls(ServerPlayer owner, List<StarMarkSkillLogic.ResolvedMark> marks)
    {
        long now = owner.level().getGameTime();
        for (StarMarkSkillLogic.ResolvedMark ballMark : marks)
        {
            Entity ball = ballMark.entity();
            /* 方块锚点没有实体、掉落物不是 LivingEntity——都当不了球 */
            if (!(ball instanceof LivingEntity))
            {
                continue;
            }
            CompoundTag data = ball.getPersistentData();
            if (data.getLong(TAG_BURST_UNTIL) <= now)
            {
                continue;
            }
            long[] used = data.getLongArray(TAG_SLING_USED);
            if (used.length >= SLING_MAX_PER_BALL)
            {
                continue;
            }
            StarMarkSkillLogic.ResolvedMark well = nearestGravityWell(ballMark, marks);
            if (well == null)
            {
                continue;
            }
            long key = well.mark().pos().asLong();
            if (containsLong(used, key))
            {
                continue;
            }
            Vec3 direction = slingExit(ballMark, well, marks);
            if (direction == null)
            {
                continue;
            }

            Vec3 velocity = ball.getDeltaMovement();
            double speed = Math.min(horizontalSpeed(ball) + SLING_BOOST, MAX_BALL_SPEED);
            ball.setDeltaMovement(direction.x * speed, velocity.y + SLING_LIFT, direction.z * speed);
            ball.hurtMarked = true;
            ball.hasImpulse = true;
            ball.fallDistance = 0.0F;

            /* 记这一笔（方块 + 次数共用同一个数组），并把球的身份续到下一个窗口 */
            data.putLongArray(TAG_SLING_USED, appendLong(used, key));
            data.putLong(TAG_BURST_UNTIL, now + BURST_FLIGHT_TICKS);

            owner.displayClientMessage(Component.literal("★引力弹弓 · 借「"
                    + well.mark().type().displayName() + "」方块加速至 "
                    + Math.round(speed * 100.0D) / 100.0D + " 格/刻，续航刷新（"
                    + (used.length + 1) + "/" + SLING_MAX_PER_BALL + "）"), true);
        }
    }

    /**
     * 离球最近的、落在 {@link #SLING_RANGE} 内的<b>同星方块</b>标记——弹弓里的那颗大质量天体。
     * <p>
     * 与 {@link #nearestStrikeable} 恰成互补：那边只认 {@link LivingEntity}（撞得响的靶子），
     * 这边只认方块（借得动的天体）。两者合起来正好把「三类标记」的角色分完。
     */
    @Nullable
    private static StarMarkSkillLogic.ResolvedMark nearestGravityWell(
            StarMarkSkillLogic.ResolvedMark ball, List<StarMarkSkillLogic.ResolvedMark> marks)
    {
        StarMarkSkillLogic.ResolvedMark nearest = null;
        double best = SLING_RANGE * SLING_RANGE;
        for (StarMarkSkillLogic.ResolvedMark other : marks)
        {
            if (other.mark().kind() != StarMark.Kind.BLOCK
                    || other.mark().type() != ball.mark().type())
            {
                continue;
            }
            double distance = other.position().distanceToSqr(ball.position());
            if (distance < best)
            {
                best = distance;
                nearest = other;
            }
        }
        return nearest;
    }

    /**
     * 借力后的航向：从方块锚点指向「下一个能撞的目标」，找不到目标时退回球原本的航向。
     * <p>
     * <b>先找目标</b>：方块是路标而不是终点，所以借力不只是踩油门，还把球重新瞄准下一个靶子——
     * 摆一排同星方块、再放几颗对岸的星，球就会一路借着飞过去。这正是「弹弓」而非「加速带」。
     * <b>找不到也不空转</b>：没有可撞目标时沿原航向继续（只加速、不改向）。若在这里返回零向量，
     * 球会当场停住——那比不加速更糟。
     *
     * @return 单位水平航向；既无目标、球又原本静止时返回 {@code null}（这一 tick 作废）
     */
    @Nullable
    private static Vec3 slingExit(StarMarkSkillLogic.ResolvedMark ball,
            StarMarkSkillLogic.ResolvedMark well, List<StarMarkSkillLogic.ResolvedMark> marks)
    {
        StarMarkSkillLogic.ResolvedMark next = nearestStrikeableFrom(well.position(), ball.entity(), marks);
        if (next != null)
        {
            Vec3 to = next.position().subtract(well.position());
            Vec3 flat = new Vec3(to.x, 0.0D, to.z);
            if (flat.lengthSqr() > 1.0E-6D)
            {
                return flat.normalize();
            }
        }
        Entity entity = ball.entity();
        if (entity == null)
        {
            return null;
        }
        Vec3 velocity = entity.getDeltaMovement();
        Vec3 flat = new Vec3(velocity.x, 0.0D, velocity.z);
        return flat.lengthSqr() > 1.0E-6D ? flat.normalize() : null;
    }

    /** {@code long[]} 里是否已含某个键（用于 {@link #TAG_SLING_USED} 的「同一方块只弹一次」）。 */
    private static boolean containsLong(long[] values, long key)
    {
        for (long value : values)
        {
            if (value == key)
            {
                return true;
            }
        }
        return false;
    }

    /** 追加一个键并返回新数组（{@code long[]} 定长，只能复制）；见 {@link #TAG_SLING_USED}。 */
    private static long[] appendLong(long[] values, long key)
    {
        long[] grown = Arrays.copyOf(values, values.length + 1);
        grown[values.length] = key;
        return grown;
    }

    /**
     * 芝诺墙的位移快照——约束的是位移，就得先知道这一 tick 走了多少。
     * <p>
     * 三张表分工（键都是 {@code Entity} 本身；{@code Entity} 不重写 equals，所以是身份比较；
     * 弱引用保证实体卸载后不会留下永久条目）：
     * <ul>
     *   <li>{@link #ZENO_PREV} —— 上一 tick 的<b>最终</b>位置，本 tick 内只读、不变；</li>
     *   <li>{@link #ZENO_CUR} —— 本 tick 的最新落点（含本 tick 已施加的修正），每 tick 覆盖；</li>
     *   <li>{@link #ZENO_TICK} —— 上面那次「转正」发生在哪个 tick，用来判断是否已进入本 tick。</li>
     * </ul>
     * <b>为什么基准不能跟着修正一起更新</b>：同一个 tick 内，芝诺墙会被<b>多条跨序标记</b>各调用
     * 一次（域里有几颗跨序之星，就有几个方向要削）。若每次修正后都把「当前位置」写回基准，
     * 第二次调用算出的位移就是 0——第二条约束被静默吃掉，玩家只要斜着绕、或者让正走向的那颗星
     * 排在列表后面，就能从一个没被削的方向磨进去。让基准焊死在上一 tick 的最终位置，
     * 每次修正都只是「把已经走过的那一段再削一点」，多个方向于是自然累加。
     * <p>
     * 「转正」放在一切判定之前，所以被跳过的那些 tick（创造模式、正在飞的球、正在穿越的施术者）
     * 同样会推进基准——否则快照僵在几十 tick 前，一恢复判定就会凭空算出一大段位移、
     * 甩出一记 {@link #ZENO_MAX_CORRECT} 顶格的莫名推力。
     * <p>
     * 只有「跨序且落在斥力圈内」的实体才会进来，因此这几张表天然只有几项，不随标记总数增长。
     */
    private static final Map<Entity, Vec3> ZENO_PREV = new WeakHashMap<>();
    private static final Map<Entity, Vec3> ZENO_CUR = new WeakHashMap<>();
    private static final Map<Entity, Long> ZENO_TICK = new WeakHashMap<>();

    /**
     * 芝诺墙在距离 {@code distance} 处允许的单 tick 朝内位移（格）。
     * <p>
     * {@code u} 是「离底线的余量占整条斥力圈的比例」：外沿处为 1、底线处为 0。于是上限在外沿取
     * {@link #ZENO_INWARD_CAP}，越往里越小，到 {@link #ZENO_CORE} 处归零。而「归零」那一点永远
     * 到不了——距离每 tick 只减 {@code cap(d)}，得到的就是指数逼近、永不归零的那条轨迹。
     */
    private static double zenoInwardCap(double distance)
    {
        if (distance <= ZENO_CORE)
        {
            return 0.0D;
        }
        double u = Math.min(1.0D, (distance - ZENO_CORE) / (REPULSION_RANGE - ZENO_CORE));
        return ZENO_INWARD_CAP * Math.pow(u, ZENO_EXP);
    }

    /**
     * 芝诺墙：把「这一 tick 朝对方靠拢的那一段位移」削到允许上限以内
     * （上限随距离线性趋零，见 {@link #zenoInwardCap}），于是距离永远收敛不到 {@link #ZENO_CORE}。
     * <p>
     * <b>为什么约束位移而不是速度</b>：速度域（往 {@code deltaMovement} 上加冲量）比拼的是
     * 「谁的力气大」，对方只要持续输出就能净推进——僵尸寻路、玩家疾跑都是这样一点点挤进来的。
     * 位移域没有这个口子：每 tick 只放行 {@code zenoInwardCap(d)} 格，而放行量本身随距离衰减，
     * 于是无论对方多快都只能<b>指数逼近、永不抵达</b>。这就是芝诺的乌龟：阿基里斯每一步都追到
     * 上一处，可剩下的那一点总还差一点。
     * <p>
     * <b>两种落点，因为两侧的权威不同</b>：
     * <ul>
     *   <li><b>生物 / 其他实体</b>：位置由服务端权威，{@code setPos} 直接生效；</li>
     *   <li><b>玩家</b>：位置由<b>客户端</b>权威，改位置必须走位置包（{@code teleportTo}），
     *       否则下一 tick 就被客户端的移动包盖回去——这与「飞行，穿越」必须用 {@code teleportTo}
     *       平移、以及「推力对服务端玩家无效」是同一条根因。</li>
     * </ul>
     * <p>
     * <b>只算水平</b>：竖直方向一概不碰。若沿三维修正，悬停的星（恶魂 / 幻翼）会把正下方的玩家
     * 按进地面，修正与碰撞互相打架。竖直贴脸本来就无害，分离交给水平与寻路自己解决。
     * <p>
     * <b>不碰正在飞的球</b>：{@link #TAG_BURST_UNTIL} 未过期时它正在被弹射飞行，这时候削位移会把
     * 抛物线压平——「星星大碰撞」的射程全靠那段自由飞行。
     * <p>
     * <b>不碰正在穿越的施术者</b>：那段位移由 {@code StarFlightSkillLogic} 每 tick 脚本化推进，
     * 两者同时改位置会互相覆盖（穿越途中星域已收，本来也走不到这里，属于防御性判断）。
     *
     * @param outward 由「对方那颗星」指向本实体的单位向量（只有水平分量参与判定）
     */
    private static void zenoWall(@Nullable Entity entity, Vec3 outward, double distance, long now)
    {
        if (entity == null || entity.isRemoved() || !entity.isAlive())
        {
            return;
        }

        /* 进入本 tick：把上一 tick 的最终落点转正成基准。本 tick 内只做一次，之后基准不再变 */
        Long recorded = ZENO_TICK.get(entity);
        if (recorded == null || recorded.longValue() != now)
        {
            Vec3 lastFinal = ZENO_CUR.get(entity);
            if (lastFinal != null)
            {
                ZENO_PREV.put(entity, lastFinal);
            }
            ZENO_TICK.put(entity, now);
        }

        Vec3 current = entity.position();
        Vec3 settled = current;
        try
        {
            if (entity instanceof Player player && (player.isSpectator() || player.isCreative()))
            {
                return;
            }
            if (entity.getPersistentData().getLong(TAG_BURST_UNTIL) > now)
            {
                return;
            }
            if (entity instanceof ServerPlayer flying && StarFlightSkillLogic.isFlying(flying))
            {
                return;
            }

            /* 上一 tick 的最终位置；本 tick 第一次进圈时还没有基准，只登记落点、不判定 */
            Vec3 previous = ZENO_PREV.get(entity);
            if (previous == null)
            {
                return;
            }

            /* 水平外向单位向量；两侧完全垂直重叠时没有「横向」可言，交给竖直与寻路自己处理 */
            Vec3 flat = new Vec3(outward.x, 0.0D, outward.z);
            double length = flat.length();
            if (length < 1.0E-6D)
            {
                return;
            }
            flat = flat.scale(1.0D / length);

            /*
             * 本 tick「一共」朝内靠近了多少（只看水平；负值表示正在拉开，不管）。
             * 基准固定在上 tick 的最终位置，而 current 已含本 tick 前面几条约束施加的修正，
             * 所以同一 tick 内被多个方向调用时，这里读到的是累计量——第二次约束不会因为
             * 「位置刚被第一条改过」而算出 0 位移、把自己静默吃掉。
             */
            double inward = -current.subtract(previous).dot(flat);
            double allowed = zenoInwardCap(distance);
            if (inward <= allowed)
            {
                return;
            }

            double step = Math.min(inward - allowed, ZENO_MAX_CORRECT);
            Vec3 corrected = current.add(flat.scale(step));
            if (entity instanceof ServerPlayer serverPlayer)
            {
                /* 位置包是唯一能压住客户端权威的手段；朝向沿用当前值，不拽视角 */
                serverPlayer.teleportTo(corrected.x, corrected.y, corrected.z);
            }
            else
            {
                entity.setPos(corrected.x, corrected.y, corrected.z);
                entity.hasImpulse = true;
            }
            settled = corrected;
        }
        finally
        {
            /* 落点无论走哪条分支都要记下（含被跳过的分支）：下次转正就是下一 tick 的基准 */
            ZENO_CUR.put(entity, settled);
        }
    }

    /**
     * 以增量速度推动目标；方块锚点（entity == null）与旁观 / 创造模式玩家跳过。
     * <p>
     * <b>跨序斥力不走这里</b>——它压的是位移，见 {@link #zenoWall}。这是唯一的例外，因为
     * 「加一个更大的冲量」既比不过持续输出的 AI，对位置由客户端权威的玩家更是完全无效。
     *
     * @param allowOwner 是否允许推动施术者本人。
     *                   弹射 / 踢飞 / 路径撞击的受害者可能就是施术者自己（自身标记同样是域内的一颗星），
     *                   所以这些场合传 {@code true}；同标记的质心吸引传 {@code false}——
     *                   否则施术者会被自己挂的星拽着走。
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
