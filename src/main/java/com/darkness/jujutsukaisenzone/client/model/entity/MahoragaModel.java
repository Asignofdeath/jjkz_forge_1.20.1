package com.darkness.jujutsukaisenzone.client.model.entity;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.entity.MahoragaEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class MahoragaModel extends DefaultedEntityGeoModel<MahoragaEntity>
{
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(JuJutsuKaisenZone.MODID, "textures/entity/makora.png");

    public MahoragaModel()
    {
        super(ResourceLocation.fromNamespaceAndPath(JuJutsuKaisenZone.MODID, "makora"));
    }

    @Override
    public ResourceLocation getTextureResource(MahoragaEntity animatable)
    {
        return TEXTURE;
    }
}
