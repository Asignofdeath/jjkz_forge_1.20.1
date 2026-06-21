package com.darkness.jujutsukaisenzone.entity;

import com.darkness.jujutsukaisenzone.entity.behavior.*;
import com.darkness.jujutsukaisenzone.entity.data.LimitlessPurpleConfig;
import com.darkness.jujutsukaisenzone.entity.util.SpecialEffectChunkForce;
import com.darkness.jujutsukaisenzone.entity.util.SpecialEffectFlight;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 万能特效实体：仅作为不可见、无敌、无碰撞的锚点。
 * <p>
 * 所有行为逻辑委托给 {@link EffectBehavior} 子类，
 * 按术式（无下限/御厨子/黑闪）分类独立管理。
 * <p>
 * 主动位移（飞行/推进）请使用 {@link SpecialEffectFlight}；超远程临时区块加载请使用 {@link SpecialEffectChunkForce}。
 */
public class SpecialEffectEntity extends Chicken
{
    public static final double MAX_HEALTH = 2048.0D;
    private static final String TAG_BEHAVIOR_ID = "EffectBehavior";
    private static final String TAG_BEHAVIOR_DATA = "EffectBehaviorData";

    @Nullable
    private EffectBehavior behavior;

    public SpecialEffectEntity(EntityType<? extends Chicken> type, Level level)
    {
        super(type, level);
        this.noPhysics = true;
        this.applySpecialState();
    }

    @Override
    public boolean isPushable()
    {
        return false;
    }

    @Override
    public boolean canBeCollidedWith()
    {
        return false;
    }

    @Override
    public boolean isPickable()
    {
        return false;
    }

    @Override
    public void push(Entity entity)
    {
    }

    @Override
    public void push(double x, double y, double z)
    {
    }

    public static AttributeSupplier.Builder createAttributes()
    {
        return Chicken.createAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    @Override
    protected void registerGoals()
    {
        // 不注册任何目标
    }

    @Override
    public boolean hurt(DamageSource source, float amount)
    {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source)
    {
        return true;
    }

    @Override
    public void tick()
    {
        super.tick();
        this.applySpecialState();

        if (!this.level().isClientSide && this.behavior != null)
        {
            this.behavior.tick(this);
        }
    }

    @Override
    public void remove(RemovalReason reason)
    {
        if (!this.level().isClientSide && this.behavior != null)
        {
            this.behavior.onDiscard(this);
        }
        super.remove(reason);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, net.minecraft.world.DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData spawnData, @Nullable CompoundTag tag)
    {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnData, tag);
        this.applySpecialState();
        return data;
    }

    // ==== Configure methods (each creates its behavior) ====

    public void configureLimitlessBlueNormal(@Nullable LivingEntity owner, boolean allowOwnerPull)
    {
        LimitlessBlueBehavior b = new LimitlessBlueBehavior();
        b.configure(owner, allowOwnerPull);
        this.behavior = b;
    }

    public void configureLimitlessRedNormal(@Nullable LivingEntity owner, boolean allowOwnerKnockback)
    {
        LimitlessRedBehavior b = new LimitlessRedBehavior();
        b.configure(owner, allowOwnerKnockback);
        this.behavior = b;
    }

    public void configureLimitlessPurpleMiniNormal(@Nullable LivingEntity owner)
    {
        LimitlessPurpleBehavior b = new LimitlessPurpleBehavior();
        b.configure(owner);
        this.behavior = b;
    }

    public void configureLimitlessRemoteMurasaki(@Nullable LivingEntity owner)
    {
        LimitlessRemoteMurasakiBehavior b = new LimitlessRemoteMurasakiBehavior();
        b.configure(owner);
        this.behavior = b;
    }

    public void configureLimitlessRemoteMurasakiStartOrb(
            @Nullable LivingEntity owner,
            LimitlessRemoteMurasakiStartOrbBehavior.Side side)
    {
        LimitlessRemoteMurasakiStartOrbBehavior b = new LimitlessRemoteMurasakiStartOrbBehavior();
        b.configure(owner, side);
        this.behavior = b;
    }

    public void configureShrineKaiDelay(@Nullable LivingEntity owner, double maxRange, boolean horizontal)
    {
        ShrineKaiDelayBehavior b = new ShrineKaiDelayBehavior();
        b.configure(owner, maxRange, horizontal);
        this.behavior = b;
    }

    public void configureBlackFlash(@Nullable LivingEntity owner)
    {
        BlackFlashFxBehavior b = new BlackFlashFxBehavior();
        b.configure(owner);
        this.behavior = b;
    }

    public void configureShrineKaiArrival(@Nullable LivingEntity owner, int slashType)
    {
        ShrineKaiArrivalBehavior b = new ShrineKaiArrivalBehavior();
        b.configure(owner, slashType);
        this.behavior = b;
    }

    public void configureShrineKaiFlyobjProjectile(@Nullable LivingEntity owner, double maxRange, Vec3 flightDir)
    {
        ShrineKaiArrowBehavior b = new ShrineKaiArrowBehavior();
        b.configure(owner, maxRange, flightDir, true, true);
        this.behavior = b;
    }

    public void configureShrineHachi(@Nullable LivingEntity owner, LivingEntity target)
    {
        ShrineHachiBehavior b = new ShrineHachiBehavior();
        b.configure(owner, target);
        this.behavior = b;
    }

