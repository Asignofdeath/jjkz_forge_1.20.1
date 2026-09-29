package com.darkness.jujutsukaisenzone.entity;

import com.darkness.jujutsukaisenzone.player.PlayerCursedGradeData;
import com.darkness.jujutsukaisenzone.player.PlayerTenShadowsRosterData;
import com.darkness.jujutsukaisenzone.technique.skill.TenShadowsShikigami;
import com.darkness.jujutsukaisenzone.technique.skill.TenShadowsSummonLogic;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * 十种影法·玉犬：GeckoLib 模型（白/黑）与行走、撕咬、扑击、进食动画。
 * <p>
 * 当前尺寸为幼年体；主人达到三级咒术师后模型放大 30%。
 */
public class JadeDogEntity extends Wolf implements GeoEntity
{
    public static final byte ACTION_IDLE = 0;
    public static final byte ACTION_BITE = 1;
    public static final byte ACTION_POUNCE = 2;
    public static final byte ACTION_EAT = 3;

    /** 幼年期渲染缩放（当前默认尺寸）。 */
    public static final float SCALE_JUVENILE = 0.5F;
    /** 三级及以上：在幼年基础上放大 30%。 */
    public static final float SCALE_ADULT = SCALE_JUVENILE * 1.3F;

    private static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("jade_dog_walking");
    private static final RawAnimation BITE_ANIM = RawAnimation.begin().thenPlay("jade_dog_bite");
    private static final RawAnimation POUNCE_ANIM = RawAnimation.begin().thenPlay("jade_dog_pounce");
    private static final RawAnimation EAT_ANIM = RawAnimation.begin().thenPlay("jade_dog_Eating");

