package com.darkness.jujutsukaisenzone.client.gui.creative;

import com.darkness.jujutsukaisenzone.registry.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public enum JjkzCreativeTabCategory
{
    TECHNIQUE(
            "gui.jujutsukaisenzone.creative_tab.category.technique",
            List.of(
                    ModItems.INNATE_TECHNIQUE_LIMITLESS,
                    ModItems.SLASH_TECHNIQUE_SUKUNA,
                    ModItems.TRIAL_TECHNIQUE,
                    ModItems.LOVE_RENDEZVOUS
            )),
    WEAPONS(
            "gui.jujutsukaisenzone.creative_tab.category.weapons",
            List.of(
                    ModItems.SM_GAVEL_WEAPON,
                    ModItems.EXECUTIONERS_SWORD
            )),
    ITEMS(
            "gui.jujutsukaisenzone.creative_tab.category.items",
            List.of(
                    ModItems.REVERSE_STUDYBOOK,
                    ModItems.BARRIER_STUDYBOOK,
                    ModItems.DOMAIN_STUDYBOOK,
                    ModItems.SOUL_STUDYBOOK,
                    ModItems.TECHNIQUE_INTELLIGENCE
            ));

    private final String labelKey;
    private final List<ItemStack> stacks;

    JjkzCreativeTabCategory(String labelKey, List<net.minecraftforge.registries.RegistryObject<Item>> items)
    {
        this.labelKey = labelKey;
        this.stacks = items.stream()
                .map(item -> new ItemStack(item.get()))
                .toList();
    }

    public String getLabelKey()
    {
        return labelKey;
    }

    public List<ItemStack> getStacks()
    {
        return stacks;
    }
}
