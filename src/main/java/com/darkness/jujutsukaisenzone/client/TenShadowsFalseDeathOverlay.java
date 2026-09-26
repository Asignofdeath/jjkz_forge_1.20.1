package com.darkness.jujutsukaisenzone.client;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 假死观战：全屏暗色蒙层 + 简短提示，营造「已死、正在观战」感。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class TenShadowsFalseDeathOverlay
{
    /** 蒙层目标不透明度（约 55%），仍能看见战场。 */
    private static final int VEIL_ALPHA = 140;
    /** 边缘再压一层，略像死亡晕影。 */
    private static final int EDGE_ALPHA = 90;
    private static final int EDGE_SIZE = 48;
    private static final int VEIL_RGB = 0x0A0A12;
    private static final int FADE_MS = 450;

    private TenShadowsFalseDeathOverlay()
    {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onRenderGui(RenderGuiEvent.Post event)
    {
        if (!ClientTenShadowsFalseDeathState.isLocalCameraActive())
        {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null)
        {
            return;
        }

        float fade = ClientTenShadowsFalseDeathState.localVeilFade(FADE_MS);
        if (fade <= 0.0F)
        {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        int width = event.getWindow().getGuiScaledWidth();
        int height = event.getWindow().getGuiScaledHeight();

        int veilAlpha = Math.round(VEIL_ALPHA * fade);
        graphics.fill(0, 0, width, height, colorWithAlpha(VEIL_RGB, veilAlpha));
        drawEdgeVignette(graphics, width, height, Math.round(EDGE_ALPHA * fade));

        Font font = minecraft.font;
        Component title = Component.translatable("hud.jujutsukaisenzone.ten_shadows_false_death");
        Component hint = Component.translatable("hud.jujutsukaisenzone.ten_shadows_false_death.hint");
        int titleAlpha = Math.round(230 * fade);
        int hintAlpha = Math.round(180 * fade);
        int titleY = height / 2 - 18;
        int hintY = titleY + 14;
        int titleX = (width - font.width(title)) / 2;
        int hintX = (width - font.width(hint)) / 2;
        graphics.drawString(font, title, titleX, titleY, colorWithAlpha(0xE8E8F0, titleAlpha), true);
        graphics.drawString(font, hint, hintX, hintY, colorWithAlpha(0xA0A0B0, hintAlpha), false);
    }

    private static void drawEdgeVignette(GuiGraphics graphics, int width, int height, int maxAlpha)
    {
        if (maxAlpha <= 0)
        {
            return;
        }
        for (int i = 0; i < EDGE_SIZE; i++)
        {
            float fade = 1.0F - (float) i / (float) EDGE_SIZE;
            int alpha = Math.round(maxAlpha * fade);
            if (alpha <= 0)
            {
                continue;
            }
            int color = colorWithAlpha(0x000000, alpha);
            graphics.fill(0, i, width, i + 1, color);
            graphics.fill(0, height - 1 - i, width, height - i, color);
            graphics.fill(i, 0, i + 1, height, color);
            graphics.fill(width - 1 - i, 0, width - i, height, color);
        }
    }

    private static int colorWithAlpha(int rgb, int alpha)
    {
        return (Mth.clamp(alpha, 0, 255) << 24) | (rgb & 0xFFFFFF);
    }
}
