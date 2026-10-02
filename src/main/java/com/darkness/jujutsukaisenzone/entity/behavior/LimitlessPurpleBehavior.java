package com.darkness.jujutsukaisenzone.entity.behavior;

import com.darkness.jujutsukaisenzone.entity.SpecialEffectEntity;
import com.darkness.jujutsukaisenzone.entity.data.LimitlessPurpleConfig;
import com.darkness.jujutsukaisenzone.registry.ModEntities;
import com.darkness.jujutsukaisenzone.registry.ModSounds;
import com.darkness.jujutsukaisenzone.photon.PhotonFxCommands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 无下限术式「茈」：胸前蓄力锚点，到期发射茈箭。
 */
public class LimitlessPurpleBehavior extends EffectBehavior
{
    public static final String ID = "limitless_purple";

    @Nullable
    private UUID ownerUuid;
    private int remainingLifeTicks = -1;

    @Override
    public String id() { return ID; }

    public void configure(@Nullable LivingEntity owner)
    {
        this.ownerUuid = owner == null ? null : owner.getUUID();
        this.remainingLifeTicks = LimitlessPurpleConfig.CHARGE_TICKS;
    }

    @Override
    public void tick(SpecialEffectEntity entity)
    {
        if (!(entity.level() instanceof ServerLevel level)) return;

        LivingEntity owner = resolveOwner(entity, this.ownerUuid);
        if (owner == null || !owner.isAlive())
        {
            entity.discard();
            return;
        }

        // 跟随玩家胸前锚点
        Vec3 anchor = anchorWorldPosition(owner);
        entity.setPos(anchor.x, anchor.y, anchor.z);
        entity.setYRot(owner.getYRot());
        entity.setXRot(owner.getXRot());

        if (this.remainingLifeTicks <= 0) return;
        this.remainingLifeTicks--;
        if (this.remainingLifeTicks == 0)
        {
            firePurpleArrow(entity);
            entity.discard();
        }
    }

    /** 茈蓄力锚点世界坐标：躯干高度 + 沿视角前方。 */
    public static Vec3 anchorWorldPosition(LivingEntity owner)
    {
        Vec3 base = owner.position().add(0.0D, owner.getBbHeight() * LimitlessPurpleConfig.ANCHOR_CHEST_HEIGHT_RATIO, 0.0D);
        Vec3 view = owner.getViewVector(1.0F);
        if (view.lengthSqr() < 1.0E-8D) view = owner.getLookAngle();
        else view = view.normalize();
        return base.add(view.scale(LimitlessPurpleConfig.ANCHOR_FORWARD_DISTANCE));
    }

    private void firePurpleArrow(SpecialEffectEntity anchor)
    {
        if (!(anchor.level() instanceof ServerLevel level)) return;

        SpecialEffectEntity arrow = ModEntities.SPECIAL_EFFECT_ENTITY.get().create(level);
        if (arrow == null) return;

        Vec3 look = anchor.getLookAngle();
        Vec3 spawn = anchor.position().add(look.scale(LimitlessPurpleConfig.ARROW_SPAWN_FORWARD));
        arrow.moveTo(spawn.x, spawn.y + LimitlessPurpleConfig.ARROW_SPAWN_Y_OFFSET, spawn.z, anchor.getYRot(), anchor.getXRot());

        LivingEntity purpleOwner = this.ownerUuid != null && level.getEntity(this.ownerUuid) instanceof LivingEntity living ? living : null;
        Vec3 velocity = look.scale(LimitlessPurpleConfig.ARROW_SPEED);

        LimitlessPurpleArrowBehavior b = new LimitlessPurpleArrowBehavior();
        b.configure(purpleOwner, velocity);
        arrow.attach(b);
        if (!level.addFreshEntity(arrow)) return;

        if (purpleOwner != null)
        {
            level.playSound(null, purpleOwner.getX(), purpleOwner.getY(), purpleOwner.getZ(),
                    ModSounds.LIMITLESS_MURASAKI_MINI.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }

        PhotonFxCommands.perform(arrow, LimitlessPurpleConfig.PHOTON_ARROW_COMMAND);
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
