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
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 打开「术式开发」全屏 LDLib 画布界面。
 * <p>
 * 五个标签页：
 * <ul>
 *   <li>术式开发 / 空战能力 —— 技能树画布</li>
 *   <li>SP 节点 —— 可重置的战斗强度加点</li>
 *   <li>SE 术式侵入 —— 咒物热装槽</li>
 *   <li>玩家数据 —— {@link PlayerDataPanelWidget}</li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
public final class TechniqueDevelopmentUI
{
    private static final int TAB_X = 8;
    private static final int TAB_Y = 36;
    private static final int TAB_WIDTH = 78;
    private static final int TAB_HEIGHT = 18;
    private static final int TAB_GAP = 3;
    private static final int COLOR_TAB_ACTIVE = 0xFF3A5A8A;
    private static final int COLOR_TAB_IDLE = 0xFF2A2A2A;

    private static final String KEY_TECHNIQUE_TAB = "gui.jujutsukaisenzone.technique_development";
    private static final String KEY_AERIAL_TAB = "gui.jujutsukaisenzone.aerial_combat_development";
    private static final String KEY_SP_NODE_TAB = "gui.jujutsukaisenzone.sp_node.title";
    private static final String KEY_SE_TAB = "gui.jujutsukaisenzone.se.title";
    private static final String KEY_PLAYER_DATA_TAB = "gui.jujutsukaisenzone.player_data";

    /** 客户端会话内记忆：关闭界面后再打开时复用上次标签页。 */
    private static TechniqueDevelopmentSection lastSection = TechniqueDevelopmentSection.TECHNIQUE;

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
        TechniqueDevelopmentSection initial = lastSection;
        context.section = initial;

        String canvasTreeId = initial.hasSkillTree()
                ? initial.treeId(player)
                : TechniqueDevelopmentSection.TECHNIQUE.treeId(player);
        context.canvas = new TechniqueDevelopmentCanvasWidget(canvasTreeId);
        context.canvas.setVisible(initial.hasSkillTree());
        context.playerDataPanel = new PlayerDataPanelWidget();
        context.playerDataPanel.setVisible(initial == TechniqueDevelopmentSection.PLAYER_DATA);
        context.spNodePanel = new SpNodePanelWidget();
        context.spNodePanel.setVisible(initial == TechniqueDevelopmentSection.SP_NODES);
        context.sePanel = new SeLoadoutPanelWidget();
        context.sePanel.setVisible(initial == TechniqueDevelopmentSection.SE_LOADOUT);
        context.titleLabel = new LabelWidget(8, 8, initial.titleKey()).setTextColor(0xFFFFFF);
        context.intelLabel = new LabelWidget(8, 22, TechniqueDevelopmentUI::formatIntelLabel).setTextColor(0xFFCCCCCC);
        context.intelLabel.setVisible(initial == TechniqueDevelopmentSection.TECHNIQUE);

        ModularUI ui = new ModularUI(IUIHolder.EMPTY, player);
        ui.setFullScreen();
        ui.widget(context.canvas);
        ui.widget(context.playerDataPanel);
        ui.widget(context.spNodePanel);
        ui.widget(context.sePanel);
        ui.widget(context.titleLabel);
        ui.widget(context.intelLabel);

        context.techniqueTab = buildTab(context, 0, KEY_TECHNIQUE_TAB, TechniqueDevelopmentSection.TECHNIQUE, player);
        context.aerialTab = buildTab(context, 1, KEY_AERIAL_TAB, TechniqueDevelopmentSection.AERIAL_COMBAT, player);
        context.spNodeTab = buildTab(context, 2, KEY_SP_NODE_TAB, TechniqueDevelopmentSection.SP_NODES, player);
        context.seTab = buildTab(context, 3, KEY_SE_TAB, TechniqueDevelopmentSection.SE_LOADOUT, player);
        context.playerDataTab = buildTab(context, 4, KEY_PLAYER_DATA_TAB, TechniqueDevelopmentSection.PLAYER_DATA, player);

        ui.widget(context.techniqueTab);
        ui.widget(context.aerialTab);
        ui.widget(context.spNodeTab);
        ui.widget(context.seTab);
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
        lastSection = section;
        context.section = section;
        boolean showCanvas = section.hasSkillTree();

        if (showCanvas)
        {
            context.canvas.setTreeId(section.treeId(player));
            context.canvas.rebuildGraph();
        }
        context.canvas.setVisible(showCanvas);
        setPanelVisible(context.playerDataPanel, section == TechniqueDevelopmentSection.PLAYER_DATA);
        setPanelVisible(context.spNodePanel, section == TechniqueDevelopmentSection.SP_NODES);
        setPanelVisible(context.sePanel, section == TechniqueDevelopmentSection.SE_LOADOUT);
        context.titleLabel.setText(section.titleKey());
        context.intelLabel.setVisible(section == TechniqueDevelopmentSection.TECHNIQUE);
        context.techniqueTab.setButtonTexture(buildTabTexture(
                KEY_TECHNIQUE_TAB, section == TechniqueDevelopmentSection.TECHNIQUE));
        context.aerialTab.setButtonTexture(buildTabTexture(
                KEY_AERIAL_TAB, section == TechniqueDevelopmentSection.AERIAL_COMBAT));
        context.spNodeTab.setButtonTexture(buildTabTexture(
                KEY_SP_NODE_TAB, section == TechniqueDevelopmentSection.SP_NODES));
        context.seTab.setButtonTexture(buildTabTexture(
                KEY_SE_TAB, section == TechniqueDevelopmentSection.SE_LOADOUT));
        context.playerDataTab.setButtonTexture(buildTabTexture(
                KEY_PLAYER_DATA_TAB, section == TechniqueDevelopmentSection.PLAYER_DATA));
    }

    private static void setPanelVisible(Widget panel, boolean visible)
    {
        panel.setVisible(visible);
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

    /** 一次 open() 期间共享的可变状态。 */
    private static final class ScreenContext
    {
        private TechniqueDevelopmentSection section;
        private TechniqueDevelopmentCanvasWidget canvas;
        private PlayerDataPanelWidget playerDataPanel;
        private SpNodePanelWidget spNodePanel;
        private SeLoadoutPanelWidget sePanel;
        private LabelWidget titleLabel;
        private LabelWidget intelLabel;
        private ButtonWidget techniqueTab;
        private ButtonWidget aerialTab;
        private ButtonWidget spNodeTab;
        private ButtonWidget seTab;
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

            @Override
            boolean hasSkillTree()
            {
                return true;
            }
        },
        AERIAL_COMBAT(KEY_AERIAL_TAB)
        {
            @Override
            String treeId(LocalPlayer player)
            {
                return AerialCombatSkills.TREE_ID;
            }

            @Override
            boolean hasSkillTree()
            {
                return true;
            }
        },
        SP_NODES(KEY_SP_NODE_TAB)
        {
            @Override
            String treeId(LocalPlayer player)
            {
                return "";
            }

            @Override
            boolean hasSkillTree()
            {
                return false;
            }
        },
        SE_LOADOUT(KEY_SE_TAB)
        {
            @Override
            String treeId(LocalPlayer player)
            {
                return "";
            }

            @Override
            boolean hasSkillTree()
            {
                return false;
            }
        },
        PLAYER_DATA(KEY_PLAYER_DATA_TAB)
        {
            @Override
            String treeId(LocalPlayer player)
            {
                return "";
            }

            @Override
            boolean hasSkillTree()
            {
                return false;
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

        abstract boolean hasSkillTree();
    }
}
