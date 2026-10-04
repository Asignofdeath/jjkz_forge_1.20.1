package com.darkness.jujutsukaisenzone.client.gui.quest;

import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.modular.ModularUIGuiContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 打开「任务」全屏 LDLib 界面。
 */
@OnlyIn(Dist.CLIENT)
public final class QuestJournalUI
{
    private QuestJournalUI()
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

        ModularUI ui = new ModularUI(IUIHolder.EMPTY, player);
        ui.setFullScreen();
        ui.widget(new QuestJournalPanelWidget());
        ui.initWidgets();

        ModularUIGuiContainer container = new ModularUIGuiContainer(ui, player.containerMenu.containerId);
        minecraft.setScreen(container);
        player.containerMenu = container.getMenu();
    }
}
