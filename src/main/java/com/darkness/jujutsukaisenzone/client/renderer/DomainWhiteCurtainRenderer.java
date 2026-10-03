package com.darkness.jujutsukaisenzone.client.renderer;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.client.ScreenShakeApi;
import com.mojang.blaze3d.shaders.AbstractUniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/**
 * 领域展开白幕：自定义 shader 全屏遮罩（铺开 → 开洞）。
 * <p>
 * 默认在 {@link RenderLevelStageEvent.Stage#AFTER_CUTOUT_BLOCKS} 绘制（实体叠在白幕上）；
 * 结界/天空盒完成后由服务端通知 {@link #elevateAboveWorld()}，改到
 * {@link RenderLevelStageEvent.Stage#AFTER_LEVEL}，以便开洞透出 jjkz_skyblock。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class DomainWhiteCurtainRenderer
{
    /** 默认总时长：约 5.25 秒（全白额外 1.7s，开洞已双倍提速）。 */
    public static final int DEFAULT_DURATION_TICKS = 105;

    /** 须与 {@code domain_white_curtain.fsh} 的 holdEnd 一致。 */
    private static final float HOLD_END_PROGRESS = 0.838F;
    /** 开洞瞬间中幅度衰减震动。 */
    private static final int HOLE_SHAKE_TICKS = 22;
    private static final float HOLE_SHAKE_MAX_DEG = 5.5F;

    private static boolean animationEnabled;
    private static boolean drawAboveWorld;
    private static boolean holeShakeTriggered;
    private static long animationStartMillis;
    private static long animationTotalMillis;

    private DomainWhiteCurtainRenderer()
    {
    }

    /**
     * 播放一次白幕铺开/开洞动画（初始不盖实体）。
     *
     * @param durationTicks 总时长（tick），至少 1
     */
    public static void play(int durationTicks)
    {
        animationEnabled = true;
        drawAboveWorld = false;
        holeShakeTriggered = false;
        animationStartMillis = System.currentTimeMillis();
        animationTotalMillis = Math.max(durationTicks, 1) * 50L;
    }

    /**
     * 结界构建完成 / 天空盒已放置：改到世界最上（AFTER_LEVEL），开洞不被天空盒盖住。
     */
    public static void elevateAboveWorld()
    {
        if (animationEnabled)
        {
            drawAboveWorld = true;
        }
    }

    public static boolean isAnimating()
    {
        return animationEnabled && System.currentTimeMillis() - animationStartMillis < animationTotalMillis;
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event)
    {
        if (!animationEnabled)
        {
            return;
        }
        RenderLevelStageEvent.Stage expected = drawAboveWorld
                ? RenderLevelStageEvent.Stage.AFTER_LEVEL
                : RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS;
        if (event.getStage() != expected)
        {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null)
        {
            return;
        }

        float progress = resolveProgress();
        if (progress < 0.0F)
        {
            return;
        }

        maybeTriggerHoleShake(progress);

        ShaderInstance shader = DomainWhiteCurtainShaders.get();
        if (shader == null)
        {
            return;
        }

        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        if (width <= 0 || height <= 0)
        {
            return;
        }

        renderFullscreen(shader, progress, width, height);
    }

    private static void maybeTriggerHoleShake(float progress)
    {
        if (holeShakeTriggered || progress < HOLD_END_PROGRESS)
        {
            return;
        }
        holeShakeTriggered = true;
        ScreenShakeApi.beginDecay(HOLE_SHAKE_TICKS, HOLE_SHAKE_MAX_DEG);
    }

    /**
     * @return 当前进度 0..1；动画已结束返回 -1
     */
    private static float resolveProgress()
    {
        long elapsed = System.currentTimeMillis() - animationStartMillis;
        if (elapsed >= animationTotalMillis)
        {
            animationEnabled = false;
            drawAboveWorld = false;
            holeShakeTriggered = false;
            return -1.0F;
        }
        return Math.min(1.0F, (float) elapsed / (float) animationTotalMillis);
    }

    private static void renderFullscreen(ShaderInstance shader, float progress, int width, int height)
    {
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
        setUniformFloat(shader, "Progress", progress);
        setUniformFloat(shader, "Aspect", (float) width / (float) height);

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
}
