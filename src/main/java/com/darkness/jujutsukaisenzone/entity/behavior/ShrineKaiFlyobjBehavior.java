package com.darkness.jujutsukaisenzone.entity.behavior;

import com.darkness.jujutsukaisenzone.entity.SpecialEffectEntity;
import com.darkness.jujutsukaisenzone.entity.data.ShrineKaiConfig;
import com.darkness.jujutsukaisenzone.entity.util.GeometryUtils;
import com.darkness.jujutsukaisenzone.photon.OwnerOnlyShrineSlashFx;
import com.darkness.jujutsukaisenzone.registry.ModSounds;
import com.darkness.jujutsukaisenzone.technique.TechniqueDamageSources;
import com.darkness.jujutsukaisenzone.technique.scaling.ShrineSkillGrowth;
import com.darkness.jujutsukaisenzone.technique.skillset.ShrineTechniqueSkillSet;
import com.darkness.jujutsukaisenzone.technique.se.ShrineFinishCutMarks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    /** 破方块遍历复用的坐标对象（避免每采样点 new 一个）。 */
    private final BlockPos.MutableBlockPos scratchPos = new BlockPos.MutableBlockPos();

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

        // 本 tick 的每格柱基点：先逐格推进（破方块 + 记基点），走完再统一结算伤害。
        List<Vec3> columnBases = new ArrayList<>(16);
        try
        {
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
                        tickColumn(entity, columnBases);
                    }
                    return LinearFlightResult.ARRIVED;
                }
                entity.setPos(entity.position().add(this.flightDir.scale(step)));
                this.flightTraveled = nextTraveled;
                tickColumn(entity, columnBases);
                remainingDist -= step;
            }
            return LinearFlightResult.CONTINUING;
        }
        finally
        {
            // finally：ARRIVED 早退也要把本 tick 的伤害结掉。
            resolveAndSettleHits(entity, columnBases);
        }
    }

    // ==== 柱形清场 ====

    /**
     * 逐格推进：破方块 + 记下本格的柱基点。
     * <p>
     * 伤害判定不在这里做（采样点集合与原来完全一致），详见 {@link #resolveAndSettleHits}。
     */
    private void tickColumn(SpecialEffectEntity entity, List<Vec3> columnBases)
    {
        if (!(entity.level() instanceof ServerLevel level)) return;
        Vec3 columnDir = getHorizontalColumnDir();
        Vec3 base = entity.position();
        columnBases.add(base);
        breakBlocksInColumn(level, base, columnDir);
    }

    private Vec3 getHorizontalColumnDir()
    {
        Vec3 dir = this.flightDir;
        Vec3 cross = new Vec3(-dir.z, 0.0D, dir.x);
        double len = cross.length();
        if (len < 1.0E-8D) return new Vec3(1.0D, 0.0D, 0.0D);
        return cross.scale(1.0D / len);
    }

    /**
     * 破方块（档 B）：整条柱的包围盒<b>只遍历一次</b>，逐方块判定「到柱轴线段的最短距离 ≤ 柱半径」，
     * 取代原先「每个采样点各遍历一遍 {@code (2r+1)³ = 125} 个立方体」（9 采样点 ≈ 1125 次迭代）。
     * <p>
     * ⚠️ 会改变破坏形状：原先覆盖是「9 个离散球的并集」，现在是「轴上连续扫过的胶囊」。两端点
     * 完全一致，只在柱<i>腰部</i>有约 0.086 格外扩 ⇒ 侧面可能多破坏极少数方块（已知并接受）。
     */
    private void breakBlocksInColumn(Level level, Vec3 base, Vec3 columnDir)
    {
        double rSqr = ShrineKaiConfig.COLUMN_RADIUS * ShrineKaiConfig.COLUMN_RADIUS;

        // 横斩柱轴线段：[base - dir*H/2, base + dir*H/2]（采样点 i∈[-H/2, H/2]）
        double half = ShrineKaiConfig.COLUMN_HEIGHT / 2.0D;
        Vec3 a = base.add(columnDir.scale(-half));
        Vec3 b = base.add(columnDir.scale(half));
        Vec3 ab = b.subtract(a);
        double abLenSqr = Math.max(ab.lengthSqr(), 1.0E-8D);

        AABB box = new AABB(a, b).inflate(ShrineKaiConfig.COLUMN_RADIUS);
        int minX = Mth.floor(box.minX), minY = Mth.floor(box.minY), minZ = Mth.floor(box.minZ);
        int maxX = Mth.floor(box.maxX), maxY = Mth.floor(box.maxY), maxZ = Mth.floor(box.maxZ);

        BlockPos.MutableBlockPos pos = this.scratchPos;
        for (int x = minX; x <= maxX; x++)
        {
            for (int y = minY; y <= maxY; y++)
            {
                for (int z = minZ; z <= maxZ; z++)
                {
                    pos.set(x, y, z);
                    if (level.isOutsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos)) continue;
                    if (!cubeIntersectsCapsule(x, y, z, a, ab, abLenSqr, rSqr)) continue;
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir()) continue;
                    if (state.getDestroySpeed(level, pos) < 0.0F) continue;
                    level.destroyBlock(pos, false);
                }
            }
        }
    }

    /**
     * 方块是否与「轴 a → a+ab、半径平方 rSqr」的胶囊相交（方块中心在轴上的投影点作球心的球-方块判定）。
     */
    private static boolean cubeIntersectsCapsule(int bx, int by, int bz,
                                                 Vec3 a, Vec3 ab, double abLenSqr, double rSqr)
    {
        double cx = bx + 0.5D;
        double cy = by + 0.5D;
        double cz = bz + 0.5D;
        double t = ((cx - a.x) * ab.x + (cy - a.y) * ab.y + (cz - a.z) * ab.z) / abLenSqr;
        t = Mth.clamp(t, 0.0D, 1.0D);
        double px = a.x + ab.x * t;
        double py = a.y + ab.y * t;
        double pz = a.z + ab.z * t;
        double qx = Mth.clamp(px, bx, bx + 1.0D);
        double qy = Mth.clamp(py, by, by + 1.0D);
        double qz = Mth.clamp(pz, bz, bz + 1.0D);
        double dx = px - qx;
        double dy = py - qy;
        double dz = pz - qz;
        return dx * dx + dy * dy + dz * dz <= rSqr;
    }

    /**
     * 本 tick 的伤害判定 + 结算（与 {@code ShrineKaiArrowBehavior} 同一手法）。
     * <p>
     * 改造前：每格 × 每采样点各查一次实体、各 deal 一次 ⇒ 一 tick 约
     * {@code 10 格 × 9 采样点 = 90} 次实体查询 + 90 次 {@code deal}。
     * 改造后：<b>整 tick 一次查询</b>（{@link #sweepBox}）+ <b>同一目标本 tick 只 deal 一次</b>。
     * <p>
     * ⚠️ 判定逻辑与采样点集合一字未改（同柱方向、同 {@code -half..half}、同
     * {@code aabbIntersectsSphere}）⇒ 命中范围不变；唯一差异是同 tick 重复命中由
     * 「deal 多次」变「deal 一次」，而重复的本来就被原版 {@code invulnerableTime} 吞 ⇒ 结算等价。
     */
    private void resolveAndSettleHits(SpecialEffectEntity entity, List<Vec3> columnBases)
    {
        if (columnBases.isEmpty() || !(entity.level() instanceof ServerLevel level)) return;

        Vec3 columnDir = getHorizontalColumnDir();
        double rSqr = ShrineKaiConfig.COLUMN_RADIUS * ShrineKaiConfig.COLUMN_RADIUS;
        LivingEntity owner = resolveOwner(entity, this.ownerUuid);

        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, sweepBox(columnBases, columnDir));
        if (candidates.isEmpty()) return;

        Set<LivingEntity> hit = new HashSet<>();
        int half = ShrineKaiConfig.COLUMN_HEIGHT / 2;
        for (Vec3 base : columnBases)
        {
            for (int i = -half; i <= half; i++)
            {
                Vec3 sample = base.add(columnDir.scale(i));
                for (LivingEntity target : candidates)
                {
                    if (hit.contains(target) || !target.isAlive()) continue;
                    if (owner != null && target.getUUID().equals(owner.getUUID())) continue;
                    if (!GeometryUtils.aabbIntersectsSphere(sample.x, sample.y, sample.z, rSqr, target.getBoundingBox())) continue;
                    hit.add(target);
                }
            }
        }
        if (hit.isEmpty()) return;

        float damage = ShrineSkillGrowth.kaiFlyobjColumnDamage(owner);
        for (LivingEntity target : hit)
        {
            if (!target.isAlive()) continue;
            if (TechniqueDamageSources.deal(target, owner, damage, ShrineTechniqueSkillSet.SKILL_KAI_FLYOBJ))
            {
                ShrineFinishCutMarks.tryMarkFromKaiHit(owner, target);
                level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                        ModSounds.SHRINE_SLASH_KAI_2_HIT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        }
    }

    /** 覆盖「整 tick 路径 × 柱截面」的查询盒（横斩：沿水平柱方向 ±半高，再留柱半径余量）。 */
    private AABB sweepBox(List<Vec3> columnBases, Vec3 columnDir)
    {
        AABB box = new AABB(columnBases.get(0), columnBases.get(0));
        for (int i = 1; i < columnBases.size(); i++)
        {
            box = box.minmax(new AABB(columnBases.get(i), columnBases.get(i)));
        }
        double half = ShrineKaiConfig.COLUMN_HEIGHT / 2.0D;
        return box.expandTowards(columnDir.scale(half)).expandTowards(columnDir.scale(-half))
                .inflate(ShrineKaiConfig.COLUMN_RADIUS);
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
