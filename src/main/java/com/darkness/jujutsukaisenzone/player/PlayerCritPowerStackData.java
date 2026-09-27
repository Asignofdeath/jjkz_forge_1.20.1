package com.darkness.jujutsukaisenzone.player;

import com.darkness.jujutsukaisenzone.technique.se.CritPowerStackSeMechanic;
import com.darkness.jujutsukaisenzone.technique.se.SeMechanicRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * 「损坏的皮质手表」等暴击叠层 SE 的层数与过期时刻。
 * <p>
 * 仅服务端权威。⚠️ {@link #expireIfNeeded} 不得在 {@link PlayerCombatPowerData#resolveServer}
 * 读层时同步面板（会递归）。
 */
public final class PlayerCritPowerStackData
{
    private static final String TAG_STACKS = "jujutsukaisenzone_crit_power_stacks";
    private static final String TAG_UNTIL = "jujutsukaisenzone_crit_power_until";

    private PlayerCritPowerStackData()
    {
    }

    public static int getStacks(Player player)
    {
        expireIfNeeded(player);
        return Math.max(0, player.getPersistentData().getInt(TAG_STACKS));
    }

    /**
     * 造成一次合格暴击后叠一层并刷新时长。
     *
     * @return 叠完后的层数；未装备对应 SE 时返回 0
     */
    public static int onCrit(ServerPlayer player)
    {
        var mechanic = SeMechanicRegistry.findEquipped(player, CritPowerStackSeMechanic.class);
        if (mechanic.isEmpty())
        {
            clear(player, true);
            return 0;
        }
        CritPowerStackSeMechanic m = mechanic.get();
        expireIfNeeded(player);
        int next = Math.min(m.maxStacks(), Math.max(0, player.getPersistentData().getInt(TAG_STACKS)) + 1);
        long until = player.level().getGameTime() + Math.max(1, m.durationTicks());
        player.getPersistentData().putInt(TAG_STACKS, next);
        player.getPersistentData().putLong(TAG_UNTIL, until);
        PlayerCombatPowerData.syncToClient(player);
        return next;
    }

    public static void clear(Player player, boolean syncPower)
    {
        boolean had = player.getPersistentData().contains(TAG_STACKS)
                || player.getPersistentData().contains(TAG_UNTIL);
        player.getPersistentData().remove(TAG_STACKS);
        player.getPersistentData().remove(TAG_UNTIL);
        if (syncPower && had && player instanceof ServerPlayer serverPlayer)
        {
            PlayerCombatPowerData.syncToClient(serverPlayer);
        }
    }

    /** 卸下暴击叠层 SE 时清层。 */
    public static void clearIfNoMechanic(ServerPlayer player)
    {
        if (SeMechanicRegistry.findEquipped(player, CritPowerStackSeMechanic.class).isEmpty())
        {
            clear(player, true);
        }
    }

    /**
     * 玩家 tick：过期时清层并同步强度（resolve 读层时只静默过期，不同步）。
     */
    public static void tick(ServerPlayer player)
    {
        if (!player.getPersistentData().contains(TAG_UNTIL))
        {
            return;
        }
        long until = player.getPersistentData().getLong(TAG_UNTIL);
        if (until > player.level().getGameTime())
        {
            return;
        }
        clear(player, true);
    }

    /** @return 是否因过期清掉了层 */
    private static boolean expireIfNeeded(Player player)
    {
        if (!player.getPersistentData().contains(TAG_UNTIL))
        {
            return false;
        }
        long until = player.getPersistentData().getLong(TAG_UNTIL);
        if (until > player.level().getGameTime())
        {
            return false;
        }
        player.getPersistentData().remove(TAG_STACKS);
        player.getPersistentData().remove(TAG_UNTIL);
        return true;
    }
}
