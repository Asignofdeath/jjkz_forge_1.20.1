package com.darkness.jujutsukaisenzone.technique.skillset;

import com.darkness.jujutsukaisenzone.compat.EpicFightCompat;
import com.darkness.jujutsukaisenzone.player.PlayerCursedEnergyData;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import com.darkness.jujutsukaisenzone.technique.development.SkillTreeNodeDef;
import com.darkness.jujutsukaisenzone.technique.development.SkillUnlockLogic;
import com.darkness.jujutsukaisenzone.technique.development.TechniqueSkillTree;
import com.darkness.jujutsukaisenzone.technique.development.UnlockCondition;
import com.darkness.jujutsukaisenzone.technique.skill.JudgmentAccuseSkillLogic;
import com.darkness.jujutsukaisenzone.technique.skill.JudgmentWeaponLogic;
import com.darkness.jujutsukaisenzone.technique.skill.JudgmentTrialSkillLogic;
import com.darkness.jujutsukaisenzone.technique.skill.JudgmentViewFileSkillLogic;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 审判术式招式组。
 */
public final class JudgmentTechniqueSkillSet extends AbstractInnateTechniqueSkillSet
{
    public static final JudgmentTechniqueSkillSet INSTANCE = new JudgmentTechniqueSkillSet();

    private static final int SLOT_GAVEL = 1;
    private static final int SLOT_TRIAL = 2;
    private static final int SLOT_ACCUSE = 3;
    private static final int SLOT_VIEW_FILE = 4;
    private static final int GAVEL_COST = 20;
    private static final int GAVEL_COOLDOWN_TICKS = 0;
    private static final int TRIAL_COST = 120;
    /** 冷却 10 秒。 */
    private static final int TRIAL_COOLDOWN_TICKS = 20 * 10;
    private static final int ACCUSE_COST = 80;
    /** 冷却 15 秒。 */
    private static final int ACCUSE_COOLDOWN_TICKS = 20 * 15;
    private static final int VIEW_FILE_COST = 50;
    /** 冷却 20 秒。 */
    private static final int VIEW_FILE_COOLDOWN_TICKS = 20 * 20;

    public static final String SKILL_GAVEL = "judgment_gavel";
    public static final String SKILL_TRIAL = "judgment_trial";
    public static final String SKILL_ACCUSE = "judgment_accuse";
    public static final String SKILL_VIEW_FILE = "judgment_view_file";

    private static final String TAG_JUDGMENT_UNLOCK_SLOT_1 = "jujutsukaisenzone_judgment_unlock_slot_1";
    private static final String TAG_JUDGMENT_LEVEL_SLOT_1 = "jujutsukaisenzone_judgment_level_slot_1";
    private static final String TAG_JUDGMENT_COOLDOWN_SLOT_1 = "jujutsukaisenzone_judgment_cooldown_slot_1";
    private static final String TAG_JUDGMENT_UNLOCK_SLOT_2 = "jujutsukaisenzone_judgment_unlock_slot_2";
    private static final String TAG_JUDGMENT_LEVEL_SLOT_2 = "jujutsukaisenzone_judgment_level_slot_2";
    private static final String TAG_JUDGMENT_COOLDOWN_SLOT_2 = "jujutsukaisenzone_judgment_cooldown_slot_2";
    private static final String TAG_JUDGMENT_UNLOCK_SLOT_3 = "jujutsukaisenzone_judgment_unlock_slot_3";
    private static final String TAG_JUDGMENT_LEVEL_SLOT_3 = "jujutsukaisenzone_judgment_level_slot_3";
    private static final String TAG_JUDGMENT_COOLDOWN_SLOT_3 = "jujutsukaisenzone_judgment_cooldown_slot_3";
    private static final String TAG_JUDGMENT_UNLOCK_SLOT_4 = "jujutsukaisenzone_judgment_unlock_slot_4";
    private static final String TAG_JUDGMENT_LEVEL_SLOT_4 = "jujutsukaisenzone_judgment_level_slot_4";
    private static final String TAG_JUDGMENT_COOLDOWN_SLOT_4 = "jujutsukaisenzone_judgment_cooldown_slot_4";

