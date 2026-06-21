package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.registry.ModItems;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

/**
 * 图书管理员：随机出售一本咒术理论书，每次补货前仅可购买 1 本。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class VillagerTradeEvents
{
    private static final int STUDYBOOK_TRADE_LEVEL = 4;
    private static final int USES_PER_RESTOCK = 1;
    private static final int VILLAGER_XP = 20;
    private static final float PRICE_MULTIPLIER = 0.05F;

    @SuppressWarnings("unchecked")
    private static final RegistryObject<Item>[] STUDYBOOKS = new RegistryObject[] {
            ModItems.REVERSE_STUDYBOOK,
            ModItems.BARRIER_STUDYBOOK,
            ModItems.DOMAIN_STUDYBOOK,
            ModItems.SOUL_STUDYBOOK
    };

    private VillagerTradeEvents()
    {
    }

    @SubscribeEvent
    public static void onVillagerTrades(VillagerTradesEvent event)
    {
        if (event.getType() != VillagerProfession.LIBRARIAN)
        {
            return;
        }
        Int2ObjectMap<List<VillagerTrades.ItemListing>> trades = event.getTrades();
        List<VillagerTrades.ItemListing> levelTrades = trades.get(STUDYBOOK_TRADE_LEVEL);
        if (levelTrades != null)
        {
            levelTrades.add(new StudybookTradeListing());
        }
    }

    private static final class StudybookTradeListing implements VillagerTrades.ItemListing
    {
        @Override
        public MerchantOffer getOffer(Entity trader, RandomSource random)
        {
            Item book = STUDYBOOKS[random.nextInt(STUDYBOOKS.length)].get();
            ItemStack cost = random.nextBoolean()
                    ? new ItemStack(Items.GOLD_INGOT, 2 + random.nextInt(4))
                    : new ItemStack(Items.DIAMOND, 1 + random.nextInt(3));
            return new MerchantOffer(
                    cost,
                    new ItemStack(book),
                    USES_PER_RESTOCK,
                    VILLAGER_XP,
                    PRICE_MULTIPLIER);
        }
    }
}
