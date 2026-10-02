package com.darkness.jujutsukaisenzone.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 咒术议会工作人员：Steve 体型站桩 NPC（玩家皮肤），不可交易、不可移动、不可受伤。
 * 通过 {@link StaffRole} 区分接待 / 委托 / 出击等岗位；音效用原版村民。
 */
public class JujutsuCouncilStaffEntity extends Mob
{
    private static final float LOOK_RANGE = 8.0F;
    private static final String TAG_STAFF_ROLE = "StaffRole";
    private static final EntityDataAccessor<String> DATA_STAFF_ROLE =
            SynchedEntityData.defineId(JujutsuCouncilStaffEntity.class, EntityDataSerializers.STRING);

    public JujutsuCouncilStaffEntity(EntityType<? extends JujutsuCouncilStaffEntity> type, Level level)
    {
        super(type, level);
        this.setPersistenceRequired();
        this.setInvulnerable(true);
        this.refreshRoleNameplate();
    }

    public static AttributeSupplier.Builder createAttributes()
    {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void defineSynchedData()
    {
        super.defineSynchedData();
        this.entityData.define(DATA_STAFF_ROLE, StaffRole.GENERIC.getId());
    }

    public StaffRole getStaffRole()
    {
        return StaffRole.byId(this.entityData.get(DATA_STAFF_ROLE));
    }

    public void setStaffRole(StaffRole role)
    {
        StaffRole resolved = role == null ? StaffRole.GENERIC : role;
        this.entityData.set(DATA_STAFF_ROLE, resolved.getId());
        this.refreshRoleNameplate();
    }

    /** 头顶常显岗位名（随 StaffRole 更新）。 */
    private void refreshRoleNameplate()
    {
        this.setCustomName(Component.translatable(this.getStaffRole().translationKey()));
        this.setCustomNameVisible(true);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key)
    {
        super.onSyncedDataUpdated(key);
        if (DATA_STAFF_ROLE.equals(key))
        {
            this.refreshRoleNameplate();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);
        tag.putString(TAG_STAFF_ROLE, this.getStaffRole().getId());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        if (tag.contains(TAG_STAFF_ROLE))
        {
            this.setStaffRole(StaffRole.byId(tag.getString(TAG_STAFF_ROLE)));
        }
        else
        {
            this.refreshRoleNameplate();
        }
    }

    @Override
    public Component getName()
    {
        return Component.translatable(this.getStaffRole().translationKey());
    }

    @Override
    protected void registerGoals()
    {
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, LOOK_RANGE, 1.0F));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    @Override
    public void tick()
    {
        super.tick();
        this.setDeltaMovement(this.getDeltaMovement().multiply(0.0D, 1.0D, 0.0D));
        this.xxa = 0.0F;
        this.yya = 0.0F;
        this.zza = 0.0F;
    }

    @Override
    public void travel(Vec3 travelVector)
    {
        super.travel(Vec3.ZERO);
        this.setDeltaMovement(this.getDeltaMovement().multiply(0.0D, 1.0D, 0.0D));
    }

    @Override
    public void aiStep()
    {
        super.aiStep();
        this.setDeltaMovement(this.getDeltaMovement().multiply(0.0D, 1.0D, 0.0D));
    }

    @Override
    protected SoundEvent getAmbientSound()
    {
        return SoundEvents.VILLAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource)
    {
        return SoundEvents.VILLAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound()
    {
        return SoundEvents.VILLAGER_DEATH;
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
    public boolean isPushable()
    {
        return false;
    }

    @Override
    public void push(Entity entity)
    {
    }

    @Override
    protected void doPush(Entity entity)
    {
    }

    @Override
    protected void pushEntities()
    {
    }

    @Override
    public void knockback(double strength, double x, double z)
    {
    }

    @Override
    public boolean canBeLeashed(Player player)
    {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer)
    {
        return false;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand)
    {
        this.getLookControl().setLookAt(player, 40.0F, 40.0F);
        if (!this.level().isClientSide)
        {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL,
                    1.0F, this.getVoicePitch());
            // 引导 / 委托 / 副本 UI 尚未接入；先按岗位回这条占位提示。
            player.sendSystemMessage(Component.translatable(this.getStaffRole().interactMessageKey()));
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }
}
