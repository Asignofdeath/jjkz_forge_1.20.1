package com.darkness.jujutsukaisenzone.technique.skillset;

import com.darkness.jujutsukaisenzone.compat.EpicFightCompat;
import com.darkness.jujutsukaisenzone.event.LimitlessPurpleShiftSpecialEvents;
import com.darkness.jujutsukaisenzone.entity.SpecialEffectEntity;
import com.darkness.jujutsukaisenzone.entity.behavior.LimitlessRemoteMurasakiBehavior;
import com.darkness.jujutsukaisenzone.entity.data.LimitlessRemoteMurasakiConfig;
import com.darkness.jujutsukaisenzone.player.BarrierTechniqueLevel;
import com.darkness.jujutsukaisenzone.player.PlayerCursedEnergyData;
import com.darkness.jujutsukaisenzone.player.PlayerHoldReleaseChargeData;
import com.darkness.jujutsukaisenzone.player.PlayerCursedGradeData;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import com.darkness.jujutsukaisenzone.registry.ModEntities;
import com.darkness.jujutsukaisenzone.registry.ModSounds;
import com.darkness.jujutsukaisenzone.technique.commonskill.ReverseTechniqueCommonKeySkill;
import com.darkness.jujutsukaisenzone.technique.development.SkillUnlockLogic;
import com.darkness.jujutsukaisenzone.technique.development.SkillTreeNodeDef;
import com.darkness.jujutsukaisenzone.technique.development.SkillTreeReferenceKind;
import com.darkness.jujutsukaisenzone.technique.development.TechniqueSkillTree;
import com.darkness.jujutsukaisenzone.technique.development.UnlockCondition;
import com.darkness.jujutsukaisenzone.technique.scaling.LimitlessPurpleSkillGrowth;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * 无下限术式：招式槽与苍、赫、茈（槽位3）等具体演出。
 */
public final class LimitlessTechniqueSkillSet extends AbstractInnateTechniqueSkillSet
{
    public static final LimitlessTechniqueSkillSet INSTANCE = new LimitlessTechniqueSkillSet();

    private static final int SLOT_BLUE_NORMAL = 1;
    private static final int SLOT_RED_NORMAL = 2;
    /** 茈普通释放 */
    private static final int SLOT_PURPLE_NORMAL = 3;
    private static final int SLOT_REMOTE_PURPLE = 4;
    private static final int BLUE_COST = 50;
    private static final int RED_COST = 100;
    private static final int PURPLE_COST = 120;
    private static final int REMOTE_PURPLE_COST = 200;
    private static final int BLUE_COOLDOWN_TICKS = 20 * 5;
    private static final int RED_COOLDOWN_TICKS = 20 * 10;
    private static final int PURPLE_COOLDOWN_TICKS = 20 * 14;
    private static final int REMOTE_PURPLE_COOLDOWN_TICKS = 20 * 30;
    private static final String TAG_LIMITLESS_UNLOCK_SLOT_1 = "jujutsukaisenzone_limitless_unlock_slot_1";
    private static final String TAG_LIMITLESS_UNLOCK_SLOT_2 = "jujutsukaisenzone_limitless_unlock_slot_2";
    private static final String TAG_LIMITLESS_UNLOCK_SLOT_3 = "jujutsukaisenzone_limitless_unlock_slot_3";
    private static final String TAG_LIMITLESS_UNLOCK_SLOT_4 = "jujutsukaisenzone_limitless_unlock_slot_4";
    /** Shift+1/2/3 变体招式解锁，与原槽位 {@link #TAG_LIMITLESS_UNLOCK_SLOT_1} 等无关。 */
    private static final String TAG_LIMITLESS_UNLOCK_VARIANT_SHIFT_SLOT_1 = "jujutsukaisenzone_limitless_unlock_variant_shift_slot_1";
    private static final String TAG_LIMITLESS_UNLOCK_VARIANT_SHIFT_SLOT_2 = "jujutsukaisenzone_limitless_unlock_variant_shift_slot_2";
    private static final String TAG_LIMITLESS_UNLOCK_VARIANT_SHIFT_SLOT_3 = "jujutsukaisenzone_limitless_unlock_variant_shift_slot_3";
    private static final String TAG_LIMITLESS_LEVEL_SLOT_1 = "jujutsukaisenzone_limitless_level_slot_1";
    private static final String TAG_LIMITLESS_LEVEL_SLOT_2 = "jujutsukaisenzone_limitless_level_slot_2";
    private static final String TAG_LIMITLESS_LEVEL_SLOT_3 = "jujutsukaisenzone_limitless_level_slot_3";
    private static final String TAG_LIMITLESS_LEVEL_SLOT_4 = "jujutsukaisenzone_limitless_level_slot_4";
    private static final String TAG_LIMITLESS_COOLDOWN_SLOT_1 = "jujutsukaisenzone_limitless_cooldown_slot_1";
    private static final String TAG_LIMITLESS_COOLDOWN_SLOT_2 = "jujutsukaisenzone_limitless_cooldown_slot_2";
    private static final String TAG_LIMITLESS_COOLDOWN_SLOT_3 = "jujutsukaisenzone_limitless_cooldown_slot_3";
    private static final String TAG_LIMITLESS_COOLDOWN_SLOT_4 = "jujutsukaisenzone_limitless_cooldown_slot_4";
    private static final String TAG_LIMITLESS_CAST_RANGE_TIER = "jujutsukaisenzone_limitless_cast_range_tier";
    public static final int CAST_RANGE_TIER_MIN = 1;
    public static final int CAST_RANGE_TIER_MAX = 4;
    private static final double BLUE_RANGE = 25.0D;
    private static final double RED_SPAWN_FROM_EYE = 1.8D;
    private static final double RED_SPAWN_Y_OFFSET = -0.28D;

