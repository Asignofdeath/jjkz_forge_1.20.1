package com.darkness.jujutsukaisenzone.player;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** 作弊：术式技能无冷却（{@code /jjkz 技能无冷却}）。 */
public final class PlayerSkillNoCooldownData
{
    private static final String TAG = "jujutsukaisenzone_skill_no_cooldown";

    private PlayerSkillNoCooldownData()
    {
    }

    public static boolean isEnabled(Player player)
    {
        return player.getPersistentData().getBoolean(TAG);
    }

    public static void setEnabled(ServerPlayer player, boolean enabled)
    {
        player.getPersistentData().putBoolean(TAG, enabled);
    }
}
