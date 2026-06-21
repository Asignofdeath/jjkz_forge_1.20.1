package com.darkness.jujutsukaisenzone.client;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.technique.EmotionalReleaseServerLogic;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yesman.epicfight.api.utils.math.Vec2i;
import yesman.epicfight.client.gui.EntityUI;
import yesman.epicfight.config.ClientConfig;

/**
 * 「释放情绪」蓄力条，样式与 Epic Fight 蓄力条一致。
 */
public final class EmotionalReleaseChargeHud
{
    private static boolean visible;
    private static long holdOriginGameTime = -1L;
    private static String label = "释放情绪";

    private EmotionalReleaseChargeHud()
    {
    }

    public static void begin(long chargeBeginGameTime)
    {
        visible = true;
        holdOriginGameTime = chargeBeginGameTime;
    }

    public static void updateHoldOrigin(long originGameTime)
    {
        if (visible)
        {
            holdOriginGameTime = originGameTime;
        }
    }

    public static void end()
    {
        visible = false;
        holdOriginGameTime = -1L;
    }

    public static boolean isVisible()
    {
        return visible;
    }

    private static float getProgress(float partialTick)
    {
        if (!visible || holdOriginGameTime < 0L)
        {
            return 0.0F;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null)
        {
            return 0.0F;
        }
        long gameTime = minecraft.player.level().getGameTime();
        float heldTicks = gameTime - holdOriginGameTime + partialTick;
        return Mth.clamp(heldTicks / (float) EmotionalReleaseServerLogic.MAX_CHARGE_TICKS, 0.0F, 1.0F);
    }

    @Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class Renderer
    {
        private Renderer()
        {
        }

        @SubscribeEvent
        public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event)
        {
            if (!visible)
            {
                return;
            }

            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null || minecraft.options.hideGui)
            {
                return;
            }

            float partialTick = minecraft.getFrameTime();
            float ratio = getProgress(partialTick);
            int screenWidth = event.getWindow().getGuiScaledWidth();
            int screenHeight = event.getWindow().getGuiScaledHeight();
            Vec2i pos = ClientConfig.getChargingBarPosition(screenWidth, screenHeight);
            GuiGraphics graphics = event.getGuiGraphics();

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            graphics.blit(EntityUI.BATTLE_ICON, pos.x, pos.y, 1, 71, 238, 13, 255, 255);
            graphics.blit(EntityUI.BATTLE_ICON, pos.x, pos.y, 1, 57, (int) (238 * ratio), 13, 255, 255);

            int stringWidth = minecraft.font.width(label);
            graphics.drawString(minecraft.font, label, pos.x + 120 - stringWidth / 2, pos.y - 12, 16777215, true);
        }
    }
}
