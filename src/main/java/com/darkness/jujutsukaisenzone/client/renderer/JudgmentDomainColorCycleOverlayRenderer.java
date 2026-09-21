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
 * 诛伏赐死：红黄青循环闪现的世界层滤镜（方块层之后、实体层之前）。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class JudgmentDomainColorCycleOverlayRenderer
{
    private static final int[][] COLORS = {
            {255, 0, 0},
            {255, 255, 0},
            {0, 255, 255}
    };
    private static final int COLOR_COUNT = COLORS.length;
    private static final int CYCLE_COUNT = 4;
    private static final long TOTAL_MILLIS = 4000L;
    private static final float PEAK_ALPHA = 0.42F;
    private static final float FLASH_FADE_IN_RATIO = 0.22F;
    private static final float FLASH_HOLD_RATIO = 0.38F;

    private static boolean animationEnabled;
    private static long animationStartMillis;
    private static int colorR;
    private static int colorG;
    private static int colorB;

    private JudgmentDomainColorCycleOverlayRenderer()
    {
    }

    public static void playCycleFlash()
    {
        animationEnabled = true;
        animationStartMillis = System.currentTimeMillis();
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event)
    {
        if (!animationEnabled || event.getStage() != RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS)
        {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null)
        {
            return;
        }

        float currentAlpha = resolveCurrentAlphaAndColor();
        if (currentAlpha <= 0.0F)
        {
            return;
        }
        renderOverlayCube(currentAlpha);
    }

    private static float resolveCurrentAlphaAndColor()
    {
        long now = System.currentTimeMillis();
        long elapsed = now - animationStartMillis;
        if (elapsed >= TOTAL_MILLIS)
        {
            animationEnabled = false;
            return 0.0F;
        }

        int totalSegments = COLOR_COUNT * CYCLE_COUNT;
        long segmentMillis = TOTAL_MILLIS / totalSegments;
        int segmentIndex = (int) Math.min(elapsed / segmentMillis, totalSegments - 1);
        int colorIndex = segmentIndex % COLOR_COUNT;
        colorR = COLORS[colorIndex][0];
        colorG = COLORS[colorIndex][1];
        colorB = COLORS[colorIndex][2];

        long withinSegment = elapsed - segmentIndex * segmentMillis;
        float segmentProgress = segmentMillis <= 0L ? 1.0F : (float) withinSegment / (float) segmentMillis;
        return PEAK_ALPHA * resolveFlashEnvelope(segmentProgress);
    }

    private static float resolveFlashEnvelope(float t)
    {
        float x = clamp(t, 0.0F, 1.0F);
        float fadeInEnd = FLASH_FADE_IN_RATIO;
        float holdEnd = fadeInEnd + FLASH_HOLD_RATIO;
        if (x < fadeInEnd && fadeInEnd > 0.0F)
        {
            return smoothStep(x / fadeInEnd);
        }
        if (x < holdEnd)
        {
            return 1.0F;
        }
        float fadeOutSpan = 1.0F - holdEnd;
        if (fadeOutSpan <= 0.0F)
        {
            return 0.0F;
        }
        return 1.0F - smoothStep((x - holdEnd) / fadeOutSpan);
    }

    private static void renderOverlayCube(float layerAlpha)
    {
        float radius = 256.0F;
        Matrix4f matrix = new Matrix4f();
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

        addFace(buffer, matrix, -radius, -radius, -radius, radius, -radius, -radius, radius, radius, -radius, -radius, radius, -radius, alphaInt);
        addFace(buffer, matrix, -radius, -radius, radius, -radius, radius, radius, radius, radius, radius, radius, -radius, radius, alphaInt);
        addFace(buffer, matrix, -radius, -radius, -radius, -radius, radius, -radius, -radius, radius, radius, -radius, -radius, radius, alphaInt);
        addFace(buffer, matrix, radius, -radius, -radius, radius, -radius, radius, radius, radius, radius, radius, radius, -radius, alphaInt);
        addFace(buffer, matrix, -radius, radius, -radius, radius, radius, -radius, radius, radius, radius, -radius, radius, radius, alphaInt);
        addFace(buffer, matrix, -radius, -radius, -radius, -radius, -radius, radius, radius, -radius, radius, radius, -radius, -radius, alphaInt);

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
            int alphaInt
    )
    {
        buffer.vertex(matrix, x1, y1, z1).color(colorR, colorG, colorB, alphaInt).endVertex();
        buffer.vertex(matrix, x2, y2, z2).color(colorR, colorG, colorB, alphaInt).endVertex();
        buffer.vertex(matrix, x3, y3, z3).color(colorR, colorG, colorB, alphaInt).endVertex();
        buffer.vertex(matrix, x4, y4, z4).color(colorR, colorG, colorB, alphaInt).endVertex();
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
