package com.darkness.jujutsukaisenzone.client;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 主线 1.3 演出开场：立刻整屏红 → 在 {@link #FADE_TO_BLACK_MILLIS} 内渐变到整屏黑。
 * <p>
 * 与 {@link QuestBlackoutOverlay} 的区别：后者是纯黑开场旁白；本层是红→黑的「受创」遮罩，
 * 红屏必须硬切（0 帧就满屏红），随后线性/平滑过渡到黑并保持全黑，直到服务端显式清除。
 * <p>
 * 屏幕空间全屏矩形，画在所有 HUD 之上；绘制路径对齐 {@link QuestBlackoutOverlay}。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class QuestRedBlackoutOverlay
{
    private static final int RED_R = 160;
    private static final int RED_G = 0;
    private static final int RED_B = 0;
    /** 红屏硬切后渐变到黑的时间。 */
    private static final long FADE_TO_BLACK_MILLIS = 1000L;
    private static final float DRAW_Z = 2100.0F;

    private static boolean active;
    private static long startMillis;

    private QuestRedBlackoutOverlay()
    {
    }

    /** 立刻红屏，并在约 1 秒内渐变为全黑。 */
    public static void show()
    {
        active = true;
        startMillis = System.currentTimeMillis();
    }

    public static void hide()
    {
        active = false;
    }

    public static boolean isActive()
    {
        return active;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END || !active)
        {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null)
        {
            minecraft.setScreen(null);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderGui(RenderGuiEvent.Post event)
    {
        if (!active)
        {
            return;
        }
        int width = event.getWindow().getGuiScaledWidth();
        int height = event.getWindow().getGuiScaledHeight();
        GuiGraphics graphics = event.getGuiGraphics();
        graphics.flush();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, DRAW_Z);
        graphics.fill(0, 0, width, height, currentColor());
        graphics.pose().popPose();
        graphics.flush();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    /** 红→黑：t=0 纯红，t=1 纯黑（此后保持黑）。 */
    private static int currentColor()
    {
        float t = fadeProgress();
        int r = Math.round(Mth.lerp(t, RED_R, 0.0F));
        int g = Math.round(Mth.lerp(t, RED_G, 0.0F));
        int b = Math.round(Mth.lerp(t, RED_B, 0.0F));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static float fadeProgress()
    {
        long elapsed = System.currentTimeMillis() - startMillis;
        if (elapsed >= FADE_TO_BLACK_MILLIS)
        {
            return 1.0F;
        }
        float linear = (float) elapsed / (float) FADE_TO_BLACK_MILLIS;
        return smoothStep(linear);
    }

    private static float smoothStep(float t)
    {
        float x = Mth.clamp(t, 0.0F, 1.0F);
        return x * x * (3.0F - 2.0F * x);
    }
}
