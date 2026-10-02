package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.Config;
import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.compat.EpicFightCompat;
import com.darkness.jujutsukaisenzone.effect.DeathSentenceEffect;
import com.darkness.jujutsukaisenzone.entity.combat.LivingCombatStats;
import com.darkness.jujutsukaisenzone.player.PlayerBlackFlashData;
import com.darkness.jujutsukaisenzone.player.PlayerCombatPowerData;
import com.darkness.jujutsukaisenzone.player.PlayerDomainGaugeData;
import com.darkness.jujutsukaisenzone.technique.JjkzDamageTypeTags;
import com.darkness.jujutsukaisenzone.technique.SkillDamageResolver;
import com.darkness.jujutsukaisenzone.technique.SkillDefenseResolver;
import com.darkness.jujutsukaisenzone.technique.TechniqueDamageBoosts;
import com.darkness.jujutsukaisenzone.technique.TechniqueDamageSources;
import com.darkness.jujutsukaisenzone.technique.crit.CritRules;
import com.darkness.jujutsukaisenzone.technique.crit.HitCritState;
import com.darkness.jujutsukaisenzone.technique.skill.JudgmentWeaponLogic;
import com.darkness.jujutsukaisenzone.technique.skill.RatioSpotLogic;
import com.darkness.jujutsukaisenzone.technique.skill.tenshadows.TenShadowsPresenceBonuses;
import com.darkness.jujutsukaisenzone.technique.skillset.SkillDamageType;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;
import yesman.epicfight.world.damagesource.EpicFightDamageSource;

import java.util.HashMap;
import java.util.Map;

