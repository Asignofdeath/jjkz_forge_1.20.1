package com.darkness.jujutsukaisenzone.technique.development;

import com.darkness.jujutsukaisenzone.network.ModNetwork;
import com.darkness.jujutsukaisenzone.player.PlayerCursedGradeData;
import com.darkness.jujutsukaisenzone.player.PlayerSeanceTechniqueData;
import com.darkness.jujutsukaisenzone.player.PlayerShrineBlackFlashMilestoneData;
import com.darkness.jujutsukaisenzone.player.PlayerSpData;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueIntelData;
import com.darkness.jujutsukaisenzone.player.SeanceTechniqueLevel;
import com.darkness.jujutsukaisenzone.technique.aerial.AerialCombatSkillRegistry;
import com.darkness.jujutsukaisenzone.technique.aerial.AerialCombatSkills;
import com.darkness.jujutsukaisenzone.technique.commonskill.CommonTechniqueSkillRegistry;
import com.darkness.jujutsukaisenzone.technique.commonskill.ReverseTechniqueCommonKeySkill;
import com.darkness.jujutsukaisenzone.technique.skillset.IInnateTechniqueSkillSet;
import com.darkness.jujutsukaisenzone.technique.skillset.TechniqueSkill;
import com.darkness.jujutsukaisenzone.technique.skillset.TechniqueSkillSetRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * 技能解锁图运行时逻辑：前置、条件、术式情报门槛与 unlockTag 读写。
 */
public final class SkillUnlockLogic
{
    private static final String MOD_TAG_PREFIX = "jujutsukaisenzone_";
    /** 解锁 key 的中缀：{@code *_unlock_slot_*}、{@code *_unlock_variant_shift_slot_*}，空战能力则以 {@code _unlock} 结尾。 */
    private static final String UNLOCK_TAG_INFIX = "_unlock";
    /** 等级 key 的中缀：{@code *_level_slot_*}。 */
    private static final String LEVEL_TAG_INFIX = "_level_";

    private SkillUnlockLogic()
    {
    }

    public static boolean isNodeRequirementMet(Player player, String techniqueId, SkillTreeNodeDef node)
    {
        if (node.isReferenceNode())
        {
            return isReferenceSatisfied(player, node);
        }
        if (node.isInfoNode())
        {
            return isInfoNodeVisible(player, node);
        }
        if (node.isSpecialMoveNode())
        {
            return isSpecialMoveUnlocked(player, techniqueId, node);
        }
        return isSkillNodeUnlocked(player, techniqueId, node);
    }

    /**
     * 特别招式节点是否已解锁。
     * <p>
     * 特别招式**没有自己的基础字段**，解锁状态直接继承它所依附的原技能：原技能解锁 = 变体可用。
     * 因此开发树里这些节点只做展示，不能单独消耗术式情报解锁
     * （{@link #canUnlockSkill} / {@link #tryUnlockSkill} 对它们一律拒绝）。
     */
    public static boolean isSpecialMoveUnlocked(Player player, String techniqueId, SkillTreeNodeDef node)
    {
        if (!node.isSpecialMoveNode())
        {
            return false;
        }
        return findBaseSkillNode(techniqueId, node)
                .map(base -> isSkillNodeUnlocked(player, techniqueId, base))
                .orElse(false);
    }

    /** 特别招式所依附的原技能节点。 */
    public static Optional<SkillTreeNodeDef> findBaseSkillNode(String techniqueId, SkillTreeNodeDef node)
    {
        if (!node.isSpecialMoveNode() || node.specialMove() == null)
        {
            return Optional.empty();
        }
        return TechniqueSkillTreeRegistry.get(techniqueId)
                .flatMap(tree -> tree.findSkillNode(node.specialMove().baseSkillId()));
    }

    /** 说明节点是否满足出现条件。 */
    public static boolean isInfoNodeVisible(Player player, SkillTreeNodeDef node)
    {
        if (!node.isInfoNode())
        {
            return false;
        }
        return UnlockConditionEvaluator.evaluateAll(player, node.visibilityConditions());
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
        if (CommonTechniqueSkillRegistry.isRegistered(skillId))
        {
            return CommonTechniqueSkillRegistry.isUnlocked(player, skillId);
        }

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
        if (MechanicLiberationRegistry.isLiberationMechanic(node.skillId()))
        {
            return canLiberateMechanic(player, techniqueId, node.skillId());
        }
        if (node.vanillaXpLevelCost() > 0)
        {
            return player.experienceLevel >= node.vanillaXpLevelCost();
        }
        return PlayerTechniqueIntelData.getIntel(player, techniqueId) >= node.intelCost();
    }

