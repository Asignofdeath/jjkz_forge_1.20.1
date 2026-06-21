package com.darkness.jujutsukaisenzone.entity.behavior;

import com.darkness.jujutsukaisenzone.entity.SpecialEffectEntity;
import com.darkness.jujutsukaisenzone.entity.data.ShrineKaiConfig;
import com.darkness.jujutsukaisenzone.registry.ModEntities;
import com.darkness.jujutsukaisenzone.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 解（御厨子术式）延迟发射：播放动画后延迟发射斩击箭。
 */
public class ShrineKaiDelayBehavior extends EffectBehavior
{
    public static final String ID = "shrine_kai_delay";

    @Nullable
    private UUID ownerUuid;
    private int remainingLifeTicks = -1;
    private boolean arrowLaunched;
    private double maxRange;
    private boolean horizontal;

    @Override
    public String id() { return ID; }

    public void configure(@Nullable LivingEntity owner, double maxRange, boolean horizontal)
    {
        this.ownerUuid = owner == null ? null : owner.getUUID();
        this.remainingLifeTicks = ShrineKaiConfig.LAUNCH_DELAY_TICKS;
        this.arrowLaunched = false;
        this.maxRange = maxRange;
        this.horizontal = horizontal;
    }

    @Override
    public void tick(SpecialEffectEntity entity)
    {
        if (!(entity.level() instanceof ServerLevel level)) return;

        if (this.ownerUuid != null)
        {
            if (!(level.getEntity(this.ownerUuid) instanceof LivingEntity owner) || !owner.isAlive())
            {
                entity.discard();
                return;
            }
        }

        if (--this.remainingLifeTicks > 0) return;

        if (!this.arrowLaunched)
        {
            this.arrowLaunched = true;
            fireKaiArrow(entity);
        }
        entity.discard();
    }

    private void fireKaiArrow(SpecialEffectEntity parent)
    {
        if (!(parent.level() instanceof ServerLevel level)) return;
        if (this.ownerUuid == null || !(level.getEntity(this.ownerUuid) instanceof LivingEntity owner)) return;

        Vec3 look = owner.getLookAngle();
        if (look.lengthSqr() < 1.0E-8D) look = owner.getViewVector(1.0F);
        look = look.normalize();

        BlockPos foot = owner.blockPosition();
        BlockPos front = foot.relative(owner.getDirection());
        Vec3 spawn = Vec3.atCenterOf(front);

        SpecialEffectEntity arrow = ModEntities.SPECIAL_EFFECT_ENTITY.get().create(level);
        if (arrow == null) return;

        arrow.moveTo(spawn.x, spawn.y, spawn.z, owner.getYRot(), owner.getXRot());

        ShrineKaiArrowBehavior b = new ShrineKaiArrowBehavior();
        b.configure(owner, this.maxRange, look, this.horizontal, false);
        arrow.setBehaviorForInternal(b);
        if (!level.addFreshEntity(arrow)) return;

        level.playSound(null, spawn.x, spawn.y, spawn.z,
                this.horizontal ? ModSounds.SHRINE_SLASH_KAI_2_FIRE.get() : ModSounds.SHRINE_SLASH_KAI_1_FIRE.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    @Override
    public void writeToNbt(CompoundTag tag)
    {
        tag.putInt("RemainingLife", this.remainingLifeTicks);
        tag.putBoolean("ArrowLaunched", this.arrowLaunched);
        tag.putDouble("MaxRange", this.maxRange);
        tag.putBoolean("Horizontal", this.horizontal);
        if (this.ownerUuid != null) tag.putUUID("Owner", this.ownerUuid);
    }

    @Override
    public void readFromNbt(CompoundTag tag)
    {
        this.remainingLifeTicks = tag.contains("RemainingLife") ? tag.getInt("RemainingLife") : -1;
        this.arrowLaunched = tag.getBoolean("ArrowLaunched");
        this.maxRange = tag.contains("MaxRange") ? tag.getDouble("MaxRange") : 0.0D;
        this.horizontal = tag.getBoolean("Horizontal");
        this.ownerUuid = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
    }
}
