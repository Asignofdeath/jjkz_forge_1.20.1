package com.darkness.jujutsukaisenzone.entity.behavior;

import com.darkness.jujutsukaisenzone.entity.SpecialEffectEntity;
import com.darkness.jujutsukaisenzone.photon.OwnerOnlyShrineSlashFx;
import com.darkness.jujutsukaisenzone.entity.data.ShrineHachiConfig;
import com.darkness.jujutsukaisenzone.network.ModNetwork;
import com.darkness.jujutsukaisenzone.technique.scaling.ShrineSkillGrowth;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 捌：在目标身上挂 Photon，锁移动，周期伤害，持续到期后释放。
 */
public class ShrineHachiBehavior extends EffectBehavior
{
    public static final String ID = "shrine_hachi";

    @Nullable
    private UUID ownerUuid;
    @Nullable
    private UUID targetUuid;
    private int remainingLifeTicks = -1;
    private int damageTicks;
    private boolean photonMounted;
    private Vec3 lockPos = Vec3.ZERO;
    private float lockYaw;
    private float lockXRot;
    private boolean aiPausedByLock;
    private boolean originNoAi;
    private boolean playerInputLocked;

    @Override
    public String id() { return ID; }

    public void configure(@Nullable LivingEntity owner, LivingEntity target)
    {
        this.ownerUuid = owner == null ? null : owner.getUUID();
        this.targetUuid = target.getUUID();
        this.remainingLifeTicks = ShrineHachiConfig.LIFETIME_TICKS;
        this.damageTicks = 0;
        this.photonMounted = false;
        this.lockPos = target.position();
        this.lockYaw = target.getYRot();
        this.lockXRot = target.getXRot();
        this.aiPausedByLock = false;
        this.originNoAi = false;
        this.playerInputLocked = false;

        if (target instanceof Mob mob)
        {
            this.originNoAi = mob.isNoAi();
            if (!mob.isNoAi())
            {
                mob.setNoAi(true);
                this.aiPausedByLock = true;
            }
        }
        if (target instanceof ServerPlayer serverPlayer)
        {
            ModNetwork.sendPlayerInputLockSync(serverPlayer, true);
            this.playerInputLocked = true;
        }
        applyMovementLock(target);
    }

    @Override
    public void tick(SpecialEffectEntity entity)
    {
        if (!(entity.level() instanceof ServerLevel level)) return;

        LivingEntity target = resolveTarget(level);
        if (target == null || !target.isAlive())
        {
            finish(entity, null);
            return;
        }

        entity.setPos(target.getX(), target.getY(), target.getZ());
        applyMovementLock(target);

        if (!this.photonMounted)
        {
            mountPhoton(target);
            this.photonMounted = true;
        }

        this.damageTicks++;
        if (this.damageTicks >= ShrineHachiConfig.DAMAGE_INTERVAL)
        {
            this.damageTicks = 0;
            dealDamage(entity, level, target);
        }

        this.remainingLifeTicks--;
        if (this.remainingLifeTicks <= 0)
        {
            finish(entity, target);
        }
    }

    @Nullable
    private LivingEntity resolveTarget(ServerLevel level)
    {
        if (this.targetUuid == null) return null;
        return level.getEntity(this.targetUuid) instanceof LivingEntity living ? living : null;
    }

    private void dealDamage(SpecialEffectEntity entity, ServerLevel level, LivingEntity target)
    {
        LivingEntity owner = resolveOwner(entity, this.ownerUuid);
        DamageSource source = owner == null ? entity.damageSources().magic() : owner.damageSources().magic();
        target.hurt(source, ShrineSkillGrowth.hachiDamage(owner));
    }

    private void applyMovementLock(LivingEntity target)
    {
        target.setDeltaMovement(Vec3.ZERO);
        target.hurtMarked = true;
        target.teleportTo(this.lockPos.x, this.lockPos.y, this.lockPos.z);
        target.setYRot(this.lockYaw);
        target.setYBodyRot(this.lockYaw);
        target.setYHeadRot(this.lockYaw);
        target.setXRot(this.lockXRot);
        if (target instanceof ServerPlayer serverPlayer)
        {
            serverPlayer.connection.teleport(this.lockPos.x, this.lockPos.y, this.lockPos.z, this.lockYaw, this.lockXRot);
        }
        if (target instanceof Mob mob && this.aiPausedByLock)
        {
            mob.setNoAi(true);
        }
    }

