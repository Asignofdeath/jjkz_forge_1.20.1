package com.darkness.jujutsukaisenzone.registry;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.block.JJKZSkyblock;
import com.darkness.jujutsukaisenzone.item.BarrierStudybookItem;
import com.darkness.jujutsukaisenzone.item.LimitlessTechniqueBinderItem;
import com.darkness.jujutsukaisenzone.item.DomainStudybookItem;
import com.darkness.jujutsukaisenzone.item.ReverseStudybookItem;
import com.darkness.jujutsukaisenzone.item.SlashTechniqueSukunaBinderItem;
import com.darkness.jujutsukaisenzone.item.SmGavelWeaponItem;
import com.darkness.jujutsukaisenzone.item.SoulStudybookItem;
import com.darkness.jujutsukaisenzone.item.TechniqueIntelligenceItem;
import com.darkness.jujutsukaisenzone.item.TrialTechniqueBinderItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems
{
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, JuJutsuKaisenZone.MODID);

    public static final RegistryObject<Item> INNATE_TECHNIQUE_LIMITLESS = ITEMS.register(
            "innate_technique_limitless",
            () -> new LimitlessTechniqueBinderItem(new Item.Properties().stacksTo(1))
    );

    /* 方块物品 */
    public static final RegistryObject<Item> JJKZ_SKYBLOCK = ITEMS.register(
            "jjkz_skyblock",
            () -> new BlockItem(ModBlocks.JJKZ_SKYBLOCK.get(), new Item.Properties())
    );

    /** 御厨子 */
    public static final RegistryObject<Item> SLASH_TECHNIQUE_SUKUNA = ITEMS.register(
            "slash_technique_sukuna",
            () -> new SlashTechniqueSukunaBinderItem(new Item.Properties().stacksTo(1))
    );

    /** 审判 */
    public static final RegistryObject<Item> TRIAL_TECHNIQUE = ITEMS.register(
            "trial_technique",
            () -> new TrialTechniqueBinderItem(new Item.Properties().stacksTo(1))
    );

    /** 法槌（Epic Fight 武器） */
    public static final RegistryObject<Item> SM_GAVEL_WEAPON = ITEMS.register(
            "sm_gavel_weapon",
            () -> new SmGavelWeaponItem(new Item.Properties().stacksTo(1))
    );

    /** 处刑人之剑（Epic Fight 武器） */
    public static final RegistryObject<Item> EXECUTIONERS_SWORD = ITEMS.register(
            "executioners_sword",
            () -> new Item(new Item.Properties().stacksTo(1))
    );

    /** 咒力流动反转理论 */
    public static final RegistryObject<Item> REVERSE_STUDYBOOK = ITEMS.register(
            "reverse_studybook",
            () -> new ReverseStudybookItem(new Item.Properties())
    );

    /** 结界的构建与要术 */
    public static final RegistryObject<Item> BARRIER_STUDYBOOK = ITEMS.register(
            "barrier_studybook",
            () -> new BarrierStudybookItem(new Item.Properties())
    );

    /** 虚构空间的具象化 */
    public static final RegistryObject<Item> DOMAIN_STUDYBOOK = ITEMS.register(
            "domain_studybook",
            () -> new DomainStudybookItem(new Item.Properties())
    );

    /** 灵魂研究手册 */
    public static final RegistryObject<Item> SOUL_STUDYBOOK = ITEMS.register(
            "soul_studybook",
            () -> new SoulStudybookItem(new Item.Properties())
    );

    /** 术式情报 */
    public static final RegistryObject<Item> TECHNIQUE_INTELLIGENCE = ITEMS.register(
            "technique_intelligence",
            () -> new TechniqueIntelligenceItem(new Item.Properties())
    );

    private ModItems()
    {
    }

    public static void register(IEventBus eventBus)
    {
        ITEMS.register(eventBus);
    }
}