/**
 * 伤害乘区统一结算管线。
 * <p>
 * 所有伤害的最终落地都要过 {@link LivingHurtEvent}——术式（{@code TechniqueDamageSources.deal}
 * → {@code LivingEntity#hurt}）与 EpicFight 体术在此汇合，这里就是全模组唯一的乘区汇合点。
 * 各乘区按固定顺序施加，取代散落各处的 {@code event.setAmount}：
 * <ol start="0">
 *   <li><b>区 0 · 施术者面板换算</b> —— 只给不走 {@code deal} 的体术通道（EF / 原版近战）补强度倍率，
 *       并顺手记下幂运算要用的原始伤害</li>
 *   <li><b>区 1 · 施术者增强</b> —— {@link TechniqueDamageBoosts} 的分型匹配「下一击增伤」</li>
 *   <li><b>区 C · 暴击</b> —— 暴击几率属性判定 + 随机浮动乘区；体术默认参与，术式需出伤点显式放行
 *       （见 {@code TechniqueDamageSources#critAllowed}），复合不参与；黑闪须先暴击且须为体术系</li>
 *   <li><b>区 3 · 目标减免</b> —— ZONE 状态减伤（自 {@code BlackFlashEvents} 迁入，判定与数值不变）</li>
 *   <li><b>区 4 · 韧性条减免</b> —— 怪物韧性未破（{@code stunShield > 0}）时的减伤，
 *       取自 {@code MobPoiseEvents#damageReductionFor}。与区 3 同属「交接之后施加」的一类：
 *       不参与区 2 的封顶基准，语义与 ZONE 减伤一致。</li>
 *   <li><b>区 V · 伤害浮动</b> —— 每次命中独立 roll，在 {@code [1-spread, 1+spread]} 内均匀随机。
 *       与区 3 / 区 4 同属「交接之后施加」的最后一道乘区，直接落在最终伤害上。
 *       与暴击浮动（{@code CritRules#rollVarianceMultiplier}）<b>互相独立、叠加生效</b>：
 *       暴击是偶发的大幅偏斜，本区是每次都有的基础抖动。</li>
 * </ol>
 * 以上三区在 {@link LivingHurtEvent}（{@link EventPriority#HIGH}）内完成；<b>区 2 · 目标防御层</b>
 * （护甲 → 体术抗性 / 术式抗性，见 {@link SkillDefenseResolver}）单独挂在
 * {@link LivingDamageEvent}，原因见 {@link #onLivingDamage}。
 * <p>
 * 黑闪的 {@code pow(x, 2.5)} 仍留在 {@code BlackFlashEvents}（它不是纯数值，还带特效与奖励），
 * 位于默认优先级。⚠️ 幂只作用在<b>底数</b>上：{@code pow(raw, 2.5) × 当前值 / raw}
 * ⇒ 各乘区（强度倍率 / 防御 / ZONE）都是<b>线性</b>通过的，<b>不会</b>被幂放大。
 * 成立的前提是底数必须是「所有乘区之前」的原始伤害——所以区 0 必须<b>无条件</b>记下它，
 * 详见该处注释。
 * <p>
 * ⚠️ 优先级次序（勿随意改动）：处刑人之剑的必杀覆盖在 {@link EventPriority#HIGHEST}
 * （它是对伤害量的<b>改写</b>，必须先于一切乘区）；本管线 {@link EventPriority#HIGH}；
 * 罪恶者增伤 / 黑闪 / 蓄力打断在默认优先级。乘法交换律保证迁移前后数值一致。
 * <p>
 * ⚠️ 三个事件阶段各能拿到什么（决定了每个乘区该挂哪）：
 * <ul>
 *   <li>{@link LivingHurtEvent}：原版护甲 <b>之前</b>。在这里设的量还会被护甲 / 附魔 / 抗性药水继续加工。</li>
 *   <li>原版内部：护甲 → 附魔 / 抗性药水 → 扣除吸收（金苹果黄心）。</li>
 *   <li>{@link LivingDamageEvent}：以上全部算完、且<b>已扣过黄心</b>，离扣血只差一步
 *       ⇒ 唯一能对「总减伤」精确封顶的位置。</li>
 * </ul>
 * <p>
 * 后续新乘区（易伤、标签抗性……）一律加进本类并写明顺序，<b>不要</b>再到别的
 * 监听器里裸写 {@code setAmount} 乘算。
 * <p>
 * ⚠️ 「克制」<b>不是</b>乘区：咒力特质克制表已于 2026-09-27 删除，破韧速度只由
 * {@code MobPoiseEvents} 的扣盾量决定，不进本管线。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TechniqueDamageZoneEvents
{
    private TechniqueDamageZoneEvents()
    {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingHurt(LivingHurtEvent event)
    {
        if (event.getEntity().level().isClientSide)
        {
            return;
        }

        LivingEntity target = event.getEntity();
        DamageSource source = event.getSource();

        // 本击暴击状态：整次 LivingHurt（含黑闪 NORMAL / SE）共用，入口清空防脏读。
        HitCritState.begin();

        // 分型解析<b>对所有攻击者</b>执行：体术抗性需要知道「这一击是不是贴身格斗」，
        // 而怪物近战的攻击者不是 ServerPlayer。区 1 再单独把自己限定到 ServerPlayer。
        SkillDamageType type = resolveDamageType(target, source);

        // 叙事分型 → 跳字可见的 message_id（EF / 原版近战等不走 deal 的通道）。
        if (type != null)
        {
            TechniqueDamageSources.projectDisplayType(source, target.level(), type);
        }

        // 领域量表：术式命中灌充；体术命中仅在装了 DomainGaugeSeMechanic 时灌（共用冷却）
        if (event.getAmount() > 0.0F
                && source.getEntity() instanceof ServerPlayer gaugeAttacker)
        {
            if (type == SkillDamageType.TECHNIQUE)
            {
                PlayerDomainGaugeData.addChargeFromTechniqueHit(gaugeAttacker);
            }
            else if (type == SkillDamageType.MARTIAL)
            {
                PlayerDomainGaugeData.addChargeFromMartialHit(gaugeAttacker);
            }
        }


        // —— 区 0：施术者面板换算 ——
        // 技能出伤已在 deal / 出伤点用 SkillDamageResolver 按「强度×技能倍率」算成绝对值。
        // 这里只接管 EF / 原版近战通道的绝对伤：再按「体术强度 / physicalBaseline」缩放。
        // ⚠️ 前提是 CursedGradeBonuses 已不再给 ATTACK_DAMAGE 加等级值，否则双计。
        // 覆盖体术与复合：黑闪主拳一律按体术；近战复合目前主要是处刑必杀（见下排除）。
        // ⚠️ 唯一要排除的是处刑人之剑的必杀：它的伤害量已在 HIGHEST 优先级被改写成
        // 固定致死值（{@code JudgmentWeaponLogic#lethalDamageAmount}），在此再乘一道面板倍率，
        // 低强度玩家会把它乘回致死线以下 ⇒ 必杀失效。判定复用分型解析用过的同一个方法，口径一致。
        if ((type == SkillDamageType.MARTIAL || type == SkillDamageType.COMPOSITE)
                && !TechniqueDamageSources.isPowerAlreadyScaled()
                && source.getEntity() instanceof LivingEntity attacker
                && shouldScalePhysicalPower(attacker, target, source, type))
        {
            float raw = event.getAmount();
            // ⚠️ 必须「无条件」记下 raw，不能只在 k ≠ 1 时记。
            // 黑闪的 pow 底数要求是「所有乘区之前」的原始伤害：BlackFlashEvents 里
            //     powered = pow(rawBeforePower, 2.5) × (当前值 / rawBeforePower)
            // 底数正确 ⇒ 得到的量里各乘区是【线性】的；底数一旦漏记，它会退到
            // consumeRawBeforePower 的兜底分支、拿已经被乘区处理过的当前值当底数，
            // 于是 k（强度）、d（抗性）、z（ZONE）全被 2.5 次方放大。
            // 实测口径：50% 体抗在黑闪里会变成等效 82% 减伤，而且只惩罚 k == 1 的玩家
            //（强度正好等于基准 / 面板未同步 power < 0 / scalingEnabled = false），方向是反的。
            SkillDamageResolver.markRawBeforePower(raw);
            float scaled = SkillDamageResolver.scaleMeleeAbsolute(attacker, raw);
            if (attacker instanceof ServerPlayer serverAttacker
                    && TenShadowsPresenceBonuses.hasToad(serverAttacker)
                    && type == SkillDamageType.MARTIAL)
            {
                int tech = PlayerCombatPowerData.techniquePower(serverAttacker);
                if (tech > 0 && Config.combatPowerTechniqueBaseline > 0.0D)
                {
                    scaled *= (float) (1.0D + TenShadowsPresenceBonuses.TOAD_MARTIAL_FROM_TECHNIQUE
                            * tech / Config.combatPowerTechniqueBaseline);
                }
            }
            if (Math.abs(scaled - raw) > 0.0001F)
            {
                event.setAmount(scaled);
            }
        }

        // —— 区 1：施术者增强（分型匹配的「下一击增伤」）——
        if (source.getEntity() instanceof ServerPlayer attacker && type != null)
        {
            float bonus = TechniqueDamageBoosts.consume(attacker, type);
            if (bonus > 0.0F)
            {
                event.setAmount(event.getAmount() * (1.0F + bonus));
            }
        }

        // —— 区 C：暴击（判定 + 随机浮动乘区）——
        // 分型资格：体术与术式（复合不放行，见 CritRules#canCrit）；真正放行与否还要过
        // TechniqueDamageSources#critAllowed —— 体术默认放行，术式默认拒绝，单发术式招式
        // 由出伤点用 dealCrit / runWithTypeCrit 显式放行。
        // 十划弱点在此之前通过 CritRules.forceNext 注入强制暴击。
        // 黑闪在 NORMAL 优先级读取 HitCritState.wasCrit（且限体术系暴击）作为硬门槛。
        if (source.getEntity() instanceof ServerPlayer critAttacker
                && type != null
                && event.getAmount() > 0.0F)
        {
            maybeForceRatioSpotCrit(critAttacker, target);
            float afterCrit = CritRules.applyCritZone(
                    critAttacker, type, event.getAmount(), critAttacker.getRandom());
            if (Math.abs(afterCrit - event.getAmount()) > 0.0001F)
            {
                event.setAmount(afterCrit);
            }
        }

        // —— 区 2 的交接：把「进入原版减免前的伤害量」和分型交给 LivingDamageEvent ——
        // 防御层不在这里算：原版护甲 / 附魔 / 抗性药水都在本事件之后结算，留在这里封不住
        // 「总减伤」（体术通道最坏 70% 体抗 + 钻石甲 80% ≈ 94%）。
        // 此时 event.getAmount() 已经过区 1；区 3（ZONE）在交接之后才施加，
        // 封顶基准天然把 ZONE 减伤排除在上限之外，维持 ZONE 原有语义。
        // 分型【必须】一并交接、不能在 LivingDamageEvent 重新解析：处刑必杀等标记
        // 若在后续优先级被消耗，再解析会丢复合身份。
        if (event.getAmount() > 0.0F)
        {
            rememberIncoming(target, source, type, event.getAmount());
        }

        // —— 区 3：目标减免（ZONE 状态）——
        // 迁自 BlackFlashEvents#onLivingHurt 开头，判定与数值原样保留：
        // 处于 ZONE 的战斗模式玩家，受到的一切伤害按配置比例减免。
        if (target instanceof ServerPlayer zonePlayer
                && EpicFightCompat.isBattleMode(zonePlayer)
                && PlayerBlackFlashData.isZoneActive(zonePlayer))
        {
            event.setAmount(event.getAmount() * (float) (1.0D - Config.blackFlashZoneDefenseBonus));
        }

        // —— 区 4：韧性条减免（怪物韧性未破时的减伤）——
        // 与区 3 同样挂在交接之后：不进区 2 的封顶基准，语义与 ZONE 减伤一致。
        // ⚠️ 必须是独立乘区、乘在最终伤害上——<b>绝不</b>走 resistanceReduction 通道，
        //    否则会被 SkillDefenseResolver#applyPenetration 当成抗性吃掉，那就成了「吃穿甲」。
        if (event.getAmount() > 0.0F)
        {
            double poiseReduction = MobPoiseEvents.damageReductionFor(target);
            if (poiseReduction > 0.0D)
            {
                event.setAmount((float) (event.getAmount() * (1.0D - poiseReduction)));
            }
        }

        // —— 区 V：伤害浮动（每次命中独立 roll）——
        // 最后一道乘区，直接乘在最终落地伤害上：不参与区 2 的封顶基准（与区 3 / 区 4 同属
        // 「交接之后施加」）。与暴击浮动互相独立、叠加生效——暴击是偶发的大幅偏斜，
        // 本区是每次都有的基础抖动，两者乘在一起。
        // ⚠️ 排除处刑人之剑的必杀：它的伤害量是 HIGHEST 优先级改写的固定致死值，
        //    再乘一道浮动会把它压到致死线以下（roll 到低段时必杀失效），判定复用分型解析的同一方法。
        // ⚠️ 关闭开关 / spread = 0 时结果不变，整体退回固定出伤。
        if (Config.damageVarianceEnabled
                && Config.damageVarianceSpread > 0.0D
                && event.getAmount() > 0.0F
                && !isExecutionersLethal(source, target))
        {
            RandomSource random = source.getEntity() instanceof LivingEntity livingAttacker
                    ? livingAttacker.getRandom()
                    : target.getRandom();
            float spread = (float) Config.damageVarianceSpread;
            float multiplier = 1.0F + (random.nextFloat() * 2.0F - 1.0F) * spread;
            event.setAmount(event.getAmount() * multiplier);
        }
    }

    /**
     * 区 2：目标防御层（护甲 → 体术抗性 / 术式抗性）。
     * <p>
     * ⚠️ 这是本管线<b>唯一</b>不在 {@link LivingHurtEvent} 里的乘区。原因只有一个：
     * 封顶。原版护甲 / 附魔保护 / 抗性药水都在 {@code LivingHurtEvent} 之后、本事件之前结算，
     * 防御层若留在前面，{@code combatDefense.defenseReductionCap} 只封得住我们自己那一层，
     * 原版护甲叠上来后总减伤会突破上限（70% 体抗 + 钻石甲 80% ≈ 94%）。挂到这里才拿得到
     * 「原版全部算完之后」的真实剩余量。
     * <p>
     * 取值口径（{@code LivingEntity#actuallyHurt} 的实际顺序：护甲 → 附魔抗性 →
     * <b>扣除吸收（金苹果黄心）</b> → 本事件）：
     * <ul>
     *   <li>{@code event.getAmount()} 是「原版全部算完、且已扣过黄心」的量；</li>
     *   <li>所以封顶基准只能用交接来的 {@code baseAmount}（原版减免<b>之前</b>的量），
     *       拿当前值当基准会算出负数上限；</li>
     *   <li>副作用（已知并在设定内）：有黄心时本层作用在扣完黄心的余量上，
     *       收益略低于「先减伤再扣黄心」。</li>
     * </ul>
     * 封顶口径：<b>本层最多把伤害压到 {@code base × floor}，绝不把伤害抬回去</b>。
     * 若原版单独就已经压到那条线以下（护甲 + 保护 IV + 抗性 II 之类），本层只加 0 减伤——
     * 不去撤销原版自己的结算。
     * <p>
     * 数值上这与「留在 LivingHurtEvent」等价（乘法可交换），差别只体现在封顶是否生效，
     * 以及黑闪的 pow 现在位于本层之前（配合区 0 的修复，各乘区本来就都是线性的，故此差异为零）。
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingDamage(LivingDamageEvent event)
    {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide || !SkillDefenseResolver.enabled())
        {
            return;
        }

        DamageSource source = event.getSource();
        Pending pending = consumeIncoming(target, source);
        if (pending == null)
        {
            // 没有交接 ⇒ 这一击不是从 hurt() 进来的（子类直接调 actuallyHurt 之类）。
            // 宁可不加成，也不要用错基准。
            return;
        }

        float amount = event.getAmount();
        if (amount <= 0.0F)
        {
            return;
        }

        LivingEntity attacker = source.getEntity() instanceof LivingEntity living ? living : null;

        double ourMultiplier = 1.0D;
        if (source.is(JjkzDamageTypeTags.IS_TECHNIQUE))
        {
            // 自研护甲% − 穿甲（第一期不对原版护甲层回补穿甲）
            ourMultiplier *= 1.0D - SkillDefenseResolver.armorReduction(target, attacker);
        }
        // 体抗 − 穿甲 / 术抗 − 术穿（按分型）
        ourMultiplier *= 1.0D - SkillDefenseResolver.resistanceReduction(target, pending.type(), attacker);

        // 允许本层再加的减伤上限：把最终伤害卡在 base × floor 之上。
        // 原版削得越狠（amount 越小）这个上限越小，最低到 0 ⇒ 本层不再加任何减伤。
        double allowedReduction = 1.0D - (double) pending.baseAmount()
                * SkillDefenseResolver.multiplierFloor() / amount;
        double appliedReduction = Math.min(1.0D - ourMultiplier, Math.max(0.0D, allowedReduction));

        if (appliedReduction > 0.0D)
        {
            event.setAmount((float) (amount * (1.0D - appliedReduction)));
        }
    }

    // ======== 区 2 的跨事件交接（LivingHurtEvent → LivingDamageEvent） ========

    /**
     * 一次「待结算防御」的现场：原版减免<b>之前</b>的伤害量，以及在这一击上已解析好的分型。
     * <p>
     * {@code type} 可能为 null（通道外、判定不出的伤害 ⇒ 不吃任何抗性）。
     * 留 {@code target} 与 {@code source} 的引用是为了取用时校验：宁可作废，不可用错基准。
     */
    private record Pending(LivingEntity target, DamageSource source, SkillDamageType type,
                           float baseAmount, int tick)
    {
    }

    /**
     * 交接表。两个事件在<b>同一个调用栈</b>上（{@code hurt → actuallyHurt → onLivingHurt →
     * 原版减免 → onLivingDamage}），所以 ThreadLocal 足够，无需弱引用。
     * <p>
     * 用 Map 而不是单个槽位：一次 {@code LivingHurtEvent} 的处理里若有别的实体被伤害（嵌套 hurt），
     * 单槽位会被后一次覆盖，外层那一击就丢掉防御层；按实体 id 分槽互不干扰。
     * <p>
     * 记录只在本 tick 内有效（{@link #rememberIncoming} 顺手清掉旧 tick 的残留），
     * 所以「伤害没走到 LivingDamageEvent」（如结算前被清零）也不会长期堆积。
     */
    private static final ThreadLocal<Map<Integer, Pending>> PENDING_DEFENSE =
            ThreadLocal.withInitial(HashMap::new);

    private static void rememberIncoming(LivingEntity target, DamageSource source,
                                         SkillDamageType type, float baseAmount)
    {
        Map<Integer, Pending> pending = PENDING_DEFENSE.get();
        int tick = target.tickCount;
        pending.values().removeIf(entry -> entry.tick() != tick);
        pending.put(target.getId(), new Pending(target, source, type, baseAmount, tick));
    }

    /** 取出并作废该实体的现场；实体 / DamageSource / tick 任一对不上就返回 null。 */
    @Nullable
    private static Pending consumeIncoming(LivingEntity target, DamageSource source)
    {
        Pending pending = PENDING_DEFENSE.get().remove(target.getId());
        if (pending == null || pending.target() != target || pending.source() != source
                || pending.tick() != target.tickCount)
        {
            return null;
        }
        return pending;
    }

    /**
     * 判定这一击的叙事分型（与 DamageSource 通道解耦）：
     * <ol>
     *   <li>出伤点显式写入的 ThreadLocal（{@code deal} / {@code runWithType}）</li>
     *   <li>处刑人之剑对【死刑】目标的挥击 → 复合</li>
     *   <li>EpicFight 基础攻击 → 体术（含黑闪主拳；冲击波另走 deal 术式）</li>
     *   <li>其它贴身格斗（原版怪物近战 / 原版玩家近战）→ 体术</li>
     *   <li>赫击飞方块砸人 → 术式</li>
     *   <li>十划碎块砸人 → 复合</li>
     *   <li>其余 → null（不吃分型加成，也不吃任何抗性）</li>
     * </ol>
     * 黑闪主拳不标复合：仍是同一记体术结算，只改伤害量。
     * <p>
     * ⚠️ 这里<b>不再</b>要求攻击者是 {@code ServerPlayer}——怪物近战也要能分出体术，
     * 否则体术抗性对怪物完全无效。处刑必杀判据自己做了 {@code instanceof} 收敛。
     */
    @Nullable
    static SkillDamageType resolveDamageType(LivingEntity target, DamageSource source)
    {
        SkillDamageType tagged = TechniqueDamageSources.currentDamageType();
        if (tagged != null)
        {
            return tagged;
        }
        Entity attacker = source.getEntity();
        if (attacker instanceof ServerPlayer serverPlayer)
        {
            if (isExecutionersComposite(serverPlayer, target, source))
            {
                return SkillDamageType.COMPOSITE;
            }
            if (source instanceof EpicFightDamageSource efSource && efSource.isBasicAttack())
            {
                return SkillDamageType.MARTIAL;
            }
        }
        if (TechniqueDamageSources.isMelee(source))
        {
            return SkillDamageType.MARTIAL;
        }
        if (source.getDirectEntity() instanceof FallingBlockEntity falling
                && falling.getPersistentData().getBoolean(TechniqueDamageSources.TAG_RED_DEBRIS))
        {
            return SkillDamageType.TECHNIQUE;
        }
        if (source.getDirectEntity() instanceof FallingBlockEntity falling
                && falling.getPersistentData().getBoolean(TechniqueDamageSources.TAG_RATIO_DEBRIS))
        {
            return SkillDamageType.COMPOSITE;
        }
        return null;
    }

    /** 玩家面板或已挂 {@link LivingCombatStats} 的式神：区 0 体术强度换算。 */
    private static boolean shouldScalePhysicalPower(LivingEntity attacker, LivingEntity target,
                                                    DamageSource source, SkillDamageType type)
    {
        if (attacker instanceof ServerPlayer serverPlayer)
        {
            return !(type == SkillDamageType.COMPOSITE
                    && isExecutionersComposite(serverPlayer, target, source));
        }
        return LivingCombatStats.has(attacker);
    }

    private static boolean isExecutionersComposite(ServerPlayer attacker, LivingEntity target, DamageSource source)
    {
        if (!DeathSentenceEffect.isActive(target))
        {
            return false;
        }
        if (!JudgmentWeaponLogic.isWieldingExecutionersSword(attacker))
        {
            return false;
        }
        // 处刑必杀叠在挥击上：EF 基础攻击，或原版玩家近战直击
        if (source instanceof EpicFightDamageSource efSource && efSource.isBasicAttack())
        {
            return true;
        }
        return source.getDirectEntity() == attacker;
    }

    /**
     * 是否为处刑人之剑对【死刑】目标的必杀一击。
     * <p>
     * 该一击的伤害量在 {@link EventPriority#HIGHEST} 被改写成固定致死值，故区 V 的浮动乘区
     * 必须排除它——否则 roll 到低段会把致死量乘回致死线以下。判定与区 0 的排除共用
     * {@link #isExecutionersComposite}，口径一致。
     */
    private static boolean isExecutionersLethal(DamageSource source, LivingEntity target)
    {
        return source.getEntity() instanceof ServerPlayer attacker
                && isExecutionersComposite(attacker, target, source);
    }

    /** 十划弱点：击破成功则强制本击暴击（浮动乘区仍 roll）。 */
    private static void maybeForceRatioSpotCrit(ServerPlayer attacker, LivingEntity target)
    {
        if (!PlayerTechniqueData.isRatio(attacker))
        {
            return;
        }
        if (RatioSpotLogic.tryBreakSpot(attacker, target))
        {
            CritRules.forceNext(attacker, RatioSpotLogic.forcedCritMult(attacker));
        }
    }

    /** 同一次 hurt 派发结束后清空，避免线程复用脏读。 */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingHurtClearCrit(LivingHurtEvent event)
    {
        if (!event.getEntity().level().isClientSide)
        {
            HitCritState.clear();
        }
    }
}
