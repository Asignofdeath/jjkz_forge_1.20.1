package com.darkness.jujutsukaisenzone.client.renderer;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.mojang.blaze3d.shaders.AbstractUniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/**
 * 领域展开「扫描波」：从球心向外扩张再收回的双线白环。
 * <p>
 * 与{@link DomainWhiteCurtainRenderer 白幕}不同，本效果<strong>不依赖后处理与深度缓冲</strong>：
 * 逐像素用 {@code InvViewProj} 反投影出视线，再解析地求环带位置。代价是环不被地形遮挡，
 * 也没有折射 / 补色（core shader 读不到主帧缓冲）—— 取舍细节见 {@code domain_scan_wave.fsh}。
 * <p>
 * 绘制时点固定在 {@link RenderLevelStageEvent.Stage#AFTER_CUTOUT_BLOCKS}：实体叠在环之上。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class DomainScanWaveRenderer
{
    /** 半径曲线中点：前半程 0 → 峰值，后半程峰值 → 0。 */
    private static final float MID_PROGRESS = 0.5F;
    /** 起始淡入结束的进度。 */
    private static final float FADE_IN_END_PROGRESS = 0.05F;
    /** 收尾淡出开始的进度。 */
    private static final float FADE_OUT_START_PROGRESS = 0.90F;
    /** 低于此 alpha 不提交绘制。 */
    private static final float MIN_ALPHA = 0.004F;

    private static boolean enabled;
    private static long startMillis;
    private static long totalMillis;
    private static double centerX;
    private static double centerY;
    private static double centerZ;
    private static float peakRadius;

    private DomainScanWaveRenderer()
    {
    }

    /**
     * 播放一次扫描波（扩张 → 收回）。
     *
     * @param x                球心世界 X
     * @param y                球心世界 Y
     * @param z                球心世界 Z
     * @param totalTicks       总时长（tick），至少 1
     * @param peakRadiusBlocks 峰值半径（世界格）
     */
    public static void play(double x, double y, double z, int totalTicks, float peakRadiusBlocks)
    {
        centerX = x;
        centerY = y;
        centerZ = z;
        peakRadius = Math.max(0.0F, peakRadiusBlocks);
        enabled = true;
        startMillis = System.currentTimeMillis();
        totalMillis = Math.max(totalTicks, 1) * 50L;
    }

    public static boolean isPlaying()
    {
        return enabled && System.currentTimeMillis() - startMillis < totalMillis;
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event)
    {
        if (!enabled)
        {
            return;
        }

        float progress = resolveProgress();
        if (progress < 0.0F)
        {
            return;
        }

        float radius = resolveRadius(progress);
        float intensity = resolveIntensity(progress);
        if (radius <= 0.0F || intensity <= 0.0F)
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

        ShaderInstance shader = DomainScanWaveShaders.get();
        if (shader == null)
        {
            return;
        }

        /* 自本波开始以来的秒数：shader 用它做脉动与放射条纹的旋转。 */
        float timeSeconds = (System.currentTimeMillis() - startMillis) / 1000.0F;

        renderFullscreen(event.getCamera(), shader, radius, intensity, timeSeconds);
    }

    /**
     * @return 当前进度 0..1；动画已结束返回 -1（并复位状态）
     */
    private static float resolveProgress()
    {
        long elapsed = System.currentTimeMillis() - startMillis;
        if (elapsed >= totalMillis)
        {
            enabled = false;
            return -1.0F;
        }
        return (float) elapsed / (float) totalMillis;
    }

    /** 半径走 0 → 峰值 → 0 的三角波。 */
    private static float resolveRadius(float progress)
    {
        float triangle = progress < MID_PROGRESS
                ? progress / MID_PROGRESS
                : (1.0F - progress) / (1.0F - MID_PROGRESS);
        return peakRadius * clamp01(triangle);
    }

    /** 首尾各做一次淡入淡出，避免半径接近 0 时整屏闪一下。 */
    private static float resolveIntensity(float progress)
    {
        return smoothStep(0.0F, FADE_IN_END_PROGRESS, progress)
                * (1.0F - smoothStep(FADE_OUT_START_PROGRESS, 1.0F, progress));
    }

    private static void renderFullscreen(Camera camera, ShaderInstance shader, float radius, float intensity, float timeSeconds)
    {
        /*
         * 必须在改写投影 / 模型视图之前抓取：本效果靠「世界 -> clip」的逆矩阵把像素还原成视线。
         * 两个矩阵都走 RenderSystem（此刻仍是层级渲染的透视投影 + 相机模型视图），同源避免口径错位。
         *
         * shader 里只用 near/far 反投影结果之差求视线方向，故模型视图是否含相机平移不影响正确性；
         * 相机世界坐标另行由 Camera#getPosition 传入。
         */
        Matrix4f view = new Matrix4f(RenderSystem.getModelViewMatrix());
        Matrix4f invViewProj = new Matrix4f(RenderSystem.getProjectionMatrix()).mul(view).invert();

        Vec3 cameraPos = camera.getPosition();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();

        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f(), VertexSorting.ORTHOGRAPHIC_Z);
        RenderSystem.getModelViewStack().pushPose();
        RenderSystem.getModelViewStack().setIdentity();
        RenderSystem.applyModelViewMatrix();

        RenderSystem.setShader(() -> shader);
        setUniformVec3(shader, "Center", centerX, centerY, centerZ);
        setUniformVec3(shader, "CameraPos", cameraPos.x, cameraPos.y, cameraPos.z);
        setUniformFloat(shader, "Radius", radius);
        setUniformFloat(shader, "Intensity", intensity);
        setUniformFloat(shader, "Time", timeSeconds);
        setUniformMatrix(shader, "InvViewProj", invViewProj);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        Matrix4f matrix = new Matrix4f();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.vertex(matrix, -1.0F, -1.0F, 0.0F).uv(0.0F, 0.0F).endVertex();
        buffer.vertex(matrix, 1.0F, -1.0F, 0.0F).uv(1.0F, 0.0F).endVertex();
        buffer.vertex(matrix, 1.0F, 1.0F, 0.0F).uv(1.0F, 1.0F).endVertex();
        buffer.vertex(matrix, -1.0F, 1.0F, 0.0F).uv(0.0F, 1.0F).endVertex();
        tesselator.end();

        RenderSystem.getModelViewStack().popPose();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.restoreProjectionMatrix();

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void setUniformFloat(ShaderInstance shader, String name, float value)
    {
        AbstractUniform uniform = shader.safeGetUniform(name);
        if (uniform != null)
        {
            uniform.set(value);
        }
    }

    private static void setUniformVec3(ShaderInstance shader, String name, double x, double y, double z)
    {
        AbstractUniform uniform = shader.safeGetUniform(name);
        if (uniform != null)
        {
            uniform.set((float) x, (float) y, (float) z);
        }
    }

    private static void setUniformMatrix(ShaderInstance shader, String name, Matrix4f value)
    {
        AbstractUniform uniform = shader.safeGetUniform(name);
        if (uniform != null)
        {
            uniform.set(value);
        }
    }

    private static float clamp01(float value)
    {
        return value < 0.0F ? 0.0F : (value > 1.0F ? 1.0F : value);
    }

    private static float smoothStep(float edge0, float edge1, float x)
    {
        if (edge1 <= edge0)
        {
            return x < edge0 ? 0.0F : 1.0F;
        }
        float t = clamp01((x - edge0) / (edge1 - edge0));
        return t * t * (3.0F - 2.0F * t);
    }
}