    private void releaseMovementLock(@Nullable LivingEntity target, ServerLevel level)
    {
        if (target == null && this.targetUuid != null)
        {
            if (level.getEntity(this.targetUuid) instanceof LivingEntity living) target = living;
        }
        if (target instanceof Mob mob && this.aiPausedByLock)
        {
            mob.setNoAi(this.originNoAi);
        }
        if (this.playerInputLocked)
        {
            ServerPlayer lockedPlayer = null;
            if (target instanceof ServerPlayer sp) lockedPlayer = sp;
            else if (this.targetUuid != null && level.getServer() != null)
                lockedPlayer = level.getServer().getPlayerList().getPlayer(this.targetUuid);
            if (lockedPlayer != null) ModNetwork.sendPlayerInputLockSync(lockedPlayer, false);
        }
        this.aiPausedByLock = false;
        this.playerInputLocked = false;
    }

    private void mountPhoton(LivingEntity target)
    {
        ServerPlayer owner = resolveOwnerPlayerFromLevel(target, this.ownerUuid);
        if (owner != null)
        {
            OwnerOnlyShrineSlashFx.mountHachi(owner, target);
        }
    }

    private void removePhoton(@Nullable LivingEntity target)
    {
        if (!this.photonMounted || target == null || !target.isAlive())
        {
            return;
        }
        ServerPlayer owner = resolveOwnerPlayerFromLevel(target, this.ownerUuid);
        if (owner != null)
        {
            OwnerOnlyShrineSlashFx.removeHachi(owner, target);
        }
        this.photonMounted = false;
    }

    @Nullable
    private static ServerPlayer resolveOwnerPlayerFromLevel(LivingEntity context, @Nullable UUID ownerUuid)
    {
        if (ownerUuid == null || !(context.level() instanceof ServerLevel level) || level.getServer() == null)
        {
            return null;
        }
        return level.getServer().getPlayerList().getPlayer(ownerUuid);
    }

    private void finish(SpecialEffectEntity entity, @Nullable LivingEntity target)
    {
        if (entity.level() instanceof ServerLevel level)
        {
            if (target == null) target = resolveTarget(level);
            releaseMovementLock(target, level);
        }
        removePhoton(target);
        entity.discard();
    }

    @Override
    public void onDiscard(SpecialEffectEntity entity)
    {
        // 如果实体被外部原因移除，确保释放所有锁
        if (entity.level() instanceof ServerLevel level)
        {
            LivingEntity target = resolveTarget(level);
            releaseMovementLock(target, level);
            removePhoton(target);
        }
    }

    @Override
    public void writeToNbt(CompoundTag tag)
    {
        tag.putInt("RemainingLife", this.remainingLifeTicks);
        tag.putInt("DamageTicks", this.damageTicks);
        tag.putBoolean("PhotonMounted", this.photonMounted);
        tag.putDouble("LockX", this.lockPos.x);
        tag.putDouble("LockY", this.lockPos.y);
        tag.putDouble("LockZ", this.lockPos.z);
        tag.putFloat("LockYaw", this.lockYaw);
        tag.putFloat("LockXRot", this.lockXRot);
        tag.putBoolean("AiPaused", this.aiPausedByLock);
        tag.putBoolean("OriginNoAi", this.originNoAi);
        tag.putBoolean("InputLocked", this.playerInputLocked);
        if (this.ownerUuid != null) tag.putUUID("Owner", this.ownerUuid);
        if (this.targetUuid != null) tag.putUUID("Target", this.targetUuid);
    }

    @Override
    public void readFromNbt(CompoundTag tag)
    {
        this.remainingLifeTicks = tag.contains("RemainingLife") ? tag.getInt("RemainingLife") : -1;
        this.damageTicks = tag.contains("DamageTicks") ? tag.getInt("DamageTicks") : 0;
        this.photonMounted = tag.getBoolean("PhotonMounted");
        this.lockPos = new Vec3(tag.getDouble("LockX"), tag.getDouble("LockY"), tag.getDouble("LockZ"));
        this.lockYaw = tag.contains("LockYaw") ? tag.getFloat("LockYaw") : 0.0F;
        this.lockXRot = tag.contains("LockXRot") ? tag.getFloat("LockXRot") : 0.0F;
        this.aiPausedByLock = tag.getBoolean("AiPaused");
        this.originNoAi = tag.getBoolean("OriginNoAi");
        this.playerInputLocked = tag.getBoolean("InputLocked");
        this.ownerUuid = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        this.targetUuid = tag.hasUUID("Target") ? tag.getUUID("Target") : null;
    }
}
