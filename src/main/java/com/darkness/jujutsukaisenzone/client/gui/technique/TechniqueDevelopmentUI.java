package com.darkness.jujutsukaisenzone.client.gui.technique;

import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueIntelData;
import com.darkness.jujutsukaisenzone.technique.aerial.AerialCombatSkills;
import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.modular.ModularUIGuiContainer;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
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
    private static final int TAB_X = 8;
    private static final int TAB_Y = 36;
    private static final int TAB_WIDTH = 88;
    private static final int TAB_HEIGHT = 18;
    private static final int TAB_GAP = 4;
    private static final int COLOR_TAB_ACTIVE = 0xFF3A5A8A;
    private static final int COLOR_TAB_IDLE = 0xFF2A2A2A;

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

        TabState tabState = new TabState();
        tabState.section = TechniqueDevelopmentSection.TECHNIQUE;

        TechniqueDevelopmentCanvasWidget canvas = new TechniqueDevelopmentCanvasWidget(tabState.section.treeId(player));
        LabelWidget titleLabel = new LabelWidget(8, 8, tabState.section.titleKey()).setTextColor(0xFFFFFF);
        LabelWidget intelLabel = new LabelWidget(8, 22, TechniqueDevelopmentUI::formatIntelLabel).setTextColor(0xFFCCCCCC);

        ModularUI ui = new ModularUI(IUIHolder.EMPTY, player);
        ui.setFullScreen();
        ui.widget(canvas);
        ui.widget(titleLabel);
        ui.widget(intelLabel);

        tabState.techniqueTab = new ButtonWidget(
                TAB_X,
                TAB_Y,
                TAB_WIDTH,
                TAB_HEIGHT,
                buildTabTexture("gui.jujutsukaisenzone.technique_development", true),
                click -> selectSection(
                        tabState,
                        TechniqueDevelopmentSection.TECHNIQUE,
                        player,
                        canvas,
                        titleLabel,
                        intelLabel,
                        tabState.techniqueTab,
                        tabState.aerialTab));
        tabState.techniqueTab.setClientSideWidget();

        tabState.aerialTab = new ButtonWidget(
                TAB_X + TAB_WIDTH + TAB_GAP,
                TAB_Y,
                TAB_WIDTH,
                TAB_HEIGHT,
                buildTabTexture("gui.jujutsukaisenzone.aerial_combat_development", false),
                click -> selectSection(
                        tabState,
                        TechniqueDevelopmentSection.AERIAL_COMBAT,
                        player,
                        canvas,
                        titleLabel,
                        intelLabel,
                        tabState.techniqueTab,
                        tabState.aerialTab));
        tabState.aerialTab.setClientSideWidget();

        ui.widget(tabState.techniqueTab);
        ui.widget(tabState.aerialTab);
        ui.initWidgets();

        ModularUIGuiContainer container = new ModularUIGuiContainer(ui, player.containerMenu.containerId);
        minecraft.setScreen(container);
        player.containerMenu = container.getMenu();
    }

    private static void selectSection(
            TabState tabState,
            TechniqueDevelopmentSection section,
            LocalPlayer player,
            TechniqueDevelopmentCanvasWidget canvas,
            LabelWidget titleLabel,
            LabelWidget intelLabel,
            ButtonWidget techniqueTab,
            ButtonWidget aerialTab)
    {
        if (tabState.section == section)
        {
            return;
        }
        tabState.section = section;
        canvas.setTreeId(section.treeId(player));
        canvas.rebuildGraph();
        titleLabel.setText(section.titleKey());
        intelLabel.setVisible(section == TechniqueDevelopmentSection.TECHNIQUE);
        techniqueTab.setButtonTexture(buildTabTexture(
                "gui.jujutsukaisenzone.technique_development",
                section == TechniqueDevelopmentSection.TECHNIQUE));
        aerialTab.setButtonTexture(buildTabTexture(
                "gui.jujutsukaisenzone.aerial_combat_development",
                section == TechniqueDevelopmentSection.AERIAL_COMBAT));
    }

    private static GuiTextureGroup buildTabTexture(String labelKey, boolean active)
    {
        int color = active ? COLOR_TAB_ACTIVE : COLOR_TAB_IDLE;
        return new GuiTextureGroup(
                new ColorRectTexture(color),
                new TextTexture(Component.translatable(labelKey).getString(), 0xFFFFFF));
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

    private static final class TabState
    {
        private TechniqueDevelopmentSection section;
        private ButtonWidget techniqueTab;
        private ButtonWidget aerialTab;
    }

    private enum TechniqueDevelopmentSection
    {
        TECHNIQUE("gui.jujutsukaisenzone.technique_development")
        {
            @Override
            String treeId(LocalPlayer player)
            {
                return PlayerTechniqueData.getInnateTechnique(player);
            }
        },
        AERIAL_COMBAT("gui.jujutsukaisenzone.aerial_combat_development")
        {
            @Override
            String treeId(LocalPlayer player)
            {
                return AerialCombatSkills.TREE_ID;
            }
        };

        private final String titleKey;

        TechniqueDevelopmentSection(String titleKey)
        {
            this.titleKey = titleKey;
        }

        String titleKey()
        {
            return titleKey;
        }

        abstract String treeId(LocalPlayer player);
    }
}
