package com.darkness.jujutsukaisenzone.client;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 「指控」：被告辩护选择 HUD（仅被告客户端显示）。
 */
public final class JudgmentAccuseChoiceHud
{
    private static boolean visible;

    private JudgmentAccuseChoiceHud()
    {
    }

    public static void show()
    {
        visible = true;
    }

    public static void hide()
    {
        visible = false;
    }

    @Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class Renderer
    {
        private static final int LABEL_COLOR = 0xFFFFFF;
        private static final int ACCENT_COLOR = 0xFFCC4444;
        private static final int OPTION_COLOR = 0xFFAAAAAA;

        private Renderer()
        {
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void onRenderGui(RenderGuiEvent.Post event)
        {
            if (!visible || !JudgmentAccuseChoiceClientState.isActive())
            {
                return;
            }

            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null || minecraft.options.hideGui || minecraft.screen != null)
            {
                return;
            }

            GuiGraphics graphics = event.getGuiGraphics();
            int screenWidth = event.getWindow().getGuiScaledWidth();
            int screenHeight = event.getWindow().getGuiScaledHeight();
            int centerX = screenWidth / 2;
            int y = screenHeight / 2 - 40;

            String title = "审判者指控";
            int titleWidth = minecraft.font.width(title);
            graphics.drawString(minecraft.font, title, centerX - titleWidth / 2, y, ACCENT_COLOR, true);
            y += 12;

            String crimeLine = JudgmentAccuseChoiceClientState.crimeChinese()
                    + " / "
                    + JudgmentAccuseChoiceClientState.crimeEnglish();
            int crimeWidth = minecraft.font.width(crimeLine);
            graphics.drawString(minecraft.font, crimeLine, centerX - crimeWidth / 2, y, LABEL_COLOR, true);
            y += 16;

            float seconds = JudgmentAccuseChoiceClientState.remainingTicks() / 20.0F;
            String timer = String.format("剩余 %.1f 秒", seconds);
            int timerWidth = minecraft.font.width(timer);
            graphics.drawString(minecraft.font, timer, centerX - timerWidth / 2, y, LABEL_COLOR, true);
            y += 14;

            drawOption(graphics, minecraft, centerX, y, "1 - 否认 / Deny");
            y += 10;
            drawOption(graphics, minecraft, centerX, y, "2 - 撒谎 / Lie");
            y += 10;
            drawOption(graphics, minecraft, centerX, y, "3 - 坦白 / Confess");
        }

        private static void drawOption(GuiGraphics graphics, Minecraft minecraft, int centerX, int y, String text)
        {
            int width = minecraft.font.width(text);
            graphics.drawString(minecraft.font, text, centerX - width / 2, y, OPTION_COLOR, true);
        }
    }
}
