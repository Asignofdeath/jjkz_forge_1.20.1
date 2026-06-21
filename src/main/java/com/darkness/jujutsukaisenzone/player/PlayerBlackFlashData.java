package com.darkness.jujutsukaisenzone.player;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * 黑闪（Black Flash）运行时状态读写工具类。
 * <p>
 * 所有数据存储在 {@code player.getPersistentData()} 中，仅运行时有效，
 * 玩家死亡/登出后自然重置，不持久化到磁盘。
 * <p>
 * 与 {@link com.darkness.jujutsukaisenzone.technique.LimitlessPurpleShiftSpecialEvents}
 * 使用相同的数据管理模式。
 */
public final class PlayerBlackFlashData
{
    // ---- NBT Tag 常量 ----

    /** 当前连击数 */
    static final String TAG_COMBO_COUNT = "jujutsukaisenzone_bf_combo_count";

    /** 上次体术命中的玩家 tickCount */
    static final String TAG_LAST_HIT_TICK = "jujutsukaisenzone_bf_last_hit_tick";

    /** 上次击杀的游戏时间（level.getGameTime()） */
    static final String TAG_LAST_KILL_TIME = "jujutsukaisenzone_bf_last_kill_time";

    /** 上次成功闪避体术的玩家 tickCount */
    static final String TAG_LAST_DODGE_TICK = "jujutsukaisenzone_bf_last_dodge_tick";

    /** 是否已向 EpicFight 注册 {@code DodgeSuccessEvent} 监听器 */
    static final String TAG_DODGE_LISTENER_REGISTERED = "jujutsukaisenzone_bf_dodge_listener";

    /** 上次黑闪触发时的玩家 tickCount（用于触发冷却） */
    static final String TAG_LAST_TRIGGER_TICK = "jujutsukaisenzone_bf_last_trigger_tick";

    /** Rush Into The ZONE 最高潮：下一次体术攻击必定黑闪 */
    static final String TAG_GUARANTEED_BLACK_FLASH = "jujutsukaisenzone_bf_guaranteed_black_flash";

    private PlayerBlackFlashData()
    {
    }

    // ======== Combo 连击 ========

    /** 连击数硬上限 */
    public static final int MAX_COMBO = 8;

    public static int getComboCount(Player player)
    {
        return player.getPersistentData().getInt(TAG_COMBO_COUNT);
    }

    public static void setComboCount(Player player, int count)
    {
        player.getPersistentData().putInt(TAG_COMBO_COUNT, Math.max(0, Math.min(count, MAX_COMBO)));
    }

    public static void incrementCombo(Player player)
    {
        int current = getComboCount(player);
        if (current < MAX_COMBO)
        {
            player.getPersistentData().putInt(TAG_COMBO_COUNT, current + 1);
        }
    }

    public static void resetCombo(Player player)
    {
        player.getPersistentData().putInt(TAG_COMBO_COUNT, 0);
    }

    // ======== Last Hit 上次命中 ========

    /** 获取上次体术命中时的玩家 tickCount，未命中过返回 -1。 */
    public static int getLastHitTick(Player player)
    {
        if (!player.getPersistentData().contains(TAG_LAST_HIT_TICK))
        {
            return -1;
        }
        return player.getPersistentData().getInt(TAG_LAST_HIT_TICK);
    }

    public static void setLastHitTick(Player player, int tickCount)
    {
        player.getPersistentData().putInt(TAG_LAST_HIT_TICK, tickCount);
    }

    /**
     * 是否处于连击窗口内。
     *
     * @param player      玩家
     * @param windowTicks 窗口大小（tick）
     * @return true 表示当前 tick 仍在连击窗口内
     */
    public static boolean isInComboWindow(Player player, int windowTicks)
    {
        int lastHit = getLastHitTick(player);
        return lastHit >= 0 && (player.tickCount - lastHit) <= windowTicks;
    }

    // ======== Last Kill 上次击杀 ========

    /** 获取上次击杀的 gameTime，未击杀过返回 -1。 */
    public static long getLastKillTime(Player player)
    {
        if (!player.getPersistentData().contains(TAG_LAST_KILL_TIME))
        {
            return -1L;
        }
        return player.getPersistentData().getLong(TAG_LAST_KILL_TIME);
    }

    public static void setLastKillTime(Player player, long gameTime)
    {
        player.getPersistentData().putLong(TAG_LAST_KILL_TIME, gameTime);
    }

    /**
     * 是否处于击杀窗口内。
     */
    public static boolean isInKillWindow(Player player, int windowTicks)
    {
        long lastKill = getLastKillTime(player);
        if (lastKill < 0L)
        {
            return false;
        }
        return (player.level().getGameTime() - lastKill) <= windowTicks;
    }

    // ======== Last Dodge 上次闪避 ========

    /** 获取上次成功闪避体术时的玩家 tickCount，未闪避过返回 -1。 */
    public static int getLastDodgeTick(Player player)
    {
        if (!player.getPersistentData().contains(TAG_LAST_DODGE_TICK))
        {
            return -1;
        }
        return player.getPersistentData().getInt(TAG_LAST_DODGE_TICK);
    }

