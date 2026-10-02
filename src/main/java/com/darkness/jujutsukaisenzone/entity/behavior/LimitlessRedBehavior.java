package com.darkness.jujutsukaisenzone.entity.behavior;

import com.darkness.jujutsukaisenzone.entity.SpecialEffectEntity;
import com.darkness.jujutsukaisenzone.entity.data.LimitlessRedConfig;
import com.darkness.jujutsukaisenzone.photon.PhotonFxCommands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 无下限术式「赫」：前摇蓄力 + 延迟发射红箭，全程使用同一特效实体。
 */
public class LimitlessRedBehavior extends EffectBehavior
{
    public static final String ID = "limitless_red";

    @Nullable
    private UUID ownerUuid;
    private int chargeTicksRemaining = -1;
    private int chargeAgeTicks;
    private boolean arrowLaunched;
    private boolean photonMounted;
    private boolean allowOwnerKnockback;

    @Override
    public String id() { return ID; }

    public void configure(@Nullable LivingEntity owner, boolean allowOwnerKnockback)
    {
        this.ownerUuid = owner == null ? null : owner.getUUID();
        this.chargeTicksRemaining = LimitlessRedConfig.ARROW_DELAY_TICKS;
        this.chargeAgeTicks = 0;
        this.arrowLaunched = false;
        this.photonMounted = false;
        this.allowOwnerKnockback = allowOwnerKnockback;
    }

    @Override
    public void tick(SpecialEffectEntity entity)
    {
        if (!(entity.level() instanceof ServerLevel))
        {
            return;
        }

        LivingEntity owner = resolveOwner(entity, this.ownerUuid);
        if (owner == null || !owner.isAlive())
        {
            entity.discard();
            return;
        }

        if (this.arrowLaunched)
        {
            return;
        }

        entity.setNoGravity(true);
        LimitlessRedChargeBehavior.syncEntityToOwnerForearm(entity, owner, this.chargeAgeTicks);
        this.chargeAgeTicks++;

        if (!this.photonMounted && entity.getServer() != null)
        {
            PhotonFxCommands.perform(entity, LimitlessRedConfig.PHOTON_FLYOBJ_COMMAND);
            this.photonMounted = true;
        }

        if (this.chargeTicksRemaining-- <= 0)
        {
            this.arrowLaunched = true;
            launchArrow(entity, owner);
        }
    }

    private void launchArrow(SpecialEffectEntity entity, LivingEntity owner)
    {
        Vec3 look = owner.getLookAngle();
        Vec3 spawn = entity.position().add(look.scale(LimitlessRedConfig.ARROW_SPAWN_FORWARD));
        entity.moveTo(
                spawn.x,
                spawn.y + LimitlessRedConfig.ARROW_SPAWN_Y_OFFSET,
                spawn.z,
                owner.getYRot(),
                owner.getXRot());

        LimitlessRedArrowBehavior arrowBehavior = new LimitlessRedArrowBehavior();
        arrowBehavior.configure(owner, this.allowOwnerKnockback, look.scale(LimitlessRedConfig.ARROW_SPEED));
        entity.attach(arrowBehavior);
    }

    @Override
    public void writeToNbt(CompoundTag tag)
    {
        tag.putInt("ChargeTicksRemaining", this.chargeTicksRemaining);
        tag.putInt("ChargeAge", this.chargeAgeTicks);
        tag.putBoolean("ArrowLaunched", this.arrowLaunched);
        tag.putBoolean("PhotonMounted", this.photonMounted);
        tag.putBoolean("AllowOwnerKnockback", this.allowOwnerKnockback);
        if (this.ownerUuid != null) tag.putUUID("Owner", this.ownerUuid);
    }

    @Override
    public void readFromNbt(CompoundTag tag)
    {
        this.chargeTicksRemaining = tag.contains("ChargeTicksRemaining")
                ? tag.getInt("ChargeTicksRemaining")
                : LimitlessRedConfig.ARROW_DELAY_TICKS;
        this.chargeAgeTicks = tag.contains("ChargeAge") ? tag.getInt("ChargeAge") : 0;
        this.arrowLaunched = tag.getBoolean("ArrowLaunched");
        this.photonMounted = tag.getBoolean("PhotonMounted");
        this.allowOwnerKnockback = tag.getBoolean("AllowOwnerKnockback");
        this.ownerUuid = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
    }
}
