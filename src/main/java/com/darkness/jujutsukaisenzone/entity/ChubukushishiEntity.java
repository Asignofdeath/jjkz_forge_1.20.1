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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
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
 * 处刑架圈景观：静态 GeckoLib 实体，用于展示大型场景模型。
 */
public class ChubukushishiEntity extends Entity implements GeoEntity
{
    private static final RawAnimation ENTRANCE_ANIM = RawAnimation.begin().thenPlay("chubukushishi_animation");
    private static final EntityDataAccessor<Boolean> DATA_PLAY_ENTRANCE_ANIM =
            SynchedEntityData.defineId(ChubukushishiEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SUPPRESS_ENTRANCE_ANIM =
            SynchedEntityData.defineId(ChubukushishiEntity.class, EntityDataSerializers.BOOLEAN);
    private static final String TAG_PLAY_ENTRANCE_ANIM = "PlayEntranceAnim";
    private static final String TAG_ENTRANCE_ANIM_EVALUATED = "EntranceAnimEvaluated";

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private boolean entranceAnimEvaluated;

    public ChubukushishiEntity(EntityType<? extends ChubukushishiEntity> type, Level level)
    {
        super(type, level);
        this.setInvulnerable(true);
        this.setNoGravity(true);
        this.blocksBuilding = true;
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
        this.entityData.set(DATA_PLAY_ENTRANCE_ANIM, this.isSupportedByGround());
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
        this.entityData.set(DATA_SUPPRESS_ENTRANCE_ANIM, true);
        this.entranceAnimEvaluated = true;
    }

    private boolean shouldPlayEntranceAnimation()
    {
        if (Boolean.TRUE.equals(this.entityData.get(DATA_SUPPRESS_ENTRANCE_ANIM)))
        {
            return false;
        }
        if (Boolean.TRUE.equals(this.entityData.get(DATA_PLAY_ENTRANCE_ANIM)))
        {
            return true;
        }
        /*
         * 客户端在实体数据同步到达前，若已落地则直接以出场动画第 0 帧渲染，
         * 避免多骨骼模型在绑定姿势下整模闪现（伏魔神龛仅移动根骨骼，不明显）。
         */
        return this.level().isClientSide
                && this.tickCount < 10
                && this.isSupportedByGround();
    }

    /** 碰撞箱底部下方存在实心方块时视为落地生成，播放出场动画。 */
    private boolean isSupportedByGround()
    {
        Level level = this.level();
        AABB box = this.getBoundingBox();
        int yBelow = Mth.floor(box.minY) - 1;
        if (yBelow < level.getMinBuildHeight())
        {
            return false;
        }

        int minX = Mth.floor(box.minX);
        int maxX = Mth.floor(box.maxX - 1.0E-7D);
        int minZ = Mth.floor(box.minZ);
        int maxZ = Mth.floor(box.maxZ - 1.0E-7D);

        for (int x = minX; x <= maxX; x++)
        {
            for (int z = minZ; z <= maxZ; z++)
            {
                BlockState state = level.getBlockState(new net.minecraft.core.BlockPos(x, yBelow, z));
                if (!state.isAir() && !state.getCollisionShape(level, new net.minecraft.core.BlockPos(x, yBelow, z)).isEmpty())
                {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean isPickable()
    {
        return true;
    }

    @Override
    public boolean canBeCollidedWith()
    {
        return !this.isRemoved();
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
        this.entityData.define(DATA_SUPPRESS_ENTRANCE_ANIM, false);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag)
    {
        if (tag.getBoolean(TAG_ENTRANCE_ANIM_EVALUATED))
        {
            this.entranceAnimEvaluated = true;
            this.entityData.set(DATA_PLAY_ENTRANCE_ANIM, tag.getBoolean(TAG_PLAY_ENTRANCE_ANIM));
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
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers)
    {
        controllers.add(new AnimationController<>(this, "main", 0, this::entranceAnimPredicate));
    }

    private PlayState entranceAnimPredicate(AnimationState<ChubukushishiEntity> state)
    {
        if (this.shouldPlayEntranceAnimation())
        {
            state.getController().setAnimation(ENTRANCE_ANIM);
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
