package com.darkness.jujutsukaisenzone.player;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 缓存末地水晶的最后攻击者，供爆炸定罪时归因。
 */
public final class PlayerEndCrystalAttackerData
{
    private static final Map<UUID, UUID> attackerByCrystal = new ConcurrentHashMap<>();

    private PlayerEndCrystalAttackerData()
    {
    }

    public static void markAttacker(EndCrystal crystal, ServerPlayer player)
    {
        attackerByCrystal.put(crystal.getUUID(), player.getUUID());
    }

    public static ServerPlayer takeAttacker(EndCrystal crystal)
    {
        UUID attackerUuid = attackerByCrystal.remove(crystal.getUUID());
        if (attackerUuid == null || crystal.level().isClientSide())
        {
            return null;
        }
        if (crystal.level().getServer() == null)
        {
            return null;
        }
        return crystal.level().getServer().getPlayerList().getPlayer(attackerUuid);
    }
}