    // 技能 ID 常量（配合快捷栏配置使用）
    public static final String SKILL_BLUE = "limitless_blue";
    public static final String SKILL_RED = "limitless_red";
    public static final String SKILL_PURPLE = "limitless_purple";
    public static final String SKILL_REMOTE_PURPLE = "limitless_remote_murasaki";
    /** 机制型：解锁「领域模式 · 瞬发」（0.2 秒领域展开，叙事名）。 */
    public static final String SKILL_INSTANT_DOMAIN = "limitless_instant_domain";
    public static final String REF_CURSED_GRADE_SPECIAL = "ref_cursed_grade_special";
    public static final String REF_BARRIER_MASTERED = "ref_barrier_mastered";

    private static final String TAG_INSTANT_DOMAIN_UNLOCK = "jujutsukaisenzone_limitless_instant_domain_unlock";
    private static final String ICON_INSTANT_DOMAIN = "textures/item/innate_technique_limitless.png";

    private static final TechniqueSkill MECHANIC_INSTANT_DOMAIN = new TechniqueSkill(
            SKILL_INSTANT_DOMAIN, 0, Component.literal("0.2秒的领域展开"), ICON_INSTANT_DOMAIN,
            0, 0, 0, "", TAG_INSTANT_DOMAIN_UNLOCK, "");

    private static final List<TechniqueSkill> MECHANICS = List.of(MECHANIC_INSTANT_DOMAIN);

    private static final List<TechniqueSkill> SKILLS = Arrays.asList(
            new TechniqueSkill(SKILL_BLUE, 1, net.minecraft.network.chat.Component.literal("苍"),
                    "textures/gui/technique/limitless_blue.png",
                    BLUE_COST, BLUE_COOLDOWN_TICKS, DEFAULT_MAX_OUTPUT_CHARGE_TICKS,
                    TAG_LIMITLESS_COOLDOWN_SLOT_1, TAG_LIMITLESS_UNLOCK_SLOT_1, TAG_LIMITLESS_LEVEL_SLOT_1),
            new TechniqueSkill(SKILL_RED, 2, net.minecraft.network.chat.Component.literal("赫"),
                    "textures/gui/technique/limitless_red.png",
                    RED_COST, RED_COOLDOWN_TICKS, DEFAULT_MAX_OUTPUT_CHARGE_TICKS,
                    TAG_LIMITLESS_COOLDOWN_SLOT_2, TAG_LIMITLESS_UNLOCK_SLOT_2, TAG_LIMITLESS_LEVEL_SLOT_2),
            new TechniqueSkill(SKILL_PURPLE, 3, net.minecraft.network.chat.Component.literal("茈"),
                    "textures/gui/technique/limitless_purple.png",
                    PURPLE_COST, PURPLE_COOLDOWN_TICKS, DEFAULT_MAX_OUTPUT_CHARGE_TICKS,
                    TAG_LIMITLESS_COOLDOWN_SLOT_3, TAG_LIMITLESS_UNLOCK_SLOT_3, TAG_LIMITLESS_LEVEL_SLOT_3),
            new TechniqueSkill(SKILL_REMOTE_PURPLE, 4,
                    net.minecraft.network.chat.Component.literal("「稍微乱来一下」· 超远程虚式茈"),
                    "textures/gui/technique/remote_murasaki.png",
                    REMOTE_PURPLE_COST, REMOTE_PURPLE_COOLDOWN_TICKS, 0,
                    TAG_LIMITLESS_COOLDOWN_SLOT_4, TAG_LIMITLESS_UNLOCK_SLOT_4, TAG_LIMITLESS_LEVEL_SLOT_4)
    );

