package com.darkness.jujutsukaisenzone.player;

import com.darkness.jujutsukaisenzone.network.ModNetwork;
import com.darkness.jujutsukaisenzone.technique.skill.TenShadowsShikigami;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * 十种影法术契约：已调伏式神位图（持久化在玩家 PersistentData，死亡时由登录克隆补拷）。
 * <p>
 * 默认仅 {@link TenShadowsShikigami#DIVINE_DOGS} 已调伏。
 */
public final class PlayerTenShadowsContractData
{
    public static final String TAG_TAMED_MASK = "jujutsukaisenzone_ten_shadows_tamed_mask";
    public static final int DEFAULT_MASK = 1 << TenShadowsShikigami.DIVINE_DOGS.index();
    /** 十种式神全部已调伏（index 0～9）。 */
    public static final int ALL_TAMED_MASK = (1 << TenShadowsShikigami.values().length) - 1;

    private PlayerTenShadowsContractData()
    {
    }

    public static int getMask(Player player)
    {
        if (player == null)
        {
            return DEFAULT_MASK;
        }
        if (!player.getPersistentData().contains(TAG_TAMED_MASK))
        {
            return DEFAULT_MASK;
        }
        return player.getPersistentData().getInt(TAG_TAMED_MASK);
    }

    public static boolean isTamed(Player player, TenShadowsShikigami shikigami)
    {
        if (shikigami == null)
        {
            return false;
        }
        return (getMask(player) & (1 << shikigami.index())) != 0;
    }

    public static boolean isTamed(Player player, int shikigamiIndex)
    {
        return isTamed(player, TenShadowsShikigami.byIndex(shikigamiIndex));
    }

    public static void setMask(ServerPlayer player, int mask)
    {
        int normalized = mask | DEFAULT_MASK;
        player.getPersistentData().putInt(TAG_TAMED_MASK, normalized);
        ModNetwork.sendTenShadowsContractSync(player, normalized);
    }

    public static void grantTamed(ServerPlayer player, TenShadowsShikigami shikigami)
    {
        if (shikigami == null)
        {
            return;
        }
        setMask(player, getMask(player) | (1 << shikigami.index()));
    }

    /** 作弊 / 调试：一键写入全部式神契约。 */
    public static void grantAllTamed(ServerPlayer player)
    {
        setMask(player, ALL_TAMED_MASK);
    }

    public static void ensureInitialized(ServerPlayer player)
    {
        if (!player.getPersistentData().contains(TAG_TAMED_MASK))
        {
            setMask(player, DEFAULT_MASK);
        }
    }

    public static void applyClientSnapshot(Player player, int mask)
    {
        if (player == null)
        {
            return;
        }
        player.getPersistentData().putInt(TAG_TAMED_MASK, mask | DEFAULT_MASK);
    }

    public static void copyFrom(Player newPlayer, Player original)
    {
        if (original.getPersistentData().contains(TAG_TAMED_MASK))
        {
            newPlayer.getPersistentData().putInt(TAG_TAMED_MASK, original.getPersistentData().getInt(TAG_TAMED_MASK));
        }
    }

    public static void syncToClient(ServerPlayer player)
    {
        ensureInitialized(player);
        ModNetwork.sendTenShadowsContractSync(player, getMask(player));
    }
}
