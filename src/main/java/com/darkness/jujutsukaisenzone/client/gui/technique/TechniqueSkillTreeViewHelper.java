package com.darkness.jujutsukaisenzone.client.gui.technique;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.player.PlayerCursedGradeData;
import com.darkness.jujutsukaisenzone.player.PlayerSeanceTechniqueData;
import com.darkness.jujutsukaisenzone.player.PlayerShrineBlackFlashMilestoneData;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueIntelData;
import com.darkness.jujutsukaisenzone.player.SeanceTechniqueLevel;
import com.darkness.jujutsukaisenzone.technique.commonskill.ReverseTechniqueCommonKeySkill;
import com.darkness.jujutsukaisenzone.technique.development.SkillTreeNodeDef;
import com.darkness.jujutsukaisenzone.technique.development.SkillTreeReferenceKind;
import com.darkness.jujutsukaisenzone.technique.development.SkillUnlockLogic;
import com.darkness.jujutsukaisenzone.technique.skillset.TechniqueSkill;
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
        SKILL_UNLOCKED,
        SKILL_CAN_UNLOCK,
        SKILL_LOCKED
    }

    private TechniqueSkillTreeViewHelper()
    {
    }

    public static NodeVisualState getVisualState(Player player, String techniqueId, SkillTreeNodeDef node)
    {
        if (node.isReferenceNode())
        {
            return SkillUnlockLogic.isReferenceSatisfied(player, node)
                    ? NodeVisualState.REFERENCE_SATISFIED
                    : NodeVisualState.REFERENCE_LOCKED;
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
            case SKILL_CAN_UNLOCK -> 0xFFFFFF55;
            case REFERENCE_LOCKED, SKILL_LOCKED -> 0xFF888888;
        };
    }

    public static Component getDisplayName(Player player, String techniqueId, SkillTreeNodeDef node)
    {
        if (node.isReferenceNode())
        {
            if (node.referenceKind() == SkillTreeReferenceKind.COMMON_SKILL
                    && ReverseTechniqueCommonKeySkill.INSTANCE.skillId().equals(node.referenceId()))
            {
                return Component.translatable("key.jujutsukaisenzone.reverse_technique");
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
            if (node.referenceKind() == SkillTreeReferenceKind.CURSED_GRADE)
            {
                int required = SkillUnlockLogic.parseRequiredCount(node.referenceId());
                return Component.literal("咒术等级：" + PlayerCursedGradeData.getRankDisplayName(required));
            }
            return Component.literal(node.nodeId());
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

        return switch (state)
        {
            case SKILL_UNLOCKED -> "已解锁";
            case SKILL_CAN_UNLOCK -> "可解锁 · " + node.intelCost() + " 情报";
            case SKILL_LOCKED -> {
                int have = PlayerTechniqueIntelData.getIntel(player, techniqueId);
                yield "需 " + node.intelCost() + " 情报（" + have + "）";
            }
            default -> "";
        };
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
}
