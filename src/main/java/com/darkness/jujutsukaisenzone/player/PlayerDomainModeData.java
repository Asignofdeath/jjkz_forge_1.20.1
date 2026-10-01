package com.darkness.jujutsukaisenzone.player;

import com.darkness.jujutsukaisenzone.network.CommandWheelNetwork;
import com.darkness.jujutsukaisenzone.technique.development.SkillUnlockLogic;
import com.darkness.jujutsukaisenzone.technique.skillset.LimitlessTechniqueSkillSet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 领域展开模式：常规 / 瞬发。会话态，默认常规。
 * <p>
 * 瞬发仅在已解锁无下限「0.2秒的领域展开」时可用；未解锁时强制常规。
 */
public final class PlayerDomainModeData
{
    public enum Mode
    {
        NORMAL,
        INSTANT
    }

    private static final Map<UUID, Mode> MODE = new ConcurrentHashMap<>();
    private static final String CLIENT_TAG = "jujutsukaisenzone_domain_mode_instant";

    private PlayerDomainModeData()
    {
    }

    public static Mode get(Player player)
    {
        if (player == null)
        {
            return Mode.NORMAL;
        }
        if (!canUseInstantMode(player))
        {
            return Mode.NORMAL;
        }
        if (player.level().isClientSide)
        {
            return player.getPersistentData().getBoolean(CLIENT_TAG) ? Mode.INSTANT : Mode.NORMAL;
        }
        return MODE.getOrDefault(player.getUUID(), Mode.NORMAL);
    }

    public static boolean isInstant(Player player)
    {
        return get(player) == Mode.INSTANT;
    }

    public static boolean canUseInstantMode(Player player)
    {
        return player != null
                && PlayerTechniqueData.isLimitless(player)
                && SkillUnlockLogic.isSkillUnlockedById(
                        player,
                        PlayerTechniqueData.TECHNIQUE_LIMITLESS,
                        LimitlessTechniqueSkillSet.SKILL_INSTANT_DOMAIN);
    }

    public static void applyClientSnapshot(Player player, boolean instant)
    {
        if (player == null)
        {
            return;
        }
        player.getPersistentData().putBoolean(CLIENT_TAG, instant);
    }

    public static Mode toggle(ServerPlayer player)
    {
        if (!canUseInstantMode(player))
        {
            set(player, Mode.NORMAL);
            return Mode.NORMAL;
        }
        Mode next = isInstant(player) ? Mode.NORMAL : Mode.INSTANT;
        set(player, next);
        return next;
    }

    public static void set(ServerPlayer player, Mode mode)
    {
        if (player == null)
        {
            return;
        }
        Mode resolved = mode == Mode.INSTANT && canUseInstantMode(player) ? Mode.INSTANT : Mode.NORMAL;
        if (resolved == Mode.NORMAL)
        {
            MODE.remove(player.getUUID());
            CommandWheelNetwork.sendDomainModeSync(player, false);
            return;
        }
        MODE.put(player.getUUID(), Mode.INSTANT);
        CommandWheelNetwork.sendDomainModeSync(player, true);
    }

    public static void clear(ServerPlayer player)
    {
        if (player == null)
        {
            return;
        }
        boolean wasInstant = MODE.containsKey(player.getUUID());
        MODE.remove(player.getUUID());
        if (wasInstant)
        {
            CommandWheelNetwork.sendDomainModeSync(player, false);
        }
    }

    public static void clearIfTechniqueChanged(ServerPlayer player, String newTechniqueId)
    {
        if (!PlayerTechniqueData.TECHNIQUE_LIMITLESS.equals(newTechniqueId))
        {
            clear(player);
        }
    }

    public static void onLogout(ServerPlayer player)
    {
        if (player != null)
        {
            MODE.remove(player.getUUID());
        }
    }
}
