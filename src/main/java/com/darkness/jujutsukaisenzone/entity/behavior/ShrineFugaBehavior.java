package com.darkness.jujutsukaisenzone.entity.behavior;

import com.darkness.jujutsukaisenzone.api.screen.ScreenShakeKind;
import com.darkness.jujutsukaisenzone.compat.EpicFightCompat;
import com.darkness.jujutsukaisenzone.entity.SpecialEffectEntity;
import com.darkness.jujutsukaisenzone.entity.data.ShrineFugaConfig;
import com.darkness.jujutsukaisenzone.entity.util.SpecialEffectFlight;
import com.darkness.jujutsukaisenzone.network.ModNetwork;
import com.darkness.jujutsukaisenzone.player.PlayerHoldReleaseChargeData;
import com.darkness.jujutsukaisenzone.registry.ModEntities;
import com.darkness.jujutsukaisenzone.registry.ModSounds;
import com.darkness.jujutsukaisenzone.photon.PhotonFxCommands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 「灶」开：同一特效实体在玩家长按蓄力期间跟随视线，松手满 2 秒后沿锁定朝向飞行；photon:fuga 全程不卸载不重挂。
 */
public final class ShrineFugaBehavior extends EffectBehavior
{
    public static final String ID = "shrine_fuga";

    @Nullable
    private UUID ownerUuid;
    private boolean flying;
    /** 匀速直线飞行状态（方向 / 已飞距离 / 射程）；速度与射程每 tick 从配置重取。 */
    private SpecialEffectFlight.LinearMotion motion =
            new SpecialEffectFlight.LinearMotion(new Vec3(0.0D, 0.0D, 1.0D), 0.0D, 1.0D);

    @Override
    public String id()
    {
        return ID;
    }

    public void configure(@Nullable LivingEntity owner)
    {
        this.ownerUuid = owner == null ? null : owner.getUUID();
        this.flying = false;
        this.motion = new SpecialEffectFlight.LinearMotion(new Vec3(0.0D, 0.0D, 1.0D), 0.0D, 1.0D);
    }

    /** 玩家长按满蓄力后松手时由服务端调用，锁定当前朝向并开始飞行。 */
    public void launchFromHoldRelease(SpecialEffectEntity entity)
    {
        if (this.flying)
        {
            return;
        }
        beginFlight(entity);
    }

    @Override
    public void tick(SpecialEffectEntity entity)
    {
        entity.setNoGravity(true);
        if (!(entity.level() instanceof ServerLevel level))
        {
            return;
        }

        LivingEntity owner = resolveOwner(entity, this.ownerUuid);
        if (owner == null || !owner.isAlive())
        {
            clearOwnerHoldReleaseState(entity);
            entity.discard();
            return;
        }

        if (!this.flying)
        {
            tickCharge(entity, owner);
            return;
        }

        tickFlight(entity, level);
    }

    private void tickCharge(SpecialEffectEntity entity, LivingEntity owner)
    {
        syncTransformWithOwner(entity, owner);
    }

    /** 锁定蓄力结束瞬间的朝向与飞行方向，发射后不再改旋转。 */
    private void beginFlight(SpecialEffectEntity entity)
    {
        this.motion.setDirection(entity.getLookAngle());
        this.motion.setTraveled(0.0D);
        this.flying = true;
        if (entity.level() instanceof ServerLevel level)
        {
            level.playSound(
                    null,
                    entity.getX(),
                    entity.getY(),
                    entity.getZ(),
                    ModSounds.SHRINE_FUGA_FIRE.get(),
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F
            );
        }
    }

    private void tickFlight(SpecialEffectEntity entity, ServerLevel level)
    {
        // 逐格匀速直线推进（每步 ≤ 1 格、射程封顶）交给 SpecialEffectFlight；
        // 速度与射程每 tick 从配置重取，改 TOML 立即生效。
        this.motion.setSpeedPerTick(ShrineFugaConfig.PROJECTILE_SPEED);
        this.motion.setMaxRange(ShrineFugaConfig.MAX_RANGE);

        SpecialEffectFlight.Result result = SpecialEffectFlight.tickLinear(entity, this.motion,
                (e, from, to) -> onFlightStep(level, e, from, to));

        if (result == SpecialEffectFlight.Result.ABORT_CHUNK_UNLOADED)
        {
            entity.discard();
            return;
        }
        if (result == SpecialEffectFlight.Result.ARRIVED)
        {
            // 抵达射程终点：按原实现直接在该点引爆。
            explode(entity, entity.position());
        }
    }

    /**
     * 单步碰撞检测（由 {@link SpecialEffectFlight#tickLinear} 每步回调）。
     * <p>
     * 与原实现逐条对应：起步 {@code COLLISION_MIN_TRAVEL} 格内不判碰撞；抵达射程终点的那一步
     * 也不判碰撞（终点一律按射程引爆）。命中实体或方块就地引爆，返回 {@code true} 终止本 tick 剩余步进。
     */
    private boolean onFlightStep(ServerLevel level, SpecialEffectEntity entity, Vec3 from, Vec3 to)
    {
        if (this.motion.traveled() < ShrineFugaConfig.COLLISION_MIN_TRAVEL) return false;
        if (this.motion.traveled() >= this.motion.maxRange()) return false;

        if (findHitLivingEntity(entity, level, from, to, this.ownerUuid) != null)
        {
            explode(entity, to);
            return true;
        }

        BlockHitResult blockHit = clipBlocks(entity, level, from, to);
        if (blockHit.getType() != HitResult.Type.MISS)
        {
            explode(entity, blockHit.getLocation());
            return true;
        }
        return false;
    }