    public static void setLastDodgeTick(Player player, int tickCount)
    {
        player.getPersistentData().putInt(TAG_LAST_DODGE_TICK, tickCount);
    }

    /**
     * 是否处于闪避窗口内。
     */
    public static boolean isInDodgeWindow(Player player, int windowTicks)
    {
        int lastDodge = getLastDodgeTick(player);
        return lastDodge >= 0 && (player.tickCount - lastDodge) <= windowTicks;
    }

    // ======== 残血判定 ========

    /**
     * 是否处于残血状态。
     *
     * @param player    玩家
     * @param threshold HP 比例阈值（0.0 ~ 1.0）
     */
    public static boolean isLowHp(Player player, double threshold)
    {
        float maxHp = player.getMaxHealth();
        if (maxHp <= 0.0F)
        {
            return false;
        }
        return player.getHealth() / maxHp <= threshold;
    }

    // ======== Dodge Listener 注册标记 ========

    public static boolean isDodgeListenerRegistered(Player player)
    {
        return player.getPersistentData().getBoolean(TAG_DODGE_LISTENER_REGISTERED);
    }

    public static void setDodgeListenerRegistered(Player player, boolean registered)
    {
        player.getPersistentData().putBoolean(TAG_DODGE_LISTENER_REGISTERED, registered);
    }

    // ======== 触发冷却 ========

    /** 获取上次黑闪触发的 tickCount，未触发过返回 -1。 */
    public static int getLastTriggerTick(Player player)
    {
        if (!player.getPersistentData().contains(TAG_LAST_TRIGGER_TICK))
        {
            return -1;
        }
        return player.getPersistentData().getInt(TAG_LAST_TRIGGER_TICK);
    }

    public static void setLastTriggerTick(Player player, int tickCount)
    {
        player.getPersistentData().putInt(TAG_LAST_TRIGGER_TICK, tickCount);
    }

    /** 是否处于黑闪触发冷却窗口内。 */
    public static boolean isInTriggerCooldown(Player player, int cooldownTicks)
    {
        int lastTrigger = getLastTriggerTick(player);
        return lastTrigger >= 0 && (player.tickCount - lastTrigger) <= cooldownTicks;
    }

    // ======== 必定黑闪（Rush Into The ZONE 最高潮） ========

    public static boolean hasGuaranteedBlackFlash(Player player)
    {
        return player.getPersistentData().getBoolean(TAG_GUARANTEED_BLACK_FLASH);
    }

    public static void setGuaranteedBlackFlash(Player player, boolean guaranteed)
    {
        if (guaranteed)
        {
            player.getPersistentData().putBoolean(TAG_GUARANTEED_BLACK_FLASH, true);
        }
        else
        {
            player.getPersistentData().remove(TAG_GUARANTEED_BLACK_FLASH);
        }
    }

    // ======== ZONE 状态 ========

    /** ZONE 状态最高等级 */
    public static final int MAX_ZONE_LEVEL = 3;

    static final String TAG_ZONE_LEVEL = "jujutsukaisenzone_bf_zone_level";
    static final String TAG_ZONE_EXPIRE_TICK = "jujutsukaisenzone_bf_zone_expire_tick";
    static final String TAG_ZONE_TRIGGER_COUNT = "jujutsukaisenzone_bf_zone_trigger_count";
    /** 当前 Zone 等级内触发黑闪的次数，用于升级判定 */
    public static final int ZONE_TRIGGERS_PER_LEVEL = 2;

    /** 获取当前 ZONE 等级（0=未激活，1=I，2=II，3=III）。 */
    public static int getZoneLevel(Player player)
    {
        return player.getPersistentData().getInt(TAG_ZONE_LEVEL);
    }

    /** ZONE 是否处于激活状态（等级≥1且未过期）。 */
    public static boolean isZoneActive(Player player)
    {
        int level = getZoneLevel(player);
        if (level <= 0)
        {
            return false;
        }
        int expire = player.getPersistentData().getInt(TAG_ZONE_EXPIRE_TICK);
        return expire > 0 && player.tickCount < expire;
    }

    /**
     * 强制进入指定 ZONE 等级（用于 Rush Into The ZONE 最高潮等效果）。
     *
     * @param level      目标等级（1=I，2=II，3=III）
     * @param expireTick 到期 tick（当前 tick + 持续时间）
     */
    public static void forceEnterZoneLevel(Player player, int level, int expireTick)
    {
        int clamped = Math.max(1, Math.min(level, MAX_ZONE_LEVEL));
        player.getPersistentData().putInt(TAG_ZONE_LEVEL, clamped);
        player.getPersistentData().putInt(TAG_ZONE_EXPIRE_TICK, expireTick);
        player.getPersistentData().putInt(TAG_ZONE_TRIGGER_COUNT, 0);
    }

