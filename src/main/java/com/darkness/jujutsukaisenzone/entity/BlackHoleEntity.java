package com.darkness.jujutsukaisenzone.entity;

import com.darkness.jujutsukaisenzone.entity.data.BlackHoleSkillConfig;
import com.darkness.jujutsukaisenzone.technique.TechniqueDamageSources;
import com.darkness.jujutsukaisenzone.technique.scaling.LimitlessDarkStarSkillGrowth;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 黑洞_A：史瓦西黑洞本体。
 * <p>
 * 两种运行模式：
 * <ul>
 *   <li><b>静态演示</b>（{@code /jjkz summonBlackholeA}）：纯视觉，不做任何物理 / 判定，永不消散。</li>
 *   <li><b>技能「极之番——暗星」</b>：飞行（路径擦伤）→ 驻留坍缩（持续牵引伤害）→ 湮灭。
 *       飞行与伤害全部服务端驱动；渲染（事件视界 / 吸积盘 / 光子环 / 相对论喷流 /
 *       全屏引力透镜）仍在客户端侧完成。</li>
 * </ul>
 */
public class BlackHoleEntity extends Entity
{
    private static final EntityDataAccessor<Float> DATA_SCHWARZSCHILD_RADIUS =
            SynchedEntityData.defineId(BlackHoleEntity.class, EntityDataSerializers.FLOAT);

    /** 静态演示：不移动、不伤害、不消失 */
    public static final int PHASE_PASSIVE = 0;
    /** 技能飞行段：沿视线直线推进，路径擦伤 */
    public static final int PHASE_FLYING = 1;
    /** 技能驻留段：原地坍缩，持续牵引伤害，到期湮灭 */
    public static final int PHASE_SETTLED = 2;

    @Nullable
    private UUID skillOwner;
    private int skillPhase = PHASE_PASSIVE;
    private final Set<UUID> scratched = new HashSet<>();
    private double flightDirX;
    private double flightDirY;
    private double flightDirZ;
    private int flightTicks;
    private int settleTicks;
    private int damageCooldown;

    public BlackHoleEntity(EntityType<? extends BlackHoleEntity> type, Level level)
    {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setInvulnerable(true);
    }

    public BlackHoleEntity(Level level, double x, double y, double z, float schwarzschildRadius)
    {
        this(com.darkness.jujutsukaisenzone.registry.ModEntities.BLACK_HOLE_A.get(), level);
        this.moveTo(x, y, z, 0.0F, 0.0F);
        this.setSchwarzschildRadius(schwarzschildRadius);
    }

    @Override
    protected void defineSynchedData()
    {
        this.getEntityData().define(DATA_SCHWARZSCHILD_RADIUS, 3.0F);
    }

    public float getSchwarzschildRadius()
    {
        return this.getEntityData().get(DATA_SCHWARZSCHILD_RADIUS);
    }

    public void setSchwarzschildRadius(float radius)
    {
        this.getEntityData().set(DATA_SCHWARZSCHILD_RADIUS, Math.max(0.1F, radius));
    }

    /** 阴影（表观事件视界）半径 = b_crit * Rs */
    public double getShadowRadius()
    {
        return com.darkness.jujutsukaisenzone.blackhole.BlackHolePhysics.shadowRadius(this.getSchwarzschildRadius());
    }

    /** 吸积盘内缘 = ISCO = 3 Rs */
    public double getIscoRadius()
    {
        return com.darkness.jujutsukaisenzone.blackhole.BlackHolePhysics.iscoRadius(this.getSchwarzschildRadius());
    }

    /** 技能模式入口：从施法者视角沿视线方向发射（服务端调用）。 */
    public void launchAsSkill(LivingEntity owner)
    {
        Vec3 look = owner.getLookAngle().normalize();
        this.flightDirX = look.x;
        this.flightDirY = look.y;
        this.flightDirZ = look.z;
        this.skillOwner = owner.getUUID();
        this.skillPhase = PHASE_FLYING;
        this.flightTicks = 0;
        this.settleTicks = BlackHoleSkillConfig.DURATION_TICKS;
        this.damageCooldown = 0;
    }

    public int getSkillPhase()
    {
        return this.skillPhase;
    }

