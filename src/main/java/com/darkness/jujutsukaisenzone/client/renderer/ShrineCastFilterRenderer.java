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
 * 伏魔神龛蓄力期间的风格滤镜：世界层染红（AFTER_CUTOUT_BLOCKS）。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ShrineCastFilterRenderer
{
    private static boolean enabled;

    private ShrineCastFilterRenderer()
    {
    }

    public static void enable()
    {
        enabled = true;
    }

    public static void disable()
    {
        enabled = false;
    }

    @SubscribeEvent
    public static void onRenderWorldLayer(RenderLevelStageEvent event)
    {
        if (!enabled || event.getStage() != RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS)
        {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null)
        {
            return;
        }
        renderOverlayCube(255, 0, 0, 0.35F);
    }

    private static void renderOverlayCube(int r, int g, int b, float alpha)
    {
        float radius = 256.0F;
        Matrix4f matrix = new Matrix4f();
        int alphaInt = Math.round(alpha * 255.0F);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        addFace(buffer, matrix, -radius, -radius, -radius, radius, -radius, -radius, radius, radius, -radius, -radius, radius, -radius, r, g, b, alphaInt);
        addFace(buffer, matrix, -radius, -radius, radius, -radius, radius, radius, radius, radius, radius, radius, -radius, radius, r, g, b, alphaInt);
        addFace(buffer, matrix, -radius, -radius, -radius, -radius, radius, -radius, -radius, radius, radius, -radius, -radius, radius, r, g, b, alphaInt);
        addFace(buffer, matrix, radius, -radius, -radius, radius, -radius, radius, radius, radius, radius, radius, radius, -radius, r, g, b, alphaInt);
        addFace(buffer, matrix, -radius, radius, -radius, radius, radius, -radius, radius, radius, radius, -radius, radius, radius, r, g, b, alphaInt);
        addFace(buffer, matrix, -radius, -radius, -radius, -radius, -radius, radius, radius, -radius, radius, radius, -radius, -radius, r, g, b, alphaInt);

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
            int r, int g, int b,
            int alphaInt
    )
    {
        buffer.vertex(matrix, x1, y1, z1).color(r, g, b, alphaInt).endVertex();
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, alphaInt).endVertex();
        buffer.vertex(matrix, x3, y3, z3).color(r, g, b, alphaInt).endVertex();
        buffer.vertex(matrix, x4, y4, z4).color(r, g, b, alphaInt).endVertex();
    }
}
