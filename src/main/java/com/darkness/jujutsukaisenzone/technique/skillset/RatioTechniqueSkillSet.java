package com.darkness.jujutsukaisenzone.technique.skillset;

import com.darkness.jujutsukaisenzone.player.PlayerRatioOvertimeData;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import com.darkness.jujutsukaisenzone.technique.development.SkillTreeNodeDef;
import com.darkness.jujutsukaisenzone.technique.development.SkillUnlockLogic;
import com.darkness.jujutsukaisenzone.technique.development.TechniqueSkillTree;
import com.darkness.jujutsukaisenzone.technique.development.UnlockCondition;
import com.darkness.jujutsukaisenzone.technique.skill.RatioAbsolutionLogic;
import com.darkness.jujutsukaisenzone.technique.skill.RatioCollapseLogic;
import com.darkness.jujutsukaisenzone.technique.skill.RatioIchimonjiLogic;
import com.darkness.jujutsukaisenzone.technique.skill.RatioImpactLogic;
import com.darkness.jujutsukaisenzone.technique.skill.RatioSpotLogic;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * 十划咒法招式组。
 */
public final class RatioTechniqueSkillSet extends AbstractInnateTechniqueSkillSet
{
    public static final RatioTechniqueSkillSet INSTANCE = new RatioTechniqueSkillSet();

    public static final String SKILL_RATIO_MARK = "ratio_mark";
    public static final String SKILL_ICHIMONJI = "ratio_ichimonji";
    public static final String SKILL_IMPACT = "ratio_impact";
    public static final String SKILL_COLLAPSE = "ratio_collapse";
    public static final String SKILL_OVERTIME_VOW = "ratio_overtime_vow";
    public static final String SKILL_OVERTIME = "ratio_overtime";
    public static final String SKILL_ABSOLUTION = "ratio_absolution";

    private static final int SLOT_RATIO_MARK = 1;
    private static final int SLOT_ICHIMONJI = 2;
    private static final int SLOT_IMPACT = 3;
    private static final int SLOT_COLLAPSE = 4;
    private static final int SLOT_OVERTIME = 5;
    private static final int SLOT_ABSOLUTION = 6;

    private static final int RATIO_MARK_COST = 25;
    private static final int RATIO_MARK_COOLDOWN_TICKS = 20;
    private static final int ICHIMONJI_COST = 40;
    private static final int ICHIMONJI_COOLDOWN_TICKS = 20 * 6;
    private static final int IMPACT_COST = 30;
    private static final int IMPACT_COOLDOWN_TICKS = 20 * 8;
    private static final int COLLAPSE_COST = 55;
    private static final int COLLAPSE_COOLDOWN_TICKS = 20 * 12;
    private static final int OVERTIME_COST = 10;
    private static final int OVERTIME_COOLDOWN_TICKS = 20 * 30;
    private static final int ABSOLUTION_COST = 80;
    private static final int ABSOLUTION_COOLDOWN_TICKS = 20 * 20;

    private static final String ICON =
            "textures/gui/technique/placeholder_technique_skill.png";
    private static final String TAG_OVERTIME_VOW_UNLOCK =
            "jujutsukaisenzone_ratio_overtime_vow_unlock";

    private static final TechniqueSkill MECHANIC_OVERTIME_VOW = new TechniqueSkill(
            SKILL_OVERTIME_VOW, 0, Component.literal("加班契约"), ICON, 0, 0, 0, "",
            TAG_OVERTIME_VOW_UNLOCK, "");

    private static final List<TechniqueSkill> MECHANICS = List.of(MECHANIC_OVERTIME_VOW);

