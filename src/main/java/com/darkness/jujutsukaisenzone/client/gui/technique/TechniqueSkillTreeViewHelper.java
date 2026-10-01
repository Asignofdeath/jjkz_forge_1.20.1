package com.darkness.jujutsukaisenzone.client.gui.technique;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.player.BarrierTechniqueLevel;
import com.darkness.jujutsukaisenzone.player.PlayerBarrierTechniqueData;
import com.darkness.jujutsukaisenzone.player.PlayerCursedGradeData;
import com.darkness.jujutsukaisenzone.player.PlayerSeanceTechniqueData;
import com.darkness.jujutsukaisenzone.player.PlayerShrineBlackFlashMilestoneData;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueIntelData;
import com.darkness.jujutsukaisenzone.player.SeanceTechniqueLevel;
import com.darkness.jujutsukaisenzone.technique.MeltdownRepairLearnLogic;
import com.darkness.jujutsukaisenzone.technique.commonskill.ReverseTechniqueCommonKeySkill;
import com.darkness.jujutsukaisenzone.technique.development.SkillTreeNodeDef;
import com.darkness.jujutsukaisenzone.technique.development.SkillTreeReferenceKind;
import com.darkness.jujutsukaisenzone.player.PlayerSpData;
import com.darkness.jujutsukaisenzone.technique.development.MechanicLiberationDef;
import com.darkness.jujutsukaisenzone.technique.development.MechanicLiberationRegistry;
import com.darkness.jujutsukaisenzone.technique.development.SkillLevelRules;
import com.darkness.jujutsukaisenzone.technique.development.SkillUnlockLogic;
import com.darkness.jujutsukaisenzone.technique.development.SkillUpgradeCostRegistry;
import com.darkness.jujutsukaisenzone.technique.development.SkillUpgradeLogic;
import com.darkness.jujutsukaisenzone.technique.skillset.TechniqueSkill;
import com.darkness.jujutsukaisenzone.technique.skillset.TechniqueSpecialMove;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Optional;

@OnlyIn(Dist.CLIENT)
public final class TechniqueSkillTreeViewHelper
{
    private static final ResourceLocation LOCKED_SKILL_ICON = ResourceLocation.fromNamespaceAndPath(
            JuJutsuKaisenZone.MODID, "textures/gui/question_mark.png");

    public enum NodeVisualState
    {
        REFERENCE_SATISFIED,
        REFERENCE_LOCKED,
        INFO,
        SKILL_UNLOCKED,
        SKILL_CAN_UNLOCK,
        SKILL_LOCKED
    }

    private TechniqueSkillTreeViewHelper()
    {
    }

    public static NodeVisualState getVisualState(Player player, String techniqueId, SkillTreeNodeDef node)
    {
        if (node.isInfoNode())
        {
            return SkillUnlockLogic.isInfoNodeVisible(player, node)
                    ? NodeVisualState.INFO
                    : NodeVisualState.SKILL_LOCKED;
        }
        if (node.isReferenceNode())
        {
            return SkillUnlockLogic.isReferenceSatisfied(player, node)
                    ? NodeVisualState.REFERENCE_SATISFIED
                    : NodeVisualState.REFERENCE_LOCKED;
        }
        // 特别招式没有独立解锁：状态完全跟随原技能，故复用技能的已解锁 / 未解锁两态。
        if (node.isSpecialMoveNode())
        {
            return SkillUnlockLogic.isSpecialMoveUnlocked(player, techniqueId, node)
                    ? NodeVisualState.SKILL_UNLOCKED
                    : NodeVisualState.SKILL_LOCKED;
        }

        Optional<TechniqueSkill> skillOpt = SkillUnlockLogic.findSkillDefinition(techniqueId, node.skillId());
        if (skillOpt.isEmpty())
        {
            return NodeVisualState.SKILL_LOCKED;
        }

        if (SkillUnlockLogic.isSkillUnlocked(player, techniqueId, node.skillId(), skillOpt.get()))
        {
            return NodeVisualState.SKILL_UNLOCKED;
        }
        if (SkillUnlockLogic.canUnlockSkill(player, techniqueId, node.nodeId()))
        {
            return NodeVisualState.SKILL_CAN_UNLOCK;
        }
        return NodeVisualState.SKILL_LOCKED;
    }

