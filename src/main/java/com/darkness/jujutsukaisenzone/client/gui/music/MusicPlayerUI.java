package com.darkness.jujutsukaisenzone.client.gui.music;

import com.darkness.jujutsukaisenzone.client.music.JjkzMusicDirector;
import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.modular.ModularUIGuiContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** 打开「音乐鉴赏」全屏 LDLib 界面。 */
@OnlyIn(Dist.CLIENT)
public final class MusicPlayerUI
{
    private static boolean sessionOpen;

    private MusicPlayerUI()
    {
    }

    public static void open()
    {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null)
        {
            return;
        }

        sessionOpen = true;
        ModularUI ui = new ModularUI(IUIHolder.EMPTY, player);
        ui.setFullScreen();
        ui.widget(new MusicPlayerPanelWidget());
        ui.initWidgets();

        ModularUIGuiContainer container = new ModularUIGuiContainer(ui, player.containerMenu.containerId);
        minecraft.setScreen(container);
        player.containerMenu = container.getMenu();
    }

    public static boolean isSessionOpen()
    {
        return sessionOpen;
    }

    /** 关界面时立刻停鉴赏曲。 */
    public static void onSessionClosed()
    {
        if (!sessionOpen)
        {
            return;
        }
        sessionOpen = false;
        JjkzMusicDirector.stopAppreciationNow();
    }
}
