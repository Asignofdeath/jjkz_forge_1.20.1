package com.darkness.jujutsukaisenzone.client.renderer.entity;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.entity.Grade3CurseSpiritAEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * 三级咒灵甲：暂复用尸壳模型。
 */
public class Grade3CurseSpiritARenderer
        extends HumanoidMobRenderer<Grade3CurseSpiritAEntity, HumanoidModel<Grade3CurseSpiritAEntity>>
{
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(JuJutsuKaisenZone.MODID, "textures/entity/grade_3_curse_spirit_a.png");

    public Grade3CurseSpiritARenderer(EntityRendererProvider.Context context)
    {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.HUSK)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(Grade3CurseSpiritAEntity entity)
    {
        return TEXTURE;
    }
}
