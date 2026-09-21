package com.darkness.jujutsukaisenzone.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/**
 * 「最大输出」蓄力条状态；绘制由 {@link JjkzChargeBarOverlay} 统一调度。
 */
public final class MaxOutputChargeHud
{
    private static final int FILL_COLOR = 0xFF44AAFF;

    private static boolean visible;
    private static int slot;
    private static int holdOriginTick = -1;
    private static int requiredTicks;
    private static String label = "最大输出";

    private MaxOutputChargeHud()
    {
    }

    public static void begin(int chargeSlot, int chargeRequiredTicks, String displayLabel)
    {
        begin(chargeSlot, chargeRequiredTicks, displayLabel, currentPlayerTick());
    }

    public static void begin(int chargeSlot, int chargeRequiredTicks, String displayLabel, int chargeBeginTick)
    {
        visible = true;
        slot = chargeSlot;
        requiredTicks = Math.max(1, chargeRequiredTicks);
        label = displayLabel == null || displayLabel.isBlank() ? "最大输出" : displayLabel;
        holdOriginTick = chargeBeginTick;
    }

    public static void updateHoldOrigin(int originTick)
    {
        if (visible)
        {
            holdOriginTick = originTick;
        }
    }

    public static void end()
    {
        visible = false;
        slot = 0;
        holdOriginTick = -1;
        requiredTicks = 0;
    }

    public static boolean isVisible()
    {
        return visible;
    }

    public static int getChargingSlot()
    {
        return visible ? slot : 0;
    }

    static void render(GuiGraphics graphics, Minecraft minecraft, int screenWidth, int screenHeight, float partialTick)
    {
        if (!visible)
        {
            return;
        }

        float progress = resolveProgress(partialTick);
        JjkzChargeBarHud.drawCentered(graphics, minecraft, screenWidth, screenHeight, label, progress, FILL_COLOR);
    }

    private static int currentPlayerTick()
    {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null ? minecraft.player.tickCount : 0;
    }

    private static float resolveProgress(float partialTick)
    {
        if (!visible || requiredTicks <= 0 || holdOriginTick < 0)
        {
            return 0.0F;
        }
        float heldTicks = currentPlayerTick() - holdOriginTick + partialTick;
        return Mth.clamp(heldTicks / (float) requiredTicks, 0.0F, 1.0F);
    }
}
