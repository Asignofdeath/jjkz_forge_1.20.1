package com.darkness.jujutsukaisenzone.client;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 生命值低于 20% 时，在屏幕边缘绘制红色晕影。
 */
public final class LowHealthVignetteOverlay
{
    private static final float LOW_HEALTH_THRESHOLD = 0.2F;
    private static final int MAX_EDGE_ALPHA = 170;
    private static final int MIN_EDGE_SIZE = 28;
    private static final int MAX_EDGE_SIZE = 56;

    private LowHealthVignetteOverlay()
    {
    }

    static float resolveIntensity(Player player)
    {
        float maxHealth = player.getMaxHealth();
        if (maxHealth <= 0.0F)
        {
            return 0.0F;
        }
        float ratio = player.getHealth() / maxHealth;
        if (ratio > LOW_HEALTH_THRESHOLD)
        {
            return 0.0F;
        }
        return Mth.clamp((LOW_HEALTH_THRESHOLD - ratio) / LOW_HEALTH_THRESHOLD, 0.0F, 1.0F);
    }

    static void draw(GuiGraphics graphics, int screenWidth, int screenHeight, float intensity)
    {
        if (intensity <= 0.0F)
        {
            return;
        }

        int edgeSize = MIN_EDGE_SIZE + Math.round((MAX_EDGE_SIZE - MIN_EDGE_SIZE) * intensity);
        int maxAlpha = Math.round(MAX_EDGE_ALPHA * intensity);

        drawEdgeGradient(graphics, screenWidth, screenHeight, edgeSize, maxAlpha, true);
        drawEdgeGradient(graphics, screenWidth, screenHeight, edgeSize, maxAlpha, false);
    }

    private static void drawEdgeGradient(
            GuiGraphics graphics,
            int screenWidth,
            int screenHeight,
            int edgeSize,
            int maxAlpha,
            boolean horizontal
    )
    {
        for (int i = 0; i < edgeSize; i++)
        {
            float fade = 1.0F - (float) i / (float) edgeSize;
            int alpha = Math.round(maxAlpha * fade);
            if (alpha <= 0)
            {
                continue;
            }
            int color = colorWithAlpha(0xFF0000, alpha);
            if (horizontal)
            {
                graphics.fill(0, i, screenWidth, i + 1, color);
                int bottomY = screenHeight - 1 - i;
                graphics.fill(0, bottomY, screenWidth, bottomY + 1, color);
            }
            else
            {
                graphics.fill(i, 0, i + 1, screenHeight, color);
                int rightX = screenWidth - 1 - i;
                graphics.fill(rightX, 0, rightX + 1, screenHeight, color);
            }
        }
    }

    private static int colorWithAlpha(int rgb, int alpha)
    {
        return (Mth.clamp(alpha, 0, 255) << 24) | (rgb & 0xFFFFFF);
    }

    @Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class Renderer
    {
        private Renderer()
        {
        }

        @SubscribeEvent
        public static void onRenderGuiOverlayPost(RenderGuiOverlayEvent.Post event)
        {
            if (!event.getOverlay().equals(VanillaGuiOverlay.HOTBAR.type()))
            {
                return;
            }

            Minecraft minecraft = Minecraft.getInstance();
            if (!CustomSegmentedHealthHud.shouldRender(minecraft))
            {
                return;
            }

            Player player = minecraft.player;
            float intensity = resolveIntensity(player);
            if (intensity <= 0.0F)
            {
                return;
            }

            draw(
                    event.getGuiGraphics(),
                    event.getWindow().getGuiScaledWidth(),
                    event.getWindow().getGuiScaledHeight(),
                    intensity
            );
        }
    }
}
