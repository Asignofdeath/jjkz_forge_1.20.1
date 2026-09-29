package com.darkness.jujutsukaisenzone.client.renderer.entity;

import com.darkness.jujutsukaisenzone.client.model.entity.MahoragaModel;
import com.darkness.jujutsukaisenzone.entity.MahoragaEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class MahoragaRenderer extends GeoEntityRenderer<MahoragaEntity>
{
    public MahoragaRenderer(EntityRendererProvider.Context context)
    {
        super(context, new MahoragaModel());
        this.withScale(MahoragaEntity.MODEL_SCALE);
        this.shadowRadius = 1.1F * MahoragaEntity.MODEL_SCALE;
    }
}
