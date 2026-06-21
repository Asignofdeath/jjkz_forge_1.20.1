package com.darkness.jujutsukaisenzone.technique.development;

import com.darkness.jujutsukaisenzone.network.ModNetwork;
import com.darkness.jujutsukaisenzone.player.PlayerCursedGradeData;
import com.darkness.jujutsukaisenzone.player.PlayerSeanceTechniqueData;
import com.darkness.jujutsukaisenzone.player.PlayerShrineBlackFlashMilestoneData;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueIntelData;
import com.darkness.jujutsukaisenzone.player.SeanceTechniqueLevel;
import com.darkness.jujutsukaisenzone.technique.skillset.IInnateTechniqueSkillSet;
import com.darkness.jujutsukaisenzone.technique.commonskill.ReverseTechniqueCommonKeySkill;
import com.darkness.jujutsukaisenzone.technique.skillset.TechniqueSkill;
import com.darkness.jujutsukaisenzone.technique.skillset.TechniqueSkillSetRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 技能解锁图运行时逻辑：前置、条件、术式情报门槛与 unlockTag 读写。
 */
public final class SkillUnlockLogic
{
    private SkillUnlockLogic()
    {
    }

    public static boolean isNodeRequirementMet(Player player, String techniqueId, SkillTreeNodeDef node)
    {
        if (node.isReferenceNode())
        {
            return isReferenceSatisfied(player, node);
        }
        return isSkillNodeUnlocked(player, techniqueId, node);
    }

    public static boolean isReferenceSatisfied(Player player, SkillTreeNodeDef node)
    {
        if (!node.isReferenceNode())
        {
            return false;
        }
        if (node.referenceKind() == SkillTreeReferenceKind.COMMON_SKILL
                && ReverseTechniqueCommonKeySkill.INSTANCE.skillId().equals(node.referenceId()))
        {
            return UnlockConditionEvaluator.evaluate(player, UnlockCondition.hasReverseTechnique());
        }
        if (node.referenceKind() == SkillTreeReferenceKind.BLACK_FLASH_COUNT)
        {
            return PlayerShrineBlackFlashMilestoneData.isMilestoneMet(player, parseRequiredCount(node.referenceId()));
        }
        if (node.referenceKind() == SkillTreeReferenceKind.SEANCE_TECHNIQUE_LEVEL)
        {
            return isSeanceTechniqueLevelMet(player, node.referenceId());
        }
        if (node.referenceKind() == SkillTreeReferenceKind.CURSED_GRADE)
        {
            return PlayerCursedGradeData.getRank(player) >= parseRequiredCount(node.referenceId());
        }
        return UnlockConditionEvaluator.evaluate(player, UnlockCondition.hasCommonSkill(node.referenceId()));
    }

    public static int parseRequiredCount(String referenceId)
    {
        if (referenceId == null || referenceId.isBlank())
        {
            return 0;
        }
        try
        {
            return Math.max(0, Integer.parseInt(referenceId.trim()));
        }
        catch (NumberFormatException ignored)
        {
            return 0;
        }
    }

    public static boolean isSkillUnlocked(Player player, String techniqueId, String skillId, TechniqueSkill skill)
    {
        Optional<TechniqueSkillTree> treeOpt = TechniqueSkillTreeRegistry.get(techniqueId);
        if (treeOpt.isEmpty())
        {
            return legacyUnlocked(player, skill);
        }

        Optional<SkillTreeNodeDef> nodeOpt = treeOpt.get().findSkillNode(skillId);
        if (nodeOpt.isEmpty())
        {
            return legacyUnlocked(player, skill);
        }
        return isSkillNodeUnlocked(player, techniqueId, nodeOpt.get());
    }

