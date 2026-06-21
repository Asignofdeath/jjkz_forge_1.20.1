package com.darkness.jujutsukaisenzone.mixin.client;

import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(CreativeModeInventoryScreen.class)
public interface CreativeModeInventoryScreenMixin
{
    @Accessor("selectedTab")
    static CreativeModeTab jjkz$getSelectedTab()
    {
        throw new AssertionError();
    }

    @Invoker("slotClicked")
    void jjkz$invokeSlotClicked(Slot slot, int slotId, int mouseButton, ClickType clickType);
}
