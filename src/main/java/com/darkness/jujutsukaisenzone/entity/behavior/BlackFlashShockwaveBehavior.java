package com.darkness.jujutsukaisenzone.entity.behavior;

import com.darkness.jujutsukaisenzone.entity.SpecialEffectEntity;
import com.darkness.jujutsukaisenzone.entity.data.BlackFlashConfig;
import com.darkness.jujutsukaisenzone.entity.util.AreaShape;
import com.darkness.jujutsukaisenzone.entity.util.BlockErase;
import com.darkness.jujutsukaisenzone.technique.TechniqueDamageSources;
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

        double cx = Mth.floor(entity.getX());
        double cy = Mth.floor(entity.getY());
        double cz = Mth.floor(entity.getZ());
        int radius = BlackFlashConfig.SHOCKWAVE_RADIUS;
        AreaShape shape = AreaShape.sphere(cx, cy, cz, radius);
        BlockErase.forEach(level, shape, (lv, pos) ->
        {
            if (lv.isOutsideBuildHeight(pos)) return false;
            var state = lv.getBlockState(pos);
            if (state.isAir() || state.getDestroySpeed(lv, pos) < 0.0F) return false;
            if (lv.random.nextDouble() < BlackFlashConfig.SHOCKWAVE_DESTROY_CHANCE)
            {
                lv.destroyBlock(pos, true);
            }
            return false;
        });

        AABB area = entity.getBoundingBox().inflate(radius);
        LivingEntity owner = resolveOwner(entity, this.ownerUuid);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area))
        {
            if (target == owner || !target.isAlive()) continue;
            if (target.distanceToSqr(entity) > radius * radius) continue;
            // 冲击波只结算一次（本方法末尾 discard）⇒ 放行暴击
            TechniqueDamageSources.dealCrit(target, owner, BlackFlashConfig.SHOCKWAVE_DAMAGE, "black_flash");
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
