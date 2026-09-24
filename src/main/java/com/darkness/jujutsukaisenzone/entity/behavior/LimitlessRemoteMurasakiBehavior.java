package com.darkness.jujutsukaisenzone.entity.behavior;

import com.darkness.jujutsukaisenzone.compat.EpicFightCompat;
import com.darkness.jujutsukaisenzone.api.screen.ScreenShakeKind;
import com.darkness.jujutsukaisenzone.entity.SpecialEffectEntity;
import com.darkness.jujutsukaisenzone.entity.data.LimitlessRemoteMurasakiConfig;
import com.darkness.jujutsukaisenzone.player.PlayerHoldReleaseChargeData;
import com.darkness.jujutsukaisenzone.entity.util.GeometryUtils;
import com.darkness.jujutsukaisenzone.entity.util.SpecialEffectChunkForce;
import com.darkness.jujutsukaisenzone.network.ModNetwork;
import com.darkness.jujutsukaisenzone.registry.ModEntities;
import com.darkness.jujutsukaisenzone.technique.config.SkillCombatConfigRegistry;
import com.darkness.jujutsukaisenzone.technique.skillset.LimitlessTechniqueSkillSet;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import com.darkness.jujutsukaisenzone.technique.TechniqueDamageSources;
import com.darkness.jujutsukaisenzone.technique.skillset.SkillDamageType;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 起手跟随玩家长按 5 秒（苍/赫光球演出），松手后面前蓄力 2 秒并挂 Photon → 同一主体向前飞行并清场。
 */
public class LimitlessRemoteMurasakiBehavior extends EffectBehavior
{
    public static final String ID = "limitless_remote_murasaki";

    private static final int PHASE_START = 0;
    private static final int PHASE_FIRE = 1;
    private static final int PHASE_PROJECTILE = 2;

    @Nullable
    private UUID ownerUuid;
    private int phase = PHASE_START;
    private int remainingLifeTicks = -1;
    private boolean photonMounted;
    private Vec3 velocity = Vec3.ZERO;
    private Vec3 launchPos = Vec3.ZERO;
    @Nullable
    private SpecialEffectChunkForce.Tracker chunkForceTracker;
    private int startElapsedTicks;
    @Nullable
    private UUID blueOrbUuid;
    @Nullable
    private UUID redOrbUuid;
    private int flightScreenShakeTicks;
    private boolean purpleOverlaySent;
    private double maxFlightDistance = SkillCombatConfigRegistry.resolveRemoteMurasakiMaxFlightDistance(null);

    @Override
    public String id()
    {
        return ID;
    }

    /** 玩家长按满蓄力后松手时由服务端调用，进入面前蓄力发射阶段。 */
    public void launchFromHoldRelease(SpecialEffectEntity entity)
    {
        if (this.phase != PHASE_START || !(entity.level() instanceof ServerLevel level))
        {
            return;
        }
        LivingEntity owner = resolveOwner(entity, this.ownerUuid);
        if (owner == null || !owner.isAlive())
        {
            return;
        }
        beginFirePhase(entity, owner, level);
    }

    public void configure(@Nullable LivingEntity owner)
    {
        this.ownerUuid = owner == null ? null : owner.getUUID();
        this.phase = PHASE_START;
        this.remainingLifeTicks = LimitlessRemoteMurasakiConfig.START_DELAY_TICKS;
        this.photonMounted = false;
        this.velocity = Vec3.ZERO;
        this.launchPos = Vec3.ZERO;
        this.chunkForceTracker = null;
        this.startElapsedTicks = 0;
        this.blueOrbUuid = null;
        this.redOrbUuid = null;
        this.flightScreenShakeTicks = 0;
        this.purpleOverlaySent = false;
        this.maxFlightDistance = SkillCombatConfigRegistry.resolveRemoteMurasakiMaxFlightDistance(owner);
        if (owner instanceof ServerPlayer serverPlayer)
        {
            EpicFightCompat.playRemoteMurasakiStartAnimation(serverPlayer);
        }
    }

