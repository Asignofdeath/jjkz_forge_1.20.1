package com.darkness.jujutsukaisenzone.registry;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs
{
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, JuJutsuKaisenZone.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN_TAB = CREATIVE_MODE_TABS.register(
            "main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.jujutsukaisenzone.main"))
                    .icon(() -> new ItemStack(ModItems.INNATE_TECHNIQUE_LIMITLESS.get()))
                    .displayItems((parameters, output) -> {
                        // 至少保留 1 个 PARENT 条目，否则 shouldDisplay() 为 false，标签页不会出现在侧边栏
                        output.accept(
                                ModItems.INNATE_TECHNIQUE_LIMITLESS.get(),
                                CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
                    })
                    .build()
    );

    private ModCreativeTabs()
    {
    }

    public static void register(IEventBus eventBus)
    {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
