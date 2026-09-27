package com.darkness.jujutsukaisenzone.player;

import com.darkness.jujutsukaisenzone.network.ModNetwork;
import com.darkness.jujutsukaisenzone.technique.development.SpNodeInstance;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * 玩家 SP 钱包。
 * <ul>
 *   <li>{@code spTotal}：生涯获得</li>
 *   <li>{@code spSpentLiberation}：机制解放永久消耗（不退）</li>
 *   <li>{@code spSpentRefresh}：货架付费刷新永久消耗（不退）</li>
 *   <li>节点占用：由已点亮 SP 节点费用动态求和（重置即退回可分配池）</li>
 * </ul>
 * {@link #getAvailable} = total − liberation − refresh − 节点占用。
 */
public final class PlayerSpData
{
    private static final String CLIENT_TAG_AVAILABLE = "jujutsukaisenzone_sp_available";
    private static final String CLIENT_TAG_TOTAL = "jujutsukaisenzone_sp_total";
    private static final String CLIENT_TAG_LIBERATION = "jujutsukaisenzone_sp_liberation";
    private static final String CLIENT_TAG_ALLOCATED = "jujutsukaisenzone_sp_allocated";

    private PlayerSpData()
    {
    }

    /** 可花在解放 / 节点 / 刷新上的余额。 */
    public static int getAvailable(Player player)
    {
        if (player instanceof ServerPlayer serverPlayer)
        {
            return computeAvailable(JjkzPlayerDataStorage.get(serverPlayer).getOrCreate(serverPlayer));
        }
        return Math.max(0, player.getPersistentData().getInt(CLIENT_TAG_AVAILABLE));
    }

    /** @deprecated 用 {@link #getAvailable}；保留别名以免旧调用方编译失败。 */
    @Deprecated
    public static int get(Player player)
    {
        return getAvailable(player);
    }

    public static int getTotal(Player player)
    {
        if (player instanceof ServerPlayer serverPlayer)
        {
            return Math.max(0, JjkzPlayerDataStorage.get(serverPlayer).getOrCreate(serverPlayer).spPoints);
        }
        return Math.max(0, player.getPersistentData().getInt(CLIENT_TAG_TOTAL));
    }

    public static int getSpentLiberation(Player player)
    {
        if (player instanceof ServerPlayer serverPlayer)
        {
            return Math.max(0, JjkzPlayerDataStorage.get(serverPlayer).getOrCreate(serverPlayer).spSpentLiberation);
        }
        return Math.max(0, player.getPersistentData().getInt(CLIENT_TAG_LIBERATION));
    }

    public static int getAllocatedToNodes(Player player)
    {
        if (player instanceof ServerPlayer serverPlayer)
        {
            return allocatedNodeCost(JjkzPlayerDataStorage.get(serverPlayer).getOrCreate(serverPlayer));
        }
        return Math.max(0, player.getPersistentData().getInt(CLIENT_TAG_ALLOCATED));
    }

    public static void add(ServerPlayer player, int amount)
    {
        if (amount <= 0)
        {
            return;
        }
        JjkzPlayerDataStorage.PlayerDataEntry data = JjkzPlayerDataStorage.get(player).getOrCreate(player);
        data.spPoints = Math.max(0, data.spPoints + amount);
        JjkzPlayerDataStorage.get(player).setDirty();
        syncToClient(player);
    }

    /**
     * 机制解放：永久扣减。成功后计入 {@code spSpentLiberation}。
     */
    public static boolean tryConsumeLiberation(ServerPlayer player, int amount)
    {
        if (amount <= 0)
        {
            return true;
        }
        JjkzPlayerDataStorage.PlayerDataEntry data = JjkzPlayerDataStorage.get(player).getOrCreate(player);
        if (computeAvailable(data) < amount)
        {
            return false;
        }
        data.spSpentLiberation = Math.max(0, data.spSpentLiberation + amount);
        JjkzPlayerDataStorage.get(player).setDirty();
        syncToClient(player);
        return true;
    }

    /**
     * @deprecated 解放请用 {@link #tryConsumeLiberation}；节点占用请走 {@link PlayerSpNodeData}。
     */
    @Deprecated
    public static boolean tryConsume(ServerPlayer player, int amount)
    {
        return tryConsumeLiberation(player, amount);
    }

    static int computeAvailable(JjkzPlayerDataStorage.PlayerDataEntry data)
    {
        return Math.max(0,
                Math.max(0, data.spPoints)
                        - Math.max(0, data.spSpentLiberation)
                        - Math.max(0, data.spSpentRefresh)
                        - allocatedNodeCost(data));
    }

    static int allocatedNodeCost(JjkzPlayerDataStorage.PlayerDataEntry data)
    {
        int sum = 0;
        for (SpNodeInstance node : data.spLitNodes)
        {
            sum += node.spCost();
        }
        return Math.max(0, sum);
    }

    public static void applyClientSnapshot(Player player, int available, int total, int liberation, int allocated)
    {
        player.getPersistentData().putInt(CLIENT_TAG_AVAILABLE, Math.max(0, available));
        player.getPersistentData().putInt(CLIENT_TAG_TOTAL, Math.max(0, total));
        player.getPersistentData().putInt(CLIENT_TAG_LIBERATION, Math.max(0, liberation));
        player.getPersistentData().putInt(CLIENT_TAG_ALLOCATED, Math.max(0, allocated));
    }

    /** 兼容旧单参数包。 */
    public static void applyClientSnapshot(Player player, int available)
    {
        applyClientSnapshot(player, available, available, 0, 0);
    }

    public static void syncToClient(ServerPlayer player)
    {
        JjkzPlayerDataStorage.PlayerDataEntry data = JjkzPlayerDataStorage.get(player).getOrCreate(player);
        ModNetwork.sendSpSync(
                player,
                computeAvailable(data),
                Math.max(0, data.spPoints),
                Math.max(0, data.spSpentLiberation),
                allocatedNodeCost(data));
    }
}
