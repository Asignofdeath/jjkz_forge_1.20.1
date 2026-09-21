package com.darkness.jujutsukaisenzone.technique.development;

import net.minecraft.world.entity.player.Player;

/**
 * 单个解锁条件；与 {@link SkillTreeNodeDef#prerequisiteNodeIds()} 为 AND 关系。
 */
public record UnlockCondition(UnlockConditionType type, String arg)
{
    public static UnlockCondition hasReverseTechnique()
    {
        return new UnlockCondition(UnlockConditionType.HAS_REVERSE_TECHNIQUE, "");
    }

    public static UnlockCondition hasCommonSkill(String commonSkillId)
    {
        return new UnlockCondition(UnlockConditionType.HAS_COMMON_SKILL, commonSkillId);
    }

    public static UnlockCondition minCursedRank(int rank)
    {
        return new UnlockCondition(UnlockConditionType.MIN_CURSED_RANK, Integer.toString(rank));
    }

    public static UnlockCondition hasDomainExpansion()
    {
        return new UnlockCondition(UnlockConditionType.HAS_DOMAIN_EXPANSION, "");
    }

    public static UnlockCondition alwaysVisible()
    {
        return new UnlockCondition(UnlockConditionType.ALWAYS_VISIBLE, "");
    }

    public boolean evaluate(Player player)
    {
        return UnlockConditionEvaluator.evaluate(player, this);
    }
}
