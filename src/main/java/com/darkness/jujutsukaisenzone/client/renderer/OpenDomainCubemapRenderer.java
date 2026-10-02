package com.darkness.jujutsukaisenzone.client.renderer;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.noodlegamer76.shadered.client.renderer.ComplexPassRenderer;
import com.noodlegamer76.shadered.client.util.skyblock.SkyBoxRenderer;
import com.noodlegamer76.shadered.client.util.skyblock.SkyblockPass;
import com.noodlegamer76.shadered.client.util.skyblock.SkyboxTranslation;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * 开放式领域：在球心处以半径绘制球体窗口，内部采样与 jjkz_skyblock 相同的立方体贴图 FBO。
 */
public final class OpenDomainCubemapRenderer
{
    private static final int SPHERE_SEGMENTS = 48;
    private static final int SPHERE_RINGS = 24;

    private static boolean active;
    private static double centerX;
    private static double centerY;
    private static double centerZ;
    private static float radius;
    private static ResourceLocation textureFolder;

    private OpenDomainCubemapRenderer()
    {
    }

    public static void activate(double x, double y, double z, int domainRadius, ResourceLocation skyboxTextureFolder)
    {
        active = true;
        centerX = x;
        centerY = y;
        centerZ = z;
        radius = Math.max(1.0F, domainRadius);
        textureFolder = skyboxTextureFolder;
        OpenDomainBlockGradeRenderer.activate(x, y, z, domainRadius);
    }

    public static void deactivate()
    {
        active = false;
        textureFolder = null;
        OpenDomainBlockGradeRenderer.deactivate();
    }

    public static boolean isActive()
    {
        return active && textureFolder != null;
    }

    public static void renderInSkyblockPass(PoseStack levelPoseStack, ComplexPassRenderer renderer)
    {
        if (!isActive())
        {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null)
        {
            return;
        }

        TextureTarget readTarget = renderer.getRenderBuffer();
        TextureTarget skyboxTarget = renderer.getWriteBuffer();

        skyboxTarget.bindWrite(true);
        levelPoseStack.pushPose();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        SkyBoxRenderer.renderBlockSkybox(
                levelPoseStack,
                textureFolder,
                GameRenderer.getPositionTexColorShader(),
                new SkyboxTranslation()
        );
        levelPoseStack.popPose();
        readTarget.bindWrite(true);

        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();

        PoseStack poseStack = new PoseStack();
        poseStack.mulPoseMatrix(levelPoseStack.last().pose());
        poseStack.translate(centerX - cameraPos.x, centerY - cameraPos.y, centerZ - cameraPos.z);
        poseStack.scale(radius, radius, radius);

        ShaderInstance shader = SkyblockPass.NORMAL.getShader();
        if (shader == null)
        {
            return;
        }

        RenderSystem.getModelViewStack().pushPose();
        RenderSystem.getModelViewStack().setIdentity();
        RenderSystem.applyModelViewMatrix();

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShader(() -> shader);
        shader.setSampler("Skybox", skyboxTarget.getColorTextureId());
        shader.setSampler("PassDepth", readTarget.getDepthTextureId());

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        emitUnitSphere(buffer, poseStack.last().pose());
        tesselator.end();

        RenderSystem.enableCull();
        RenderSystem.getModelViewStack().popPose();
        RenderSystem.applyModelViewMatrix();
    }

    private static void emitUnitSphere(BufferBuilder buffer, Matrix4f matrix)
    {
        for (int ring = 0; ring < SPHERE_RINGS; ring++)
        {
            float v0 = (float) ring / SPHERE_RINGS;
            float v1 = (float) (ring + 1) / SPHERE_RINGS;
            float phi0 = (float) (Math.PI * (v0 - 0.5F));
            float phi1 = (float) (Math.PI * (v1 - 0.5F));
            float y0 = (float) Math.sin(phi0);
            float y1 = (float) Math.sin(phi1);
            float r0 = (float) Math.cos(phi0);
            float r1 = (float) Math.cos(phi1);

            for (int seg = 0; seg < SPHERE_SEGMENTS; seg++)
            {
                float u0 = (float) seg / SPHERE_SEGMENTS;
                float u1 = (float) (seg + 1) / SPHERE_SEGMENTS;
                float theta0 = (float) (u0 * Math.PI * 2.0D);
                float theta1 = (float) (u1 * Math.PI * 2.0D);

                float x00 = r0 * (float) Math.cos(theta0);
                float z00 = r0 * (float) Math.sin(theta0);
                float x01 = r0 * (float) Math.cos(theta1);
                float z01 = r0 * (float) Math.sin(theta1);
                float x10 = r1 * (float) Math.cos(theta0);
                float z10 = r1 * (float) Math.sin(theta0);
                float x11 = r1 * (float) Math.cos(theta1);
                float z11 = r1 * (float) Math.sin(theta1);

                put(buffer, matrix, x00, y0, z00);
                put(buffer, matrix, x01, y0, z01);
                put(buffer, matrix, x11, y1, z11);
                put(buffer, matrix, x10, y1, z10);
            }
        }
    }

    private static void put(BufferBuilder buffer, Matrix4f matrix, float x, float y, float z)
    {
        buffer.vertex(matrix, x, y, z).uv(0.0F, 0.0F).color(255, 255, 255, 255).endVertex();
    }
}
