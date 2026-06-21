package com.darkness.jujutsukaisenzone.client.gui.creative;

import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.LocalizationUtils;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.lowdraglib.utils.Size;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class JjkzCreativeTabPanel extends WidgetGroup
{
    private final WidgetGroup tabBarGroup;
    private final WidgetGroup gridGroup;
    private final List<ButtonWidget> categoryTabButtons = new ArrayList<>();
    private final List<ItemStack> itemSlotStacks = new ArrayList<>();

    private ModularUI modularUi;
    private JjkzCreativeTabCategory activeCategory = JjkzCreativeTabCategory.TECHNIQUE;
    @Nullable
    private CreativeModeInventoryScreen creativeScreen;

    public JjkzCreativeTabPanel()
    {
        super(Position.ORIGIN, new Size(JjkzCreativeTabLayout.PANEL_WIDTH, JjkzCreativeTabLayout.PANEL_HEIGHT));
        setBackground(new ColorRectTexture(JjkzCreativeTabLayout.COLOR_PANEL_BG));

        tabBarGroup = new WidgetGroup(0, 0, JjkzCreativeTabLayout.PANEL_WIDTH, JjkzCreativeTabLayout.TAB_HEIGHT);
        gridGroup = new WidgetGroup(
                0,
                JjkzCreativeTabLayout.TAB_HEIGHT,
                JjkzCreativeTabLayout.PANEL_WIDTH,
                JjkzCreativeTabLayout.ITEM_GRID_HEIGHT);

        addWidget(tabBarGroup);
        addWidget(gridGroup);

        buildCategoryTabs();
        rebuildItemGrid();
    }

    public void bind(ModularUI modularUi, CreativeModeInventoryScreen screen)
    {
        this.modularUi = modularUi;
        this.creativeScreen = screen;
        setGui(modularUi);
        attachGuiRecursive(this, modularUi);
    }

    public void setScreenPosition(int screenX, int screenY)
    {
        setSelfPosition(new Position(screenX, screenY));
    }

    public boolean isMouseOverPanel(double mouseX, double mouseY)
    {
        return isMouseOverElement(mouseX, mouseY);
    }

    @Nullable
    public ItemStack getHoveredItemStack(double mouseX, double mouseY)
    {
        Widget hovered = getHoverElement(mouseX, mouseY);
        if (hovered == null)
        {
            return ItemStack.EMPTY;
        }
        int index = categoryTabButtons.indexOf(hovered);
        if (index >= 0)
        {
            return ItemStack.EMPTY;
        }
        for (int i = 0; i < gridGroup.widgets.size(); i++)
        {
            if (gridGroup.widgets.get(i) == hovered && i < itemSlotStacks.size())
            {
                return itemSlotStacks.get(i);
            }
        }
        return ItemStack.EMPTY;
    }

    public ItemStack getStackAtGridIndex(int gridIndex)
    {
        if (gridIndex < 0 || gridIndex >= itemSlotStacks.size())
        {
            return ItemStack.EMPTY;
        }
        return itemSlotStacks.get(gridIndex);
    }

    public boolean handleTabMouseClicked(double mouseX, double mouseY, int button)
    {
        Widget hovered = getHoverElement(mouseX, mouseY);
        if (hovered == null || !categoryTabButtons.contains(hovered))
        {
            return false;
        }
        return hovered.mouseClicked(mouseX, mouseY, button);
    }

    private void buildCategoryTabs()
    {
        tabBarGroup.clearAllWidgets();
        categoryTabButtons.clear();

        JjkzCreativeTabCategory[] categories = JjkzCreativeTabCategory.values();
        for (int i = 0; i < categories.length; i++)
        {
            JjkzCreativeTabCategory category = categories[i];
            int x = i * (JjkzCreativeTabLayout.TAB_WIDTH + JjkzCreativeTabLayout.TAB_GAP);
            ButtonWidget tabButton = new ButtonWidget(
                    x,
                    2,
                    JjkzCreativeTabLayout.TAB_WIDTH,
                    JjkzCreativeTabLayout.TAB_HEIGHT - 4,
                    buildTabTexture(category, category == activeCategory),
                    click -> selectCategory(category));
            tabButton.setClientSideWidget();
            categoryTabButtons.add(tabButton);
            tabBarGroup.addWidget(tabButton);
        }
    }

    private void selectCategory(JjkzCreativeTabCategory category)
    {
        if (activeCategory == category)
        {
            return;
        }
        activeCategory = category;
        refreshCategoryTabTextures();
        rebuildItemGrid();
    }

    private void refreshCategoryTabTextures()
    {
        JjkzCreativeTabCategory[] categories = JjkzCreativeTabCategory.values();
        for (int i = 0; i < categoryTabButtons.size() && i < categories.length; i++)
        {
            categoryTabButtons.get(i).setButtonTexture(
                    buildTabTexture(categories[i], categories[i] == activeCategory));
        }
    }

    private GuiTextureGroup buildTabTexture(JjkzCreativeTabCategory category, boolean active)
    {
        int color = active ? JjkzCreativeTabLayout.COLOR_TAB_ACTIVE : JjkzCreativeTabLayout.COLOR_TAB_IDLE;
        return new GuiTextureGroup(
                new ColorRectTexture(color),
                new TextTexture(LocalizationUtils.format(category.getLabelKey()), 0xFFFFFF));
    }

    private void rebuildItemGrid()
    {
        gridGroup.clearAllWidgets();
        itemSlotStacks.clear();

        List<ItemStack> stacks = activeCategory.getStacks();
        for (int index = 0; index < stacks.size(); index++)
        {
            ItemStack stack = stacks.get(index);
            int col = index % JjkzCreativeTabLayout.GRID_COLS;
            int row = index / JjkzCreativeTabLayout.GRID_COLS;
            int x = col * JjkzCreativeTabLayout.SLOT_STEP;
            int y = row * JjkzCreativeTabLayout.SLOT_STEP;

            itemSlotStacks.add(stack);
            ButtonWidget slotButton = new ButtonWidget(
                    x,
                    y,
                    JjkzCreativeTabLayout.SLOT_STEP,
                    JjkzCreativeTabLayout.SLOT_STEP,
                    new GuiTextureGroup(
                            new ColorRectTexture(JjkzCreativeTabLayout.COLOR_SLOT_BG),
                            new ItemStackTexture(stack)),
                    click -> {
                    });
            slotButton.setClientSideWidget();
            gridGroup.addWidget(slotButton);
        }

        if (modularUi != null)
        {
            attachGuiRecursive(this, modularUi);
        }
    }

    private static void attachGuiRecursive(WidgetGroup group, ModularUI modularUi)
    {
        for (Widget widget : group.getContainedWidgets(true))
        {
            widget.setGui(modularUi);
            if (widget instanceof WidgetGroup childGroup)
            {
                attachGuiRecursive(childGroup, modularUi);
            }
        }
    }
}
