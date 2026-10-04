package com.darkness.jujutsukaisenzone.client;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 主线 1.1 小屋巡镜期间不渲染原版聊天栏（{@link VanillaGuiOverlay#CHAT_PANEL}），挡住 CMDCam 命令回执。
 * <p>
 * 时长覆盖黑屏旁白 + 三组巡镜 + 电影黑幕对话。
 */
public final class MainQuest11CabinChatHudSuppressor
{
    public static final int HIDE_CHAT_TICKS = 1400;

    private static int remainingHideTicks = 0;

    private MainQuest11CabinChatHudSuppressor()
    {
    }

    public static void startHideForCabinCamera()
    {
        remainingHideTicks = HIDE_CHAT_TICKS;
    }

    static void tick()
    {
        if (remainingHideTicks > 0)
        {
            remainingHideTicks--;
        }
    }

    @Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class Events
    {
        private Events()
        {
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event)
        {
            if (event.phase != TickEvent.Phase.END)
            {
                return;
            }
            MainQuest11CabinChatHudSuppressor.tick();
        }

        @SubscribeEvent
        public static void onRenderGuiOverlayPre(RenderGuiOverlayEvent.Pre event)
        {
            if (remainingHideTicks <= 0)
            {
                return;
            }
            if (event.getOverlay().equals(VanillaGuiOverlay.CHAT_PANEL.type()))
            {
                event.setCanceled(true);
            }
        }
    }
}