    private static final TechniqueSkillTree SKILL_TREE = TechniqueSkillTree.builder(PlayerTechniqueData.TECHNIQUE_LIMITLESS)
            .node(SkillTreeNodeDef.skill("limitless_blue", SKILL_BLUE).autoGranted().build())
            .node(SkillTreeNodeDef.reference(
                    "ref_reverse_technique",
                    SkillTreeReferenceKind.COMMON_SKILL,
                    ReverseTechniqueCommonKeySkill.INSTANCE.skillId()
            ).build())
            .node(SkillTreeNodeDef.skill("limitless_red", SKILL_RED)
                    .requires("ref_reverse_technique")
                    .intelCost(100)
                    .build())
            .node(SkillTreeNodeDef.skill("limitless_purple", SKILL_PURPLE)
                    .requires("limitless_blue", "limitless_red")
                    .intelCost(150)
                    .build())
            .node(SkillTreeNodeDef.reference(
                    REF_CURSED_GRADE_SPECIAL,
                    SkillTreeReferenceKind.CURSED_GRADE,
                    Integer.toString(PlayerCursedGradeData.RANK_SPECIAL)
            ).build())
            .node(SkillTreeNodeDef.skill("limitless_remote_murasaki", SKILL_REMOTE_PURPLE)
                    .requires("limitless_purple", REF_CURSED_GRADE_SPECIAL)
                    .intelCost(500)
                    .build())
            .node(SkillTreeNodeDef.info("limitless_domain_muryokusho")
                    .titleKey("skill_tree.jujutsukaisenzone.limitless_domain_muryokusho.title")
                    .iconPath("textures/item/innate_technique_limitless.png")
                    .descriptionKey("skill_tree.jujutsukaisenzone.limitless_domain_muryokusho.desc")
                    .requires("limitless_purple")
                    .visibleWhen(UnlockCondition.hasDomainExpansion())
                    .build())
            .node(SkillTreeNodeDef.reference(
                    REF_BARRIER_MASTERED,
                    SkillTreeReferenceKind.BARRIER_TECHNIQUE_LEVEL,
                    BarrierTechniqueLevel.MASTERED.name()
            ).requires("limitless_purple").build())
            /*
             * 机制型：「0.2秒的领域展开」——解锁通用圆盘「领域模式」的瞬发档。
             * 条件：已习得领域展开 + 结界术达到掌握（熟练）；前置节点用引用节点展示结界等级。
             */
            .node(SkillTreeNodeDef.skill("limitless_instant_domain", SKILL_INSTANT_DOMAIN)
                    .requires("limitless_purple", REF_BARRIER_MASTERED)
                    .conditions(
                            UnlockCondition.hasDomainExpansion(),
                            UnlockCondition.minBarrierTechniqueLevel(BarrierTechniqueLevel.MASTERED))
                    .intelCost(300)
                    .build())
            .build();

    /**
     * Shift + 槽位数字触发的特别招式（不占独立槽位）。
     * <p>
     * 数值沿用原技能（等级 / 咒力 / 冷却 = 同数字槽位），因此这里只声明「挂在哪个原技能上、
     * 用什么名字」；开发树的节点由 {@code TechniqueSpecialMoveAttacher} 自动派生。
     * 图标留空 = 复用原技能图标。
     */
    private static final List<TechniqueSpecialMove> SPECIAL_MOVES = List.of(
            new TechniqueSpecialMove("limitless_blue_shift", SKILL_BLUE, SpecialMoveInput.MOD_SHIFT,
                    Component.literal("苍（机动）"), null),
            new TechniqueSpecialMove("limitless_red_shift", SKILL_RED, SpecialMoveInput.MOD_SHIFT,
                    Component.literal("赫（机动）"), null),
            new TechniqueSpecialMove("limitless_purple_shift", SKILL_PURPLE, SpecialMoveInput.MOD_SHIFT,
                    Component.literal("茈（升空悬停）"), null));

    static
    {
        SKILL_TREE.validateAgainstSkills(Stream.concat(SKILLS.stream(), MECHANICS.stream()).toList());
    }

    @Override
    public List<TechniqueSpecialMove> getSpecialMoves(String baseSkillId)
    {
        return SPECIAL_MOVES.stream()
                .filter(move -> move.baseSkillId().equals(baseSkillId))
                .toList();
    }

