package com.darkness.jujutsukaisenzone.entity.behavior;

import com.darkness.jujutsukaisenzone.entity.SpecialEffectEntity;
import com.darkness.jujutsukaisenzone.entity.data.ShrineKaiConfig;
import com.darkness.jujutsukaisenzone.entity.util.GeometryUtils;
import com.darkness.jujutsukaisenzone.photon.OwnerOnlyShrineSlashFx;
import com.darkness.jujutsukaisenzone.registry.ModSounds;
import com.darkness.jujutsukaisenzone.technique.scaling.ShrineSkillGrowth;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 「解」切割 flyobj：沿视线飞出，全程挂载 Photon，横斩柱形清场。
 */
public class ShrineKaiFlyobjBehavior extends EffectBehavior
{
    public static final String ID = "shrine_kai_flyobj";

    @Nullable
    private UUID ownerUuid;
    private boolean photonMounted;
    private Vec3 flightDir = Vec3.ZERO;
    private double flightMaxRange;
    private double flightTraveled;
    private double flightSpeedPerTick;

    @Override
    public String id() { return ID; }

    public void configure(@Nullable LivingEntity owner, double maxRange, Vec3 flightDir)
    {
        this.ownerUuid = owner == null ? null : owner.getUUID();
        this.photonMounted = false;
        this.flightDir = flightDir.lengthSqr() < 1.0E-8D ? new Vec3(0.0D, 0.0D, 1.0D) : flightDir.normalize();
        this.flightMaxRange = maxRange;
        this.flightTraveled = 0.0D;
        this.flightSpeedPerTick = ShrineKaiConfig.FLYOBJ_SPEED;
    }

    @Override
    public void tick(SpecialEffectEntity entity)
    {
        if (!(entity.level() instanceof ServerLevel level)) return;

        if (!this.photonMounted)
        {
            mountPhoton(entity);
            this.photonMounted = true;
        }

        // 匀速飞行
        LinearFlightResult result = tickFlight(entity);
        if (result == LinearFlightResult.ARRIVED || result == LinearFlightResult.ABORT_CHUNK_UNLOADED)
        {
            removePhotonIfMounted(entity);
            entity.discard();
        }
    }

    // ==== 飞行 ====

    public enum LinearFlightResult { CONTINUING, ARRIVED, ABORT_CHUNK_UNLOADED }

    private LinearFlightResult tickFlight(SpecialEffectEntity entity)
    {
        if (!entity.level().hasChunkAt(entity.blockPosition())) return LinearFlightResult.ABORT_CHUNK_UNLOADED;

        double remainingDist = this.flightSpeedPerTick;
        while (remainingDist > 0.0D)
        {
            double step = Math.min(remainingDist, 1.0D);
            double nextTraveled = this.flightTraveled + step;
            if (nextTraveled >= this.flightMaxRange)
            {
                double finalDist = this.flightMaxRange - this.flightTraveled;
                if (finalDist > 0.0D)
                {
                    entity.setPos(entity.position().add(this.flightDir.scale(finalDist)));
                    this.flightTraveled = this.flightMaxRange;
                    tickColumn(entity);
                }
                return LinearFlightResult.ARRIVED;
            }
            entity.setPos(entity.position().add(this.flightDir.scale(step)));
            this.flightTraveled = nextTraveled;
            tickColumn(entity);
            remainingDist -= step;
        }
        return LinearFlightResult.CONTINUING;
    }

    // ==== 柱形清场 ====

    private void tickColumn(SpecialEffectEntity entity)
    {
        if (!(entity.level() instanceof ServerLevel level)) return;
        Vec3 columnDir = getHorizontalColumnDir();
        Vec3 base = entity.position();
        double rSqr = ShrineKaiConfig.COLUMN_RADIUS * ShrineKaiConfig.COLUMN_RADIUS;
        int half = ShrineKaiConfig.COLUMN_HEIGHT / 2;
        for (int i = -half; i <= half; i++)
        {
            Vec3 sample = base.add(columnDir.scale(i));
            breakBlocksAtColumnSample(level, sample, rSqr);
            damageLivingEntitiesAtColumnSample(entity, level, sample, rSqr);
        }
    }

    private Vec3 getHorizontalColumnDir()
    {
        Vec3 dir = this.flightDir;
        Vec3 cross = new Vec3(-dir.z, 0.0D, dir.x);
        double len = cross.length();
        if (len < 1.0E-8D) return new Vec3(1.0D, 0.0D, 0.0D);
        return cross.scale(1.0D / len);
    }

