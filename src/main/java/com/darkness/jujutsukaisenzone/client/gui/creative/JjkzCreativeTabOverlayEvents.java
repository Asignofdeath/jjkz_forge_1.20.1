package com.darkness.jujutsukaisenzone.client.gui.creative;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class JjkzCreativeTabOverlayEvents
{
    private JjkzCreativeTabOverlayEvents()
    {
    }

    @SubscribeEvent
    public static void onRenderPost(ScreenEvent.Render.Post event)
    {
        if (!(event.getScreen() instanceof CreativeModeInventoryScreen screen))
        {
            return;
        }
        JjkzCreativeTabOverlayHost host = JjkzCreativeTabOverlayHost.getInstance();
        if (!JjkzCreativeTabHelper.isMainTabActive())
        {
            return;
        }
        host.render(event.getGuiGraphics(), screen, event.getMouseX(), event.getMouseY(), event.getPartialTick());
    }

    @SubscribeEvent
    public static void onMouseButtonPressedPre(ScreenEvent.MouseButtonPressed.Pre event)
    {
        if (!(event.getScreen() instanceof CreativeModeInventoryScreen screen))
        {
            return;
        }
        JjkzCreativeTabOverlayHost host = JjkzCreativeTabOverlayHost.getInstance();
        if (host.handleMouseClicked(screen, event.getMouseX(), event.getMouseY(), event.getButton()))
        {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMouseScrolledPre(ScreenEvent.MouseScrolled.Pre event)
    {
        if (!(event.getScreen() instanceof CreativeModeInventoryScreen screen))
        {
            return;
        }
        JjkzCreativeTabOverlayHost host = JjkzCreativeTabOverlayHost.getInstance();
        if (host.handleMouseScrolled(screen, event.getMouseX(), event.getMouseY(), event.getScrollDelta()))
        {
            event.setCanceled(true);
        }
    }
}