    private LimitlessTechniqueSkillSet()
    {
    }

    @Override
    public String techniqueId()
    {
        return PlayerTechniqueData.TECHNIQUE_LIMITLESS;
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
    protected void castSkill(ServerPlayer player, String skillId)
    {
        switch (skillId)
        {
            case SKILL_BLUE -> castBlueNormal(player, false);
            case SKILL_RED -> castRedNormal(player, false);
            case SKILL_PURPLE -> castPurpleMiniNormal(player);
        }
    }

    @Override
    public int getHoldReleaseChargeTicksBySkill(Player player, String skillId)
    {
        if (!SKILL_REMOTE_PURPLE.equals(skillId))
        {
            return 0;
        }
        TechniqueSkill skill = findSkillById(skillId);
        if (skill == null || !isSkillUnlocked(player, skill) || getSkillLevel(player, skill) <= 0)
        {
            return 0;
        }
        return LimitlessRemoteMurasakiConfig.HOLD_RELEASE_CHARGE_TICKS;
    }

    @Override
    public void beginHoldReleaseChargeBySkill(ServerPlayer player, String skillId, int slot)
    {
        if (!SKILL_REMOTE_PURPLE.equals(skillId))
        {
            return;
        }
        if (PlayerHoldReleaseChargeData.isCharging(player))
        {
            return;
        }
        TechniqueSkill skill = findSkillById(skillId);
        if (skill == null)
        {
            return;
        }
        if (!isSkillUnlocked(player, skill))
        {
            player.sendSystemMessage(Component.literal("该招式尚未解锁。"));
            return;
        }
        if (getSkillLevel(player, skill) <= 0)
        {
            player.sendSystemMessage(Component.literal("该招式等级异常，无法释放。"));
            return;
        }
        long now = player.level().getGameTime();
        if (usesSkillCooldown(player))
        {
            int remain = getRemainingSkillCooldownTicks(player, skill, now);
            if (remain > 0)
            {
                player.sendSystemMessage(Component.literal("招式冷却中：" + formatCooldownSeconds(remain) + " 秒"));
                return;
            }
        }
        if (!player.isCreative())
        {
            if (skill.cost() > 0 && PlayerCursedEnergyData.getCurrent(player) < skill.cost())
            {
                player.sendSystemMessage(Component.literal("咒力不足，无法释放该招式。"));
                return;
            }
        }
        beginRemoteMurasakiHoldRelease(player, skill, slot, now);
    }

    @Override
    public void cancelHoldReleaseChargeBySkill(ServerPlayer player, String skillId, int slot)
    {
        if (!PlayerHoldReleaseChargeData.isCharging(player) || PlayerHoldReleaseChargeData.getChargingSlot(player) != slot)
        {
            return;
        }
        if (!SKILL_REMOTE_PURPLE.equals(PlayerHoldReleaseChargeData.getChargingSkillId(player)))
        {
            return;
        }
        discardRemoteMurasakiChargeEntity(player);
        PlayerHoldReleaseChargeData.clear(player);
    }

    /** 长按满 5 秒后由 {@link LimitlessRemoteMurasakiBehavior} 调用，无需松手即进入发射阶段。 */
    public void tryAutoCompleteRemoteMurasakiHoldRelease(ServerPlayer player, UUID entityUuid)
    {
        if (!PlayerHoldReleaseChargeData.isCharging(player))
        {
            return;
        }
        if (!SKILL_REMOTE_PURPLE.equals(PlayerHoldReleaseChargeData.getChargingSkillId(player)))
        {
            return;
        }
        if (!PlayerHoldReleaseChargeData.getChargeEntityUuid(player).equals(entityUuid))
        {
            return;
        }
        long heldTicks = player.level().getGameTime() - PlayerHoldReleaseChargeData.getChargeStartGameTick(player);
        if (heldTicks < LimitlessRemoteMurasakiConfig.HOLD_RELEASE_CHARGE_TICKS)
        {
            return;
        }
        completeRemoteMurasakiHoldRelease(player, entityUuid);
    }

    @Override
    public void releaseHoldReleaseChargeBySkill(ServerPlayer player, String skillId, int slot)
    {
        if (!PlayerHoldReleaseChargeData.isCharging(player) || PlayerHoldReleaseChargeData.getChargingSlot(player) != slot)
        {
            return;
        }
        if (!SKILL_REMOTE_PURPLE.equals(skillId)
                || !SKILL_REMOTE_PURPLE.equals(PlayerHoldReleaseChargeData.getChargingSkillId(player)))
        {
            return;
        }
        long now = player.level().getGameTime();
        long heldTicks = now - PlayerHoldReleaseChargeData.getChargeStartGameTick(player);
        UUID entityUuid = PlayerHoldReleaseChargeData.getChargeEntityUuid(player);
        if (heldTicks < LimitlessRemoteMurasakiConfig.HOLD_RELEASE_CHARGE_TICKS)
        {
            PlayerHoldReleaseChargeData.clear(player);
            discardRemoteMurasakiChargeEntity(player, entityUuid);
            return;
        }
        completeRemoteMurasakiHoldRelease(player, entityUuid);
    }

    private void completeRemoteMurasakiHoldRelease(ServerPlayer player, UUID entityUuid)
    {
        if (!PlayerHoldReleaseChargeData.isCharging(player))
        {
            return;
        }
        TechniqueSkill skill = findSkillById(SKILL_REMOTE_PURPLE);
        if (skill == null)
        {
            PlayerHoldReleaseChargeData.clear(player);
            return;
        }
        long now = player.level().getGameTime();
        PlayerHoldReleaseChargeData.clear(player);
        if (!isSkillUnlocked(player, skill) || getSkillLevel(player, skill) <= 0)
        {
            discardRemoteMurasakiChargeEntity(player, entityUuid);
            return;
        }
        if (usesSkillCooldown(player))
        {
            int remain = getRemainingSkillCooldownTicks(player, skill, now);
            if (remain > 0)
            {
                player.sendSystemMessage(Component.literal("招式冷却中，未能完成释放。"));
                discardRemoteMurasakiChargeEntity(player, entityUuid);
                return;
            }
        }
        if (!player.isCreative())
        {
            if (skill.cost() > 0 && !PlayerCursedEnergyData.tryConsume(player, skill.cost()))
            {
                player.sendSystemMessage(Component.literal("咒力不足，未能完成释放。"));
                discardRemoteMurasakiChargeEntity(player, entityUuid);
                return;
            }
        }
        if (usesSkillCooldown(player))
        {
            setSkillCooldown(player, skill, now + skill.cooldownTicks());
        }
        if (abortCastIfBrainDamageFailed(player))
        {
            discardRemoteMurasakiChargeEntity(player, entityUuid);
            return;
        }
        launchRemoteMurasakiHoldRelease(player, entityUuid);
    }

    @Override
    protected void castSpecialMoveBySkill(ServerPlayer player, String skillId, int modifierMask)
    {
        /* 掩码与变体解锁都已由基类收口在扣费之前（acceptsSpecialMove / isSpecialMoveUnlocked），
           这里只负责演出——不要再在这里写掩码判断，那会在咒力与冷却已经扣掉之后才 return。 */
        switch (skillId)
        {
            case SKILL_BLUE -> castBlueNormal(player, true);
            case SKILL_RED -> castRedNormal(player, true);
            case SKILL_PURPLE -> LimitlessPurpleShiftSpecialEvents.begin(player);
        }
    }

    /**
     * 苍 / 赫 / 茈 的三个 Shift 变体各有**独立解锁字段**（与同槽位普通招式分离），
     * 所以要在基类结算咒力与冷却之前先查一次：未解锁时基类只提示「该招式尚未解锁。」，不扣费。
     */
    @Override
    protected boolean isSpecialMoveUnlocked(ServerPlayer player, String skillId, int modifierMask)
    {
        return switch (skillId)
        {
            case SKILL_BLUE -> isShiftVariantUnlockedForSlot1(player);
            case SKILL_RED -> isShiftVariantUnlockedForSlot2(player);
            case SKILL_PURPLE -> isShiftVariantUnlockedForSlot3(player);
            default -> true;
        };
    }

    @Override
    public boolean supportsCastRangeTiers()
    {
        return true;
    }

    @Override
    public int adjustCastRangeTier(ServerPlayer player, int delta)
    {
        int current = getCastRangeTier(player);
        int next = Math.max(CAST_RANGE_TIER_MIN, Math.min(CAST_RANGE_TIER_MAX, current + delta));
        if (next != current)
        {
            player.getPersistentData().putInt(TAG_LIMITLESS_CAST_RANGE_TIER, next);
        }
        return next;
    }

    @Override
    public int getCastRangeTier(ServerPlayer player)
    {
        if (!player.getPersistentData().contains(TAG_LIMITLESS_CAST_RANGE_TIER))
        {
            return CAST_RANGE_TIER_MAX;
        }
        return Math.max(CAST_RANGE_TIER_MIN, Math.min(CAST_RANGE_TIER_MAX, player.getPersistentData().getInt(TAG_LIMITLESS_CAST_RANGE_TIER)));
    }

    @Override
    protected void castSkillSlot(ServerPlayer player, int slot)
    {
        if (slot == SLOT_BLUE_NORMAL)
        {
            castBlueNormal(player, false);
        }
        else if (slot == SLOT_RED_NORMAL)
        {
            castRedNormal(player, false);
        }
        else if (slot == SLOT_PURPLE_NORMAL)
        {
            castPurpleMiniNormal(player);
        }
    }

    /**
     * Shift + 槽位数字：苍/赫机动变体、Shift+3 茈升空悬停变体（独立解锁字段；咒力、冷却、等级仍与同数字槽位一致）。
     */
    @Override
    protected void castSpecialMove(ServerPlayer player, SpecialMoveInput input)
    {
        if (input.modifierMask() != SpecialMoveInput.MOD_SHIFT)
        {
            return;
        }
        int slot = input.baseSlot();
        if (slot == SLOT_BLUE_NORMAL)
        {
            castWithSlotEconomy(player, slot, () -> castBlueNormal(player, true), LimitlessTechniqueSkillSet::isShiftVariantUnlockedForSlot1);
        }
        else if (slot == SLOT_RED_NORMAL)
        {
            castWithSlotEconomy(player, slot, () -> castRedNormal(player, true), LimitlessTechniqueSkillSet::isShiftVariantUnlockedForSlot2);
        }
        else if (slot == SLOT_PURPLE_NORMAL)
        {
            castWithSlotEconomy(player, slot, () -> LimitlessPurpleShiftSpecialEvents.begin(player), LimitlessTechniqueSkillSet::isShiftVariantUnlockedForSlot3);
        }
    }

    /** 与 {@link #TAG_LIMITLESS_UNLOCK_SLOT_1} 分离：仅控制 Shift+1 苍变体。 */
    private static boolean isShiftVariantUnlockedForSlot1(Player player)
    {
        return !player.getPersistentData().contains(TAG_LIMITLESS_UNLOCK_VARIANT_SHIFT_SLOT_1)
                || player.getPersistentData().getBoolean(TAG_LIMITLESS_UNLOCK_VARIANT_SHIFT_SLOT_1);
    }

    private static boolean isShiftVariantUnlockedForSlot2(Player player)
    {
        return !player.getPersistentData().contains(TAG_LIMITLESS_UNLOCK_VARIANT_SHIFT_SLOT_2)
                || player.getPersistentData().getBoolean(TAG_LIMITLESS_UNLOCK_VARIANT_SHIFT_SLOT_2);
    }

    private static boolean isShiftVariantUnlockedForSlot3(Player player)
    {
        return !player.getPersistentData().contains(TAG_LIMITLESS_UNLOCK_VARIANT_SHIFT_SLOT_3)
                || player.getPersistentData().getBoolean(TAG_LIMITLESS_UNLOCK_VARIANT_SHIFT_SLOT_3);
    }

    @Override
    protected int getSlotCost(int slot)
    {
        return switch (slot)
        {
            case SLOT_BLUE_NORMAL -> BLUE_COST;
            case SLOT_RED_NORMAL -> RED_COST;
            case SLOT_PURPLE_NORMAL -> PURPLE_COST;
            default -> 0;
        };
    }

    @Override
    protected int getSlotCooldownTicks(Player player, int slot)
    {
        if (slot == SLOT_PURPLE_NORMAL)
        {
            return LimitlessPurpleSkillGrowth.cooldownTicks(player);
        }
        return super.getSlotCooldownTicks(player, slot);
    }

    @Override
    protected int getSkillCooldownTicks(Player player, TechniqueSkill skill)
    {
        if (SKILL_PURPLE.equals(skill.skillId()))
        {
            return LimitlessPurpleSkillGrowth.cooldownTicks(player);
        }
        return super.getSkillCooldownTicks(player, skill);
    }

    @Override
    protected int getSlotCooldownTicks(int slot)
    {
        return switch (slot)
        {
            case SLOT_BLUE_NORMAL -> BLUE_COOLDOWN_TICKS;
            case SLOT_RED_NORMAL -> RED_COOLDOWN_TICKS;
            case SLOT_PURPLE_NORMAL -> PURPLE_COOLDOWN_TICKS;
            default -> 0;
        };
    }

    @Override
    protected boolean isSlotUnlocked(Player player, int slot)
    {
        TechniqueSkill skill = switch (slot)
        {
            case SLOT_BLUE_NORMAL -> SKILLS.get(0);
            case SLOT_RED_NORMAL -> SKILLS.get(1);
            case SLOT_PURPLE_NORMAL -> SKILLS.get(2);
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
            case SLOT_BLUE_NORMAL -> TAG_LIMITLESS_LEVEL_SLOT_1;
            case SLOT_RED_NORMAL -> TAG_LIMITLESS_LEVEL_SLOT_2;
            case SLOT_PURPLE_NORMAL -> TAG_LIMITLESS_LEVEL_SLOT_3;
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
            case SLOT_BLUE_NORMAL -> TAG_LIMITLESS_COOLDOWN_SLOT_1;
            case SLOT_RED_NORMAL -> TAG_LIMITLESS_COOLDOWN_SLOT_2;
            case SLOT_PURPLE_NORMAL -> TAG_LIMITLESS_COOLDOWN_SLOT_3;
            default -> null;
        };
    }

    @Override
    public String getSlotDisplayName(int slot)
    {
        return switch (slot)
        {
            case SLOT_BLUE_NORMAL -> "苍";
            case SLOT_RED_NORMAL -> "赫";
            case SLOT_PURPLE_NORMAL -> "茈";
            default -> null;
        };
    }

    @Override
    public String getSlotIconPath(int slot)
    {
        return switch (slot)
        {
            case SLOT_BLUE_NORMAL -> "textures/gui/technique/limitless_blue.png";
            case SLOT_RED_NORMAL -> "textures/gui/technique/limitless_red.png";
            case SLOT_PURPLE_NORMAL -> "textures/gui/technique/limitless_purple.png";
            default -> null;
        };
    }

    private void castBlueNormal(ServerPlayer player, boolean allowOwnerPull)
    {
        ServerLevel level = player.serverLevel();
        double effectiveRange = getEffectiveCastRange(player, BLUE_RANGE);
        Vec3 targetPos = resolveCrosshairPosition(player, effectiveRange);
        SpecialEffectEntity entity = ModEntities.SPECIAL_EFFECT_ENTITY.get().create(level);
        if (entity == null)
        {
            return;
        }

        entity.moveTo(targetPos.x, targetPos.y, targetPos.z, player.getYRot(), 0.0F);
        entity.configureLimitlessBlueNormal(player, allowOwnerPull);
        if (!level.addFreshEntity(entity))
        {
            return;
        }

        level.playSound(
                null,
                targetPos.x,
                targetPos.y,
                targetPos.z,
                ModSounds.LIMITLESS_BLUE_INHALE.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F
        );

        CommandSourceStack commandSource = entity.createCommandSourceStack()
                .withPermission(2)
                .withSuppressedOutput();
        level.getServer().getCommands().performPrefixedCommand(commandSource, "photon fx photon:limitless_blue_1 entity @s");
    }

    private void castRedNormal(ServerPlayer player, boolean allowOwnerKnockback)
    {
        EpicFightCompat.playLimitlessAkaShortAnimation(player);

        ServerLevel level = player.serverLevel();
        level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                ModSounds.LIMITLESS_AKA_SHORT_CHARGE.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F);

        Vec3 spawn = player.getEyePosition(1.0F)
                .add(player.getLookAngle().scale(RED_SPAWN_FROM_EYE))
                .add(0.0D, RED_SPAWN_Y_OFFSET, 0.0D);
        SpecialEffectEntity entity = ModEntities.SPECIAL_EFFECT_ENTITY.get().create(level);
        if (entity == null)
        {
            return;
        }

        entity.moveTo(spawn.x, spawn.y, spawn.z, player.getYRot(), player.getXRot());
        entity.configureLimitlessRedNormal(player, allowOwnerKnockback);
        if (!level.addFreshEntity(entity))
        {
            return;
        }
    }

