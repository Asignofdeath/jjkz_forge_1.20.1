package com.darkness.jujutsukaisenzone.client.model.entity;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.entity.CourtSeatEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class CourtSeatModel extends DefaultedEntityGeoModel<CourtSeatEntity>
{
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(JuJutsuKaisenZone.MODID, "textures/entity/court_seat_texture.png");

    public CourtSeatModel()
    {
        super(ResourceLocation.fromNamespaceAndPath(JuJutsuKaisenZone.MODID, "court_seat"));
    }

    @Override
    public ResourceLocation getTextureResource(CourtSeatEntity animatable)
    {
        return TEXTURE;
    }
}
