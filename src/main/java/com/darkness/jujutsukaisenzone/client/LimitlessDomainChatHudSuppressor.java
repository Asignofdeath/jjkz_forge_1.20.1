package com.darkness.jujutsukaisenzone.client;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 无量空处结界构建完成后，在若干客户端刻内不渲染原版聊天栏区域（{@link VanillaGuiOverlay#CHAT_PANEL}），减轻 CMDCam 等客户端命令的刷屏反馈。
 * <p>
 * 持续时长与 {@link com.darkness.jujutsukaisenzone.technique.domain.LimitlessDomainExpansion} 施法者演出锁时长（{@code LOCK_PLAYER_DURATION_TICKS}，当前 280）一致。
 */
public final class LimitlessDomainChatHudSuppressor
{
    /**
     * 与 {@link com.darkness.jujutsukaisenzone.technique.domain.LimitlessDomainExpansion} 内 {@code LOCK_PLAYER_DURATION_TICKS} 相同（当前为 280）。
     */
    public static final int HIDE_CHAT_TICKS = 280;

    private static int remainingHideTicks = 0;

    private LimitlessDomainChatHudSuppressor()
    {
    }

    /**
     * 自下一刻起隐藏聊天栏 {@link #HIDE_CHAT_TICKS} 个客户端刻；若已在隐藏中则重新计满。
     */
    public static void startHideForLimitlessDomainComplete()
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

    public static boolean isHidingChatHud()
    {
        return remainingHideTicks > 0;
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
            LimitlessDomainChatHudSuppressor.tick();
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
