package com.darkness.jujutsukaisenzone.technique.skillset;

import com.darkness.jujutsukaisenzone.api.mimicry.MimicryVoiceLineEvents;
import com.darkness.jujutsukaisenzone.api.mimicry.MimicryVoiceLines;
import com.darkness.jujutsukaisenzone.compat.EpicFightCompat;
import com.darkness.jujutsukaisenzone.entity.SpecialEffectEntity;
import com.darkness.jujutsukaisenzone.entity.behavior.ShrineFugaBehavior;
import com.darkness.jujutsukaisenzone.entity.behavior.ShrineHachiBehavior;
import com.darkness.jujutsukaisenzone.entity.behavior.ShrineKaiArrowBehavior;
import com.darkness.jujutsukaisenzone.entity.behavior.ShrineKaiDelayBehavior;
import com.darkness.jujutsukaisenzone.entity.data.ShrineFugaConfig;
import com.darkness.jujutsukaisenzone.player.BarrierTechniqueLevel;
import com.darkness.jujutsukaisenzone.player.PlayerCursedEnergyData;
import com.darkness.jujutsukaisenzone.player.PlayerHoldReleaseChargeData;
import com.darkness.jujutsukaisenzone.player.PlayerMimicryData;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import com.darkness.jujutsukaisenzone.player.SeanceTechniqueLevel;
import com.darkness.jujutsukaisenzone.player.mimicry.MimicryCharacterRegistry;
import com.darkness.jujutsukaisenzone.registry.ModEntities;
import com.darkness.jujutsukaisenzone.registry.ModSounds;
import com.darkness.jujutsukaisenzone.technique.development.SkillUnlockLogic;
import com.darkness.jujutsukaisenzone.technique.development.SkillTreeNodeDef;
import com.darkness.jujutsukaisenzone.technique.development.SkillTreeReferenceKind;
import com.darkness.jujutsukaisenzone.technique.development.TechniqueSkillTree;
import com.darkness.jujutsukaisenzone.technique.development.UnlockCondition;
import com.darkness.jujutsukaisenzone.technique.se.SeMechanicRegistry;
import com.darkness.jujutsukaisenzone.technique.se.ShrineFinishCutMarks;
import com.darkness.jujutsukaisenzone.technique.se.ShrineFinishCutSeMechanic;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * 御厨子（伏魔神龛）术式：解、捌等招式。
 * <p>
 * 解（槽位1）：播放斩击动画后延迟发射无碰撞斩击箭，飞行途中柱形清场，到达射程终点后挂载 Photon。
 * 施法距离由鼠标侧键档位控制。
 * <p>
 * 「解」切割：固定射程 50，特效实体沿视线飞出并挂 Photon，横斩柱形清场。
 * <p>
 * 捌（槽位3）：施法者前方 2 格内有生物时可发动，对目标挂 slash_hachi Photon 并持续 3 秒周期伤害。
 * 装载 SE「收刀」时，还可对带开刀印的准星目标远程收刀（见 {@link ShrineFinishCutSeMechanic}）。
 */
public final class ShrineTechniqueSkillSet extends AbstractInnateTechniqueSkillSet
{
    public static final ShrineTechniqueSkillSet INSTANCE = new ShrineTechniqueSkillSet();

