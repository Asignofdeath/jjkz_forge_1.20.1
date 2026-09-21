package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.effect.ProtectedChestStructureRules;
import com.darkness.jujutsukaisenzone.effect.SinfulOneEffect;
import com.darkness.jujutsukaisenzone.player.PlayerChestTheftData;
import com.darkness.jujutsukaisenzone.player.PlayerCrimeRecordData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 从地牢、废弃矿井或村庄箱子中取走物品时，记录「盗窃 / 侵占」罪行。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SinfulOneChestTheftEvents
{
    private SinfulOneChestTheftEvents()
    {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event)
    {
        if (event.getLevel().isClientSide())
        {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player))
        {
            return;
        }
        if (!isEligiblePlayer(player))
        {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level))
        {
            return;
        }

        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        if (!ProtectedChestStructureRules.isLootableChestBlock(state))
        {
            return;
        }
        if (!ProtectedChestStructureRules.isProtectedStructureChest(level, pos))
        {
            return;
        }
        PlayerChestTheftData.setPendingChest(player, pos);
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
        if (!(player.level() instanceof ServerLevel level))
        {
            return;
        }

        BlockPos chestPos = PlayerChestTheftData.takePendingChest(player);
        if (chestPos == null)
        {
            return;
        }
        if (!ProtectedChestStructureRules.isProtectedStructureChest(level, chestPos))
        {
            return;
        }
        if (PlayerChestTheftData.hasLooted(player, chestPos))
        {
            return;
        }
        if (!(event.getContainer() instanceof ChestMenu chestMenu))
        {
            return;
        }

        int chestSlots = chestMenu.getRowCount() * 9;
        int[] previousCounts = snapshotSlotCounts(chestMenu, chestSlots);
        BlockPos immutablePos = chestPos.immutable();
        final boolean[] triggered = {false};

        chestMenu.addSlotListener(new ContainerListener()
        {
            @Override
            public void slotChanged(AbstractContainerMenu menu, int slotIndex, ItemStack stack)
            {
                if (triggered[0] || slotIndex >= chestSlots)
                {
                    return;
                }
                int newCount = stack.isEmpty() ? 0 : stack.getCount();
                int oldCount = previousCounts[slotIndex];
                previousCounts[slotIndex] = newCount;
                if (newCount < oldCount)
                {
                    triggered[0] = true;
                    PlayerChestTheftData.markLooted(player, immutablePos);
                    SinfulOneEffect.apply(
                            player,
                            PlayerCrimeRecordData.Category.THEFT,
                            PlayerChestTheftData.CRIME_DESCRIPTION,
                            immutablePos);
                }
            }

            @Override
            public void dataChanged(AbstractContainerMenu menu, int dataSlot, int value)
            {
            }
        });
    }

    private static int[] snapshotSlotCounts(ChestMenu menu, int chestSlots)
    {
        int[] counts = new int[chestSlots];
        for (int i = 0; i < chestSlots; i++)
        {
            ItemStack stack = menu.getSlot(i).getItem();
            counts[i] = stack.isEmpty() ? 0 : stack.getCount();
        }
        return counts;
    }

    private static boolean isEligiblePlayer(ServerPlayer player)
    {
        return !player.isCreative() && !player.isSpectator();
    }
}
