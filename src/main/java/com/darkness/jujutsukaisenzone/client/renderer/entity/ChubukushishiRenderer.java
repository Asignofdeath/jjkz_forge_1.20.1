package com.darkness.jujutsukaisenzone.client.renderer.entity;

import com.darkness.jujutsukaisenzone.client.model.entity.ChubukushishiModel;
import com.darkness.jujutsukaisenzone.entity.ChubukushishiEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class ChubukushishiRenderer extends GeoEntityRenderer<ChubukushishiEntity>
{
    private static final float MODEL_SCALE = 0.8F;

    public ChubukushishiRenderer(EntityRendererProvider.Context context)
    {
        super(context, new ChubukushishiModel());
        this.withScale(MODEL_SCALE);
        this.shadowRadius = 2.5F * MODEL_SCALE;
    }
}
