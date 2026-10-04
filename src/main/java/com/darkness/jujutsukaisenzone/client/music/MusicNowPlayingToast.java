package com.darkness.jujutsukaisenzone.client.music;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.music.ModMusicTrack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 开播时右上角划入小框：曲名 + 来源，停留后收回。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class MusicNowPlayingToast
{
    private static final long SLIDE_IN_MS = 280L;
    private static final long HOLD_MS = 3200L;
    private static final long SLIDE_OUT_MS = 320L;
    private static final long TOTAL_MS = SLIDE_IN_MS + HOLD_MS + SLIDE_OUT_MS;

    private static final int PANEL_RGB = 0x12121A;
    private static final int BORDER_RGB = 0xC9A227;
    private static final int TITLE_RGB = 0xF5E6A8;
    private static final int SOURCE_RGB = 0xA8A8B8;
    private static final int MARGIN = 8;
    private static final int SLIDE_DISTANCE = 48;

    private static ActiveToast active;

    private MusicNowPlayingToast()
    {
    }

    public static void show(ModMusicTrack track)
    {
        if (track == null)
        {
            return;
        }
        active = new ActiveToast(track.title(), track.source(), System.currentTimeMillis());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onScreenRenderPost(ScreenEvent.Render.Post event)
    {
        render(event.getGuiGraphics(), event.getScreen().width, event.getScreen().height);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onRenderGui(RenderGuiEvent.Post event)
    {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null)
        {
            return;
        }
        render(
                event.getGuiGraphics(),
                event.getWindow().getGuiScaledWidth(),
                event.getWindow().getGuiScaledHeight());
    }

    private static void render(GuiGraphics graphics, int screenW, int screenH)
    {
        ActiveToast toast = active;
        if (toast == null)
        {
            return;
        }
        long elapsed = System.currentTimeMillis() - toast.startedAtMs;
        if (elapsed >= TOTAL_MS)
        {
            active = null;
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        int paddingX = 12;
        int paddingY = 8;
        int gap = 3;
        int contentW = Math.max(font.width(toast.title), font.width(toast.source));
        int panelW = Math.max(140, contentW + paddingX * 2);
        int panelH = paddingY * 2 + font.lineHeight + gap + font.lineHeight;

        float slide = resolveSlide(elapsed);
        float alpha = resolveAlpha(elapsed);
        if (alpha <= 0.01F)
        {
            return;
        }

        int right = screenW - MARGIN;
        int left = right - panelW;
        int top = MARGIN;
        int drawLeft = Math.round(left + slide);
        int drawRight = drawLeft + panelW;
        int bottom = top + panelH;

        graphics.pose().pushPose();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        graphics.pose().translate(0.0F, 0.0F, 1000.0F);

        int panelAlpha = Math.round(210 * alpha);
        int borderAlpha = Math.round(230 * alpha);
        graphics.fill(drawLeft - 1, top - 1, drawRight + 1, bottom + 1, colorWithAlpha(BORDER_RGB, borderAlpha));
        graphics.fill(drawLeft, top, drawRight, bottom, colorWithAlpha(PANEL_RGB, panelAlpha));
        graphics.fill(drawLeft, top, drawRight, top + 2, colorWithAlpha(BORDER_RGB, borderAlpha));

        int textX = drawLeft + paddingX;
        int y = top + paddingY;
        graphics.drawString(font, toast.title, textX, y, colorWithAlpha(TITLE_RGB, Math.round(255 * alpha)), false);
        y += font.lineHeight + gap;
        graphics.drawString(font, toast.source, textX, y, colorWithAlpha(SOURCE_RGB, Math.round(230 * alpha)), false);

        graphics.pose().popPose();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** 正值表示仍在屏外右侧，0 为到位。 */
    private static float resolveSlide(long elapsed)
    {
        if (elapsed < SLIDE_IN_MS)
        {
            float t = 1.0F - easeOutCubic(elapsed / (float) SLIDE_IN_MS);
            return SLIDE_DISTANCE * t;
        }
        if (elapsed < SLIDE_IN_MS + HOLD_MS)
        {
            return 0.0F;
        }
        float t = (elapsed - SLIDE_IN_MS - HOLD_MS) / (float) SLIDE_OUT_MS;
        return SLIDE_DISTANCE * easeInCubic(Mth.clamp(t, 0.0F, 1.0F));
    }

    private static float resolveAlpha(long elapsed)
    {
        if (elapsed < SLIDE_IN_MS)
        {
            return easeOutCubic(elapsed / (float) SLIDE_IN_MS);
        }
        if (elapsed < SLIDE_IN_MS + HOLD_MS)
        {
            return 1.0F;
        }
        float t = (elapsed - SLIDE_IN_MS - HOLD_MS) / (float) SLIDE_OUT_MS;
        return 1.0F - easeInCubic(Mth.clamp(t, 0.0F, 1.0F));
    }

    private static float easeOutCubic(float t)
    {
        float u = 1.0F - t;
        return 1.0F - u * u * u;
    }

    private static float easeInCubic(float t)
    {
        return t * t * t;
    }

    private static int colorWithAlpha(int rgb, int alpha)
    {
        return (Mth.clamp(alpha, 0, 255) << 24) | (rgb & 0xFFFFFF);
    }

    private record ActiveToast(Component title, Component source, long startedAtMs)
    {
    }
}