    public static boolean canUnlockSkill(Player player, String techniqueId, String nodeId)
    {
        Optional<TechniqueSkillTree> treeOpt = TechniqueSkillTreeRegistry.get(techniqueId);
        if (treeOpt.isEmpty())
        {
            return false;
        }
        SkillTreeNodeDef node = treeOpt.get().findNode(nodeId).orElse(null);
        if (node == null || !node.isSkillNode())
        {
            return false;
        }
        if (isSkillNodeUnlocked(player, techniqueId, node))
        {
            return false;
        }
        if (!arePrerequisitesMet(player, techniqueId, node))
        {
            return false;
        }
        if (!UnlockConditionEvaluator.evaluateAll(player, node.unlockConditions()))
        {
            return false;
        }
        return PlayerTechniqueIntelData.getIntel(player, techniqueId) >= node.intelCost();
    }

    public static SkillUnlockResult tryUnlockSkill(ServerPlayer player, String techniqueId, String nodeId)
    {
        if (!techniqueId.equals(PlayerTechniqueData.getInnateTechnique(player)))
        {
            return SkillUnlockResult.WRONG_TECHNIQUE;
        }

        Optional<TechniqueSkillTree> treeOpt = TechniqueSkillTreeRegistry.get(techniqueId);
        if (treeOpt.isEmpty())
        {
            return SkillUnlockResult.UNKNOWN_NODE;
        }

        SkillTreeNodeDef node = treeOpt.get().findNode(nodeId).orElse(null);
        if (node == null)
        {
            return SkillUnlockResult.UNKNOWN_NODE;
        }
        if (!node.isSkillNode())
        {
            return SkillUnlockResult.NOT_SKILL_NODE;
        }
        if (isSkillNodeUnlocked(player, techniqueId, node))
        {
            return SkillUnlockResult.ALREADY_UNLOCKED;
        }
        if (!arePrerequisitesMet(player, techniqueId, node))
        {
            return SkillUnlockResult.MISSING_PREREQUISITES;
        }
        if (!UnlockConditionEvaluator.evaluateAll(player, node.unlockConditions()))
        {
            return SkillUnlockResult.CONDITIONS_NOT_MET;
        }
        if (PlayerTechniqueIntelData.getIntel(player, techniqueId) < node.intelCost())
        {
            return SkillUnlockResult.INSUFFICIENT_INTEL;
        }

        setSkillUnlocked(player, techniqueId, node, true);
        return SkillUnlockResult.SUCCESS;
    }

    public static Optional<TechniqueSkill> findSkillDefinition(String techniqueId, String skillId)
    {
        return TechniqueSkillSetRegistry.get(techniqueId)
                .flatMap(set -> set.getConfigurableSkills().stream()
                        .filter(skill -> skill.skillId().equals(skillId))
                        .findFirst());
    }

    /** 当前可拖入快捷栏、可释放的已解锁技能列表。 */
    public static List<TechniqueSkill> getUnlockedConfigurableSkills(Player player, String techniqueId)
    {
        if (techniqueId == null || techniqueId.isBlank())
        {
            return Collections.emptyList();
        }
        return TechniqueSkillSetRegistry.get(techniqueId)
                .map(set -> set.getConfigurableSkills().stream()
                        .filter(skill -> isSkillBarEligible(player, techniqueId, skill, set))
                        .toList())
                .orElse(Collections.emptyList());
    }

    /** 已解锁且满足快捷栏展示条件的技能。 */
    public static boolean isSkillBarEligible(
            Player player,
            String techniqueId,
            TechniqueSkill skill,
            IInnateTechniqueSkillSet skillSet)
    {
        return isSkillUnlocked(player, techniqueId, skill.skillId(), skill)
                && skillSet.isSkillVisibleInSkillBar(player, skill);
    }

    /** 读取服务端已解锁技能的 unlockTag 快照，供登录同步到客户端。 */
    public static Map<String, Boolean> snapshotUnlockTags(ServerPlayer player)
    {
        Map<String, Boolean> result = new LinkedHashMap<>();
        CompoundTag data = player.getPersistentData();
        for (String key : data.getAllKeys())
        {
            if (key.startsWith("jujutsukaisenzone_") && key.contains("_unlock_") && data.getBoolean(key))
            {
                result.put(key, true);
            }
        }
        return result;
    }