    private static final EntityDataAccessor<Boolean> DATA_WHITE =
            SynchedEntityData.defineId(JadeDogEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_MATURE =
            SynchedEntityData.defineId(JadeDogEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Byte> DATA_ACTION =
            SynchedEntityData.defineId(JadeDogEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> DATA_ACTION_SEQ =
            SynchedEntityData.defineId(JadeDogEntity.class, EntityDataSerializers.INT);

    private static final String TAG_WHITE = "JadeDogWhite";
    private static final String TAG_MATURE = "JadeDogMature";
    private static final int BITE_TICKS = 21;
    private static final int POUNCE_TICKS = 19;
    private static final int EAT_TICKS = 40;
    /** 判定「周围无威胁」的搜索半径。 */
    private static final double SAFE_RADIUS = 12.0D;
    /** 进食回复：最大生命的比例。 */
    private static final float EAT_HEAL_FRACTION = 0.25F;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int actionTicks;
    private int lastClientActionSeq = -1;

    public JadeDogEntity(EntityType<? extends JadeDogEntity> type, Level level)
    {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes()
    {
        return Wolf.createAttributes();
    }

    public void setWhiteVariant(boolean white)
    {
        this.entityData.set(DATA_WHITE, white);
    }

    public boolean isWhiteVariant()
    {
        return Boolean.TRUE.equals(this.entityData.get(DATA_WHITE));
    }

    /** 按主人真实咒术等级设置成年态（≥三级）。 */
    public void applyMaturityFromOwner(Player owner)
    {
        if (owner == null)
        {
            setMature(false);
            return;
        }
        setMature(PlayerCursedGradeData.getRawRank(owner) >= PlayerCursedGradeData.RANK_GRADE_3);
    }

    public void setMature(boolean mature)
    {
        boolean was = isMature();
        this.entityData.set(DATA_MATURE, mature);
        if (was != mature)
        {
            this.refreshDimensions();
        }
    }

    public boolean isMature()
    {
        return Boolean.TRUE.equals(this.entityData.get(DATA_MATURE));
    }

    public float getModelScale()
    {
        return isMature() ? SCALE_ADULT : SCALE_JUVENILE;
    }

    /** 主人升至三级（或降级）时，刷新场上友方玉犬体型。 */
    public static void refreshMaturityForOwner(ServerPlayer owner)
    {
        if (owner == null)
        {
            return;
        }
        ServerLevel level = owner.serverLevel();
        List<UUID> ids = PlayerTenShadowsRosterData.get(owner, TenShadowsShikigami.DIVINE_DOGS.index());
        for (UUID id : ids)
        {
            Entity entity = level.getEntity(id);
            if (entity instanceof JadeDogEntity dog
                    && TenShadowsSummonLogic.isOwnedBy(dog, owner.getUUID()))
            {
                dog.applyMaturityFromOwner(owner);
            }
        }
    }

    public boolean isEating()
    {
        return getAction() == ACTION_EAT;
    }

    public void playBiteAnimation()
    {
        if (this.level().isClientSide || isEating())
        {
            return;
        }
        startAction(ACTION_BITE, BITE_TICKS);
    }

    public void playPounceAnimation()
    {
        if (this.level().isClientSide || isEating())
        {
            return;
        }
        startAction(ACTION_POUNCE, POUNCE_TICKS);
    }

    /**
     * 击败敌人后：若周围无威胁则原地进食并回复生命。
     *
     * @param victim 刚被击败的目标（不计入威胁）
     */
    public boolean tryStartEatingAfterKill(LivingEntity victim)
    {
        if (this.level().isClientSide || isEating() || !TenShadowsSummonLogic.isFriendlyOwned(this))
        {
            return false;
        }
        if (hasNearbyThreat(victim))
        {
            return false;
        }
        this.setTarget(null);
        this.getNavigation().stop();
        this.setDeltaMovement(Vec3.ZERO);
        this.setOrderedToSit(false);
        float heal = Math.max(4.0F, this.getMaxHealth() * EAT_HEAL_FRACTION);
        this.heal(heal);
        startAction(ACTION_EAT, EAT_TICKS);
        return true;
    }

    private boolean hasNearbyThreat(LivingEntity exclude)
    {
        UUID ownerId = TenShadowsSummonLogic.getOwnerId(this);
        AABB box = this.getBoundingBox().inflate(SAFE_RADIUS);
        for (LivingEntity other : this.level().getEntitiesOfClass(LivingEntity.class, box,
                living -> living.isAlive() && living != this && living != exclude))
        {
            if (ownerId != null && TenShadowsSummonLogic.isOwnedBy(other, ownerId))
            {
                continue;
            }
            if (ownerId != null && other.getUUID().equals(ownerId))
            {
                continue;
            }
            if (isThreatToDog(other, ownerId))
            {
                return true;
            }
        }
        return false;
    }

    private boolean isThreatToDog(LivingEntity candidate, UUID ownerId)
    {
        if (candidate instanceof Player player)
        {
            if (player instanceof ServerPlayer && ownerId != null && player.getUUID().equals(ownerId))
            {
                return false;
            }
            return true;
        }
        if (candidate instanceof Enemy)
        {
            return true;
        }
        if (candidate instanceof Mob mob)
        {
            LivingEntity target = mob.getTarget();
            if (target == this)
            {
                return true;
            }
            return ownerId != null && target != null && target.getUUID().equals(ownerId);
        }
        return false;
    }

    private void startAction(byte action, int ticks)
    {
        this.entityData.set(DATA_ACTION, action);
        this.entityData.set(DATA_ACTION_SEQ, this.entityData.get(DATA_ACTION_SEQ) + 1);
        this.actionTicks = ticks;
    }

    public byte getAction()
    {
        Byte action = this.entityData.get(DATA_ACTION);
        return action == null ? ACTION_IDLE : action;
    }

    @Override
    public void tick()
    {
        super.tick();
        if (this.level().isClientSide)
        {
            return;
        }
        if (this.actionTicks > 0)
        {
            this.actionTicks--;
            if (getAction() == ACTION_EAT)
            {
                this.getNavigation().stop();
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.0D, 1.0D, 0.0D));
                this.setOrderedToSit(false);
            }
            if (this.actionTicks <= 0)
            {
                this.entityData.set(DATA_ACTION, ACTION_IDLE);
            }
        }
    }

    @Override
    public void travel(Vec3 travelVector)
    {
        if (isEating())
        {
            super.travel(Vec3.ZERO);
            return;
        }
        super.travel(travelVector);
    }

    @Override
    public boolean doHurtTarget(Entity target)
    {
        boolean hit = super.doHurtTarget(target);
        if (hit)
        {
            playBiteAnimation();
        }
        return hit;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand)
    {
        if (TenShadowsSummonLogic.isFriendlyOwned(this))
        {
            return InteractionResult.PASS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void setOrderedToSit(boolean sit)
    {
        if (isEating())
        {
            super.setOrderedToSit(false);
            return;
        }
        super.setOrderedToSit(sit);
    }

    @Override
    protected void defineSynchedData()
    {
        super.defineSynchedData();
        this.entityData.define(DATA_WHITE, true);
        this.entityData.define(DATA_MATURE, false);
        this.entityData.define(DATA_ACTION, ACTION_IDLE);
        this.entityData.define(DATA_ACTION_SEQ, 0);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose)
    {
        EntityDimensions base = super.getDimensions(pose);
        return isMature() ? base.scale(1.3F) : base;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key)
    {
        if (DATA_MATURE.equals(key))
        {
            this.refreshDimensions();
        }
        super.onSyncedDataUpdated(key);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);
        tag.putBoolean(TAG_WHITE, isWhiteVariant());
        tag.putBoolean(TAG_MATURE, isMature());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        if (tag.contains(TAG_WHITE))
        {
            setWhiteVariant(tag.getBoolean(TAG_WHITE));
        }
        if (tag.contains(TAG_MATURE))
        {
            setMature(tag.getBoolean(TAG_MATURE));
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers)
    {
        controllers.add(new AnimationController<>(this, "main", 4, this::predicate));
    }

    private PlayState predicate(AnimationState<JadeDogEntity> state)
    {
        int seq = this.entityData.get(DATA_ACTION_SEQ);
        if (seq != this.lastClientActionSeq)
        {
            state.getController().forceAnimationReset();
            this.lastClientActionSeq = seq;
        }
        byte action = getAction();
        if (action == ACTION_EAT)
        {
            state.getController().setAnimation(EAT_ANIM);
            return PlayState.CONTINUE;
        }
        if (action == ACTION_POUNCE)
        {
            state.getController().setAnimation(POUNCE_ANIM);
            return PlayState.CONTINUE;
        }
        if (action == ACTION_BITE)
        {
            state.getController().setAnimation(BITE_ANIM);
            return PlayState.CONTINUE;
        }
        if (state.isMoving())
        {
            state.getController().setAnimation(WALK_ANIM);
            return PlayState.CONTINUE;
        }
        return PlayState.STOP;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache()
    {
        return cache;
    }
}
