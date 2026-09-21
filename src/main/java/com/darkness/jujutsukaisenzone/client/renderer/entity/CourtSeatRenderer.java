package com.darkness.jujutsukaisenzone.client.renderer.entity;

import com.darkness.jujutsukaisenzone.client.model.entity.CourtSeatModel;
import com.darkness.jujutsukaisenzone.entity.CourtSeatEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class CourtSeatRenderer extends GeoEntityRenderer<CourtSeatEntity>
{
    private static final float MODEL_SCALE = 0.45F;

    public CourtSeatRenderer(EntityRendererProvider.Context context)
    {
        super(context, new CourtSeatModel());
        this.withScale(MODEL_SCALE);
        this.shadowRadius = 2.0F * MODEL_SCALE;
    }

    /**
     * GeckoLib 对非 LivingEntity 传入的 rotationYaw 恒为 0，必须直接读实体 yaw。
     */
    @Override
    protected void applyRotations(CourtSeatEntity animatable, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick)
    {
        float yaw = Mth.rotLerp(partialTick, animatable.yRotO, animatable.getYRot());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
    }
}
