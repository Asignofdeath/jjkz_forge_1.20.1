package com.darkness.jujutsukaisenzone.entity.behavior;

import com.darkness.jujutsukaisenzone.entity.SpecialEffectEntity;
import com.darkness.jujutsukaisenzone.entity.data.ShrineKaiConfig;
import com.darkness.jujutsukaisenzone.entity.util.GeometryUtils;
import com.darkness.jujutsukaisenzone.photon.OwnerOnlyShrineSlashFx;
import com.darkness.jujutsukaisenzone.registry.ModEntities;
import com.darkness.jujutsukaisenzone.registry.ModSounds;
import com.darkness.jujutsukaisenzone.technique.TechniqueDamageSources;
import com.darkness.jujutsukaisenzone.technique.scaling.ShrineSkillGrowth;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 解斩击箭：匀速飞行，沿箭上方/侧方柱形清场，到达终点挂 Photon。
 * <p>
 * 同时用于「解」挥砍箭和「解」切割 flyobj（flyobj 模式下不创建终点锚点）。
 */
public class ShrineKaiArrowBehavior extends EffectBehavior
{
    public static final String ID = "shrine_kai_arrow";

    @Nullable
    private UUID ownerUuid;
    private boolean flyobjMode;
    private boolean photonMounted;
    private int slashType = 1; // 1=竖斩 2=横斩
    private Vec3 flightDir = Vec3.ZERO;
    private double maxRange;
    private double traveled;

    @Override
    public String id() { return ID; }

    public void configure(@Nullable LivingEntity owner, double maxRange, Vec3 flightDir, boolean horizontal, boolean flyobjMode)
    {
        this.ownerUuid = owner == null ? null : owner.getUUID();
        this.flyobjMode = flyobjMode;
        this.slashType = horizontal ? 2 : 1;
        this.flightDir = flightDir.lengthSqr() < 1.0E-8D ? new Vec3(0.0D, 0.0D, 1.0D) : flightDir.normalize();
        this.maxRange = maxRange;
        this.traveled = 0.0D;
    }

    @Override
    public void tick(SpecialEffectEntity entity)
    {
        entity.setNoGravity(true);

        if (!(entity.level() instanceof ServerLevel level)) return;
        if (!entity.level().hasChunkAt(entity.blockPosition()))
        {
            entity.discard();
            return;
        }

        if (this.flyobjMode && !this.photonMounted)
        {
            mountFlyobjPhoton(entity);
            this.photonMounted = true;
        }

        double remainingDist = ShrineKaiConfig.ARROW_SPEED;
        while (remainingDist > 0.0D)
        {
            double step = Math.min(remainingDist, 1.0D);
            double nextTraveled = this.traveled + step;
            if (nextTraveled >= this.maxRange)
            {
                double finalDist = this.maxRange - this.traveled;
                if (finalDist > 0.0D)
                {
                    entity.setPos(entity.position().add(this.flightDir.scale(finalDist)));
                    this.traveled = this.maxRange;
                    if (!this.flyobjMode)
                    {
                        applyRotation(entity);
                    }
                    tickColumn(entity);
                }
                arrive(entity);
                return;
            }
            entity.setPos(entity.position().add(this.flightDir.scale(step)));
            this.traveled = nextTraveled;
            if (!this.flyobjMode)
            {
                applyRotation(entity);
            }
            tickColumn(entity);
            remainingDist -= step;
        }
    }

    private void arrive(SpecialEffectEntity entity)
    {
        if (this.flyobjMode)
        {
            removeFlyobjPhoton(entity);
            entity.discard();
            return;
        }

        // 非 flyobj：在终点创建 Photon 锚点
        LivingEntity owner = resolveOwner(entity, this.ownerUuid);
        Vec3 pos = entity.position();

        SpecialEffectEntity fx = ModEntities.SPECIAL_EFFECT_ENTITY.get().create(entity.level());
        if (fx != null)
        {
            if (owner != null)
            {
                double dx = owner.getX() - pos.x;
                double dz = owner.getZ() - pos.z;
                double hd = Math.sqrt(dx * dx + dz * dz);
                float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
                float pitch = (float) -Math.toDegrees(Math.atan2(owner.getEyeY() - pos.y, hd));
                fx.moveTo(pos.x, pos.y, pos.z, yaw, pitch);
            }
            else
            {
                fx.moveTo(pos.x, pos.y, pos.z, entity.getYRot(), entity.getXRot());
            }
            fx.configureShrineKaiArrival(owner, this.slashType);
            entity.level().addFreshEntity(fx);
        }

        entity.discard();
    }

    // ==== Column sweep ====

