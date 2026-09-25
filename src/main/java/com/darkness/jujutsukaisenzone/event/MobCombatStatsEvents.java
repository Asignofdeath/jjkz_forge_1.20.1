package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.entity.combat.MobCombatStats;
import com.darkness.jujutsukaisenzone.entity.combat.MobCombatStatsRegistry;
import com.darkness.jujutsukaisenzone.entity.combat.MobCombatStatsReloadListener;
import com.darkness.jujutsukaisenzone.registry.ModAttributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;

import java.util.UUID;

/**
 * 生物战斗属性表：数据包热重载 + 实体入世时套表。
 * <p>
 * 入世（含区块加载）时先卸掉本模组固定 UUID 的修饰符，再按当前表重挂——
 * {@code /reload} 后已存在的怪下次加载也会跟上新表。不作用于玩家。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MobCombatStatsEvents
{
    private static final UUID PHYSICAL_RESISTANCE_UUID =
            UUID.fromString("6b2c9a01-3001-4000-8000-000000000001");
    private static final UUID TECHNIQUE_RESISTANCE_UUID =
            UUID.fromString("6b2c9a01-3002-4000-8000-000000000002");
    private static final UUID PHYSICAL_PENETRATION_UUID =
            UUID.fromString("6b2c9a01-3003-4000-8000-000000000003");
    private static final UUID TECHNIQUE_PENETRATION_UUID =
            UUID.fromString("6b2c9a01-3004-4000-8000-000000000004");
    private static final String MODIFIER_NAME = "jjkz_mob_combat_stats";

    private MobCombatStatsEvents()
    {
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event)
    {
        event.addListener(new MobCombatStatsReloadListener());
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event)
    {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof LivingEntity living))
        {
            return;
        }
        if (living instanceof Player)
        {
            return;
        }
        applyFromTable(living);
    }

    /** 按当前注册表刷新该生物的四项修饰符（表里没有则清掉旧修饰符）。 */
    public static void applyFromTable(LivingEntity living)
    {
        MobCombatStats stats = MobCombatStatsRegistry.get(living.getType());
        if (stats == null)
        {
            clearAll(living);
            return;
        }
        setModifier(living, ModAttributes.PHYSICAL_RESISTANCE, PHYSICAL_RESISTANCE_UUID,
                stats.physicalResistance());
        setModifier(living, ModAttributes.TECHNIQUE_RESISTANCE, TECHNIQUE_RESISTANCE_UUID,
                stats.techniqueResistance());
        setModifier(living, ModAttributes.PHYSICAL_PENETRATION, PHYSICAL_PENETRATION_UUID,
                stats.physicalPenetration());
        setModifier(living, ModAttributes.TECHNIQUE_PENETRATION, TECHNIQUE_PENETRATION_UUID,
                stats.techniquePenetration());
    }

    private static void clearAll(LivingEntity living)
    {
        removeModifier(living, ModAttributes.PHYSICAL_RESISTANCE, PHYSICAL_RESISTANCE_UUID);
        removeModifier(living, ModAttributes.TECHNIQUE_RESISTANCE, TECHNIQUE_RESISTANCE_UUID);
        removeModifier(living, ModAttributes.PHYSICAL_PENETRATION, PHYSICAL_PENETRATION_UUID);
        removeModifier(living, ModAttributes.TECHNIQUE_PENETRATION, TECHNIQUE_PENETRATION_UUID);
    }

    private static void setModifier(LivingEntity living, RegistryObject<Attribute> attribute,
                                    UUID uuid, double amount)
    {
        AttributeInstance instance = living.getAttribute(attribute.get());
        if (instance == null)
        {
            return;
        }
        instance.removeModifier(uuid);
        if (amount == 0.0D)
        {
            return;
        }
        instance.addPermanentModifier(new AttributeModifier(
                uuid, MODIFIER_NAME, amount, AttributeModifier.Operation.ADDITION));
    }

    private static void removeModifier(LivingEntity living, RegistryObject<Attribute> attribute, UUID uuid)
    {
        AttributeInstance instance = living.getAttribute(attribute.get());
        if (instance != null)
        {
            instance.removeModifier(uuid);
        }
    }
}
