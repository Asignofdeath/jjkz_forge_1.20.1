package com.darkness.jujutsukaisenzone.client.renderer.entity;

import com.darkness.jujutsukaisenzone.client.model.entity.JadeDogModel;
import com.darkness.jujutsukaisenzone.entity.JadeDogEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class JadeDogRenderer extends GeoEntityRenderer<JadeDogEntity>
{
    public JadeDogRenderer(EntityRendererProvider.Context context)
    {
        super(context, new JadeDogModel());
        this.shadowRadius = 0.55F * JadeDogEntity.SCALE_JUVENILE;
    }

    @Override
    public void render(JadeDogEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight)
    {
        this.shadowRadius = 0.55F * entity.getModelScale();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public void scaleModelForRender(float widthScale, float heightScale, PoseStack poseStack,
                                    JadeDogEntity animatable, BakedGeoModel model, boolean isReRender,
                                    float partialTick, int packedLight, int packedOverlay)
    {
        float scale = animatable.getModelScale();
        super.scaleModelForRender(widthScale * scale, heightScale * scale, poseStack, animatable, model,
                isReRender, partialTick, packedLight, packedOverlay);
    }
}
