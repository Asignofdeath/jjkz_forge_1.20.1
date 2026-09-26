package com.darkness.jujutsukaisenzone.technique.shikigami;

import net.minecraft.world.entity.player.Player;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** 式神模式注册表。 */
public final class ShikigamiModeRegistry
{
    private static final Map<String, ShikigamiMode> MODES = new ConcurrentHashMap<>();

    private ShikigamiModeRegistry()
    {
    }

    public static void register(ShikigamiMode mode)
    {
        if (mode == null || mode.modeId() == null || mode.modeId().isBlank())
        {
            throw new IllegalArgumentException("ShikigamiMode 无效");
        }
        ShikigamiMode previous = MODES.putIfAbsent(mode.modeId(), mode);
        if (previous != null)
        {
            throw new IllegalStateException("重复注册式神模式：" + mode.modeId());
        }
    }

    public static Optional<ShikigamiMode> get(String modeId)
    {
        if (modeId == null || modeId.isBlank())
        {
            return Optional.empty();
        }
        return Optional.ofNullable(MODES.get(modeId));
    }

    public static Optional<ShikigamiMode> findActive(Player player)
    {
        if (player == null)
        {
            return Optional.empty();
        }
        for (ShikigamiMode mode : MODES.values())
        {
            if (mode.isActive(player))
            {
                return Optional.of(mode);
            }
        }
        return Optional.empty();
    }

    public static Collection<ShikigamiMode> all()
    {
        return MODES.values();
    }
}
