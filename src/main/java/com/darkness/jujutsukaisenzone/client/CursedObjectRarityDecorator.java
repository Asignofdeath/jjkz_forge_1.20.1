package com.darkness.jujutsukaisenzone.client;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.item.CursedObjectItem;
import com.darkness.jujutsukaisenzone.registry.ModItems;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterItemDecorationsEvent;
import net.minecraftforge.client.IItemDecorator;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;

/**
 * 咒物背包/快捷栏稀有度边框：罕见青框、特级金框；常见不画。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CursedObjectRarityDecorator
{
    private static final IItemDecorator DECORATOR = CursedObjectRarityDecorator::renderBorder;

    private CursedObjectRarityDecorator()
    {
    }

    @SubscribeEvent
    public static void onRegisterItemDecorations(RegisterItemDecorationsEvent event)
    {
        for (RegistryObject<Item> entry : ModItems.ITEMS.getEntries())
        {
            Item item = entry.get();
            if (item instanceof CursedObjectItem)
            {
                event.register(item, DECORATOR);
            }
        }
    }

    private static boolean renderBorder(GuiGraphics graphics, Font font, ItemStack stack, int xOffset, int yOffset)
    {
        if (!(stack.getItem() instanceof CursedObjectItem cursed))
        {
            return false;
        }
        int color = cursed.grade().borderColorArgb();
        if (color == 0)
        {
            return false;
        }
        graphics.fill(xOffset, yOffset, xOffset + 16, yOffset + 1, color);
        graphics.fill(xOffset, yOffset + 15, xOffset + 16, yOffset + 16, color);
        graphics.fill(xOffset, yOffset, xOffset + 1, yOffset + 16, color);
        graphics.fill(xOffset + 15, yOffset, xOffset + 16, yOffset + 16, color);
        return true;
    }
}
