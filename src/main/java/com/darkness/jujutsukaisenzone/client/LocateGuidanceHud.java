package com.darkness.jujutsukaisenzone.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;

/**
 * 结构引导 HUD：实时显示玩家与目标的坐标差。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LocateGuidanceHud
{
    private static boolean active;
    private static String displayName = "";
    private static int targetX;
    private static int targetY;
    private static int targetZ;

    private LocateGuidanceHud()
    {
    }

    public static void setTarget(String name, int x, int y, int z)
    {
        active = true;
        displayName = name == null ? "" : name;
        targetX = x;
        targetY = y;
        targetZ = z;
    }

    public static void clear()
    {
        active = false;
        displayName = "";
    }

    public static boolean isActive()
    {
        return active;
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event)
    {
        clear();
    }

    @SubscribeEvent
    public static void onClientLogin(ClientPlayerNetworkEvent.LoggingIn event)
    {
        clear();
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiOverlayEvent.Post event)
    {
        if (!active || !event.getOverlay().equals(VanillaGuiOverlay.HOTBAR.type()))
        {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.options.hideGui)
        {
            return;
        }

        double dx = targetX + 0.5D - player.getX();
        double dy = targetY + 0.5D - player.getY();
        double dz = targetZ + 0.5D - player.getZ();
        int idx = Mth.floor(dx);
        int idy = Mth.floor(dy);
        int idz = Mth.floor(dz);
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

        String line1 = "【" + displayName + "】";
        String line2 = String.format("ΔX:%+d  ΔY:%+d  ΔZ:%+d", idx, idy, idz);
        String line3 = String.format("距离: %.1fm", dist);

        GuiGraphics graphics = event.getGuiGraphics();
        int sw = event.getWindow().getGuiScaledWidth();
        int y = 12;
        int color = 0xFFE8C56B;
        graphics.drawCenteredString(minecraft.font, line1, sw / 2, y, color);
        graphics.drawCenteredString(minecraft.font, line2, sw / 2, y + 10, 0xFFFFFFFF);
        graphics.drawCenteredString(minecraft.font, line3, sw / 2, y + 20, 0xFFB0B0B0);
    }
}
