package com.darkness.jujutsukaisenzone.client.gui.creative;

import com.darkness.jujutsukaisenzone.registry.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

public enum JjkzCreativeTabCategory
{
    TECHNIQUE(
            "gui.jujutsukaisenzone.creative_tab.category.technique",
            List.of(
                    ModItems.INNATE_TECHNIQUE_LIMITLESS,
                    ModItems.SLASH_TECHNIQUE_SUKUNA,
                    ModItems.TRIAL_TECHNIQUE,
                    ModItems.LOVE_RENDEZVOUS,
                    ModItems.RATIO_TECHNIQUE,
                    ModItems.TEN_SHADOWS_TECHNIQUE
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
                    ModItems.TECHNIQUE_INTELLIGENCE,
                    ModItems.CURSED_OBJECT_RUSTY_POCKET_WATCH,
                    ModItems.CURSED_OBJECT_BROKEN_GETA,
                    ModItems.CURSED_OBJECT_TORN_FAN,
                    ModItems.CURSED_OBJECT_OLD_PIPE,
                    ModItems.CURSED_OBJECT_CRACKED_ABACUS,
                    ModItems.CURSED_OBJECT_MELTED_ICE_PACK,
                    ModItems.CURSED_OBJECT_LOOSE_EARPLUGS,
                    ModItems.CURSED_OBJECT_WARPED_MIRROR,
                    ModItems.CURSED_OBJECT_WORN_DOLL,
                    ModItems.CURSED_OBJECT_CRACKED_PHONE,
                    ModItems.CURSED_OBJECT_NAMELESS_PORTRAIT,
                    ModItems.CURSED_OBJECT_DRIED_FLOWERS,
                    ModItems.CURSED_OBJECT_BROKEN_DIARY,
                    ModItems.CURSED_OBJECT_BLOODIED_BANDAGE,
                    ModItems.CURSED_OBJECT_SINGLE_BENTO,
                    ModItems.CURSED_OBJECT_COLD_WEDDING_RING,
                    ModItems.CURSED_OBJECT_TORN_STUDENT_ID,
                    ModItems.CURSED_OBJECT_CRACKED_URN,
                    ModItems.CURSED_OBJECT_CHIPPED_CUTTING_BOARD,
                    ModItems.CURSED_OBJECT_PROOF_OF_THE_STRONGEST,
                    ModItems.CURSED_OBJECT_FILLED_TIMESHEET,
                    ModItems.CURSED_OBJECT_DAMAGED_LEATHER_WATCH,
                    ModItems.CURSED_OBJECT_SUKUNA_FINGER
            ));

    private final String labelKey;
    private final List<RegistryObject<Item>> items;

    JjkzCreativeTabCategory(String labelKey, List<RegistryObject<Item>> items)
    {
        this.labelKey = labelKey;
        this.items = List.copyOf(items);
    }

    public String getLabelKey()
    {
        return labelKey;
    }

    /** 每次取用时生成栈；咒物给未探查空白件（可堆叠，右键探查后再实例化）。 */
    public List<ItemStack> getStacks()
    {
        return items.stream()
                .map(item -> new ItemStack(item.get()))
                .toList();
    }
}
