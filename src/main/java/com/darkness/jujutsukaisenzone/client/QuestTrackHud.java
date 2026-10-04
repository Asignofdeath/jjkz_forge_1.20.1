package com.darkness.jujutsukaisenzone.client;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 主线任务追踪 HUD（左上）。坐标差仍由 {@link LocateGuidanceHud} 负责。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class QuestTrackHud
{
    private static boolean active;
    private static String questId = "";
    private static String title = "";
    private static String objective = "";

    private QuestTrackHud()
    {
    }

    public static void setTracked(String id, String titleText, String objectiveText)
    {
        active = true;
        questId = id == null ? "" : id;
        title = titleText == null ? "" : titleText;
        objective = objectiveText == null ? "" : objectiveText;
    }

    public static void clear()
    {
        active = false;
        questId = "";
        title = "";
        objective = "";
    }

    public static boolean isActive()
    {
        return active;
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiOverlayEvent.Post event)
    {
        if (!active || !event.getOverlay().equals(VanillaGuiOverlay.HOTBAR.type()))
        {
            return;
        }
        if (QuestBlackoutOverlay.isActive() || QuestCinematicLetterboxOverlay.isActive())
        {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui)
        {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        int x = 8;
        int y = 48;
        String prefix = Component.translatable("gui.jujutsukaisenzone.quest.hud_prefix").getString();
        graphics.drawString(minecraft.font, prefix + title, x, y, 0xFFE8C56B, true);
        graphics.drawString(minecraft.font, objective, x, y + 12, 0xFFFFFFFF, true);
        if (!questId.isEmpty())
        {
            graphics.drawString(minecraft.font, questId, x, y + 24, 0xFF808080, true);
        }
    }
}
