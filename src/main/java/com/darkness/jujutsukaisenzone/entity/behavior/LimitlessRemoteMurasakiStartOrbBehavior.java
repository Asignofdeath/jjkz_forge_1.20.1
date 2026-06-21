package com.darkness.jujutsukaisenzone.entity.behavior;

import com.darkness.jujutsukaisenzone.entity.SpecialEffectEntity;
import com.darkness.jujutsukaisenzone.entity.data.LimitlessRemoteMurasakiConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 超远程虚式茈起手演出：苍/赫光球跟随玩家视角停在左右两侧，随后向中线合拢。
 */
public final class LimitlessRemoteMurasakiStartOrbBehavior extends EffectBehavior
{
    public static final String ID = "limitless_remote_murasaki_start_orb";

    public enum Side
    {
        LEFT,
        RIGHT
    }

    @Nullable
    private UUID ownerUuid;
    private Side side = Side.LEFT;
    private boolean converging;

    @Override
    public String id()
    {
        return ID;
    }

    public void configure(@Nullable LivingEntity owner, Side side)
    {
        this.ownerUuid = owner == null ? null : owner.getUUID();
        this.side = side;
        this.converging = false;
    }

    public void beginConverge()
    {
        this.converging = true;
    }

    @Override
    public void tick(SpecialEffectEntity entity)
    {
        if (!(entity.level() instanceof ServerLevel))
        {
            return;
        }

        LivingEntity owner = resolveOwner(entity, this.ownerUuid);
        if (owner == null || !owner.isAlive())
        {
            entity.discard();
            return;
        }

        Vec3 target = this.converging
                ? LimitlessRemoteMurasakiBehavior.startCenterPosition(owner)
                : sideHoldPosition(owner);
        if (this.converging)
        {
            moveToward(entity, target, LimitlessRemoteMurasakiConfig.START_CONVERGE_SPEED_PER_TICK);
        }
        else
        {
            entity.setPos(target.x, target.y, target.z);
        }
        entity.setYRot(owner.getYRot());
        entity.setXRot(owner.getXRot());
    }

    private Vec3 sideHoldPosition(LivingEntity owner)
    {
        Vec3 center = LimitlessRemoteMurasakiBehavior.startCenterPosition(owner);
        Vec3 lateral = horizontalRight(owner).scale(LimitlessRemoteMurasakiConfig.START_SIDE_OFFSET);
        if (this.side == Side.LEFT)
        {
            lateral = lateral.scale(-1.0D);
        }
        return center.add(lateral);
    }

    private static void moveToward(SpecialEffectEntity entity, Vec3 target, double maxStep)
    {
        Vec3 pos = entity.position();
        Vec3 delta = target.subtract(pos);
        double dist = delta.length();
        if (dist < 0.05D)
        {
            entity.setPos(target.x, target.y, target.z);
            return;
        }
        double step = Math.min(maxStep, dist);
        Vec3 next = pos.add(delta.scale(step / dist));
        entity.setPos(next.x, next.y, next.z);
    }

    /** 玩家视线前方中心点（躯干高度）。 */
    static Vec3 startCenterPosition(LivingEntity owner)
    {
        Vec3 base = owner.position().add(
                0.0D,
                owner.getBbHeight() * LimitlessRemoteMurasakiConfig.FIRE_ANCHOR_HEIGHT_RATIO,
                0.0D);
        return base.add(viewDirection(owner).scale(LimitlessRemoteMurasakiConfig.START_CENTER_FORWARD));
    }

    /** 水平右向单位向量（相对玩家朝向）。 */
    static Vec3 horizontalRight(LivingEntity owner)
    {
        Vec3 look = viewDirection(owner);
        Vec3 flat = new Vec3(look.x, 0.0D, look.z);
        if (flat.lengthSqr() < 1.0E-8D)
        {
            float yawRad = owner.getYRot() * (float) (Math.PI / 180.0D);
            flat = new Vec3(-Mth.sin(yawRad), 0.0D, Mth.cos(yawRad));
        }
        flat = flat.normalize();
        return new Vec3(-flat.z, 0.0D, flat.x);
    }

    private static Vec3 viewDirection(LivingEntity owner)
    {
        Vec3 view = owner.getViewVector(1.0F);
        if (view.lengthSqr() < 1.0E-8D)
        {
            view = owner.getLookAngle();
        }
        return view.normalize();
    }

    @Override
    public void writeToNbt(CompoundTag tag)
    {
        tag.putString("Side", this.side.name());
        tag.putBoolean("Converging", this.converging);
        if (this.ownerUuid != null)
        {
            tag.putUUID("Owner", this.ownerUuid);
        }
    }

    @Override
    public void readFromNbt(CompoundTag tag)
    {
        this.side = tag.contains("Side") ? Side.valueOf(tag.getString("Side")) : Side.LEFT;
        this.converging = tag.getBoolean("Converging");
        this.ownerUuid = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
    }
}
