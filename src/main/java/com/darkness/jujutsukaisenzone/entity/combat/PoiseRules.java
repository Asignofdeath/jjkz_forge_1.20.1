package com.darkness.jujutsukaisenzone.entity.combat;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * 韧性条的规则中枢：资格判定、档位派生、减免比例、Break 效力上限。
 * <p>
 * 所有常量集中在这里，数值调整只改本文件；{@code vanilla.json} 只需写特例。
 * <p>
 * <b>体抗 + 术抗</b>被当作「怪物档位」使用：
 * 45 是下限（正好把劫掠兽 45、铁傀儡 55 划进来，把猪灵蛮兵 35、唤魔者 30 划出去），
 * 60 / 75 是减免档的分界。改口径只改 {@link #QUALIFY_TOUGHNESS} 一族常量。
 */
public final class PoiseRules
{
    /** 默认门槛：体抗 + 术抗 >= 该值才可能有韧性条（下限对齐劫掠兽）。 */
    public static final double QUALIFY_TOUGHNESS = 45.0D;

    /** 没写 poise 块时的派生比例：韧性上限 = 最大生命 × 该值。 */
    public static final double FALLBACK_HEALTH_RATIO = 0.75D;
    /** 派生值的下限：避免低血高抗怪（如潜影贝）韧性少得看不见。 */
    public static final double FALLBACK_MIN = 30.0D;
    /** 派生值的上限：避免高血怪（如监守者 500 血）派生出一条砍不动的条。 */
    public static final double FALLBACK_MAX = 200.0D;

    /** 减免档分界（体抗 + 术抗）。 */
    public static final double TIER_2_TOUGHNESS = 60.0D;
    public static final double TIER_3_TOUGHNESS = 75.0D;
    /** 档 I / II / III 的伤害减免比例。 */
    public static final double TIER_1_REDUCTION = 0.15D;
    public static final double TIER_2_REDUCTION = 0.25D;
    public static final double TIER_3_REDUCTION = 0.35D;

    /**
     * Break 效力软上限：超过后收益减半。
     * <p>
     * 存在的理由：Break 是破韧的<b>唯一</b>通道，不设上限会让整根韧性条变得两刀就破，
     * 韧性条本身（霸体 + 未破减伤的那段强势期）就失去意义。
     */
    public static final double BREAK_POTENCY_SOFT_CAP = 0.8D;
    /** Break 效力硬上限。 */
    public static final double BREAK_POTENCY_HARD_CAP = 1.5D;

    /**
     * 每次命中扣除的基础韧性值（玩家侧，与伤害量无关）。
     * <p>
     * 锚点：铁傀儡 80 点 → 8 刀破；劫掠兽 70 → 7 刀；监守者 160 → 16 刀。
     * <p>
     * ⚠️ 刻意<b>不</b>按本次伤害换算。理由：破韧应当是一条<b>独立于伤害</b>的进度轴
     * ——否则堆伤害的玩家会顺带把所有怪的韧性条一起秒掉，整根条等于不存在。
     * 「重击比普攻破得多」属于后续细化（要按招式分类），不在当前范围。
     */
    public static final double POISE_DAMAGE_BASE = 10.0D;

    /** 最后一次被命中后，多少 tick 才开始回复韧性（避免边打边回）。 */
    public static final int REGEN_DELAY_TICKS = 60;

    /** 韧性回复的结算间隔（tick）。 */
    public static final int REGEN_INTERVAL_TICKS = 20;

    /** 破韧后「破损态」持续秒数。 */
    public static final float BREAK_DURATION_SECONDS = 10.0F;
    /** 破损期内韧性上限被锁到的比例（避免一破就永久木桩，也避免一破就白嫖）。 */
    public static final double BROKEN_SHIELD_CAP_RATIO = 0.3D;

    /** 破损态时长（tick）。由 {@link #BREAK_DURATION_SECONDS} 换算，改秒数即可。 */
    public static final int BROKEN_DURATION_TICKS = (int) (BREAK_DURATION_SECONDS * 20.0F);

    /**
     * 破韧后施加的虚弱等级（{@code amplifier}，0 = 虚弱 I、1 = 虚弱 II）。
     * 虚弱按原版规则削目标的攻击力，即「打崩姿态后它自己也软了」。
     */
    public static final int BROKEN_WEAKNESS_AMPLIFIER = 1;

    /**
     * 破韧后从<b>体抗 / 术抗</b>上扣掉的百分点 —— 这是本模组「易伤」的落地方式
     * （{@code ModAttributes} 注释已写明「抗性允许负值（易伤）」，
     * {@code SkillDefenseResolver} 对负抗性原样保留、不吃穿透）。
     * <p>
     * ⚠️ 走 <b>Attribute 层</b>而不是走区 4 那类乘区：易伤是一条<b>常态化的目标状态</b>，
     * 应当被面板/调试命令看得见，也应当和其他抗性来源一起参与穿甲结算；
     * 而区 4 那类独立乘区是「管线内的临时乘算」，语义不同。
     */
    public static final double BROKEN_VULNERABILITY_POINTS = 25.0D;

    /** 破损期内韧性上限被锁到的具体数值。 */
    public static float brokenShieldCap(double fullPoise)
    {
        return (float) (fullPoise * BROKEN_SHIELD_CAP_RATIO);
    }

    private PoiseRules()
    {
    }

    /** 只按档位判断该不该有韧性条（用于没写 poise 块的条目）。 */
    public static boolean qualifiesByToughness(double toughness)
    {
        return toughness >= QUALIFY_TOUGHNESS;
    }

    /** 体抗 + 术抗 → 伤害减免档位。 */
    public static double reductionFromToughness(double toughness)
    {
        if (toughness >= TIER_3_TOUGHNESS)
        {
            return TIER_3_REDUCTION;
        }
        if (toughness >= TIER_2_TOUGHNESS)
        {
            return TIER_2_REDUCTION;
        }
        return TIER_1_REDUCTION;
    }

    /** 实际生效的伤害减免比例（数据包写了就用写的，没写按档位派生）。 */
    public static double effectiveReduction(PoiseStats poise, double toughness)
    {
        if (poise.reductionDerived())
        {
            return reductionFromToughness(toughness);
        }
        return Mth.clamp(poise.damageReduction(), 0.0D, 0.95D);
    }

    /**
     * 该生物应有的韧性配置；{@code null} 表示<b>没有韧性条</b>。
     * <p>
     * 判定顺序（顺序即优先级）：
     * <ol>
     *   <li>玩家一律没有（韧性条只属于怪物）。</li>
     *   <li>表里没有这个生物 → 没有。</li>
     *   <li>写了 poise 块 → 以块为准；{@code value <= 0} 视为显式关闭。</li>
     *   <li>没写 poise 块 → 体抗 + 术抗 达到 {@link #QUALIFY_TOUGHNESS} 才派生。</li>
     * </ol>
     */
    @Nullable
    public static PoiseStats resolve(LivingEntity entity)
    {
        if (entity instanceof Player)
        {
            return null;
        }
        MobCombatStats stats = MobCombatStatsRegistry.get(entity.getType());
        if (stats == null)
        {
            return null;
        }
        if (stats.hasPoiseBlock())
        {
            PoiseStats declared = stats.poise();
            return declared != null && declared.enabled() ? declared : null;
        }
        if (!qualifiesByToughness(stats.toughness()))
        {
            return null;
        }
        double derived = Mth.clamp(
                Math.round(entity.getMaxHealth() * FALLBACK_HEALTH_RATIO),
                FALLBACK_MIN, FALLBACK_MAX);
        return new PoiseStats(
                derived,
                PoiseStats.DEFAULT_REGEN,
                PoiseStats.REDUCTION_DERIVED,
                true,
                PoiseStats.DEFAULT_POISE_RESISTANCE);
    }

    /**
     * 一次命中扣掉多少韧性。
     * <p>
     * <pre>
     * poiseDmg = POISE_DAMAGE_BASE × (1 + B_eff)
     * B_eff    = min(B, 软上限折算) × (specialized ? poiseResistance : 1)
     * </pre>
     * <b>Break 效力是唯一能加速破韧的通道</b>：没有特质克制一类的第二个乘数，
     * 破韧速度只由「玩家堆了多少 Break」和「这只怪对 Break 的抗性」两个数决定。
     * <p>
     * 实测口径：铁傀儡（80 点）无 Break 时 8 刀破；Break +50%（resistance 0.35 ⇒ 实到 +17.5%）时 7 刀。
     */
    public static double poiseDamage(PoiseStats poise, double breakPotency)
    {
        return POISE_DAMAGE_BASE * (1.0D + effectiveBreakPotency(poise, breakPotency));
    }

    /**
     * Break 效力在该怪身上的<b>实际</b>值（软上限折算 + 特攻怪打折）。
     * <p>
     * ⛔ 全工程只有 {@link #poiseDamage} 一处调用它：Break 是破韧的唯一通道，
     * 不存在第二个需要共用这个数的乘区（早期的「克制表」已删除）。
     */
    public static double effectiveBreakPotency(PoiseStats poise, double breakPotency)
    {
        return normalizeBreakPotency(breakPotency)
                * (poise.specialized() ? Mth.clamp(poise.poiseResistance(), 0.0D, 1.0D) : 1.0D);
    }

    /** Break 效力取软上限之后的实际值：软上限内 1:1，超出部分只算一半，最后卡硬上限。 */
    public static double normalizeBreakPotency(double raw)
    {
        if (raw <= 0.0D || Double.isNaN(raw))
        {
            return 0.0D;
        }
        double value = raw <= BREAK_POTENCY_SOFT_CAP
                ? raw
                : BREAK_POTENCY_SOFT_CAP + (raw - BREAK_POTENCY_SOFT_CAP) * 0.5D;
        return Math.min(value, BREAK_POTENCY_HARD_CAP);
    }
}
