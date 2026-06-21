package com.darkness.jujutsukaisenzone.registry;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.client.renderer.entity.DemonSubduingShrineRenderer;
import com.darkness.jujutsukaisenzone.client.renderer.entity.WedgeRenderer;
import com.darkness.jujutsukaisenzone.entity.SpecialEffectEntity;
import com.darkness.jujutsukaisenzone.entity.WedgeEntity;
import net.minecraft.client.renderer.entity.ChickenRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModEntityEvents
{
    private ModEntityEvents()
    {
    }

    @SubscribeEvent
    public static void onEntityAttributes(EntityAttributeCreationEvent event)
    {
        event.put(ModEntities.SPECIAL_EFFECT_ENTITY.get(), SpecialEffectEntity.createAttributes().build());
        event.put(ModEntities.WEDGE.get(), WedgeEntity.createAttributes().build());
    }

    @Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Client
    {
        private Client()
        {
        }

        @SubscribeEvent
        public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event)
        {
            event.registerEntityRenderer(ModEntities.SPECIAL_EFFECT_ENTITY.get(), ChickenRenderer::new);
            event.registerEntityRenderer(ModEntities.DEMON_SUBDUING_SHRINE.get(), DemonSubduingShrineRenderer::new);
            event.registerEntityRenderer(ModEntities.WEDGE.get(), WedgeRenderer::new);
        }
    }
}