    private static final List<TechniqueSkill> SKILLS = List.of(
            new TechniqueSkill(
                    SKILL_RATIO_MARK, SLOT_RATIO_MARK, Component.literal("十划"), ICON,
                    RATIO_MARK_COST, RATIO_MARK_COOLDOWN_TICKS, 0,
                    "jujutsukaisenzone_ratio_cooldown_slot_1",
                    "jujutsukaisenzone_ratio_unlock_slot_1",
                    "jujutsukaisenzone_ratio_level_slot_1"),
            new TechniqueSkill(
                    SKILL_ICHIMONJI, SLOT_ICHIMONJI, Component.literal("一文字斩"), ICON,
                    ICHIMONJI_COST, ICHIMONJI_COOLDOWN_TICKS, 0,
                    "jujutsukaisenzone_ratio_cooldown_slot_2",
                    "jujutsukaisenzone_ratio_unlock_slot_2",
                    "jujutsukaisenzone_ratio_level_slot_2"),
            new TechniqueSkill(
                    SKILL_IMPACT, SLOT_IMPACT, Component.literal("冲击"), ICON,
                    IMPACT_COST, IMPACT_COOLDOWN_TICKS, 0,
                    "jujutsukaisenzone_ratio_cooldown_slot_3",
                    "jujutsukaisenzone_ratio_unlock_slot_3",
                    "jujutsukaisenzone_ratio_level_slot_3"),
            new TechniqueSkill(
                    SKILL_COLLAPSE, SLOT_COLLAPSE, Component.literal("瓦落瓦落"), ICON,
                    COLLAPSE_COST, COLLAPSE_COOLDOWN_TICKS, 0,
                    "jujutsukaisenzone_ratio_cooldown_slot_4",
                    "jujutsukaisenzone_ratio_unlock_slot_4",
                    "jujutsukaisenzone_ratio_level_slot_4"),
            new TechniqueSkill(
                    SKILL_OVERTIME, SLOT_OVERTIME, Component.literal("加班"), ICON,
                    OVERTIME_COST, OVERTIME_COOLDOWN_TICKS, 0,
                    "jujutsukaisenzone_ratio_cooldown_slot_5",
                    "jujutsukaisenzone_ratio_unlock_slot_5",
                    "jujutsukaisenzone_ratio_level_slot_5"),
            new TechniqueSkill(
                    SKILL_ABSOLUTION, SLOT_ABSOLUTION, Component.literal("无可谅解"), ICON,
                    ABSOLUTION_COST, ABSOLUTION_COOLDOWN_TICKS, 0,
                    "jujutsukaisenzone_ratio_cooldown_slot_6",
                    "jujutsukaisenzone_ratio_unlock_slot_6",
                    "jujutsukaisenzone_ratio_level_slot_6"));

    private static final TechniqueSkillTree SKILL_TREE =
            TechniqueSkillTree.builder(PlayerTechniqueData.TECHNIQUE_RATIO)
                    .node(SkillTreeNodeDef.skill("ratio_mark", SKILL_RATIO_MARK).autoGranted().build())
                    .node(SkillTreeNodeDef.info("ratio_rule_73")
                            .titleKey("skill_tree.jujutsukaisenzone.ratio_rule_73.title")
                            .iconPath(ICON)
                            .descriptionKey("skill_tree.jujutsukaisenzone.ratio_rule_73.desc")
                            .requires("ratio_mark")
                            .visibleWhen(UnlockCondition.alwaysVisible())
                            .autoGranted()
                            .build())
                    .node(SkillTreeNodeDef.skill("ratio_ichimonji", SKILL_ICHIMONJI)
                            .requires("ratio_mark")
                            .autoGranted()
                            .build())
                    .node(SkillTreeNodeDef.skill("ratio_impact", SKILL_IMPACT)
                            .requires("ratio_mark")
                            .autoGranted()
                            .build())
                    .node(SkillTreeNodeDef.skill("ratio_collapse", SKILL_COLLAPSE)
                            .requires("ratio_ichimonji")
                            .autoGranted()
                            .build())
                    .node(SkillTreeNodeDef.skill("ratio_overtime_vow", SKILL_OVERTIME_VOW)
                            .requires("ratio_mark")
                            .build())
                    .node(SkillTreeNodeDef.info("ratio_overtime_info")
                            .titleKey("skill_tree.jujutsukaisenzone.ratio_overtime_info.title")
                            .iconPath(ICON)
                            .descriptionKey("skill_tree.jujutsukaisenzone.ratio_overtime_info.desc")
                            .requires("ratio_overtime_vow")
                            .visibleWhen(UnlockCondition.alwaysVisible())
                            .build())
                    .node(SkillTreeNodeDef.skill("ratio_overtime", SKILL_OVERTIME)
                            .requires("ratio_overtime_vow")
                            .autoGranted()
                            .build())
                    .node(SkillTreeNodeDef.skill("ratio_absolution", SKILL_ABSOLUTION)
                            .requires("ratio_overtime_vow")
                            .autoGranted()
                            .build())
                    .build();

