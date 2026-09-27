package com.darkness.jujutsukaisenzone.registry;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;

/**
 * 攻防面板属性：抗性（防御）与穿透（攻击）成对；另有玩家效率类属性。
 * <p>
 * 与护甲（{@link net.minecraft.world.entity.ai.attributes.Attributes#ARMOR}）的分工——
 * <b>平行三层</b>，抗性不是护甲子集：
 * <ul>
 *   <li><b>护甲</b>：通用物理层；术式通道因 {@code BYPASSES_ARMOR} 改走自研线性公式</li>
 *   <li><b>体术抗性</b>（{@link #PHYSICAL_RESISTANCE}）：只吃贴身格斗 /
 *       {@code SkillDamageType.MARTIAL}</li>
 *   <li><b>术式抗性</b>（{@link #TECHNIQUE_RESISTANCE}）：只吃 {@code TECHNIQUE}</li>
 * </ul>
 * 攻击侧穿透（见 {@link com.darkness.jujutsukaisenzone.technique.SkillDefenseResolver}）：
 * <ul>
 *   <li><b>穿甲</b>（{@link #PHYSICAL_PENETRATION}）：穿透自研护甲% + 体术抗性</li>
 *   <li><b>术穿</b>（{@link #TECHNIQUE_PENETRATION}）：只穿透术式抗性</li>
 * </ul>
 * 复合伤害两层各按一半计入，与强度均值对称。
 * <p>
 * 效率类（仅玩家）：单位为<b>百分比点数</b>，默认 {@code 100} = 100% 效率。
 * 实际倍率 = {@code getValue() / 100.0}。
 * <p>
 * ⚠️ 抗性/穿透单位是百分比点数（{@code 25} = 25%）。抗性允许负值（易伤）；穿透范围 0~100。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModAttributes
{
    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(ForgeRegistries.ATTRIBUTES, JuJutsuKaisenZone.MODID);

    /** 体术抗性（百分比点数，正数减伤、负数易伤）。 */
    public static final RegistryObject<Attribute> PHYSICAL_RESISTANCE = ATTRIBUTES.register(
            "physical_resistance",
            () -> new RangedAttribute(
                    "attribute.name.jujutsukaisenzone.physical_resistance",
                    0.0D,
                    -100.0D,
                    100.0D)
                    .setSyncable(true));

    /** 术式抗性（百分比点数，正数减伤、负数易伤）。 */
    public static final RegistryObject<Attribute> TECHNIQUE_RESISTANCE = ATTRIBUTES.register(
            "technique_resistance",
            () -> new RangedAttribute(
                    "attribute.name.jujutsukaisenzone.technique_resistance",
                    0.0D,
                    -100.0D,
                    100.0D)
                    .setSyncable(true));

    /** 穿甲：穿透护甲减伤%与体术抗性（百分比点数，默认 0）。 */
    public static final RegistryObject<Attribute> PHYSICAL_PENETRATION = ATTRIBUTES.register(
            "physical_penetration",
            () -> new RangedAttribute(
                    "attribute.name.jujutsukaisenzone.physical_penetration",
                    0.0D,
                    0.0D,
                    100.0D)
                    .setSyncable(true));

    /** 术穿：只穿透术式抗性（百分比点数，默认 0）。 */
    public static final RegistryObject<Attribute> TECHNIQUE_PENETRATION = ATTRIBUTES.register(
            "technique_penetration",
            () -> new RangedAttribute(
                    "attribute.name.jujutsukaisenzone.technique_penetration",
                    0.0D,
                    0.0D,
                    100.0D)
                    .setSyncable(true));

    /**
     * 反转术式效率（百分比点数，默认 100 = 100%）。
     * 实际治疗倍率 = 属性值 / 100。
     */
    public static final RegistryObject<Attribute> REVERSE_TECHNIQUE_EFFICIENCY = ATTRIBUTES.register(
            "reverse_technique_efficiency",
            () -> new RangedAttribute(
                    "attribute.name.jujutsukaisenzone.reverse_technique_efficiency",
                    100.0D,
                    0.0D,
                    500.0D)
                    .setSyncable(true));

    /**
     * 领域量表充能效率（百分比点数，默认 100 = 100%）。
     * 与脑损效率相乘：最终灌充倍率 = (属性值/100) × 脑损档位系数。
     */
    public static final RegistryObject<Attribute> DOMAIN_GAUGE_CHARGE_EFFICIENCY = ATTRIBUTES.register(
            "domain_gauge_charge_efficiency",
            () -> new RangedAttribute(
                    "attribute.name.jujutsukaisenzone.domain_gauge_charge_efficiency",
                    100.0D,
                    0.0D,
                    500.0D)
                    .setSyncable(true));

    /**
     * 暴击几率（百分比点数，默认 5 = 5%）。硬顶见 {@code CritRules.CHANCE_HARD_CAP_POINTS}。
     * 无暴击伤害属性：暴击成功后由随机浮动乘区增幅伤害。
     */
    public static final RegistryObject<Attribute> CRIT_CHANCE = ATTRIBUTES.register(
            "crit_chance",
            () -> new RangedAttribute(
                    "attribute.name.jujutsukaisenzone.crit_chance",
                    5.0D,
                    0.0D,
                    60.0D)
                    .setSyncable(true));

    /**
     * 破韧效力（百分比点数，默认 0 = 0%）。
     * <p>
     * 作用于怪物韧性条的破韧速度：{@code poiseDmg = base × (1 + B_eff)}，
     * 其中 {@code B_eff = normalize(值/100) × (特攻怪 ? poiseResistance : 1)}。
     * <b>软上限 +80%（超出部分收益减半）、硬上限 +150%</b>，见
     * {@code PoiseRules#BREAK_POTENCY_SOFT_CAP} 一族——上限取 150 就是为了对齐硬上限。
     * <p>
     * ⚠️ 它是<b>唯一</b>能加速破韧的通道（咒力特质克制表已删除），所以在特攻怪身上会被打折
     * （{@code PoiseStats#poiseResistance}）——不打折的话堆 Break 的玩家会把所有怪的条一起秒掉，
     * 韧性条本身就没意义了。
     */
    public static final RegistryObject<Attribute> BREAK_POTENCY = ATTRIBUTES.register(
            "break_potency",
            () -> new RangedAttribute(
                    "attribute.name.jujutsukaisenzone.break_potency",
                    0.0D,
                    0.0D,
                    150.0D)
                    .setSyncable(true));

    private ModAttributes()
    {
    }

    public static void register(IEventBus bus)
    {
        ATTRIBUTES.register(bus);
    }

    /** 反转术式治疗倍率（无属性时 1.0）。 */
    public static double reverseTechniqueEfficiencyMultiplier(@Nullable LivingEntity entity)
    {
        return efficiencyMultiplier(entity, REVERSE_TECHNIQUE_EFFICIENCY);
    }

    /** 领域量表面板充能倍率（不含脑损；无属性时 1.0）。 */
    public static double domainGaugeChargeEfficiencyMultiplier(@Nullable LivingEntity entity)
    {
        return efficiencyMultiplier(entity, DOMAIN_GAUGE_CHARGE_EFFICIENCY);
    }

    /**
     * 破韧效力的<b>原始比值</b>（{@code 属性值 / 100}，未经软/硬上限折算）。
     * 无属性（非玩家等）时 0.
     * <p>
     * ⚠️ 拿到的值<b>不能</b>直接乘进破韧公式：还差特攻怪打折与软上限两道，
     * 那两步在 {@code PoiseRules#effectiveBreakPotency} 里做，别在这里重复实现。
     */
    public static double breakPotency(@Nullable LivingEntity entity)
    {
        return efficiencyMultiplier(entity, BREAK_POTENCY);
    }

    private static double efficiencyMultiplier(@Nullable LivingEntity entity, RegistryObject<Attribute> attribute)
    {
        if (entity == null)
        {
            return 1.0D;
        }
        AttributeInstance instance = entity.getAttribute(attribute.get());
        if (instance == null)
        {
            return 1.0D;
        }
        return Math.max(0.0D, instance.getValue()) / 100.0D;
    }

    /**
     * ⚠️ 属性必须挂到实体类型上才读得到，否则 {@code entity.getAttribute(...)} 返回 {@code null}。
     * 抗性/穿透挂全部生物；效率类仅玩家。
     */
    @SubscribeEvent
    public static void onEntityAttributeModification(EntityAttributeModificationEvent event)
    {
        event.getTypes().forEach(entityType ->
        {
            event.add(entityType, PHYSICAL_RESISTANCE.get());
            event.add(entityType, TECHNIQUE_RESISTANCE.get());
            event.add(entityType, PHYSICAL_PENETRATION.get());
            event.add(entityType, TECHNIQUE_PENETRATION.get());
        });
        event.add(EntityType.PLAYER, REVERSE_TECHNIQUE_EFFICIENCY.get());
        event.add(EntityType.PLAYER, DOMAIN_GAUGE_CHARGE_EFFICIENCY.get());
        event.add(EntityType.PLAYER, CRIT_CHANCE.get());
        event.add(EntityType.PLAYER, BREAK_POTENCY.get());
    }
}
