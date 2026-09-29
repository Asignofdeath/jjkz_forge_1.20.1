package com.darkness.jujutsukaisenzone.technique;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.technique.skillset.SkillDamageType;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 术式伤害的统一入口。
 * <p>
 * 此前所有术式伤害都直接写 {@code damageSources().magic()}——这个 DamageSource 的
 * {@code directEntity} / {@code causingEntity} 都是 null，于是原版 {@code LivingEntity#hurt}
 * 里的归因段被整段跳过：{@code getLastHurtByMob()} / {@code getLastHurtByPlayer()} 永远是 null，
 * {@code DamageSource#getEntity()} 也永远是 null。表现出来就是「伤害打出来了，但没有任何地方
 * 知道是谁打的」：
 * <ul>
 *   <li>{@code CursedGradeKillEvents#resolveKiller} 三条判据（getEntity / getDirectEntity /
 *       getLastHurtByMob）全部落空 ⇒ 术式击杀不给咒术经验</li>
 *   <li>{@code TechniqueIntelligenceDropEvents} / {@code SinfulOneKillEvents} / {@code BlackFlashEvents}
 *       一律只看 {@code getSource().getEntity()} ⇒ 术式击杀也不掉术式情报、不记罪恶者、不判黑闪</li>
 * </ul>
 * 这里只补归因：伤害类型用本模组自己的 {@link #TECHNIQUE_DAMAGE_TYPE}（见其文档说明为何不再借
 * {@code MAGIC}），变的只有「谁打的」。
 * <p>
 * ⚠️ 原版 {@code LivingEntity#hurt} 里归因和击退写在同一段（都读 {@code getEntity()}），
 * 所以补上归因就会连带出 0.4 击退与「把施术者记成攻击者」。击退用
 * {@link #onLivingKnockBack} 精确取消（见 {@link #deal}）；「记成攻击者」保留——
 * 生物被术式打中后开始仇恨施术者，这是原版语义，同时
 * {@code CursedGradeKillEvents} 也依赖 {@code getLastHurtByMob()} 这条兜底。
 * <p>
 * ⚠️ 刻意<b>不</b>填 {@code directEntity}（见 {@link #techniqueSource}）：{@code getSourcePosition()}
 * 只在 {@code isDamageSourceBlocked}（举盾格挡）里被用到，填了就等于让盾牌能挡下术式。
 * 保持 null ⇒ 术式无视盾牌。
 * <p>
 * 分型（{@link SkillDamageType}）是<strong>叙事元数据</strong>，与通道解耦：不改穿甲 / 盾牌，
 * 仅在结算期间写入 {@link #ACTIVE_DAMAGE_TYPE}。走 {@code thrown} / {@code explode} /
 * EpicFight 等通道时，用 {@link #runWithType} / {@link #callWithType} 包住出伤即可挂上分型；
 * 防御管线（{@code TechniqueDamageZoneEvents} 区 2）按它决定吃体抗还是术抗。
 * <p>
 * 暴击许可同理：本类维护 {@link #CRIT_OVERRIDE}（出伤点表态）+ {@link #critAllowed}（分型默认）。
 * 默认口径是「体术可暴、术式不可暴」，理由是术式里高频出伤太多；单发术式招式必须显式走
 * {@link #dealCrit} / {@link #runWithTypeCrit}。改出伤点之前先读 {@link #critAllowed} 的文档。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TechniqueDamageSources
{
    /**
     * 正在结算术式伤害的目标。键用 IdentityHashMap（实体相等即同一对象），
     * 只在 {@link #deal} 调用栈存活期间有值，服务端线程独占。
     */
    private static final ThreadLocal<IdentityHashMap<LivingEntity, Boolean>> KNOCKBACK_GUARD =
            ThreadLocal.withInitial(IdentityHashMap::new);

    /**
     * 正在结算的伤害分型。只在 {@link #deal} / {@link #runWithType} 调用栈存活期间有值，
     * 供 {@code LivingHurtEvent} 监听器读取「这一击算体 / 术 / 复」。
     */
    private static final ThreadLocal<SkillDamageType> ACTIVE_DAMAGE_TYPE = new ThreadLocal<>();

    /**
     * 正在结算的叙事成员 id（skillId / 特别招式 id / 内部别名）。
     * 只在 {@link #deal} / {@link #runWithNarrative} / 带叙事参数的 {@link #runWithType} 栈内有值；
     * 魔虚罗适应等用 {@link #currentNarrativeFamilyId()} 解析成族键。
     */
    private static final ThreadLocal<String> ACTIVE_NARRATIVE_MEMBER = new ThreadLocal<>();

    /**
     * 本次伤害是否已由 {@link #deal} 做过强度换算。
     * <p>
     * {@code TechniqueDamageZoneEvents} 的「区 0」负责给 EF / 原版近战这类<b>不走 deal</b> 的体术伤害
     * 补强度倍率；而走 deal 的伤害已经在进 {@code hurt} 之前乘过了，这里必须让区 0 能识别出来，
     * 否则同一个倍率会被乘两遍。
     */
    private static final ThreadLocal<Boolean> POWER_ALREADY_SCALED = ThreadLocal.withInitial(() -> Boolean.FALSE);

    /**
     * 这一击的暴击许可覆盖。
     * <p>
     * {@code null} = 出伤点没表态 ⇒ 按分型取默认（见 {@link #critAllowed(SkillDamageType)}）：
     * <b>体术默认可暴击，术式默认不可</b>。出伤点用 {@link #runWithCrit} / {@link #dealCrit} /
     * {@link #runWithTypeCrit} 表态——单发术式招式显式放行，体术侧的多段招式可显式拒绝。
     * <p>
     * 只在调用栈内存活（服务端主线程独占），与 {@link #POWER_ALREADY_SCALED} 同一形态；
     * 但默认值是 {@code null} 而不是 {@code false}——「没表态」必须能与「明确拒绝」区分开。
     */
    private static final ThreadLocal<Boolean> CRIT_OVERRIDE = new ThreadLocal<>();

    /** 赫击飞方块的 persistentData 标记：落地砸人仍算术式伤害。 */
    public static final String TAG_RED_DEBRIS = "jujutsukaisenzone_red_debris";
    /** 十划「瓦落瓦落」掀起的坠落碎块（复合伤害）。 */
    public static final String TAG_RATIO_DEBRIS = "jujutsukaisenzone_ratio_debris";

    /**
     * 术式专属伤害类型（数据包 {@code data/jujutsukaisenzone/damage_type/technique.json}）。
     * <p>
     * 为什么不再复用 {@code MAGIC}：
     * <ul>
     *   <li>{@code MAGIC} 在 {@code WITCH_RESISTANT_TO} 里 ⇒ 女巫会对<span>所有</span>术式白嫖 ×0.15 减伤，
     *       这只是「瞬伤药水也是 MAGIC」的连带副作用，不是设计意图</li>
     *   <li>需要一个干净的通道标识，让防御管线能精确区分「这是本模组的术式」</li>
     * </ul>
     * 标签安排（见 {@code data/minecraft/tags/damage_type/}）：
     * <b>加进</b> {@code BYPASSES_ARMOR} / {@code BYPASSES_SHIELD}，
     * <b>不加</b> {@code BYPASSES_RESISTANCE} / {@code BYPASSES_ENCHANTMENTS}（继续吃抗性药水与附魔保护），
     * 也<b>不加</b> {@code WITCH_RESISTANT_TO}。
     * <p>
     * ⚠️ 之所以仍然绕过原版护甲、改由 {@code TechniqueDamageZoneEvents} 区 2 自己算，是因为原版
     * {@code CombatRules.getDamageAfterAbsorb} 对小伤害的减免比例极高（8 点伤害对钻石甲会被砍掉约 72%），
     * 而术式大多是「每 1~5 tick」的高频小伤害，直接继承会把高频流派整个打死。
     */
    public static final ResourceKey<DamageType> TECHNIQUE_DAMAGE_TYPE =
            ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(JuJutsuKaisenZone.MODID, "technique"));
    public static final ResourceKey<DamageType> MARTIAL_DAMAGE_TYPE =
            ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(JuJutsuKaisenZone.MODID, "martial"));
    public static final ResourceKey<DamageType> COMPOSITE_DAMAGE_TYPE =
            ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(JuJutsuKaisenZone.MODID, "composite"));
    public static final ResourceKey<DamageType> BLACK_FLASH_DAMAGE_TYPE =
            ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(JuJutsuKaisenZone.MODID, "black_flash"));

    /** StylizedDamage 等按 {@link DamageSource#getMsgId()} 匹配；须与数据包 message_id 一致。 */
    public static final String MSG_TECHNIQUE = "jjkz.technique";
    public static final String MSG_MARTIAL = "jjkz.martial";
    public static final String MSG_COMPOSITE = "jjkz.composite";
    public static final String MSG_BLACK_FLASH = "jjkz.black_flash";

    /**
     * EF / 原版近战等无法换成 {@link NarrativeDamageSource} 时，把叙事 message_id 挂在 source 身份上。
     * {@code DamageSourceMixin} 读这里，避免反射改 {@code private final type}（Java 16+ 常失败）。
     * 只在单次 {@code hurt} → {@link LivingHurtEvent} 栈内存活，LOWEST 清空。
     */
    private static final ThreadLocal<IdentityHashMap<DamageSource, String>> DISPLAY_MSG_OVERRIDE =
            ThreadLocal.withInitial(IdentityHashMap::new);

    /** 术式伤害类型的 holder 缓存：{@link Level#registryAccess()} 变了就重取（换存档 / 跨维度）。服务端单线程，无并发问题。 */
    private static Holder<DamageType> cachedTechniqueType;
    private static RegistryAccess cachedTechniqueAccess;

    private TechniqueDamageSources()
    {
    }

    /**
     * 取出术式伤害类型。数据包注册表在运行时才可用，所以只能按需从 level 的 registryAccess 里查。
     */
    private static Holder<DamageType> techniqueType(Level level)
    {
        RegistryAccess access = level.registryAccess();
        if (cachedTechniqueType == null || cachedTechniqueAccess != access)
        {
            cachedTechniqueType = access.registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(TECHNIQUE_DAMAGE_TYPE);
            cachedTechniqueAccess = access;
        }
        return cachedTechniqueType;
    }

    /**
     * 构造一次术式伤害的 {@link DamageSource}。
     * <p>
     * {@code directEntity} 刻意留 null：{@code getSourcePosition()} 只在举盾格挡判定里用到，
     * 填了就等于让盾牌能挡下术式（同时 {@code jjkz:technique} 也已加进 {@code BYPASSES_SHIELD}，
     * 这里是第二道保险，两道都留着以防标签被别的数据包改掉）。
     */
    public static DamageSource techniqueSource(Level level, @Nullable LivingEntity owner)
    {
        return new DamageSource(techniqueType(level), null, owner);
    }

    /**
     * 构造一次带叙事分型 message_id 的伤害源。
     * <p>
     * 战斗标签仍用 {@link #TECHNIQUE_DAMAGE_TYPE}（与旧 {@link #techniqueSource} 一致：穿甲 / 无视盾），
     * 仅覆盖 {@link DamageSource#getMsgId()}，供跳字模组区分体术 / 术式 / 复合。
     */
    public static DamageSource narrativeSource(Level level, @Nullable LivingEntity owner, SkillDamageType damageType)
    {
        return new NarrativeDamageSource(techniqueType(level), null, owner, msgIdFor(damageType));
    }

    public static String msgIdFor(SkillDamageType damageType)
    {
        if (damageType == null)
        {
            return MSG_TECHNIQUE;
        }
        return switch (damageType)
        {
            case MARTIAL -> MSG_MARTIAL;
            case COMPOSITE -> MSG_COMPOSITE;
            case TECHNIQUE -> MSG_TECHNIQUE;
        };
    }

    /**
     * 给已有 {@link DamageSource} 挂叙事 message_id（EF 近战 / 黑闪等无法重建 source 的通道）。
     * 对象身份不变（{@code instanceof EpicFightDamageSource} 仍成立）；
     * 实际改写的是 {@link #peekDisplayMsgId}，由 Mixin 注入 {@link DamageSource#getMsgId()}。
     */
    public static void projectDisplayType(DamageSource source, Level level, ResourceKey<DamageType> displayType)
    {
        if (!com.darkness.jujutsukaisenzone.gameplay.ModRules.StylizedDamageStyles.isEnabled())
        {
            return;
        }
        if (source == null || displayType == null)
        {
            return;
        }
        if (source instanceof NarrativeDamageSource)
        {
            return;
        }
        String msgId = messageIdOf(displayType);
        if (msgId != null)
        {
            DISPLAY_MSG_OVERRIDE.get().put(source, msgId);
        }
    }

    public static void projectDisplayType(DamageSource source, Level level, SkillDamageType damageType)
    {
        if (damageType == null)
        {
            return;
        }
        ResourceKey<DamageType> key = switch (damageType)
        {
            case MARTIAL -> MARTIAL_DAMAGE_TYPE;
            case COMPOSITE -> COMPOSITE_DAMAGE_TYPE;
            case TECHNIQUE -> TECHNIQUE_DAMAGE_TYPE;
        };
        projectDisplayType(source, level, key);
    }

    @Nullable
    private static String messageIdOf(ResourceKey<DamageType> key)
    {
        if (MARTIAL_DAMAGE_TYPE.equals(key))
        {
            return MSG_MARTIAL;
        }
        if (TECHNIQUE_DAMAGE_TYPE.equals(key))
        {
            return MSG_TECHNIQUE;
        }
        if (COMPOSITE_DAMAGE_TYPE.equals(key))
        {
            return MSG_COMPOSITE;
        }
        if (BLACK_FLASH_DAMAGE_TYPE.equals(key))
        {
            return MSG_BLACK_FLASH;
        }
        return null;
    }

    /** Mixin 用：当前 source 若被 {@link #projectDisplayType} 投影过，返回叙事 message_id。 */
    @Nullable
    public static String peekDisplayMsgId(DamageSource source)
    {
        if (source == null)
        {
            return null;
        }
        Map<DamageSource, String> map = DISPLAY_MSG_OVERRIDE.get();
        return map.isEmpty() ? null : map.get(source);
    }

    /** {@link LivingHurtEvent} 结束后清掉投影，避免 ThreadLocal 脏数据。 */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void clearDisplayMsgOverride(LivingHurtEvent event)
    {
        IdentityHashMap<DamageSource, String> map = DISPLAY_MSG_OVERRIDE.get();
        if (!map.isEmpty())
        {
            map.clear();
        }
    }

    /**
     * 判定这一击是不是「贴身格斗」——EpicFight 基础攻击、原版怪物近战、原版玩家近战都走这条。
     * <p>
     * 判据用原版语义本身：近战伤害的 {@code directEntity} 就是攻击者本人
     * （{@code mobAttack} / {@code playerAttack} 都是 {@code DamageSource(entity, entity)}），
     * 而投掷物 / 弹射物的 {@code directEntity} 是那个投射物，爆炸则两者常常都是 null。
     * 比查标签更准，也不依赖 EpicFight。
     */
    public static boolean isMelee(DamageSource source)
    {
        Entity direct = source.getDirectEntity();
        return direct != null
                && direct == source.getEntity()
                && direct instanceof LivingEntity
                && !source.is(DamageTypeTags.IS_EXPLOSION)
                && !source.is(DamageTypeTags.IS_PROJECTILE);
    }


    /**
     * 对目标造成一次术式伤害（带施术者归因，且不产生击退）。
     *
     * @param target 受击目标
     * @param owner  施术者；为 null 时退回改造前的无归因 {@code magic()}
     *               （此时原版也不会触发击退，无需额外处理）
     * @param amount 伤害量
     * @return 与 {@link LivingEntity#hurt} 一致：伤害是否真正生效
     */
    public static boolean deal(LivingEntity target, @Nullable LivingEntity owner, float amount)
    {
        return deal(target, owner, amount, SkillDamageType.TECHNIQUE);
    }

    /** 同 {@link #deal(LivingEntity, LivingEntity, float, SkillDamageType)}，并挂叙事成员 id。 */
    public static boolean deal(
            LivingEntity target,
            @Nullable LivingEntity owner,
            float amount,
            @Nullable String narrativeMemberId)
    {
        return deal(target, owner, amount, SkillDamageType.TECHNIQUE, narrativeMemberId);
    }

    /**
     * 对目标造成一次指定分型的伤害。
     * <p>
     * 分型写入 {@link #ACTIVE_DAMAGE_TYPE} 供乘区管线使用；跳字侧通过 {@link NarrativeDamageSource}
     * 投影 message_id。穿甲 / 盾牌语义仍用 {@link #TECHNIQUE_DAMAGE_TYPE}，仅在结算期间写入
     * {@link #ACTIVE_DAMAGE_TYPE}，供乘区管线（{@code TechniqueDamageZoneEvents}）按分型挑加成。
     * <p>
     * ⚠️ <b>强度面板换算在这里统一完成</b>：调用方一律传「四级基准点数」（就是原来写死的那个数，
     * 例如 {@code LimitlessBlueConfig.DAMAGE} = 8.0F），本方法按施术者当前的体术 / 术式强度
     * 换算成实际伤害（见 {@link SkillDamageResolver}）。
     * 因此调用方<b>不要</b>自己再乘强度，否则重复放大。
     *
     * @param damageType 这一击的叙事分型；传 {@link SkillDamageType#TECHNIQUE} 与旧三参重载等价
     */
    public static boolean deal(LivingEntity target, @Nullable LivingEntity owner, float amount, SkillDamageType damageType)
    {
        return deal(target, owner, amount, damageType, null);
    }

    /**
     * 同 {@link #deal(LivingEntity, LivingEntity, float, SkillDamageType)}，并挂叙事成员 id
     *（技能族见 {@link NarrativeSkillFamilies}）。
     */
    public static boolean deal(
            LivingEntity target,
            @Nullable LivingEntity owner,
            float amount,
            SkillDamageType damageType,
            @Nullable String narrativeMemberId)
    {
        return callWithNarrative(narrativeMemberId, () -> dealInternal(target, owner, amount, damageType));
    }

    private static boolean dealInternal(
            LivingEntity target,
            @Nullable LivingEntity owner,
            float amount,
            SkillDamageType damageType)
    {
        float resolvedAmount = resolvePowerScaled(owner, amount, damageType);
        boolean previousScaledFlag = POWER_ALREADY_SCALED.get();
        POWER_ALREADY_SCALED.set(true);
        // 把「强度放大前」的原始点数交给黑闪：黑闪要对原始值取幂，否则 pow 会把面板倍率一起指数放大。
        // 标记只在本次 hurt 调用栈内存活（LivingHurtEvent 是同步派发的），finally 里必定清掉。
        if (Math.abs(resolvedAmount - amount) > 0.0001F)
        {
            SkillDamageResolver.markRawBeforePower(amount);
        }
        try
        {
            return callWithType(damageType, () -> {
                DamageSource source = com.darkness.jujutsukaisenzone.gameplay.ModRules.StylizedDamageStyles.isEnabled()
                        ? narrativeSource(target.level(), owner, damageType)
                        : techniqueSource(target.level(), owner);
                if (owner == null)
                {
                    return target.hurt(source, resolvedAmount);
                }

                IdentityHashMap<LivingEntity, Boolean> guard = KNOCKBACK_GUARD.get();
                guard.put(target, Boolean.TRUE);
                try
                {
                    return target.hurt(source, resolvedAmount);
                }
                finally
                {
                    guard.remove(target);
                }
            });
        }
        finally
        {
            SkillDamageResolver.clearRawBeforePower();
            POWER_ALREADY_SCALED.set(previousScaledFlag);
        }
    }

    /**
     * 放行暴击的一次术式伤害。
     * <p>
     * 与 {@link #deal(LivingEntity, LivingEntity, float, SkillDamageType)} 的差别只有一个：
     * 在结算栈内把暴击许可置为 {@code true}，让术式分型也能吃到乘区 C 的暴击判定。
     * <b>只给单发招式用</b>（赫箭爆炸、星痕撞击 / 爆裂、法槌这类一次结算的）——
     * 周期性 / 每 tick 出伤的招式走它，暴击提示会随 tick 刷屏，而且逐 tick 掷骰的期望
     * 与单次掷骰无异，只是方差被平均掉。
     */
    public static boolean dealCrit(LivingEntity target, @Nullable LivingEntity owner, float amount, SkillDamageType damageType)
    {
        return dealCrit(target, owner, amount, damageType, null);
    }

    public static boolean dealCrit(
            LivingEntity target,
            @Nullable LivingEntity owner,
            float amount,
            SkillDamageType damageType,
            @Nullable String narrativeMemberId)
    {
        return callWithCrit(true, () -> deal(target, owner, amount, damageType, narrativeMemberId));
    }

    /** 同 {@link #dealCrit(LivingEntity, LivingEntity, float, SkillDamageType)}，分型默认术式。 */
    public static boolean dealCrit(LivingEntity target, @Nullable LivingEntity owner, float amount)
    {
        return dealCrit(target, owner, amount, SkillDamageType.TECHNIQUE);
    }

    public static boolean dealCrit(
            LivingEntity target,
            @Nullable LivingEntity owner,
            float amount,
            @Nullable String narrativeMemberId)
    {
        return dealCrit(target, owner, amount, SkillDamageType.TECHNIQUE, narrativeMemberId);
    }
    /**
     * 明确拒绝暴击的一次伤害。
     * <p>
     * 术式分型默认本就不可暴击，所以这个入口只对<b>体术</b>有意义：留给「体术但多段 / 高频」
     * 的招式主动收闸门用（默认的体术是放行的）。
     * <p>
     * ⚠️ 目前<b>没有调用点</b>：所有体术招式都按默认放行，多段招式也只逐段各掷一次骰（不做
     * 「整招只掷一次」的预算）。保留它是为了以后真需要给某个体术招式收闸门时有现成入口——
     * 别因为编辑器报「未使用」就顺手删掉。
     */
    public static boolean dealNoCrit(LivingEntity target, @Nullable LivingEntity owner, float amount, SkillDamageType damageType)
    {
        return callWithCrit(false, () -> deal(target, owner, amount, damageType));
    }

    /** 当前是否处于 {@link #deal} 的结算栈内（区 0 用它避免重复乘强度）。 */
    public static boolean isPowerAlreadyScaled()
    {
        return POWER_ALREADY_SCALED.get();
    }

    /**
     * 这一击能不能暴击。
     * <p>
     * 判据次序：<b>出伤点的显式表态 → 分型默认</b>。
     * <ul>
     *   <li>体术（{@link SkillDamageType#MARTIAL}）默认可暴击。这条默认值不能省：EpicFight
     *       基础攻击与原版近战<b>不走</b> {@link #deal}，拿不到任何表态，一旦默认改为「一律不可暴击」，
     *       现在唯一的暴击来源会被整个关掉。</li>
     *   <li>术式 / 复合默认不可暴击。术式里「每 1~5 tick 出一次伤」的招式很多（苍、捌、解、茈、
     *       灶开、领域斩击……），单发招式用 {@link #dealCrit} / {@link #runWithTypeCrit} 显式放行。</li>
     * </ul>
     * 默认取向刻意选「没表态就不给暴击」：漏标的后果是「该暴的没暴」，而不是「高频技能失控」。
     */
    public static boolean critAllowed(@Nullable SkillDamageType damageType)
    {
        Boolean override = CRIT_OVERRIDE.get();
        if (override != null)
        {
            return override;
        }
        return damageType == SkillDamageType.MARTIAL;
    }

    /** 在调用栈内覆盖暴击许可（嵌套安全：退出时恢复外层值，与 {@link #callWithType} 同一写法）。 */
    public static <T> T callWithCrit(boolean allowed, Supplier<T> action)
    {
        Boolean previous = CRIT_OVERRIDE.get();
        CRIT_OVERRIDE.set(allowed);
        try
        {
            return action.get();
        }
        finally
        {
            if (previous == null)
            {
                CRIT_OVERRIDE.remove();
            }
            else
            {
                CRIT_OVERRIDE.set(previous);
            }
        }
    }

    /** 同 {@link #callWithCrit}，无返回值。 */
    public static void runWithCrit(boolean allowed, Runnable action)
    {
        callWithCrit(allowed, () -> {
            action.run();
            return null;
        });
    }

    /**
     * 强度面板换算：把「四级基准点数」变成施术者当前强度下的实际伤害。
     * <ul>
     *   <li>术式 → 术式强度</li>
     *   <li>体术 → 体术强度</li>
     *   <li>复合 → 两者各算一次取平均（避免黑闪这类同时动用两种能力的技能吃双倍面板）</li>
     * </ul>
     * 非玩家（没有面板）一律原样返回。
     */
    private static float resolvePowerScaled(@Nullable LivingEntity owner, float baselinePoints, SkillDamageType damageType)
    {
        return switch (damageType)
        {
            case TECHNIQUE -> SkillDamageResolver.technique(owner, baselinePoints);
            case MARTIAL -> SkillDamageResolver.physical(owner, baselinePoints);
            case COMPOSITE -> (SkillDamageResolver.technique(owner, baselinePoints)
                    + SkillDamageResolver.physical(owner, baselinePoints)) * 0.5F;
        };
    }

    /**
     * 在任意出伤通道上挂叙事分型（不改 DamageSource 本身）。
     * 用于 {@code thrown} / {@code explode} / {@code mobProjectile} 等不能或不该改走 {@link #deal} 的路径。
     */
    public static void runWithType(SkillDamageType damageType, Runnable action)
    {
        callWithType(damageType, () -> {
            action.run();
            return null;
        });
    }

    /** 同 {@link #runWithType}，并挂叙事成员 id。 */
    public static void runWithType(
            SkillDamageType damageType,
            @Nullable String narrativeMemberId,
            Runnable action)
    {
        callWithNarrative(narrativeMemberId, () -> {
            runWithType(damageType, action);
            return null;
        });
    }

    /**
     * 同 {@link #runWithType}，带回返回值（如 {@code hurt} 的 boolean）。
     */
    public static <T> T callWithType(SkillDamageType damageType, Supplier<T> action)
    {
        SkillDamageType previousType = ACTIVE_DAMAGE_TYPE.get();
        ACTIVE_DAMAGE_TYPE.set(damageType);
        try
        {
            return action.get();
        }
        finally
        {
            if (previousType == null)
            {
                ACTIVE_DAMAGE_TYPE.remove();
            }
            else
            {
                ACTIVE_DAMAGE_TYPE.set(previousType);
            }
        }
    }

    /**
     * 同 {@link #runWithType}，并在结算栈内放行暴击。
     * <p>
     * 给「不能或不该改走 {@link #deal}、但确实是一次结算」的通道用：例如
     * {@code runWithType(TECHNIQUE, () -> target.hurt(thrownSource, dmg))} 这种直连 {@code hurt}
     * 的写法，想参与暴击就换成它。高频通道不要用。
     */
    public static void runWithTypeCrit(SkillDamageType damageType, Runnable action)
    {
        runWithTypeCrit(damageType, null, action);
    }

    public static void runWithTypeCrit(
            SkillDamageType damageType,
            @Nullable String narrativeMemberId,
            Runnable action)
    {
        callWithNarrative(narrativeMemberId, () -> callWithCrit(true, () -> callWithType(damageType, () -> {
            action.run();
            return null;
        })));
    }

    /**
     * 在任意出伤通道上挂叙事成员 id（可与 {@link #runWithType} 嵌套）。
     */
    public static void runWithNarrative(@Nullable String narrativeMemberId, Runnable action)
    {
        callWithNarrative(narrativeMemberId, () -> {
            action.run();
            return null;
        });
    }

    public static <T> T callWithNarrative(@Nullable String narrativeMemberId, Supplier<T> action)
    {
        if (narrativeMemberId == null || narrativeMemberId.isBlank())
        {
            return action.get();
        }
        String previous = ACTIVE_NARRATIVE_MEMBER.get();
        ACTIVE_NARRATIVE_MEMBER.set(narrativeMemberId);
        try
        {
            return action.get();
        }
        finally
        {
            if (previous == null)
            {
                ACTIVE_NARRATIVE_MEMBER.remove();
            }
            else
            {
                ACTIVE_NARRATIVE_MEMBER.set(previous);
            }
        }
    }

    /**
     * 当前结算中的伤害分型。只在 {@link #deal} / {@link #runWithType} 调用栈内有值；
     * 通道外的伤害由 {@code TechniqueDamageZoneEvents} 另行判定（如 EpicFight 基础攻击 → 体术）。
     */
    @Nullable
    public static SkillDamageType currentDamageType()
    {
        return ACTIVE_DAMAGE_TYPE.get();
    }

    /** 当前结算中的叙事成员 id；未挂则为 null。 */
    @Nullable
    public static String currentNarrativeMemberId()
    {
        return ACTIVE_NARRATIVE_MEMBER.get();
    }

    /** 当前结算对应的叙事族 id（经 {@link NarrativeSkillFamilies#resolveFamilyId}）。 */
    @Nullable
    public static String currentNarrativeFamilyId()
    {
        return NarrativeSkillFamilies.resolveFamilyId(ACTIVE_NARRATIVE_MEMBER.get());
    }

    /**
     * 取消术式伤害带来的原版 0.4 击退。
     * <p>
     * 原版 {@code LivingEntity#hurt} 在归因非空时会调 {@code knockback(0.4F, dx, dz)}，
     * 而 Forge 在 {@code knockback} 入口就发出可取消的 {@link LivingKnockBackEvent}
     * （补丁：{@code if(event.isCanceled()) return;}）⇒ 借这个入口精确取消，
     * 改动只覆盖 {@link #deal} 调用栈内的那一次击退（标记在 finally 里移除），
     * 近战 / 弹射物 / EpicFight 自己的位移一律不受影响。
     */
    @SubscribeEvent
    public static void onLivingKnockBack(LivingKnockBackEvent event)
    {
        if (KNOCKBACK_GUARD.get().containsKey(event.getEntity()))
        {
            event.setCanceled(true);
        }
    }
}
