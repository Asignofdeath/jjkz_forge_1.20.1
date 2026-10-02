package com.darkness.jujutsukaisenzone.client;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
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
 * 若希望黑条不盖 HUD，把订阅事件换成 {@code RenderGuiOverlayEvent.Pre} 即可（届时黑条落在 HUD 之下）。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class CinematicLetterboxOverlay
{
    /** 黑条 RGB（纯黑）。 */
    private static final int BAR_COLOR = 0xFF000000;

    private static boolean active;
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
     */
    public static void play(int totalTicks, int fadeInTicks, int fadeOutTicks, float barFraction)
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

    @SubscribeEvent(priority = EventPriority.LOW)
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
        if (barHeight <= 0)
        {
            return;
        }
        event.getGuiGraphics().fill(0, 0, width, barHeight, BAR_COLOR);
        event.getGuiGraphics().fill(0, height - barHeight, width, height, BAR_COLOR);
    }

    /** 返回 0 ~ 1 的当前强度；窗口结束后自动失效。 */
    private static float resolveFade()
    {
        long elapsed = System.currentTimeMillis() - startMillis;
        if (elapsed >= totalMillis)
        {
            active = false;
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
