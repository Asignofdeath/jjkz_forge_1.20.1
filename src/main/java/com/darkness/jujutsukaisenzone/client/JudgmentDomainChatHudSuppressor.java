package com.darkness.jujutsukaisenzone.client;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 诛伏赐死结界镜头演出期间，在若干客户端刻内不渲染原版聊天栏区域（{@link VanillaGuiOverlay#CHAT_PANEL}）。
 * <p>
 * 持续时长 220 tick = 11 秒（覆盖镜头组 A 3 秒 + 镜头组 B 2 秒及编排余量）。
 */
public final class JudgmentDomainChatHudSuppressor
{
    public static final int HIDE_CHAT_TICKS = 220;

    private static int remainingHideTicks = 0;

    private JudgmentDomainChatHudSuppressor()
    {
    }

    public static void startHideForJudgmentDomainCamera()
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
    public static final class JudgmentDomainChatHudEvents
    {
        private JudgmentDomainChatHudEvents()
        {
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event)
        {
            if (event.phase != TickEvent.Phase.END)
            {
                return;
            }
            JudgmentDomainChatHudSuppressor.tick();
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
