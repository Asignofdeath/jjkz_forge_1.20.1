package com.darkness.jujutsukaisenzone.client.renderer.entity;

import com.darkness.jujutsukaisenzone.client.model.entity.JudgeModel;
import com.darkness.jujutsukaisenzone.entity.JudgeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class JudgeRenderer extends GeoEntityRenderer<JudgeEntity>
{
    private static final float MODEL_SCALE = 1.25F;

    public JudgeRenderer(EntityRendererProvider.Context context)
    {
        super(context, new JudgeModel());
        this.withScale(MODEL_SCALE);
        this.shadowRadius = 2.5F * MODEL_SCALE;
    }

    @Override
    protected void applyRotations(JudgeEntity animatable, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick)
    {
        float yaw = Mth.rotLerp(partialTick, animatable.yRotO, animatable.getYRot());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
    }
}
