package com.darkness.jujutsukaisenzone.client;

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
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/**
 * 主线 1.2：{@code ghost_ambient} 开播后，将天空 / 雾染为血色并保持，直至服务端清除。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class MainQuest12BloodSkyOverlay
{
    private static final int FADE_IN_TICKS = 60;
    private static final float PEAK_BLEND = 0.88F;
    private static final float FOG_R = 0.58F;
    private static final float FOG_G = 0.05F;
    private static final float FOG_B = 0.05F;
    private static final int OVERLAY_R = 160;
    private static final int OVERLAY_G = 12;
    private static final int OVERLAY_B = 12;
    private static final float OVERLAY_PEAK_ALPHA = 0.32F;

    private static boolean active;
    private static long fadeInStartMillis;

    private MainQuest12BloodSkyOverlay()
    {
    }

    public static boolean isActive()
    {
        return active && currentBlend() > 0.0F;
    }

    public static void show()
    {
        active = true;
        fadeInStartMillis = System.currentTimeMillis();
    }

    public static void hide()
    {
        active = false;
    }

    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event)
    {
        float blend = currentBlend();
        if (blend <= 0.0F)
        {
            return;
        }
        event.setRed(lerp(event.getRed(), FOG_R, blend));
        event.setGreen(lerp(event.getGreen(), FOG_G, blend));
        event.setBlue(lerp(event.getBlue(), FOG_B, blend));
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event)
    {
        if (!active || event.getStage() != RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS)
        {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null)
        {
            return;
        }
        float blend = currentBlend();
        if (blend <= 0.0F)
        {
            return;
        }
        renderOverlayCube(blend * OVERLAY_PEAK_ALPHA);
    }

    private static float currentBlend()
    {
        if (!active)
        {
            return 0.0F;
        }
        long elapsed = System.currentTimeMillis() - fadeInStartMillis;
        long fadeInMillis = FADE_IN_TICKS * 50L;
        if (fadeInMillis <= 0L)
        {
            return PEAK_BLEND;
        }
        float t = Math.min(1.0F, (float) elapsed / (float) fadeInMillis);
        return PEAK_BLEND * smoothStep(t);
    }

    private static void renderOverlayCube(float layerAlpha)
    {
        float radius = 256.0F;
        Matrix4f matrix = new Matrix4f();
        int alphaInt = Math.round(layerAlpha * 255.0F);
        if (alphaInt <= 0)
        {
            return;
        }

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
            int alphaInt)
    {
        buffer.vertex(matrix, x1, y1, z1).color(OVERLAY_R, OVERLAY_G, OVERLAY_B, alphaInt).endVertex();
        buffer.vertex(matrix, x2, y2, z2).color(OVERLAY_R, OVERLAY_G, OVERLAY_B, alphaInt).endVertex();
        buffer.vertex(matrix, x3, y3, z3).color(OVERLAY_R, OVERLAY_G, OVERLAY_B, alphaInt).endVertex();
        buffer.vertex(matrix, x4, y4, z4).color(OVERLAY_R, OVERLAY_G, OVERLAY_B, alphaInt).endVertex();
    }

    private static float lerp(float from, float to, float t)
    {
        return from + (to - from) * t;
    }

    private static float smoothStep(float t)
    {
        float x = Math.min(1.0F, Math.max(0.0F, t));
        return x * x * (3.0F - 2.0F * x);
    }
}
