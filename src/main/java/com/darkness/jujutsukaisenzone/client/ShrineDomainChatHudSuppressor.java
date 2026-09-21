package com.darkness.jujutsukaisenzone.client;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 伏魔神龛结界构建完成后，在若干客户端刻内不渲染原版聊天栏区域（{@link VanillaGuiOverlay#CHAT_PANEL}），减轻 CMDCam 等客户端命令的刷屏反馈。
 * <p>
 * 持续时长与 {@link com.darkness.jujutsukaisenzone.technique.domain.ShrineDomainExpansion} 战前锁定时长（{@code SHRINE_PRE_COMBAT_DURATION_TICKS}，当前 140）一致。
 */
public final class ShrineDomainChatHudSuppressor
{
    /**
     * 与 {@link com.darkness.jujutsukaisenzone.technique.domain.ShrineDomainExpansion} 内 {@code SHRINE_PRE_COMBAT_DURATION_TICKS} 相同（当前为 140）。
     */
    public static final int HIDE_CHAT_TICKS = 140;

    private static int remainingHideTicks = 0;

    private ShrineDomainChatHudSuppressor()
    {
    }

    public static void startHideForShrineDomainComplete()
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
    public static final class ShrineDomainChatHudEvents
    {
        private ShrineDomainChatHudEvents()
        {
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event)
        {
            if (event.phase != TickEvent.Phase.END)
            {
                return;
            }
            ShrineDomainChatHudSuppressor.tick();
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
