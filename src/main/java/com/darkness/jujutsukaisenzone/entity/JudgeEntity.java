package com.darkness.jujutsukaisenzone.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * 审判者：静态 GeckoLib 实体，生成时播放出场动画。
 */
public class JudgeEntity extends Entity implements GeoEntity
{
    private static final RawAnimation APPEARS_ANIM = RawAnimation.begin().thenPlay("judge_appears_animation");
    private static final RawAnimation ANGRY_ANIM = RawAnimation.begin().thenPlay("judge_angry_animation");
    private static final EntityDataAccessor<Boolean> DATA_PLAY_ENTRANCE_ANIM =
            SynchedEntityData.defineId(JudgeEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_PLAY_ANGRY_ANIM =
            SynchedEntityData.defineId(JudgeEntity.class, EntityDataSerializers.BOOLEAN);
    private static final String TAG_PLAY_ENTRANCE_ANIM = "PlayEntranceAnim";
    private static final String TAG_PLAY_ANGRY_ANIM = "PlayAngryAnim";
    private static final String TAG_ENTRANCE_ANIM_EVALUATED = "EntranceAnimEvaluated";

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private boolean entranceAnimEvaluated;

    public JudgeEntity(EntityType<? extends JudgeEntity> type, Level level)
    {
        super(type, level);
        this.setInvulnerable(true);
        this.setNoGravity(true);
        this.noPhysics = true;
        this.blocksBuilding = false;
    }

    @Override
    public void tick()
    {
        super.tick();
        if (!this.level().isClientSide)
        {
            if (!this.entranceAnimEvaluated && this.tickCount == 1)
            {
                this.prepareEntranceAnimation();
            }
            this.setDeltaMovement(Vec3.ZERO);
            this.move(MoverType.SELF, Vec3.ZERO);
        }
    }

    /** 在 {@code addFreshEntity} 之前调用，使生成包即携带出场动画标记，避免客户端先闪绑定姿势。 */
    public void prepareEntranceAnimation()
    {
        if (this.entranceAnimEvaluated)
        {
            return;
        }
        this.entityData.set(DATA_PLAY_ENTRANCE_ANIM, true);
        this.entranceAnimEvaluated = true;
    }

    /** 在 {@code addFreshEntity} 之前调用，以绑定姿势静态展示，不播放出场动画。 */
    public void prepareStaticDisplay()
    {
        if (this.entranceAnimEvaluated)
        {
            return;
        }
        this.entityData.set(DATA_PLAY_ENTRANCE_ANIM, false);
        this.entranceAnimEvaluated = true;
    }

    /** 目标被判处【死刑】或【附加了没收的死刑】时播放。 */
    public void prepareAngryAnimation()
    {
        this.entityData.set(DATA_PLAY_ANGRY_ANIM, true);
    }

    /** 愤怒动画时长（tick），与 {@code judge_angry_animation} 的 0.4167 秒一致。 */
    public static int angryAnimationDurationTicks()
    {
        return 9;
    }

    /** 出场动画时长（tick），与 {@code judge_appears_animation} 的 2.25 秒一致。 */
    public static int entranceAnimationDurationTicks()
    {
        return 45;
    }

    private boolean shouldPlayAngryAnimation()
    {
        return Boolean.TRUE.equals(this.entityData.get(DATA_PLAY_ANGRY_ANIM));
    }

    private boolean shouldPlayEntranceAnimation()
    {
        if (Boolean.TRUE.equals(this.entityData.get(DATA_PLAY_ENTRANCE_ANIM)))
        {
            return true;
        }
        return this.level().isClientSide && this.tickCount < 10;
    }

    @Override
    public boolean isPushable()
    {
        return false;
    }

    @Override
    public boolean isPickable()
    {
        return false;
    }

    @Override
    public boolean canBeCollidedWith()
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

    @Override
    protected void defineSynchedData()
    {
        this.entityData.define(DATA_PLAY_ENTRANCE_ANIM, false);
        this.entityData.define(DATA_PLAY_ANGRY_ANIM, false);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag)
    {
        if (tag.getBoolean(TAG_ENTRANCE_ANIM_EVALUATED))
        {
            this.entranceAnimEvaluated = true;
            this.entityData.set(DATA_PLAY_ENTRANCE_ANIM, tag.getBoolean(TAG_PLAY_ENTRANCE_ANIM));
        }
        if (tag.contains(TAG_PLAY_ANGRY_ANIM))
        {
            this.entityData.set(DATA_PLAY_ANGRY_ANIM, tag.getBoolean(TAG_PLAY_ANGRY_ANIM));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag)
    {
        if (this.entranceAnimEvaluated)
        {
            tag.putBoolean(TAG_ENTRANCE_ANIM_EVALUATED, true);
            tag.putBoolean(TAG_PLAY_ENTRANCE_ANIM, this.entityData.get(DATA_PLAY_ENTRANCE_ANIM));
        }
        if (this.entityData.get(DATA_PLAY_ANGRY_ANIM))
        {
            tag.putBoolean(TAG_PLAY_ANGRY_ANIM, true);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers)
    {
        controllers.add(new AnimationController<>(this, "angry", 10, this::angryAnimPredicate));
        controllers.add(new AnimationController<>(this, "main", 0, this::entranceAnimPredicate));
    }

    private PlayState angryAnimPredicate(AnimationState<JudgeEntity> state)
    {
        if (this.shouldPlayAngryAnimation())
        {
            state.getController().setAnimation(ANGRY_ANIM);
            return PlayState.CONTINUE;
        }
        return PlayState.STOP;
    }

    private PlayState entranceAnimPredicate(AnimationState<JudgeEntity> state)
    {
        if (this.shouldPlayAngryAnimation())
        {
            return PlayState.STOP;
        }
        if (this.shouldPlayEntranceAnimation())
        {
            state.getController().setAnimation(APPEARS_ANIM);
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
