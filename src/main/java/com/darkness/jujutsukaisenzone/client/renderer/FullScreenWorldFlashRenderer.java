package com.darkness.jujutsukaisenzone.client.renderer;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/**
 * 全屏白色闪光：在整幅世界画面（方块、实体、粒子、天气等）合成之后绘制，
 * 仍早于 HUD/GUI，因此覆盖整个 3D 视图且不会挡住操作界面。
 * <p>
 * 与 {@link WhiteOverlayLayerRenderer} 分离：后者在 {@link RenderLevelStageEvent.Stage#AFTER_CUTOUT_BLOCKS}，
 * 实体仍会叠在白层之上；本类使用 {@link RenderLevelStageEvent.Stage#AFTER_LEVEL} 实现“闪在一切世界内容之上”。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class FullScreenWorldFlashRenderer
{
    private static boolean animationEnabled = false;
    private static long animationStartMillis = 0L;
    private static long animationTotalMillis = 0L;
    private static long animationFadeInMillis = 0L;
    private static long animationFadeOutMillis = 0L;
    private static float animationPeakAlpha = 0.0F;

    private FullScreenWorldFlashRenderer()
    {
    }

    /**
     * 播放一次全屏闪光（淡入—保持—淡出）。参数为 tick，与蓄力白色滤镜链路无关。
     */
    public static void playFadeAnimation(int totalTicks, int fadeInTicks, int fadeOutTicks, float peakAlpha)
    {
        int safeTotalTicks = Math.max(totalTicks, 1);
        int safeFadeInTicks = Math.max(fadeInTicks, 0);
        int safeFadeOutTicks = Math.max(fadeOutTicks, 0);
        if (safeFadeInTicks + safeFadeOutTicks > safeTotalTicks)
        {
            int overflow = safeFadeInTicks + safeFadeOutTicks - safeTotalTicks;
            safeFadeOutTicks = Math.max(0, safeFadeOutTicks - overflow);
        }

        animationEnabled = true;
        animationStartMillis = System.currentTimeMillis();
        animationTotalMillis = safeTotalTicks * 50L;
        animationFadeInMillis = safeFadeInTicks * 50L;
        animationFadeOutMillis = safeFadeOutTicks * 50L;
        animationPeakAlpha = clamp(peakAlpha, 0.0F, 1.0F);
    }

    public static boolean isAnimating()
    {
        return animationEnabled;
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event)
    {
        if (!animationEnabled)
        {
            return;
        }
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL)
        {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null)
        {
            return;
        }

        float currentAlpha = resolveCurrentAlpha();
        if (currentAlpha <= 0.0F)
        {
            return;
        }
        renderOverlayCube(currentAlpha);
    }

    private static float resolveCurrentAlpha()
    {
        long now = System.currentTimeMillis();
        long elapsed = now - animationStartMillis;
        if (elapsed >= animationTotalMillis)
        {
            animationEnabled = false;
            return 0.0F;
        }

        long holdMillis = Math.max(0L, animationTotalMillis - animationFadeInMillis - animationFadeOutMillis);
        if (elapsed < animationFadeInMillis && animationFadeInMillis > 0L)
        {
            float t = (float) elapsed / (float) animationFadeInMillis;
            return animationPeakAlpha * smoothStep(t);
        }
        if (elapsed < animationFadeInMillis + holdMillis)
        {
            return animationPeakAlpha;
        }
        if (animationFadeOutMillis > 0L)
        {
            long fadeOutElapsed = elapsed - animationFadeInMillis - holdMillis;
            float t = (float) fadeOutElapsed / (float) animationFadeOutMillis;
            return animationPeakAlpha * (1.0F - smoothStep(t));
        }
        return 0.0F;
    }

    private static void renderOverlayCube(float layerAlpha)
    {
        float radius = 256.0F;
        Matrix4f matrix = new Matrix4f();
        int color = 255;
        int alphaInt = Math.round(layerAlpha * 255.0F);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        addFace(buffer, matrix, -radius, -radius, -radius, radius, -radius, -radius, radius, radius, -radius, -radius, radius, -radius, color, alphaInt);
        addFace(buffer, matrix, -radius, -radius, radius, -radius, radius, radius, radius, radius, radius, radius, -radius, radius, color, alphaInt);
        addFace(buffer, matrix, -radius, -radius, -radius, -radius, radius, -radius, -radius, radius, radius, -radius, -radius, radius, color, alphaInt);
        addFace(buffer, matrix, radius, -radius, -radius, radius, -radius, radius, radius, radius, radius, radius, radius, -radius, color, alphaInt);
        addFace(buffer, matrix, -radius, radius, -radius, radius, radius, -radius, radius, radius, radius, -radius, radius, radius, color, alphaInt);
        addFace(buffer, matrix, -radius, -radius, -radius, -radius, -radius, radius, radius, -radius, radius, radius, -radius, -radius, color, alphaInt);

        tesselator.end();

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void addFace(
            BufferBuilder buffer,
            Matrix4f matrix,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float x4, float y4, float z4,
            int color,
            int alphaInt
    )
    {
        buffer.vertex(matrix, x1, y1, z1).color(color, color, color, alphaInt).endVertex();
        buffer.vertex(matrix, x2, y2, z2).color(color, color, color, alphaInt).endVertex();
        buffer.vertex(matrix, x3, y3, z3).color(color, color, color, alphaInt).endVertex();
        buffer.vertex(matrix, x4, y4, z4).color(color, color, color, alphaInt).endVertex();
    }

    private static float clamp(float value, float min, float max)
    {
        if (value < min)
        {
            return min;
        }
        return Math.min(value, max);
    }

    private static float smoothStep(float t)
    {
        float x = clamp(t, 0.0F, 1.0F);
        return x * x * (3.0F - 2.0F * x);
    }
}