    static
    {
        SKILL_TREE.validateAgainstSkills(Stream.concat(SKILLS.stream(), MECHANICS.stream()).toList());
    }

    private RatioTechniqueSkillSet()
    {
    }

    @Override
    public String techniqueId()
    {
        return PlayerTechniqueData.TECHNIQUE_RATIO;
    }

    @Override
    public List<TechniqueSkill> getConfigurableSkills()
    {
        return SKILLS;
    }

    @Override
    public List<TechniqueSkill> getMechanicSkills()
    {
        return MECHANICS;
    }

    @Override
    public Optional<TechniqueSkillTree> getSkillTree()
    {
        return Optional.of(SKILL_TREE);
    }

    @Override
    public void useSkill(ServerPlayer player, String skillId)
    {
        if (!acceptsClientHotbarInput(player))
        {
            return;
        }
        if (SKILL_RATIO_MARK.equals(skillId))
        {
            castRatioMark(player);
            return;
        }
        if (SKILL_ICHIMONJI.equals(skillId))
        {
            castIchimonji(player);
            return;
        }
        if (SKILL_IMPACT.equals(skillId))
        {
            castImpact(player);
            return;
        }
        if (SKILL_COLLAPSE.equals(skillId))
        {
            castCollapse(player);
            return;
        }
        if (SKILL_OVERTIME.equals(skillId))
        {
            castOvertimeBreach(player);
            return;
        }
        if (SKILL_ABSOLUTION.equals(skillId))
        {
            castAbsolution(player);
            return;
        }
        super.useSkill(player, skillId);
    }

    private void castRatioMark(ServerPlayer player)
    {
        TechniqueSkill skill = findSkillById(SKILL_RATIO_MARK);
        if (skill == null)
        {
            return;
        }
        LivingEntity target = RatioSpotLogic.resolveCrosshairTarget(player, RatioSpotLogic.APPLY_RANGE);
        if (target == null)
        {
            player.displayClientMessage(
                    Component.literal("请对准一名目标").withStyle(ChatFormatting.YELLOW),
                    true);
            return;
        }
        if (player.distanceToSqr(target) > RatioSpotLogic.APPLY_RANGE * RatioSpotLogic.APPLY_RANGE)
        {
            player.displayClientMessage(
                    Component.literal("目标太远").withStyle(ChatFormatting.YELLOW),
                    true);
            return;
        }
        castWithSkillEconomy(player, skill, () -> RatioSpotLogic.tryApplySpot(player, target));
    }

    private void castIchimonji(ServerPlayer player)
    {
        TechniqueSkill skill = findSkillById(SKILL_ICHIMONJI);
        if (skill == null)
        {
            return;
        }
        if (RatioIchimonjiLogic.isSlashing(player))
        {
            player.displayClientMessage(
                    Component.literal("一文字斩进行中").withStyle(ChatFormatting.YELLOW),
                    true);
            return;
        }
        castWithSkillEconomy(player, skill, () -> RatioIchimonjiLogic.tryBegin(player));
    }

    private void castImpact(ServerPlayer player)
    {
        if (com.darkness.jujutsukaisenzone.technique.skill.RatioImpactLogic.hasPendingFollowUp(player))
        {
            RatioImpactLogic.tryFollowUp(player);
            return;
        }
        TechniqueSkill skill = findSkillById(SKILL_IMPACT);
        if (skill == null)
        {
            return;
        }
        castWithSkillEconomy(player, skill, () -> RatioImpactLogic.tryStageOne(player));
    }

