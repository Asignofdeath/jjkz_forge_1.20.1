package com.darkness.jujutsukaisenzone.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
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
 * CG / 任务演出用 Geo 演员：无 AI、无重力、无碰撞、不进存档。
 * 物理（朝向）与动画 clip 分开设。
 */
public class CutsceneActorEntity extends Entity implements GeoEntity
{
    public static final String APPEARANCE_XIAOSHIZI = "xiaoshizi";
    public static final String CLIP_XIAOSHIZI_SITTING_ON_THE_BED = "xiaoshizi_sitting_on_thebed";
    public static final String CLIP_XIAOSHIZI_LOOKDOWN = "xiaoshizi_sitting_on_thebed_lookdown";
    public static final String CLIP_XIAOSHIZI_NOD = "xiaoshizi_sitting_on_thebed_nod";
    public static final String CLIP_XIAOSHIZI_WAVE = "xiaoshizi_sitting_on_thebed_wave";
    public static final String APPEARANCE_KIRO = "kiro";
    public static final String CLIP_KIRO_STAND_IDLE = "geo_human_entity_stand_idle";
    public static final String CLIP_KIRO_CRAFT_AT_THE_WORKBENCH = "craft_at_the_workbench";
    public static final String CLIP_KIRO_REACH_OUT = "reach_out_your_hand";

    private static final EntityDataAccessor<String> DATA_APPEARANCE =
            SynchedEntityData.defineId(CutsceneActorEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_CLIP =
            SynchedEntityData.defineId(CutsceneActorEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_LOOP =
            SynchedEntityData.defineId(CutsceneActorEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_CLIP_NONCE =
            SynchedEntityData.defineId(CutsceneActorEntity.class, EntityDataSerializers.INT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int clientAppliedNonce = Integer.MIN_VALUE;
    private int restoreTicks = -1;
    private String restoreClip = "";
    private boolean restoreLoop;

    public CutsceneActorEntity(EntityType<? extends CutsceneActorEntity> type, Level level)
    {
        super(type, level);
        this.setInvulnerable(true);
        this.setNoGravity(true);
        this.noPhysics = true;
        this.blocksBuilding = false;
    }

    /** 在 {@code addFreshEntity} 之前调用。 */
    public void prepare(String appearance, String clip)
    {
        this.entityData.set(DATA_APPEARANCE, appearance == null ? "" : appearance);
        setClip(clip, true);
    }

    public void setClip(String clip, boolean loop)
    {
        String next = clip == null ? "" : clip;
        this.restoreTicks = -1;
        this.entityData.set(DATA_CLIP, next);
        this.entityData.set(DATA_LOOP, loop);
        this.entityData.set(DATA_CLIP_NONCE, this.entityData.get(DATA_CLIP_NONCE) + 1);
    }

    /** 播一次后回到 {@code restoreClip}（空 clip = 默认姿势）。 */
    public void playOnceThen(String clip, String restoreClip, boolean restoreLoop, int durationTicks)
    {
        setClip(clip, false);
        this.restoreClip = restoreClip == null ? "" : restoreClip;
        this.restoreLoop = restoreLoop;
        this.restoreTicks = Math.max(0, durationTicks);
    }

    public void lookYaw(float yaw)
    {
        float wrapped = Mth.wrapDegrees(yaw);
        this.setYRot(wrapped);
        this.yRotO = wrapped;
    }

    public void lookAt(Vec3 target)
    {
        double dx = target.x - this.getX();
        double dz = target.z - this.getZ();
        lookYaw((float) Math.toDegrees(Math.atan2(-dx, dz)));
    }

    public String getAppearance()
    {
        return this.entityData.get(DATA_APPEARANCE);
    }

    public String getClip()
    {
        return this.entityData.get(DATA_CLIP);
    }

    public boolean isClipLoop()
    {
        return Boolean.TRUE.equals(this.entityData.get(DATA_LOOP));
    }

    @Override
    public boolean shouldBeSaved()
    {
        return false;
    }

    @Override
    public void tick()
    {
        super.tick();
        if (!this.level().isClientSide)
        {
            this.setDeltaMovement(Vec3.ZERO);
            this.move(MoverType.SELF, Vec3.ZERO);
            if (this.restoreTicks > 0)
            {
                this.restoreTicks--;
                if (this.restoreTicks == 0)
                {
                    setClip(this.restoreClip, this.restoreLoop);
                }
            }
        }
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
        this.entityData.define(DATA_APPEARANCE, "");
        this.entityData.define(DATA_CLIP, "");
        this.entityData.define(DATA_LOOP, true);
        this.entityData.define(DATA_CLIP_NONCE, 0);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag)
    {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag)
    {
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers)
    {
        controllers.add(new AnimationController<>(this, "main", 0, this::predicate));
    }

    private PlayState predicate(AnimationState<CutsceneActorEntity> state)
    {
        int nonce = this.entityData.get(DATA_CLIP_NONCE);
        if (nonce != this.clientAppliedNonce)
        {
            state.getController().forceAnimationReset();
            this.clientAppliedNonce = nonce;
        }
        String clip = this.getClip();
        if (clip == null || clip.isEmpty())
        {
            return PlayState.STOP;
        }
        if (this.isClipLoop())
        {
            state.getController().setAnimation(RawAnimation.begin().thenLoop(clip));
        }
        else
        {
            state.getController().setAnimation(RawAnimation.begin().thenPlay(clip));
        }
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache()
    {
        return cache;
    }
}
