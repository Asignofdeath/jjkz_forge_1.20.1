package com.darkness.jujutsukaisenzone.entity.behavior;

import com.darkness.jujutsukaisenzone.entity.SpecialEffectEntity;
import com.darkness.jujutsukaisenzone.entity.data.BlackFlashConfig;
import com.darkness.jujutsukaisenzone.photon.PhotonFxCommands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 黑闪 FX 锚点：首 tick 执行 Photon，到期移除。
 */
public class BlackFlashFxBehavior extends EffectBehavior
{
    public static final String ID = "black_flash_fx";

    @Nullable
    private UUID ownerUuid;
    private int remainingLifeTicks = -1;

    @Override
    public String id() { return ID; }

    public void configure(@Nullable LivingEntity owner)
    {
        this.ownerUuid = owner == null ? null : owner.getUUID();
        this.remainingLifeTicks = BlackFlashConfig.FX_LIFETIME_TICKS;
    }

    @Override
    public void tick(SpecialEffectEntity entity)
    {
        if (this.remainingLifeTicks == BlackFlashConfig.FX_LIFETIME_TICKS)
        {
            if (entity.getServer() != null)
            {
                PhotonFxCommands.perform(entity, BlackFlashConfig.PHOTON_COMMAND);
            }
        }
        this.remainingLifeTicks--;
        if (this.remainingLifeTicks <= 0)
        {
            entity.discard();
        }
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

    @Nullable
    public LivingEntity getOwner(SpecialEffectEntity entity)
    {
        return resolveOwner(entity, this.ownerUuid);
    }
}
