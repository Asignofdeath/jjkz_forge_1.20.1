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
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * A 档屏幕空间径向扭曲：主帧缓冲 ping-pong，向投影中心拉扯像素。
 * <p>
 * 调试命令（{@code /jjkz spaceWarp}）可强制开关；否则在开放式无量空处激活时，
 * 按玩家到球壳表面的距离自动驱动强度（越近越强）。
 * <p>
 * 在 {@link RenderLevelStageEvent.Stage#AFTER_LEVEL} 执行。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class SpaceWarpRenderer
{
    /** 默认强度（与命令未指定时一致）。 */
    public static final float DEFAULT_INTENSITY = 0.55F;
    /** 宽高比校正后的作用半径（UV 空间）。 */
    private static final float DEFAULT_RADIUS = 0.55F;
    /**
     * 开放式领域边界扭曲带厚度（格）：
     * 距球壳表面超过此值则强度为 0，贴在表面时为 1。
     */
    private static final float OPEN_DOMAIN_BOUNDARY_BAND = 18.0F;

    /** 调试命令强制开启时优先于领域自动强度。 */
    private static boolean manualActive;
    private static float manualIntensity = DEFAULT_INTENSITY;

    private static TextureTarget colorScratch;
    private static final Matrix4f VIEW_PROJ = new Matrix4f();
    private static final Matrix4f TMP = new Matrix4f();
    private static final PoseStack CAMERA_VIEW = new PoseStack();
    private static final Vector4f CLIP = new Vector4f();

    private SpaceWarpRenderer()
    {
    }

    /**
     * 调试命令入口：强制开关与强度。关闭后恢复由开放式领域边界自动驱动。
     */
    public static void setState(boolean enabled, float intensityValue)
    {
        manualActive = enabled;
        manualIntensity = Math.max(0.0F, Math.min(1.0F, intensityValue));
    }

    public static boolean isActive()
    {
        return resolveIntensity(Minecraft.getInstance().player) > 0.001F;
    }

    public static float getIntensity()
    {
        return resolveIntensity(Minecraft.getInstance().player);
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event)
    {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL)
        {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (minecraft.level == null || player == null)
        {
            return;
        }

        float intensity = resolveIntensity(player);
        if (intensity <= 0.001F)
        {
            return;
        }

        ShaderInstance shader = SpaceWarpShaders.get();
        if (shader == null)
        {
            return;
        }

        RenderTarget main = minecraft.getMainRenderTarget();
        ensureScratch(main.width, main.height);

        float centerU = 0.5F;
        float centerV = 0.5F;
        boolean domainDriven = !manualActive && OpenDomainBlockGradeRenderer.isActive();
        if (domainDriven)
        {
            if (projectWorldPointToUv(
                    event,
                    OpenDomainBlockGradeRenderer.getCenterX(),
                    OpenDomainBlockGradeRenderer.getCenterY(),
                    OpenDomainBlockGradeRenderer.getCenterZ()))
            {
                centerU = CLIP.x;
                centerV = CLIP.y;
            }
        }
        else
        {
            Vec3 eye = player.getEyePosition(event.getPartialTick());
            if (projectWorldPointToUv(event, eye.x, eye.y, eye.z))
            {
                centerU = CLIP.x;
                centerV = CLIP.y;
            }
        }

        // 1) 主缓冲颜色 → scratch
        colorScratch.bindWrite(true);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, main.getColorTextureId());
        drawFullscreenUnitQuad();

        // 2) scratch → 主缓冲（径向扭曲）
        main.bindWrite(false);
        RenderSystem.setShader(() -> shader);
        shader.setSampler("DiffuseSampler", colorScratch.getColorTextureId());

        float aspect = main.height > 0 ? (float) main.width / (float) main.height : 1.0F;
        setUniformVec2(shader, "CenterUV", centerU, centerV);
        setUniformFloat(shader, "Intensity", intensity);
        setUniformFloat(shader, "Radius", DEFAULT_RADIUS);
        setUniformFloat(shader, "Aspect", aspect);

        drawFullscreenUnitQuad();

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        minecraft.getMainRenderTarget().bindWrite(false);
    }

    private static float resolveIntensity(LocalPlayer player)
    {
        if (manualActive)
        {
            return manualIntensity;
        }
        return computeOpenDomainBoundaryIntensity(player);
    }

    /**
     * 距开放式领域球壳越近强度越大；内外两侧同一套带状衰减。
     */
    private static float computeOpenDomainBoundaryIntensity(LocalPlayer player)
    {
        if (player == null || !OpenDomainBlockGradeRenderer.isActive())
        {
            return 0.0F;
        }
        double dx = player.getX() - OpenDomainBlockGradeRenderer.getCenterX();
        double dy = player.getY() - OpenDomainBlockGradeRenderer.getCenterY();
        double dz = player.getZ() - OpenDomainBlockGradeRenderer.getCenterZ();
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double toSurface = Math.abs(dist - OpenDomainBlockGradeRenderer.getRadius());
        if (toSurface >= OPEN_DOMAIN_BOUNDARY_BAND)
        {
            return 0.0F;
        }
        return (float) (1.0D - toSurface / OPEN_DOMAIN_BOUNDARY_BAND);
    }

    /**
     * 将世界坐标投到 UV；成功时把结果写进 {@link #CLIP} 的 xy（0..1），返回 true。
     */
    private static boolean projectWorldPointToUv(RenderLevelStageEvent event, double worldX, double worldY, double worldZ)
    {
        Camera camera = event.getCamera();
        Vec3 cam = camera.getPosition();

        CAMERA_VIEW.setIdentity();
        CAMERA_VIEW.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
        CAMERA_VIEW.mulPose(Axis.YP.rotationDegrees(camera.getYRot() + 180.0F));

        VIEW_PROJ.set(event.getProjectionMatrix());
        VIEW_PROJ.mul(CAMERA_VIEW.last().pose());

        CLIP.set(
                (float) (worldX - cam.x),
                (float) (worldY - cam.y),
                (float) (worldZ - cam.z),
                1.0F
        );
        VIEW_PROJ.transform(CLIP);
        if (Math.abs(CLIP.w) < 1.0e-5F)
        {
            return false;
        }
        float invW = 1.0F / CLIP.w;
        float ndcX = CLIP.x * invW;
        float ndcY = CLIP.y * invW;
        if (CLIP.w <= 0.0F || ndcX < -1.5F || ndcX > 1.5F || ndcY < -1.5F || ndcY > 1.5F)
        {
            return false;
        }
        CLIP.x = ndcX * 0.5F + 0.5F;
        CLIP.y = ndcY * 0.5F + 0.5F;
        return true;
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

    private static void setUniformVec2(ShaderInstance shader, String name, float x, float y)
    {
        Uniform uniform = shader.getUniform(name);
        if (uniform != null)
        {
            uniform.set(x, y);
        }
    }
}
