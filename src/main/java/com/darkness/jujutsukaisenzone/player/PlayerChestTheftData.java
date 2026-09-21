package com.darkness.jujutsukaisenzone.player;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 记录玩家已从受保护结构箱子中取走过物品，避免同一箱子重复定罪。
 */
public final class PlayerChestTheftData
{
    private static final String TAG_ROOT = "jujutsukaisenzone_chest_theft";
    private static final String TAG_LOOTED_CHESTS = "lootedChests";

    private static final Map<UUID, BlockPos> pendingChestByPlayer = new ConcurrentHashMap<>();

    public static final String CRIME_DESCRIPTION = "盗窃 / 侵占";

    private PlayerChestTheftData()
    {
    }

    public static void setPendingChest(ServerPlayer player, BlockPos pos)
    {
        if (pos != null)
        {
            pendingChestByPlayer.put(player.getUUID(), pos.immutable());
        }
    }

    public static BlockPos takePendingChest(ServerPlayer player)
    {
        return pendingChestByPlayer.remove(player.getUUID());
    }

    public static boolean hasLooted(ServerPlayer player, BlockPos pos)
    {
        ListTag list = player.getPersistentData().getCompound(TAG_ROOT).getList(TAG_LOOTED_CHESTS, Tag.TAG_STRING);
        String key = chestKey(pos);
        for (Tag tag : list)
        {
            if (tag instanceof StringTag stringTag && key.equals(stringTag.getAsString()))
            {
                return true;
            }
        }
        return false;
    }

    public static void markLooted(ServerPlayer player, BlockPos pos)
    {
        CompoundTag root = player.getPersistentData().getCompound(TAG_ROOT);
        ListTag list = root.getList(TAG_LOOTED_CHESTS, Tag.TAG_STRING);
        String key = chestKey(pos);
        for (Tag tag : list)
        {
            if (tag instanceof StringTag stringTag && key.equals(stringTag.getAsString()))
            {
                return;
            }
        }
        list.add(StringTag.valueOf(key));
        root.put(TAG_LOOTED_CHESTS, list);
        player.getPersistentData().put(TAG_ROOT, root);
    }

    private static String chestKey(BlockPos pos)
    {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }
}