    /**
     * 首次进入 ZONE I 级。
     *
     * @param expireTick 到期 tick（当前 tick + 持续时间）
     */
    public static void enterZone(Player player, int expireTick)
    {
        player.getPersistentData().putInt(TAG_ZONE_LEVEL, 1);
        player.getPersistentData().putInt(TAG_ZONE_EXPIRE_TICK, expireTick);
    }

    /**
     * ZONE 升级（I→II→III），已达 III 则只刷新持续时间。
     *
     * @param expireTick 新的到期 tick
     */
    public static void levelUpZone(Player player, int expireTick)
    {
        int current = getZoneLevel(player);
        int next = Math.min(current + 1, MAX_ZONE_LEVEL);
        player.getPersistentData().putInt(TAG_ZONE_LEVEL, next);
        player.getPersistentData().putInt(TAG_ZONE_EXPIRE_TICK, expireTick);
    }

    /** 仅刷新 ZONE 到期时间，不改变等级。 */
    public static void setZoneExpireTick(Player player, int expireTick)
    {
        player.getPersistentData().putInt(TAG_ZONE_EXPIRE_TICK, expireTick);
    }

    /** 清除 ZONE 状态。 */
    public static void clearZone(Player player)
    {
        player.getPersistentData().putInt(TAG_ZONE_LEVEL, 0);
        player.getPersistentData().putInt(TAG_ZONE_EXPIRE_TICK, 0);
        player.getPersistentData().putInt(TAG_ZONE_TRIGGER_COUNT, 0);
    }

    /** 获取当前 Zone 等级内的黑闪触发次数。 */
    public static int getZoneTriggerCount(Player player)
    {
        return player.getPersistentData().getInt(TAG_ZONE_TRIGGER_COUNT);
    }

    public static void setZoneTriggerCount(Player player, int count)
    {
        player.getPersistentData().putInt(TAG_ZONE_TRIGGER_COUNT, count);
    }

    /** Zone 等级内黑闪触发次数 +1。 */
    public static void incrementZoneTriggerCount(Player player)
    {
        int current = getZoneTriggerCount(player);
        player.getPersistentData().putInt(TAG_ZONE_TRIGGER_COUNT, current + 1);
    }

    /**
     * 根据当前 ZONE 等级返回黑闪概率加成。
     */
    public static double getZoneBlackFlashBonus(Player player, double bonusI, double bonusII, double bonusIII)
    {
        int level = getZoneLevel(player);
        return switch (level)
        {
            case 1 -> bonusI;
            case 2 -> bonusII;
            case 3 -> bonusIII;
            default -> 0.0D;
        };
    }

    // ======== 概率计算 ========

    /**
     * 计算当前黑闪触发概率。
     * <p>
     * 公式：baseChance + comboBonus + killBonus + lowHpBonus + dodgeBonus + zoneBonus + targetLowHpBonus
     * 结果上限为 1.0。
     */
    public static double calculateChance(
            Player player,
            double baseChance,
            double comboBonusPerHit,
            double killBonus,
            double lowHpThreshold,
            double lowHpBonus,
            double dodgeBonus,
            int comboWindowTicks,
            int killWindowTicks,
            int dodgeWindowTicks,
            double zoneBonusI,
            double zoneBonusII,
            double zoneBonusIII,
            double targetLowHpBonus)
    {
        double chance = baseChance;

        // 连击加成
        if (isInComboWindow(player, comboWindowTicks))
        {
            chance += getComboCount(player) * comboBonusPerHit;
        }

        // 击杀加成
        if (isInKillWindow(player, killWindowTicks))
        {
            chance += killBonus;
        }

        // 残血加成
        if (isLowHp(player, lowHpThreshold))
        {
            chance += lowHpBonus;
        }

        // 闪避加成
        if (isInDodgeWindow(player, dodgeWindowTicks))
        {
            chance += dodgeBonus;
        }

        // ZONE 加成
        if (isZoneActive(player))
        {
            chance += getZoneBlackFlashBonus(player, zoneBonusI, zoneBonusII, zoneBonusIII);
        }

        // 攻击目标残血加成
        chance += targetLowHpBonus;

        return Math.min(chance, 1.0D);
    }

    // ======== 全部重置 ========

    /** 清除所有黑闪运行时状态。 */
    public static void resetAll(Player player)
    {
        CompoundTag tag = player.getPersistentData();
        tag.remove(TAG_COMBO_COUNT);
        tag.remove(TAG_LAST_HIT_TICK);
        tag.remove(TAG_LAST_KILL_TIME);
        tag.remove(TAG_LAST_DODGE_TICK);
        tag.remove(TAG_LAST_TRIGGER_TICK);
        tag.remove(TAG_GUARANTEED_BLACK_FLASH);
        tag.remove(TAG_ZONE_LEVEL);
        tag.remove(TAG_ZONE_EXPIRE_TICK);
        tag.remove(TAG_ZONE_TRIGGER_COUNT);
        // 注意：不清除 TAG_DODGE_LISTENER_REGISTERED，
        // 避免 PlayerTick 中重复注册监听器
    }
}
