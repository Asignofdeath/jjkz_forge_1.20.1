package com.darkness.jujutsukaisenzone.technique.development;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

/**
 * 主动技能从当前等级升到下一级所需的消耗。
 */
public record SkillUpgradeCost(int vanillaXpLevels, Supplier<? extends Item> cursedObject, int cursedObjectCount)
{
    public SkillUpgradeCost
    {
        vanillaXpLevels = Math.max(0, vanillaXpLevels);
        cursedObjectCount = Math.max(1, cursedObjectCount);
        if (cursedObject == null)
        {
            throw new IllegalArgumentException("cursedObject");
        }
    }

    public static SkillUpgradeCost of(int vanillaXpLevels, RegistryObject<? extends Item> item, int count)
    {
        return new SkillUpgradeCost(vanillaXpLevels, item, count);
    }

    public ItemStack createIngredientStack()
    {
        return new ItemStack(cursedObject.get(), cursedObjectCount);
    }
}
