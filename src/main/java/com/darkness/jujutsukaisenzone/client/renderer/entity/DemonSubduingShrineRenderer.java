package com.darkness.jujutsukaisenzone.client.renderer.entity;

import com.darkness.jujutsukaisenzone.client.model.entity.DemonSubduingShrineModel;
import com.darkness.jujutsukaisenzone.entity.DemonSubduingShrineEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class DemonSubduingShrineRenderer extends GeoEntityRenderer<DemonSubduingShrineEntity>
{
    private static final float MODEL_SCALE = 0.5F;

    public DemonSubduingShrineRenderer(EntityRendererProvider.Context context)
    {
        super(context, new DemonSubduingShrineModel());
        this.withScale(MODEL_SCALE);
        this.shadowRadius = 2.5F * MODEL_SCALE;
    }
}