    private static final List<TechniqueSkill> SKILLS = Arrays.asList(
            new TechniqueSkill(SKILL_GAVEL, SLOT_GAVEL, Component.literal("法槌"),
                    "textures/gui/technique/gavel.png",
                    GAVEL_COST, GAVEL_COOLDOWN_TICKS, 0,
                    TAG_JUDGMENT_COOLDOWN_SLOT_1, TAG_JUDGMENT_UNLOCK_SLOT_1, TAG_JUDGMENT_LEVEL_SLOT_1),
            new TechniqueSkill(SKILL_TRIAL, SLOT_TRIAL, Component.literal("审判"),
                    "textures/gui/technique/trial.png",
                    TRIAL_COST, TRIAL_COOLDOWN_TICKS, 0,
                    TAG_JUDGMENT_COOLDOWN_SLOT_2, TAG_JUDGMENT_UNLOCK_SLOT_2, TAG_JUDGMENT_LEVEL_SLOT_2),
            new TechniqueSkill(SKILL_ACCUSE, SLOT_ACCUSE, Component.literal("指控"),
                    "textures/gui/technique/accuse.png",
                    ACCUSE_COST, ACCUSE_COOLDOWN_TICKS, 0,
                    TAG_JUDGMENT_COOLDOWN_SLOT_3, TAG_JUDGMENT_UNLOCK_SLOT_3, TAG_JUDGMENT_LEVEL_SLOT_3),
            new TechniqueSkill(SKILL_VIEW_FILE, SLOT_VIEW_FILE, Component.literal("查看档案"),
                    "textures/gui/technique/viewfile.png",
                    VIEW_FILE_COST, VIEW_FILE_COOLDOWN_TICKS, 0,
                    TAG_JUDGMENT_COOLDOWN_SLOT_4, TAG_JUDGMENT_UNLOCK_SLOT_4, TAG_JUDGMENT_LEVEL_SLOT_4));

    private static final TechniqueSkillTree SKILL_TREE = TechniqueSkillTree.builder(PlayerTechniqueData.TECHNIQUE_JUDGMENT)
            .node(SkillTreeNodeDef.skill("judgment_gavel", SKILL_GAVEL).autoGranted().build())
            .node(SkillTreeNodeDef.info("judgment_sinful_one")
                    .titleKey("skill_tree.jujutsukaisenzone.judgment_sinful_one.title")
                    .iconPath("textures/gui/technique/gavel.png")
                    .descriptionKey("skill_tree.jujutsukaisenzone.judgment_sinful_one.desc")
                    .requires("judgment_gavel")
                    .visibleWhen(UnlockCondition.alwaysVisible())
                    .autoGranted()
                    .build())
            .node(SkillTreeNodeDef.skill("judgment_trial", SKILL_TRIAL)
                    .requires("judgment_gavel")
                    .intelCost(200)
                    .build())
            .node(SkillTreeNodeDef.skill("judgment_accuse", SKILL_ACCUSE)
                    .requires("judgment_gavel")
                    .intelCost(300)
                    .build())
            .node(SkillTreeNodeDef.skill("judgment_view_file", SKILL_VIEW_FILE)
                    .requires("judgment_gavel")
                    .intelCost(500)
                    .build())
            .node(SkillTreeNodeDef.info("judgment_domain_zhufucisi")
                    .titleKey("skill_tree.jujutsukaisenzone.judgment_domain_zhufucisi.title")
                    .iconPath("textures/item/trial_technique.png")
                    .descriptionKey("skill_tree.jujutsukaisenzone.judgment_domain_zhufucisi.desc")
                    .requires("judgment_trial")
                    .visibleWhen(UnlockCondition.hasDomainExpansion())
                    .build())
            .build();

    static
    {
        SKILL_TREE.validateAgainstSkills(SKILLS);
    }

    private JudgmentTechniqueSkillSet()
    {
    }

    @Override
    public String techniqueId()
    {
        return PlayerTechniqueData.TECHNIQUE_JUDGMENT;
    }

    @Override
    public List<TechniqueSkill> getConfigurableSkills()
    {
        return SKILLS;
    }

    @Override
    public Optional<TechniqueSkillTree> getSkillTree()
    {
        return Optional.of(SKILL_TREE);
    }