    private static final int SLOT_KAI = 1;
    private static final int SLOT_KAI_FLYOBJ = 2;
    private static final int SLOT_HACHI = 3;
    private static final int SLOT_FUGA = 4;
    private static final int KAI_COST = 40;
    private static final int KAI_FLYOBJ_COST = 40;
    private static final int HACHI_COST = 40;
    private static final int FUGA_COST = 40;
    private static final int KAI_COOLDOWN_TICKS = 0;
    private static final int KAI_FLYOBJ_COOLDOWN_TICKS = 0;
    private static final int HACHI_COOLDOWN_TICKS = 0;
    private static final int FUGA_COOLDOWN_TICKS = 0;
    private static final String TAG_SHRINE_UNLOCK_SLOT_1 = "jujutsukaisenzone_shrine_unlock_slot_1";
    private static final String TAG_SHRINE_UNLOCK_SLOT_2 = "jujutsukaisenzone_shrine_unlock_slot_2";
    private static final String TAG_SHRINE_UNLOCK_SLOT_3 = "jujutsukaisenzone_shrine_unlock_slot_3";
    private static final String TAG_SHRINE_UNLOCK_SLOT_4 = "jujutsukaisenzone_shrine_unlock_slot_4";
    private static final String TAG_SHRINE_LEVEL_SLOT_1 = "jujutsukaisenzone_shrine_level_slot_1";
    private static final String TAG_SHRINE_LEVEL_SLOT_2 = "jujutsukaisenzone_shrine_level_slot_2";
    private static final String TAG_SHRINE_LEVEL_SLOT_3 = "jujutsukaisenzone_shrine_level_slot_3";
    private static final String TAG_SHRINE_LEVEL_SLOT_4 = "jujutsukaisenzone_shrine_level_slot_4";
    private static final String TAG_SHRINE_COOLDOWN_SLOT_1 = "jujutsukaisenzone_shrine_cooldown_slot_1";
    private static final String TAG_SHRINE_COOLDOWN_SLOT_2 = "jujutsukaisenzone_shrine_cooldown_slot_2";
    private static final String TAG_SHRINE_COOLDOWN_SLOT_3 = "jujutsukaisenzone_shrine_cooldown_slot_3";
    private static final String TAG_SHRINE_COOLDOWN_SLOT_4 = "jujutsukaisenzone_shrine_cooldown_slot_4";
    private static final String TAG_SHRINE_CAST_RANGE_TIER = "jujutsukaisenzone_shrine_cast_range_tier";
    public static final int CAST_RANGE_TIER_MIN = 1;
    public static final int CAST_RANGE_TIER_MAX = 4;
    private static final double KAI_BASE_RANGE = 25.0D;
    /** 「解」切割固定射程，不受施法距离档位影响。 */
    private static final double KAI_FLYOBJ_RANGE = 50.0D;
    /** 捌：施法者前方该距离（格）内的可攻击生物。 */
    private static final double HACHI_CAST_RANGE = 2.0D;

    // 技能 ID 常量（配合快捷栏配置使用）
    public static final String SKILL_KAI = "shrine_kai";
    public static final String SKILL_KAI_FLYOBJ = "shrine_kai_flyobj";
    public static final String SKILL_HACHI = "shrine_hachi";
    public static final String SKILL_FUGA = "shrine_fuga";
    public static final String REF_BLACK_FLASH_3 = "ref_shrine_black_flash_3";
    public static final String REF_SEANCE_COMPLETE = "ref_seance_technique_complete";
    /** 领域「伏魔神龛」在开发树上的 nodeId。 */
    public static final String NODE_DOMAIN_FUMOSHINKAN = "shrine_domain_fumoshinkan";

    /**
     * 领域「伏魔神龛」的 skillId。
     * <p>
     * 领域不是可释放技能，但开发树上的节点是 <b>skill 节点</b>（不是说明节点）：它是一个
     * 机制型技能，随「习得领域展开」自动点亮（{@code autoUnlockWhenConditionsMet}），
     * 玩家不花术式情报。瞬发 / 开放式一类领域模式节点都以它为前置。
     * 「本领域斩击不参与暴击」这条标注因此可以按 skillId 定位，见 {@code CritSkillPolicy}。
     */
    public static final String SKILL_DOMAIN = "shrine_domain";

    /** 机制型：解锁「领域模式 · 瞬发」（叙事名「真正的咒术」）。 */
    public static final String SKILL_INSTANT_DOMAIN = "shrine_instant_domain";
    /**
     * 「结界术达到掌握」的展示引用节点 nodeId。
     * <p>
     * 与无下限树里的同名节点是两棵树各自的节点，故这里另取一个 id，避免任何按 nodeId 的查表串台。
     */
    public static final String REF_BARRIER_MASTERED = "ref_shrine_barrier_mastered";

    private static final String TAG_DOMAIN_UNLOCK = "jujutsukaisenzone_shrine_domain_unlock";
    private static final String TAG_INSTANT_DOMAIN_UNLOCK = "jujutsukaisenzone_shrine_instant_domain_unlock";
    private static final String ICON_DOMAIN = "textures/item/slash_technique_sukuna.png";
    private static final String ICON_INSTANT_DOMAIN = "textures/item/slash_technique_sukuna.png";

    /**
     * 机制型：领域「伏魔神龛」在开发树上的落点。
     * <p>
     * 不占槽位、无咒力、无冷却。解锁与否<b>不写在存档里</b> —— 节点走
     * {@code autoUnlockWhenConditionsMet}，每次判定都实时看「有没有习得领域展开」，
     * 所以领域被忘掉 / 改动时节点跟着灭，不需要额外维护一份开关。
     */
    private static final TechniqueSkill MECHANIC_DOMAIN = new TechniqueSkill(
            SKILL_DOMAIN, 0, Component.literal("伏魔神龛"), ICON_DOMAIN,
            0, 0, 0, "", TAG_DOMAIN_UNLOCK, "");

