package com.darkness.jujutsukaisenzone.client;

/**
 * 客户端「释放情绪」冷却与蓄力同步缓存（由 S2C 包更新）。
 */
public final class EmotionalReleaseClientState
{
    private static long cooldownUntilGameTime;
    private static boolean charging;

    private EmotionalReleaseClientState()
    {
    }

    public static void applySync(boolean chargingNow, long chargeStartGameTime, long cooldownUntil)
    {
        charging = chargingNow;
        cooldownUntilGameTime = cooldownUntil;
        if (chargingNow)
        {
            EmotionalReleaseChargeHud.begin((int) chargeStartGameTime);
        }
        else
        {
            EmotionalReleaseChargeHud.end();
        }
    }

    public static void forceInterruptLocal()
    {
        charging = false;
        EmotionalReleaseChargeHud.end();
    }

    public static boolean isCharging()
    {
        return charging;
    }

    public static boolean isOnCooldown(long localGameTimeEstimate)
    {
        return cooldownUntilGameTime > 0L && localGameTimeEstimate < cooldownUntilGameTime;
    }

    public static void setCooldownUntil(long gameTime)
    {
        cooldownUntilGameTime = gameTime;
    }
}
