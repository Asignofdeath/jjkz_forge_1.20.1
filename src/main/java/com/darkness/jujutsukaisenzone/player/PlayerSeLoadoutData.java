package com.darkness.jujutsukaisenzone.player;

import com.darkness.jujutsukaisenzone.network.ModNetwork;
import com.darkness.jujutsukaisenzone.technique.se.SeEffectDef;
import com.darkness.jujutsukaisenzone.technique.se.SeEffectRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * SE 术式侵入槽：装备时从背包扣除咒物，卸下退回；装备中不可作升级耗材。
 */
public final class PlayerSeLoadoutData
{
    /** 固定 SE 槽数量。 */
    public static final int SLOT_COUNT = 2;
    private static final String CLIENT_TAG = "jujutsukaisenzone_se_slots";

    private PlayerSeLoadoutData()
    {
    }

    public static List<String> getSlots(Player player)
    {
        if (player instanceof ServerPlayer serverPlayer)
        {
            return normalize(JjkzPlayerDataStorage.get(serverPlayer).getOrCreate(serverPlayer).seSlots);
        }
        CompoundTag root = player.getPersistentData().getCompound(CLIENT_TAG);
        ListTag list = root.getList("ids", Tag.TAG_STRING);
        List<String> result = new ArrayList<>(SLOT_COUNT);
        for (int i = 0; i < SLOT_COUNT; i++)
        {
            result.add(i < list.size() ? list.getString(i) : "");
        }
        return result;
    }

    public static Optional<SeEffectDef> getEquipped(Player player, int slot)
    {
        List<String> slots = getSlots(player);
        if (slot < 0 || slot >= slots.size())
        {
            return Optional.empty();
        }
        return SeEffectRegistry.find(slots.get(slot));
    }

    public static List<SeEffectDef> getAllEquipped(Player player)
    {
        List<SeEffectDef> result = new ArrayList<>();
        for (String id : getSlots(player))
        {
            SeEffectRegistry.find(id).ifPresent(result::add);
        }
        return result;
    }

    public static boolean isSeEquipped(Player player, String seId)
    {
        if (seId == null || seId.isBlank())
        {
            return false;
        }
        return getSlots(player).contains(seId);
    }

    public static boolean isItemEquippedAsSe(Player player, Item item)
    {
        return SeEffectRegistry.findByItem(item)
                .map(def -> isSeEquipped(player, def.seId()))
                .orElse(false);
    }

    public static SeEquipResult tryEquip(ServerPlayer player, int slot, String seId)
    {
        if (slot < 0 || slot >= SLOT_COUNT)
        {
            return SeEquipResult.BAD_SLOT;
        }
        Optional<SeEffectDef> defOpt = SeEffectRegistry.find(seId);
        if (defOpt.isEmpty())
        {
            return SeEquipResult.UNKNOWN_SE;
        }
        SeEffectDef def = defOpt.get();
        JjkzPlayerDataStorage.PlayerDataEntry data = JjkzPlayerDataStorage.get(player).getOrCreate(player);
        List<String> slots = normalize(data.seSlots);

        if (slots.contains(seId))
        {
            return SeEquipResult.ALREADY_EQUIPPED;
        }
        if (!def.mutexGroup().isEmpty())
        {
            for (String otherId : slots)
            {
                Optional<SeEffectDef> other = SeEffectRegistry.find(otherId);
                if (other.isPresent() && def.mutexGroup().equals(other.get().mutexGroup()))
                {
                    return SeEquipResult.MUTEX_CONFLICT;
                }
            }
        }
        if (!hasItem(player, def.item()))
        {
            return SeEquipResult.MISSING_ITEM;
        }

        String previous = slots.get(slot);
        if (!previous.isBlank())
        {
            SeEffectRegistry.find(previous).ifPresent(prev -> giveItem(player, prev.item()));
        }

        consumeOne(player, def.item());
        slots.set(slot, seId);
        data.seSlots.clear();
        data.seSlots.addAll(slots);
        JjkzPlayerDataStorage.get(player).setDirty();
        syncToClient(player);
        return SeEquipResult.SUCCESS;
    }

    public static SeEquipResult tryUnequip(ServerPlayer player, int slot)
    {
        if (slot < 0 || slot >= SLOT_COUNT)
        {
            return SeEquipResult.BAD_SLOT;
        }
        JjkzPlayerDataStorage.PlayerDataEntry data = JjkzPlayerDataStorage.get(player).getOrCreate(player);
        List<String> slots = normalize(data.seSlots);
        String current = slots.get(slot);
        if (current.isBlank())
        {
            return SeEquipResult.EMPTY_SLOT;
        }
        SeEffectRegistry.find(current).ifPresent(def -> giveItem(player, def.item()));
        slots.set(slot, "");
        data.seSlots.clear();
        data.seSlots.addAll(slots);
        JjkzPlayerDataStorage.get(player).setDirty();
        syncToClient(player);
        return SeEquipResult.SUCCESS;
    }

    public static void applyClientSnapshot(Player player, List<String> slots)
    {
        CompoundTag root = new CompoundTag();
        ListTag list = new ListTag();
        List<String> normalized = normalize(slots);
        for (String id : normalized)
        {
            list.add(StringTag.valueOf(id == null ? "" : id));
        }
        root.put("ids", list);
        player.getPersistentData().put(CLIENT_TAG, root);
    }

    public static void syncToClient(ServerPlayer player)
    {
        ModNetwork.sendSeLoadoutSync(player, getSlots(player));
    }

    private static List<String> normalize(List<String> source)
    {
        List<String> result = new ArrayList<>(SLOT_COUNT);
        for (int i = 0; i < SLOT_COUNT; i++)
        {
            String id = source != null && i < source.size() && source.get(i) != null ? source.get(i) : "";
            result.add(id);
        }
        return result;
    }

    private static boolean hasItem(Player player, Item item)
    {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++)
        {
            if (player.getInventory().getItem(i).is(item))
            {
                return true;
            }
        }
        return false;
    }

    private static void consumeOne(Player player, Item item)
    {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++)
        {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item))
            {
                stack.shrink(1);
                if (player instanceof ServerPlayer serverPlayer)
                {
                    serverPlayer.inventoryMenu.broadcastChanges();
                }
                return;
            }
        }
    }

    private static void giveItem(Player player, Item item)
    {
        ItemStack stack = new ItemStack(item);
        if (!player.getInventory().add(stack))
        {
            player.drop(stack, false);
        }
        if (player instanceof ServerPlayer serverPlayer)
        {
            serverPlayer.inventoryMenu.broadcastChanges();
        }
    }

    public enum SeEquipResult
    {
        SUCCESS,
        BAD_SLOT,
        UNKNOWN_SE,
        ALREADY_EQUIPPED,
        MUTEX_CONFLICT,
        MISSING_ITEM,
        EMPTY_SLOT
    }
}
