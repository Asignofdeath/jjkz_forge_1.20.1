package com.darkness.jujutsukaisenzone.technique.development;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;
import java.util.function.Supplier;

/**
 * 主动技能从当前等级升到下一级所需的消耗：原版经验等级 + 2～5 种咒物（AND）。
 */
public record SkillUpgradeCost(int vanillaXpLevels, List<CursedObjectIngredient> cursedObjects)
{
    public static final int MIN_CURSED_OBJECT_TYPES = 2;
    public static final int MAX_CURSED_OBJECT_TYPES = 5;

    public SkillUpgradeCost
    {
        vanillaXpLevels = Math.max(0, vanillaXpLevels);
        if (cursedObjects == null
                || cursedObjects.size() < MIN_CURSED_OBJECT_TYPES
                || cursedObjects.size() > MAX_CURSED_OBJECT_TYPES)
        {
            throw new IllegalArgumentException(
                    "cursedObjects size must be "
                            + MIN_CURSED_OBJECT_TYPES + ".." + MAX_CURSED_OBJECT_TYPES);
        }
        cursedObjects = List.copyOf(cursedObjects);
    }

    public static SkillUpgradeCost of(int vanillaXpLevels, CursedObjectIngredient... ingredients)
    {
        return new SkillUpgradeCost(vanillaXpLevels, List.of(ingredients));
    }

    /**
     * 单种咒物材料（物品 + 数量）。
     */
    public record CursedObjectIngredient(Supplier<? extends Item> item, int count)
    {
        public CursedObjectIngredient
        {
            count = Math.max(1, count);
            if (item == null)
            {
                throw new IllegalArgumentException("item");
            }
        }

        public static CursedObjectIngredient of(RegistryObject<? extends Item> item, int count)
        {
            return new CursedObjectIngredient(item, count);
        }

        public ItemStack createStack()
        {
            return new ItemStack(item.get(), count);
        }
    }
}
