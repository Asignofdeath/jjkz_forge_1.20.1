package com.darkness.jujutsukaisenzone.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerPlayer;
import com.darkness.jujutsukaisenzone.api.screen.ScreenShakeKind;
import com.darkness.jujutsukaisenzone.network.ModNetwork;
import com.darkness.jujutsukaisenzone.technique.TechniqueDamageSources;
import com.darkness.jujutsukaisenzone.technique.skill.TenShadowsSummonLogic;
import com.darkness.jujutsukaisenzone.technique.skill.tenshadows.TenShadowsShikigamiSkillRatios;
import com.darkness.jujutsukaisenzone.technique.skillset.SkillDamageType;
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
    private static final RawAnimation RIDE_LIFT_ANIM = RawAnimation.begin().thenPlayAndHold("makora_hold_up_with_one_hand");
    private static final RawAnimation RIDE_IDLE_ANIM = RawAnimation.begin().thenLoop("makora_hold_up_with_one_hand_idle");
    private static final RawAnimation RIDE_WALK_ANIM = RawAnimation.begin().thenLoop("makora_hold_up_with_one_hand_walking");
    private static final RawAnimation RIDE_ATTACK_ANIM = RawAnimation.begin().thenPlayAndHold("makora_hold_up_with_one_hand_attack");
    private static final RawAnimation RIDE_KICK_ANIM = RawAnimation.begin().thenPlayAndHold("makora_hold_up_with_one_hand_kick");
    private static final RawAnimation RIDE_PROTECT_ANIM = RawAnimation.begin().thenPlayAndHold("makora_hold_up_with_one_hand_protect");
    /** 抱起动画开播后第 3.5 秒让玩家骑上（3.5s × 20 tps）。 */
    private static final int LIFT_TICKS = 70;
    private static final int ATTACK_TICKS = 35;
    private static final int ATTACK_HIT_TICKS = 9;
    /** 踢击约 0.4167s，出伤对齐腿伸出峰值（~0.17s）。 */
    private static final int KICK_TICKS = 9;
    private static final int KICK_HIT_TICKS = 6;
    private static final int PROTECT_TICKS = 12;
    private static final int ATTACK_COOLDOWN = 90;
    private static final int KICK_COOLDOWN = 35;
    private static final double KICK_LAUNCH_Y = 1.05D;
    private static final double KICK_LAUNCH_H = 0.55D;
    /** 行走震屏：范围 / 间隔 / 持续 / 振幅。 */
    private static final double WALK_TREMOR_RANGE = 10.0D;
    private static final int WALK_TREMOR_INTERVAL = 10;
    private static final int WALK_TREMOR_DURATION = 14;
    private static final float WALK_TREMOR_AMPLITUDE = 0.95F;
    /** 奔跑震屏：略强、略密。 */
    private static final double RUN_TREMOR_RANGE = 14.0D;
    private static final int RUN_TREMOR_INTERVAL = 6;
    private static final int RUN_TREMOR_DURATION = 12;
    private static final float RUN_TREMOR_AMPLITUDE = 1.55F;
    /** 预警：提前播格挡动画。 */
    private static final double PROTECT_WARN_RANGE = 10.0D;
    /** 拦截：反弹弹射物。 */
    private static final double PROTECT_INTERCEPT_RANGE = 3.5D;

    private static final EntityDataAccessor<Byte> DATA_LOCOMOTION =
            SynchedEntityData.defineId(MahoragaEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> DATA_RIDE_ACTION =
            SynchedEntityData.defineId(MahoragaEntity.class, EntityDataSerializers.BYTE);
    private static final byte RIDE_NONE = 0;
    private static final byte RIDE_APPROACH = 1;
    private static final byte RIDE_LIFT = 2;
    private static final byte RIDE_IDLE = 3;
    private static final byte RIDE_ATTACK = 4;
    private static final byte RIDE_PROTECT = 5;
    private static final byte RIDE_KICK = 6;
    private static final double RIDE_APPROACH_DISTANCE_SQR = 5.0D;

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
    private int rideActionTicks;
    private int rideAttackCooldown;
    @javax.annotation.Nullable
    private UUID pendingRider;
    @javax.annotation.Nullable
    private Vec3 liftAnchor;
    /** 骑乘普攻目标（不用 Mob#getTarget，避免骑乘清仇恨时被抹掉）。 */
    @javax.annotation.Nullable
    private UUID rideAttackTargetId;
    /** 格挡预警时锁定的朝向目标（射手或弹体），与格挡提前量同步。 */
    @javax.annotation.Nullable
    private UUID rideProtectFaceId;
    /** 下次近战用踢击（与挥击交替）。 */
    private boolean nextRideKick = true;

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

    public boolean isRideMode()
    {
        return this.entityData.get(DATA_RIDE_ACTION) != RIDE_NONE;
    }

    public void beginRide(ServerPlayer player)
    {
        this.getNavigation().stop();
        this.setTarget(null);
        this.setAggressive(false);
        this.pendingRider = player.getUUID();
        this.rideActionTicks = 0;
        this.entityData.set(DATA_RIDE_ACTION, RIDE_APPROACH);
    }

    public void protectRider()
    {
        if (isRideMode() && this.entityData.get(DATA_RIDE_ACTION) >= RIDE_IDLE)
        {
            boolean starting = this.entityData.get(DATA_RIDE_ACTION) != RIDE_PROTECT;
            this.entityData.set(DATA_RIDE_ACTION, RIDE_PROTECT);
            this.rideActionTicks = PROTECT_TICKS;
            if (starting)
            {
                playShieldBlockSound();
            }
        }
    }

    private void playShieldBlockSound()
    {
        if (this.level().isClientSide)
        {
            return;
        }
        this.level().playSound(
                null,
                this.getX(),
                this.getY() + 2.0D,
                this.getZ(),
                SoundEvents.SHIELD_BLOCK,
                SoundSource.NEUTRAL,
                1.0F,
                0.9F + this.random.nextFloat() * 0.2F);
    }

    @Override
    public boolean doHurtTarget(Entity target)
    {
        return !isRideMode() && super.doHurtTarget(target);
    }

    @Override
    public void travel(Vec3 input)
    {
        if (this.getControllingPassenger() instanceof Player rider && isRideMode())
        {
            /* 仅在移动时转向：转视角不转身体，避免座位跟着绕圈 */
            if (this.entityData.get(DATA_RIDE_ACTION) != RIDE_PROTECT
                    && (Math.abs(rider.xxa) > 0.05F || Math.abs(rider.zza) > 0.05F))
            {
                this.setYRot(rider.getYRot());
                this.yRotO = this.getYRot();
                this.yBodyRot = this.getYRot();
                this.yHeadRot = this.getYRot();
            }
            this.setXRot(0.0F);
            /* 骑乘移速约为属性值的 78%（相对原 1.3 倍率削弱约 40%）。 */
            this.setSpeed((float) this.getAttributeValue(Attributes.MOVEMENT_SPEED) * 0.78F);
            super.travel(new Vec3(rider.xxa * 0.5F, 0.0D, rider.zza));
            return;
        }
        byte rideAction = this.entityData.get(DATA_RIDE_ACTION);
        if (rideAction == RIDE_LIFT || rideAction == RIDE_IDLE
                || rideAction == RIDE_ATTACK || rideAction == RIDE_PROTECT
                || rideAction == RIDE_KICK)
        {
            super.travel(Vec3.ZERO);
            return;
        }
        super.travel(input);
    }

    @Override
    public LivingEntity getControllingPassenger()
    {
        return this.getFirstPassenger() instanceof LivingEntity living ? living : null;
    }

    @Override
    public void positionRider(Entity passenger, MoveFunction move)
    {
        if (!this.hasPassenger(passenger))
        {
            return;
        }
        double angle = Math.toRadians(this.getYRot());
        // Held pose of root/pelvis/torso/right_arm/bone, scaled with the model.
        double right = 0.3D;
        double forward = 0.8D;
        double x = this.getX() + Math.cos(angle) * right - Math.sin(angle) * forward;
        double z = this.getZ() + Math.sin(angle) * right + Math.cos(angle) * forward;
        move.accept(passenger, x, this.getY() + 2.3D, z);
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
        if (!this.level().isClientSide && isRideMode())
        {
            /* 压住 MeleeAttackGoal / 外部 AI 在 super.tick 里抢导航 */
            this.setTarget(null);
        }
        super.tick();
        if (this.level().isClientSide)
        {
            return;
        }
        tickRide();
        if (!isRideMode())
        {
            updateLocomotion();
            tickLocomotionTremor();
        }
    }

    /** 非骑乘走/跑时，给附近玩家发适中屏幕震动。 */
    private void tickLocomotionTremor()
    {
        byte loco = getLocomotion();
        if (loco != LOCO_WALK && loco != LOCO_RUN)
        {
            return;
        }
        if (this.getDeltaMovement().horizontalDistanceSqr() < 1.0E-4D && this.getNavigation().isDone())
        {
            return;
        }
        boolean running = loco == LOCO_RUN;
        int interval = running ? RUN_TREMOR_INTERVAL : WALK_TREMOR_INTERVAL;
        if (this.tickCount % interval != 0)
        {
            return;
        }
        double range = running ? RUN_TREMOR_RANGE : WALK_TREMOR_RANGE;
        float amplitude = running ? RUN_TREMOR_AMPLITUDE : WALK_TREMOR_AMPLITUDE;
        int duration = running ? RUN_TREMOR_DURATION : WALK_TREMOR_DURATION;
        for (ServerPlayer player : this.level().getEntitiesOfClass(ServerPlayer.class,
                this.getBoundingBox().inflate(range),
                p -> p.isAlive() && !p.isSpectator() && p.getVehicle() != this
                        && p.distanceToSqr(this) <= range * range))
        {
            ModNetwork.sendScreenShake(player, ScreenShakeKind.SMOOTH_WOBBLE, duration, amplitude);
        }
    }

    private void tickRide()
    {
        if (!isRideMode())
        {
            return;
        }
        if (rideAttackCooldown > 0)
        {
            rideAttackCooldown--;
        }
        if (this.entityData.get(DATA_RIDE_ACTION) == RIDE_APPROACH)
        {
            ServerPlayer rider = resolvePendingRider();
            if (!isValidPendingRider(rider))
            {
                endRide();
                return;
            }
            this.setTarget(null);
            faceRider(rider);
            double horizontalDistanceSqr = this.distanceToSqr(rider.getX(), this.getY(), rider.getZ());
            if (horizontalDistanceSqr > RIDE_APPROACH_DISTANCE_SQR)
            {
                this.getNavigation().moveTo(rider, 1.1D);
                return;
            }
            this.getNavigation().stop();
            this.setTarget(null);
            this.liftAnchor = rider.position();
            rider.setDeltaMovement(Vec3.ZERO);
            rider.fallDistance = 0.0F;
            this.rideActionTicks = LIFT_TICKS;
            this.entityData.set(DATA_RIDE_ACTION, RIDE_LIFT);
            return;
        }
        if (this.entityData.get(DATA_RIDE_ACTION) == RIDE_LIFT)
        {
            ServerPlayer rider = resolvePendingRider();
            if (!isValidPendingRider(rider))
            {
                endRide();
                return;
            }
            this.getNavigation().stop();
            faceRider(rider);
            rider.setDeltaMovement(Vec3.ZERO);
            rider.fallDistance = 0.0F;
            if (liftAnchor != null && rider.position().distanceToSqr(liftAnchor) > 0.0025D)
            {
                rider.teleportTo(liftAnchor.x, liftAnchor.y, liftAnchor.z);
            }
            if (--rideActionTicks <= 0)
            {
                pendingRider = null;
                liftAnchor = null;
                if (rider.startRiding(this, true))
                {
                    this.entityData.set(DATA_RIDE_ACTION, RIDE_IDLE);
                }
                else
                {
                    endRide();
                }
            }
            return;
        }
        if (!(this.getFirstPassenger() instanceof ServerPlayer rider) || !rider.isAlive()
                || !TenShadowsSummonLogic.isOwnedBy(this, rider.getUUID()))
        {
            endRide();
            return;
        }
        this.getNavigation().stop();
        tickRideProjectileGuard(rider);
        if (this.entityData.get(DATA_RIDE_ACTION) == RIDE_PROTECT)
        {
            faceProtectThreat();
        }
        if (rideActionTicks > 0 && --rideActionTicks == 0)
        {
            this.rideAttackTargetId = null;
            this.rideProtectFaceId = null;
            this.entityData.set(DATA_RIDE_ACTION, RIDE_IDLE);
        }
        if (rideAttackCooldown == 0 && this.entityData.get(DATA_RIDE_ACTION) == RIDE_IDLE)
        {
            LivingEntity target = findRideTarget(rider);
            if (target != null)
            {
                this.rideAttackTargetId = target.getUUID();
                if (nextRideKick)
                {
                    this.entityData.set(DATA_RIDE_ACTION, RIDE_KICK);
                    this.rideActionTicks = KICK_TICKS;
                    this.rideAttackCooldown = KICK_COOLDOWN;
                }
                else
                {
                    this.entityData.set(DATA_RIDE_ACTION, RIDE_ATTACK);
                    this.rideActionTicks = ATTACK_TICKS;
                    this.rideAttackCooldown = ATTACK_COOLDOWN;
                }
                nextRideKick = !nextRideKick;
            }
        }
        if (this.entityData.get(DATA_RIDE_ACTION) == RIDE_ATTACK && rideActionTicks == ATTACK_HIT_TICKS)
        {
            applyRideMeleeHit(rider, false);
        }
        if (this.entityData.get(DATA_RIDE_ACTION) == RIDE_KICK && rideActionTicks == KICK_HIT_TICKS)
        {
            applyRideMeleeHit(rider, true);
        }
    }

    private void applyRideMeleeHit(ServerPlayer rider, boolean launch)
    {
        LivingEntity target = resolveRideAttackTarget();
        if (target == null || !target.isAlive() || target == rider
                || this.distanceToSqr(target) > 16.0D
                || TenShadowsSummonLogic.isOwnedBy(target, rider.getUUID()))
        {
            return;
        }
        this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        float skillRatio = launch
                ? TenShadowsShikigamiSkillRatios.MAHORAGA_RIDE_KICK
                : TenShadowsShikigamiSkillRatios.MAHORAGA_RIDE_PUNCH;
        TechniqueDamageSources.deal(target, this, skillRatio, SkillDamageType.MARTIAL);
        if (launch)
        {
            Vec3 away = target.position().subtract(this.position());
            if (away.horizontalDistanceSqr() < 1.0E-4D)
            {
                float yaw = this.getYRot() * ((float) Math.PI / 180.0F);
                away = new Vec3(-Mth.sin(yaw), 0.0D, Mth.cos(yaw));
            }
            Vec3 horizontal = new Vec3(away.x, 0.0D, away.z).normalize().scale(KICK_LAUNCH_H);
            target.setDeltaMovement(horizontal.x, KICK_LAUNCH_Y, horizontal.z);
            target.hurtMarked = true;
            target.hasImpulse = true;
            target.fallDistance = 0.0F;
        }
    }

    /** 360° 预警播格挡并同时转向攻击源；近距再反弹。 */
    private void tickRideProjectileGuard(ServerPlayer rider)
    {
        AABB warnBox = this.getBoundingBox().inflate(PROTECT_WARN_RANGE);
        AABB interceptBox = this.getBoundingBox().inflate(PROTECT_INTERCEPT_RANGE);
        Vec3 guardPoint = this.position().add(0.0D, 2.0D, 0.0D);
        Entity nearestFace = null;
        double nearestDistSqr = Double.MAX_VALUE;
        for (Projectile projectile : this.level().getEntitiesOfClass(Projectile.class,
                warnBox, p -> p.isAlive()
                        && p.getOwner() != rider && p.getOwner() != this
                        && p.getDeltaMovement().lengthSqr() > 0.01D))
        {
            /* 先锁定朝向（反弹前的主人），与格挡预警同帧 */
            Entity faceSource = projectile.getOwner();
            if (faceSource == null || faceSource == this || !faceSource.isAlive())
            {
                faceSource = projectile;
            }
            double distSqr = this.distanceToSqr(projectile);
            if (distSqr < nearestDistSqr)
            {
                nearestDistSqr = distSqr;
                nearestFace = faceSource;
            }

            boolean alreadyProtecting = this.entityData.get(DATA_RIDE_ACTION) == RIDE_PROTECT;
            protectRider();

            if (!interceptBox.intersects(projectile.getBoundingBox()))
            {
                continue;
            }
            if (alreadyProtecting)
            {
                playShieldBlockSound();
            }
            Vec3 velocity = projectile.getDeltaMovement();
            double speed = Math.max(0.8D, velocity.length());
            Vec3 away = projectile.position().subtract(guardPoint);
            if (away.lengthSqr() < 1.0E-6D)
            {
                away = velocity.lengthSqr() > 1.0E-6D ? velocity : new Vec3(0.0D, 0.0D, 1.0D);
            }
            projectile.setDeltaMovement(away.normalize().scale(speed * 1.5D).add(0.0D, 0.25D, 0.0D));
            projectile.setOwner(this);
            projectile.hurtMarked = true;
        }
        if (nearestFace != null)
        {
            this.rideProtectFaceId = nearestFace.getUUID();
            faceEntity(nearestFace);
        }
    }

    /** 格挡持续期间保持面朝预警锁定的攻击源。 */
    private void faceProtectThreat()
    {
        if (rideProtectFaceId == null || !(this.level() instanceof net.minecraft.server.level.ServerLevel server))
        {
            return;
        }
        Entity threat = server.getEntity(rideProtectFaceId);
        if (threat == null || !threat.isAlive() || threat == this)
        {
            return;
        }
        faceEntity(threat);
    }

    private void faceEntity(Entity source)
    {
        this.getLookControl().setLookAt(source, 40.0F, 40.0F);
        double dx = source.getX() - this.getX();
        double dz = source.getZ() - this.getZ();
        if (dx * dx + dz * dz < 1.0E-6D)
        {
            return;
        }
        float yaw = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
        this.setYRot(yaw);
        this.yHeadRot = yaw;
        this.yBodyRot = yaw;
    }

    @javax.annotation.Nullable
    private LivingEntity resolveRideAttackTarget()
    {
        if (rideAttackTargetId == null || !(this.level() instanceof net.minecraft.server.level.ServerLevel server))
        {
            return null;
        }
        Entity entity = server.getEntity(rideAttackTargetId);
        return entity instanceof LivingEntity living ? living : null;
    }

    @javax.annotation.Nullable
    private ServerPlayer resolvePendingRider()
    {
        if (pendingRider == null || !(this.level() instanceof net.minecraft.server.level.ServerLevel server))
        {
            return null;
        }
        return server.getServer().getPlayerList().getPlayer(pendingRider);
    }

    private boolean isValidPendingRider(@javax.annotation.Nullable ServerPlayer rider)
    {
        return rider != null && rider.isAlive() && !rider.isPassenger()
                && rider.distanceToSqr(this) <= 64.0D
                && TenShadowsSummonLogic.isOwnedBy(this, rider.getUUID());
    }

    private void faceRider(ServerPlayer rider)
    {
        this.getLookControl().setLookAt(rider, 30.0F, 30.0F);
        double dx = rider.getX() - this.getX();
        double dz = rider.getZ() - this.getZ();
        float yaw = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
        this.setYRot(yaw);
        this.yHeadRot = yaw;
        this.yBodyRot = yaw;
    }

    @javax.annotation.Nullable
    private LivingEntity findRideTarget(ServerPlayer rider)
    {
        LivingEntity best = null;
        double bestDistance = 16.0D;
        for (LivingEntity candidate : this.level().getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(4.0D), living -> living.isAlive()
                        && living != rider && living != this
                        && (living instanceof Enemy || living instanceof net.minecraft.world.entity.player.Player)
                        && !TenShadowsSummonLogic.isOwnedBy(living, rider.getUUID())))
        {
            double distance = this.distanceToSqr(candidate);
            if (distance < bestDistance)
            {
                best = candidate;
                bestDistance = distance;
            }
        }
        return best;
    }

    private void endRide()
    {
        pendingRider = null;
        liftAnchor = null;
        rideAttackTargetId = null;
        rideProtectFaceId = null;
        rideActionTicks = 0;
        this.entityData.set(DATA_RIDE_ACTION, RIDE_NONE);
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
        this.entityData.define(DATA_RIDE_ACTION, RIDE_NONE);
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
    /** 骑乘攻击动画 2 倍速。 */
    private static final double RIDE_ATTACK_ANIM_SPEED = 2.0D;
    /** 骑乘格挡动画 3 倍速。 */
    private static final double RIDE_PROTECT_ANIM_SPEED = 3.0D;

    private PlayState predicate(AnimationState<MahoragaEntity> state)
    {
        byte ride = this.entityData.get(DATA_RIDE_ACTION);
        if (ride != RIDE_NONE)
        {
            if (ride == RIDE_APPROACH)
            {
                state.getController().setAnimationSpeed(1.0D);
                state.getController().setAnimation(WALK_ANIM);
                return PlayState.CONTINUE;
            }
            if (ride == RIDE_LIFT)
            {
                state.getController().setAnimationSpeed(1.0D);
                state.getController().setAnimation(RIDE_LIFT_ANIM);
                return PlayState.CONTINUE;
            }
            if (ride == RIDE_ATTACK)
            {
                state.getController().setAnimationSpeed(RIDE_ATTACK_ANIM_SPEED);
                state.getController().setAnimation(RIDE_ATTACK_ANIM);
                return PlayState.CONTINUE;
            }
            if (ride == RIDE_KICK)
            {
                state.getController().setAnimationSpeed(1.0D);
                state.getController().setAnimation(RIDE_KICK_ANIM);
                return PlayState.CONTINUE;
            }
            if (ride == RIDE_PROTECT)
            {
                state.getController().setAnimationSpeed(RIDE_PROTECT_ANIM_SPEED);
                state.getController().setAnimation(RIDE_PROTECT_ANIM);
                return PlayState.CONTINUE;
            }
            /* RIDE_IDLE：走动才播 walking，静止用托举待机 */
            state.getController().setAnimationSpeed(1.0D);
            boolean rideMoving = state.isMoving()
                    || this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4D;
            state.getController().setAnimation(rideMoving ? RIDE_WALK_ANIM : RIDE_IDLE_ANIM);
            return PlayState.CONTINUE;
        }
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
