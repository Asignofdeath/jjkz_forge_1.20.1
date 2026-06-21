package com.darkness.jujutsukaisenzone.entity.behavior;

import com.darkness.jujutsukaisenzone.entity.SpecialEffectEntity;
import com.darkness.jujutsukaisenzone.entity.data.ShrineKaiConfig;
import com.darkness.jujutsukaisenzone.photon.OwnerOnlyShrineSlashFx;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 解到达终点锚点：挂 Photon 斩击特效，面朝施法者。
 */
public class ShrineKaiArrivalBehavior extends EffectBehavior
{
    public static final String ID = "shrine_kai_arrival";

    @Nullable
    private UUID ownerUuid;
    private int remainingLifeTicks = -1;
    private int slashType = 1; // 1=竖斩 2=横斩

    @Override
    public String id() { return ID; }

    public void configure(@Nullable LivingEntity owner, int slashType)
    {
        this.ownerUuid = owner == null ? null : owner.getUUID();
        this.remainingLifeTicks = ShrineKaiConfig.ARRIVAL_POST_PHOTON_TICKS;
        this.slashType = slashType;
    }

    @Override
    public void tick(SpecialEffectEntity entity)
    {
        if (this.remainingLifeTicks == ShrineKaiConfig.ARRIVAL_POST_PHOTON_TICKS)
        {
            ServerPlayer owner = resolveOwnerPlayer(entity, this.ownerUuid);
            if (owner != null)
            {
                OwnerOnlyShrineSlashFx.mountKaiArrival(owner, entity, this.slashType == 2);
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
        tag.putInt("SlashType", this.slashType);
        if (this.ownerUuid != null) tag.putUUID("Owner", this.ownerUuid);
    }

    @Override
    public void readFromNbt(CompoundTag tag)
    {
        this.remainingLifeTicks = tag.contains("RemainingLife") ? tag.getInt("RemainingLife") : -1;
        this.slashType = tag.contains("SlashType") ? tag.getInt("SlashType") : 1;
        this.ownerUuid = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
    }
}
