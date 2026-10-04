package com.darkness.jujutsukaisenzone.client;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 任务开场全屏黑 + 居中白字旁白（切断视野：放屋、传送）。
 * <p>
 * 这不是对话。对话用 {@link QuestCinematicLetterboxOverlay}（电影黑幕，巡镜之后）。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class QuestBlackoutOverlay
{
    private static final int COLOR_BLACK = 0xFF000000;
    private static final int COLOR_WHITE = 0xFFFFFFFF;
    private static final float DRAW_Z = 2000.0F;

    private static boolean active;
    private static String captionKey = "";

    private QuestBlackoutOverlay()
    {
    }

    public static void show(String key)
    {
        active = true;
        captionKey = key == null ? "" : key;
    }

    public static void hide()
    {
        active = false;
        captionKey = "";
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
        graphics.fill(0, 0, width, height, COLOR_BLACK);

        if (!captionKey.isBlank())
        {
            Font font = Minecraft.getInstance().font;
            Component text = Component.translatable(captionKey);
            int wrapWidth = Math.max(16, width - 48);
            List<FormattedCharSequence> lines = font.split(text, wrapWidth);
            int totalH = lines.size() * font.lineHeight;
            int y = (height - totalH) / 2;
            for (FormattedCharSequence line : lines)
            {
                int lineW = font.width(line);
                graphics.drawString(font, line, (width - lineW) / 2, y, COLOR_WHITE, false);
                y += font.lineHeight;
            }
        }

        graphics.pose().popPose();
        graphics.flush();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }
}
