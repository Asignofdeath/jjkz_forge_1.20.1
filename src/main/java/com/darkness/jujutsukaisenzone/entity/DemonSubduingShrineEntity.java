package com.darkness.jujutsukaisenzone.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * 伏魔神龛：静态 GeckoLib 实体，用于展示大型场景模型。
 */
public class DemonSubduingShrineEntity extends Entity implements GeoEntity
{
    private static final RawAnimation SHRINE_RISES_ANIM = RawAnimation.begin().thenPlay("shrine_rises");
    private static final EntityDataAccessor<Boolean> DATA_PLAY_RISE_ANIM =
            SynchedEntityData.defineId(DemonSubduingShrineEntity.class, EntityDataSerializers.BOOLEAN);
    private static final String TAG_PLAY_RISE_ANIM = "PlayRiseAnim";
    private static final String TAG_RISE_ANIM_EVALUATED = "RiseAnimEvaluated";
    private static final BlockState LIGHT_BLOCK_STATE =
            Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, LightBlock.MAX_LEVEL);
    private static final int LIGHT_BLOCK_UPDATE_FLAGS = 3;
    /** 领域构建完成后延迟放置光明方块的 tick 数：120 tick = 6 秒 */
    private static final int LIGHT_BLOCK_PLACE_DELAY_TICKS = 110;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Map<BlockPos, BlockState> placedLightBlocks = new HashMap<>();
    private boolean riseAnimEvaluated;
    private boolean lightBlocksPlaced;

    public DemonSubduingShrineEntity(EntityType<? extends DemonSubduingShrineEntity> type, Level level)
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
            if (this.tickCount == 1)
            {
                if (!this.riseAnimEvaluated)
                {
                    this.evaluateRiseAnimation();
                }
            }
            if (this.tickCount == LIGHT_BLOCK_PLACE_DELAY_TICKS)
            {
                if (!this.lightBlocksPlaced)
                {
                    this.placeLightBlocksAtSelf();
                }
            }
            this.setDeltaMovement(Vec3.ZERO);
            this.move(MoverType.SELF, Vec3.ZERO);
        }
    }

    @Override
    public void remove(Entity.RemovalReason reason)
    {
        if (!this.level().isClientSide)
        {
            this.restoreLightBlocks();
        }
        super.remove(reason);
    }

    private void placeLightBlocksAtSelf()
    {
        this.lightBlocksPlaced = true;
        AABB box = this.getBoundingBox();
        int minX = Mth.floor(box.minX);
        int maxX = Mth.floor(box.maxX - 1.0E-7D);
        int topY = Mth.floor(box.maxY - 1.0E-7D);
        int minZ = Mth.floor(box.minZ);
        int maxZ = Mth.floor(box.maxZ - 1.0E-7D);

        for (int x = minX; x <= maxX; x++)
        {
            for (int z = minZ; z <= maxZ; z++)
            {
                BlockPos pos = new BlockPos(x, topY, z);
                BlockState existing = this.level().getBlockState(pos);
                if (!existing.isAir())
                {
                    continue;
                }
                this.placedLightBlocks.put(pos.immutable(), existing);
                this.level().setBlock(pos, LIGHT_BLOCK_STATE, LIGHT_BLOCK_UPDATE_FLAGS);
            }
        }
    }

    private void restoreLightBlocks()
    {
        if (this.placedLightBlocks.isEmpty())
        {
            return;
        }

        for (Map.Entry<BlockPos, BlockState> entry : this.placedLightBlocks.entrySet())
        {
            BlockPos pos = entry.getKey();
            if (this.level().getBlockState(pos).is(Blocks.LIGHT))
            {
                this.level().setBlock(pos, entry.getValue(), LIGHT_BLOCK_UPDATE_FLAGS);
            }
        }
        this.placedLightBlocks.clear();
    }

    private void evaluateRiseAnimation()
    {
        boolean playRise = this.isSupportedByGround();
        this.entityData.set(DATA_PLAY_RISE_ANIM, playRise);
        this.riseAnimEvaluated = true;
    }

    /** 碰撞箱底部下方存在实心方块时视为落地生成。 */
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
                BlockPos pos = new BlockPos(x, yBelow, z);
                BlockState state = level.getBlockState(pos);
                if (!state.isAir() && !state.getCollisionShape(level, pos).isEmpty())
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
        this.entityData.define(DATA_PLAY_RISE_ANIM, false);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag)
    {
        if (tag.getBoolean(TAG_RISE_ANIM_EVALUATED))
        {
            this.riseAnimEvaluated = true;
            this.entityData.set(DATA_PLAY_RISE_ANIM, tag.getBoolean(TAG_PLAY_RISE_ANIM));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag)
    {
        if (this.riseAnimEvaluated)
        {
            tag.putBoolean(TAG_RISE_ANIM_EVALUATED, true);
            tag.putBoolean(TAG_PLAY_RISE_ANIM, this.entityData.get(DATA_PLAY_RISE_ANIM));
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers)
    {
        controllers.add(new AnimationController<>(this, "main", 0, this::shrineRisesPredicate));
    }

    private PlayState shrineRisesPredicate(AnimationState<DemonSubduingShrineEntity> state)
    {
        if (Boolean.TRUE.equals(this.entityData.get(DATA_PLAY_RISE_ANIM)))
        {
            state.getController().setAnimation(SHRINE_RISES_ANIM);
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
