package com.darkness.jujutsukaisenzone.client;

import com.darkness.jujutsukaisenzone.technique.aerial.AerialCombatSkills;
import com.darkness.jujutsukaisenzone.technique.description.TechniqueSkillDescriptionKeys;
import com.darkness.jujutsukaisenzone.technique.description.TechniqueSkillDescriptionView;
import com.darkness.jujutsukaisenzone.technique.description.TechniqueSkillDescriptions;
import com.darkness.jujutsukaisenzone.technique.development.SkillUnlockLogic;
import com.darkness.jujutsukaisenzone.technique.skillset.TechniqueSkill;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 术式技能悬停描述框：将 {@link TechniqueSkillDescriptionView} 格式化为多行 tooltip。
 * <p>
 * 阶段 3/4 的快捷栏配置界面与术式开发界面应统一调用此类。
 */
@OnlyIn(Dist.CLIENT)
public final class TechniqueSkillTooltipHelper
{
    private TechniqueSkillTooltipHelper()
    {
    }

    public static List<Component> build(Player player, String techniqueId, TechniqueSkill skill)
    {
        if (AerialCombatSkills.SKILL_CURSE_LEVITATION.equals(skill.skillId()))
        {
            return List.of(
                    skill.displayName().copy().withStyle(ChatFormatting.GOLD),
                    Component.translatable("skill.jujutsukaisenzone.curse_levitation.desc")
                            .withStyle(ChatFormatting.DARK_GRAY));
        }
        if (AerialCombatSkills.SKILL_AERIAL_MULTI_JUMP.equals(skill.skillId()))
        {
            return List.of(
                    skill.displayName().copy().withStyle(ChatFormatting.GOLD),
                    Component.translatable(
                                    "skill.jujutsukaisenzone.aerial_multi_jump.desc",
                                    com.darkness.jujutsukaisenzone.compat.EpicFightCompat.AERIAL_MULTI_JUMP_STAMINA_COST)
                            .withStyle(ChatFormatting.DARK_GRAY));
        }
        TechniqueSkillDescriptionView view = TechniqueSkillDescriptions.resolve(player, techniqueId, skill);
        boolean mechanic = SkillUnlockLogic.isMechanicSkill(techniqueId, skill.skillId());
        /*
         * 悬停框有两页：简洁页是默认视图，完整页靠按住 Shift 唤出。
         *
         * 「有没有简洁页」只看 brief 键在不在——键缺失时 translatable 会把键名原样返回，
         * 此时直接给完整页，免得悬停框里冒出一串键名。这样「哪些技能有简洁页」由 lang 决定，
         * 不必在这里维护一份名单：新技能写好 .brief 就自动生效。
         */
        if (!Screen.hasShiftDown() && hasBrief(skill.skillId()))
        {
            return buildBrief(view, skill.skillId(), mechanic);
        }
        return mechanic ? buildMechanic(view) : build(view);
    }

    /**
     * 该技能有没有简洁页文案。
     * <p>
     * {@code .brief} 键缺失时 {@code translatable} 会把键名原样返回，以此判断。
     */
    private static boolean hasBrief(String skillId)
    {
        String key = TechniqueSkillDescriptionKeys.brief(skillId);
        String text = Component.translatable(key).getString();
        return !key.equals(text) && !text.isBlank();
    }

    /**
     * 技能的简洁页：<b>不出现任何数值</b>，只有几句叙事，供玩家一眼看清「这个技能是什么」。
     * <p>
     * 它是悬停框的默认视图；按住 Shift 换到完整页（见 {@link #build(Player, String, TechniqueSkill)}）。
     * 机制型技能的浅紫类型标注在这里照旧保留，与完整页一致。
     */
    private static List<Component> buildBrief(TechniqueSkillDescriptionView view, String skillId, boolean mechanic)
    {
        if (view == null || view.displayName().getString().isEmpty())
        {
            return Collections.emptyList();
        }
        List<Component> lines = new ArrayList<>(5);
        lines.add(view.displayName().copy().withStyle(ChatFormatting.GOLD));
        if (mechanic)
        {
            lines.add(Component.translatable(TechniqueSkillDescriptionKeys.MECHANIC_SKILL_TAG)
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        lines.add(Component.translatable(TechniqueSkillDescriptionKeys.SHIFT_FOR_DETAILS)
                .withStyle(ChatFormatting.DARK_GRAY));
        lines.add(Component.empty());
        lines.add(Component.translatable(TechniqueSkillDescriptionKeys.brief(skillId))
                .withStyle(ChatFormatting.GRAY));
        return Collections.unmodifiableList(lines);
    }

    /**
     * 机制型技能的悬停框：<b>只留名字、类型标注与描述</b>。
     * <p>
     * 它不占槽位、没有咒力也不吃冷却，走通用 {@link #build(TechniqueSkillDescriptionView)} 会多出
     * 「咒力消耗：0」「冷却：0 秒」两行噪声。空战能力（滞空 / 多段跳）当初就是为此在本类里逐个特判掉的，
     * 这里改成按「是不是机制」判定，以后新增机制型技能不必再加分支。
     * <p>
     * 标注行「机制型技能」与特别招式的 {@code special_move_tag} 同一形态（见
     * {@code SkillTreeNodeWidget#buildSpecialMoveTooltip}）：标题之下、正文之上，同为浅紫。
     */
    private static List<Component> buildMechanic(TechniqueSkillDescriptionView view)
    {
        if (view == null || view.displayName().getString().isEmpty())
        {
            return Collections.emptyList();
        }
        List<Component> lines = new ArrayList<>(4);
        lines.add(view.displayName().copy().withStyle(ChatFormatting.GOLD));
        lines.add(Component.translatable(TechniqueSkillDescriptionKeys.MECHANIC_SKILL_TAG)
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        if (!view.description().getString().isBlank())
        {
            lines.add(Component.empty());
            lines.add(view.description().copy().withStyle(ChatFormatting.DARK_GRAY));
        }
        return Collections.unmodifiableList(lines);
    }

    public static List<Component> build(TechniqueSkillDescriptionView view)
    {
        if (view == null || view.displayName().getString().isEmpty())
        {
            return Collections.emptyList();
        }

        List<Component> lines = new ArrayList<>(4);
        lines.add(view.displayName().copy().withStyle(ChatFormatting.GOLD));
        if (view.maintainSkill())
        {
            lines.add(Component.translatable(
                            TechniqueSkillDescriptionKeys.MAINTAIN_COST,
                            view.cursedEnergyCost())
                    .withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable(TechniqueSkillDescriptionKeys.TOGGLE)
                    .withStyle(ChatFormatting.GRAY));
        }
        else
        {
            lines.add(Component.translatable(
                            TechniqueSkillDescriptionKeys.COST,
                            view.cursedEnergyCost())
                    .withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable(
                            TechniqueSkillDescriptionKeys.COOLDOWN,
                            view.cooldownSeconds())
                    .withStyle(ChatFormatting.GRAY));
        }
        if (!view.description().getString().isBlank())
        {
            lines.add(Component.empty());
            lines.add(view.description().copy().withStyle(ChatFormatting.DARK_GRAY));
        }
        return Collections.unmodifiableList(lines);
    }
}
