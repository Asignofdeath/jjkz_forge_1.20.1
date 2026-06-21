package com.darkness.jujutsukaisenzone.entity.behavior;

import com.darkness.jujutsukaisenzone.compat.EpicFightBoneUtil;
import com.darkness.jujutsukaisenzone.entity.SpecialEffectEntity;
import com.darkness.jujutsukaisenzone.entity.data.LimitlessRedConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 「赫」前摇蓄力：跟随施法者右小臂（{@link EpicFightBoneUtil#RIGHT_FOREARM_JOINT_NAME}）并挂载 Photon。
 */
public final class LimitlessRedChargeBehavior extends EffectBehavior
{
    public static final String ID = "limitless_red_charge";

    @Nullable
    private UUID ownerUuid;
    private int remainingLifeTicks = -1;
    private int chargeAgeTicks;
    private boolean photonMounted;

    @Override
    public String id()
    {
        return ID;
    }

    public void configure(@Nullable LivingEntity owner)
    {
        this.ownerUuid = owner == null ? null : owner.getUUID();
        this.remainingLifeTicks = LimitlessRedConfig.ARROW_DELAY_TICKS;
        this.chargeAgeTicks = 0;
        this.photonMounted = false;
    }

    @Override
    public void tick(SpecialEffectEntity entity)
    {
        entity.setNoGravity(true);

        LivingEntity owner = resolveOwner(entity, this.ownerUuid);
        if (owner == null || !owner.isAlive() || this.remainingLifeTicks-- <= 0)
        {
            entity.discard();
            return;
        }

        syncEntityToOwnerForearm(entity, owner, this.chargeAgeTicks);
        this.chargeAgeTicks++;

        if (!this.photonMounted && entity.getServer() != null)
        {
            CommandSourceStack source = entity.createCommandSourceStack().withPermission(2).withSuppressedOutput();
            entity.getServer().getCommands().performPrefixedCommand(source, LimitlessRedConfig.PHOTON_FLYOBJ_COMMAND);
            this.photonMounted = true;
        }
    }

    /** 将蓄力实体对齐到施法者右小臂，并按蓄力时长应用分阶段挂点偏移。 */
    public static void syncEntityToOwnerForearm(SpecialEffectEntity entity, LivingEntity owner)
    {
        syncEntityToOwnerForearm(entity, owner, 0);
    }

    public static void syncEntityToOwnerForearm(SpecialEffectEntity entity, LivingEntity owner, int chargeAgeTicks)
    {
        EpicFightBoneUtil.BoneTransform bone = EpicFightBoneUtil.getRightForearmWorldTransform(owner, 1.0F);
        if (bone != null)
        {
            Vec3 pos = applyChargeAnchorOffset(bone.position(), owner, chargeAgeTicks);
            entity.moveTo(pos.x, pos.y, pos.z, bone.yRot(), bone.xRot());
            return;
        }
        Vec3 pos = applyChargeAnchorOffset(owner.position(), owner, chargeAgeTicks);
        entity.moveTo(pos.x, pos.y, pos.z, owner.getYRot(), owner.getXRot());
    }

    private static Vec3 applyChargeAnchorOffset(Vec3 anchor, LivingEntity owner, int chargeAgeTicks)
    {
        boolean latePhase = chargeAgeTicks >= LimitlessRedConfig.CHARGE_OFFSET_PHASE_SWITCH_TICKS;
        double forwardOffset = latePhase
                ? LimitlessRedConfig.CHARGE_FORWARD_OFFSET_LATE
                : LimitlessRedConfig.CHARGE_FORWARD_OFFSET_EARLY;
        double downOffset = latePhase ? LimitlessRedConfig.CHARGE_DOWN_OFFSET_LATE : 0.0D;

        Vec3 offset = anchor.add(0.0D, -downOffset, 0.0D);

        Vec3 look = owner.getLookAngle();
        double leftOffset = LimitlessRedConfig.CHARGE_POSITION_LEFT_OFFSET;
        if (leftOffset != 0.0D)
        {
            double planar = Math.sqrt(look.x * look.x + look.z * look.z);
            if (planar >= 1.0E-8D)
            {
                offset = offset.add(look.z / planar * leftOffset, 0.0D, -look.x / planar * leftOffset);
            }
        }

        if (forwardOffset != 0.0D && look.lengthSqr() >= 1.0E-8D)
        {
            offset = offset.add(look.normalize().scale(forwardOffset));
        }

        return offset;
    }

    @Override
    public void writeToNbt(CompoundTag tag)
    {
        tag.putInt("RemainingLife", this.remainingLifeTicks);
        tag.putInt("ChargeAge", this.chargeAgeTicks);
        tag.putBoolean("PhotonMounted", this.photonMounted);
        if (this.ownerUuid != null)
        {
            tag.putUUID("Owner", this.ownerUuid);
        }
    }

    @Override
    public void readFromNbt(CompoundTag tag)
    {
        this.remainingLifeTicks = tag.contains("RemainingLife") ? tag.getInt("RemainingLife") : -1;
        this.chargeAgeTicks = tag.contains("ChargeAge") ? tag.getInt("ChargeAge") : 0;
        this.photonMounted = tag.getBoolean("PhotonMounted");
        this.ownerUuid = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
    }
}
