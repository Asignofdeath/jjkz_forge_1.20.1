package com.darkness.jujutsukaisenzone.client;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yesman.epicfight.client.gui.EntityUI;
import yesman.epicfight.config.ClientConfig;
import yesman.epicfight.api.utils.math.Vec2i;

/**
 * 术式槽位「最大输出」蓄力条，样式与 Epic Fight 蓄力条一致。
 */
public final class MaxOutputChargeHud
{
    private static boolean visible;
    private static int slot;
    private static int holdOriginTick;
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

    private static int currentPlayerTick()
    {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null ? minecraft.player.tickCount : 0;
    }

    private static float getProgress(float partialTick)
    {
        if (!visible || requiredTicks <= 0)
        {
            return 0.0F;
        }
        float heldTicks = currentPlayerTick() - holdOriginTick + partialTick;
        return Mth.clamp(heldTicks / (float) requiredTicks, 0.0F, 1.0F);
    }

    @Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class Renderer
    {
        private Renderer()
        {
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void onRenderGui(RenderGuiEvent.Post event)
        {
            if (!visible)
            {
                return;
            }

            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null || minecraft.options.hideGui || minecraft.screen != null)
            {
                return;
            }
            if (!(minecraft.gui instanceof ForgeGui forgeGui))
            {
                return;
            }

            float partialTick = event.getPartialTick();
            float ratio = getProgress(partialTick);
            int screenWidth = event.getWindow().getGuiScaledWidth();
            int screenHeight = event.getWindow().getGuiScaledHeight();
            Vec2i pos = ClientConfig.getChargingBarPosition(screenWidth, screenHeight);
            GuiGraphics graphics = event.getGuiGraphics();

            forgeGui.setupOverlayRenderState(true, false);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            graphics.blit(EntityUI.BATTLE_ICON, pos.x, pos.y, 1, 71, 238, 13, 255, 255);
            graphics.blit(EntityUI.BATTLE_ICON, pos.x, pos.y, 1, 57, (int) (238 * ratio), 13, 255, 255);

            int stringWidth = minecraft.font.width(label);
            graphics.drawString(minecraft.font, label, pos.x + 120 - stringWidth / 2, pos.y - 12, 16777215, true);
        }
    }
}
