package com.darkness.jujutsukaisenzone.client.gui.creative;

import com.darkness.jujutsukaisenzone.mixin.client.CreativeModeInventoryScreenMixin;
import com.darkness.jujutsukaisenzone.registry.ModCreativeTabs;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

public final class JjkzCreativeTabHelper
{
    private static final ResourceKey<CreativeModeTab> MAIN_TAB_KEY = ModCreativeTabs.MAIN_TAB.getKey();

    private JjkzCreativeTabHelper()
    {
    }

    public static boolean isMainTabActive()
    {
        CreativeModeTab selected = getSelectedCreativeTab();
        if (selected == null)
        {
            return false;
        }
        if (selected == ModCreativeTabs.MAIN_TAB.get())
        {
            return true;
        }
        return BuiltInRegistries.CREATIVE_MODE_TAB.getResourceKey(selected)
                .filter(MAIN_TAB_KEY::equals)
                .isPresent();
    }

    public static CreativeModeTab getSelectedCreativeTab()
    {
        return CreativeModeInventoryScreenMixin.jjkz$getSelectedTab();
    }

    public static boolean isInPickerOverlayArea(CreativeModeInventoryScreen screen, double mouseX, double mouseY)
    {
        int left = screen.getGuiLeft() + JjkzCreativeTabLayout.GRID_OFFSET_X;
        int top = screen.getGuiTop() + JjkzCreativeTabLayout.GRID_OFFSET_Y;
        return mouseX >= left
                && mouseY >= top
                && mouseX < left + JjkzCreativeTabLayout.PANEL_WIDTH
                && mouseY < top + JjkzCreativeTabLayout.PANEL_HEIGHT;
    }

    public static int getContainerSlotIndex(double mouseX, double mouseY, CreativeModeInventoryScreen screen)
    {
        int left = screen.getGuiLeft() + JjkzCreativeTabLayout.GRID_OFFSET_X;
        int top = screen.getGuiTop() + JjkzCreativeTabLayout.GRID_OFFSET_Y + JjkzCreativeTabLayout.TAB_HEIGHT;
        int localX = (int) (mouseX - left);
        int localY = (int) (mouseY - top);
        if (localX < 0 || localY < 0
                || localX >= JjkzCreativeTabLayout.PANEL_WIDTH
                || localY >= JjkzCreativeTabLayout.ITEM_GRID_HEIGHT)
        {
            return -1;
        }
        int col = localX / JjkzCreativeTabLayout.SLOT_STEP;
        int row = localY / JjkzCreativeTabLayout.SLOT_STEP;
        int containerSlot = (row + 1) * JjkzCreativeTabLayout.GRID_COLS + col;
        return containerSlot < 45 ? containerSlot : -1;
    }

    public static int getGridItemIndex(double mouseX, double mouseY, CreativeModeInventoryScreen screen)
    {
        int left = screen.getGuiLeft() + JjkzCreativeTabLayout.GRID_OFFSET_X;
        int top = screen.getGuiTop() + JjkzCreativeTabLayout.GRID_OFFSET_Y + JjkzCreativeTabLayout.TAB_HEIGHT;
        int localX = (int) (mouseX - left);
        int localY = (int) (mouseY - top);
        if (localX < 0 || localY < 0
                || localX >= JjkzCreativeTabLayout.PANEL_WIDTH
                || localY >= JjkzCreativeTabLayout.ITEM_GRID_HEIGHT)
        {
            return -1;
        }
        return (localY / JjkzCreativeTabLayout.SLOT_STEP) * JjkzCreativeTabLayout.GRID_COLS
                + (localX / JjkzCreativeTabLayout.SLOT_STEP);
    }

    public static boolean isInTabBarArea(CreativeModeInventoryScreen screen, double mouseX, double mouseY)
    {
        int left = screen.getGuiLeft() + JjkzCreativeTabLayout.GRID_OFFSET_X;
        int top = screen.getGuiTop() + JjkzCreativeTabLayout.GRID_OFFSET_Y;
        return mouseX >= left
                && mouseY >= top
                && mouseX < left + JjkzCreativeTabLayout.PANEL_WIDTH
                && mouseY < top + JjkzCreativeTabLayout.TAB_HEIGHT;
    }

    /**
     * 将点击委托给原版 {@link CreativeModeInventoryScreen#slotClicked} 的 CONTAINER 分支逻辑。
     */
    public static void invokeContainerSlotClick(
            CreativeModeInventoryScreen screen,
            int containerSlotIndex,
            ItemStack slotStack,
            int mouseButton)
    {
        if (containerSlotIndex < 0 || containerSlotIndex >= 45)
        {
            return;
        }
        CreativeModeInventoryScreen.ItemPickerMenu menu = screen.getMenu();
        Slot slot = menu.slots.get(containerSlotIndex);
        slot.set(slotStack.isEmpty() ? ItemStack.EMPTY : slotStack.copy());
        CreativeModeInventoryScreenMixin mixin = (CreativeModeInventoryScreenMixin) (Object) screen;
        mixin.jjkz$invokeSlotClicked(slot, slot.index, mouseButton, resolveClickType(mouseButton));
    }

    private static ClickType resolveClickType(int mouseButton)
    {
        Minecraft minecraft = Minecraft.getInstance();
        long window = minecraft.getWindow().getWindow();
        InputConstants.Key mouseKey = InputConstants.Type.MOUSE.getOrCreate(mouseButton);
        if (minecraft.options.keyPickItem.isActiveAndMatches(mouseKey))
        {
            return ClickType.CLONE;
        }
        boolean shift = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
        return shift ? ClickType.QUICK_MOVE : ClickType.PICKUP;
    }
}
