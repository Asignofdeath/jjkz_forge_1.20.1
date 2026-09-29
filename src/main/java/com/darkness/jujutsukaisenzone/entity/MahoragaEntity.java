package com.darkness.jujutsukaisenzone.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * 十种影法·魔虚罗：GeckoLib 模型。
 * <p>
 * 索敌瞬间按距离定行走 / 奔跑并锁定；追击同一目标期间不重判。
 * 无目标时：常态行走用 walking（移速降低），静止用 idle。
 */
public class MahoragaEntity extends PathfinderMob implements GeoEntity
{
    public static final byte LOCO_IDLE = 0;
    public static final byte LOCO_WALK = 1;
    public static final byte LOCO_RUN = 2;

    /** 索敌时超过此距离锁定为奔跑。 */
    public static final double RUN_DISTANCE = 8.0D;

    public static final float MODEL_SCALE = 0.8F;

    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("makora_idle");
    private static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("makora_walking");
    private static final RawAnimation RUN_ANIM = RawAnimation.begin().thenLoop("makora_run");

    private static final EntityDataAccessor<Byte> DATA_LOCOMOTION =
            SynchedEntityData.defineId(MahoragaEntity.class, EntityDataSerializers.BYTE);

    private static final String TAG_LOCOMOTION = "MahoragaLocomotion";
    private static final UUID LOCO_SPEED_UUID = UUID.fromString("c3d4e5f6-3001-4000-8000-000000070001");
    private static final String LOCO_SPEED_NAME = "jjkz_mahoraga_loco";
    /** walking 相对基础移速。 */
    private static final double WALK_SPEED_FACTOR = 0.72D;
    /** run 相对基础移速。 */
    private static final double RUN_SPEED_FACTOR = 1.35D;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    /** 当前锁定的追击目标；仅在换目标时重判走/跑。 */
    @javax.annotation.Nullable
    private UUID lockedChaseTargetId;
    /** 对该目标锁定的追击姿态（WALK / RUN）。 */
    private byte lockedChaseLoco = LOCO_WALK;

    public MahoragaEntity(EntityType<? extends MahoragaEntity> type, Level level)
    {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes()
    {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 120.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ATTACK_DAMAGE, 12.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.85D)
                .add(Attributes.ARMOR, 10.0D);
    }

    @Override
    protected void registerGoals()
    {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, true));
    }

    public byte getLocomotion()
    {
        Byte value = this.entityData.get(DATA_LOCOMOTION);
        return value == null ? LOCO_IDLE : value;
    }

    public boolean isRunning()
    {
        return getLocomotion() == LOCO_RUN;
    }

    public boolean isWalking()
    {
        return getLocomotion() == LOCO_WALK;
    }

    /** 供十影 AI 选用导航倍率：读索敌时锁定的姿态，不按实时距离。 */
    public double navigationSpeedForChase(LivingEntity target)
    {
        if (target == null || !target.isAlive())
        {
            return navigationSpeedForFollow();
        }
        if (lockedChaseTargetId == null || !lockedChaseTargetId.equals(target.getUUID()))
        {
            beginChase(target);
        }
        return lockedChaseLoco == LOCO_RUN ? 1.4D : 0.9D;
    }

    public double navigationSpeedForFollow()
    {
        return 0.95D;
    }

    /**
     * 索敌瞬间调用：按此刻距离锁定行走或奔跑，直至目标更换或清空。
     */
    public void beginChase(LivingEntity target)
    {
        if (target == null || !target.isAlive())
        {
            clearChaseLock();
            return;
        }
        UUID id = target.getUUID();
        if (id.equals(this.lockedChaseTargetId))
        {
            setLocomotion(this.lockedChaseLoco);
            return;
        }
        this.lockedChaseTargetId = id;
        this.lockedChaseLoco = this.distanceTo(target) > RUN_DISTANCE ? LOCO_RUN : LOCO_WALK;
        setLocomotion(this.lockedChaseLoco);
    }

    public void clearChaseLock()
    {
        this.lockedChaseTargetId = null;
        this.lockedChaseLoco = LOCO_WALK;
    }

    @Override
    public void setTarget(@javax.annotation.Nullable LivingEntity target)
    {
        LivingEntity previous = this.getTarget();
        super.setTarget(target);
        if (this.level().isClientSide)
        {
            return;
        }
        if (target == null || !target.isAlive())
        {
            clearChaseLock();
            return;
        }
        if (previous == null || !previous.getUUID().equals(target.getUUID()))
        {
            beginChase(target);
        }
    }

    private void setLocomotion(byte loco)
    {
        if (getLocomotion() == loco)
        {
            return;
        }
        this.entityData.set(DATA_LOCOMOTION, loco);
        applyLocomotionSpeed(loco);
    }

    private void applyLocomotionSpeed(byte loco)
    {
        AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null)
        {
            return;
        }
        speed.removeModifier(LOCO_SPEED_UUID);
        double factor = switch (loco)
        {
            case LOCO_WALK -> WALK_SPEED_FACTOR;
            case LOCO_RUN -> RUN_SPEED_FACTOR;
            default -> 1.0D;
        };
        if (Math.abs(factor - 1.0D) < 0.001D)
        {
            return;
        }
        speed.addTransientModifier(new AttributeModifier(
                LOCO_SPEED_UUID,
                LOCO_SPEED_NAME,
                factor - 1.0D,
                AttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    @Override
    public void tick()
    {
        super.tick();
        if (this.level().isClientSide)
        {
            return;
        }
        updateLocomotion();
    }

    private void updateLocomotion()
    {
        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive())
        {
            if (this.lockedChaseTargetId == null || !this.lockedChaseTargetId.equals(target.getUUID()))
            {
                beginChase(target);
            }
            else
            {
                setLocomotion(this.lockedChaseLoco);
            }
            return;
        }

        clearChaseLock();
        boolean moving = this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4D
                || !this.getNavigation().isDone();
        if (moving)
        {
            setLocomotion(LOCO_WALK);
            return;
        }
        setLocomotion(LOCO_IDLE);
    }

    @Override
    protected void defineSynchedData()
    {
        super.defineSynchedData();
        this.entityData.define(DATA_LOCOMOTION, LOCO_IDLE);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);
        tag.putByte(TAG_LOCOMOTION, getLocomotion());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        if (tag.contains(TAG_LOCOMOTION))
        {
            byte loco = tag.getByte(TAG_LOCOMOTION);
            this.entityData.set(DATA_LOCOMOTION, loco);
            applyLocomotionSpeed(loco);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers)
    {
        controllers.add(new AnimationController<>(this, "main", 5, this::predicate));
    }

    /** run 动画播放倍率（相对制作时的时长）。 */
    private static final double RUN_ANIM_SPEED = 1.6D;

    private PlayState predicate(AnimationState<MahoragaEntity> state)
    {
        byte loco = getLocomotion();
        if (loco == LOCO_RUN)
        {
            state.getController().setAnimationSpeed(RUN_ANIM_SPEED);
            state.getController().setAnimation(RUN_ANIM);
            return PlayState.CONTINUE;
        }
        state.getController().setAnimationSpeed(1.0D);
        if (loco == LOCO_WALK || state.isMoving())
        {
            state.getController().setAnimation(WALK_ANIM);
            return PlayState.CONTINUE;
        }
        state.getController().setAnimation(IDLE_ANIM);
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache()
    {
        return cache;
    }
}