    public static int getBorderColor(NodeVisualState state)
    {
        return switch (state)
        {
            case REFERENCE_SATISFIED, SKILL_UNLOCKED -> 0xFF55FF55;
            case INFO -> 0xFF88CCFF;
            case SKILL_CAN_UNLOCK -> 0xFFFFFF55;
            case REFERENCE_LOCKED, SKILL_LOCKED -> 0xFF888888;
        };
    }

    /** 特别招式节点用独立的边框色（青蓝）区分于同列的普通技能。 */
    public static int getBorderColor(NodeVisualState state, SkillTreeNodeDef node)
    {
        if (node == null || !node.isSpecialMoveNode())
        {
            return getBorderColor(state);
        }
        return state == NodeVisualState.SKILL_UNLOCKED ? 0xFF66CCFF : 0xFF5A6B7A;
    }

    public static Component getDisplayName(Player player, String techniqueId, SkillTreeNodeDef node)
    {
        if (node.isInfoNode())
        {
            return Component.translatable(node.infoTitleKey());
        }
        if (node.isReferenceNode())
        {
            if (node.referenceKind() == SkillTreeReferenceKind.COMMON_SKILL
                    && ReverseTechniqueCommonKeySkill.INSTANCE.skillId().equals(node.referenceId()))
            {
                return Component.translatable("key.jujutsukaisenzone.reverse_technique");
            }
            if (node.referenceKind() == SkillTreeReferenceKind.COMMON_SKILL
                    && MeltdownRepairLearnLogic.SKILL_ID.equals(node.referenceId()))
            {
                return Component.translatable("gui.jujutsukaisenzone.player_data.meltdown_repair");
            }
            if (node.referenceKind() == SkillTreeReferenceKind.BLACK_FLASH_COUNT)
            {
                int required = SkillUnlockLogic.parseRequiredCount(node.referenceId());
                return Component.literal("打出" + required + "发黑闪");
            }
            if (node.referenceKind() == SkillTreeReferenceKind.SEANCE_TECHNIQUE_LEVEL)
            {
                return Component.literal("降灵术完全掌握");
            }
            if (node.referenceKind() == SkillTreeReferenceKind.BARRIER_TECHNIQUE_LEVEL)
            {
                BarrierTechniqueLevel required = BarrierTechniqueLevel.fromString(node.referenceId());
                return Component.literal("结界术" + getBarrierLevelDisplayName(required));
            }
            if (node.referenceKind() == SkillTreeReferenceKind.CURSED_GRADE)
            {
                int required = SkillUnlockLogic.parseRequiredCount(node.referenceId());
                return Component.literal("咒术等级：" + PlayerCursedGradeData.getRankDisplayName(required));
            }
            return Component.literal(node.nodeId());
        }

        if (node.isSpecialMoveNode())
        {
            if (!SkillUnlockLogic.isSpecialMoveUnlocked(player, techniqueId, node))
            {
                return Component.literal("XXX");
            }
            return getSpecialMoveDisplayName(techniqueId, node);
        }

        if (isSkillHidden(player, techniqueId, node))
        {
            return Component.literal("XXX");
        }

        return SkillUnlockLogic.findSkillDefinition(techniqueId, node.skillId())
                .map(TechniqueSkill::displayName)
                .orElse(Component.literal(node.nodeId()));
    }

    public static ResourceLocation getIconLocation(Player player, String techniqueId, SkillTreeNodeDef node)
    {
        if (node.isInfoNode())
        {
            return node.infoIconPath() != null && !node.infoIconPath().isBlank()
                    ? iconResource(node.infoIconPath())
                    : null;
        }
        if (node.isSpecialMoveNode())
        {
            if (!SkillUnlockLogic.isSpecialMoveUnlocked(player, techniqueId, node))
            {
                return LOCKED_SKILL_ICON;
            }
            TechniqueSpecialMove move = node.specialMove();
            if (move != null && move.hasOwnIcon())
            {
                return iconResource(move.iconPath());
            }
            return baseSkillIcon(techniqueId, node);
        }
        if (!node.isSkillNode())
        {
            return null;
        }
        if (isSkillHidden(player, techniqueId, node))
        {
            return LOCKED_SKILL_ICON;
        }
        return SkillUnlockLogic.findSkillDefinition(techniqueId, node.skillId())
                .map(skill -> iconResource(skill.iconPath()))
                .orElse(null);
    }