    public static SkillUnlockResult tryUnlockSkill(ServerPlayer player, String techniqueId, String nodeId)
    {
        if (!AerialCombatSkills.TREE_ID.equals(techniqueId)
                && !techniqueId.equals(PlayerTechniqueData.getInnateTechnique(player)))
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
        if (MechanicLiberationRegistry.isLiberationMechanic(node.skillId()))
        {
            return tryLiberateMechanic(player, techniqueId, node);
        }
        if (node.vanillaXpLevelCost() > 0)
        {
            if (player.experienceLevel < node.vanillaXpLevelCost())
            {
                return SkillUnlockResult.INSUFFICIENT_VANILLA_XP;
            }
        }
        else if (PlayerTechniqueIntelData.getIntel(player, techniqueId) < node.intelCost())
        {
            return SkillUnlockResult.INSUFFICIENT_INTEL;
        }

        if (node.vanillaXpLevelCost() > 0)
        {
            player.giveExperienceLevels(-node.vanillaXpLevelCost());
        }

        setSkillUnlocked(player, techniqueId, node, true);
        return SkillUnlockResult.SUCCESS;
    }

    /**
     * 机制型技能是否满足「关联主动满级 + SP」解放条件（不含前置节点；由 {@link #canUnlockSkill} 先查）。
     */
    public static boolean canLiberateMechanic(Player player, String techniqueId, String mechanicSkillId)
    {
        Optional<MechanicLiberationDef> defOpt = MechanicLiberationRegistry.find(mechanicSkillId);
        if (defOpt.isEmpty())
        {
            return false;
        }
        MechanicLiberationDef def = defOpt.get();
        int activeLevel = SkillUpgradeLogic.getSkillLevel(player, techniqueId, def.requiredActiveSkillId());
        if (!SkillLevelRules.isMaxLevel(activeLevel))
        {
            return false;
        }
        return PlayerSpData.getAvailable(player) >= def.spCost();
    }

    private static SkillUnlockResult tryLiberateMechanic(ServerPlayer player, String techniqueId, SkillTreeNodeDef node)
    {
        Optional<MechanicLiberationDef> defOpt = MechanicLiberationRegistry.find(node.skillId());
        if (defOpt.isEmpty())
        {
            return SkillUnlockResult.UNKNOWN_NODE;
        }
        MechanicLiberationDef def = defOpt.get();
        int activeLevel = SkillUpgradeLogic.getSkillLevel(player, techniqueId, def.requiredActiveSkillId());
        if (!SkillLevelRules.isMaxLevel(activeLevel))
        {
            return SkillUnlockResult.ACTIVE_NOT_MAX_LEVEL;
        }
        if (!PlayerSpData.tryConsumeLiberation(player, def.spCost()))
        {
            return SkillUnlockResult.INSUFFICIENT_SP;
        }
        setSkillUnlocked(player, techniqueId, node, true);
        return SkillUnlockResult.SUCCESS;
    }

    /**
     * 按 skillId 找技能定义，槽位技能优先。
     * <p>
     * 两个来源都要查：{@link IInnateTechniqueSkillSet#getConfigurableSkills()}（占快捷栏的招式）
     * 与 {@link IInnateTechniqueSkillSet#getMechanicSkills()}（不占槽位的机制型技能）。
     * 机制型技能刻意不放进前者——那会让它出现在技能栏配置界面；但开发树节点 -> 技能定义的解析
     * （{@code isSkillNodeUnlocked} / {@code getVisualState} / tooltip）必须能找到它，
     * 所以这里把两者并成一条检索路径。
     */
    public static Optional<TechniqueSkill> findSkillDefinition(String techniqueId, String skillId)
    {
        Optional<TechniqueSkill> aerial = AerialCombatSkillRegistry.find(skillId);
        if (aerial.isPresent())
        {
            return aerial;
        }
        Optional<TechniqueSkill> common = CommonTechniqueSkillRegistry.find(skillId);
        if (common.isPresent())
        {
            return common;
        }
        return TechniqueSkillSetRegistry.get(techniqueId)
                .flatMap(set -> Stream.concat(
                                set.getConfigurableSkills().stream(),
                                set.getMechanicSkills().stream())
                        .filter(skill -> skill.skillId().equals(skillId))
                        .findFirst());
    }

    /**
     * 机制型技能是否已解锁——<b>运行时门控的入口</b>。
     * <p>
     * 与 {@link #isSkillUnlocked} 的差别只在「不需要调用方自己拿到 {@link TechniqueSkill} 实例」：
     * 机制型技能没有槽位、不进技能栏，服务端逻辑（如 {@code StarMarkRules} 的引力弹弓）手里
     * 只有 skillId，因此由这里代为解析。解析不到时返回 {@code false}——把「查不到」当未解锁，
     * 好过当已解锁：写错一个 id 只会让机制不生效，不会让它无条件生效。
     */
    public static boolean isSkillUnlockedById(Player player, String techniqueId, String skillId)
    {
        return findSkillDefinition(techniqueId, skillId)
                .map(skill -> isSkillUnlocked(player, techniqueId, skillId, skill))
                .orElse(false);
    }

    /**
     * skillId 是否属于该术式的机制型技能（{@link IInnateTechniqueSkillSet#getMechanicSkills()}）。
     * <p>
     * 供展示层区分「机制型技能」与普通招式：机制型技能没有咒力与冷却，tooltip 不该出现
     * 「咒力消耗：0 / 冷却：0 秒」两行（见 {@code TechniqueSkillTooltipHelper}）。
     */
    public static boolean isMechanicSkill(String techniqueId, String skillId)
    {
        if (skillId == null || skillId.isBlank())
        {
            return false;
        }
        return TechniqueSkillSetRegistry.get(techniqueId)
                .map(set -> set.getMechanicSkills().stream()
                        .anyMatch(skill -> skill.skillId().equals(skillId)))
                .orElse(false);
    }

