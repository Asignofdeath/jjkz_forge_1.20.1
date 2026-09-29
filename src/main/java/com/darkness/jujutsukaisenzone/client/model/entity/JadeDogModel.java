package com.darkness.jujutsukaisenzone.client.model.entity;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.entity.JadeDogEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class JadeDogModel extends DefaultedEntityGeoModel<JadeDogEntity>
{
    private static final ResourceLocation WHITE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(JuJutsuKaisenZone.MODID, "textures/entity/jade_dog_white.png");
    private static final ResourceLocation BLACK_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(JuJutsuKaisenZone.MODID, "textures/entity/jade_dog_black.png");
    private static final ResourceLocation BLACK_MODEL =
            ResourceLocation.fromNamespaceAndPath(JuJutsuKaisenZone.MODID, "geo/entity/jade_dog_black.geo.json");

    public JadeDogModel()
    {
        super(ResourceLocation.fromNamespaceAndPath(JuJutsuKaisenZone.MODID, "jade_dog"));
    }

    @Override
    public ResourceLocation getModelResource(JadeDogEntity animatable)
    {
        return animatable.isWhiteVariant() ? super.getModelResource(animatable) : BLACK_MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(JadeDogEntity animatable)
    {
        return animatable.isWhiteVariant() ? WHITE_TEXTURE : BLACK_TEXTURE;
    }
}
