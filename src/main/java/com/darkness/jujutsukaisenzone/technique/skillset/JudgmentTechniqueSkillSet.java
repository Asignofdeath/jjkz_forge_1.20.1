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
import java.util.stream.Stream;

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

    /**
     * 机制型技能：三式法槌。
     * <p>
     * <b>不占槽位、不能主动释放</b>，只能在术式开发树里解锁；解锁后「法槌」再按一次才会循环形态
     * （标准 → 长柄 → 大锤），未解锁时法槌恒为标准形态（门控在
     * {@code JudgmentWeaponLogic#cycleGavelForm}）。
     * <p>
     * 形态是审判术式最外显的一课：拿到法槌就能看见「它还有别的样子」，因此定价最低、前置只要
     * 自动授予的法槌本身，是全术式的引导第一课。
     */
    public static final String SKILL_GAVEL_FORMS = "judgment_gavel_forms";
    /**
     * 机制型技能：量刑。
     * <p>
     * 「裁决分档」的开关——未解锁时有罪一律只判【没收】，解锁后才会掷【死刑】骰
     * （门控在 {@code JudgmentTrialSkillLogic#rollGuiltyVerdictType}）。
     * <p>
     * 它把「有罪之后会怎样」拆成两步教：「有罪 = 没收」先立住，再让玩家第一次见到【死刑】。
     * 由于「审判」「指控」「领域审理」三条路径共用同一个掷骰入口，这一处门控即三路全覆盖。
     */
    public static final String SKILL_SENTENCING = "judgment_sentencing";
    /**
     * 机制型技能：从重论处。
     * <p>
     * 「重罪与 debuff 加成」的开关——未解锁时【死刑】只吃基础概率，罪状库的重罪加成与目标身上
     * 每个 harmful debuff 的加成一律不计（门控在 {@code JudgmentTrialSkillLogic#rollGuiltyVerdictType}，
     * 与 {@link #SKILL_SENTENCING} 同一处）。
     * <p>
     * 这是全术式最晦涩的一块数值，所以放在最后：玩家先见过【死刑】，才会好奇「怎么让它更容易发生」。
     * 前置取 {@link #SKILL_SENTENCING}——没有死刑就没有「从重」可言。
     */
    public static final String SKILL_AGGRAVATION = "judgment_aggravation";

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

    /** 机制型技能的解锁标记（各自唯一的实质字段）。命名照 {@code *_unlock} 约定，随登录同步到客户端。 */
    private static final String TAG_GAVEL_FORMS_UNLOCK = "jujutsukaisenzone_judgment_gavel_forms_unlock";
    private static final String TAG_SENTENCING_UNLOCK = "jujutsukaisenzone_judgment_sentencing_unlock";
    private static final String TAG_AGGRAVATION_UNLOCK = "jujutsukaisenzone_judgment_aggravation_unlock";

    /** 机制型技能的图标：暂借同套主动技能的图标，待补美术后替换。 */
    private static final String ICON_GAVEL = "textures/gui/technique/gavel.png";
    private static final String ICON_TRIAL = "textures/gui/technique/trial.png";
    private static final String ICON_ACCUSE = "textures/gui/technique/accuse.png";

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

    /**
     * 机制型技能定义（不占槽位、不进技能栏、不能主动释放，见
     * {@link IInnateTechniqueSkillSet#getMechanicSkills()}）。
     * <p>
     * 字段口径与 {@code LoveRendezvousTechniqueSkillSet} 的同名列表一致：{@code defaultSlot = 0}
     * 让按键解析（按 defaultSlot 匹配槽位）永远命中不到；咒力 / 冷却 / 等级填 0 与空串，因为它没有
     * 自己的经济——解锁一次即永久生效。唯一实质内容是 {@code unlockTag}。
     * <p>
     * 三个机制的降级形态互为前提，构成审判术式的学习阶梯：
     * <ol>
     *   <li>{@link #SKILL_GAVEL_FORMS}——法槌恒为标准形态；</li>
     *   <li>{@link #SKILL_SENTENCING}——有罪只判【没收】，不会出现【死刑】；</li>
     *   <li>{@link #SKILL_AGGRAVATION}——【死刑】只吃基础概率，重罪与 debuff 加成不计。</li>
     * </ol>
     */
    private static final TechniqueSkill MECHANIC_GAVEL_FORMS = new TechniqueSkill(
            SKILL_GAVEL_FORMS, 0, Component.literal("三式法槌"), ICON_GAVEL, 0, 0, 0, "",
            TAG_GAVEL_FORMS_UNLOCK, "");

    private static final TechniqueSkill MECHANIC_SENTENCING = new TechniqueSkill(
            SKILL_SENTENCING, 0, Component.literal("量刑"), ICON_TRIAL, 0, 0, 0, "",
            TAG_SENTENCING_UNLOCK, "");

    private static final TechniqueSkill MECHANIC_AGGRAVATION = new TechniqueSkill(
            SKILL_AGGRAVATION, 0, Component.literal("从重论处"), ICON_ACCUSE, 0, 0, 0, "",
            TAG_AGGRAVATION_UNLOCK, "");

    private static final List<TechniqueSkill> MECHANICS =
            List.of(MECHANIC_GAVEL_FORMS, MECHANIC_SENTENCING, MECHANIC_AGGRAVATION);

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
            /*
             * 说明节点「上诉」：原本写在「诛伏赐死」说明末尾。它讲的是<b>领域关闭之后</b>的另一套流程
             * （上诉期 → 即时重开 → 单人重审），与「领域内怎么审理」不是同一件事，压在一起会把那份
             * 说明再撑长一截，所以拆成自己的节点。
             *
             * 布局锚点仍取「审判」（与领域说明同一锚点、同一列），声明顺序紧随领域节点 ⇒ 落在它正下方，
             * 读作「领域之上的另一条规则」。可见性条件与领域说明一致：没有领域就还谈不到上诉。
             */
            .node(SkillTreeNodeDef.info("judgment_appeal")
                    .titleKey("skill_tree.jujutsukaisenzone.judgment_appeal.title")
                    .iconPath("textures/item/trial_technique.png")
                    .descriptionKey("skill_tree.jujutsukaisenzone.judgment_appeal.desc")
                    .requires("judgment_trial")
                    .visibleWhen(UnlockCondition.hasDomainExpansion())
                    .build())
            /*
             * 机制型技能「三式法槌」：<b>不占槽位、不能主动释放</b>，解锁即永久生效——它改的是
             * 「法槌再按一次会怎样」这条既有行为，而不是多给一个按键。
             *
             * 前置取「法槌」本身（自动授予、零情报），是全术式的引导第一课；定价也最低。
             */
            .node(SkillTreeNodeDef.skill("judgment_gavel_forms", SKILL_GAVEL_FORMS)
                    .requires("judgment_gavel")
                    .intelCost(150)
                    .build())
            /*
             * 机制型技能「量刑」：把「有罪之后会怎样」拆成两步的第一半——先只会【没收】。
             *
             * 前置取「审判」：裁决只在有罪之后发生，而有罪由审判 / 指控 / 领域审理产生，
             * 没学会其中任意一条就还见不到「有罪」这个词，此时开放裁决分档没有意义。
             */
            .node(SkillTreeNodeDef.skill("judgment_sentencing", SKILL_SENTENCING)
                    .requires("judgment_trial")
                    .intelCost(150)
                    .build())
            /*
             * 机制型技能「从重论处」：第二半——【死刑】的概率怎么被推高。
             *
             * 前置取「量刑」而不是与它平级：没有死刑就没有「从重」可言，顺序不可颠倒。
             * 它也是三条里最晦涩的一块（罪状库 + debuff 两层加成），因此定价最高、放在最后。
             */
            .node(SkillTreeNodeDef.skill("judgment_aggravation", SKILL_AGGRAVATION)
                    .requires("judgment_sentencing")
                    .intelCost(200)
                    .build())
            .build();

    static
    {
        /*
         * 机制型技能与槽位技能一样，在树上都是一个 SKILL 节点，所以一并校验：
         * 少写节点会在这里直接抛异常，而不是等到某天玩家点开开发树才变成「XXX / 无描述」。
         */
        SKILL_TREE.validateAgainstSkills(Stream.concat(SKILLS.stream(), MECHANICS.stream()).toList());
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

    /** 机制型技能：不占槽位、不进技能栏，只能靠开发树解锁（见 {@link #SKILL_GAVEL_FORMS}）。 */
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
