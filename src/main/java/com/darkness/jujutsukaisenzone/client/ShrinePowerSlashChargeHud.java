package com.darkness.jujutsukaisenzone.client;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 伏魔神龛强力斩击：锁定蓄力文本进度条（仅施法者客户端显示）。
 * <p>
 * 由 {@link ShrineDomainLockOnClientHandler} 根据 Epic Fight 锁定状态在客户端驱动进度；
 * 服务端 {@code SyncShrinePowerSlashChargeS2CPacket} 仅用于强制隐藏（斩击开始、冷却等）。
 */
public final class ShrinePowerSlashChargeHud
{
    /** 与 {@link com.darkness.jujutsukaisenzone.technique.domain.ShrineDomainExpansion} 内 {@code POWER_SLASH_LOCK_CHARGE_TICKS} 一致。 */
    public static final int LOCK_CHARGE_TICKS = 60;

    private static boolean visible;
    private static int lockChargeStartTick = -1;
    private static float serverProgress = -1.0F;

    private ShrinePowerSlashChargeHud()
    {
    }

    /** 开始锁定蓄力条（客户端 tick 计时）。 */
    public static void beginLockCharge()
    {
        visible = true;
        serverProgress = -1.0F;
        lockChargeStartTick = currentPlayerTick();
    }

    /** 服务端同步进度（包到达时作为权威值覆盖客户端推算）。 */
    public static void setChargeProgress(float chargeProgress)
    {
        visible = true;
        serverProgress = Mth.clamp(chargeProgress, 0.0F, 1.0F);
        if (lockChargeStartTick < 0)
        {
            lockChargeStartTick = currentPlayerTick();
        }
    }

    public static void hide()
    {
        visible = false;
        lockChargeStartTick = -1;
        serverProgress = -1.0F;
    }

    private static int currentPlayerTick()
    {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null ? minecraft.player.tickCount : 0;
    }

    private static float resolveProgress(float partialTick)
    {
        if (!visible)
        {
            return 0.0F;
        }
        if (serverProgress >= 0.0F)
        {
            return serverProgress;
        }
        if (lockChargeStartTick < 0)
        {
            return 0.0F;
        }
        float heldTicks = currentPlayerTick() - lockChargeStartTick + partialTick;
        return Mth.clamp(heldTicks / (float) LOCK_CHARGE_TICKS, 0.0F, 1.0F);
    }

    @Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class Renderer
    {
        private static final int BAR_WIDTH = 140;
        private static final int BAR_HEIGHT = 8;
        private static final int LABEL_COLOR = 0xFFFFFF;
        private static final int BAR_BG_COLOR = 0x66000000;
        private static final int BAR_FILL_COLOR = 0xFFCC3333;

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

            float progress = resolveProgress(event.getPartialTick());
            GuiGraphics graphics = event.getGuiGraphics();
            int screenWidth = event.getWindow().getGuiScaledWidth();
            int screenHeight = event.getWindow().getGuiScaledHeight();
            int centerX = screenWidth / 2;
            int barX = centerX - BAR_WIDTH / 2;
            int barY = screenHeight / 2 + 12;

            String label = "强力斩击";
            int labelWidth = minecraft.font.width(label);
            graphics.drawString(minecraft.font, label, centerX - labelWidth / 2, barY - 12, LABEL_COLOR, true);

            graphics.fill(barX, barY, barX + BAR_WIDTH, barY + BAR_HEIGHT, BAR_BG_COLOR);
            int filledWidth = Math.round(BAR_WIDTH * progress);
            if (filledWidth > 0)
            {
                graphics.fill(barX, barY, barX + filledWidth, barY + BAR_HEIGHT, BAR_FILL_COLOR);
            }

            String percentText = Math.round(progress * 100.0F) + "%";
            int percentWidth = minecraft.font.width(percentText);
            graphics.drawString(minecraft.font, percentText, centerX - percentWidth / 2, barY + BAR_HEIGHT + 2, LABEL_COLOR, true);
        }
    }
}
