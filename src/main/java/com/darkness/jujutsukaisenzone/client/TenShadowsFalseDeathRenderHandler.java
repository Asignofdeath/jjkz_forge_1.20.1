package com.darkness.jujutsukaisenzone.client;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 假死躺姿：渲染时把模型绕局部 X 轴转 90°（MVP，正式倒地骨骼后可替换）。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class TenShadowsFalseDeathRenderHandler
{
    private TenShadowsFalseDeathRenderHandler()
    {
    }

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event)
    {
        applyLiePose(event.getPoseStack(), event.getEntity());
    }

    @SubscribeEvent
    public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event)
    {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player)
        {
            /* 玩家已由 RenderPlayerEvent 处理，避免叠两次 */
            return;
        }
        applyLiePose(event.getPoseStack(), entity);
    }

    private static void applyLiePose(PoseStack poseStack, LivingEntity entity)
    {
        if (!ClientTenShadowsFalseDeathState.isFalseDead(entity))
        {
            return;
        }
        float height = entity.getBbHeight() * 0.5F;
        poseStack.translate(0.0D, height * 0.15D, 0.0D);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.translate(0.0D, -height * 0.35D, 0.0D);
    }
}
