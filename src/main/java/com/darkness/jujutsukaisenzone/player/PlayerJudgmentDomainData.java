package com.darkness.jujutsukaisenzone.player;

import com.darkness.jujutsukaisenzone.gameplay.ModRules;
import net.minecraft.server.level.ServerPlayer;

/**
 * 诛伏赐死领域展开冷却：领域关闭后基础 10 秒；若在上次关闭 30 秒内再次展开并成功关闭，则该次关闭后冷却 20 秒。
 */
public final class PlayerJudgmentDomainData
{
    private static final String TAG_COOLDOWN_UNTIL = "jujutsukaisenzone_judgment_domain_cd_until";
    private static final String TAG_LAST_CLOSED = "jujutsukaisenzone_judgment_domain_last_closed";
    private static final String TAG_PENDING_DOUBLED = "jujutsukaisenzone_judgment_domain_pending_doubled_cd";

    private static final int BASE_COOLDOWN_TICKS = 20 * 10;
    private static final int DOUBLED_COOLDOWN_TICKS = BASE_COOLDOWN_TICKS * 2;
    private static final int RAPID_RECAST_WINDOW_TICKS = 20 * 30;

    private PlayerJudgmentDomainData()
    {
    }

    public static boolean isOnCooldown(ServerPlayer player)
    {
        if (ModRules.SkillNoCooldown.isEnabled())
        {
            return false;
        }
        long now = player.level().getGameTime();
        long until = player.getPersistentData().getLong(TAG_COOLDOWN_UNTIL);
        return until > now;
    }

    public static void notifyCooldownBlocked(ServerPlayer player)
    {
        long now = player.level().getGameTime();
        long until = player.getPersistentData().getLong(TAG_COOLDOWN_UNTIL);
        if (until <= now)
        {
            return;
        }
        double remainSeconds = (until - now) / 20.0D;
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "「诛伏赐死」冷却中：" + String.format("%.1f", remainSeconds) + " 秒"));
    }

    /**
     * 开始蓄力前调用：若距上次领域关闭 ≤30 秒，则标记本次领域关闭后使用 20 秒冷却。
     */
    public static void prepareCast(ServerPlayer player)
    {
        long now = player.level().getGameTime();
        long lastClosed = player.getPersistentData().getLong(TAG_LAST_CLOSED);
        boolean doubled = lastClosed > 0L && (now - lastClosed) <= RAPID_RECAST_WINDOW_TICKS;
        player.getPersistentData().putBoolean(TAG_PENDING_DOUBLED, doubled);
    }

    public static void clearPendingCast(ServerPlayer player)
    {
        player.getPersistentData().remove(TAG_PENDING_DOUBLED);
    }

    /** 领域关闭时调用（含手动解除与裁决后自动解除）。 */
    public static void applyCooldownOnDomainClosed(ServerPlayer player)
    {
        long now = player.level().getGameTime();
        player.getPersistentData().putLong(TAG_LAST_CLOSED, now);
        if (ModRules.SkillNoCooldown.isEnabled())
        {
            clearPendingCast(player);
            return;
        }
        int cooldownTicks = player.getPersistentData().getBoolean(TAG_PENDING_DOUBLED)
                ? DOUBLED_COOLDOWN_TICKS
                : BASE_COOLDOWN_TICKS;
        player.getPersistentData().putLong(TAG_COOLDOWN_UNTIL, now + cooldownTicks);
        clearPendingCast(player);
    }
}
