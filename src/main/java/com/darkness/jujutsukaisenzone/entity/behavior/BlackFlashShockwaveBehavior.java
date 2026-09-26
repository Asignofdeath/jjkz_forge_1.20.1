package com.darkness.jujutsukaisenzone.entity.behavior;

import com.darkness.jujutsukaisenzone.entity.SpecialEffectEntity;
import com.darkness.jujutsukaisenzone.entity.data.BlackFlashConfig;
import com.darkness.jujutsukaisenzone.technique.TechniqueDamageSources;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 黑闪冲击波延迟器：到期时对周围方块破坏 + 生物伤害。
 */
public class BlackFlashShockwaveBehavior extends EffectBehavior
{
    public static final String ID = "black_flash_shockwave";

    @Nullable
    private UUID ownerUuid;
    private int remainingLifeTicks = -1;

    @Override
    public String id() { return ID; }

    public void configure(@Nullable LivingEntity owner)
    {
        this.ownerUuid = owner == null ? null : owner.getUUID();
        this.remainingLifeTicks = BlackFlashConfig.SHOCKWAVE_DELAY_TICKS;
    }

    @Override
    public void tick(SpecialEffectEntity entity)
    {
        this.remainingLifeTicks--;
        if (this.remainingLifeTicks > 0) return;

        if (!(entity.level() instanceof ServerLevel level))
        {
            entity.discard();
            return;
        }

        int cx = Mth.floor(entity.getX());
        int cy = Mth.floor(entity.getY());
        int cz = Mth.floor(entity.getZ());
        int radius = BlackFlashConfig.SHOCKWAVE_RADIUS;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int dx = -radius; dx <= radius; dx++)
        {
            for (int dy = -radius; dy <= radius; dy++)
            {
                for (int dz = -radius; dz <= radius; dz++)
                {
                    if (dx * dx + dy * dy + dz * dz > radius * radius) continue;
                    pos.set(cx + dx, cy + dy, cz + dz);
                    if (level.isOutsideBuildHeight(pos)) continue;
                    var state = level.getBlockState(pos);
                    if (state.isAir() || state.getDestroySpeed(level, pos) < 0.0F) continue;
                    if (entity.level().random.nextDouble() < BlackFlashConfig.SHOCKWAVE_DESTROY_CHANCE)
                    {
                        level.destroyBlock(pos, true);
                    }
                }
            }
        }

        AABB area = entity.getBoundingBox().inflate(radius);
        LivingEntity owner = resolveOwner(entity, this.ownerUuid);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area))
        {
            if (target == owner || !target.isAlive()) continue;
            if (target.distanceToSqr(entity) > radius * radius) continue;
            // 冲击波只结算一次（本方法末尾 discard）⇒ 放行暴击
            TechniqueDamageSources.dealCrit(target, owner, BlackFlashConfig.SHOCKWAVE_DAMAGE);
        }

        entity.discard();
    }

    @Override
    public void writeToNbt(CompoundTag tag)
    {
        tag.putInt("RemainingLife", this.remainingLifeTicks);
        if (this.ownerUuid != null) tag.putUUID("Owner", this.ownerUuid);
    }

    @Override
    public void readFromNbt(CompoundTag tag)
    {
        this.remainingLifeTicks = tag.contains("RemainingLife") ? tag.getInt("RemainingLife") : -1;
        this.ownerUuid = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
    }
}
