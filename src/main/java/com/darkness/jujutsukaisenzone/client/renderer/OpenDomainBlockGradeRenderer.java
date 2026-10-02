package com.darkness.jujutsukaisenzone.client.renderer;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/**
 * 开放式无量空处：按深度重建相机相对坐标，只对球内可见表面调色。
 * <p>
 * 与 {@link OpenDomainCubemapRenderer} 共用激活态；在
 * {@link RenderLevelStageEvent.Stage#AFTER_CUTOUT_BLOCKS} 对主帧缓冲 ping-pong 着色
 * （实体 / Shadered 天空盒绘制前，只影响已画的不透明与裁剪层）。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class OpenDomainBlockGradeRenderer
{
    private static boolean active;
    private static double centerX;
    private static double centerY;
    private static double centerZ;
    private static float radius;

    private static TextureTarget colorScratch;
    private static final Matrix4f INV_VIEW_PROJ = new Matrix4f();
    private static final Matrix4f TMP = new Matrix4f();
    private static final PoseStack CAMERA_VIEW = new PoseStack();

    private OpenDomainBlockGradeRenderer()
    {
    }

    public static void activate(double x, double y, double z, int domainRadius)
    {
        active = true;
        centerX = x;
        centerY = y;
        centerZ = z;
        radius = Math.max(1.0F, domainRadius);
    }

    public static void deactivate()
    {
        active = false;
    }

    public static boolean isActive()
    {
        return active;
    }

    public static double getCenterX()
    {
        return centerX;
    }

    public static double getCenterY()
    {
        return centerY;
    }

    public static double getCenterZ()
    {
        return centerZ;
    }

    public static float getRadius()
    {
        return radius;
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
        ShaderInstance shader = OpenDomainBlockGradeShaders.get();
        if (shader == null)
        {
            return;
        }

        RenderTarget main = minecraft.getMainRenderTarget();
        ensureScratch(main.width, main.height);

        // 1) 主缓冲颜色 → scratch
        colorScratch.bindWrite(true);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, main.getColorTextureId());
        drawFullscreenUnitQuad();
        colorScratch.copyDepthFrom(main);

        // 2) scratch → 主缓冲（颜色+深度都从 scratch 读，避免读写同一附件）
        main.bindWrite(false);
        RenderSystem.setShader(() -> shader);
        shader.setSampler("DiffuseSampler", colorScratch.getColorTextureId());
        shader.setSampler("DepthSampler", colorScratch.getDepthTextureId());

        Camera camera = event.getCamera();
        buildInvViewProj(event.getProjectionMatrix(), camera);
        setUniformMat4(shader, "InvViewProjMat", INV_VIEW_PROJ);

        Vec3 cam = camera.getPosition();
        setUniformVec3(
                shader,
                "DomainCenterRel",
                (float) (centerX - cam.x),
                (float) (centerY - cam.y),
                (float) (centerZ - cam.z)
        );
        setUniformFloat(shader, "DomainRadius", radius);
        setUniformFloat(shader, "Time", (minecraft.level.getGameTime() + event.getPartialTick()) * 0.05F);

        drawFullscreenUnitQuad();

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        minecraft.getMainRenderTarget().bindWrite(false);
    }

    /**
     * 与 GameRenderer 世界绘制一致：仅相机旋转，几何为相机相对坐标。
     */
    private static void buildInvViewProj(Matrix4f projection, Camera camera)
    {
        CAMERA_VIEW.setIdentity();
        CAMERA_VIEW.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
        CAMERA_VIEW.mulPose(Axis.YP.rotationDegrees(camera.getYRot() + 180.0F));
        INV_VIEW_PROJ.set(projection);
        INV_VIEW_PROJ.mul(CAMERA_VIEW.last().pose());
        INV_VIEW_PROJ.invert();
    }

    private static void ensureScratch(int width, int height)
    {
        if (colorScratch == null || colorScratch.width != width || colorScratch.height != height)
        {
            if (colorScratch != null)
            {
                colorScratch.destroyBuffers();
            }
            colorScratch = new TextureTarget(width, height, true, Minecraft.ON_OSX);
            colorScratch.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        }
    }

    private static void drawFullscreenUnitQuad()
    {
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f(), VertexSorting.ORTHOGRAPHIC_Z);
        RenderSystem.getModelViewStack().pushPose();
        RenderSystem.getModelViewStack().setIdentity();
        RenderSystem.applyModelViewMatrix();

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        Matrix4f matrix = TMP.identity();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.vertex(matrix, -1.0F, -1.0F, 0.0F).uv(0.0F, 0.0F).endVertex();
        buffer.vertex(matrix, 1.0F, -1.0F, 0.0F).uv(1.0F, 0.0F).endVertex();
        buffer.vertex(matrix, 1.0F, 1.0F, 0.0F).uv(1.0F, 1.0F).endVertex();
        buffer.vertex(matrix, -1.0F, 1.0F, 0.0F).uv(0.0F, 1.0F).endVertex();
        tesselator.end();

        RenderSystem.getModelViewStack().popPose();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.restoreProjectionMatrix();
    }

    private static void setUniformFloat(ShaderInstance shader, String name, float value)
    {
        Uniform uniform = shader.getUniform(name);
        if (uniform != null)
        {
            uniform.set(value);
        }
    }

    private static void setUniformVec3(ShaderInstance shader, String name, float x, float y, float z)
    {
        Uniform uniform = shader.getUniform(name);
        if (uniform != null)
        {
            uniform.set(x, y, z);
        }
    }

    private static void setUniformMat4(ShaderInstance shader, String name, Matrix4f matrix)
    {
        Uniform uniform = shader.getUniform(name);
        if (uniform != null)
        {
            uniform.set(matrix);
        }
    }
}
