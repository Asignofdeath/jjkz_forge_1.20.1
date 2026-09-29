package com.darkness.jujutsukaisenzone.registry;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.client.renderer.entity.GavelProjectileRenderer;
import com.darkness.jujutsukaisenzone.client.renderer.entity.ChubukushishiRenderer;
import com.darkness.jujutsukaisenzone.client.renderer.entity.CourtSeatRenderer;
import com.darkness.jujutsukaisenzone.client.renderer.entity.DemonSubduingShrineRenderer;
import com.darkness.jujutsukaisenzone.client.renderer.entity.JadeDogRenderer;
import com.darkness.jujutsukaisenzone.client.renderer.entity.JudgeRenderer;
import com.darkness.jujutsukaisenzone.client.renderer.entity.MahoragaRenderer;
import com.darkness.jujutsukaisenzone.client.renderer.entity.WedgeRenderer;
import com.darkness.jujutsukaisenzone.entity.JadeDogEntity;
import com.darkness.jujutsukaisenzone.entity.MahoragaEntity;
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
        event.put(ModEntities.JADE_DOG.get(), JadeDogEntity.createAttributes().build());
        event.put(ModEntities.MAHORAGA.get(), MahoragaEntity.createAttributes().build());
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
            event.registerEntityRenderer(ModEntities.JUDGE.get(), JudgeRenderer::new);
            event.registerEntityRenderer(ModEntities.COURT_SEAT.get(), CourtSeatRenderer::new);
            event.registerEntityRenderer(ModEntities.EXECUTION_SCAFFOLD_CIRCLE.get(), ChubukushishiRenderer::new);
            event.registerEntityRenderer(ModEntities.GAVEL_PROJECTILE.get(), GavelProjectileRenderer::new);
            event.registerEntityRenderer(ModEntities.WEDGE.get(), WedgeRenderer::new);
            event.registerEntityRenderer(ModEntities.JADE_DOG.get(), JadeDogRenderer::new);
            event.registerEntityRenderer(ModEntities.MAHORAGA.get(), MahoragaRenderer::new);
        }
    }
}