    private void explode(SpecialEffectEntity entity, Vec3 impact)
    {
        if (!(entity.level() instanceof ServerLevel level))
        {
            entity.discard();
            return;
        }

        LivingEntity owner = resolveOwner(entity, this.ownerUuid);
        level.playSound(
                null,
                impact.x,
                impact.y,
                impact.z,
                ModSounds.SHRINE_FUGA_BOOM.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F
        );
        broadcastBoomClientEffects(level, impact);
        SpecialEffectEntity boom = ModEntities.SPECIAL_EFFECT_ENTITY.get().create(level);
        if (boom != null)
        {
            boom.moveTo(impact.x, impact.y, impact.z, entity.getYRot(), entity.getXRot());
            ShrineFugaBoomBehavior behavior = new ShrineFugaBoomBehavior();
            behavior.configure(owner);
            boom.attach(behavior);
            level.addFreshEntity(boom);
        }

        entity.discard();
    }

    private static void broadcastBoomClientEffects(ServerLevel level, Vec3 impact)
    {
        double radiusSqr = ShrineFugaConfig.BOOM_SCREEN_SHAKE_RADIUS * ShrineFugaConfig.BOOM_SCREEN_SHAKE_RADIUS;
        for (ServerPlayer player : level.players())
        {
            if (player.distanceToSqr(impact.x, impact.y, impact.z) > radiusSqr)
            {
                continue;
            }
            ModNetwork.sendScreenShake(
                    player,
                    ScreenShakeKind.CONSTANT,
                    ShrineFugaConfig.BOOM_SCREEN_SHAKE_TICKS,
                    ShrineFugaConfig.BOOM_SCREEN_SHAKE_MAX_DEG
            );
            ModNetwork.sendFugaBoomFlashSequenceSync(player);
        }
    }

    private void clearOwnerHoldReleaseState(SpecialEffectEntity entity)
    {
        if (this.ownerUuid == null || entity.getServer() == null)
        {
            return;
        }
        ServerPlayer player = entity.getServer().getPlayerList().getPlayer(this.ownerUuid);
        if (player != null
                && PlayerHoldReleaseChargeData.isCharging(player)
                && PlayerHoldReleaseChargeData.getChargeEntityUuid(player).equals(entity.getUUID()))
        {
            EpicFightCompat.stopFugaChargeAnimation(player);
            PlayerHoldReleaseChargeData.clear(player);
        }
    }

    /** 每 tick 将锚点位置、身体朝向与视线与玩家对齐（供 photon look 模式读取）。 */
    public static void syncTransformWithOwner(SpecialEffectEntity entity, LivingEntity owner)
    {
        Vec3 anchor = anchorWorldPosition(owner);
        entity.moveTo(anchor.x, anchor.y, anchor.z, owner.getYRot(), owner.getXRot());
        entity.setYBodyRot(owner.getYRot());
        entity.setYHeadRot(owner.getYRot());
    }

    /** 玩家面前蓄力锚点世界坐标。 */
    public static Vec3 anchorWorldPosition(LivingEntity owner)
    {
        Vec3 base = owner.position().add(0.0D, owner.getBbHeight() * ShrineFugaConfig.ANCHOR_HEIGHT_RATIO, 0.0D);
        Vec3 view = owner.getViewVector(1.0F);
        if (view.lengthSqr() < 1.0E-8D)
        {
            view = owner.getLookAngle();
        }
        else
        {
            view = view.normalize();
        }
        return base.add(view.scale(ShrineFugaConfig.ANCHOR_FORWARD_DISTANCE));
    }

    @Nullable
    private static LivingEntity findHitLivingEntity(
            SpecialEffectEntity entity,
            ServerLevel level,
            Vec3 from,
            Vec3 to,
            @Nullable UUID ownerUuid)
    {
        AABB sweep = new AABB(from, to).inflate(0.35D);
        LivingEntity owner = resolveOwner(entity, ownerUuid);
        LivingEntity closest = null;
        double closestDistSqr = Double.MAX_VALUE;

        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, sweep))
        {
            if (!target.isAlive())
            {
                continue;
            }
            if (target.getUUID().equals(entity.getUUID()) || target instanceof SpecialEffectEntity)
            {
                continue;
            }
            if (owner != null && target.getUUID().equals(owner.getUUID()))
            {
                continue;
            }
            if (!target.getBoundingBox().intersects(sweep))
            {
                continue;
            }
            double distSqr = target.distanceToSqr(to);
            if (distSqr < closestDistSqr)
            {
                closestDistSqr = distSqr;
                closest = target;
            }
        }
        return closest;
    }

    private static BlockHitResult clipBlocks(SpecialEffectEntity entity, Level level, Vec3 from, Vec3 to)
    {
        return level.clip(new ClipContext(
                from,
                to,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                entity
        ));
    }

    @Override
    public void onDiscard(SpecialEffectEntity entity)
    {
        if (entity.getServer() == null)
        {
            return;
        }
        PhotonFxCommands.perform(entity, ShrineFugaConfig.PHOTON_FUGA_REMOVE_COMMAND);
    }

    @Override
    public void writeToNbt(CompoundTag tag)
    {
        tag.putBoolean("Flying", this.flying);
        this.motion.writeToNbt(tag);
        if (this.ownerUuid != null)
        {
            tag.putUUID("Owner", this.ownerUuid);
        }
    }

    @Override
    public void readFromNbt(CompoundTag tag)
    {
        this.flying = tag.getBoolean("Flying");
        this.motion = SpecialEffectFlight.LinearMotion.readFromNbt(tag);
        this.ownerUuid = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
    }
}
