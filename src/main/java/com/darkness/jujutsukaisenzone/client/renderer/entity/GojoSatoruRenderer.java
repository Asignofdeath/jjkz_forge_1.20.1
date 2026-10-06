package com.darkness.jujutsukaisenzone.client.renderer.entity;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.entity.GojoSatoruEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * 五条悟正主原版 Renderer：Steve 人形，贴图 {@code gojosatoru_teacher_noeyemask.png}。
 * Epic Fight 接管后由 {@code Meshes.BIPED} 绘制，贴图仍走这里的 {@link #getTextureLocation}。
 */
public class GojoSatoruRenderer
        extends HumanoidMobRenderer<GojoSatoruEntity, HumanoidModel<GojoSatoruEntity>>
{
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            JuJutsuKaisenZone.MODID, "textures/entity/gojosatoru_teacher_noeyemask.png");

    public GojoSatoruRenderer(EntityRendererProvider.Context context)
    {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(GojoSatoruEntity entity)
    {
        return TEXTURE;
    }
}
