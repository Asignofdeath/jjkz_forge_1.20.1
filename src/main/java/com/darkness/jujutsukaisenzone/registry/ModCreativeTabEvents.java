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
        event.accept(ModItems.SM_GAVEL_WEAPON, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
        event.accept(ModItems.EXECUTIONERS_SWORD, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
        event.accept(ModItems.REVERSE_STUDYBOOK, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
        event.accept(ModItems.BARRIER_STUDYBOOK, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
        event.accept(ModItems.DOMAIN_STUDYBOOK, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
        event.accept(ModItems.SOUL_STUDYBOOK, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
        event.accept(ModItems.TECHNIQUE_INTELLIGENCE, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
    }
}