    public void configureShrineFugaCharge(@Nullable LivingEntity owner)
    {
        ShrineFugaBehavior b = new ShrineFugaBehavior();
        b.configure(owner);
        this.behavior = b;
    }

    public void configureBlackFlashShockwaveDelay(@Nullable LivingEntity owner)
    {
        BlackFlashShockwaveBehavior b = new BlackFlashShockwaveBehavior();
        b.configure(owner);
        this.behavior = b;
    }

    // ==== Internal accessor for behaviors in entity.behavior package ====

    /** 供 {@code entity.behavior} 包内 Behavior 创建飞行实体时设置行为。 */
    public void setBehaviorForInternal(EffectBehavior behavior)
    {
        this.behavior = behavior;
    }

    @Nullable
    public EffectBehavior getEffectBehavior()
    {
        return this.behavior;
    }

    // ==== Flight API（实现见 {@link SpecialEffectFlight}） ====

    /** 进入飞行态：关闭重力，与现有飞行 Behavior 一致。 */
    public void prepareFlight()
    {
        SpecialEffectFlight.applyFlightPose(this);
    }

    /**
     * 分步直线推进，每 tick 最多移动 {@code motion.speedPerTick()} 格。
     *
     * @see SpecialEffectFlight#tickLinear(SpecialEffectEntity, SpecialEffectFlight.LinearMotion, SpecialEffectFlight.StepCallback)
     */
    public SpecialEffectFlight.Result advanceLinearFlight(
            SpecialEffectFlight.LinearMotion motion,
            @Nullable SpecialEffectFlight.StepCallback stepCallback)
    {
        return SpecialEffectFlight.tickLinear(this, motion, stepCallback);
    }

    /**
     * 连续速度推进，每 tick 沿 {@code motion.velocity()} 位移一次。
     *
     * @see SpecialEffectFlight#tickVelocity(SpecialEffectEntity, SpecialEffectFlight.VelocityMotion)
     */
    public SpecialEffectFlight.Result advanceVelocityFlight(SpecialEffectFlight.VelocityMotion motion)
    {
        return SpecialEffectFlight.tickVelocity(this, motion);
    }

    /**
     * 连续速度推进，不因未加载区块中止；需配合 {@link #maintainChunkForce} 使用。
     *
     * @see SpecialEffectFlight#tickVelocityUnbounded(SpecialEffectEntity, Vec3)
     */
    public void advanceVelocityFlightUnbounded(Vec3 velocity)
    {
        SpecialEffectFlight.tickVelocityUnbounded(this, velocity);
    }

    // ==== Chunk force API（实现见 {@link SpecialEffectChunkForce}） ====

    /** 创建一次飞行会话的区块强制加载 tracker，结束时必须 {@link #releaseChunkForce}。 */
    public SpecialEffectChunkForce.Tracker createChunkForceTracker(SpecialEffectChunkForce.Options options)
    {
        return SpecialEffectChunkForce.create(options);
    }

    public SpecialEffectChunkForce.Tracker createChunkForceTracker(int aheadChunks, int trailChunks)
    {
        return SpecialEffectChunkForce.create(aheadChunks, trailChunks);
    }

    /** 每 tick 位移前调用：沿 {@code flightDirection} 维护临时强制加载区块。 */
    public void maintainChunkForce(SpecialEffectChunkForce.Tracker tracker, Vec3 flightDirection)
    {
        if (this.level() instanceof ServerLevel level)
        {
            tracker.tickAlongPath(level, this, flightDirection);
        }
    }

    /** 释放 tracker 持有的全部强制加载 ticket。 */
    public void releaseChunkForce(SpecialEffectChunkForce.Tracker tracker)
    {
        if (this.level() instanceof ServerLevel level)
        {
            tracker.releaseAll(level, this);
        }
    }

    // ==== Static utilities

    /** 与 Shift+茈升空悬停流程对齐的蓄力时长（tick）。 */
    public static int limitlessPurpleMiniChargeTicks()
    {
        return LimitlessPurpleConfig.CHARGE_TICKS;
    }

    /** 茈蓄力锚点世界坐标。 */
    public static Vec3 limitlessPurpleMiniAnchorWorldPosition(LivingEntity owner)
    {
        return LimitlessPurpleBehavior.anchorWorldPosition(owner);
    }

    // ==== NBT ====

    @Override
    public void addAdditionalSaveData(CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);
        if (this.behavior != null)
        {
            tag.putString(TAG_BEHAVIOR_ID, this.behavior.id());
            CompoundTag data = new CompoundTag();
            this.behavior.writeToNbt(data);
            tag.put(TAG_BEHAVIOR_DATA, data);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);

        if (!tag.contains(TAG_BEHAVIOR_ID))
        {
            this.behavior = null;
            return;
        }

        this.behavior = EffectBehavior.createFromId(tag.getString(TAG_BEHAVIOR_ID));
        if (this.behavior != null && tag.contains(TAG_BEHAVIOR_DATA))
        {
            this.behavior.readFromNbt(tag.getCompound(TAG_BEHAVIOR_DATA));
        }
    }

    private void applySpecialState()
    {
        this.noPhysics = true;
        this.setNoAi(true);
        this.setInvulnerable(true);
        this.setNoGravity(true);
        this.setSilent(true);
        this.setInvisible(true);
        if (this.getHealth() < this.getMaxHealth())
        {
            this.setHealth(this.getMaxHealth());
        }
    }
}