    /** 特别招式的默认图标：原技能的图标。 */
    private static ResourceLocation baseSkillIcon(String techniqueId, SkillTreeNodeDef node)
    {
        String baseSkillId = node.baseSkillId();
        if (baseSkillId == null)
        {
            return null;
        }
        return SkillUnlockLogic.findSkillDefinition(techniqueId, baseSkillId)
                .map(skill -> iconResource(skill.iconPath()))
                .orElse(null);
    }

    /** 特别招式的展示名：优先用声明中的名字，否则「原技能名（Shift）」。 */
    private static Component getSpecialMoveDisplayName(String techniqueId, SkillTreeNodeDef node)
    {
        TechniqueSpecialMove move = node.specialMove();
        if (move == null)
        {
            return Component.literal(node.nodeId());
        }
        if (move.displayName() != null)
        {
            return move.displayName();
        }
        return baseSkillDisplayName(techniqueId, move.baseSkillId())
                .copy()
                .append(Component.literal("（" + move.modifierLabel() + "）"));
    }

    /** 原技能的展示名（特别招式的名称 / 提示文案会引用它）。 */
    public static Component baseSkillDisplayName(String techniqueId, String baseSkillId)
    {
        return SkillUnlockLogic.findSkillDefinition(techniqueId, baseSkillId)
                .map(TechniqueSkill::displayName)
                .orElse(Component.literal(baseSkillId));
    }

    private static boolean isSkillHidden(Player player, String techniqueId, SkillTreeNodeDef node)
    {
        if (player == null || node.isReferenceNode())
        {
            return false;
        }
        return SkillUnlockLogic.findSkillDefinition(techniqueId, node.skillId())
                .map(skill -> !SkillUnlockLogic.isSkillUnlocked(player, techniqueId, node.skillId(), skill))
                .orElse(true);
    }

    public static String getSubtitle(Player player, String techniqueId, SkillTreeNodeDef node, NodeVisualState state)
    {
        if (node.isInfoNode())
        {
            return Component.translatable("gui.jujutsukaisenzone.skill_tree.info").getString();
        }
        if (node.isReferenceNode())
        {
            if (node.referenceKind() == SkillTreeReferenceKind.BLACK_FLASH_COUNT)
            {
                int required = SkillUnlockLogic.parseRequiredCount(node.referenceId());
                if (state == NodeVisualState.REFERENCE_SATISFIED)
                {
                    return "已达成";
                }
                return PlayerShrineBlackFlashMilestoneData.getCount(player) + "/" + required;
            }
            if (node.referenceKind() == SkillTreeReferenceKind.SEANCE_TECHNIQUE_LEVEL)
            {
                if (state == NodeVisualState.REFERENCE_SATISFIED)
                {
                    return "已达成";
                }
                SeanceTechniqueLevel required = SeanceTechniqueLevel.fromString(node.referenceId());
                return getSeanceLevelDisplayName(PlayerSeanceTechniqueData.getLevel(player))
                        + " / " + getSeanceLevelDisplayName(required);
            }
            if (node.referenceKind() == SkillTreeReferenceKind.BARRIER_TECHNIQUE_LEVEL)
            {
                if (state == NodeVisualState.REFERENCE_SATISFIED)
                {
                    return "已达成";
                }
                BarrierTechniqueLevel required = BarrierTechniqueLevel.fromString(node.referenceId());
                return getBarrierLevelDisplayName(PlayerBarrierTechniqueData.getLevel(player))
                        + " / " + getBarrierLevelDisplayName(required);
            }
            if (node.referenceKind() == SkillTreeReferenceKind.CURSED_GRADE)
            {
                int required = SkillUnlockLogic.parseRequiredCount(node.referenceId());
                if (state == NodeVisualState.REFERENCE_SATISFIED)
                {
                    return "已达成";
                }
                return PlayerCursedGradeData.getRankDisplayName(PlayerCursedGradeData.getRank(player))
                        + " / " + PlayerCursedGradeData.getRankDisplayName(required);
            }
            return state == NodeVisualState.REFERENCE_SATISFIED ? "已习得" : "未习得";
        }

        if (node.isSpecialMoveNode())
        {
            TechniqueSpecialMove move = node.specialMove();
            if (move == null)
            {
                return "";
            }
            if (state == NodeVisualState.SKILL_UNLOCKED)
            {
                return Component.translatable(
                        "gui.jujutsukaisenzone.skill_tree.special_move_trigger",
                        move.modifierLabel()).getString();
            }
            return Component.translatable(
                    "gui.jujutsukaisenzone.skill_tree.special_move_follows",
                    baseSkillDisplayName(techniqueId, move.baseSkillId())).getString();
        }

        return switch (state)
        {
            case SKILL_UNLOCKED -> getUnlockedSubtitle(player, techniqueId, node);
            case SKILL_CAN_UNLOCK -> getCanUnlockSubtitle(player, techniqueId, node);
            case SKILL_LOCKED -> getLockedSubtitle(player, techniqueId, node);
            default -> "";
        };
    }

