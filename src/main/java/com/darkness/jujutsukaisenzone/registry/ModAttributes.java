package com.darkness.jujutsukaisenzone.registry;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 攻防面板属性：抗性（防御）与穿透（攻击）成对。
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
 * ⚠️ 单位是<b>百分比点数</b>（{@code 25} = 25%）。抗性允许负值（易伤）；穿透范围 0~100。
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

    private ModAttributes()
    {
    }

    public static void register(IEventBus bus)
    {
        ATTRIBUTES.register(bus);
    }

    /**
     * ⚠️ 属性必须挂到实体类型上才读得到，否则 {@code entity.getAttribute(...)} 返回 {@code null}。
     * 给所有已注册属性的生物类型批量追加（含怪），否则抗性 / 穿透无处存放。
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
    }
}
