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

import javax.annotation.Nullable;

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
    /** 常规人形：倒地仰躺。geo 骨骼与所有 humanoid 一致，可跨模型共用。 */
    public static final String CLIP_HUMANOID_FALL_DOWN_AND_BACK = "fall_down_and_back";
    /** 五条悟（戴眼罩）外观 id，对应 {@code gojosatoru_teacher.*} 资源。 */
    public static final String APPEARANCE_GOJO = "gojosatoru_teacher";
    public static final String CLIP_GOJO_STAND_UP_AND_PROTECT = "stand_up_and_protect";
    /** 起身保护后转头看一眼玩家、再复原（一次性，定格末帧）。 */
    public static final String CLIP_GOJO_STAND_UP_AND_PROTECT_AND_LOOK_BACK = "stand_up_and_protect_and_look_back";
    public static final String CLIP_GOJO_TAKE_OFF_THE_BLINDFOLD = "take_off_the_blindfold";
    /** 五条悟（摘眼罩）外观 id，对应 {@code gojosatoru_teacher_noeyemask.*} 资源。 */
    public static final String APPEARANCE_GOJO_NO_EYE_MASK = "gojosatoru_teacher_noeyemask";
    public static final String CLIP_GOJO_MURYOUKUUSHO_GESTURE = "muryoukuusho_gesture_noeyemask";
    /** 贴图覆盖哨兵：客户端解析为「本地玩家自己的皮肤」。 */
    public static final String TEXTURE_LOCAL_PLAYER_SKIN = "@local_player_skin";

    private static final EntityDataAccessor<String> DATA_APPEARANCE =
            SynchedEntityData.defineId(CutsceneActorEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_CLIP =
            SynchedEntityData.defineId(CutsceneActorEntity.class, EntityDataSerializers.STRING);
    /** 贴图覆盖：空 = 用 appearance 同名贴图；非空 = 完整资源路径（玩家皮肤等）。 */
    private static final EntityDataAccessor<String> DATA_TEXTURE_OVERRIDE =
            SynchedEntityData.defineId(CutsceneActorEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_LOOP =
            SynchedEntityData.defineId(CutsceneActorEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_CLIP_NONCE =
            SynchedEntityData.defineId(CutsceneActorEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_TARGET_YAW =
            SynchedEntityData.defineId(CutsceneActorEntity.class, EntityDataSerializers.FLOAT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int clientAppliedNonce = Integer.MIN_VALUE;
    private int restoreTicks = -1;
    private String restoreClip = "";
    private boolean restoreLoop;
    /** 每 tick 逼近目标的比例；客户端也跑，避免追踪包把转身切成几帧。 */
    private static final float TURN_LERP = 0.28F;

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

    /** 运行期换外观（如五条悟摘眼罩换模型）。 */
    public void setAppearance(String appearance)
    {
        this.entityData.set(DATA_APPEARANCE, appearance == null ? "" : appearance);
    }

    /** 贴图覆盖：完整资源路径（如玩家皮肤）；传 null / 空 = 回退 appearance 同名贴图。 */
    public void setTextureOverride(@Nullable String texturePath)
    {
        this.entityData.set(DATA_TEXTURE_OVERRIDE, texturePath == null ? "" : texturePath);
    }

    @Nullable
    public String getTextureOverride()
    {
        String value = this.entityData.get(DATA_TEXTURE_OVERRIDE);
        return value == null || value.isEmpty() ? null : value;
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
        this.entityData.set(DATA_TARGET_YAW, Mth.wrapDegrees(yaw));
    }

    /** 出生/瞬切：不插值。 */
    public void snapLookYaw(float yaw)
    {
        float wrapped = Mth.wrapDegrees(yaw);
        this.entityData.set(DATA_TARGET_YAW, wrapped);
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

    private void stepYawTowardTarget()
    {
        float target = this.entityData.get(DATA_TARGET_YAW);
        float current = Mth.wrapDegrees(this.getYRot());
        float delta = Mth.wrapDegrees(target - current);
        if (Math.abs(delta) < 0.15F)
        {
            this.setYRot(target);
            return;
        }
        this.setYRot(current + delta * TURN_LERP);
    }

    /** 朝向由本地插值，不要被位置包的 yaw 拽跳。 */
    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps, boolean teleport)
    {
        super.lerpTo(x, y, z, this.getYRot(), this.getXRot(), steps, teleport);
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
        this.setDeltaMovement(Vec3.ZERO);
        this.move(MoverType.SELF, Vec3.ZERO);
        stepYawTowardTarget();
        if (!this.level().isClientSide)
        {
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
        this.entityData.define(DATA_TARGET_YAW, 0.0F);
        this.entityData.define(DATA_TEXTURE_OVERRIDE, "");
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
        // 主线 1.3：摘眼罩按 0.5 倍速播，墙钟约 3.5s，末帧仍由 JSON hold_on_last_frame 定格。
        if (CLIP_GOJO_TAKE_OFF_THE_BLINDFOLD.equals(clip))
        {
            state.getController().setAnimationSpeed(0.5D);
        }
        else
        {
            state.getController().setAnimationSpeed(1.0D);
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
