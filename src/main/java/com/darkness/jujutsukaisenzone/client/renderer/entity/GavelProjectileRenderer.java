package com.darkness.jujutsukaisenzone.client.renderer.entity;

import com.darkness.jujutsukaisenzone.entity.GavelProjectileEntity;
import com.darkness.jujutsukaisenzone.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * 渲染飞行中的法槌 Blockbench Java 模型（非 GROUND 掉落物样式）。
 */
public class GavelProjectileRenderer extends EntityRenderer<GavelProjectileEntity>
{
    private static final float MODEL_SCALE = 1.25F;

    private final ItemRenderer itemRenderer;

    public GavelProjectileRenderer(EntityRendererProvider.Context context)
    {
        super(context);
        this.itemRenderer = context.getItemRenderer();
        this.shadowRadius = 0.35F;
    }

    @Override
    public void render(
            GavelProjectileEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight)
    {
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.15F, 0.0F);
        applyFlightRotation(entity, partialTick, poseStack);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);

        ItemStack stack = entity.getItem();
        if (stack.isEmpty())
        {
            stack = new ItemStack(ModItems.SM_GAVEL_WEAPON.get());
        }

        this.itemRenderer.renderStatic(
                stack,
                ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                buffer,
                entity.level(),
                entity.getId());

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private static void applyFlightRotation(GavelProjectileEntity entity, float partialTick, PoseStack poseStack)
    {
        Vec3 motion = entity.getDeltaMovement();
        if (motion.lengthSqr() > 1.0E-7D)
        {
            float yaw = (float) (Mth.atan2(motion.x, motion.z) * (180.0F / (float) Math.PI));
            float pitch = (float) (Mth.atan2(motion.y, motion.horizontalDistance()) * (180.0F / (float) Math.PI));
            poseStack.mulPose(Axis.YP.rotationDegrees(yaw - 90.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
            return;
        }

        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, entity.yRotO, entity.getYRot()) - 90.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partialTick, entity.xRotO, entity.getXRot())));
    }

    @Override
    public ResourceLocation getTextureLocation(GavelProjectileEntity entity)
    {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
