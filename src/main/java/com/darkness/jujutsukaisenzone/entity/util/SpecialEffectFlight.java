package com.darkness.jujutsukaisenzone.entity.util;

import com.darkness.jujutsukaisenzone.entity.SpecialEffectEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * {@link SpecialEffectEntity} 通用飞行推进工具。
 * <p>
 * 特效实体 {@code noPhysics=true}，不会靠原版物理自动位移；飞行需在 Behavior 每 tick 主动调用本类。
 * 现有术式 Behavior 保持各自实现不变，新技能优先使用此处 API。
 */
public final class SpecialEffectFlight
{
    public enum Result
    {
        /** 本 tick 已推进，下 tick 继续。 */
        CONTINUING,
        /** 已达 {@link LinearMotion#maxRange}。 */
        ARRIVED,
        /** 区块未加载，调用方通常应 {@link SpecialEffectEntity#discard()}。 */
        ABORT_CHUNK_UNLOADED,
        /** {@link StepCallback} 请求提前终止（如碰撞）。 */
        STOPPED
    }

    /**
     * 分步直线飞行时每步后的可选回调。
     * 返回 {@code true} 表示立即停止本 tick 剩余步进，并返回 {@link Result#STOPPED}。
     */
    @FunctionalInterface
    public interface StepCallback
    {
        boolean onStep(SpecialEffectEntity entity, Vec3 from, Vec3 to);
    }

    /** 分步匀速直线飞行状态（由 Behavior 持有，可自行写入 NBT）。 */
    public static final class LinearMotion
    {
        private Vec3 direction;
        private double speedPerTick;
        private double traveled;
        private double maxRange;

        public LinearMotion(Vec3 direction, double speedPerTick, double maxRange)
        {
            this.direction = normalizeDirection(direction);
            this.speedPerTick = Math.max(0.0D, speedPerTick);
            this.traveled = 0.0D;
            this.maxRange = maxRange > 0.0D ? maxRange : Double.POSITIVE_INFINITY;
        }

        public static LinearMotion unlimited(Vec3 direction, double speedPerTick)
        {
            return new LinearMotion(direction, speedPerTick, Double.POSITIVE_INFINITY);
        }

        public Vec3 direction()
        {
            return this.direction;
        }

        public void setDirection(Vec3 direction)
        {
            this.direction = normalizeDirection(direction);
        }

        public double speedPerTick()
        {
            return this.speedPerTick;
        }

        public void setSpeedPerTick(double speedPerTick)
        {
            this.speedPerTick = Math.max(0.0D, speedPerTick);
        }

        public double traveled()
        {
            return this.traveled;
        }

        public void setTraveled(double traveled)
        {
            this.traveled = Math.max(0.0D, traveled);
        }

        public double maxRange()
        {
            return this.maxRange;
        }

        public void setMaxRange(double maxRange)
        {
            this.maxRange = maxRange > 0.0D ? maxRange : Double.POSITIVE_INFINITY;
        }

        public boolean hasMaxRange()
        {
            return Double.isFinite(this.maxRange);
        }

        public void writeToNbt(CompoundTag tag)
        {
            tag.putDouble("DirX", this.direction.x);
            tag.putDouble("DirY", this.direction.y);
            tag.putDouble("DirZ", this.direction.z);
            tag.putDouble("Speed", this.speedPerTick);
            tag.putDouble("Traveled", this.traveled);
            tag.putDouble("MaxRange", this.maxRange);
        }

        public static LinearMotion readFromNbt(CompoundTag tag)
        {
            Vec3 dir = new Vec3(tag.getDouble("DirX"), tag.getDouble("DirY"), tag.getDouble("DirZ"));
            double speed = tag.getDouble("Speed");
            double maxRange = tag.contains("MaxRange") ? tag.getDouble("MaxRange") : Double.POSITIVE_INFINITY;
            LinearMotion motion = new LinearMotion(dir, speed, maxRange);
            motion.traveled = tag.getDouble("Traveled");
            return motion;
        }
    }

    /** 连续速度飞行状态（茈箭风格，由 Behavior 持有）。 */
    public static final class VelocityMotion
    {
        private Vec3 velocity;

        public VelocityMotion(Vec3 velocity)
        {
            this.velocity = velocity;
        }

