package com.darkness.jujutsukaisenzone.entity.behavior;

import com.darkness.jujutsukaisenzone.entity.SpecialEffectEntity;
import com.darkness.jujutsukaisenzone.entity.data.ShrineKaiConfig;
import com.darkness.jujutsukaisenzone.entity.util.AreaShape;
import com.darkness.jujutsukaisenzone.entity.util.BlockErase;
import com.darkness.jujutsukaisenzone.entity.util.GeometryUtils;
import com.darkness.jujutsukaisenzone.entity.util.SpecialEffectFlight;
import com.darkness.jujutsukaisenzone.photon.OwnerOnlyShrineSlashFx;
import com.darkness.jujutsukaisenzone.registry.ModEntities;
import com.darkness.jujutsukaisenzone.registry.ModSounds;
import com.darkness.jujutsukaisenzone.technique.TechniqueDamageSources;
import com.darkness.jujutsukaisenzone.technique.scaling.ShrineSkillGrowth;
import com.darkness.jujutsukaisenzone.technique.skillset.ShrineTechniqueSkillSet;
import com.darkness.jujutsukaisenzone.technique.se.ShrineFinishCutMarks;
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
    /** 匀速直线飞行状态（方向 / 已飞距离 / 射程）；速度每 tick 从配置重取。 */
    private SpecialEffectFlight.LinearMotion motion =
            new SpecialEffectFlight.LinearMotion(new Vec3(0.0D, 0.0D, 1.0D), 0.0D, 1.0D);

    @Override
    public String id() { return ID; }

    public void configure(@Nullable LivingEntity owner, double maxRange, Vec3 flightDir, boolean horizontal, boolean flyobjMode)
    {
        this.ownerUuid = owner == null ? null : owner.getUUID();
        this.flyobjMode = flyobjMode;
        this.slashType = horizontal ? 2 : 1;
        this.motion = new SpecialEffectFlight.LinearMotion(flightDir, ShrineKaiConfig.ARROW_SPEED, maxRange);
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
            // 逐格匀速直线推进（每步 ≤ 1 格、射程封顶）与每步后的旋转、柱清场都交给
            // SpecialEffectFlight；速度每 tick 从配置重取，改 TOML 立即生效。
            this.motion.setSpeedPerTick(ShrineKaiConfig.ARROW_SPEED);
            SpecialEffectFlight.Result result = SpecialEffectFlight.tickLinear(entity, this.motion, (e, from, to) ->
            {
                if (!this.flyobjMode)
                {
                    applyRotation(e);
                }
                tickColumn(e, columnBases);
                return false;
            });

            if (result == SpecialEffectFlight.Result.ABORT_CHUNK_UNLOADED)
            {
                entity.discard();
                return;
            }
            if (result == SpecialEffectFlight.Result.ARRIVED)
            {
                arrive(entity);
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
            ShrineKaiArrivalBehavior arrivalBehavior = new ShrineKaiArrivalBehavior();
            arrivalBehavior.configure(owner, this.slashType);
            fx.attach(arrivalBehavior);
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
        Vec3 dir = this.motion.direction();
        Vec3 cross = new Vec3(-dir.z, 0.0D, dir.x);
        double len = cross.length();
        if (len < 1.0E-8D) return new Vec3(1.0D, 0.0D, 0.0D);
        return cross.scale(1.0D / len);
    }

    /**
     * 破方块：整条柱的包围盒只遍历一次，逐方块判定「到柱轴线段的最短距离 ≤ 柱半径」。
     * <p>
     * 几何委托 {@link AreaShape#capsule}（球沿轴连续扫过的胶囊语义），遍历委托 {@link BlockErase}。
     * 判据与旧实现一致——这是本项目唯一会改变破坏形状的改动：两端点完全一致，
     * 只在柱腰部有约 {@code r - sqrt(r² - 0.25) ≈ 0.086} 格的外扩，侧面可能多破坏极少数方块（已知并接受）。
     */
    private void breakBlocksInColumn(Level level, Vec3 base, Vec3 columnDir)
    {
        // 柱轴线段：竖斩 [base, base + up*(H-1)]；横斩 [base - dir*H/2, base + dir*H/2]
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

        AreaShape shape = AreaShape.capsule(a, b, ShrineKaiConfig.COLUMN_RADIUS);
        BlockErase.forEach(level, shape, (lv, pos) ->
        {
            if (lv.isOutsideBuildHeight(pos) || !lv.getWorldBorder().isWithinBounds(pos)) return false;
            BlockState state = lv.getBlockState(pos);
            if (state.isAir()) return false;
            if (state.getDestroySpeed(lv, pos) < 0.0F) return false;
            lv.destroyBlock(pos, false);
            return false;
        });
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
     * <p>
     * <b>暴击放行（走 {@code dealCrit}）</b>：本方法虽每 tick 都会对柱内目标调一次 {@code deal}，
     * 但原版无敌帧（{@code invulnerableTime > 10}）在 {@code hurt} 里、{@code actuallyHurt} 之前
     * 就把重复命中吞掉了，而 {@code LivingHurtEvent}（暴击乘区所在）只在 {@code actuallyHurt}
     * 内派发 ⇒ 同一目标真实结算被压到最快每 10 tick 一次，掷骰频率与单发招式同一量级。
     * 去重集合 {@code hit} 保证同一 tick 内一个目标最多 {@code deal} 一次（见上）。
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
            if (TechniqueDamageSources.dealCrit(target, owner, damage, ShrineTechniqueSkillSet.SKILL_KAI))
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
        Vec3 dir = this.motion.direction();
        double h = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
        float yaw = (float) (Mth.atan2(dir.x, dir.z) * (180.0D / Math.PI));
        float pitch = (float) (-Mth.atan2(dir.y, h) * (180.0D / Math.PI));
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
        this.motion.writeToNbt(tag);
        if (this.ownerUuid != null) tag.putUUID("Owner", this.ownerUuid);
    }

    @Override
    public void readFromNbt(CompoundTag tag)
    {
        this.flyobjMode = tag.getBoolean("FlyobjMode");
        this.photonMounted = tag.getBoolean("PhotonMounted");
        this.slashType = tag.contains("SlashType") ? tag.getInt("SlashType") : 1;
        this.motion = SpecialEffectFlight.LinearMotion.readFromNbt(tag);
        this.ownerUuid = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
    }
}
