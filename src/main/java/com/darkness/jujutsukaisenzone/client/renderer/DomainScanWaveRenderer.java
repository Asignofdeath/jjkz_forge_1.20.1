package com.darkness.jujutsukaisenzone.client.renderer;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.UUID;
import org.joml.Matrix4f;
import org.slf4j.Logger;

import static com.mojang.blaze3d.platform.GlConst.GL_DRAW_FRAMEBUFFER;

/**
 * 领域展开「扫描波」：以球心为中心、在世界空间里扩张再收回的球壳波。
 * <p>
 * 走的是<strong>原版 PostChain 后处理 + 深度缓冲反投影</strong>（与{@link DomainWhiteCurtainRenderer 白幕}同一套设施）。
 * 逐像素从深度反投影出真实的表面世界坐标，再算它到球心的距离，因此亮环是<strong>贴在地形上的等高线</strong>：
 * 会顺着坡面起伏、会被山体挡住 —— 这是屏幕空间近似做不到的。
 * <p>
 * 执行时点固定在 {@link RenderLevelStageEvent.Stage#AFTER_LEVEL}：此刻世界已画完、深度完整，且 GUI 尚未绘制。
 * <p>
 * <strong>球心每帧跟踪施法者脚底</strong>：包里的坐标只是快照兜底，只要施法者还在客户端实体列表里，
 * 就用它<em>当前</em>的脚底位置当球心（连带插值）。前摇期间不锁操作、玩家可自由移动 ——
 * 固定球心会让人一走波心就留在原地，看起来「不是从脚下发出的」。
 * <p>
 * 与 JCraft 时停特效（{@code jcraft:za_warudo}）的差别：去掉色相反转 / 鱼眼折射，
 * 只保留「球壳扫描」与「扫过即去饱和、收回即复原」。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class DomainScanWaveRenderer
{
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * 后处理链定义：只声明 {@code swap} / {@code depthMain} 两个目标，pass 由 {@link PostChain#addPass} 挂。
     * <p>
     * 之所以不用 JSON 声明 pass：{@code PostChain.passes} 是 private，而 {@code addPass} 会返回
     * {@link PostPass}，我们靠它拿 {@link EffectInstance} 设置每帧 uniform。
     */
    private static final ResourceLocation CHAIN_LOCATION =
            ResourceLocation.fromNamespaceAndPath(JuJutsuKaisenZone.MODID, "shaders/post/domain_scan_wave.json");
    /** 本效果的程序名（对应 shaders/program/domain_scan_wave.*）。 */
    private static final String WAVE_PROGRAM = JuJutsuKaisenZone.MODID + ":domain_scan_wave";
    /** 回拷程序：把 swap 的画面画回主帧缓冲。 */
    private static final String BLIT_PROGRAM = "minecraft:blit";
    private static final String DEPTH_SAMPLER = "DepthSampler";
    private static final String SWAP_TARGET = "swap";
    private static final String DEPTH_TARGET = "depthMain";

    /** 半径曲线中点：前半程 0 → 峰值，后半程峰值 → 0。 */
    private static final float MID_PROGRESS = 0.5F;
    /** 起始淡入结束的进度。 */
    private static final float FADE_IN_END_PROGRESS = 0.05F;
    /** 收尾淡出开始的进度。 */
    private static final float FADE_OUT_START_PROGRESS = 0.90F;
    /** 低于此强度不提交绘制。 */
    private static final float MIN_INTENSITY = 0.004F;

    private static PostChain chain;
    private static PostPass wavePass;
    private static RenderTarget depthTarget;
    private static int chainWidth = -1;
    private static int chainHeight = -1;
    /** 是否需要（重）建后处理链；尝试失败后置 false，等下一次资源重载再试，避免每帧刷日志。 */
    private static boolean chainOutdated = true;

    private static boolean enabled;
    private static long startMillis;
    private static long totalMillis;
    /** 施法者 UUID：每帧用它去客户端实体列表里找活人，找到就跟踪其脚底。 */
    private static UUID casterUuid;
    /** 快照球心 —— 仅在施法者不在客户端实体列表（走远 / 换维度）时兜底。 */
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
     * @param casterUuid       施法者 UUID，用于每帧跟踪其脚底当球心；为 {@code null} 时只用快照坐标
     * @param x                兜底球心世界 X
     * @param y                兜底球心世界 Y
     * @param z                兜底球心世界 Z
     * @param totalTicks       总时长（tick），至少 1
     * @param peakRadiusBlocks 峰值半径（世界格）
     */
    public static void play(UUID casterUuid, double x, double y, double z, int totalTicks, float peakRadiusBlocks)
    {
        DomainScanWaveRenderer.casterUuid = casterUuid;
        centerX = x;
        centerY = y;
        centerZ = z;
        peakRadius = Math.max(0.0F, peakRadiusBlocks);
        enabled = true;
        startMillis = System.currentTimeMillis();
        totalMillis = Math.max(totalTicks, 1) * 50L;
    }

    /**
     * 资源重载时调用：{@code PostChain} 里编译好的 program 已随旧资源失效，必须整条重建。
     * 由 {@link DomainScanWaveReloadListener} 在 {@code RegisterShadersEvent} 时触发。
     */
    public static void invalidate()
    {
        chainOutdated = true;
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event)
    {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL)
        {
            return;
        }

        if (enabled && System.currentTimeMillis() - startMillis >= totalMillis)
        {
            enabled = false;
        }
        if (!enabled)
        {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null)
        {
            return;
        }

        float progress = (float) (System.currentTimeMillis() - startMillis) / (float) totalMillis;
        float radius = resolveRadius(progress);
        float intensity = resolveIntensity(progress);
        if (radius <= 0.0F || intensity <= MIN_INTENSITY)
        {
            return;
        }

        RenderTarget mainTarget = minecraft.getMainRenderTarget();
        ensureChain(minecraft, mainTarget);
        if (chain == null || wavePass == null)
        {
            return;
        }

        Vec3 center = resolveCenter(minecraft, event.getPartialTick());
        applyUniforms(minecraft, center, radius, intensity);
        render(mainTarget, event.getPartialTick());
    }

    /**
     * 取本帧的球心：施法者还在客户端实体列表里就用它<strong>当前</strong>的脚底位置（带插值），
     * 否则退回服务端发来的快照坐标。
     * <p>
     * 用 {@link net.minecraft.world.entity.Entity#getPosition(float)} 而不是 {@code getX/Y/Z}：
     * 前者按 {@code partialTick} 在上一 tick 与当前 tick 之间插值，人在走动时波心才不会一顿一顿。
     */
    private static Vec3 resolveCenter(Minecraft minecraft, float partialTick)
    {
        UUID uuid = casterUuid;
        ClientLevel level = minecraft.level;
        if (uuid != null && level != null)
        {
            Vec3 live = findCasterFoot(level, uuid, partialTick);
            if (live != null)
            {
                return live;
            }
        }
        return new Vec3(centerX, centerY, centerZ);
    }

    /** 在客户端已知的玩家列表里按 UUID 找施法者；找不到返回 {@code null}。 */
    private static Vec3 findCasterFoot(ClientLevel level, UUID uuid, float partialTick)
    {
        for (AbstractClientPlayer player : level.players())
        {
            if (uuid.equals(player.getUUID()))
            {
                return player.getPosition(partialTick);
            }
        }
        return null;
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

    private static void applyUniforms(Minecraft minecraft, Vec3 center, float radius, float intensity)
    {
        /*
         * 必须在改写投影 / 模型视图之前抓取：本效果靠「clip -> 相机相对世界」的逆矩阵把像素还原成
         * 世界坐标。两个矩阵都走 RenderSystem（此刻仍是世界渲染的透视投影 + 相机模型视图），
         * 同源避免口径错位。世界渲染阶段的 ModelViewMat 只含相机朝向、不含平移，
         * 故 shader 里还原出的坐标还要再加回相机世界坐标。
         */
        Matrix4f inverseTransform = new Matrix4f(RenderSystem.getProjectionMatrix())
                .mul(RenderSystem.getModelViewMatrix())
                .invert();
        Vec3 cameraPos = minecraft.gameRenderer.getMainCamera().getPosition();

        EffectInstance effect = wavePass.getEffect();
        setUniformMatrix(effect, "InverseTransformMatrix", inverseTransform);
        setUniformVec3(effect, "CameraPosition", cameraPos.x, cameraPos.y, cameraPos.z);
        setUniformVec3(effect, "Center", center.x, center.y, center.z);
        setUniformFloat(effect, "Radius", radius);
        setUniformFloat(effect, "Intensity", intensity);
    }

    private static void render(RenderTarget mainTarget, float partialTick)
    {
        /*
         * 主帧缓冲的深度确实是一张独立纹理（RenderTarget 用 glFramebufferTexture2D 挂的
         * GL_DEPTH_ATTACHMENT），但要当 sampler 用就得先拷进临时目标 —— 原版自己也是这么做的
         * （itemEntityTarget.copyDepthFrom / translucentTarget.copyDepthFrom），这样不必碰 Mixin。
         */
        if (depthTarget != null)
        {
            depthTarget.copyDepthFrom(mainTarget);
            GlStateManager._glBindFramebuffer(GL_DRAW_FRAMEBUFFER, mainTarget.frameBufferId);
        }

        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.resetTextureMatrix();

        /* 全屏 quad 只用 ProjMat / OutSize（PostPass 自己下发），模型视图归零更保险。 */
        RenderSystem.getModelViewStack().pushPose();
        RenderSystem.getModelViewStack().setIdentity();
        RenderSystem.applyModelViewMatrix();

        chain.process(partialTick);

        RenderSystem.getModelViewStack().popPose();
        RenderSystem.applyModelViewMatrix();

        mainTarget.bindWrite(true);
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
    }

    private static void ensureChain(Minecraft minecraft, RenderTarget mainTarget)
    {
        int width = minecraft.getWindow().getWidth();
        int height = minecraft.getWindow().getHeight();

        if (chain != null && (chainOutdated || width != chainWidth || height != chainHeight))
        {
            closeChain();
        }
        if (chain != null || !chainOutdated)
        {
            return;
        }

        chainOutdated = false;
        chainWidth = width;
        chainHeight = height;
        try
        {
            PostChain built = new PostChain(minecraft.getTextureManager(), minecraft.getResourceManager(),
                    mainTarget, CHAIN_LOCATION);
            RenderTarget swap = built.getTempTarget(SWAP_TARGET);
            RenderTarget depth = built.getTempTarget(DEPTH_TARGET);
            if (swap == null || depth == null)
            {
                built.close();
                LOGGER.error("扫描波后处理链缺少 {} / {} 目标，本次不渲染", SWAP_TARGET, DEPTH_TARGET);
                return;
            }

            PostPass pass = built.addPass(WAVE_PROGRAM, mainTarget, swap);
            pass.addAuxAsset(DEPTH_SAMPLER, depth::getDepthTextureId, depth.width, depth.height);
            built.addPass(BLIT_PROGRAM, swap, mainTarget);
            /* 正交投影矩阵是在 resize() 里下发给各 pass 的，所以必须放在 addPass 之后调用，
               否则新挂的 pass 拿不到 ProjMat。 */
            built.resize(width, height);

            chain = built;
            wavePass = pass;
            depthTarget = depth;
        }
        catch (Exception exception)
        {
            LOGGER.error("扫描波后处理链加载失败，本次不渲染", exception);
            closeChain();
            chainOutdated = false;
        }
    }

    private static void closeChain()
    {
        if (chain != null)
        {
            chain.close();
        }
        chain = null;
        wavePass = null;
        depthTarget = null;
        chainOutdated = true;
    }

    private static void setUniformFloat(EffectInstance effect, String name, float value)
    {
        Uniform uniform = effect.getUniform(name);
        if (uniform != null)
        {
            uniform.set(value);
        }
    }

    private static void setUniformVec3(EffectInstance effect, String name, double x, double y, double z)
    {
        Uniform uniform = effect.getUniform(name);
        if (uniform != null)
        {
            uniform.set((float) x, (float) y, (float) z);
        }
    }

    private static void setUniformMatrix(EffectInstance effect, String name, Matrix4f value)
    {
        Uniform uniform = effect.getUniform(name);
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