    /** 起手演出中线前方锚点，供 {@link LimitlessRemoteMurasakiStartOrbBehavior} 使用。 */
    public static Vec3 startCenterPosition(LivingEntity owner)
    {
        return LimitlessRemoteMurasakiStartOrbBehavior.startCenterPosition(owner);
    }

    /** 玩家面前锚点世界坐标：躯干高度 + 沿视角前方固定距离。 */
    public static Vec3 anchorWorldPosition(LivingEntity owner)
    {
        Vec3 base = owner.position().add(
                0.0D,
                owner.getBbHeight() * LimitlessRemoteMurasakiConfig.FIRE_ANCHOR_HEIGHT_RATIO,
                0.0D);
        Vec3 view = owner.getViewVector(1.0F);
        if (view.lengthSqr() < 1.0E-8D)
        {
            view = owner.getLookAngle();
        }
        else
        {
            view = view.normalize();
        }
        return base.add(view.scale(LimitlessRemoteMurasakiConfig.FIRE_ANCHOR_FORWARD_DISTANCE));
    }

    @Override
    public void tick(SpecialEffectEntity entity)
    {
        if (!(entity.level() instanceof ServerLevel level))
        {
            return;
        }

        LivingEntity owner = resolveOwner(entity, this.ownerUuid);
        if (owner == null || !owner.isAlive())
        {
            clearOwnerHoldReleaseState(entity);
            endProjectile(entity, level);
            return;
        }

        switch (this.phase)
        {
            case PHASE_START -> tickStartPhase(entity, owner);
            case PHASE_FIRE -> tickFirePhase(entity, level, owner);
            case PHASE_PROJECTILE -> tickProjectilePhase(entity, level);
        }
    }

    @Override
    public void onDiscard(SpecialEffectEntity entity)
    {
        if (entity.level() instanceof ServerLevel level)
        {
            discardStartOrbs(level);
            releaseChunkForce(level, entity);
        }
    }

    private void tickStartPhase(SpecialEffectEntity entity, LivingEntity owner)
    {
        if (!(entity.level() instanceof ServerLevel level))
        {
            return;
        }

        entity.setPos(owner.getX(), owner.getY(), owner.getZ());
        entity.setYRot(owner.getYRot());
        entity.setXRot(owner.getXRot());

        if (this.startElapsedTicks == LimitlessRemoteMurasakiConfig.START_BLUE_SPAWN_TICK)
        {
            this.blueOrbUuid = spawnStartOrb(
                    level, owner, LimitlessRemoteMurasakiStartOrbBehavior.Side.LEFT,
                    LimitlessRemoteMurasakiConfig.PHOTON_START_BLUE_COMMAND);
            sendStartBlueOverlayIfCaster(owner);
        }
        if (this.startElapsedTicks == LimitlessRemoteMurasakiConfig.START_RED_SPAWN_TICK)
        {
            this.redOrbUuid = spawnStartOrb(
                    level, owner, LimitlessRemoteMurasakiStartOrbBehavior.Side.RIGHT,
                    LimitlessRemoteMurasakiConfig.PHOTON_START_RED_COMMAND);
            sendStartRedOverlayIfCaster(owner);
        }
        if (this.startElapsedTicks == LimitlessRemoteMurasakiConfig.START_CONVERGE_TICK)
        {
            beginStartOrbConverge(level);
        }

        this.startElapsedTicks++;
        tryTriggerPurpleOverlayIfCaster(owner);

        if (this.remainingLifeTicks > 0)
        {
            this.remainingLifeTicks--;
        }

        if (this.startElapsedTicks >= LimitlessRemoteMurasakiConfig.HOLD_RELEASE_CHARGE_TICKS
                && owner instanceof ServerPlayer serverPlayer)
        {
            LimitlessTechniqueSkillSet.INSTANCE.tryAutoCompleteRemoteMurasakiHoldRelease(serverPlayer, entity.getUUID());
        }
    }

