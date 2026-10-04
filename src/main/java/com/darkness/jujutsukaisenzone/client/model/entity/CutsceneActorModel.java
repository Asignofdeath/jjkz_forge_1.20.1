package com.darkness.jujutsukaisenzone.client.model.entity;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.entity.CutsceneActorEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class CutsceneActorModel extends DefaultedEntityGeoModel<CutsceneActorEntity>
{
    public CutsceneActorModel()
    {
        super(ResourceLocation.fromNamespaceAndPath(JuJutsuKaisenZone.MODID, CutsceneActorEntity.APPEARANCE_XIAOSHIZI));
    }

    @Override
    public ResourceLocation getModelResource(CutsceneActorEntity animatable)
    {
        return ResourceLocation.fromNamespaceAndPath(
                JuJutsuKaisenZone.MODID, "geo/entity/" + appearance(animatable) + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(CutsceneActorEntity animatable)
    {
        return ResourceLocation.fromNamespaceAndPath(
                JuJutsuKaisenZone.MODID, "textures/entity/" + appearance(animatable) + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(CutsceneActorEntity animatable)
    {
        return ResourceLocation.fromNamespaceAndPath(
                JuJutsuKaisenZone.MODID, "animations/entity/" + appearance(animatable) + ".animation.json");
    }

    private static String appearance(CutsceneActorEntity animatable)
    {
        String id = animatable.getAppearance();
        return id == null || id.isEmpty() ? CutsceneActorEntity.APPEARANCE_XIAOSHIZI : id;
    }
}