    private void castCollapse(ServerPlayer player)
    {
        TechniqueSkill skill = findSkillById(SKILL_COLLAPSE);
        if (skill == null)
        {
            return;
        }
        castWithSkillEconomy(player, skill, () -> RatioCollapseLogic.tryCast(player));
    }

    private void castOvertimeBreach(ServerPlayer player)
    {
        TechniqueSkill skill = findSkillById(SKILL_OVERTIME);
        if (skill == null)
        {
            return;
        }
        if (!SkillUnlockLogic.isSkillUnlockedById(player, techniqueId(), SKILL_OVERTIME_VOW))
        {
            player.displayClientMessage(
                    Component.literal("尚未立下加班契约").withStyle(ChatFormatting.YELLOW),
                    true);
            return;
        }
        if (PlayerRatioOvertimeData.isVowBroken(player))
        {
            player.displayClientMessage(
                    Component.literal("今日已违约").withStyle(ChatFormatting.YELLOW),
                    true);
            return;
        }
        castWithSkillEconomy(player, skill, () ->
        {
            PlayerRatioOvertimeData.breakVow(player);
            player.displayClientMessage(
                    Component.literal("违约加班：今日不再受作息加减成，无可谅解不可用")
                            .withStyle(ChatFormatting.RED),
                    true);
        });
    }

    private void castAbsolution(ServerPlayer player)
    {
        TechniqueSkill skill = findSkillById(SKILL_ABSOLUTION);
        if (skill == null)
        {
            return;
        }
        // 门控在 tryCast 内；不满足时仍会扣经济——先校验再扣
        if (!com.darkness.jujutsukaisenzone.technique.skill.RatioOvertimeLogic.canUseAbsolution(player))
        {
            RatioAbsolutionLogic.tryCast(player);
            return;
        }
        castWithSkillEconomy(player, skill, () -> RatioAbsolutionLogic.tryCast(player));
    }

    @Override
    protected void castSkill(ServerPlayer player, String skillId)
    {
    }

    @Override
    protected void castSkillSlot(ServerPlayer player, int slot)
    {
    }

    private static TechniqueSkill skillForSlot(int slot)
    {
        for (TechniqueSkill skill : SKILLS)
        {
            if (skill.defaultSlot() == slot)
            {
                return skill;
            }
        }
        return null;
    }

    @Override
    protected int getSlotCost(int slot)
    {
        TechniqueSkill skill = skillForSlot(slot);
        return skill == null ? 0 : skill.cost();
    }

    @Override
    protected int getSlotCooldownTicks(int slot)
    {
        TechniqueSkill skill = skillForSlot(slot);
        return skill == null ? 0 : skill.cooldownTicks();
    }

    @Override
    protected boolean isSlotUnlocked(Player player, int slot)
    {
        TechniqueSkill skill = skillForSlot(slot);
        return skill != null && SkillUnlockLogic.isSkillUnlocked(player, techniqueId(), skill.skillId(), skill);
    }

    @Override
    protected int getSlotSkillLevel(Player player, int slot)
    {
        TechniqueSkill skill = skillForSlot(slot);
        if (skill == null)
        {
            return 0;
        }
        String key = skill.levelTag();
        if (key == null || key.isEmpty())
        {
            return 0;
        }
        if (!player.getPersistentData().contains(key))
        {
            return 1;
        }
        return Math.max(1, player.getPersistentData().getInt(key));
    }

    @Override
    protected String getSlotCooldownTag(int slot)
    {
        TechniqueSkill skill = skillForSlot(slot);
        return skill == null ? null : skill.cooldownTag();
    }

    @Override
    public String getSlotDisplayName(int slot)
    {
        TechniqueSkill skill = skillForSlot(slot);
        return skill == null ? null : skill.displayName().getString();
    }

    @Override
    public String getSlotIconPath(int slot)
    {
        TechniqueSkill skill = skillForSlot(slot);
        return skill == null ? null : skill.iconPath();
    }
}
