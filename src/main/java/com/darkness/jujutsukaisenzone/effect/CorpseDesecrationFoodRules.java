package com.darkness.jujutsukaisenzone.effect;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Set;

/**
 * 判定物品是否为怪物掉落且可食用的「侮辱尸体」相关食物。
 */
public final class CorpseDesecrationFoodRules
{
    public static final String CRIME_DESCRIPTION = "食用怪物掉落物";

    private static final Set<Item> MONSTER_DROP_FOODS = Set.of(
            Items.ROTTEN_FLESH,
            Items.SPIDER_EYE);

    private CorpseDesecrationFoodRules()
    {
    }

    public static boolean isMonsterDropFood(ItemStack stack)
    {
        return !stack.isEmpty() && MONSTER_DROP_FOODS.contains(stack.getItem());
    }
}