    /** 机制型技能：无槽位、无咒力、无冷却，只做解锁开关（与无下限「0.2秒的领域展开」同口径）。 */
    private static final TechniqueSkill MECHANIC_INSTANT_DOMAIN = new TechniqueSkill(
            SKILL_INSTANT_DOMAIN, 0, Component.literal("真正的咒术"), ICON_INSTANT_DOMAIN,
            0, 0, 0, "", TAG_INSTANT_DOMAIN_UNLOCK, "");

    private static final List<TechniqueSkill> MECHANICS = List.of(MECHANIC_DOMAIN, MECHANIC_INSTANT_DOMAIN);

    private static final List<TechniqueSkill> SKILLS = Arrays.asList(
            new TechniqueSkill(SKILL_KAI, 1, Component.literal("「解」挥砍"),
                    "textures/gui/technique/slash_kai_1.png",
                    KAI_COST, KAI_COOLDOWN_TICKS, 0, // 暂不支持最大输出
                    TAG_SHRINE_COOLDOWN_SLOT_1, TAG_SHRINE_UNLOCK_SLOT_1, TAG_SHRINE_LEVEL_SLOT_1),
            new TechniqueSkill(SKILL_KAI_FLYOBJ, 2, Component.literal("「解」切割"),
                    "textures/gui/technique/kai_flyobj.png",
                    KAI_FLYOBJ_COST, KAI_FLYOBJ_COOLDOWN_TICKS, 0,
                    TAG_SHRINE_COOLDOWN_SLOT_2, TAG_SHRINE_UNLOCK_SLOT_2, TAG_SHRINE_LEVEL_SLOT_2),
            new TechniqueSkill(SKILL_HACHI, 3, Component.literal("「捌」"),
                    "textures/gui/technique/slash_hachi.png",
                    HACHI_COST, HACHI_COOLDOWN_TICKS, 0,
                    TAG_SHRINE_COOLDOWN_SLOT_3, TAG_SHRINE_UNLOCK_SLOT_3, TAG_SHRINE_LEVEL_SLOT_3),
            new TechniqueSkill(SKILL_FUGA, 4, Component.literal("「灶」开"),
                    "textures/gui/technique/fuga.png",
                    FUGA_COST, FUGA_COOLDOWN_TICKS, 0,
                    TAG_SHRINE_COOLDOWN_SLOT_4, TAG_SHRINE_UNLOCK_SLOT_4, TAG_SHRINE_LEVEL_SLOT_4)
    );

    private static final TechniqueSkillTree SKILL_TREE = TechniqueSkillTree.builder(PlayerTechniqueData.TECHNIQUE_SHRINE)
            .node(SkillTreeNodeDef.skill("shrine_kai", SKILL_KAI).autoGranted().build())
            .node(SkillTreeNodeDef.skill("shrine_kai_flyobj", SKILL_KAI_FLYOBJ)
                    .requires("shrine_kai")
                    .intelCost(200)
                    .build())
            .node(SkillTreeNodeDef.reference(
                    REF_BLACK_FLASH_3,
                    SkillTreeReferenceKind.BLACK_FLASH_COUNT,
                    "3"
            ).requires("shrine_kai_flyobj").build())
            .node(SkillTreeNodeDef.skill("shrine_hachi", SKILL_HACHI)
                    .requires("shrine_kai_flyobj", REF_BLACK_FLASH_3)
                    .intelCost(200)
                    .build())
            .node(SkillTreeNodeDef.reference(
                    REF_SEANCE_COMPLETE,
                    SkillTreeReferenceKind.SEANCE_TECHNIQUE_LEVEL,
                    SeanceTechniqueLevel.COMPLETE.name()
            ).build())
            .node(SkillTreeNodeDef.skill("shrine_fuga", SKILL_FUGA)
                    .requires(REF_SEANCE_COMPLETE)
                    .intelCost(600)
                    .build())
            /*
             * 领域「伏魔神龛」：习得领域展开的<b>瞬间自动点亮</b>，不消耗术式情报、也不需要点选。
             * 它是瞬发 / 开放式一类领域模式节点的共同前置，所以必须是真实的 skill 节点
             * （说明节点不参与连线与解锁判定，挂不住任何东西）。
             */
            .node(SkillTreeNodeDef.skill(NODE_DOMAIN_FUMOSHINKAN, SKILL_DOMAIN)
                    .requires("shrine_hachi")
                    .conditions(UnlockCondition.hasDomainExpansion())
                    .visibleWhen(UnlockCondition.hasDomainExpansion())
                    .autoUnlockWhenConditionsMet()
                    .build())
            .node(SkillTreeNodeDef.reference(
                    REF_BARRIER_MASTERED,
                    SkillTreeReferenceKind.BARRIER_TECHNIQUE_LEVEL,
                    BarrierTechniqueLevel.MASTERED.name()
            ).requires(NODE_DOMAIN_FUMOSHINKAN).build())
            /*
             * 机制型：「真正的咒术」——解锁通用圆盘「领域模式」的瞬发档。
             * 条件与无下限「0.2秒的领域展开」一致：已习得领域展开 + 结界术达到掌握；
             * 前置节点用引用节点展示结界等级。领域节点本身在未习得领域展开时隐藏，故这里同步隐藏。
             */
            .node(SkillTreeNodeDef.skill("shrine_instant_domain", SKILL_INSTANT_DOMAIN)
                    .requires(NODE_DOMAIN_FUMOSHINKAN, REF_BARRIER_MASTERED)
                    .conditions(
                            UnlockCondition.hasDomainExpansion(),
                            UnlockCondition.minBarrierTechniqueLevel(BarrierTechniqueLevel.MASTERED))
                    .visibleWhen(UnlockCondition.hasDomainExpansion())
                    .intelCost(300)
                    .build())
            .build();