    /** 将服务端下发的 unlockTag 写入客户端 persistentData。 */
    public static void applyClientUnlockTags(Player player, Map<String, Boolean> unlockTags)
    {
        for (Map.Entry<String, Boolean> entry : unlockTags.entrySet())
        {
            if (Boolean.TRUE.equals(entry.getValue()))
            {
                player.getPersistentData().putBoolean(entry.getKey(), true);
            }
        }
    }

    public static void syncToClient(ServerPlayer player)
    {
        ModNetwork.sendSkillUnlocksSync(player, snapshotUnlockTags(player));
    }

    /** 快捷栏槽位仅允许已解锁技能；否则返回空串。 */
    public static String sanitizeSkillBarSlot(Player player, String techniqueId, String skillId)
    {
        if (skillId == null || skillId.isBlank())
        {
            return "";
        }
        return TechniqueSkillSetRegistry.get(techniqueId)
                .flatMap(set -> findSkillDefinition(techniqueId, skillId)
                        .filter(skill -> isSkillBarEligible(player, techniqueId, skill, set))
                        .map(TechniqueSkill::skillId))
                .orElse("");
    }

    private static boolean isSeanceTechniqueLevelMet(Player player, String referenceId)
    {
        SeanceTechniqueLevel required = SeanceTechniqueLevel.fromString(referenceId);
        if (required == SeanceTechniqueLevel.UNLEARNED)
        {
            return false;
        }
        return PlayerSeanceTechniqueData.getLevel(player).getTier() >= required.getTier();
    }

    private static boolean isSkillNodeUnlocked(Player player, String techniqueId, SkillTreeNodeDef node)
    {
        if (node.autoGranted())
        {
            return PlayerTechniqueData.getInnateTechnique(player).equals(techniqueId);
        }
        Optional<TechniqueSkill> skillOpt = findSkillDefinition(techniqueId, node.skillId());
        if (skillOpt.isEmpty())
        {
            return false;
        }
        return readUnlockTag(player, skillOpt.get());
    }

    private static boolean arePrerequisitesMet(Player player, String techniqueId, SkillTreeNodeDef node)
    {
        for (String prereqId : node.prerequisiteNodeIds())
        {
            Optional<SkillTreeNodeDef> prereqOpt = TechniqueSkillTreeRegistry.get(techniqueId).flatMap(tree -> tree.findNode(prereqId));
            if (prereqOpt.isEmpty() || !isNodeRequirementMet(player, techniqueId, prereqOpt.get()))
            {
                return false;
            }
        }
        return true;
    }

    private static void setSkillUnlocked(Player player, String techniqueId, SkillTreeNodeDef node, boolean unlocked)
    {
        findSkillDefinition(techniqueId, node.skillId())
                .ifPresent(skill -> writeUnlockTag(player, skill, unlocked));
    }

    private static boolean readUnlockTag(Player player, TechniqueSkill skill)
    {
        String key = skill.unlockTag();
        if (key == null || key.isEmpty())
        {
            return true;
        }
        return player.getPersistentData().getBoolean(key);
    }

    private static void writeUnlockTag(Player player, TechniqueSkill skill, boolean unlocked)
    {
        String key = skill.unlockTag();
        if (key == null || key.isEmpty())
        {
            return;
        }
        player.getPersistentData().putBoolean(key, unlocked);
    }

    /** 无技能树定义时沿用旧逻辑：缺 tag 视为已解锁。 */
    private static boolean legacyUnlocked(Player player, TechniqueSkill skill)
    {
        String key = skill.unlockTag();
        if (key == null || key.isEmpty())
        {
            return true;
        }
        return !player.getPersistentData().contains(key) || player.getPersistentData().getBoolean(key);
    }
}