    @Override
    public void useSkill(ServerPlayer player, String skillId)
    {
        if (SKILL_TRIAL.equals(skillId))
        {
            if (!EpicFightCompat.isBattleMode(player)
                    || !techniqueId().equals(PlayerTechniqueData.getInnateTechnique(player)))
            {
                return;
            }
            tryBeginTrialLock(player);
            return;
        }
        if (SKILL_ACCUSE.equals(skillId))
        {
            if (!EpicFightCompat.isBattleMode(player)
                    || !techniqueId().equals(PlayerTechniqueData.getInnateTechnique(player)))
            {
                return;
            }
            TechniqueSkill skill = findSkillById(SKILL_ACCUSE);
            if (skill == null || !validateAccuseReady(player, skill))
            {
                return;
            }
            castWithSkillEconomy(player, skill, () -> JudgmentAccuseSkillLogic.tryBeginAccuse(player));
            return;
        }
        if (SKILL_VIEW_FILE.equals(skillId))
        {
            if (!EpicFightCompat.isBattleMode(player)
                    || !techniqueId().equals(PlayerTechniqueData.getInnateTechnique(player)))
            {
                return;
            }
            TechniqueSkill skill = findSkillById(SKILL_VIEW_FILE);
            if (skill == null || !validateViewFileReady(player, skill))
            {
                return;
            }
            castWithSkillEconomy(player, skill, () -> JudgmentViewFileSkillLogic.tryViewFile(player));
            return;
        }
        super.useSkill(player, skillId);
    }

    @Override
    protected void castSkill(ServerPlayer player, String skillId)
    {
        if (SKILL_GAVEL.equals(skillId))
        {
            JudgmentWeaponLogic.tryGrantGavelWeapon(player);
        }
    }

    private boolean validateAccuseReady(ServerPlayer player, TechniqueSkill skill)
    {
        if (!validateTrialLockReady(player, skill))
        {
            return false;
        }
        if (JudgmentAccuseSkillLogic.isActive(player))
        {
            return false;
        }
        if (JudgmentTrialSkillLogic.isLocking(player))
        {
            return false;
        }
        if (!JudgmentAccuseSkillLogic.hasCrosshairTarget(player))
        {
            player.displayClientMessage(Component.literal("请对准指控目标").withStyle(ChatFormatting.YELLOW), true);
            return false;
        }
        return true;
    }

    private boolean validateViewFileReady(ServerPlayer player, TechniqueSkill skill)
    {
        if (!validateTrialLockReady(player, skill))
        {
            return false;
        }
        if (!JudgmentViewFileSkillLogic.hasCrosshairPlayerTarget(player))
        {
            player.displayClientMessage(Component.literal("请对准一名玩家").withStyle(ChatFormatting.YELLOW), true);
            return false;
        }
        return true;
    }

    /** 开始「审判」锁定阶段（延迟扣咒力与冷却至裁决完成）。 */
    public boolean tryBeginTrialLock(ServerPlayer player)
    {
        TechniqueSkill skill = findSkillById(SKILL_TRIAL);
        if (skill == null || !validateTrialLockReady(player, skill))
        {
            return false;
        }
        if (JudgmentTrialSkillLogic.isLocking(player))
        {
            return false;
        }
        JudgmentTrialSkillLogic.startSession(player);
        return true;
    }

    /** 锁定满 2 秒后扣咒力并进入冷却。 */
    public boolean finalizeTrialEconomy(ServerPlayer player)
    {
        TechniqueSkill skill = findSkillById(SKILL_TRIAL);
        if (skill == null)
        {
            return false;
        }
        if (!player.isCreative())
        {
            if (!PlayerCursedEnergyData.tryConsume(player, skill.cost()))
            {
                return false;
            }
        }
        long now = player.level().getGameTime();
        if (usesSkillCooldown(player))
        {
            String key = skill.cooldownTag();
            if (key != null && !key.isEmpty())
            {
                player.getPersistentData().putLong(key, now + getSkillCooldownTicks(player, skill));
            }
        }
        return true;
    }

