package com.darkness.jujutsukaisenzone.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * 法庭座位：静态 GeckoLib 实体，用于展示场景模型。
 */
public class CourtSeatEntity extends Entity implements GeoEntity
{
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public CourtSeatEntity(EntityType<? extends CourtSeatEntity> type, Level level)
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
            this.setDeltaMovement(Vec3.ZERO);
            this.move(MoverType.SELF, Vec3.ZERO);
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
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache()
    {
        return cache;
    }
}