    @Nullable
    private LivingEntity resolveOwner()
    {
        if (this.skillOwner == null || !(this.level() instanceof ServerLevel serverLevel))
        {
            return null;
        }
        Entity entity = serverLevel.getEntity(this.skillOwner);
        return entity instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    @Override
    public void tick()
    {
        super.tick();
        if (this.level().isClientSide || this.skillPhase == PHASE_PASSIVE)
        {
            return;
        }
        if (!(this.level() instanceof ServerLevel serverLevel))
        {
            return;
        }
        if (this.skillPhase == PHASE_FLYING)
        {
            tickFlight(serverLevel);
        }
        else if (this.skillPhase == PHASE_SETTLED)
        {
            tickSettled(serverLevel);
        }
    }

    private void tickFlight(ServerLevel level)
    {
        this.flightTicks++;
        Vec3 step = new Vec3(this.flightDirX, this.flightDirY, this.flightDirZ)
                .scale(BlackHoleSkillConfig.FLIGHT_SPEED);

        // 撞到实体方块就落定
        if (!level.noCollision(this, this.getBoundingBox().move(step)))
        {
            settle();
            return;
        }
        this.setPos(this.getX() + step.x, this.getY() + step.y, this.getZ() + step.z);

        // 路径擦伤：每个目标整个飞行段只结算一次；不放行暴击（术式保守默认）
        LivingEntity owner = resolveOwner();
        float scratch = LimitlessDarkStarSkillGrowth.scratchDamage(owner);
        if (scratch > 0.0F)
        {
            double scratchRadius = this.getShadowRadius() * BlackHoleSkillConfig.SCRATCH_RADIUS_FACTOR;
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                    this.getBoundingBox().inflate(scratchRadius),
                    e -> e.isAlive() && e != owner && e.canBeSeenByAnyone()))
            {
                if (this.scratched.add(target.getUUID()))
                {
                    TechniqueDamageSources.deal(target, owner, scratch);
                }
            }
        }

        if (this.flightTicks >= BlackHoleSkillConfig.MAX_FLIGHT_TICKS)
        {
            settle();
        }
    }

    private void settle()
    {
        this.skillPhase = PHASE_SETTLED;
        this.settleTicks = BlackHoleSkillConfig.DURATION_TICKS;
        this.damageCooldown = 0;
        this.scratched.clear();
    }

    private void tickSettled(ServerLevel level)
    {
        this.settleTicks--;
        if (this.settleTicks <= 0)
        {
            extinguish(level);
            return;
        }

        this.damageCooldown--;
        if (this.damageCooldown > 0)
        {
            return;
        }
        this.damageCooldown = BlackHoleSkillConfig.DAMAGE_INTERVAL;

        LivingEntity owner = resolveOwner();
        float damage = LimitlessDarkStarSkillGrowth.zoneDamage(owner);
        double zoneRadius = this.getShadowRadius() * BlackHoleSkillConfig.ZONE_RADIUS_FACTOR;
        Vec3 center = this.position();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(center.x - zoneRadius, center.y - zoneRadius, center.z - zoneRadius,
                        center.x + zoneRadius, center.y + zoneRadius, center.z + zoneRadius),
                e -> e.isAlive() && e != owner && e.canBeSeenByAnyone()))
        {
            if (damage > 0.0F)
            {
                // 不放行暴击（术式保守默认）
                TechniqueDamageSources.deal(target, owner, damage);
            }
            // 牵引：非玩家实体加向心速度（服务端不能用 deltaMovement 移动玩家）
            if (!(target instanceof Player))
            {
                Vec3 pull = center.subtract(target.position()).normalize()
                        .scale(BlackHoleSkillConfig.PULL_ACCELERATION);
                target.setDeltaMovement(target.getDeltaMovement().add(pull));
                target.hurtMarked = true;
            }
        }
    }

    private void extinguish(ServerLevel level)
    {
        Vec3 center = this.position();
        level.sendParticles(ParticleTypes.POOF, center.x, center.y, center.z, 20,
                this.getShadowRadius() * 0.3D, this.getShadowRadius() * 0.3D, this.getShadowRadius() * 0.3D, 0.02D);
        this.discard();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag)
    {
        this.setSchwarzschildRadius(tag.getFloat("SchwarzschildRadius"));
        this.skillPhase = tag.getInt("SkillPhase");
        this.flightTicks = tag.getInt("FlightTicks");
        this.settleTicks = tag.getInt("SettleTicks");
        this.damageCooldown = tag.getInt("DamageCooldown");
        this.flightDirX = tag.getDouble("FlightDirX");
        this.flightDirY = tag.getDouble("FlightDirY");
        this.flightDirZ = tag.getDouble("FlightDirZ");
        if (tag.hasUUID("SkillOwner"))
        {
            this.skillOwner = tag.getUUID("SkillOwner");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag)
    {
        tag.putFloat("SchwarzschildRadius", this.getSchwarzschildRadius());
        tag.putInt("SkillPhase", this.skillPhase);
        tag.putInt("FlightTicks", this.flightTicks);
        tag.putInt("SettleTicks", this.settleTicks);
        tag.putInt("DamageCooldown", this.damageCooldown);
        tag.putDouble("FlightDirX", this.flightDirX);
        tag.putDouble("FlightDirY", this.flightDirY);
        tag.putDouble("FlightDirZ", this.flightDirZ);
        if (this.skillOwner != null)
        {
            tag.putUUID("SkillOwner", this.skillOwner);
        }
    }

    @Override
    public boolean isPickable()
    {
        return false;
    }

    @Override
    public boolean isPushable()
    {
        return false;
    }
}
