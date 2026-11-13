package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.heaven.scale.ExternalMobPowerScale;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 天道适配器 {@link ExternalMobPowerScale} 的 Forge 事件壳。
 * 逻辑见 {@code heaven.scale}；本类只订阅 join / hurt。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class VanillaMobHealthScaleEvents
{
    private VanillaMobHealthScaleEvents()
    {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onEntityJoinLevel(EntityJoinLevelEvent event)
    {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof LivingEntity living))
        {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel serverLevel))
        {
            return;
        }
        ExternalMobPowerScale.applyOnJoin(living, serverLevel.getServer());
    }

    /**
     * 在护甲结算前按 √生命缩放放大攻击方伤害；不改属性，避免与近战属性伤叠乘。
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingHurt(LivingHurtEvent event)
    {
        if (event.getEntity().level().isClientSide())
        {
            return;
        }
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker))
        {
            return;
        }
        if (!ExternalMobPowerScale.shouldScale(attacker))
        {
            return;
        }
        double healthScale = ExternalMobPowerScale.readHealthScale(attacker);
        if (healthScale <= 1.0001D)
        {
            return;
        }
        float factor = (float) Math.sqrt(healthScale);
        event.setAmount(event.getAmount() * factor);
    }
}
