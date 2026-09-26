package com.darkness.jujutsukaisenzone.player;

import com.darkness.jujutsukaisenzone.network.CommandWheelNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 十影式神作战姿态：协同攻击 / 护卫。会话态，默认协同。
 */
public final class PlayerTenShadowsStanceData
{
    public enum Stance
    {
        ASSIST,
        GUARD
    }

    private static final Map<UUID, Stance> STANCE = new ConcurrentHashMap<>();
    private static final String CLIENT_TAG = "jujutsukaisenzone_ten_shadows_stance_guard";

    private PlayerTenShadowsStanceData()
    {
    }

    public static Stance get(Player player)
    {
        if (player == null)
        {
            return Stance.ASSIST;
        }
        if (player.level().isClientSide)
        {
            return player.getPersistentData().getBoolean(CLIENT_TAG) ? Stance.GUARD : Stance.ASSIST;
        }
        return STANCE.getOrDefault(player.getUUID(), Stance.ASSIST);
    }

    public static boolean isGuard(Player player)
    {
        return get(player) == Stance.GUARD;
    }

    public static boolean isAssist(Player player)
    {
        return get(player) == Stance.ASSIST;
    }

    public static void applyClientSnapshot(Player player, boolean guard)
    {
        if (player == null)
        {
            return;
        }
        player.getPersistentData().putBoolean(CLIENT_TAG, guard);
    }

    public static Stance toggle(ServerPlayer player)
    {
        Stance next = isGuard(player) ? Stance.ASSIST : Stance.GUARD;
        set(player, next);
        return next;
    }

    public static void set(ServerPlayer player, Stance stance)
    {
        if (stance == null || stance == Stance.ASSIST)
        {
            STANCE.remove(player.getUUID());
            CommandWheelNetwork.sendStanceSync(player, false);
            return;
        }
        STANCE.put(player.getUUID(), Stance.GUARD);
        CommandWheelNetwork.sendStanceSync(player, true);
    }

    public static void clear(ServerPlayer player)
    {
        if (player == null)
        {
            return;
        }
        boolean wasGuard = STANCE.containsKey(player.getUUID());
        STANCE.remove(player.getUUID());
        if (wasGuard)
        {
            CommandWheelNetwork.sendStanceSync(player, false);
        }
    }

    public static void onLogout(ServerPlayer player)
    {
        STANCE.remove(player.getUUID());
    }
}
