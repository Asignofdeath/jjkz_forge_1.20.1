package com.darkness.jujutsukaisenzone.client.renderer.entity;

import com.darkness.jujutsukaisenzone.client.model.entity.CutsceneActorModel;
import com.darkness.jujutsukaisenzone.entity.CutsceneActorEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class CutsceneActorRenderer extends GeoEntityRenderer<CutsceneActorEntity>
{
    public CutsceneActorRenderer(EntityRendererProvider.Context context)
    {
        super(context, new CutsceneActorModel());
        this.shadowRadius = 0.5F;
    }

    /**
     * GeckoLib 对非 LivingEntity 传入的 rotationYaw 恒为 0，必须直接读实体 yaw。
     */
    @Override
    protected void applyRotations(CutsceneActorEntity animatable, PoseStack poseStack, float ageInTicks,
            float rotationYaw, float partialTick)
    {
        float yaw = Mth.rotLerp(partialTick, animatable.yRotO, animatable.getYRot());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
    }
}