        public Vec3 velocity()
        {
            return this.velocity;
        }

        public void setVelocity(Vec3 velocity)
        {
            this.velocity = velocity;
        }

        public void writeToNbt(CompoundTag tag)
        {
            tag.putDouble("VelX", this.velocity.x);
            tag.putDouble("VelY", this.velocity.y);
            tag.putDouble("VelZ", this.velocity.z);
        }

        public static VelocityMotion readFromNbt(CompoundTag tag)
        {
            return new VelocityMotion(new Vec3(tag.getDouble("VelX"), tag.getDouble("VelY"), tag.getDouble("VelZ")));
        }
    }

    private SpecialEffectFlight()
    {
    }

    /** 与现有飞行 Behavior 一致：关闭重力，不依赖原版下落。 */
    public static void applyFlightPose(SpecialEffectEntity entity)
    {
        entity.setNoGravity(true);
    }

    /**
     * 分步直线推进：每 tick 最多移动 {@link LinearMotion#speedPerTick()} 格，单步不超过 1 格。
     * <p>
     * 适用于「解」切割、「灶」开等需要逐格检测或每步清场的技能。
     */
    public static Result tickLinear(SpecialEffectEntity entity, LinearMotion motion, @Nullable StepCallback callback)
    {
        if (!entity.level().hasChunkAt(entity.blockPosition()))
        {
            return Result.ABORT_CHUNK_UNLOADED;
        }

        applyFlightPose(entity);
        entity.setDeltaMovement(Vec3.ZERO);

        double remainingDist = motion.speedPerTick();
        while (remainingDist > 0.0D)
        {
            double step = Math.min(remainingDist, 1.0D);
            double nextTraveled = motion.traveled() + step;

            if (motion.hasMaxRange() && nextTraveled >= motion.maxRange())
            {
                double finalDist = motion.maxRange() - motion.traveled();
                if (finalDist > 0.0D)
                {
                    Vec3 from = entity.position();
                    Vec3 to = from.add(motion.direction().scale(finalDist));
                    moveTo(entity, to);
                    motion.setTraveled(motion.maxRange());
                    invokeStepCallback(callback, entity, from, to);
                }
                return Result.ARRIVED;
            }

            Vec3 from = entity.position();
            Vec3 to = from.add(motion.direction().scale(step));
            moveTo(entity, to);
            motion.setTraveled(nextTraveled);
            remainingDist -= step;

            if (invokeStepCallback(callback, entity, from, to))
            {
                return Result.STOPPED;
            }
        }
        return Result.CONTINUING;
    }

    /**
     * 连续速度推进：每 tick 沿 {@link VelocityMotion#velocity()} 位移一次。
     * <p>
     * 适用于茈箭等无射程上限、球体持续清场的技能。
     */
    public static Result tickVelocity(SpecialEffectEntity entity, VelocityMotion motion)
    {
        if (!entity.level().hasChunkAt(entity.blockPosition()))
        {
            return Result.ABORT_CHUNK_UNLOADED;
        }

        tickVelocityUnbounded(entity, motion.velocity());
        return Result.CONTINUING;
    }

    /**
     * 连续速度推进，不因未加载区块中止。
     * <p>
     * 需配合 {@link SpecialEffectChunkForce} 在位移前临时加载区块，供超远程飞行技能使用。
     */
    public static void tickVelocityUnbounded(SpecialEffectEntity entity, Vec3 velocity)
    {
        applyFlightPose(entity);
        entity.setDeltaMovement(velocity);
        entity.setPos(entity.position().add(velocity));
    }

    private static void moveTo(SpecialEffectEntity entity, Vec3 pos)
    {
        entity.setPos(pos.x, pos.y, pos.z);
    }

    private static boolean invokeStepCallback(
            @Nullable StepCallback callback,
            SpecialEffectEntity entity,
            Vec3 from,
            Vec3 to)
    {
        return callback != null && callback.onStep(entity, from, to);
    }

    private static Vec3 normalizeDirection(Vec3 direction)
    {
        if (direction.lengthSqr() < 1.0E-8D)
        {
            return new Vec3(0.0D, 0.0D, 1.0D);
        }
        return direction.normalize();
    }
}