    private void tickColumn(SpecialEffectEntity entity)
    {
        if (!(entity.level() instanceof ServerLevel level)) return;
        Vec3 columnDir = this.slashType == 2 ? getHorizontalColumnDir() : entity.getUpVector(1.0F);
        Vec3 base = entity.position();
        double rSqr = ShrineKaiConfig.COLUMN_RADIUS * ShrineKaiConfig.COLUMN_RADIUS;

        if (this.slashType == 2)
        {
            int half = ShrineKaiConfig.COLUMN_HEIGHT / 2;
            for (int i = -half; i <= half; i++)
            {
                Vec3 sample = base.add(columnDir.scale(i));
                breakBlocksAtSample(level, sample, rSqr);
                damageAtSample(entity, level, sample, rSqr);
            }
        }
        else
        {
            for (int i = 0; i < ShrineKaiConfig.COLUMN_HEIGHT; i++)
            {
                Vec3 sample = base.add(columnDir.scale(i));
                breakBlocksAtSample(level, sample, rSqr);
                damageAtSample(entity, level, sample, rSqr);
            }
        }
    }

    private Vec3 getHorizontalColumnDir()
    {
        Vec3 cross = new Vec3(-this.flightDir.z, 0.0D, this.flightDir.x);
        double len = cross.length();
        if (len < 1.0E-8D) return new Vec3(1.0D, 0.0D, 0.0D);
        return cross.scale(1.0D / len);
    }

    private void breakBlocksAtSample(Level level, Vec3 sample, double rSqr)
    {
        int ri = Mth.ceil(ShrineKaiConfig.COLUMN_RADIUS);
        int ox = Mth.floor(sample.x), oy = Mth.floor(sample.y), oz = Mth.floor(sample.z);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int dx = -ri; dx <= ri; dx++)
            for (int dy = -ri; dy <= ri; dy++)
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

    private void damageAtSample(SpecialEffectEntity entity, ServerLevel level, Vec3 sample, double rSqr)
    {
        AABB query = new AABB(sample, sample).inflate(ShrineKaiConfig.COLUMN_RADIUS);
        LivingEntity owner = resolveOwner(entity, this.ownerUuid);

        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, query))
        {
            if (!target.isAlive()) continue;
            if (!GeometryUtils.aabbIntersectsSphere(sample.x, sample.y, sample.z, rSqr, target.getBoundingBox())) continue;
            if (owner != null && target.getUUID().equals(owner.getUUID())) continue;
            if (TechniqueDamageSources.deal(target, owner, ShrineSkillGrowth.kaiColumnDamage(owner)))
            {
                SoundEvent hitSound = this.slashType == 2
                        ? ModSounds.SHRINE_SLASH_KAI_2_HIT.get()
                        : ModSounds.SHRINE_SLASH_KAI_1_HIT.get();
                level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                        hitSound, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        }
    }

    private void applyRotation(SpecialEffectEntity entity)
    {
        double h = Math.sqrt(this.flightDir.x * this.flightDir.x + this.flightDir.z * this.flightDir.z);
        float yaw = (float) (Mth.atan2(this.flightDir.x, this.flightDir.z) * (180.0D / Math.PI));
        float pitch = (float) (-Mth.atan2(this.flightDir.y, h) * (180.0D / Math.PI));
        entity.setYRot(yaw);
        entity.setXRot(pitch);
        entity.setYBodyRot(yaw);
        entity.setYHeadRot(yaw);
    }

    // ==== Flyobj Photon ====

    private void mountFlyobjPhoton(SpecialEffectEntity entity)
    {
        ServerPlayer owner = resolveOwnerPlayer(entity, this.ownerUuid);
        if (owner != null)
        {
            OwnerOnlyShrineSlashFx.mountFlyobj(owner, entity);
        }
    }

    private void removeFlyobjPhoton(SpecialEffectEntity entity)
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

    @Override
    public void writeToNbt(CompoundTag tag)
    {
        tag.putBoolean("FlyobjMode", this.flyobjMode);
        tag.putBoolean("PhotonMounted", this.photonMounted);
        tag.putInt("SlashType", this.slashType);
        tag.putDouble("FlightDirX", this.flightDir.x);
        tag.putDouble("FlightDirY", this.flightDir.y);
        tag.putDouble("FlightDirZ", this.flightDir.z);
        tag.putDouble("MaxRange", this.maxRange);
        tag.putDouble("Traveled", this.traveled);
        if (this.ownerUuid != null) tag.putUUID("Owner", this.ownerUuid);
    }

    @Override
    public void readFromNbt(CompoundTag tag)
    {
        this.flyobjMode = tag.getBoolean("FlyobjMode");
        this.photonMounted = tag.getBoolean("PhotonMounted");
        this.slashType = tag.contains("SlashType") ? tag.getInt("SlashType") : 1;
        this.flightDir = new Vec3(tag.getDouble("FlightDirX"), tag.getDouble("FlightDirY"), tag.getDouble("FlightDirZ"));
        if (this.flightDir.lengthSqr() < 1.0E-8D) this.flightDir = new Vec3(0.0D, 0.0D, 1.0D);
        else this.flightDir = this.flightDir.normalize();
        this.maxRange = tag.contains("MaxRange") ? tag.getDouble("MaxRange") : 0.0D;
        this.traveled = tag.contains("Traveled") ? tag.getDouble("Traveled") : 0.0D;
        this.ownerUuid = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
    }
}
