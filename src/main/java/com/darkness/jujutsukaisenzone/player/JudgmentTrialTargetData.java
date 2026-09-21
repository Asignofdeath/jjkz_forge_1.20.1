package com.darkness.jujutsukaisenzone.player;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * 「审判」：施法者对同一目标的累计审判次数（用于叠加有罪概率）。
 */
public final class JudgmentTrialTargetData
{
    private static final String TAG_ROOT = "jujutsukaisenzone_judgment_trial_target_counts";

    /** 同一目标最多叠加的有罪概率加成（%）。 */
    public static final int MAX_GUILTY_BONUS_PERCENT = 20;
    /** 每次对同一目标审判后，下次有罪概率加成（%）。 */
    public static final double GUILTY_BONUS_PER_TRIAL_PERCENT = 1.5D;

    private JudgmentTrialTargetData()
    {
    }

    public static int getTrialCount(ServerPlayer caster, UUID targetUuid)
    {
        if (targetUuid == null)
        {
            return 0;
        }
        CompoundTag root = caster.getPersistentData().getCompound(TAG_ROOT);
        return Math.max(0, root.getInt(targetUuid.toString()));
    }

    public static double getGuiltyBonusPercent(ServerPlayer caster, UUID targetUuid)
    {
        return Math.min(MAX_GUILTY_BONUS_PERCENT, getTrialCount(caster, targetUuid) * GUILTY_BONUS_PER_TRIAL_PERCENT);
    }

    public static void incrementTrialCount(ServerPlayer caster, UUID targetUuid)
    {
        if (targetUuid == null)
        {
            return;
        }
        CompoundTag root = caster.getPersistentData().getCompound(TAG_ROOT);
        String key = targetUuid.toString();
        root.putInt(key, getTrialCount(caster, targetUuid) + 1);
        caster.getPersistentData().put(TAG_ROOT, root);
    }

    /** 目标被判【有罪】后清除施法者对其的审判叠层。 */
    public static void clearTrialCount(ServerPlayer caster, UUID targetUuid)
    {
        if (targetUuid == null)
        {
            return;
        }
        CompoundTag root = caster.getPersistentData().getCompound(TAG_ROOT);
        String key = targetUuid.toString();
        if (!root.contains(key))
        {
            return;
        }
        root.remove(key);
        caster.getPersistentData().put(TAG_ROOT, root);
    }
}
