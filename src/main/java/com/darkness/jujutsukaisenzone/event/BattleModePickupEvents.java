package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.compat.EpicFightCompat;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Epic Fight 战斗模式下：掉落物只进入背包（槽位 9~35），不进入快捷栏（0~8）。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BattleModePickupEvents
{
    private BattleModePickupEvents()
    {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onEntityItemPickup(EntityItemPickupEvent event)
    {
        Player player = event.getEntity();
        if (player.level().isClientSide() || !EpicFightCompat.isBattleMode(player))
        {
            return;
        }

        ItemEntity itemEntity = event.getItem();
        ItemStack entityStack = itemEntity.getItem();
        if (entityStack.isEmpty())
        {
            return;
        }

        // 在副本上插入背包，避免直接改实体 stack 时 inventory.add 的创造模式等边界行为导致“拾取消失”
        ItemStack insertStack = entityStack.copy();
        int picked = insertIntoBackpack(player.getInventory(), insertStack);
        event.setCanceled(true);

        if (picked <= 0)
        {
            return;
        }

        entityStack.shrink(picked);

        Item item = entityStack.getItem();
        ItemStack pickupCopy = entityStack.copy();
        pickupCopy.setCount(picked);

        ForgeEventFactory.firePlayerItemPickupEvent(player, itemEntity, pickupCopy);
        player.take(itemEntity, picked);

        if (entityStack.isEmpty())
        {
            itemEntity.discard();
        }

        player.awardStat(Stats.ITEM_PICKED_UP.get(item), picked);
        player.onItemPickup(itemEntity);

        if (player instanceof ServerPlayer serverPlayer)
        {
            serverPlayer.containerMenu.broadcastChanges();
        }
    }

    /**
     * 将物品放入背包区（9~35），返回实际放入数量。
     */
    private static int insertIntoBackpack(Inventory inventory, ItemStack stack)
    {
        int remaining = stack.getCount();

        for (int slot = Inventory.getSelectionSize(); slot < Inventory.INVENTORY_SIZE && remaining > 0; slot++)
        {
            ItemStack inSlot = inventory.getItem(slot);
            if (inSlot.isEmpty() || !ItemStack.isSameItemSameTags(inSlot, stack) || !inSlot.isStackable())
            {
                continue;
            }

            int space = Math.min(inSlot.getMaxStackSize() - inSlot.getCount(), inventory.getMaxStackSize() - inSlot.getCount());
            if (space <= 0)
            {
                continue;
            }

            int moved = Math.min(space, remaining);
            inSlot.grow(moved);
            inSlot.setPopTime(Inventory.POP_TIME_DURATION);
            remaining -= moved;
        }

        for (int slot = Inventory.getSelectionSize(); slot < Inventory.INVENTORY_SIZE && remaining > 0; slot++)
        {
            if (!inventory.getItem(slot).isEmpty())
            {
                continue;
            }

            int moved = Math.min(stack.getMaxStackSize(), remaining);
            ItemStack placed = stack.copy();
            placed.setCount(moved);
            placed.setPopTime(Inventory.POP_TIME_DURATION);
            inventory.setItem(slot, placed);
            remaining -= moved;
        }

        int inserted = stack.getCount() - remaining;
        if (inserted > 0)
        {
            inventory.setChanged();
            stack.setCount(remaining);
        }

        return inserted;
    }
}
