package com.darkness.jujutsukaisenzone.entity;

import com.darkness.jujutsukaisenzone.entity.ai.GojoSatoruLimitlessCombatGoal;
import com.darkness.jujutsukaisenzone.entity.combat.AiLimitlessCasts;
import com.darkness.jujutsukaisenzone.entity.combat.AiTechniqueStats;
import com.darkness.jujutsukaisenzone.faction.FactionStance;
import com.darkness.jujutsukaisenzone.faction.JjkzFactions;
import com.darkness.jujutsukaisenzone.talent.InnateTalent;
import com.darkness.jujutsukaisenzone.talent.InnateTalentData;
import com.darkness.jujutsukaisenzone.talent.InnateTalentSource;
import com.darkness.jujutsukaisenzone.player.PlayerDomainModeData;
import com.darkness.jujutsukaisenzone.technique.domain.AiDomainCasterRegistry;
import com.darkness.jujutsukaisenzone.technique.domain.AiDomainModeData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 五条悟（AI 术师实体）：有 AI、能开领域放技能的正主。
 * <p>
 * 与 {@link CutsceneActorEntity} 的差别：那个是「无 AI、无重力、不进存档」的纯 Geo 演员，
 * 只能播动画、不能施法；本实体是<b>真实术师</b> —— 持有 {@link AiTechniqueStats} 特级面板、
 * 可经 {@link AiDomainCasterRegistry} 展开真正的无量空处（常规 / 瞬发 / 开放式），且走模组统一的伤害体系。
 * <p>
 * <b>数值来源</b>：独立的 {@link AiTechniqueStats}
 * （体术 {@link AiTechniqueStats#GOJO_PHYSICAL_POWER} / 术式 {@link AiTechniqueStats#GOJO_TECHNIQUE_POWER} /
 * 咒力 {@link AiTechniqueStats#GOJO_CURSED_ENERGY_MAX}），
 * 不读也不写任何玩家存档。构造时（服务端）若尚无面板则自动套用五条悟预设。
 * <p>
 * <b>模型</b>：Epic Fight {@code Meshes.BIPED}，贴图 {@code gojosatoru_teacher_noeyemask.png}。
 */
public class GojoSatoruEntity extends PathfinderMob
{
    public GojoSatoruEntity(EntityType<? extends GojoSatoruEntity> type, Level level)
    {
        super(type, level);
        this.setPersistenceRequired();
        /* 服务端构造即保证面板存在（客户端实体不落数据，无所谓）。 */
        if (!level.isClientSide)
        {
            if (!AiTechniqueStats.has(this))
            {
                AiTechniqueStats.applyGojoPreset(this);
            }
            ensureSixEyesTalent();
            JjkzFactions.assign(this, JjkzFactions.SORCERER);
        }
    }

    /**
     * 基础属性：对齐<b>特级玩家</b>的量级（见 {@code CursedGradeBonuses}）。
     * <ul>
     *   <li>生命 60000 —— 与特级玩家同一档（玩家=基础上限 20 + 等级加成 59980）；</li>
     *   <li>护甲 20 —— 与特级玩家的等级护甲加成一致；</li>
     *   <li>击退抗性 1.0 —— 满额，五条悟不会被任何击退推动；</li>
     *   <li>移速 0.32 —— 略快于普通生物。</li>
     * </ul>
     * 抗性 / 穿透不在这里写死，走 {@code mob_combat_stats} 数据包表（会被
     * {@code MobCombatStatsEvents} 转成属性修饰符挂上）。
     */
    public static AttributeSupplier.Builder createAttributes()
    {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 60000.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.ATTACK_DAMAGE, 8.0D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.ARMOR, 20.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    /**
     * 索敌：还击 + 按阵营立场锁定目标。近战追击 / 出拳由 {@link com.darkness.jujutsukaisenzone.compat.JjkzBipedPatch}
     * （EF {@code AnimatedAttackGoal}）在入世时挂上，这里不重复加原版 {@code MeleeAttackGoal}。
     */
    @Override
    protected void registerGoals()
    {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new GojoSatoruLimitlessCombatGoal(this));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, null));
    }

    @Override
    public boolean canAttack(LivingEntity target)
    {
        return super.canAttack(target) && FactionStance.canAggress(this, target);
    }

    // ==================== 面板 / 施法 ====================

    /** 确保五条悟面板已写入（幂等）。 */
    public void ensurePreset()
    {
        if (!AiTechniqueStats.has(this))
        {
            AiTechniqueStats.applyGojoPreset(this);
        }
        ensureSixEyesTalent();
    }

    private void ensureSixEyesTalent()
    {
        if (this.level().isClientSide)
        {
            return;
        }
        InnateTalentData.set(this, InnateTalent.SIX_EYES, InnateTalentSource.INNATE);
    }

    /**
     * 展开<b>真正的常规无量空处</b>（走 {@link AiDomainCasterRegistry} 完整真实演出）。
     * <p>
     * 服务端专用；成功后本实体被登记为领域施法者，由注册表逐 tick 维持。
     *
     * @return 是否成功展开
     */
    public boolean castLimitlessDomain()
    {
        if (this.level().isClientSide)
        {
            return false;
        }
        ensurePreset();
        AiDomainModeData.set(this, PlayerDomainModeData.Mode.NORMAL);
        return AiDomainCasterRegistry.castLimitlessDomain(this);
    }

    /**
     * 领域展开手势。正主外观由 Epic Fight 蓄力动画驱动
     * （{@code EpicFightCompat.playLimitlessDomainChargeAnimation}），此方法留给任务调用点。
     */
    public void playDomainGesture()
    {
    }

    /**
     * 便捷生成：把实体放到指定位置并朝向给定 yaw，然后加入世界。
     *
     * @return 已加入世界的实体
     */
    public static GojoSatoruEntity spawnAt(Level level, Vec3 pos, float yaw)
    {
        GojoSatoruEntity gojo = com.darkness.jujutsukaisenzone.registry.ModEntities.GOJO_SATORU.get().create(level);
        if (gojo == null)
        {
            return null;
        }
        gojo.moveTo(pos.x, pos.y, pos.z, yaw, 0.0F);
        gojo.ensurePreset();
        level.addFreshEntity(gojo);
        return gojo;
    }

    // ==================== 存档 ====================

    @Override
    public void addAdditionalSaveData(CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);
        /* AiTechniqueStats 存 getPersistentData()，不自动落盘 ⇒ 这里显式持久化面板 + 当前咒力。 */
        CompoundTag stats = this.getPersistentData().getCompound(AiTechniqueStats.TAG_ROOT);
        if (!stats.isEmpty())
        {
            tag.put(AiTechniqueStats.TAG_ROOT, stats.copy());
        }
        AiLimitlessCasts.copyCooldownTagsTo(tag, this);
        CompoundTag talent = this.getPersistentData().getCompound(InnateTalentData.TAG_ROOT);
        if (!talent.isEmpty())
        {
            tag.put(InnateTalentData.TAG_ROOT, talent.copy());
        }
        JjkzFactions.copyToSave(this, tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        if (!this.level().isClientSide && tag.contains(AiTechniqueStats.TAG_ROOT))
        {
            this.getPersistentData().put(AiTechniqueStats.TAG_ROOT,
                    tag.getCompound(AiTechniqueStats.TAG_ROOT).copy());
            /* set() 才会同步 LivingCombatStats，且会把咒力回满（避免读档后因咒力耗尽无法展开）。 */
            AiTechniqueStats.set(this,
                    AiTechniqueStats.physicalPower(this),
                    AiTechniqueStats.techniquePower(this),
                    AiTechniqueStats.cursedEnergyMax(this),
                    AiTechniqueStats.rank(this));
            AiLimitlessCasts.restoreCooldownTags(this, tag);
        }
        JjkzFactions.restoreFromSave(this, tag);
        JjkzFactions.assign(this, JjkzFactions.SORCERER);
        ensureSixEyesTalent();
    }
}
