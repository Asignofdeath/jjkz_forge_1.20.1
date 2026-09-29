package com.darkness.jujutsukaisenzone.entity.behavior;

import com.darkness.jujutsukaisenzone.entity.SpecialEffectEntity;
import com.darkness.jujutsukaisenzone.entity.data.LimitlessBlueConfig;
import com.darkness.jujutsukaisenzone.entity.util.LimitlessBlueBlockPull;
import com.darkness.jujutsukaisenzone.technique.TechniqueDamageSources;
import com.darkness.jujutsukaisenzone.technique.scaling.LimitlessBlueSkillGrowth;
import com.darkness.jujutsukaisenzone.technique.skillset.LimitlessTechniqueSkillSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 无下限术式「苍」：吸附范围内生物与方块，方块在中心堆成球体。
 */
public class LimitlessBlueBehavior extends EffectBehavior
{
    public static final String ID = "limitless_blue";

    @Nullable
    private UUID ownerUuid;
    private int remainingLifeTicks = -1;
    private boolean allowOwnerPull;
    private boolean ownerPullCompleted;
    @Nullable
    private LimitlessBlueBlockPull.BallStack ballStack;
    private final Set<Long> convertedBlockPositions = new HashSet<>();

    @Override
    public String id() { return ID; }

    public void configure(@Nullable LivingEntity owner, boolean allowOwnerPull)
    {
        this.ownerUuid = owner == null ? null : owner.getUUID();
        this.remainingLifeTicks = LimitlessBlueConfig.LIFETIME_TICKS;
        this.allowOwnerPull = allowOwnerPull;
        this.ownerPullCompleted = false;
        this.ballStack = null;
        this.convertedBlockPositions.clear();
    }

    @Override
    public void tick(SpecialEffectEntity entity)
    {
        if (this.remainingLifeTicks-- <= 0)
        {
            entity.discard();
            return;
        }

        LivingEntity owner = resolveOwner(entity, this.ownerUuid);
        float damage = LimitlessBlueSkillGrowth.damage(owner);
        double pullRadius = LimitlessBlueSkillGrowth.pullRadius(owner);

        if (entity.level() instanceof ServerLevel level)
        {
            tickBlockPull(level, entity, pullRadius);
        }

        AABB area = entity.getBoundingBox().inflate(pullRadius);
        for (LivingEntity target : entity.level().getEntitiesOfClass(LivingEntity.class, area, t -> canPullByBlue(entity, t)))
        {
            pullTarget(entity, target, pullRadius);
            if (entity.tickCount % LimitlessBlueConfig.DAMAGE_INTERVAL == 0
                    && entity.distanceToSqr(target) <= LimitlessBlueConfig.DAMAGE_RADIUS * LimitlessBlueConfig.DAMAGE_RADIUS
                    && canDamageByBlue(target))
            {
                TechniqueDamageSources.deal(target, owner, damage, LimitlessTechniqueSkillSet.SKILL_BLUE);
            }
        }
    }

    private void tickBlockPull(ServerLevel level, SpecialEffectEntity entity, double pullRadius)
    {
        Vec3 center = pullCenter(entity);
        if (this.ballStack == null)
        {
            this.ballStack = new LimitlessBlueBlockPull.BallStack(center);
        }

        if (entity.tickCount % LimitlessBlueConfig.BLOCK_PULL_CONVERT_INTERVAL == 0)
        {
            LimitlessBlueBlockPull.tryConvertWave(
                    level,
                    center,
                    this.convertedBlockPositions,
                    LimitlessBlueConfig.BLOCK_PULL_MAX_PER_WAVE,
                    pullRadius);
        }

        LimitlessBlueBlockPull.pullFlyingBlocks(level, center, pullRadius);

        AABB absorbArea = new AABB(center, center).inflate(LimitlessBlueConfig.BLOCK_BALL_ABSORB_DISTANCE + 1.0D);
        for (FallingBlockEntity block : level.getEntitiesOfClass(FallingBlockEntity.class, absorbArea))
        {
            LimitlessBlueBlockPull.tryAbsorbIntoBall(level, block, this.ballStack);
        }
    }

    private static Vec3 pullCenter(SpecialEffectEntity entity)
    {
        return entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
    }

    private boolean canPullByBlue(SpecialEffectEntity entity, LivingEntity target)
    {
        if (target == entity || !target.isAlive()) return false;
        if (this.ownerUuid == null) return true;
        boolean isOwner = this.ownerUuid.equals(target.getUUID());
        if (!isOwner) return true;
        if (!this.allowOwnerPull) return false;
        if (this.ownerPullCompleted) return false;

        double stopRadiusSqr = LimitlessBlueConfig.OWNER_PULL_STOP_RADIUS * LimitlessBlueConfig.OWNER_PULL_STOP_RADIUS;
        if (target.distanceToSqr(entity) <= stopRadiusSqr)
        {
            this.ownerPullCompleted = true;
            return false;
        }
        return true;
    }

    private boolean canDamageByBlue(LivingEntity target)
    {
        if (target == null || !target.isAlive()) return false;
        return this.ownerUuid == null || !this.ownerUuid.equals(target.getUUID());
    }

    private void pullTarget(SpecialEffectEntity entity, LivingEntity target, double pullRadius)
    {
        Vec3 center = pullCenter(entity);
        Vec3 targetCenter = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        Vec3 offset = center.subtract(targetCenter);
        double distance = offset.length();
        if (distance < 1.0E-4D) return;

        Vec3 pull = offset.normalize().scale(LimitlessBlueConfig.PULL_STRENGTH + Math.max(0.0D, pullRadius - distance) * 0.02D);
        target.setDeltaMovement(target.getDeltaMovement().scale(0.45D).add(pull.x, pull.y + 0.08D, pull.z));
        target.hurtMarked = true;
    }

    @Override
    public void writeToNbt(CompoundTag tag)
    {
        tag.putInt("RemainingLife", this.remainingLifeTicks);
        tag.putBoolean("AllowOwnerPull", this.allowOwnerPull);
        tag.putBoolean("OwnerPullCompleted", this.ownerPullCompleted);
        tag.putLongArray("ConvertedBlocks", this.convertedBlockPositions.stream().mapToLong(Long::longValue).toArray());
        if (this.ownerUuid != null) tag.putUUID("Owner", this.ownerUuid);
    }

    @Override
    public void readFromNbt(CompoundTag tag)
    {
        this.remainingLifeTicks = tag.contains("RemainingLife") ? tag.getInt("RemainingLife") : -1;
        this.allowOwnerPull = tag.getBoolean("AllowOwnerPull");
        this.ownerPullCompleted = tag.getBoolean("OwnerPullCompleted");
        this.ownerUuid = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        this.ballStack = null;
        this.convertedBlockPositions.clear();
        if (tag.contains("ConvertedBlocks"))
        {
            for (long value : tag.getLongArray("ConvertedBlocks"))
            {
                this.convertedBlockPositions.add(value);
            }
        }
    }
}
