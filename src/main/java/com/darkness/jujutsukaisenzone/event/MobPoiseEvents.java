package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.entity.combat.MobCombatStats;
import com.darkness.jujutsukaisenzone.entity.combat.MobCombatStatsRegistry;
import com.darkness.jujutsukaisenzone.entity.combat.PoiseRules;
import com.darkness.jujutsukaisenzone.entity.combat.PoiseStats;
import com.darkness.jujutsukaisenzone.registry.ModAttributes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;
import yesman.epicfight.api.forgeevent.EntityStunEvent;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.boss.enderdragon.EnderDragonPatch;
import yesman.epicfight.world.damagesource.StunType;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * 怪物韧性条的全部运行时逻辑（数据面 + 扣减面 + 状态面）。
 * <p>
 * 六件事：入世挂盾 / 清盾（{@link #applyPoise}）、夺回 EF 原生的扣盾权（{@link #onEntityStun}）、
 * 玩家命中扣盾（{@link #onLivingHurt}）、空闲回复（{@link #onLivingTick}）、
 * 破韧后的破损态（{@link #enterBroken} / {@link #exitBroken}），
 * 以及供伤害管线取用的减免比例（{@link #damageReductionFor}）。
 * <p>
 * <b>本类不写 {@code event.setAmount}</b>——按本模组铁律，一切伤害乘算只在
 * {@code TechniqueDamageZoneEvents} 里落地（见该类的「区 4 · 韧性条减免」）。
 * 本类只提供 {@link #damageReductionFor(LivingEntity)} 供其调用。
 * <p>
 * <b>为什么复用 Epic Fight 的硬直护盾（stun shield）</b>：它就是「血条下方那根白条」
 * （{@code HealthBar} 在 y ∈ [-0.08, -0.0488] 绘制），而且盾 &gt; 0 时 EF 原生的
 * {@code EntityEvents#hurtEvent} 就会把 SHORT/LONG 硬直降级为 NONE——<b>霸体是白送的</b>，
 * 不用我们再写一遍。盾值走 {@code EntityDataAccessor}，同步与存档也是白送的。
 * <p>
 * <b>但 EF 的盾只挡硬直、不挡伤害</b>，所以「未破时减免伤害」必须另做（见上）。
 * <p>
 * ⛔ <b>为什么扣盾要我们自己写：EF 只在伤害源是 {@code EpicFightDamageSource} 时才碰盾</b>
 * （{@code EntityEvents#hurtEvent} 第 145 行取源、第 195 行要求非空），而本模组的术式伤害
 * 走的是<b>原版 {@code DamageSource}</b>（见 {@code TechniqueDamageSources}）⇒ EF 整个跳过。
 * 更糟的是 EF 自己的近战也只在盾上扣一个 {@code impact}（默认 0.5）——铁傀儡 80 点要砍 160 刀。
 * 所以「术式/近战都打不动韧性条」不是设定，是<b>缺一条结算管线</b>；本类就是那条管线。
 * <p>
 * ⚠️ <b>只有继承了 {@code LivingEntityPatch} 的实体才有真盾。</b>
 * {@code GlobalMobPatch} 继承的是 {@code HurtableEntityPatch}，其 {@code getStunShield()}
 * 恒为 0、{@code setStunShield()} 是空实现，而且 {@code HealthBar} 只认
 * {@code LivingEntityPatch}。所以没有 EF 专属 patch 的怪（监守者、潜影贝等）
 * 当前<b>拿不到</b>韧性条——那是后续「自注册 patch」阶段要补的。
 * <p>
 * ⚠️ <b>末影龙全程豁免</b>：它的盾由 EF 的水晶机制自己管（{@code DragonCrystalLinkPhase}），
 * 我们既不在入世时挂，也不在命中时扣，否则会把 EF 的水晶相位打乱。
 * <p>
 * <b>破损态的语义</b>（阶段 4）：把一条<b>正数</b>的韧性条打到 0 的<b>那一击</b>触发，
 * 持续 {@link PoiseRules#BROKEN_DURATION_TICKS} tick。期间：
 * <ul>
 *   <li>盾清 0 且<b>不回复</b>。⚠️ 「不回复」不是偷懒：EF 的霸体判据就是盾 &gt; impact，
 *       回一点点霸体就会提前回来，「移除霸体增幅」这句话就成了假的。</li>
 *   <li>韧性上限锁到 {@link PoiseRules#BROKEN_SHIELD_CAP_RATIO}。⚠️ 这条<b>不是</b>视觉效果
 *       ——盾为 0 时 {@code HealthBar} 直接 {@code return}，整根白条是<b>消失</b>的，
 *       所以它纯粹是一道兜底闸：万一有别的代码路径在破损期内给盾加值，也回不到满值、
 *       不会把霸体偷偷还回来。</li>
 *   <li>挂虚弱 {@link PoiseRules#BROKEN_WEAKNESS_AMPLIFIER} + 体抗/术抗各
 *       {@link PoiseRules#BROKEN_VULNERABILITY_POINTS} 点的易伤。</li>
 *   <li>到期恢复满上限、撤掉虚弱与易伤，盾从 0 按正常节奏回复。</li>
 * </ul>
 * ⚠️ 破损期内继续命中<b>不会</b>刷新窗口（判据是「这一击把正数的条打到 0」）。
 * 不这样闸的话，玩家一直贴脸打就是永久破防，韧性条沦为一次性的开关。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MobPoiseEvents
{
    /**
     * 最后一次「被玩家命中」的游戏刻，用于韧性回复延迟。
     * <p>
     * key 用<b>实体本身</b>而不是 UUID，并套 {@link WeakHashMap}：怪物被卸载 / 消失后
     * 条目随实体一起回收，省掉一套卸载清理逻辑（{@code Entity#equals} 是同一性比较，
     * 正是这里需要的语义）。{@link #BROKEN_UNTIL} 同理。
     */
    private static final Map<LivingEntity, Long> LAST_PLAYER_HIT =
            Collections.synchronizedMap(new WeakHashMap<>());

    /** 破损态截止的游戏刻；键不在表里 = 未破。 */
    private static final Map<LivingEntity, Long> BROKEN_UNTIL =
            Collections.synchronizedMap(new WeakHashMap<>());

    /** 易伤用的两个修饰符 UUID：与 {@code MobCombatStatsEvents} 的四个<b>必须不同</b>，
     *  否则会把数据包套上的体抗/术抗整个顶掉。 */
    private static final UUID BROKEN_PHYSICAL_RESISTANCE_UUID =
            UUID.fromString("6b2c9a01-3005-4000-8000-000000000005");
    private static final UUID BROKEN_TECHNIQUE_RESISTANCE_UUID =
            UUID.fromString("6b2c9a01-3006-4000-8000-000000000006");
    private static final String BROKEN_MODIFIER_NAME = "jjkz_poise_broken";

    private MobPoiseEvents()
    {
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event)
    {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof LivingEntity living))
        {
            return;
        }
        applyPoise(living);
    }

    /**
     * 按当前生物表给该实体刷新韧性条（表里没有则清空）。
     * <p>
     * ⚠️ 顺序：{@code setStunShield} 内部会 {@code clamp(value, 0, getMaxStunShield())}，
     * <b>必须先设上限再设当前值</b>，反过来会被旧的 0 上限夹成 0。
     * <p>
     * 时序上安全：EF 从 NBT 恢复盾值走 {@code MixinLivingEntity#readAdditionalSaveData}
     * （实体反序列化期间），早于 {@code EntityJoinLevelEvent}，所以这里设的值不会被它覆盖。
     * <p>
     * 顺带做破损态自愈：实体中途卸载时 {@link #BROKEN_UNTIL} 的条目会随实体被回收，
     * 于是 {@code exitBroken} 再也不会跑到，锁住的上限和易伤修饰符必须在这里补清
     * （属性修饰符不落盘，重载后本就不在，但上限与虚弱要擦干净）。
     */
    public static void applyPoise(LivingEntity living)
    {
        clearBroken(living);
        LivingEntityPatch<?> patch = getPatch(living);
        if (patch == null)
        {
            /* 没有 EF 专属 patch 的怪暂不支持（GlobalMobPatch 的盾是空实现）。 */
            return;
        }
        PoiseStats poise = PoiseRules.resolve(living);
        if (poise == null)
        {
            clearPoise(patch);
            return;
        }
        if (isEnderDragon(patch))
        {
            /* 末影龙的盾由 EF 的水晶机制自己管，别抢。 */
            return;
        }
        float value = (float) poise.poise();
        if (patch.getMaxStunShield() == value)
        {
            return;
        }
        patch.setMaxStunShield(value);
        patch.setStunShield(value);
    }

    /**
     * 夺回 EF 原生的扣盾权（阶段 6）。
     * <p>
     * ⛔ <b>不夺回的话，同一下半段伤害会被扣两次盾</b>：EF 系伤害（近战 / EF 技能）除了走我们
     * 的 {@link #onLivingHurt} 扣 {@link PoiseRules#POISE_DAMAGE_BASE}，EF 自己还会在
     * {@code EntityEvents#hurtEvent} 里按 {@code impact} 再扣一次
     * （{@code HurtableEntityPatch#damageStunShield} → {@code shield - impact}）。
     * 而 {@code impact} 来自 IMPACT 属性、<b>不是</b>那个可有可无的默认 0.5：
     * {@code IronGolemPatch} 就把铁傀儡的 IMPACT 设成 6.0 —— 于是术式 8 刀破、EF 近战 5 刀破，
     * 两个通道差了近一倍。
     * <p>
     * 做法是取消 {@link EntityStunEvent}，但<b>只取消 EF 本来就要「什么都不做」的那一类</b>：
     * 当 {@code stunType == NONE} 时（盾 &gt; impact 被 EF 降级掉的情况），EF 后续只会
     * 扣盾 + {@code applyStun(NONE)} + {@code setStunReductionOnHit(NONE)}，
     * 后两者对 NONE 是空操作，取消掉唯一的效果就是「EF 不再扣盾」。真会施加硬直
     * （SHORT/LONG/KNOCKDOWN/NEUTRALIZE/HOLD）的场合一律放行，不改变任何硬直行为。
     * <p>
     * ⚠️ 已知且刻意保留的偏差：EF 的击倒类（KNOCKDOWN/NEUTRALIZE）不吃「盾 &gt; 0 免硬直」
     * 这条（EF 只对 SHORT/LONG 降级），所以那些命中仍会走 EF 的扣盾。要做成「霸体连击倒一起挡」
     * 是设计变更，不在这里顺手改。
     * <p>
     * ⚠️ 唯一被跳过的副作用是 {@code HurtableEntityPatch#setStunReductionOnHit}：
     * 它那个「让硬直抗性随时间回落」的标记在盾完好的期间不再被置位。由于盾完好时本来就
     * 上不了硬直（霸体），这个偏差在游戏性上不可见。
     */
    @SubscribeEvent
    public static void onEntityStun(EntityStunEvent event)
    {
        if (event.getStunType() != StunType.NONE)
        {
            return;
        }
        if (!(event.getStunnedEntityPatch() instanceof LivingEntityPatch<?> patch))
        {
            /* GlobalMobPatch 之类根本没有真盾，EF 那一步是空扣。 */
            return;
        }
        if (patch.getStunShield() <= 0.0F || isEnderDragon(patch))
        {
            /* 破盾后/末影龙：EF 的扣盾要么无意义要么归它自己管，别插手。 */
            return;
        }
        if (PoiseRules.resolve(patch.getOriginal()) == null)
        {
            /* 不是我们挂的条，不动。 */
            return;
        }
        event.setCanceled(true);
    }

    /**
     * 玩家命中时扣韧性（阶段 3 · 破韧结算）。
     * <p>
     * ⚠️ 挂 {@link EventPriority#LOWEST}：要等所有乘区（含区 4 韧性减免）改完
     * {@code amount} 之后再判断这一刀是否真的落地。{@code LivingHurtEvent} 只在
     * {@code actuallyHurt} 内部派发，也就天然过滤掉了无敌帧内被吞掉的重复命中。
     * <p>
     * ⚠️ <b>扣的量与本次伤害无关</b>，固定 {@link PoiseRules#POISE_DAMAGE_BASE}。
     * 理由：破韧必须是一条<b>独立于伤害</b>的进度轴，否则堆伤害的玩家会顺手把所有怪的
     * 韧性条一起秒掉，整根条等于不存在。
     * <p>
     * ⚠️ 只认<b>玩家</b>造成的命中：破韧速度与 Break 效力都是玩家侧的成长轴。
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingHurt(LivingHurtEvent event)
    {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide() || event.isCanceled() || event.getAmount() <= 0.0F)
        {
            return;
        }
        if (!(event.getSource().getEntity() instanceof Player attacker))
        {
            return;
        }
        PoiseStats poise = PoiseRules.resolve(target);
        if (poise == null)
        {
            return;
        }
        /* 先记时刻再判断盾：已破的目标继续挨打时也要把回复往后推。 */
        long now = target.level().getGameTime();
        LAST_PLAYER_HIT.put(target, now);

        LivingEntityPatch<?> patch = getPatch(target);
        if (patch == null || isEnderDragon(patch) || patch.getMaxStunShield() <= 0.0F)
        {
            return;
        }
        float shield = patch.getStunShield();
        if (shield <= 0.0F)
        {
            /* 破损期内（或刚破完还没回）不再往下扣，也不刷新破损窗口。 */
            return;
        }
        /* Break 效力是加速破韧的唯一通道（特质克制表已删除）。 */
        double breakPotency = ModAttributes.breakPotency(attacker);
        float after = (float) Math.max(0.0D,
                shield - PoiseRules.poiseDamage(poise, breakPotency));
        patch.setStunShield(after);
        if (after <= 0.0F)
        {
            /* 判据：这一击把「正数」的条打到了 0 —— 只有这种情况才算破。 */
            enterBroken(target, patch, poise, now);
        }
    }

    /**
     * 韧性回复 + 破损态到期（阶段 3 / 4）。
     * <p>
     * 每 {@link PoiseRules#REGEN_INTERVAL_TICKS} tick 结算一次，且必须距最后一次被玩家命中
     * 超过 {@link PoiseRules#REGEN_DELAY_TICKS}——否则「边打边回」，破不破全看攻速。
     * <p>
     * ⚠️ 取模判断放在最前面：本监听器每 tick 对<b>每个</b>生物派发，先做一次取模能省掉
     * 绝大多数 tick 上的能力查找与注册表查询。
     */
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event)
    {
        LivingEntity living = event.getEntity();
        if (living.level().isClientSide())
        {
            return;
        }
        long now = living.level().getGameTime();
        if (now % PoiseRules.REGEN_INTERVAL_TICKS != 0L)
        {
            return;
        }
        if (living.isDeadOrDying())
        {
            LAST_PLAYER_HIT.remove(living);
            BROKEN_UNTIL.remove(living);
            return;
        }
        Long brokenUntil = BROKEN_UNTIL.get(living);
        if (brokenUntil != null)
        {
            if (now < brokenUntil)
            {
                /* 破损期内禁止回复：盾一旦 > 0，EF 的霸体就会提前回来。 */
                return;
            }
            exitBroken(living);
            return;
        }
        LivingEntityPatch<?> patch = getPatch(living);
        if (patch == null || isEnderDragon(patch))
        {
            LAST_PLAYER_HIT.remove(living);
            return;
        }
        float max = patch.getMaxStunShield();
        if (max <= 0.0F)
        {
            LAST_PLAYER_HIT.remove(living);
            return;
        }
        float shield = patch.getStunShield();
        if (shield >= max)
        {
            LAST_PLAYER_HIT.remove(living);
            return;
        }
        Long last = LAST_PLAYER_HIT.get(living);
        if (last != null && now - last < PoiseRules.REGEN_DELAY_TICKS)
        {
            return;
        }
        PoiseStats poise = PoiseRules.resolve(living);
        if (poise == null)
        {
            return;
        }
        double gain = poise.regenOrDefault() * PoiseRules.REGEN_INTERVAL_TICKS / 20.0D;
        patch.setStunShield((float) Math.min(max, shield + gain));
    }

    /**
     * 进入破损态：锁上限 → 清盾 → 记截止刻 → 挂虚弱与易伤。
     * <p>
     * ⚠️ 又一次「先 setMax 再 setStun」：{@code setStunShield} 会夹到当前上限。
     * <p>
     * 破损态收益<b>恒为满档</b>：破韧速度可以靠 Break 效力调，但「破了之后拿多少」是固定的
     * （虚弱 {@link PoiseRules#BROKEN_WEAKNESS_AMPLIFIER}、易伤各
     * {@link PoiseRules#BROKEN_VULNERABILITY_POINTS} 点），便于玩家建立预期。
     * 早期的「用 Break 硬破就只给半档」那道闸门已随特质克制表一起删除——
     * Break 是唯一解法，再罚它就是自相矛盾。
     */
    private static void enterBroken(LivingEntity living, LivingEntityPatch<?> patch,
                                    PoiseStats poise, long now)
    {
        patch.setMaxStunShield(PoiseRules.brokenShieldCap(poise.poise()));
        patch.setStunShield(0.0F);
        BROKEN_UNTIL.put(living, now + PoiseRules.BROKEN_DURATION_TICKS);
        living.addEffect(new MobEffectInstance(
                MobEffects.WEAKNESS,
                PoiseRules.BROKEN_DURATION_TICKS,
                Math.max(0, PoiseRules.BROKEN_WEAKNESS_AMPLIFIER),
                false,
                true));
        applyVulnerability(living);
    }

    /** 退出破损态：擦掉虚弱与易伤，把韧性上限还回满值（盾从 0 重新开始回）。 */
    private static void exitBroken(LivingEntity living)
    {
        clearBroken(living);
        LivingEntityPatch<?> patch = getPatch(living);
        PoiseStats poise = PoiseRules.resolve(living);
        if (patch == null || isEnderDragon(patch) || poise == null)
        {
            return;
        }
        patch.setMaxStunShield((float) poise.poise());
    }

    /**
     * 擦掉本模组造成的破损态痕迹。
     * <p>
     * ⚠️ 以 {@link #BROKEN_UNTIL} 里是否有本实体为闸：本方法会在<b>每个</b>生物入世时被调用，
     * 不设闸就会把别人（药水等）挂的虚弱一起删掉。
     */
    private static void clearBroken(LivingEntity living)
    {
        if (BROKEN_UNTIL.remove(living) == null)
        {
            return;
        }
        living.removeEffect(MobEffects.WEAKNESS);
        removeVulnerability(living);
    }

    /** 易伤落地点：体抗 / 术抗各扣 {@link PoiseRules#BROKEN_VULNERABILITY_POINTS} 点。 */
    private static void applyVulnerability(LivingEntity living)
    {
        double points = PoiseRules.BROKEN_VULNERABILITY_POINTS;
        setModifier(living, ModAttributes.PHYSICAL_RESISTANCE, BROKEN_PHYSICAL_RESISTANCE_UUID, points);
        setModifier(living, ModAttributes.TECHNIQUE_RESISTANCE, BROKEN_TECHNIQUE_RESISTANCE_UUID, points);
    }

    private static void removeVulnerability(LivingEntity living)
    {
        removeModifier(living, ModAttributes.PHYSICAL_RESISTANCE, BROKEN_PHYSICAL_RESISTANCE_UUID);
        removeModifier(living, ModAttributes.TECHNIQUE_RESISTANCE, BROKEN_TECHNIQUE_RESISTANCE_UUID);
    }

    private static void setModifier(LivingEntity living, RegistryObject<Attribute> attribute,
                                    UUID uuid, double points)
    {
        AttributeInstance instance = living.getAttribute(attribute.get());
        if (instance == null)
        {
            return;
        }
        instance.removeModifier(uuid);
        instance.addPermanentModifier(new AttributeModifier(
                uuid, BROKEN_MODIFIER_NAME,
                -points,
                AttributeModifier.Operation.ADDITION));
    }

    private static void removeModifier(LivingEntity living, RegistryObject<Attribute> attribute, UUID uuid)
    {
        AttributeInstance instance = living.getAttribute(attribute.get());
        if (instance != null)
        {
            instance.removeModifier(uuid);
        }
    }

    /**
     * 韧性未破时该目标的伤害减免比例；不适用时返回 {@code 0}。
     * <p>
     * 调用方（伤害管线）只需把它乘成 {@code 1 - r} 的<b>独立乘区</b>：
     * 该减免刻意不进 {@code resistanceReduction} 通道，否则会被
     * {@code SkillDefenseResolver#applyPenetration} 当成抗性吃掉——那就成了「吃穿甲」，
     * 违反韧性条的设计前提。
     * <p>
     * 破损态下恒为 0（易伤另走 Attribute 层，不在这里混算）。
     */
    public static double damageReductionFor(LivingEntity target)
    {
        if (isBroken(target))
        {
            return 0.0D;
        }
        PoiseStats poise = PoiseRules.resolve(target);
        if (poise == null || !isIntact(target))
        {
            return 0.0D;
        }
        MobCombatStats stats = MobCombatStatsRegistry.get(target.getType());
        double toughness = stats == null ? 0.0D : stats.toughness();
        return PoiseRules.effectiveReduction(poise, toughness);
    }

    /** 韧性是否仍完好（盾 &gt; 0 即霸体期）。 */
    public static boolean isIntact(LivingEntity living)
    {
        LivingEntityPatch<?> patch = getPatch(living);
        return patch != null && patch.getStunShield() > 0.0F;
    }

    /** 是否处于破损态。 */
    public static boolean isBroken(LivingEntity living)
    {
        return BROKEN_UNTIL.containsKey(living);
    }

    /** 清掉本模组挂上的韧性条（数据包把某项删掉或改成 {@code value: 0} 时走这里）。 */
    private static void clearPoise(LivingEntityPatch<?> patch)
    {
        if (patch.getMaxStunShield() <= 0.0F || isEnderDragon(patch))
        {
            return;
        }
        patch.setStunShield(0.0F);
        patch.setMaxStunShield(0.0F);
    }

    @Nullable
    private static LivingEntityPatch<?> getPatch(LivingEntity living)
    {
        return EpicFightCapabilities.getEntityPatch(living, LivingEntityPatch.class);
    }

    private static boolean isEnderDragon(LivingEntityPatch<?> patch)
    {
        return patch instanceof EnderDragonPatch;
    }
}