    /** 当前可拖入快捷栏、可释放的已解锁技能列表。 */
    public static List<TechniqueSkill> getUnlockedConfigurableSkills(Player player, String techniqueId)
    {
        if (techniqueId == null || techniqueId.isBlank())
        {
            return Collections.emptyList();
        }
        List<TechniqueSkill> result = new ArrayList<>();
        TechniqueSkillSetRegistry.get(techniqueId).ifPresent(set -> set.getConfigurableSkills().stream()
                .filter(skill -> isSkillBarEligible(player, techniqueId, skill, set))
                .forEach(result::add));
        CommonTechniqueSkillRegistry.getConfigurableSkills().stream()
                .filter(skill -> CommonTechniqueSkillRegistry.isUnlocked(player, skill.skillId()))
                .forEach(result::add);
        return result;
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
            if (isUnlockTagKey(key, data) && data.getBoolean(key))
            {
                result.put(key, true);
            }
        }
        return result;
    }

    /**
     * 死亡重生时把技能成长数据从旧玩家搬到新玩家上。
     * <p>
     * Forge 重生走的是 {@code PlayerList#respawn} → new {@code ServerPlayer} → {@code restoreFrom}，
     * 而 {@code restoreFrom} 只搬运 persistentData 的 {@code PlayerPersisted} 子标签（原版注释：
     * Copy over a section of the Entity Data from the old player）——写在<b>根标签</b>上的解锁 / 等级键
     * 一条都不会跟过来。所以必须在 {@code PlayerEvent.Clone} 里手动补一份，否则玩家一死，开发树、
     * 技能栏、空战能力全部退回未解锁。
     * <p>
     * 只搬「技能成长」键（{@link #isUnlockTagKey} / {@link #isLevelTagKey}）：咒力、连击、星域这些
     * 战斗态数据本来就该在死亡时清空，不会顺带搬过去。
     */
    public static void copyProgressionFrom(Player newPlayer, Player originalPlayer)
    {
        CompoundTag oldData = originalPlayer.getPersistentData();
        if (oldData.isEmpty())
        {
            return;
        }
        CompoundTag newData = newPlayer.getPersistentData();
        for (String key : oldData.getAllKeys())
        {
            if (isUnlockTagKey(key, oldData) || isLevelTagKey(key, oldData))
            {
                newData.put(key, oldData.get(key).copy());
            }
        }
    }

    /**
     * 技能解锁 tag：{@code jujutsukaisenzone_*_unlock*} 的布尔叶子。
     * <p>
     * 必须连带判类型：{@code jujutsukaisenzone_love_rendezvous_gate_unlocks}（星域跨序位图）名字里也带
     * {@code _unlock}，但它是 long 数组、且属于一局内的会话进度，不能混进解锁状态里。
     */
    private static boolean isUnlockTagKey(String key, CompoundTag data)
    {
        return key.startsWith(MOD_TAG_PREFIX) && key.contains(UNLOCK_TAG_INFIX)
                && data.contains(key, CompoundTag.TAG_BYTE);
    }

    /** 技能等级 tag：{@code jujutsukaisenzone_*_level_slot_*} 的整型叶子。 */
    private static boolean isLevelTagKey(String key, CompoundTag data)
    {
        return key.startsWith(MOD_TAG_PREFIX) && key.contains(LEVEL_TAG_INFIX)
                && data.contains(key, CompoundTag.TAG_INT);
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
        syncLevelsToClient(player);
    }

    public static Map<String, Integer> snapshotLevelTags(ServerPlayer player)
    {
        Map<String, Integer> result = new LinkedHashMap<>();
        CompoundTag data = player.getPersistentData();
        for (String key : data.getAllKeys())
        {
            if (isLevelTagKey(key, data))
            {
                result.put(key, SkillLevelRules.clampLevel(data.getInt(key)));
            }
        }
        return result;
    }

    public static void applyClientLevelTags(Player player, Map<String, Integer> levelTags)
    {
        for (Map.Entry<String, Integer> entry : levelTags.entrySet())
        {
            if (entry.getKey() == null || entry.getKey().isBlank())
            {
                continue;
            }
            player.getPersistentData().putInt(entry.getKey(), SkillLevelRules.clampLevel(entry.getValue()));
        }
    }

    public static void syncLevelsToClient(ServerPlayer player)
    {
        ModNetwork.sendSkillLevelsSync(player, snapshotLevelTags(player));
    }

    /** 快捷栏槽位仅允许已解锁技能；否则返回空串。 */
    public static String sanitizeSkillBarSlot(Player player, String techniqueId, String skillId)
    {
        if (skillId == null || skillId.isBlank())
        {
            return "";
        }
        if (CommonTechniqueSkillRegistry.isRegistered(skillId))
        {
            return CommonTechniqueSkillRegistry.isUnlocked(player, skillId) ? skillId : "";
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
