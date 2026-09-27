package com.darkness.jujutsukaisenzone.registry;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 主创造标签页不在原版格子里列出物品，但仍注册到搜索标签页。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModCreativeTabEvents
{
    private ModCreativeTabEvents()
    {
    }

    @SubscribeEvent
    public static void onBuildCreativeTabContents(BuildCreativeModeTabContentsEvent event)
    {
        if (!event.getTabKey().equals(ModCreativeTabs.MAIN_TAB.getKey()))
        {
            return;
        }

        event.accept(ModItems.INNATE_TECHNIQUE_LIMITLESS, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
        event.accept(ModItems.SLASH_TECHNIQUE_SUKUNA, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
        event.accept(ModItems.TRIAL_TECHNIQUE, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
        event.accept(ModItems.LOVE_RENDEZVOUS, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
        event.accept(ModItems.RATIO_TECHNIQUE, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
        event.accept(ModItems.TEN_SHADOWS_TECHNIQUE, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
        event.accept(ModItems.SM_GAVEL_WEAPON, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
        event.accept(ModItems.EXECUTIONERS_SWORD, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
        event.accept(ModItems.REVERSE_STUDYBOOK, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
        event.accept(ModItems.BARRIER_STUDYBOOK, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
        event.accept(ModItems.DOMAIN_STUDYBOOK, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
        event.accept(ModItems.SOUL_STUDYBOOK, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
        event.accept(ModItems.TECHNIQUE_INTELLIGENCE, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
        event.accept(ModItems.CURSED_OBJECT_RUSTY_POCKET_WATCH, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
        event.accept(ModItems.CURSED_OBJECT_BROKEN_GETA, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
        event.accept(ModItems.CURSED_OBJECT_TORN_FAN, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
        event.accept(ModItems.CURSED_OBJECT_OLD_PIPE, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
        event.accept(ModItems.CURSED_OBJECT_CRACKED_ABACUS, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
        event.accept(ModItems.CURSED_OBJECT_MELTED_ICE_PACK, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
        event.accept(ModItems.CURSED_OBJECT_LOOSE_EARPLUGS, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
        event.accept(ModItems.CURSED_OBJECT_WARPED_MIRROR, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
        event.accept(ModItems.CURSED_OBJECT_WORN_DOLL, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
        event.accept(ModItems.CURSED_OBJECT_CRACKED_PHONE, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
        event.accept(ModItems.CURSED_OBJECT_NAMELESS_PORTRAIT, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
        event.accept(ModItems.CURSED_OBJECT_DRIED_FLOWERS, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
        event.accept(ModItems.CURSED_OBJECT_BROKEN_DIARY, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
        event.accept(ModItems.CURSED_OBJECT_BLOODIED_BANDAGE, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
        event.accept(ModItems.CURSED_OBJECT_SINGLE_BENTO, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
        event.accept(ModItems.CURSED_OBJECT_COLD_WEDDING_RING, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
        event.accept(ModItems.CURSED_OBJECT_TORN_STUDENT_ID, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
        event.accept(ModItems.CURSED_OBJECT_CRACKED_URN, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
        event.accept(ModItems.CURSED_OBJECT_PROOF_OF_THE_STRONGEST, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
        event.accept(ModItems.CURSED_OBJECT_FILLED_TIMESHEET, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
        event.accept(ModItems.CURSED_OBJECT_DAMAGED_LEATHER_WATCH, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
        event.accept(ModItems.CURSED_OBJECT_SUKUNA_FINGER, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
    }
}