    private static String getUnlockedSubtitle(Player player, String techniqueId, SkillTreeNodeDef node)
    {
        if (SkillUnlockLogic.isMechanicSkill(techniqueId, node.skillId()))
        {
            return "已解放";
        }
        if (!SkillUpgradeCostRegistry.isUpgradeable(node.skillId()))
        {
            return "已解锁";
        }
        int level = SkillUpgradeLogic.getSkillLevel(player, techniqueId, node.skillId());
        if (SkillLevelRules.isMaxLevel(level))
        {
            return "Lv." + level + " 满级";
        }
        return "Lv." + level + " · 右键升级";
    }

    private static String getCanUnlockSubtitle(Player player, String techniqueId, SkillTreeNodeDef node)
    {
        var liberation = MechanicLiberationRegistry.find(node.skillId());
        if (liberation.isPresent())
        {
            return "可解放 · " + liberation.get().spCost() + " SP";
        }
        if (node.vanillaXpLevelCost() > 0)
        {
            return Component.translatable(
                    "gui.jujutsukaisenzone.skill_tree.can_unlock_xp",
                    node.vanillaXpLevelCost()).getString();
        }
        return "可解锁 · " + node.intelCost() + " 情报";
    }

    private static String getLockedSubtitle(Player player, String techniqueId, SkillTreeNodeDef node)
    {
        var liberation = MechanicLiberationRegistry.find(node.skillId());
        if (liberation.isPresent())
        {
            MechanicLiberationDef def = liberation.get();
            int activeLevel = SkillUpgradeLogic.getSkillLevel(player, techniqueId, def.requiredActiveSkillId());
            int sp = PlayerSpData.getAvailable(player);
            if (!SkillLevelRules.isMaxLevel(activeLevel))
            {
                return "需关联主动满级（" + activeLevel + "/" + SkillLevelRules.MAX_LEVEL + "）";
            }
            return "需 " + def.spCost() + " SP（" + sp + "）";
        }
        if (node.vanillaXpLevelCost() > 0)
        {
            return Component.translatable(
                    "gui.jujutsukaisenzone.skill_tree.need_xp",
                    node.vanillaXpLevelCost(),
                    player.experienceLevel).getString();
        }
        int have = PlayerTechniqueIntelData.getIntel(player, techniqueId);
        return "需 " + node.intelCost() + " 情报（" + have + "）";
    }

    public static ResourceLocation iconResource(String path)
    {
        if (path == null || path.isBlank())
        {
            return null;
        }
        if (path.contains(":"))
        {
            return ResourceLocation.parse(path);
        }
        return ResourceLocation.fromNamespaceAndPath(JuJutsuKaisenZone.MODID, path);
    }

    public static Player localPlayer()
    {
        return Minecraft.getInstance().player;
    }

    private static String getSeanceLevelDisplayName(SeanceTechniqueLevel level)
    {
        return switch (level)
        {
            case UNLEARNED -> "未习得";
            case BEGINNER -> "初习";
            case MASTERED -> "掌握";
            case COMPLETE -> "完全掌握";
        };
    }

    private static String getBarrierLevelDisplayName(BarrierTechniqueLevel level)
    {
        return switch (level)
        {
            case UNLEARNED -> "未习得";
            case BEGINNER -> "初习";
            case MASTERED -> "掌握";
            case COMPLETE -> "完全掌握";
        };
    }
}
