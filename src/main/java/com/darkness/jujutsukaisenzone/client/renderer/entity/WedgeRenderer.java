package com.darkness.jujutsukaisenzone.client.renderer.entity;

import com.darkness.jujutsukaisenzone.entity.WedgeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;

public class WedgeRenderer extends EntityRenderer<WedgeEntity>
{
    private static final ResourceLocation END_CRYSTAL_LOCATION =
            ResourceLocation.withDefaultNamespace("textures/entity/end_crystal/end_crystal.png");
    private static final RenderType RENDER_TYPE = RenderType.entityCutoutNoCull(END_CRYSTAL_LOCATION);
    private static final float SIN_45 = (float) Math.sin(Math.PI / 4D);
    private final ModelPart cube;
    private final ModelPart glass;
    private final ModelPart base;

    public WedgeRenderer(EntityRendererProvider.Context context)
    {
        super(context);
        this.shadowRadius = 0.5F;
        ModelPart modelPart = context.bakeLayer(ModelLayers.END_CRYSTAL);
        this.glass = modelPart.getChild("glass");
        this.cube = modelPart.getChild("cube");
        this.base = modelPart.getChild("base");
    }

    @Override
    public void render(WedgeEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight)
    {
        poseStack.pushPose();
        float bob = getBobOffset(entity, partialTicks);
        float spin = ((float) entity.time + partialTicks) * 3.0F;
        VertexConsumer vertexConsumer = buffer.getBuffer(RENDER_TYPE);
        poseStack.pushPose();
        poseStack.scale(2.0F, 2.0F, 2.0F);
        poseStack.translate(0.0F, -0.5F, 0.0F);
        int overlay = OverlayTexture.NO_OVERLAY;
        if (entity.showsBottom())
        {
            this.base.render(poseStack, vertexConsumer, packedLight, overlay);
        }

        poseStack.mulPose(Axis.YP.rotationDegrees(spin));
        poseStack.translate(0.0F, 1.5F + bob / 2.0F, 0.0F);
        poseStack.mulPose(new Quaternionf().setAngleAxis((float) Math.PI / 3F, SIN_45, 0.0F, SIN_45));
        this.glass.render(poseStack, vertexConsumer, packedLight, overlay);
        poseStack.scale(0.875F, 0.875F, 0.875F);
        poseStack.mulPose(new Quaternionf().setAngleAxis((float) Math.PI / 3F, SIN_45, 0.0F, SIN_45));
        poseStack.mulPose(Axis.YP.rotationDegrees(spin));
        this.glass.render(poseStack, vertexConsumer, packedLight, overlay);
        poseStack.scale(0.875F, 0.875F, 0.875F);
        poseStack.mulPose(new Quaternionf().setAngleAxis((float) Math.PI / 3F, SIN_45, 0.0F, SIN_45));
        poseStack.mulPose(Axis.YP.rotationDegrees(spin));
        this.cube.render(poseStack, vertexConsumer, packedLight, overlay);
        poseStack.popPose();
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    public static float getBobOffset(WedgeEntity entity, float partialTick)
    {
        float time = (float) entity.time + partialTick;
        float bob = Mth.sin(time * 0.2F) / 2.0F + 0.5F;
        bob = (bob * bob + bob) * 0.4F;
        return bob - 1.4F;
    }

    @Override
    public ResourceLocation getTextureLocation(WedgeEntity entity)
    {
        return END_CRYSTAL_LOCATION;
    }
}
