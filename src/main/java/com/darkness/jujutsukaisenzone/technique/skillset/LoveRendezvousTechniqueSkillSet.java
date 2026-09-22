package com.darkness.jujutsukaisenzone.technique.skillset;

import com.darkness.jujutsukaisenzone.compat.EpicFightCompat;
import com.darkness.jujutsukaisenzone.player.PlayerStarMarkData;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import com.darkness.jujutsukaisenzone.player.StarMark;
import com.darkness.jujutsukaisenzone.player.StarMarkType;
import com.darkness.jujutsukaisenzone.technique.development.SkillTreeNodeDef;
import com.darkness.jujutsukaisenzone.technique.development.SkillUnlockLogic;
import com.darkness.jujutsukaisenzone.technique.development.TechniqueSkillTree;
import com.darkness.jujutsukaisenzone.technique.skill.StarMarkRules;
import com.darkness.jujutsukaisenzone.technique.skill.StarMarkSkillLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * 星间飞行（Love Rendezvous）术式招式组。
 * <p>
 * 槽位 1「星星标记」：近战命中实体 / 看向方块后按技能键即打上「当前标记」，咒力在打标那一刻结算一次。
 * 槽位 2「牵引」：把当前标记的所有目标拉向自己，持续 5 秒。
 * 槽位 3「禁行」：展开星域，域内跨序之星无法靠近，持续 15 秒。
 * <p>
 * 三态力学（同标记吸引 / 相邻无效果 / 跨序斥力）见 {@link StarMarkRules}。
 */
public final class LoveRendezvousTechniqueSkillSet extends AbstractInnateTechniqueSkillSet
{
    public static final LoveRendezvousTechniqueSkillSet INSTANCE = new LoveRendezvousTechniqueSkillSet();

    public static final String SKILL_STAR_MARK = "love_rendezvous_star_mark";
    public static final String SKILL_STAR_PULL = "love_rendezvous_star_pull";
    public static final String SKILL_STAR_GATE = "love_rendezvous_star_gate";

    private static final int SLOT_STAR_MARK = 1;
    private static final int SLOT_STAR_PULL = 2;
    private static final int SLOT_STAR_GATE = 3;

    private static final int STAR_MARK_COST = 30;
    private static final int STAR_MARK_COOLDOWN_TICKS = 0;
    private static final int STAR_PULL_COST = 40;
    private static final int STAR_PULL_COOLDOWN_TICKS = 8 * 20;
    private static final int STAR_GATE_COST = 60;
    private static final int STAR_GATE_COOLDOWN_TICKS = 30 * 20;

    private static final String ICON_STAR_MARK = "textures/gui/technique/star_mark.png";
    /** 牵引 / 禁行暂时复用星标图标，待补美术后替换。 */
    private static final String ICON_STAR_PULL = ICON_STAR_MARK;
    private static final String ICON_STAR_GATE = ICON_STAR_MARK;

    private static final List<TechniqueSkill> SKILLS = List.of(
            new TechniqueSkill(SKILL_STAR_MARK, SLOT_STAR_MARK, Component.literal("星星标记"), ICON_STAR_MARK,
                    STAR_MARK_COST, STAR_MARK_COOLDOWN_TICKS, 0,
                    tag("cooldown", SLOT_STAR_MARK), tag("unlock", SLOT_STAR_MARK), tag("level", SLOT_STAR_MARK)),
            new TechniqueSkill(SKILL_STAR_PULL, SLOT_STAR_PULL, Component.literal("牵引"), ICON_STAR_PULL,
                    STAR_PULL_COST, STAR_PULL_COOLDOWN_TICKS, 0,
                    tag("cooldown", SLOT_STAR_PULL), tag("unlock", SLOT_STAR_PULL), tag("level", SLOT_STAR_PULL)),
            new TechniqueSkill(SKILL_STAR_GATE, SLOT_STAR_GATE, Component.literal("禁行"), ICON_STAR_GATE,
                    STAR_GATE_COST, STAR_GATE_COOLDOWN_TICKS, 0,
                    tag("cooldown", SLOT_STAR_GATE), tag("unlock", SLOT_STAR_GATE), tag("level", SLOT_STAR_GATE)));

    private static final TechniqueSkillTree SKILL_TREE = TechniqueSkillTree.builder(PlayerTechniqueData.TECHNIQUE_LOVE_RENDEZVOUS)
            .node(SkillTreeNodeDef.skill("love_rendezvous_star_mark", SKILL_STAR_MARK).autoGranted().build())
            .node(SkillTreeNodeDef.skill("love_rendezvous_star_pull", SKILL_STAR_PULL)
                    .requires("love_rendezvous_star_mark")
                    .intelCost(200)
                    .build())
            .node(SkillTreeNodeDef.skill("love_rendezvous_star_gate", SKILL_STAR_GATE)
                    .requires("love_rendezvous_star_pull")
                    .intelCost(300)
                    .build())
            .build();