    private void breakBlocksAtColumnSample(Level level, Vec3 sample, double rSqr)
    {
        int ri = Mth.ceil(ShrineKaiConfig.COLUMN_RADIUS);
        int ox = Mth.floor(sample.x);
        int oy = Mth.floor(sample.y);
        int oz = Mth.floor(sample.z);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int dx = -ri; dx <= ri; dx++)
        {
            for (int dy = -ri; dy <= ri; dy++)
            {
                for (int dz = -ri; dz <= ri; dz++)
                {
                    pos.set(ox + dx, oy + dy, oz + dz);
                    if (level.isOutsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos)) continue;
                    if (!GeometryUtils.blockCubeIntersectsSphere(sample.x, sample.y, sample.z, rSqr, pos.getX(), pos.getY(), pos.getZ())) continue;
                    if (level.getBlockState(pos).isAir()) continue;
                    if (level.getBlockState(pos).getDestroySpeed(level, pos) < 0.0F) continue;
                    level.destroyBlock(pos, false);
                }
            }
        }
    }

    private void damageLivingEntitiesAtColumnSample(SpecialEffectEntity entity, ServerLevel level, Vec3 sample, double rSqr)
    {
        AABB query = new AABB(sample, sample).inflate(ShrineKaiConfig.COLUMN_RADIUS);
        LivingEntity owner = resolveOwner(entity, this.ownerUuid);

        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, query))
        {
            if (!target.isAlive()) continue;
            if (!GeometryUtils.aabbIntersectsSphere(sample.x, sample.y, sample.z, rSqr, target.getBoundingBox())) continue;
            if (owner != null && target.getUUID().equals(owner.getUUID())) continue;
            if (target.hurt(entity.damageSources().magic(), ShrineSkillGrowth.kaiColumnDamage(owner)))
            {
                level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                        ModSounds.SHRINE_SLASH_KAI_2_HIT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        }
    }

    // ==== Photon ====

    private void mountPhoton(SpecialEffectEntity entity)
    {
        ServerPlayer owner = resolveOwnerPlayer(entity, this.ownerUuid);
        if (owner != null)
        {
            OwnerOnlyShrineSlashFx.mountFlyobj(owner, entity);
        }
    }

    private void removePhotonIfMounted(SpecialEffectEntity entity)
    {
        if (!this.photonMounted)
        {
            return;
        }
        ServerPlayer owner = resolveOwnerPlayer(entity, this.ownerUuid);
        if (owner != null)
        {
            OwnerOnlyShrineSlashFx.removeFlyobj(owner, entity);
        }
        this.photonMounted = false;
    }

    // ==== NBT ====

    @Override
    public void writeToNbt(CompoundTag tag)
    {
        tag.putBoolean("PhotonMounted", this.photonMounted);
        tag.putDouble("FlightDirX", this.flightDir.x);
        tag.putDouble("FlightDirY", this.flightDir.y);
        tag.putDouble("FlightDirZ", this.flightDir.z);
        tag.putDouble("FlightMaxRange", this.flightMaxRange);
        tag.putDouble("FlightTraveled", this.flightTraveled);
        tag.putDouble("FlightSpeed", this.flightSpeedPerTick);
        if (this.ownerUuid != null) tag.putUUID("Owner", this.ownerUuid);
    }

    @Override
    public void readFromNbt(CompoundTag tag)
    {
        this.photonMounted = tag.getBoolean("PhotonMounted");
        this.flightDir = new Vec3(tag.getDouble("FlightDirX"), tag.getDouble("FlightDirY"), tag.getDouble("FlightDirZ"));
        if (this.flightDir.lengthSqr() < 1.0E-8D) this.flightDir = new Vec3(0.0D, 0.0D, 1.0D);
        else this.flightDir = this.flightDir.normalize();
        this.flightMaxRange = tag.contains("FlightMaxRange") ? tag.getDouble("FlightMaxRange") : 0.0D;
        this.flightTraveled = tag.contains("FlightTraveled") ? tag.getDouble("FlightTraveled") : 0.0D;
        this.flightSpeedPerTick = tag.contains("FlightSpeed") ? tag.getDouble("FlightSpeed") : 0.0D;
        this.ownerUuid = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
    }
}
