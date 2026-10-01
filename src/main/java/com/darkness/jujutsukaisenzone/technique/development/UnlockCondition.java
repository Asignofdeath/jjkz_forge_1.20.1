package com.darkness.jujutsukaisenzone.technique.development;

import com.darkness.jujutsukaisenzone.player.BarrierTechniqueLevel;
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

    public static UnlockCondition hasMeltdownRepair()
    {
        return new UnlockCondition(UnlockConditionType.HAS_MELTDOWN_REPAIR, "");
    }

    public static UnlockCondition alwaysVisible()
    {
        return new UnlockCondition(UnlockConditionType.ALWAYS_VISIBLE, "");
    }

    /**
     * 十影：已调伏指定式神后，说明节点才可见。
     *
     * @param shikigamiId {@code TenShadowsShikigami#id()}，如 {@code divine_dogs}
     */
    public static UnlockCondition hasTamedShikigami(String shikigamiId)
    {
        return new UnlockCondition(UnlockConditionType.HAS_TAMED_SHIKIGAMI, shikigamiId == null ? "" : shikigamiId);
    }

    /** 结界术等级至少达到指定档（含更高等级）。 */
    public static UnlockCondition minBarrierTechniqueLevel(BarrierTechniqueLevel level)
    {
        return new UnlockCondition(
                UnlockConditionType.MIN_BARRIER_TECHNIQUE_LEVEL,
                level == null ? BarrierTechniqueLevel.UNLEARNED.name() : level.name());
    }

    public boolean evaluate(Player player)
    {
        return UnlockConditionEvaluator.evaluate(player, this);
    }
}
