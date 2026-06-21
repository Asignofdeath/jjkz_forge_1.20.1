package com.darkness.jujutsukaisenzone.client.gui.technique;

import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueIntelData;
import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.modular.ModularUIGuiContainer;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 打开「术式开发」全屏 LDLib 画布界面。
 */
@OnlyIn(Dist.CLIENT)
public final class TechniqueDevelopmentUI
{
    private TechniqueDevelopmentUI()
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
        ui.widget(new TechniqueDevelopmentCanvasWidget());
        ui.widget(new LabelWidget(8, 8, "gui.jujutsukaisenzone.technique_development").setTextColor(0xFFFFFF));
        ui.widget(new LabelWidget(8, 22, TechniqueDevelopmentUI::formatIntelLabel).setTextColor(0xFFCCCCCC));
        ui.initWidgets();

        ModularUIGuiContainer container = new ModularUIGuiContainer(ui, player.containerMenu.containerId);
        minecraft.setScreen(container);
        player.containerMenu = container.getMenu();
    }

    private static String formatIntelLabel()
    {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null)
        {
            return "";
        }
        String techniqueId = PlayerTechniqueData.getInnateTechnique(player);
        if (techniqueId.isBlank() || PlayerTechniqueData.TECHNIQUE_NONE.equals(techniqueId))
        {
            return Component.translatable("gui.jujutsukaisenzone.technique_intel_count", 0).getString();
        }
        int intel = PlayerTechniqueIntelData.getIntel(player, techniqueId);
        return Component.translatable("gui.jujutsukaisenzone.technique_intel_count", intel).getString();
    }
}