    static
    {
        SKILL_TREE.validateAgainstSkills(Stream.concat(SKILLS.stream(), MECHANICS.stream()).toList());
    }

    private ShrineTechniqueSkillSet()
    {
    }

    @Override
    public String techniqueId()
    {
        return PlayerTechniqueData.TECHNIQUE_SHRINE;
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
    public boolean isSkillVisibleInSkillBar(Player player, TechniqueSkill skill)
    {
        if (!SKILL_FUGA.equals(skill.skillId()))
        {
            return true;
        }
        return PlayerTechniqueData.TECHNIQUE_SHRINE.equals(PlayerTechniqueData.getInnateTechnique(player))
                && PlayerMimicryData.isActive(player)
                && MimicryCharacterRegistry.CHARACTER_SUKUNA.equals(PlayerMimicryData.getActiveCharacterId(player));
    }

    @Override
    public Optional<TechniqueSkillTree> getSkillTree()
    {
        return Optional.of(SKILL_TREE);
    }

    @Override
    protected void castSkill(ServerPlayer player, String skillId)
    {
        if (SKILL_KAI.equals(skillId))
        {
            castKai(player);
        }
        else if (SKILL_KAI_FLYOBJ.equals(skillId))
        {
            castKaiFlyobj(player);
        }
        else if (SKILL_HACHI.equals(skillId))
        {
            castHachi(player);
        }
    }

    @Override
    public int getHoldReleaseChargeTicksBySkill(Player player, String skillId)
    {
        if (!SKILL_FUGA.equals(skillId))
        {
            return 0;
        }
        TechniqueSkill skill = findSkillById(skillId);
        if (skill == null || !isSkillUnlocked(player, skill) || getSkillLevel(player, skill) <= 0)
        {
            return 0;
        }
        return ShrineFugaConfig.CHARGE_TICKS;
    }

    @Override
    public void beginHoldReleaseChargeBySkill(ServerPlayer player, String skillId, int slot)
    {
        if (!SKILL_FUGA.equals(skillId))
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
        beginFugaHoldRelease(player, skill, slot, now);
    }

    @Override
    public void cancelHoldReleaseChargeBySkill(ServerPlayer player, String skillId, int slot)
    {
        if (!PlayerHoldReleaseChargeData.isCharging(player) || PlayerHoldReleaseChargeData.getChargingSlot(player) != slot)
        {
            return;
        }
        if (!SKILL_FUGA.equals(PlayerHoldReleaseChargeData.getChargingSkillId(player)))
        {
            return;
        }
        discardFugaChargeEntity(player);
        PlayerHoldReleaseChargeData.clear(player);
    }

    @Override
    public void releaseHoldReleaseChargeBySkill(ServerPlayer player, String skillId, int slot)
    {
        if (!PlayerHoldReleaseChargeData.isCharging(player) || PlayerHoldReleaseChargeData.getChargingSlot(player) != slot)
        {
            return;
        }
        if (!SKILL_FUGA.equals(skillId) || !SKILL_FUGA.equals(PlayerHoldReleaseChargeData.getChargingSkillId(player)))
        {
            return;
        }
        TechniqueSkill skill = findSkillById(skillId);
        if (skill == null)
        {
            PlayerHoldReleaseChargeData.clear(player);
            return;
        }
        long now = player.level().getGameTime();
        long heldTicks = now - PlayerHoldReleaseChargeData.getChargeStartGameTick(player);
        UUID entityUuid = PlayerHoldReleaseChargeData.getChargeEntityUuid(player);
        PlayerHoldReleaseChargeData.clear(player);
        if (heldTicks < ShrineFugaConfig.CHARGE_TICKS)
        {
            discardFugaChargeEntity(player, entityUuid);
            return;
        }
        if (!isSkillUnlocked(player, skill) || getSkillLevel(player, skill) <= 0)
        {
            discardFugaChargeEntity(player, entityUuid);
            return;
        }
        if (usesSkillCooldown(player))
        {
            int remain = getRemainingSkillCooldownTicks(player, skill, now);
            if (remain > 0)
            {
                player.sendSystemMessage(Component.literal("招式冷却中，未能完成释放。"));
                discardFugaChargeEntity(player, entityUuid);
                return;
            }
        }
        if (!player.isCreative())
        {
            if (skill.cost() > 0 && !PlayerCursedEnergyData.tryConsume(player, skill.cost()))
            {
                player.sendSystemMessage(Component.literal("咒力不足，未能完成释放。"));
                discardFugaChargeEntity(player, entityUuid);
                return;
            }
        }
        if (usesSkillCooldown(player))
        {
            setSkillCooldown(player, skill, now + skill.cooldownTicks());
        }
        if (abortCastIfBrainDamageFailed(player))
        {
            discardFugaChargeEntity(player, entityUuid);
            return;
        }
        launchFugaHoldRelease(player, entityUuid);
    }

    // 本术式暂无特别招式变体：不覆写 castSpecialMoveBySkill，也不声明 getSpecialMoves。
    // 未声明 = 基类 acceptsSpecialMove 在结算咒力与冷却之前就把修饰键组合静默拦下，
    // 所以「Shift/Ctrl/Alt + 数字键」不会白扣一次咒力（这里以前是个空的覆写，会白扣）。

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
            player.getPersistentData().putInt(TAG_SHRINE_CAST_RANGE_TIER, next);
        }
        return next;
    }

    @Override
    public int getCastRangeTier(ServerPlayer player)
    {
        if (!player.getPersistentData().contains(TAG_SHRINE_CAST_RANGE_TIER))
        {
            return CAST_RANGE_TIER_MAX;
        }
        return Math.max(CAST_RANGE_TIER_MIN, Math.min(CAST_RANGE_TIER_MAX, player.getPersistentData().getInt(TAG_SHRINE_CAST_RANGE_TIER)));
    }

    @Override
    protected void castSkillSlot(ServerPlayer player, int slot)
    {
        if (slot == SLOT_KAI)
        {
            castKai(player);
        }
        else if (slot == SLOT_KAI_FLYOBJ)
        {
            castKaiFlyobj(player);
        }
        else if (slot == SLOT_HACHI)
        {
            castHachi(player);
        }
    }

    @Override
    protected void castSpecialMove(ServerPlayer player, SpecialMoveInput input)
    {
        // 暂无特别招式变体
    }

    @Override
    protected int getSlotCost(int slot)
    {
        return switch (slot)
        {
            case SLOT_KAI -> KAI_COST;
            case SLOT_KAI_FLYOBJ -> KAI_FLYOBJ_COST;
            case SLOT_HACHI -> HACHI_COST;
            case SLOT_FUGA -> FUGA_COST;
            default -> 0;
        };
    }

    @Override
    protected int getSlotCooldownTicks(int slot)
    {
        return switch (slot)
        {
            case SLOT_KAI -> KAI_COOLDOWN_TICKS;
            case SLOT_KAI_FLYOBJ -> KAI_FLYOBJ_COOLDOWN_TICKS;
            case SLOT_HACHI -> HACHI_COOLDOWN_TICKS;
            case SLOT_FUGA -> FUGA_COOLDOWN_TICKS;
            default -> 0;
        };
    }

    @Override
    protected boolean isSlotUnlocked(Player player, int slot)
    {
        TechniqueSkill skill = switch (slot)
        {
            case SLOT_KAI -> SKILLS.get(0);
            case SLOT_KAI_FLYOBJ -> SKILLS.get(1);
            case SLOT_HACHI -> SKILLS.get(2);
            case SLOT_FUGA -> SKILLS.get(3);
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
            case SLOT_KAI -> TAG_SHRINE_LEVEL_SLOT_1;
            case SLOT_KAI_FLYOBJ -> TAG_SHRINE_LEVEL_SLOT_2;
            case SLOT_HACHI -> TAG_SHRINE_LEVEL_SLOT_3;
            case SLOT_FUGA -> TAG_SHRINE_LEVEL_SLOT_4;
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
            case SLOT_KAI -> TAG_SHRINE_COOLDOWN_SLOT_1;
            case SLOT_KAI_FLYOBJ -> TAG_SHRINE_COOLDOWN_SLOT_2;
            case SLOT_HACHI -> TAG_SHRINE_COOLDOWN_SLOT_3;
            case SLOT_FUGA -> TAG_SHRINE_COOLDOWN_SLOT_4;
            default -> null;
        };
    }

    @Override
    public String getSlotDisplayName(int slot)
    {
        return switch (slot)
        {
            case SLOT_KAI -> "「解」挥砍";
            case SLOT_KAI_FLYOBJ -> "「解」切割";
            case SLOT_HACHI -> "「捌」";
            case SLOT_FUGA -> "「灶」开";
            default -> null;
        };
    }

    @Override
    public String getSlotIconPath(int slot)
    {
        return switch (slot)
        {
            case SLOT_KAI -> "textures/gui/technique/slash_kai_1.png";
            case SLOT_KAI_FLYOBJ -> "textures/gui/technique/kai_flyobj.png";
            case SLOT_HACHI -> "textures/gui/technique/slash_hachi.png";
            case SLOT_FUGA -> "textures/gui/technique/fuga.png";
            default -> null;
        };
    }

    /**
     * 解：t=0 播放斩击动画并登记延迟发射；t=8 在脚前方 1 格生成斩击箭并按当时视线飞出。
     * 随机选择竖斩/横斩。
     */
    private void castKai(ServerPlayer player)
    {
        ServerLevel level = player.serverLevel();
        double maxRange = getEffectiveCastRange(player, KAI_BASE_RANGE);

        boolean horizontal = level.random.nextBoolean();
        if (horizontal)
        {
            EpicFightCompat.playSlashKai2Animation(player);
        }
        else
        {
            EpicFightCompat.playSlashKai1Animation(player);
        }

        MimicryVoiceLines.tryPlay(player, MimicryVoiceLineEvents.SHRINE_KAI_SLASH);

        SpecialEffectEntity entity = ModEntities.SPECIAL_EFFECT_ENTITY.get().create(level);
        if (entity == null)
        {
            return;
        }

        entity.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        ShrineKaiDelayBehavior kaiDelay = new ShrineKaiDelayBehavior();
        kaiDelay.configure(player, maxRange, horizontal);
        entity.attach(kaiDelay);
        level.addFreshEntity(entity);
    }

    /** 「解」切割：生成并发射 flyobj 特效实体，固定射程 {@link #KAI_FLYOBJ_RANGE}。 */
    private void castKaiFlyobj(ServerPlayer player)
    {
        ServerLevel level = player.serverLevel();

        Vec3 flightDir = player.getLookAngle();
        if (flightDir.lengthSqr() < 1.0E-8D)
        {
            flightDir = player.getViewVector(1.0F);
        }
        flightDir = flightDir.normalize();

        Vec3 spawn = player.getEyePosition().add(flightDir.scale(0.5D));

        SpecialEffectEntity fx = ModEntities.SPECIAL_EFFECT_ENTITY.get().create(level);
        if (fx == null)
        {
            return;
        }

        fx.moveTo(spawn.x, spawn.y, spawn.z, player.getYRot(), player.getXRot());
        fx.setYBodyRot(player.getYRot());
        fx.setYHeadRot(player.getYRot());
        flightDir = fx.getLookAngle().normalize();
        ShrineKaiArrowBehavior kaiFlyobj = new ShrineKaiArrowBehavior();
        kaiFlyobj.configure(player, KAI_FLYOBJ_RANGE, flightDir, true, true);
        fx.attach(kaiFlyobj);
        if (!level.addFreshEntity(fx))
        {
            return;
        }

        level.playSound(
                null,
                spawn.x,
                spawn.y,
                spawn.z,
                ModSounds.SHRINE_SLASH_KAI_FLYOBJ.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F
        );
    }

    /** 「灶」开：按下开始蓄力，松手满 2 秒后发射火焰箭。 */
    private void beginFugaHoldRelease(ServerPlayer player, TechniqueSkill skill, int slot, long startTick)
    {
        ServerLevel level = player.serverLevel();

        SpecialEffectEntity entity = ModEntities.SPECIAL_EFFECT_ENTITY.get().create(level);
        if (entity == null)
        {
            return;
        }

        ShrineFugaBehavior.syncTransformWithOwner(entity, player);
        ShrineFugaBehavior fugaCharge = new ShrineFugaBehavior();
        fugaCharge.configure(player);
        entity.attach(fugaCharge);
        if (!level.addFreshEntity(entity))
        {
            return;
        }

        CommandSourceStack commandSource = entity.createCommandSourceStack()
                .withPermission(2)
                .withSuppressedOutput();
        level.getServer().getCommands().performPrefixedCommand(commandSource, ShrineFugaConfig.PHOTON_FUGA_COMMAND);
        level.playSound(
                null,
                entity.getX(),
                entity.getY(),
                entity.getZ(),
                ModSounds.SHRINE_FUGA_START.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F
        );
        PlayerHoldReleaseChargeData.beginCharge(player, slot, skill.skillId(), entity.getUUID(), startTick);
        EpicFightCompat.playFugaChargeAnimation(player);
    }

    private void launchFugaHoldRelease(ServerPlayer player, UUID entityUuid)
    {
        EpicFightCompat.stopFugaChargeAnimation(player);
        SpecialEffectEntity entity = findFugaChargeEntity(player, entityUuid);
        if (entity == null)
        {
            return;
        }
        if (!(entity.getEffectBehavior() instanceof ShrineFugaBehavior behavior))
        {
            entity.discard();
            return;
        }
        behavior.launchFromHoldRelease(entity);
        MimicryVoiceLines.tryPlay(player, MimicryVoiceLineEvents.SHRINE_FUGA_LAUNCH);
    }

    private void discardFugaChargeEntity(ServerPlayer player)
    {
        if (!PlayerHoldReleaseChargeData.isCharging(player))
        {
            return;
        }
        discardFugaChargeEntity(player, PlayerHoldReleaseChargeData.getChargeEntityUuid(player));
    }

    private void discardFugaChargeEntity(ServerPlayer player, UUID entityUuid)
    {
        EpicFightCompat.stopFugaChargeAnimation(player);
        SpecialEffectEntity entity = findFugaChargeEntity(player, entityUuid);
        if (entity != null)
        {
            entity.discard();
        }
    }

    @Nullable
    private SpecialEffectEntity findFugaChargeEntity(ServerPlayer player, UUID entityUuid)
    {
        Entity entity = player.serverLevel().getEntity(entityUuid);
        if (entity instanceof SpecialEffectEntity fx
                && fx.getEffectBehavior() instanceof ShrineFugaBehavior)
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
        double sec = ticks / 20.0D;
        return String.format(java.util.Locale.ROOT, "%.1f", sec);
    }

    /** 捌：前方近战目标，或（装「收刀」时）准星上带开刀印的远程目标。 */
    private void castHachi(ServerPlayer player)
    {
        LivingEntity target = findHachiTarget(player);
        if (target == null)
        {
            boolean finishCut = SeMechanicRegistry.findEquipped(player, ShrineFinishCutSeMechanic.class).isPresent();
            player.sendSystemMessage(Component.literal(finishCut
                    ? "没有可收刀的目标（前方或开刀印）。"
                    : "前方没有可斩击的目标。"));
            return;
        }

        ServerLevel level = player.serverLevel();

        EpicFightCompat.playSlashHachiAnimation(player);

        level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                ModSounds.SHRINE_SLASH_HACHI.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F
        );

        SpecialEffectEntity fx = ModEntities.SPECIAL_EFFECT_ENTITY.get().create(level);
        if (fx == null)
        {
            return;
        }

        fx.moveTo(target.getX(), target.getY(), target.getZ(), 0.0F, 0.0F);
        ShrineHachiBehavior hachi = new ShrineHachiBehavior();
        hachi.configure(player, target);
        fx.attach(hachi);
        level.addFreshEntity(fx);
        ShrineFinishCutMarks.consume(player, target);
    }

    @Nullable
    private LivingEntity findHachiTarget(ServerPlayer player)
    {
        LivingEntity melee = findHachiMeleeTarget(player);
        if (melee != null)
        {
            return melee;
        }
        return findHachiFinishCutTarget(player);
    }

    /** 默认捌：前方 {@link #HACHI_CAST_RANGE} 格内最近的可攻击生物。 */
    @Nullable
    private LivingEntity findHachiMeleeTarget(ServerPlayer player)
    {
        double maxDistSqr = HACHI_CAST_RANGE * HACHI_CAST_RANGE;
        Vec3 lookHoriz = horizontalLookDirection(player);
        AABB searchBox = player.getBoundingBox().inflate(HACHI_CAST_RANGE);
        LivingEntity closest = null;
        double closestDistSqr = maxDistSqr;
        for (LivingEntity entity : player.serverLevel().getEntitiesOfClass(
                LivingEntity.class,
                searchBox,
                candidate -> candidate != player && candidate.isAlive() && candidate.isAttackable()))
        {
            Vec3 toTarget = entity.position()
                    .add(0.0D, entity.getBbHeight() * 0.5D, 0.0D)
                    .subtract(player.getEyePosition());
            Vec3 toTargetHoriz = new Vec3(toTarget.x, 0.0D, toTarget.z);
            double horizDistSqr = toTargetHoriz.lengthSqr();
            if (horizDistSqr < 1.0E-8D || horizDistSqr > maxDistSqr)
            {
                continue;
            }
            if (toTargetHoriz.normalize().dot(lookHoriz) <= 0.0D)
            {
                continue;
            }
            if (horizDistSqr <= closestDistSqr)
            {
                closestDistSqr = horizDistSqr;
                closest = entity;
            }
        }
        return closest;
    }

    /**
     * SE「收刀」旁路：准星指向、视线可达、距离内、且带有本玩家开刀印的目标。
     * 未装备该 SE 时恒为 null，不改默认捌规则。
     */
    @Nullable
    private LivingEntity findHachiFinishCutTarget(ServerPlayer player)
    {
        Optional<ShrineFinishCutSeMechanic> mechanic =
                SeMechanicRegistry.findEquipped(player, ShrineFinishCutSeMechanic.class);
        if (mechanic.isEmpty())
        {
            return null;
        }
        double range = Math.max(1.0D, mechanic.get().remoteCastRange());
        LivingEntity look = pickLivingAlongLook(player, range);
        if (look == null || !ShrineFinishCutMarks.hasMark(player, look))
        {
            return null;
        }
        if (!hasLineOfSight(player, look))
        {
            return null;
        }
        return look;
    }

    @Nullable
    private static LivingEntity pickLivingAlongLook(ServerPlayer player, double maxDistance)
    {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(maxDistance));
        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(maxDistance)).inflate(1.0D);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                player,
                eye,
                end,
                searchBox,
                entity -> entity instanceof LivingEntity living
                        && living.isAlive()
                        && living.isAttackable()
                        && living != player
                        && !living.isSpectator()
                        && entity.isPickable(),
                maxDistance * maxDistance);
        if (hit != null && hit.getEntity() instanceof LivingEntity living)
        {
            return living;
        }
        return null;
    }

    private static boolean hasLineOfSight(ServerPlayer player, LivingEntity target)
    {
        Vec3 eye = player.getEyePosition();
        Vec3 aim = target.getEyePosition();
        BlockHitResult block = player.level().clip(new ClipContext(
                eye, aim, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (block.getType() == HitResult.Type.MISS)
        {
            return true;
        }
        return block.getLocation().distanceToSqr(eye) >= aim.distanceToSqr(eye) - 1.0E-3D;
    }

    /** 施法者水平朝向，用于判定目标是否在「前方」。 */
    private static Vec3 horizontalLookDirection(ServerPlayer player)
    {
        Vec3 look = player.getLookAngle();
        Vec3 horiz = new Vec3(look.x, 0.0D, look.z);
        if (horiz.lengthSqr() < 1.0E-8D)
        {
            return Vec3.atLowerCornerOf(player.getDirection().getNormal());
        }
        return horiz.normalize();
    }

    private double getEffectiveCastRange(ServerPlayer player, double baseRange)
    {
        int tier = getCastRangeTier(player);
        double ratio = tier * 0.25D;
        return baseRange * ratio;
    }
}