    private void tryTriggerPurpleOverlayIfCaster(LivingEntity owner)
    {
        if (this.purpleOverlaySent
                || this.startElapsedTicks < LimitlessRemoteMurasakiConfig.START_PURPLE_OVERLAY_TICK)
        {
            return;
        }
        this.purpleOverlaySent = true;
        if (!(owner instanceof ServerPlayer serverPlayer))
        {
            return;
        }
        ModNetwork.sendPurpleOverlayAnimationSync(serverPlayer);
        ModNetwork.sendScreenShake(
                serverPlayer,
                ScreenShakeKind.DECAY,
                LimitlessRemoteMurasakiConfig.START_SCREEN_SHAKE_TICKS,
                LimitlessRemoteMurasakiConfig.START_SCREEN_SHAKE_MAX_DEG);
    }

    private static void sendStartBlueOverlayIfCaster(LivingEntity owner)
    {
        if (!(owner instanceof ServerPlayer serverPlayer))
        {
            return;
        }
        ModNetwork.sendColoredWorldOverlayAnimationSync(
                serverPlayer,
                LimitlessRemoteMurasakiConfig.START_BLUE_OVERLAY_COLOR_R,
                LimitlessRemoteMurasakiConfig.START_BLUE_OVERLAY_COLOR_G,
                LimitlessRemoteMurasakiConfig.START_BLUE_OVERLAY_COLOR_B,
                LimitlessRemoteMurasakiConfig.START_BLUE_OVERLAY_TOTAL_TICKS,
                LimitlessRemoteMurasakiConfig.START_BLUE_OVERLAY_FADE_IN_TICKS,
                LimitlessRemoteMurasakiConfig.START_BLUE_OVERLAY_FADE_OUT_TICKS,
                LimitlessRemoteMurasakiConfig.START_BLUE_OVERLAY_PEAK_ALPHA);
    }

    private static void sendStartRedOverlayIfCaster(LivingEntity owner)
    {
        if (!(owner instanceof ServerPlayer serverPlayer))
        {
            return;
        }
        ModNetwork.sendColoredWorldOverlayAnimationSync(
                serverPlayer,
                LimitlessRemoteMurasakiConfig.START_RED_OVERLAY_COLOR_R,
                LimitlessRemoteMurasakiConfig.START_RED_OVERLAY_COLOR_G,
                LimitlessRemoteMurasakiConfig.START_RED_OVERLAY_COLOR_B,
                LimitlessRemoteMurasakiConfig.START_RED_OVERLAY_TOTAL_TICKS,
                LimitlessRemoteMurasakiConfig.START_RED_OVERLAY_FADE_IN_TICKS,
                LimitlessRemoteMurasakiConfig.START_RED_OVERLAY_FADE_OUT_TICKS,
                LimitlessRemoteMurasakiConfig.START_RED_OVERLAY_PEAK_ALPHA);
    }

    private void beginFirePhase(SpecialEffectEntity entity, LivingEntity owner, ServerLevel level)
    {
        tryTriggerPurpleOverlayIfCaster(owner);
        discardStartOrbs(level);

        this.phase = PHASE_FIRE;
        this.remainingLifeTicks = LimitlessRemoteMurasakiConfig.FIRE_DELAY_TICKS;

        if (owner instanceof ServerPlayer serverPlayer)
        {
            EpicFightCompat.stopRemoteMurasakiStartAnimation(serverPlayer);
            EpicFightCompat.playRemoteMurasakiFireAnimation(serverPlayer);
        }

        Vec3 anchor = anchorWorldPosition(owner);
        entity.setPos(anchor.x, anchor.y, anchor.z);
        entity.setYRot(owner.getYRot());
        entity.setXRot(owner.getXRot());
        mountPhotonIfNeeded(entity);
    }

    private void tickFirePhase(SpecialEffectEntity entity, ServerLevel level, LivingEntity owner)
    {
        Vec3 anchor = anchorWorldPosition(owner);
        entity.setPos(anchor.x, anchor.y, anchor.z);
        entity.setYRot(owner.getYRot());
        entity.setXRot(owner.getXRot());

        if (this.remainingLifeTicks > 0)
        {
            this.remainingLifeTicks--;
            return;
        }

        beginProjectilePhase(entity, owner);
    }

