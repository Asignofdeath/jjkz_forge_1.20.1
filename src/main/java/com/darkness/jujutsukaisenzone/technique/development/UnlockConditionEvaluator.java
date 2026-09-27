package com.darkness.jujutsukaisenzone.technique.development;

import com.darkness.jujutsukaisenzone.player.PlayerCursedGradeData;
import com.darkness.jujutsukaisenzone.player.PlayerDomainExpansionData;
import com.darkness.jujutsukaisenzone.player.PlayerMeltdownRepairData;
import com.darkness.jujutsukaisenzone.player.PlayerReverseTechniqueData;
import com.darkness.jujutsukaisenzone.technique.MeltdownRepairLearnLogic;
import com.darkness.jujutsukaisenzone.technique.commonskill.ReverseTechniqueCommonKeySkill;
import net.minecraft.world.entity.player.Player;

/**
 * 统一评估解锁条件，供 UI 与服务端共用。
 */
public final class UnlockConditionEvaluator
{
    private UnlockConditionEvaluator()
    {
    }

    public static boolean evaluate(Player player, UnlockCondition condition)
    {
        return switch (condition.type())
        {
            case HAS_REVERSE_TECHNIQUE -> PlayerReverseTechniqueData.hasReverseTechnique(player);
            case HAS_COMMON_SKILL -> evaluateCommonSkill(player, condition.arg());
            case MIN_CURSED_RANK -> PlayerCursedGradeData.getRank(player) >= parseInt(condition.arg(), 0);
            case HAS_DOMAIN_EXPANSION -> PlayerDomainExpansionData.hasLearned(player);
            case HAS_MELTDOWN_REPAIR -> PlayerMeltdownRepairData.hasLearned(player);
            case ALWAYS_VISIBLE -> true;
        };
    }

    public static boolean evaluateAll(Player player, Iterable<UnlockCondition> conditions)
    {
        for (UnlockCondition condition : conditions)
        {
            if (!evaluate(player, condition))
            {
                return false;
            }
        }
        return true;
    }

    private static boolean evaluateCommonSkill(Player player, String commonSkillId)
    {
        if (ReverseTechniqueCommonKeySkill.INSTANCE.skillId().equals(commonSkillId))
        {
            return PlayerReverseTechniqueData.hasReverseTechnique(player);
        }
        if (MeltdownRepairLearnLogic.SKILL_ID.equals(commonSkillId))
        {
            return PlayerMeltdownRepairData.hasLearned(player);
        }
        return false;
    }

    private static int parseInt(String value, int fallback)
    {
        try
        {
            return Integer.parseInt(value);
        }
        catch (NumberFormatException ignored)
        {
            return fallback;
        }
    }
}
