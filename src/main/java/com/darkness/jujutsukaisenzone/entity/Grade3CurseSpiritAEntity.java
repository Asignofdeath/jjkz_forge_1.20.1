package com.darkness.jujutsukaisenzone.entity;

import com.darkness.jujutsukaisenzone.faction.FactionStance;
import com.darkness.jujutsukaisenzone.faction.JjkzFactions;
import com.darkness.jujutsukaisenzone.gameplay.CursedSpiritGrade;
import com.darkness.jujutsukaisenzone.gameplay.CursedSpiritHealthTable;
import com.darkness.jujutsukaisenzone.heaven.CurseSpiritGradeSource;
import com.darkness.jujutsukaisenzone.heaven.threat.CurseSpiritHeavenSe;
import com.darkness.jujutsukaisenzone.technique.skill.ShrineCutMarkLogic;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * 三级咒灵甲：暂用尸壳模型占位，待正式咒灵 Geo 替换。
 * 生命走 {@link CursedSpiritHealthTable#GRADE_3}。
 * 普攻命中玩家时随机可断肢叠切痕（偏弱，仅叠痕不断肢）。
 */
public class Grade3CurseSpiritAEntity extends Monster implements CurseSpiritGradeSource
{
    /** 普攻切痕基础量（弱于捌的 3）。 */
    private static final float MELEE_CUT_MARK_BASE = 2.0F;

    public Grade3CurseSpiritAEntity(EntityType<? extends Grade3CurseSpiritAEntity> type, Level level)
    {
        super(type, level);
        if (!level.isClientSide)
        {
            JjkzFactions.assign(this, JjkzFactions.CURSE_SPIRIT);
        }
    }

    @Override
    public CursedSpiritGrade cursedSpiritGrade()
    {
        return CursedSpiritGrade.GRADE_3;
    }

    public static AttributeSupplier.Builder createAttributes()
    {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, CursedSpiritHealthTable.GRADE_3.typical())
                .add(Attributes.MOVEMENT_SPEED, 0.345D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 35.0D)
                .add(Attributes.ARMOR, 2.0D);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(
            ServerLevelAccessor level,
            DifficultyInstance difficulty,
            MobSpawnType spawnType,
            @Nullable SpawnGroupData spawnData,
            @Nullable CompoundTag tag)
    {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnData, tag);
        CursedSpiritHealthTable.applyMaxHealth(this, CursedSpiritGrade.GRADE_3, this.getRandom());
        CurseSpiritHeavenSe.ensureRolled(this, CursedSpiritGrade.GRADE_3);
        return data;
    }

    @Override
    protected void registerGoals()
    {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, null));
    }

    @Override
    public boolean canAttack(LivingEntity target)
    {
        return super.canAttack(target) && FactionStance.canAggress(this, target);
    }

    @Override
    public boolean doHurtTarget(Entity target)
    {
        boolean hit = super.doHurtTarget(target);
        if (hit && !this.level().isClientSide && target instanceof Player player && player.isAlive())
        {
            float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
            ShrineCutMarkLogic.applyRandomLimb(this, player, MELEE_CUT_MARK_BASE, damage);
        }
        return hit;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);
        JjkzFactions.copyToSave(this, tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        JjkzFactions.restoreFromSave(this, tag);
        JjkzFactions.assign(this, JjkzFactions.CURSE_SPIRIT);
    }
}