    private void beginProjectilePhase(SpecialEffectEntity entity, LivingEntity owner)
    {
        Vec3 look = owner.getLookAngle();
        if (look.lengthSqr() < 1.0E-8D)
        {
            look = owner.getViewVector(1.0F);
        }
        look = look.normalize();

        Vec3 launch = entity.position().add(look.scale(LimitlessRemoteMurasakiConfig.ARROW_SPAWN_FORWARD));
        entity.setPos(
                launch.x,
                launch.y + LimitlessRemoteMurasakiConfig.ARROW_SPAWN_Y_OFFSET,
                launch.z);
        entity.setYRot(owner.getYRot());
        entity.setXRot(owner.getXRot());

        this.launchPos = entity.position();
        this.velocity = look.scale(LimitlessRemoteMurasakiConfig.ARROW_SPEED);
        this.maxFlightDistance = SkillCombatConfigRegistry.resolveRemoteMurasakiMaxFlightDistance(owner);
        this.phase = PHASE_PROJECTILE;
        this.ownerUuid = owner.getUUID();
        this.flightScreenShakeTicks = 0;
        this.chunkForceTracker = entity.createChunkForceTracker(LimitlessRemoteMurasakiConfig.CHUNK_FORCE_OPTIONS);
    }

    private void tickProjectilePhase(SpecialEffectEntity entity, ServerLevel level)
    {
        if (SpecialEffectChunkForce.hasExceededDistance(
                this.launchPos, entity.position(), this.maxFlightDistance))
        {
            endProjectile(entity, level);
            return;
        }

        if (this.chunkForceTracker != null)
        {
            entity.maintainChunkForce(this.chunkForceTracker, this.velocity);
        }

        entity.advanceVelocityFlightUnbounded(this.velocity);

        if (this.flightScreenShakeTicks % LimitlessRemoteMurasakiConfig.FLIGHT_SCREEN_SHAKE_INTERVAL_TICKS == 0)
        {
            broadcastFlightScreenShake(level, entity);
        }
        this.flightScreenShakeTicks++;

        eraseBlocksInSphere(entity);
        damageLivingInSphere(entity);
    }

    private static void broadcastFlightScreenShake(ServerLevel level, SpecialEffectEntity entity)
    {
        Vec3 pos = entity.position();
        double radiusSqr = LimitlessRemoteMurasakiConfig.FLIGHT_SCREEN_SHAKE_RADIUS
                * LimitlessRemoteMurasakiConfig.FLIGHT_SCREEN_SHAKE_RADIUS;
        for (ServerPlayer player : level.players())
        {
            if (player.distanceToSqr(pos.x, pos.y, pos.z) > radiusSqr)
            {
                continue;
            }
            ModNetwork.sendScreenShake(
                    player,
                    ScreenShakeKind.SMOOTH_WOBBLE,
                    LimitlessRemoteMurasakiConfig.FLIGHT_SCREEN_SHAKE_DURATION_TICKS,
                    LimitlessRemoteMurasakiConfig.FLIGHT_SCREEN_SHAKE_MAX_AMPLITUDE);
        }
    }

