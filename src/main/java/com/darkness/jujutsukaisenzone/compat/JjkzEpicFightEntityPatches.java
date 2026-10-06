package com.darkness.jujutsukaisenzone.compat;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.registry.ModEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import yesman.epicfight.api.client.forgeevent.PatchedRenderersEvent;
import yesman.epicfight.api.client.model.Meshes;
import yesman.epicfight.api.forgeevent.EntityPatchRegistryEvent;
import yesman.epicfight.client.renderer.patched.entity.PHumanoidRenderer;
import yesman.epicfight.gameasset.Armatures;

/**
 * 本模组自定义实体的 Epic Fight patch / armature / patched renderer 注册。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Bus.MOD)
public final class JjkzEpicFightEntityPatches
{
    private JjkzEpicFightEntityPatches()
    {
    }

    @SubscribeEvent
    public static void onEntityPatchRegistry(EntityPatchRegistryEvent event)
    {
        event.getTypeEntry().put(ModEntities.GOJO_SATORU.get(), entity -> JjkzBipedPatch::new);
        Armatures.registerEntityTypeArmature(ModEntities.GOJO_SATORU.get(), Armatures.BIPED);
    }

    @Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Bus.MOD, value = Dist.CLIENT)
    public static final class Client
    {
        private Client()
        {
        }

        @SubscribeEvent
        public static void onPatchedRenderersAdd(PatchedRenderersEvent.Add event)
        {
            event.addPatchedEntityRenderer(
                    ModEntities.GOJO_SATORU.get(),
                    entityType -> new PHumanoidRenderer<>(Meshes.BIPED, event.getContext(), entityType)
                            .initLayerLast(event.getContext(), entityType));
        }
    }
}
