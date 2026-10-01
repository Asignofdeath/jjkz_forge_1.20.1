package com.darkness.jujutsukaisenzone.technique.skillset;

import com.darkness.jujutsukaisenzone.commandwheel.CommandWheelEntry;
import com.darkness.jujutsukaisenzone.commandwheel.TenShadowsCommandWheelProvider;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import com.darkness.jujutsukaisenzone.technique.development.SkillTreeNodeDef;
import com.darkness.jujutsukaisenzone.technique.development.SkillUnlockLogic;
import com.darkness.jujutsukaisenzone.technique.development.SkillUpgradeLogic;
import com.darkness.jujutsukaisenzone.technique.development.TechniqueSkillTree;
import com.darkness.jujutsukaisenzone.technique.development.UnlockCondition;
import com.darkness.jujutsukaisenzone.technique.skill.TenShadowsShikigami;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Optional;

/**
 * 十种影法术：单技能「手之影」；「影之式神」模式由指令圆盘切入。
 * <p>
 * 式神本身不注册为 {@link TechniqueSkill}；模式内裸数字键召唤，修饰键 + 数字键走式神招式管线
 * （{@code technique.shikigami}），与热键栏特别招式平行。
 */
public final class TenShadowsTechniqueSkillSet extends AbstractInnateTechniqueSkillSet
{
    public static final TenShadowsTechniqueSkillSet INSTANCE = new TenShadowsTechniqueSkillSet();

    public static final String SKILL_HAND_SHADOW = "hand_shadow";

    private static final int SLOT_HAND_SHADOW = 1;
    private static final int HAND_SHADOW_COST = 20;
    private static final int HAND_SHADOW_COOLDOWN_TICKS = 20 * 5;
    /** 占位遁影：隐身时长。 */
    public static final int HAND_SHADOW_INVIS_TICKS = 20 * 3;

    private static final String ICON =
            "textures/gui/technique/placeholder_technique_skill.png";

    private static final List<TechniqueSkill> SKILLS = List.of(
            new TechniqueSkill(
                    SKILL_HAND_SHADOW,
                    SLOT_HAND_SHADOW,
                    Component.literal("手之影"),
                    ICON,
                    HAND_SHADOW_COST,
                    HAND_SHADOW_COOLDOWN_TICKS,
                    0,
                    "jujutsukaisenzone_ten_shadows_cooldown_hand_shadow",
                    "jujutsukaisenzone_ten_shadows_unlock_hand_shadow",
                    "jujutsukaisenzone_ten_shadows_level_hand_shadow"));

    private static final TechniqueSkillTree SKILL_TREE =
            TechniqueSkillTree.builder(PlayerTechniqueData.TECHNIQUE_TEN_SHADOWS)
                    .node(SkillTreeNodeDef.skill("hand_shadow", SKILL_HAND_SHADOW).autoGranted().build())
                    .node(SkillTreeNodeDef.info("ten_shadows_shikigami_mode")
                            .titleKey("skill_tree.jujutsukaisenzone.ten_shadows_shikigami_mode.title")
                            .iconPath(ICON)
                            .descriptionKey("skill_tree.jujutsukaisenzone.ten_shadows_shikigami_mode.desc")
                            .requires("hand_shadow")
                            .visibleWhen(UnlockCondition.alwaysVisible())
                            .autoGranted()
                            .build())
                    .node(SkillTreeNodeDef.info("ten_shadows_ritual")
                            .titleKey("skill_tree.jujutsukaisenzone.ten_shadows_ritual.title")
                            .iconPath(ICON)
                            .descriptionKey("skill_tree.jujutsukaisenzone.ten_shadows_ritual.desc")
                            .requires("hand_shadow")
                            .visibleWhen(UnlockCondition.alwaysVisible())
                            .autoGranted()
                            .build())
                    .node(shikigamiInfo(TenShadowsShikigami.DIVINE_DOGS))
                    .node(shikigamiInfo(TenShadowsShikigami.NUE))
                    .node(shikigamiInfo(TenShadowsShikigami.TOAD))
                    .node(shikigamiInfo(TenShadowsShikigami.GREAT_SERPENT))
                    .node(shikigamiInfo(TenShadowsShikigami.MAX_ELEPHANT))
                    .node(shikigamiInfo(TenShadowsShikigami.RABBIT_ESCAPE))
                    .node(shikigamiInfo(TenShadowsShikigami.ROUND_DEER))
                    .node(shikigamiInfo(TenShadowsShikigami.PIERCING_OX))
                    .node(shikigamiInfo(TenShadowsShikigami.TIGER_FUNERAL))
                    .node(shikigamiInfo(TenShadowsShikigami.MAHORAGA))
                    .build();

    /** 已调伏后才显示的式神说明节点（召唤 / 维持 / 招式咒力见 lang）。 */
    private static SkillTreeNodeDef shikigamiInfo(TenShadowsShikigami shikigami)
    {
        String nodeId = "ten_shadows_" + shikigami.id();
        return SkillTreeNodeDef.info(nodeId)
                .titleKey("skill_tree.jujutsukaisenzone." + nodeId + ".title")
                .iconPath(ICON)
                .descriptionKey("skill_tree.jujutsukaisenzone." + nodeId + ".desc")
                .requires("hand_shadow")
                .visibleWhen(UnlockCondition.hasTamedShikigami(shikigami.id()))
                .autoGranted()
                .build();
    }

    static
    {
        SKILL_TREE.validateAgainstSkills(SKILLS);
    }

    private TenShadowsTechniqueSkillSet()
    {
    }

    @Override
    public String techniqueId()
    {
        return PlayerTechniqueData.TECHNIQUE_TEN_SHADOWS;
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
    public List<CommandWheelEntry> commandWheelEntries()
    {
        return TenShadowsCommandWheelProvider.ENTRIES;
    }

    @Override
    public void useSkill(ServerPlayer player, String skillId)
    {
        if (!acceptsClientHotbarInput(player))
        {
            return;
        }
        if (SKILL_HAND_SHADOW.equals(skillId))
        {
            TechniqueSkill skill = findSkillById(SKILL_HAND_SHADOW);
            if (skill == null)
            {
                return;
            }
            castWithSkillEconomy(player, skill, () -> castHandShadow(player));
            return;
        }
        super.useSkill(player, skillId);
    }

    private static void castHandShadow(ServerPlayer player)
    {
        player.addEffect(new MobEffectInstance(
                MobEffects.INVISIBILITY,
                handShadowInvisTicks(player),
                0,
                false,
                false,
                true));
        player.displayClientMessage(
                Component.literal("手之影 · 遁入影子（占位）").withStyle(ChatFormatting.DARK_GRAY),
                true);
    }

    /** 隐身时长：Lv1=3s，每级 +1s。 */
    public static int handShadowInvisTicks(ServerPlayer player)
    {
        return HAND_SHADOW_INVIS_TICKS
                + 20 * SkillUpgradeLogic.upgradeSteps(
                player, PlayerTechniqueData.TECHNIQUE_TEN_SHADOWS, SKILL_HAND_SHADOW);
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