    private void endProjectile(SpecialEffectEntity entity, ServerLevel level)
    {
        clearOwnerHoldReleaseState(entity);
        discardStartOrbs(level);
        releaseChunkForce(level, entity);
        entity.discard();
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
            LimitlessTechniqueSkillSet.abortRemoteMurasakiStartPresentation(player);
            PlayerHoldReleaseChargeData.clear(player);
        }
    }

    @Nullable
    private UUID spawnStartOrb(
            ServerLevel level,
            LivingEntity owner,
            LimitlessRemoteMurasakiStartOrbBehavior.Side side,
            String photonCommand)
    {
        SpecialEffectEntity orb = ModEntities.SPECIAL_EFFECT_ENTITY.get().create(level);
        if (orb == null)
        {
            return null;
        }

        Vec3 center = LimitlessRemoteMurasakiStartOrbBehavior.startCenterPosition(owner);
        Vec3 lateral = LimitlessRemoteMurasakiStartOrbBehavior.horizontalRight(owner)
                .scale(LimitlessRemoteMurasakiConfig.START_SIDE_OFFSET);
        if (side == LimitlessRemoteMurasakiStartOrbBehavior.Side.LEFT)
        {
            lateral = lateral.scale(-1.0D);
        }
        Vec3 spawn = center.add(lateral);
        orb.moveTo(spawn.x, spawn.y, spawn.z, owner.getYRot(), owner.getXRot());
        orb.configureLimitlessRemoteMurasakiStartOrb(owner, side);
        if (!level.addFreshEntity(orb))
        {
            return null;
        }

        CommandSourceStack commandSource = orb.createCommandSourceStack()
                .withPermission(2)
                .withSuppressedOutput();
        level.getServer().getCommands().performPrefixedCommand(commandSource, photonCommand);
        return orb.getUUID();
    }

    private void beginStartOrbConverge(ServerLevel level)
    {
        triggerStartOrbConverge(level, this.blueOrbUuid);
        triggerStartOrbConverge(level, this.redOrbUuid);
    }

    private void triggerStartOrbConverge(ServerLevel level, @Nullable UUID orbUuid)
    {
        if (orbUuid == null)
        {
            return;
        }
        if (!(level.getEntity(orbUuid) instanceof SpecialEffectEntity orb))
        {
            return;
        }
        if (orb.getEffectBehavior() instanceof LimitlessRemoteMurasakiStartOrbBehavior orbBehavior)
        {
            orbBehavior.beginConverge();
        }
    }

    private void discardStartOrbs(ServerLevel level)
    {
        discardStartOrb(level, this.blueOrbUuid);
        discardStartOrb(level, this.redOrbUuid);
        this.blueOrbUuid = null;
        this.redOrbUuid = null;
    }

    private void discardStartOrb(ServerLevel level, @Nullable UUID orbUuid)
    {
        if (orbUuid == null)
        {
            return;
        }
        if (level.getEntity(orbUuid) instanceof SpecialEffectEntity orb)
        {
            orb.discard();
        }
    }

    private void releaseChunkForce(ServerLevel level, SpecialEffectEntity entity)
    {
        if (this.chunkForceTracker != null)
        {
            entity.releaseChunkForce(this.chunkForceTracker);
            this.chunkForceTracker = null;
        }
    }

    private void mountPhotonIfNeeded(SpecialEffectEntity entity)
    {
        if (this.photonMounted || !(entity.level() instanceof ServerLevel level))
        {
            return;
        }

        CommandSourceStack commandSource = entity.createCommandSourceStack()
                .withPermission(2)
                .withSuppressedOutput();
        level.getServer().getCommands().performPrefixedCommand(commandSource, LimitlessRemoteMurasakiConfig.PHOTON_COMMAND);
        this.photonMounted = true;
    }

    private void eraseBlocksInSphere(SpecialEffectEntity entity)
    {
        Level level = entity.level();
        if (level.isClientSide())
        {
            return;
        }

        double cx = entity.getX();
        double cy = entity.getY();
        double cz = entity.getZ();
        double r = LimitlessRemoteMurasakiConfig.ARROW_BLOCK_ERASE_RADIUS;
        double rSqr = r * r;
        int ri = Mth.ceil(r);
        int ox = Mth.floor(cx);
        int oy = Mth.floor(cy);
        int oz = Mth.floor(cz);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int dx = -ri; dx <= ri; dx++)
        {
            for (int dy = -ri; dy <= ri; dy++)
            {
                for (int dz = -ri; dz <= ri; dz++)
                {
                    pos.set(ox + dx, oy + dy, oz + dz);
                    if (level.isOutsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos))
                    {
                        continue;
                    }
                    if (!level.hasChunkAt(pos))
                    {
                        continue;
                    }
                    if (!GeometryUtils.blockCubeIntersectsSphere(cx, cy, cz, rSqr, pos.getX(), pos.getY(), pos.getZ()))
                    {
                        continue;
                    }
                    if (level.getBlockState(pos).isAir())
                    {
                        continue;
                    }
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                }
            }
        }
    }

    private void damageLivingInSphere(SpecialEffectEntity entity)
    {
        Level level = entity.level();
        if (level.isClientSide())
        {
            return;
        }

        double cx = entity.getX();
        double cy = entity.getY();
        double cz = entity.getZ();
        double r = LimitlessRemoteMurasakiConfig.ARROW_BLOCK_ERASE_RADIUS;
        double rSqr = r * r;
        AABB query = new AABB(cx, cy, cz, cx, cy, cz).inflate(r);

        LivingEntity owner = resolveOwner(entity, this.ownerUuid);
        float dmg = LimitlessRemoteMurasakiConfig.ARROW_SPHERE_DAMAGE_PER_TICK;

        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, query))
        {
            if (!target.isAlive())
            {
                continue;
            }
            if (!GeometryUtils.aabbIntersectsSphere(cx, cy, cz, rSqr, target.getBoundingBox()))
            {
                continue;
            }
            if (owner != null && target.getUUID().equals(owner.getUUID()))
            {
                continue;
            }

            DamageSource source = owner != null
                    ? entity.damageSources().mobProjectile(entity, owner)
                    : entity.damageSources().magic();
            TechniqueDamageSources.runWithType(SkillDamageType.TECHNIQUE, () -> {
                target.hurt(source, dmg);
                if (!(target instanceof Player) && !target.isAlive())
                {
                    target.discard();
                }
            });
        }
    }

    @Override
    public void writeToNbt(CompoundTag tag)
    {
        tag.putInt("Phase", this.phase);
        tag.putInt("RemainingLife", this.remainingLifeTicks);
        tag.putBoolean("PhotonMounted", this.photonMounted);
        tag.putDouble("VelX", this.velocity.x);
        tag.putDouble("VelY", this.velocity.y);
        tag.putDouble("VelZ", this.velocity.z);
        tag.putDouble("LaunchX", this.launchPos.x);
        tag.putDouble("LaunchY", this.launchPos.y);
        tag.putDouble("LaunchZ", this.launchPos.z);
        tag.putInt("StartElapsed", this.startElapsedTicks);
        tag.putBoolean("PurpleOverlaySent", this.purpleOverlaySent);
        if (this.blueOrbUuid != null)
        {
            tag.putUUID("BlueOrb", this.blueOrbUuid);
        }
        if (this.redOrbUuid != null)
        {
            tag.putUUID("RedOrb", this.redOrbUuid);
        }
        if (this.ownerUuid != null)
        {
            tag.putUUID("Owner", this.ownerUuid);
        }
    }

    @Override
    public void readFromNbt(CompoundTag tag)
    {
        this.phase = tag.contains("Phase") ? tag.getInt("Phase") : PHASE_START;
        this.remainingLifeTicks = tag.contains("RemainingLife") ? tag.getInt("RemainingLife") : -1;
        this.photonMounted = tag.getBoolean("PhotonMounted");
        this.velocity = new Vec3(tag.getDouble("VelX"), tag.getDouble("VelY"), tag.getDouble("VelZ"));
        this.launchPos = new Vec3(tag.getDouble("LaunchX"), tag.getDouble("LaunchY"), tag.getDouble("LaunchZ"));
        this.ownerUuid = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        this.startElapsedTicks = tag.contains("StartElapsed") ? tag.getInt("StartElapsed") : 0;
        this.purpleOverlaySent = tag.getBoolean("PurpleOverlaySent");
        this.blueOrbUuid = tag.hasUUID("BlueOrb") ? tag.getUUID("BlueOrb") : null;
        this.redOrbUuid = tag.hasUUID("RedOrb") ? tag.getUUID("RedOrb") : null;
        this.chunkForceTracker = this.phase == PHASE_PROJECTILE
                ? SpecialEffectChunkForce.create(LimitlessRemoteMurasakiConfig.CHUNK_FORCE_OPTIONS)
                : null;
    }
}
