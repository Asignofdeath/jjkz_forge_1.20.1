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
 * <p>
 * 三个标签页：
 * <ul>
 *   <li>术式开发 / 空战能力 —— 技能树画布（同一个 {@link TechniqueDevelopmentCanvasWidget}，切换树 id）</li>
 *   <li>玩家数据 —— 用 {@link PlayerDataPanelWidget} 顶替画布，展示 {@code /jjkz data} +
 *       {@code /jjkz stats} 的内容</li>
 * </ul>
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

    private static final String KEY_TECHNIQUE_TAB = "gui.jujutsukaisenzone.technique_development";
    private static final String KEY_AERIAL_TAB = "gui.jujutsukaisenzone.aerial_combat_development";
    private static final String KEY_PLAYER_DATA_TAB = "gui.jujutsukaisenzone.player_data";

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

        ScreenContext context = new ScreenContext();
        context.section = TechniqueDevelopmentSection.TECHNIQUE;

        context.canvas = new TechniqueDevelopmentCanvasWidget(context.section.treeId(player));
        context.panel = new PlayerDataPanelWidget();
        context.panel.setVisible(false);
        context.titleLabel = new LabelWidget(8, 8, context.section.titleKey()).setTextColor(0xFFFFFF);
        context.intelLabel = new LabelWidget(8, 22, TechniqueDevelopmentUI::formatIntelLabel).setTextColor(0xFFCCCCCC);

        ModularUI ui = new ModularUI(IUIHolder.EMPTY, player);
        ui.setFullScreen();
        ui.widget(context.canvas);
        ui.widget(context.panel);
        ui.widget(context.titleLabel);
        ui.widget(context.intelLabel);

        context.techniqueTab = buildTab(context, 0, KEY_TECHNIQUE_TAB, TechniqueDevelopmentSection.TECHNIQUE, player);
        context.aerialTab = buildTab(context, 1, KEY_AERIAL_TAB, TechniqueDevelopmentSection.AERIAL_COMBAT, player);
        context.playerDataTab = buildTab(context, 2, KEY_PLAYER_DATA_TAB, TechniqueDevelopmentSection.PLAYER_DATA, player);

        ui.widget(context.techniqueTab);
        ui.widget(context.aerialTab);
        ui.widget(context.playerDataTab);
        ui.initWidgets();

        ModularUIGuiContainer container = new ModularUIGuiContainer(ui, player.containerMenu.containerId);
        minecraft.setScreen(container);
        player.containerMenu = container.getMenu();
    }

    private static ButtonWidget buildTab(
            ScreenContext context,
            int index,
            String labelKey,
            TechniqueDevelopmentSection section,
            LocalPlayer player)
    {
        ButtonWidget button = new ButtonWidget(
                TAB_X + index * (TAB_WIDTH + TAB_GAP),
                TAB_Y,
                TAB_WIDTH,
                TAB_HEIGHT,
                buildTabTexture(labelKey, section == context.section),
                click -> selectSection(context, section, player));
        button.setClientSideWidget();
        return button;
    }

    private static void selectSection(ScreenContext context, TechniqueDevelopmentSection section, LocalPlayer player)
    {
        if (context.section == section)
        {
            return;
        }
        context.section = section;
        boolean playerData = section == TechniqueDevelopmentSection.PLAYER_DATA;

        // 画布与数据面板互斥：数据页不需要技能树，也就跳过一次重建。
        if (!playerData)
        {
            context.canvas.setTreeId(section.treeId(player));
            context.canvas.rebuildGraph();
        }
        context.canvas.setVisible(!playerData);
        context.panel.setVisible(playerData);
        context.titleLabel.setText(section.titleKey());
        context.intelLabel.setVisible(section == TechniqueDevelopmentSection.TECHNIQUE);
        context.techniqueTab.setButtonTexture(buildTabTexture(
                KEY_TECHNIQUE_TAB, section == TechniqueDevelopmentSection.TECHNIQUE));
        context.aerialTab.setButtonTexture(buildTabTexture(
                KEY_AERIAL_TAB, section == TechniqueDevelopmentSection.AERIAL_COMBAT));
        context.playerDataTab.setButtonTexture(buildTabTexture(KEY_PLAYER_DATA_TAB, playerData));
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

    /** 一次 open() 期间共享的可变状态，避免把七八个控件在方法间传来传去。 */
    private static final class ScreenContext
    {
        private TechniqueDevelopmentSection section;
        private TechniqueDevelopmentCanvasWidget canvas;
        private PlayerDataPanelWidget panel;
        private LabelWidget titleLabel;
        private LabelWidget intelLabel;
        private ButtonWidget techniqueTab;
        private ButtonWidget aerialTab;
        private ButtonWidget playerDataTab;
    }

    private enum TechniqueDevelopmentSection
    {
        TECHNIQUE(KEY_TECHNIQUE_TAB)
        {
            @Override
            String treeId(LocalPlayer player)
            {
                return PlayerTechniqueData.getInnateTechnique(player);
            }
        },
        AERIAL_COMBAT(KEY_AERIAL_TAB)
        {
            @Override
            String treeId(LocalPlayer player)
            {
                return AerialCombatSkills.TREE_ID;
            }
        },
        /** 数据页没有技能树，{@link #treeId} 不会被使用。 */
        PLAYER_DATA(KEY_PLAYER_DATA_TAB)
        {
            @Override
            String treeId(LocalPlayer player)
            {
                return "";
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
