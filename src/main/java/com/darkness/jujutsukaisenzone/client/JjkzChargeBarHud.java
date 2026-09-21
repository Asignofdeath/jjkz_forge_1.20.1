package com.darkness.jujutsukaisenzone.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/**
 * 模组自绘蓄力进度条（不依赖 Epic Fight 贴图或 overlay）。
 */
public final class JjkzChargeBarHud
{
    public static final int BAR_WIDTH = 140;
    public static final int BAR_HEIGHT = 8;
    public static final int LABEL_COLOR = 0xFFFFFF;
    public static final int BAR_BG_COLOR = 0xAA000000;
    public static final int BAR_BORDER_COLOR = 0xFFCCCCCC;

    private JjkzChargeBarHud()
    {
    }

    public static void drawCentered(
            GuiGraphics graphics,
            Minecraft minecraft,
            int screenWidth,
            int screenHeight,
            String label,
            float progress,
            int fillColor)
    {
        drawCentered(graphics, minecraft, screenWidth, screenHeight, label, progress, fillColor, 0);
    }

    public static void drawCentered(
            GuiGraphics graphics,
            Minecraft minecraft,
            int screenWidth,
            int screenHeight,
            String label,
            float progress,
            int fillColor,
            int offsetY)
    {
        float clamped = Mth.clamp(progress, 0.0F, 1.0F);
        int centerX = screenWidth / 2;
        int barX = centerX - BAR_WIDTH / 2;
        int barY = screenHeight / 2 + 12 + offsetY;

        if (label != null && !label.isBlank())
        {
            int labelWidth = minecraft.font.width(label);
            graphics.drawString(minecraft.font, label, centerX - labelWidth / 2, barY - 12, LABEL_COLOR, true);
        }

        graphics.fill(barX - 1, barY - 1, barX + BAR_WIDTH + 1, barY + BAR_HEIGHT + 1, BAR_BORDER_COLOR);
        graphics.fill(barX, barY, barX + BAR_WIDTH, barY + BAR_HEIGHT, BAR_BG_COLOR);
        int filledWidth = Math.round(BAR_WIDTH * clamped);
        if (filledWidth > 0)
        {
            graphics.fill(barX, barY, barX + filledWidth, barY + BAR_HEIGHT, fillColor);
        }

        String percentText = Math.round(clamped * 100.0F) + "%";
        int percentWidth = minecraft.font.width(percentText);
        graphics.drawString(minecraft.font, percentText, centerX - percentWidth / 2, barY + BAR_HEIGHT + 2, LABEL_COLOR, true);
    }
}
