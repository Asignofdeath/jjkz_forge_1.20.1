package com.darkness.jujutsukaisenzone.client.gui.creative;

import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

@OnlyIn(Dist.CLIENT)
public final class JjkzCreativeTabOverlayHost
{
    private static final JjkzCreativeTabOverlayHost INSTANCE = new JjkzCreativeTabOverlayHost();

    @Nullable
    private ModularUI modularUi;
    @Nullable
    private JjkzCreativeTabPanel panel;

    private JjkzCreativeTabOverlayHost()
    {
    }

    public static JjkzCreativeTabOverlayHost getInstance()
    {
        return INSTANCE;
    }

    public boolean shouldRender(CreativeModeInventoryScreen screen)
    {
        return JjkzCreativeTabHelper.isMainTabActive();
    }

    public void prepare(CreativeModeInventoryScreen screen)
    {
        if (!shouldRender(screen))
        {
            return;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null)
        {
            return;
        }

        if (panel == null)
        {
            modularUi = new ModularUI(
                    JjkzCreativeTabLayout.PANEL_WIDTH,
                    JjkzCreativeTabLayout.PANEL_HEIGHT,
                    IUIHolder.EMPTY,
                    player);
            panel = new JjkzCreativeTabPanel();
            panel.setClientSideWidget();
            panel.initWidget();
            panel.bind(modularUi, screen);
        }

        int screenX = screen.getGuiLeft() + JjkzCreativeTabLayout.GRID_OFFSET_X;
        int screenY = screen.getGuiTop() + JjkzCreativeTabLayout.GRID_OFFSET_Y;
        panel.setScreenPosition(screenX, screenY);
        panel.bind(modularUi, screen);
    }

    public void render(GuiGraphics graphics, CreativeModeInventoryScreen screen, int mouseX, int mouseY, float partialTick)
    {
        if (!shouldRender(screen))
        {
            return;
        }

        prepare(screen);
        if (panel == null)
        {
            return;
        }

        int left = screen.getGuiLeft() + JjkzCreativeTabLayout.GRID_OFFSET_X;
        int top = screen.getGuiTop() + JjkzCreativeTabLayout.GRID_OFFSET_Y;

        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 300.0F);
        graphics.fill(
                left,
                top,
                left + JjkzCreativeTabLayout.PANEL_WIDTH,
                top + JjkzCreativeTabLayout.PANEL_HEIGHT,
                JjkzCreativeTabLayout.COLOR_PANEL_BG);

        panel.drawInBackground(graphics, mouseX, mouseY, partialTick);
        panel.drawInForeground(graphics, mouseX, mouseY, partialTick);
        graphics.pose().popPose();

        ItemStack hovered = panel.getHoveredItemStack(mouseX, mouseY);
        if (!hovered.isEmpty())
        {
            graphics.renderTooltip(Minecraft.getInstance().font, hovered, mouseX, mouseY);
        }
    }

    public boolean handleMouseClicked(CreativeModeInventoryScreen screen, double mouseX, double mouseY, int button)
    {
        if (!shouldRender(screen))
        {
            return false;
        }
        if (!JjkzCreativeTabHelper.isInPickerOverlayArea(screen, mouseX, mouseY))
        {
            return false;
        }
        prepare(screen);
        if (panel == null)
        {
            return true;
        }

        if (JjkzCreativeTabHelper.isInTabBarArea(screen, mouseX, mouseY))
        {
            panel.handleTabMouseClicked(mouseX, mouseY, button);
            return true;
        }

        int containerSlot = JjkzCreativeTabHelper.getContainerSlotIndex(mouseX, mouseY, screen);
        if (containerSlot >= 0)
        {
            int gridIndex = JjkzCreativeTabHelper.getGridItemIndex(mouseX, mouseY, screen);
            ItemStack slotStack = gridIndex >= 0 ? panel.getStackAtGridIndex(gridIndex) : ItemStack.EMPTY;
            JjkzCreativeTabHelper.invokeContainerSlotClick(screen, containerSlot, slotStack, button);
        }
        return true;
    }

    public boolean handleMouseScrolled(CreativeModeInventoryScreen screen, double mouseX, double mouseY, double delta)
    {
        if (!shouldRender(screen))
        {
            return false;
        }
        return JjkzCreativeTabHelper.isInPickerOverlayArea(screen, mouseX, mouseY);
    }
}
