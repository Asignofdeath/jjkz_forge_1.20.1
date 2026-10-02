package com.darkness.jujutsukaisenzone.entity.behavior;

import com.darkness.jujutsukaisenzone.entity.SpecialEffectEntity;
import com.darkness.jujutsukaisenzone.entity.data.ShrineKaiConfig;
import com.darkness.jujutsukaisenzone.entity.util.GeometryUtils;
import com.darkness.jujutsukaisenzone.photon.OwnerOnlyShrineSlashFx;
import com.darkness.jujutsukaisenzone.registry.ModEntities;
import com.darkness.jujutsukaisenzone.registry.ModSounds;
import com.darkness.jujutsukaisenzone.technique.TechniqueDamageSources;
import com.darkness.jujutsukaisenzone.technique.scaling.ShrineSkillGrowth;
import com.darkness.jujutsukaisenzone.technique.skillset.ShrineTechniqueSkillSet;
import com.darkness.jujutsukaisenzone.technique.se.ShrineFinishCutMarks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
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
    /** 破方块遍历复用的坐标对象（避免每采样点 new 一个）。 */
    private final BlockPos.MutableBlockPos scratchPos = new BlockPos.MutableBlockPos();

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

        // 本 tick 的每格柱基点：先逐格推进（破方块 + 记基点），走完再统一做一次
        // 伤害判定与结算。见 resolveAndSettleHits 的注释（为什么能这么合并）。
        List<Vec3> columnBases = new ArrayList<>(16);
        try
        {
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
                        tickColumn(entity, columnBases);
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
                tickColumn(entity, columnBases);
                remainingDist -= step;
            }
        }
        finally
        {
            // finally：arrive() / discard() 早退也要把本 tick 的伤害结掉。
            resolveAndSettleHits(entity, columnBases);
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

    /**
     * 逐格推进：破方块 + 记下本格的柱基点。
     * <p>
     * 伤害判定<b>不在这里</b>做——采样点集合与原来完全一致（同样的柱方向、同样的
     * {@code from..to}），但判定延到本 tick 末由 {@link #resolveAndSettleHits} 统一执行，
     * 以便把「每个采样点查一次实体 + 每个采样点 deal 一次」压成「整 tick 查一次 + 每目标一次」。
     */
    private void tickColumn(SpecialEffectEntity entity, List<Vec3> columnBases)
    {
        if (!(entity.level() instanceof ServerLevel level)) return;
        Vec3 columnDir = this.slashType == 2 ? getHorizontalColumnDir() : entity.getUpVector(1.0F);
        Vec3 base = entity.position();
        columnBases.add(base);
        breakBlocksInColumn(level, base, columnDir);
    }

    private Vec3 getHorizontalColumnDir()
    {
        Vec3 cross = new Vec3(-this.flightDir.z, 0.0D, this.flightDir.x);
        double len = cross.length();
        if (len < 1.0E-8D) return new Vec3(1.0D, 0.0D, 0.0D);
        return cross.scale(1.0D / len);
    }

    /**
     * 破方块（档 B）：整条柱的包围盒<b>只遍历一次</b>，逐方块判定「到柱轴线段的最短距离 ≤ 柱半径」，
     * 取代原先「每个采样点各遍历一遍 {@code (2r+1)³ = 125} 个立方体」。
     * <p>
     * 迭代量：竖斩约 1000 → 约 160；横斩约 1125 → 约 180~360（取决于柱方向与坐标轴的对齐度）。
     * <p>
     * ⚠️ 这是本项目<b>唯一</b>会改变破坏形状的改动：原先的覆盖是「8~9 个离散球的并集」，
     * 现在是「轴上连续扫过的胶囊」。因球心间距 1 格小于直径 3 格、重叠很深，两端点完全一致，
     * 只在柱<i>腰部</i>有约 {@code r - sqrt(r² - 0.25) ≈ 0.086} 格的外扩
     * ⇒ 侧面可能多破坏极少数方块（已知并接受）。
     */
    private void breakBlocksInColumn(Level level, Vec3 base, Vec3 columnDir)
    {
        double rSqr = ShrineKaiConfig.COLUMN_RADIUS * ShrineKaiConfig.COLUMN_RADIUS;

        // 柱轴线段：竖斩 [base, base + up*(H-1)]（采样点 i∈[0, H-1]）；横斩 [base - dir*H/2, base + dir*H/2]
        Vec3 a;
        Vec3 b;
        if (this.slashType == 2)
        {
            double half = ShrineKaiConfig.COLUMN_HEIGHT / 2.0D;
            a = base.add(columnDir.scale(-half));
            b = base.add(columnDir.scale(half));
        }
        else
        {
            a = base;
            b = base.add(columnDir.scale(ShrineKaiConfig.COLUMN_HEIGHT - 1));
        }
        Vec3 ab = b.subtract(a);
        double abLenSqr = Math.max(ab.lengthSqr(), 1.0E-8D);

        AABB box = new AABB(a, b).inflate(ShrineKaiConfig.COLUMN_RADIUS);
        int minX = Mth.floor(box.minX), minY = Mth.floor(box.minY), minZ = Mth.floor(box.minZ);
        int maxX = Mth.floor(box.maxX), maxY = Mth.floor(box.maxY), maxZ = Mth.floor(box.maxZ);

        BlockPos.MutableBlockPos pos = this.scratchPos;
        for (int x = minX; x <= maxX; x++)
            for (int y = minY; y <= maxY; y++)
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

    /**
     * 方块是否与「轴 a → a+ab、半径平方 rSqr」的胶囊相交。
     * <p>
     * 取方块中心在轴上的投影点（夹到线段内）当球心，再做球-方块判定——即「球沿轴连续扫过」
     * 的胶囊语义，与 {@code GeometryUtils.blockCubeIntersectsSphere} 同一套距离判据。
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
     * 本 tick 的伤害判定 + 结算（性能改造的核心）。
     * <p>
     * 改造前：沿柱逐采样点各查一次实体、各 deal 一次 ⇒ 一支解箭一 tick
     * 约 {@code 10 格 × 8 采样点 = 80} 次实体查询 + 80 次 {@code deal}。
     * <p>
     * 改造后：
     * <ol>
     *   <li><b>实体查询次数 = 1/tick</b> —— 用一个覆盖「整 tick 路径 × 柱截面」的 AABB
     *       查一次候选集，再对候选做逐点球判定（纯算术）。</li>
     *   <li><b>同一目标本 tick 只 deal 一次</b> —— 用 {@code hit} 集合去重。</li>
     * </ol>
     * <p>
     * ⚠️ <b>判定逻辑与采样点集合一字未改</b>：仍是同样的柱方向、同样的 {@code from..to}、
     * 同样的 {@code GeometryUtils.aabbIntersectsSphere} 球-盒判定，所以命中范围不变。
     * 唯一的行为差异是「同一 tick 内同一目标的重复命中从『deal 多次』变成『deal 一次』」——
     * 而重复的那些本来就会被原版 {@code invulnerableTime} 吞掉（{@code LivingHurtEvent}
     * 只在 {@code actuallyHurt} 内派发），结算结果完全等价。
     * <p>
     * 顺带：{@code ShrineSkillGrowth.kaiColumnDamage} 与 {@code resolveOwner} 从「每采样点一次」
     * 降为「每 tick 一次」。
     */
    private void resolveAndSettleHits(SpecialEffectEntity entity, List<Vec3> columnBases)
    {
        if (columnBases.isEmpty() || !(entity.level() instanceof ServerLevel level)) return;

        Vec3 columnDir = this.slashType == 2 ? getHorizontalColumnDir() : entity.getUpVector(1.0F);
        double rSqr = ShrineKaiConfig.COLUMN_RADIUS * ShrineKaiConfig.COLUMN_RADIUS;
        LivingEntity owner = resolveOwner(entity, this.ownerUuid);

        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, sweepBox(columnBases, columnDir));
        if (candidates.isEmpty()) return;

        Set<LivingEntity> hit = new HashSet<>();
        int from = this.slashType == 2 ? -(ShrineKaiConfig.COLUMN_HEIGHT / 2) : 0;
        int to = this.slashType == 2 ? ShrineKaiConfig.COLUMN_HEIGHT / 2 : ShrineKaiConfig.COLUMN_HEIGHT - 1;
        for (Vec3 base : columnBases)
        {
            for (int i = from; i <= to; i++)
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

        float damage = ShrineSkillGrowth.kaiColumnDamage(owner);
        SoundEvent hitSound = this.slashType == 2
                ? ModSounds.SHRINE_SLASH_KAI_2_HIT.get()
                : ModSounds.SHRINE_SLASH_KAI_1_HIT.get();
        for (LivingEntity target : hit)
        {
            if (!target.isAlive()) continue;
            if (TechniqueDamageSources.deal(target, owner, damage, ShrineTechniqueSkillSet.SKILL_KAI))
            {
                ShrineFinishCutMarks.tryMarkFromKaiHit(owner, target);
                level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                        hitSound, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        }
    }

    /** 覆盖「整 tick 路径 × 柱截面」的查询盒（按柱方向展开，再留一个柱半径的余量）。 */
    private AABB sweepBox(List<Vec3> columnBases, Vec3 columnDir)
    {
        AABB box = new AABB(columnBases.get(0), columnBases.get(0));
        for (int i = 1; i < columnBases.size(); i++)
        {
            box = box.minmax(new AABB(columnBases.get(i), columnBases.get(i)));
        }
        if (this.slashType == 2)
        {
            double half = ShrineKaiConfig.COLUMN_HEIGHT / 2.0D;
            box = box.expandTowards(columnDir.scale(half)).expandTowards(columnDir.scale(-half));
        }
        else
        {
            box = box.expandTowards(columnDir.scale(ShrineKaiConfig.COLUMN_HEIGHT));
        }
        return box.inflate(ShrineKaiConfig.COLUMN_RADIUS);
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
