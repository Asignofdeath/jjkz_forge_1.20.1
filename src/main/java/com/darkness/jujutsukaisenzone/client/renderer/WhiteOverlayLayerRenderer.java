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
 * 在世界渲染的“方块层之后、实体层之前”插入白色滤镜层。
 * <p>
 * 业务意图：让已渲染的方块被白色覆盖，而后续渲染的生物实体保持显示在白层之上。
 * 重要副作用：该层会覆盖当前帧缓冲中的世界颜色，因此透明度建议保持较低以避免过曝。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class WhiteOverlayLayerRenderer
{
    private static boolean enabled = false;
    private static float alpha = 0.4F;
    private static boolean animationEnabled = false;
    private static long animationStartMillis = 0L;
    private static long animationTotalMillis = 0L;
    private static long animationFadeInMillis = 0L;
    private static long animationFadeOutMillis = 0L;
    private static float animationPeakAlpha = 0.0F;

    private WhiteOverlayLayerRenderer()
    {
    }

    public static void setState(boolean isEnabled, float layerAlpha)
    {
        enabled = isEnabled;
        alpha = clamp(layerAlpha, 0.0F, 1.0F);
        animationEnabled = false;
    }

    /**
     * 播放一次白色滤镜动画（淡入-保持-淡出）。
     * <p>
     * 业务意图：在技能关键时刻制造短时视觉压迫感，不改变常驻命令滤镜状态。
     * 重要假设：时长参数均为 tick，调用方负责保证总时长不小于淡入+淡出时长。
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

    public static boolean isEnabled()
    {
        return enabled;
    }

    public static float getAlpha()
    {
        return alpha;
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event)
    {
        if (!enabled && !animationEnabled)
        {
            return;
        }
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS)
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

    /**
     * 计算当前帧应使用的透明度。
     * <p>
     * 实现简述：若动画激活则按“淡入-保持-淡出”曲线返回透明度；否则返回命令设置的常驻透明度。
     */
    private static float resolveCurrentAlpha()
    {
        if (!animationEnabled)
        {
            return enabled ? alpha : 0.0F;
        }

        long now = System.currentTimeMillis();
        long elapsed = now - animationStartMillis;
        if (elapsed >= animationTotalMillis)
        {
            animationEnabled = false;
            return enabled ? alpha : 0.0F;
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

    /**
     * 通过包围相机的半透明立方体执行覆盖渲染。
     * <p>
     * 关键假设：该阶段矩阵已处于世界空间，顶点坐标以相机为原点可稳定覆盖玩家视野。
     */
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
