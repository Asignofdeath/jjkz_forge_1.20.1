package com.darkness.jujutsukaisenzone.effect;

import com.darkness.jujutsukaisenzone.compat.EpicFightCompat;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/**
 * 判定物品是否属于「危险器具」武器。
 */
public final class DangerousWeaponRules
{
    public static final String CRAFT_CRIME_DESCRIPTION = "合成武器";
    public static final String ENCHANT_CRIME_DESCRIPTION = "附魔武器";
    public static final String TRADE_CRIME_DESCRIPTION = "购买武器";

    private DangerousWeaponRules()
    {
    }

    public static boolean isWeapon(ItemStack stack)
    {
        if (stack.isEmpty())
        {
            return false;
        }
        return stack.is(ItemTags.SWORDS)
                || stack.is(ItemTags.AXES)
                || stack.getItem() instanceof BowItem
                || stack.getItem() instanceof CrossbowItem
                || stack.getItem() instanceof TridentItem
                || EpicFightCompat.isEpicFightWeapon(stack);
    }

    public static boolean isEnchantedWeapon(ItemStack stack)
    {
        return isWeapon(stack) && EnchantmentHelper.getEnchantments(stack).size() > 0;
    }
}