    /** 超远程虚式茈：长按 5 秒起手演出，满后自动进入发射阶段。 */
    private void beginRemoteMurasakiHoldRelease(ServerPlayer player, TechniqueSkill skill, int slot, long startTick)
    {
        ServerLevel level = player.serverLevel();
        level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                ModSounds.LIMITLESS_REMOTE_MURASAKI.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F);

        SpecialEffectEntity entity = ModEntities.SPECIAL_EFFECT_ENTITY.get().create(level);
        if (entity == null)
        {
            return;
        }

        entity.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        entity.configureLimitlessRemoteMurasaki(player);
        if (!level.addFreshEntity(entity))
        {
            return;
        }

        PlayerHoldReleaseChargeData.beginCharge(player, slot, skill.skillId(), entity.getUUID(), startTick);
    }

    private void launchRemoteMurasakiHoldRelease(ServerPlayer player, UUID entityUuid)
    {
        SpecialEffectEntity entity = findRemoteMurasakiChargeEntity(player, entityUuid);
        if (entity == null)
        {
            return;
        }
        if (!(entity.getEffectBehavior() instanceof LimitlessRemoteMurasakiBehavior behavior))
        {
            entity.discard();
            return;
        }
        behavior.launchFromHoldRelease(entity);
    }

    private void discardRemoteMurasakiChargeEntity(ServerPlayer player)
    {
        if (!PlayerHoldReleaseChargeData.isCharging(player))
        {
            return;
        }
        discardRemoteMurasakiChargeEntity(player, PlayerHoldReleaseChargeData.getChargeEntityUuid(player));
    }

    private void discardRemoteMurasakiChargeEntity(ServerPlayer player, UUID entityUuid)
    {
        abortRemoteMurasakiStartPresentation(player);
        SpecialEffectEntity entity = findRemoteMurasakiChargeEntity(player, entityUuid);
        if (entity != null)
        {
            entity.discard();
        }
    }

    /** 中止起手演出：停止起手动画与起手音效。 */
    public static void abortRemoteMurasakiStartPresentation(ServerPlayer player)
    {
        EpicFightCompat.stopRemoteMurasakiStartAnimation(player);
        stopRemoteMurasakiStartSound(player);
    }

    private static void stopRemoteMurasakiStartSound(ServerPlayer player)
    {
        player.connection.send(new ClientboundStopSoundPacket(
                ModSounds.LIMITLESS_REMOTE_MURASAKI.get().getLocation(),
                SoundSource.PLAYERS));
    }

    @Nullable
    private SpecialEffectEntity findRemoteMurasakiChargeEntity(ServerPlayer player, UUID entityUuid)
    {
        Entity entity = player.serverLevel().getEntity(entityUuid);
        if (entity instanceof SpecialEffectEntity fx
                && fx.getEffectBehavior() instanceof LimitlessRemoteMurasakiBehavior)
        {
            return fx;
        }
        return null;
    }

    private int getRemainingSkillCooldownTicks(ServerPlayer player, TechniqueSkill skill, long now)
    {
        String key = skill.cooldownTag();
        if (key == null || key.isEmpty())
        {
            return 0;
        }
        long expire = player.getPersistentData().getLong(key);
        return expire > now ? (int) (expire - now) : 0;
    }

    private void setSkillCooldown(ServerPlayer player, TechniqueSkill skill, long expireTick)
    {
        String key = skill.cooldownTag();
        if (key != null && !key.isEmpty())
        {
            player.getPersistentData().putLong(key, expireTick);
        }
    }

    private static String formatCooldownSeconds(int ticks)
    {
        return Integer.toString((ticks + 19) / 20);
    }

    /** 茈普通释放：胸前蓄力锚点，蓄力结束后发射茈箭（与 Shift+3 升空悬停共用）。 */
    public void castPurpleMiniNormal(ServerPlayer player)
    {
        ServerLevel level = player.serverLevel();
        Vec3 chest = SpecialEffectEntity.limitlessPurpleMiniAnchorWorldPosition(player);
        SpecialEffectEntity entity = ModEntities.SPECIAL_EFFECT_ENTITY.get().create(level);
        if (entity == null)
        {
            return;
        }

        entity.moveTo(chest.x, chest.y, chest.z, player.getYRot(), player.getXRot());
        entity.configureLimitlessPurpleMiniNormal(player);
        if (!level.addFreshEntity(entity))
        {
            return;
        }

        CommandSourceStack commandSource = entity.createCommandSourceStack()
                .withPermission(2)
                .withSuppressedOutput();
        level.getServer().getCommands().performPrefixedCommand(commandSource, "photon fx photon:limitless_purple_mini_fire entity @s");
    }

    private static Vec3 resolveCrosshairPosition(ServerPlayer player, double maxDistance)
    {
        HitResult hitResult = player.pick(maxDistance, 1.0F, false);
        if (hitResult.getType() != HitResult.Type.MISS)
        {
            return hitResult.getLocation();
        }
        return player.getEyePosition().add(player.getLookAngle().scale(maxDistance));
    }

    private double getEffectiveCastRange(ServerPlayer player, double baseRange)
    {
        int tier = getCastRangeTier(player);
        double ratio = tier * 0.25D;
        return baseRange * ratio;
    }
}
