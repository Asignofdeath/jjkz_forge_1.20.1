package com.darkness.jujutsukaisenzone.client;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 电影上下黑条（letterbox）：视野上下各压一条黑带，营造过场演出感。
 * <p>
 * 绘制在 {@link RenderGuiEvent.Post}（GUI 最上层），因此会盖住血条 / 技能栏等 HUD —— 这是刻意的：
 * 电影感优先于 HUD 可见性。但<strong>不锁操作</strong>，玩家仍可自由移动与输入。
 * <p>
 * 绘制时关闭深度测试并抬高 Z：物品栏图标等会写入更高深度，默认 {@code fill} 会被挡住，
 * 导致黑条盖不住热键栏物品图标。
 * <p>
 * 下方黑条居中绘制无量空处日语原文「領域展開　無量空処」，随黑条同强度淡入淡出。
 * 施术者客户端额外在屏幕中央绘制领域展开手势图，进退场与黑条同一套 fade。
 * <p>
 * 若希望黑条不盖 HUD，把订阅事件换成 {@code RenderGuiOverlayEvent.Pre} 即可（届时黑条落在 HUD 之下）。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class CinematicLetterboxOverlay
{
    /** 黑条 RGB（纯黑）。 */
    private static final int BAR_COLOR = 0xFF000000;
    /** 抬到 HUD 物品图标之上（与 {@code SkillUpgradeResultPopup} 同口径）。 */
    private static final float DRAW_Z = 1000.0F;
    /** 下方黑条字幕：领域展开「无量空处」日语原文汉字。 */
    private static final String CAPTION = "領域展開　無量空処";
    /** 字幕 RGB（纯白）；透明度随黑条 fade。 */
    private static final int CAPTION_RGB = 0xFFFFFF;
    /** 开放式无量空处：施术者屏幕中央手势图。 */
    private static final ResourceLocation GESTURE_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            JuJutsuKaisenZone.MODID, "textures/gui/domain/ryouiki_tenkai_muryoukuusho.png");
    private static final int GESTURE_TEX_W = 1311;
    private static final int GESTURE_TEX_H = 3355;
    /** 手势图相对上下黑条之间可用高度的占比。 */
    private static final float GESTURE_HEIGHT_FIT = 0.92F;
    /** 手势图相对屏宽上限，避免过宽。 */
    private static final float GESTURE_WIDTH_CAP = 0.42F;

    private static boolean active;
    private static boolean showGesture;
    private static long startMillis;
    private static long totalMillis;
    private static long fadeInMillis;
    private static long fadeOutMillis;
    private static float targetBarFraction;

    private CinematicLetterboxOverlay()
    {
    }

    /**
     * 播放一次黑条（淡入 — 保持 — 淡出）。参数为 tick，与其它全屏滤镜链路无关。
     *
     * @param barFraction 单侧黑条占屏幕高度的比例（夹取到 0 ~ 0.5）
     * @param showGestureImage 是否在屏幕中央绘制手势图（仅施术者）
     */
    public static void play(int totalTicks, int fadeInTicks, int fadeOutTicks, float barFraction, boolean showGestureImage)
    {
        int safeTotalTicks = Math.max(totalTicks, 1);
        int safeFadeInTicks = Math.max(fadeInTicks, 0);
        int safeFadeOutTicks = Math.max(fadeOutTicks, 0);
        if (safeFadeInTicks + safeFadeOutTicks > safeTotalTicks)
        {
            int overflow = safeFadeInTicks + safeFadeOutTicks - safeTotalTicks;
            safeFadeOutTicks = Math.max(0, safeFadeOutTicks - overflow);
        }

        active = true;
        showGesture = showGestureImage;
        startMillis = System.currentTimeMillis();
        totalMillis = safeTotalTicks * 50L;
        fadeInMillis = safeFadeInTicks * 50L;
        fadeOutMillis = safeFadeOutTicks * 50L;
        targetBarFraction = Mth.clamp(barFraction, 0.0F, 0.5F);
    }

    public static boolean isActive()
    {
        return active;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderGui(RenderGuiEvent.Post event)
    {
        if (!active)
        {
            return;
        }
        float fade = resolveFade();
        if (fade <= 0.0F)
        {
            return;
        }

        int width = event.getWindow().getGuiScaledWidth();
        int height = event.getWindow().getGuiScaledHeight();
        int barHeight = Math.round(height * targetBarFraction * fade);
        if (barHeight <= 0 && !showGesture)
        {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        // 先冲掉已排队的 GUI 批次，再改深度状态，避免 fill 延后提交时状态已被还原。
        graphics.flush();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, DRAW_Z);
        if (barHeight > 0)
        {
            graphics.fill(0, 0, width, barHeight, BAR_COLOR);
            graphics.fill(0, height - barHeight, width, height, BAR_COLOR);
            drawBottomCaption(graphics, width, height, barHeight, fade);
        }
        if (showGesture)
        {
            drawCenterGesture(graphics, width, height, fade);
        }
        graphics.pose().popPose();
        graphics.flush();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    /** 下方黑条内垂直居中绘制字幕；黑条过矮时不画，避免裁切。 */
    private static void drawBottomCaption(GuiGraphics graphics, int width, int height, int barHeight, float fade)
    {
        Font font = Minecraft.getInstance().font;
        if (barHeight < font.lineHeight + 2)
        {
            return;
        }
        int alpha = Math.round(255.0F * fade);
        if (alpha <= 0)
        {
            return;
        }
        int color = (alpha << 24) | CAPTION_RGB;
        int textY = height - (barHeight + font.lineHeight) / 2;
        graphics.drawCenteredString(font, CAPTION, width / 2, textY, color);
    }

    /**
     * 屏幕中央（上下满幅黑条之间）绘制手势图；尺寸按满幅黑条布局，仅透明度跟 fade，避免淡入时缩放跳动。
     */
    private static void drawCenterGesture(GuiGraphics graphics, int width, int height, float fade)
    {
        if (fade <= 0.01F)
        {
            return;
        }
        int fullBarHeight = Math.round(height * targetBarFraction);
        int middleTop = fullBarHeight;
        int middleHeight = Math.max(1, height - fullBarHeight * 2);
        float maxH = middleHeight * GESTURE_HEIGHT_FIT;
        float maxW = width * GESTURE_WIDTH_CAP;
        float scale = Math.min(maxH / (float) GESTURE_TEX_H, maxW / (float) GESTURE_TEX_W);
        int drawW = Math.max(1, Math.round(GESTURE_TEX_W * scale));
        int drawH = Math.max(1, Math.round(GESTURE_TEX_H * scale));
        int x = (width - drawW) / 2;
        int y = middleTop + (middleHeight - drawH) / 2;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, fade);
        graphics.blit(
                GESTURE_TEXTURE,
                x,
                y,
                drawW,
                drawH,
                0.0F,
                0.0F,
                GESTURE_TEX_W,
                GESTURE_TEX_H,
                GESTURE_TEX_W,
                GESTURE_TEX_H);
        // blit 延后提交：必须在还原 shader color 前 flush，否则淡入淡出不生效。
        graphics.flush();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    /** 返回 0 ~ 1 的当前强度；窗口结束后自动失效。 */
    private static float resolveFade()
    {
        long elapsed = System.currentTimeMillis() - startMillis;
        if (elapsed >= totalMillis)
        {
            active = false;
            showGesture = false;
            return 0.0F;
        }

        long holdMillis = Math.max(0L, totalMillis - fadeInMillis - fadeOutMillis);
        if (elapsed < fadeInMillis && fadeInMillis > 0L)
        {
            return smoothStep((float) elapsed / (float) fadeInMillis);
        }
        if (elapsed < fadeInMillis + holdMillis)
        {
            return 1.0F;
        }
        if (fadeOutMillis > 0L)
        {
            long fadeOutElapsed = elapsed - fadeInMillis - holdMillis;
            return 1.0F - smoothStep((float) fadeOutElapsed / (float) fadeOutMillis);
        }
        return 0.0F;
    }

    private static float smoothStep(float t)
    {
        float x = Mth.clamp(t, 0.0F, 1.0F);
        return x * x * (3.0F - 2.0F * x);
    }
}
