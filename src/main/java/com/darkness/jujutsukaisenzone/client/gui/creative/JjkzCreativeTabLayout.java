package com.darkness.jujutsukaisenzone.client.gui.creative;

/**
 * 创造模式物品格区域布局（相对 {@link net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen} 的 left/top）。
 */
public final class JjkzCreativeTabLayout
{
    /** 原版创造物品格左上角偏移 */
    public static final int GRID_OFFSET_X = 9;
    public static final int GRID_OFFSET_Y = 18;
    /** 原版创造物品格 5 行 × 9 列 */
    public static final int GRID_COLS = 9;
    public static final int VANILLA_PICKER_ROWS = 5;
    public static final int SLOT_STEP = 18;
    /** 玩家快捷栏（创造界面内）相对 guiTop 的 Y */
    public static final int PLAYER_INVENTORY_OFFSET_Y = 112;

    public static final int TAB_HEIGHT = 20;
    /** 4 个分类：术式 / 武器 / 道具 / 咒物，需塞进 PANEL_WIDTH */
    public static final int TAB_WIDTH = 36;
    public static final int TAB_GAP = 4;

    /** 仅覆盖原版创造物品格，不含玩家快捷栏 */
    public static final int PANEL_WIDTH = GRID_COLS * SLOT_STEP;
    public static final int PANEL_HEIGHT = VANILLA_PICKER_ROWS * SLOT_STEP;
    public static final int ITEM_GRID_HEIGHT = PANEL_HEIGHT - TAB_HEIGHT;

    public static final int COLOR_PANEL_BG = 0xFF2A2A2A;
    public static final int COLOR_TAB_IDLE = 0xFF444444;
    public static final int COLOR_TAB_ACTIVE = 0xFF8866AA;
    public static final int COLOR_SLOT_BG = 0xFF8B8B8B;

    private JjkzCreativeTabLayout()
    {
    }
}
