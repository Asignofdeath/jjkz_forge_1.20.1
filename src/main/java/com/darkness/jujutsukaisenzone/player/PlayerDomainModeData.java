package com.darkness.jujutsukaisenzone.player;

import com.darkness.jujutsukaisenzone.network.CommandWheelNetwork;
import com.darkness.jujutsukaisenzone.technique.development.SkillUnlockLogic;
import com.darkness.jujutsukaisenzone.technique.skillset.LimitlessTechniqueSkillSet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 领域展开模式：常规 / 瞬发 / 开放式。会话态，默认常规。
 * <p>
 * 瞬发：已解锁无下限「0.2秒的领域展开」。
 * 开放式：已解锁无下限「开放式无量空处」。
 */
public final class PlayerDomainModeData
{
    public enum Mode
    {
        NORMAL,
        INSTANT,
        OPEN
    }

    private static final Map<UUID, Mode> MODE = new ConcurrentHashMap<>();
    private static final String CLIENT_TAG = "jujutsukaisenzone_domain_mode";

    private PlayerDomainModeData()
    {
    }

    public static Mode get(Player player)
    {
        if (player == null)
        {
            return Mode.NORMAL;
        }
        Mode raw;
        if (player.level().isClientSide)
        {
            String name = player.getPersistentData().getString(CLIENT_TAG);
            raw = parseMode(name);
        }
        else
        {
            raw = MODE.getOrDefault(player.getUUID(), Mode.NORMAL);
        }
        return sanitize(player, raw);
    }

    public static boolean isInstant(Player player)
    {
        return get(player) == Mode.INSTANT;
    }

    public static boolean isOpen(Player player)
    {
        return get(player) == Mode.OPEN;
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

    public static boolean canUseOpenMode(Player player)
    {
        return player != null
                && PlayerTechniqueData.isLimitless(player)
                && SkillUnlockLogic.isSkillUnlockedById(
                        player,
                        PlayerTechniqueData.TECHNIQUE_LIMITLESS,
                        LimitlessTechniqueSkillSet.SKILL_OPEN_DOMAIN);
    }

    public static boolean canUseDomainModeWheel(Player player)
    {
        return canUseInstantMode(player) || canUseOpenMode(player);
    }

    public static List<Mode> availableModes(Player player)
    {
        List<Mode> modes = new ArrayList<>(3);
        modes.add(Mode.NORMAL);
        if (canUseInstantMode(player))
        {
            modes.add(Mode.INSTANT);
        }
        if (canUseOpenMode(player))
        {
            modes.add(Mode.OPEN);
        }
        return modes;
    }

    public static void applyClientSnapshot(Player player, Mode mode)
    {
        if (player == null)
        {
            return;
        }
        player.getPersistentData().putString(CLIENT_TAG, sanitize(player, mode).name());
    }

    /** @deprecated 兼容旧包；请用 {@link #applyClientSnapshot(Player, Mode)} */
    @Deprecated
    public static void applyClientSnapshot(Player player, boolean instant)
    {
        applyClientSnapshot(player, instant ? Mode.INSTANT : Mode.NORMAL);
    }

    public static Mode toggle(ServerPlayer player)
    {
        List<Mode> available = availableModes(player);
        if (available.size() <= 1)
        {
            set(player, Mode.NORMAL);
            return Mode.NORMAL;
        }
        Mode current = get(player);
        int idx = available.indexOf(current);
        if (idx < 0)
        {
            idx = 0;
        }
        Mode next = available.get((idx + 1) % available.size());
        set(player, next);
        return next;
    }

    public static void set(ServerPlayer player, Mode mode)
    {
        if (player == null)
        {
            return;
        }
        Mode resolved = sanitize(player, mode == null ? Mode.NORMAL : mode);
        if (resolved == Mode.NORMAL)
        {
            MODE.remove(player.getUUID());
            CommandWheelNetwork.sendDomainModeSync(player, Mode.NORMAL);
            return;
        }
        MODE.put(player.getUUID(), resolved);
        CommandWheelNetwork.sendDomainModeSync(player, resolved);
    }

    public static void clear(ServerPlayer player)
    {
        if (player == null)
        {
            return;
        }
        boolean had = MODE.containsKey(player.getUUID());
        MODE.remove(player.getUUID());
        if (had)
        {
            CommandWheelNetwork.sendDomainModeSync(player, Mode.NORMAL);
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

    public static String langKey(Mode mode)
    {
        return switch (mode == null ? Mode.NORMAL : mode)
        {
            case INSTANT -> "command_wheel.jujutsukaisenzone.domain_mode_instant";
            case OPEN -> "command_wheel.jujutsukaisenzone.domain_mode_open";
            default -> "command_wheel.jujutsukaisenzone.domain_mode_normal";
        };
    }

    private static Mode sanitize(Player player, Mode mode)
    {
        if (mode == Mode.INSTANT && !canUseInstantMode(player))
        {
            return Mode.NORMAL;
        }
        if (mode == Mode.OPEN && !canUseOpenMode(player))
        {
            return Mode.NORMAL;
        }
        return mode == null ? Mode.NORMAL : mode;
    }

    private static Mode parseMode(String name)
    {
        if (name == null || name.isEmpty())
        {
            return Mode.NORMAL;
        }
        /* 兼容旧客户端布尔快照残留 */
        if ("true".equalsIgnoreCase(name))
        {
            return Mode.INSTANT;
        }
        if ("false".equalsIgnoreCase(name))
        {
            return Mode.NORMAL;
        }
        try
        {
            return Mode.valueOf(name);
        }
        catch (IllegalArgumentException ignored)
        {
            return Mode.NORMAL;
        }
    }
}
