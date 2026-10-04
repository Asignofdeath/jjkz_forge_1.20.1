package com.darkness.jujutsukaisenzone.client.renderer.entity;

import com.darkness.jujutsukaisenzone.entity.Grade3CurseSpiritAEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * 三级咒灵甲：暂复用尸壳模型与贴图。
 */
public class Grade3CurseSpiritARenderer
        extends HumanoidMobRenderer<Grade3CurseSpiritAEntity, HumanoidModel<Grade3CurseSpiritAEntity>>
{
    private static final ResourceLocation HUSK_TEXTURE =
            new ResourceLocation("textures/entity/zombie/husk.png");

    public Grade3CurseSpiritARenderer(EntityRendererProvider.Context context)
    {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.HUSK)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(Grade3CurseSpiritAEntity entity)
    {
        return HUSK_TEXTURE;
    }
}
