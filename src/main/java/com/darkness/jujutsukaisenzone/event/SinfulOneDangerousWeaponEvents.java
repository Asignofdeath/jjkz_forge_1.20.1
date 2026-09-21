package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.effect.DangerousWeaponRules;
import com.darkness.jujutsukaisenzone.effect.SinfulOneEffect;
import com.darkness.jujutsukaisenzone.player.PlayerCrimeRecordData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.TradeWithVillagerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 合成、附魔或向铁匠 / 工具匠购买武器时，记录「非法制造、买卖危险器具」罪行。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SinfulOneDangerousWeaponEvents
{
    private SinfulOneDangerousWeaponEvents()
    {
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player))
        {
            return;
        }
        if (!isEligiblePlayer(player))
        {
            return;
        }
        if (DangerousWeaponRules.isWeapon(event.getCrafting()))
        {
            applyDangerousWeaponCrime(player, DangerousWeaponRules.CRAFT_CRIME_DESCRIPTION);
        }
    }

    @SubscribeEvent
    public static void onContainerOpen(PlayerContainerEvent.Open event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player))
        {
            return;
        }
        if (!isEligiblePlayer(player))
        {
            return;
        }

        AbstractContainerMenu menu = event.getContainer();
        if (menu instanceof EnchantmentMenu enchantmentMenu)
        {
            attachEnchantListener(player, enchantmentMenu);
        }
        else if (menu instanceof AnvilMenu anvilMenu)
        {
            attachAnvilListener(player, anvilMenu);
        }
    }

    @SubscribeEvent
    public static void onTradeWithVillager(TradeWithVillagerEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player))
        {
            return;
        }
        if (!isEligiblePlayer(player))
        {
            return;
        }
        if (!(event.getAbstractVillager() instanceof Villager villager))
        {
            return;
        }

        VillagerProfession profession = villager.getVillagerData().getProfession();
        if (profession != VillagerProfession.WEAPONSMITH && profession != VillagerProfession.TOOLSMITH)
        {
            return;
        }

        ItemStack result = event.getMerchantOffer().getResult();
        if (DangerousWeaponRules.isWeapon(result))
        {
            applyDangerousWeaponCrime(player, DangerousWeaponRules.TRADE_CRIME_DESCRIPTION);
        }
    }

    private static void attachEnchantListener(ServerPlayer player, EnchantmentMenu menu)
    {
        final boolean[] triggered = {false};
        menu.addSlotListener(new ContainerListener()
        {
            @Override
            public void slotChanged(AbstractContainerMenu containerMenu, int slotIndex, ItemStack stack)
            {
                if (triggered[0] || slotIndex != 0)
                {
                    return;
                }
                if (DangerousWeaponRules.isEnchantedWeapon(stack))
                {
                    triggered[0] = true;
                    applyDangerousWeaponCrime(player, DangerousWeaponRules.ENCHANT_CRIME_DESCRIPTION);
                }
            }

            @Override
            public void dataChanged(AbstractContainerMenu containerMenu, int dataSlot, int value)
            {
            }
        });
    }

    private static void attachAnvilListener(ServerPlayer player, AnvilMenu menu)
    {
        final boolean[] triggered = {false};
        menu.addSlotListener(new ContainerListener()
        {
            @Override
            public void slotChanged(AbstractContainerMenu containerMenu, int slotIndex, ItemStack stack)
            {
                if (triggered[0] || slotIndex != 2)
                {
                    return;
                }
                if (DangerousWeaponRules.isEnchantedWeapon(stack))
                {
                    triggered[0] = true;
                    applyDangerousWeaponCrime(player, DangerousWeaponRules.ENCHANT_CRIME_DESCRIPTION);
                }
            }

            @Override
            public void dataChanged(AbstractContainerMenu containerMenu, int dataSlot, int value)
            {
            }
        });
    }

    private static void applyDangerousWeaponCrime(ServerPlayer player, String description)
    {
        SinfulOneEffect.apply(
                player,
                PlayerCrimeRecordData.Category.DANGEROUS_WEAPON,
                description,
                player.blockPosition());
    }

    private static boolean isEligiblePlayer(ServerPlayer player)
    {
        return !player.isCreative() && !player.isSpectator();
    }
}
