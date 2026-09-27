package com.darkness.jujutsukaisenzone.player;

import com.darkness.jujutsukaisenzone.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * 玩家咒力特质：目前只保存<b>一个</b>特质，供 SE 装载校验读取。
 * <p>
 * 权威数据在 {@link JjkzPlayerDataStorage}；客户端只读缓存，由同步包写入。
 * 读取一律经过 {@link CursedEnergyTrait#byId}，所以任何一侧拿到脏值都只会回落成 {@link CursedEnergyTrait#NORMAL}，
 * 不会抛异常。
 * <p>
 * SE 侧特质在咒物<strong>实例</strong>上（产出随机），装载时要求 {@link #getTrait} 与该实例特质相等。
 */
public final class PlayerCursedEnergyTraitData
{
    private static final String CLIENT_TAG = "jujutsukaisenzone_cursed_energy_trait";

    private PlayerCursedEnergyTraitData()
    {
    }

    public static CursedEnergyTrait getTrait(Player player)
    {
        if (player instanceof ServerPlayer serverPlayer)
        {
            return CursedEnergyTrait.byId(
                    JjkzPlayerDataStorage.get(serverPlayer).getOrCreate(serverPlayer).cursedEnergyTrait);
        }
        return CursedEnergyTrait.byId(player.getPersistentData().getString(CLIENT_TAG));
    }

    public static void setTrait(ServerPlayer player, CursedEnergyTrait trait)
    {
        CursedEnergyTrait resolved = trait == null ? CursedEnergyTrait.NORMAL : trait;
        JjkzPlayerDataStorage.PlayerDataEntry data = JjkzPlayerDataStorage.get(player).getOrCreate(player);
        data.cursedEnergyTrait = resolved.id();
        JjkzPlayerDataStorage.get(player).setDirty();
        syncToClient(player);
    }

    /** 玩家特质是否与咒物实例特质吻合。 */
    public static boolean matchesInstance(Player player, CursedEnergyTrait instanceTrait)
    {
        return instanceTrait != null && getTrait(player) == instanceTrait;
    }

    public static void applyClientSnapshot(Player player, String traitId)
    {
        player.getPersistentData().putString(CLIENT_TAG, CursedEnergyTrait.byId(traitId).id());
    }

    public static void syncToClient(ServerPlayer player)
    {
        ModNetwork.sendCursedEnergyTraitSync(player, getTrait(player).id());
    }
}
