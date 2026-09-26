package com.darkness.jujutsukaisenzone.player;

import com.darkness.jujutsukaisenzone.network.ModNetwork;
import com.darkness.jujutsukaisenzone.technique.skill.TenShadowsSummonLogic;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 十种影法术「影之式神」模式：会话态，不写玩家 NBT。
 * <p>
 * 开启后客户端占用数字行 1～0 召唤式神；关闭后交还术式快捷栏输入。
 */
public final class PlayerTenShadowsModeData
{
    private static final Map<UUID, Boolean> ACTIVE = new ConcurrentHashMap<>();

    private PlayerTenShadowsModeData()
    {
    }

    public static boolean isShikigamiMode(Player player)
    {
        if (player == null)
        {
            return false;
        }
        if (player.level().isClientSide)
        {
            return player.getPersistentData().getBoolean(CLIENT_TAG);
        }
        return Boolean.TRUE.equals(ACTIVE.get(player.getUUID()));
    }

    private static final String CLIENT_TAG = "jujutsukaisenzone_ten_shadows_shikigami_mode";

    public static void applyClientSnapshot(Player player, boolean active)
    {
        if (player == null)
        {
            return;
        }
        player.getPersistentData().putBoolean(CLIENT_TAG, active);
    }

    public static boolean toggle(ServerPlayer player)
    {
        boolean next = !isShikigamiMode(player);
        set(player, next);
        return next;
    }

    public static void set(ServerPlayer player, boolean active)
    {
        if (active)
        {
            ACTIVE.put(player.getUUID(), Boolean.TRUE);
        }
        else
        {
            ACTIVE.remove(player.getUUID());
            TenShadowsSummonLogic.dismissAll(player, false);
            com.darkness.jujutsukaisenzone.technique.skill.TenShadowsRitualLogic.clearDraft(player.getUUID());
        }
        ModNetwork.sendTenShadowsModeSync(player, active);
    }

    public static void clear(ServerPlayer player)
    {
        if (player == null)
        {
            return;
        }
        boolean wasActive = ACTIVE.containsKey(player.getUUID());
        ACTIVE.remove(player.getUUID());
        TenShadowsSummonLogic.dismissAll(player, false);
        if (wasActive)
        {
            ModNetwork.sendTenShadowsModeSync(player, false);
        }
    }

    public static void clearIfTechniqueChanged(ServerPlayer player, String newTechniqueId)
    {
        if (!PlayerTechniqueData.TECHNIQUE_TEN_SHADOWS.equals(newTechniqueId))
        {
            clear(player);
            PlayerTenShadowsStanceData.clear(player);
        }
    }

    public static void onLogout(ServerPlayer player)
    {
        ACTIVE.remove(player.getUUID());
        TenShadowsSummonLogic.dismissAll(player, false);
        PlayerTenShadowsRosterData.clearAll(player);
    }
}