    static
    {
        SKILL_TREE.validateAgainstSkills(SKILLS);
    }

    private LoveRendezvousTechniqueSkillSet()
    {
    }

    /** 按项目命名约定生成 tag：{@code jujutsukaisenzone_love_rendezvous_<kind>_slot_<slot>}。 */
    private static String tag(String kind, int slot)
    {
        return "jujutsukaisenzone_love_rendezvous_" + kind + "_slot_" + slot;
    }

    private static TechniqueSkill skillForSlot(int slot)
    {
        return slot >= 1 && slot <= SKILLS.size() ? SKILLS.get(slot - 1) : null;
    }

    @Override
    public String techniqueId()
    {
        return PlayerTechniqueData.TECHNIQUE_LOVE_RENDEZVOUS;
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

    // ======================== 释放 ========================

    @Override
    public void useSkill(ServerPlayer player, String skillId)
    {
        if (!EpicFightCompat.isBattleMode(player)
                || !techniqueId().equals(PlayerTechniqueData.getInnateTechnique(player)))
        {
            return;
        }
        TechniqueSkill skill = findSkillById(skillId);
        if (skill == null)
        {
            return;
        }
        if (SKILL_STAR_MARK.equals(skillId))
        {
            /* 打标本身在「接触」那一刻结算咒力 / 冷却（见 tryApplyMark）。
               实体走近战命中，方块走技能键——视线由服务端自行射线，客户端只报「按了键」。
               没瞄到方块时退化为报当前状态，不消耗。 */
            BlockPos looked = StarMarkSkillLogic.lookedAtBlock(player);
            if (looked == null)
            {
                notifyStarMarkStatus(player);
                return;
            }
            tryApplyMark(player, StarMarkSkillLogic.markOfBlock(player, looked),
                    StarMarkSkillLogic.describeBlock(looked));
            return;
        }
        if (SKILL_STAR_PULL.equals(skillId))
        {
            castWithSkillEconomy(player, skill, () -> StarMarkRules.activatePull(player));
            return;
        }
        if (SKILL_STAR_GATE.equals(skillId))
        {
            castWithSkillEconomy(player, skill, () -> StarMarkRules.activateGate(player));
            return;
        }
        super.useSkill(player, skillId);
    }

    /**
     * 打标入口：近战命中实体（{@code StarMarkEvents}）与技能键命中方块（{@link #useSkill}）都走这里。
     * <p>
     * 内含战斗模式与术式校验，并复用星标技能的解锁 / 咒力 / 冷却结算；
     * 同目标同星属于空操作，不消耗。
     */
    public void tryApplyMark(ServerPlayer player, StarMark target, String targetName)
    {
        if (!EpicFightCompat.isBattleMode(player)
                || !techniqueId().equals(PlayerTechniqueData.getInnateTechnique(player)))
        {
            return;
        }
        if (player.isSpectator() || !player.isAlive())
        {
            return;
        }

        StarMarkType selected = PlayerStarMarkData.getSelected(player);
        StarMark existing = PlayerStarMarkData.findTarget(player, target);
        if (existing != null && existing.type() == selected)
        {
            player.displayClientMessage(
                    Component.literal(selected.displayName() + " 已在该目标身上，无需重复标记"), true);
            return;
        }

        TechniqueSkill skill = findSkillById(SKILL_STAR_MARK);
        if (skill == null)
        {
            return;
        }
        castWithSkillEconomy(player, skill, () -> applyMark(player, target, targetName, existing));
    }

    private void applyMark(ServerPlayer player, StarMark target, String targetName, @Nullable StarMark existing)
    {
        PlayerStarMarkData.put(player, target);
        String suffix = existing == null ? "" : "（已覆盖 " + existing.type().displayName() + "）";
        player.displayClientMessage(
                Component.literal(target.type().displayName() + " · 已标记" + targetName + suffix), true);
    }

    private void notifyStarMarkStatus(ServerPlayer player)
    {
        StarMarkType selected = PlayerStarMarkData.getSelected(player);
        int count = PlayerStarMarkData.countOfType(player, selected);
        player.displayClientMessage(
                Component.literal("★当前标记 " + selected.displayName() + " · 已作用于 " + count
                        + " 个目标；近战命中实体 / 看向方块按技能键即可打标"),
                true);
    }

    @Override
    protected void castSkillSlot(ServerPlayer player, int slot)
    {
        // 释放统一走 skillId 驱动的 useSkill
    }

    // ======================== 槽位属性 ========================

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
