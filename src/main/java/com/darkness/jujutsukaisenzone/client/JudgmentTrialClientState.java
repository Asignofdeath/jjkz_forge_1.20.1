package com.darkness.jujutsukaisenzone.client;

/**
 * 客户端：施法者是否处于「审判」锁定阶段（由服务端同步）。
 */
public final class JudgmentTrialClientState
{
    private static boolean trialLockActive;

    private JudgmentTrialClientState()
    {
    }

    public static boolean isTrialLockActive()
    {
        return trialLockActive;
    }

    public static void setTrialLockActive(boolean active)
    {
        trialLockActive = active;
    }
}