    private boolean validateTrialLockReady(ServerPlayer player, TechniqueSkill skill)
    {
        if (!isSkillUnlocked(player, skill))
        {
            player.sendSystemMessage(Component.literal("该招式尚未解锁。"));
            return false;
        }
        if (getSkillLevel(player, skill) <= 0)
        {
            player.sendSystemMessage(Component.literal("该招式等级异常，无法释放。"));
            return false;
        }
        long now = player.level().getGameTime();
        if (usesSkillCooldown(player))
        {
            String key = skill.cooldownTag();
            if (key != null && !key.isEmpty())
            {
                long expire = player.getPersistentData().getLong(key);
                if (expire > now)
                {
                    int remain = (int) (expire - now);
                    player.sendSystemMessage(Component.literal("招式冷却中：" + String.format("%.1f", remain / 20.0D) + " 秒"));
                    return false;
                }
            }
        }
        if (!player.isCreative() && PlayerCursedEnergyData.getCurrent(player) < skill.cost())
        {
            player.sendSystemMessage(Component.literal("咒力不足，无法释放该招式。"));
            return false;
        }
        return true;
    }

    @Override
    protected void castSkillSlot(ServerPlayer player, int slot)
    {
        // 暂无实现
    }

    @Override
    protected int getSlotCost(int slot)
    {
        return switch (slot)
        {
            case SLOT_GAVEL -> GAVEL_COST;
            case SLOT_TRIAL -> TRIAL_COST;
            case SLOT_ACCUSE -> ACCUSE_COST;
            case SLOT_VIEW_FILE -> VIEW_FILE_COST;
            default -> 0;
        };
    }

    @Override
    protected int getSlotCooldownTicks(int slot)
    {
        return switch (slot)
        {
            case SLOT_GAVEL -> GAVEL_COOLDOWN_TICKS;
            case SLOT_TRIAL -> TRIAL_COOLDOWN_TICKS;
            case SLOT_ACCUSE -> ACCUSE_COOLDOWN_TICKS;
            case SLOT_VIEW_FILE -> VIEW_FILE_COOLDOWN_TICKS;
            default -> 0;
        };
    }

    @Override
    protected boolean isSlotUnlocked(Player player, int slot)
    {
        TechniqueSkill skill = switch (slot)
        {
            case SLOT_GAVEL -> SKILLS.get(0);
            case SLOT_TRIAL -> SKILLS.get(1);
            case SLOT_ACCUSE -> SKILLS.get(2);
            case SLOT_VIEW_FILE -> SKILLS.get(3);
            default -> null;
        };
        if (skill == null)
        {
            return false;
        }
        return SkillUnlockLogic.isSkillUnlocked(player, techniqueId(), skill.skillId(), skill);
    }

    @Override
    protected int getSlotSkillLevel(Player player, int slot)
    {
        String key = switch (slot)
        {
            case SLOT_GAVEL -> TAG_JUDGMENT_LEVEL_SLOT_1;
            case SLOT_TRIAL -> TAG_JUDGMENT_LEVEL_SLOT_2;
            case SLOT_ACCUSE -> TAG_JUDGMENT_LEVEL_SLOT_3;
            case SLOT_VIEW_FILE -> TAG_JUDGMENT_LEVEL_SLOT_4;
            default -> null;
        };
        if (key == null)
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
        return switch (slot)
        {
            case SLOT_GAVEL -> TAG_JUDGMENT_COOLDOWN_SLOT_1;
            case SLOT_TRIAL -> TAG_JUDGMENT_COOLDOWN_SLOT_2;
            case SLOT_ACCUSE -> TAG_JUDGMENT_COOLDOWN_SLOT_3;
            case SLOT_VIEW_FILE -> TAG_JUDGMENT_COOLDOWN_SLOT_4;
            default -> null;
        };
    }

    @Override
    public String getSlotDisplayName(int slot)
    {
        return switch (slot)
        {
            case SLOT_GAVEL -> "法槌";
            case SLOT_TRIAL -> "审判";
            case SLOT_ACCUSE -> "指控";
            case SLOT_VIEW_FILE -> "查看档案";
            default -> null;
        };
    }

    @Override
    public String getSlotIconPath(int slot)
    {
        return switch (slot)
        {
            case SLOT_GAVEL -> "textures/gui/technique/gavel.png";
            case SLOT_TRIAL -> "textures/gui/technique/trial.png";
            case SLOT_ACCUSE -> "textures/gui/technique/accuse.png";
            case SLOT_VIEW_FILE -> "textures/gui/technique/viewfile.png";
            default -> null;
        };
    }
}
