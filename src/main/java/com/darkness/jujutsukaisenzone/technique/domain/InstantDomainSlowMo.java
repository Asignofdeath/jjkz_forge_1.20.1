package com.darkness.jujutsukaisenzone.technique.domain;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.network.ModNetwork;
import com.darkness.jujutsukaisenzone.player.PlayerBrainDamageData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 瞬发领域「慢镜头」：半径内全体生物定格（含施术者），关闭后对非施术者按领域内停留时长 ×6 延续控制
 * （上限 90 秒；倍率高于常规 ×3，补偿瞬发领域偏短）。
 * <p>
 * 不使用会刷图标的缓慢/挖掘疲劳；不改朝向、不搬散布点。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
final class InstantDomainSlowMo
{
    /** 作用半径（格）。 */
    static final double RADIUS = 32.0D;
    /** 与 {@code LimitlessDomainExpansion#POST_DOMAIN_LOCK_MAX_TICKS} 一致。 */
    private static final int POST_DOMAIN_LOCK_MAX_TICKS = 90 * 20;
    /**
     * 关域后遗症：领域内停留 tick × 该倍率。
     * 瞬发领域时长偏短，用 ×6 补偿（常规无量空处为 ×3）。
     */
    private static final int POST_DOMAIN_LOCK_MULTIPLIER = 6;

    private static final UUID SPEED_MODIFIER_UUID =
            UUID.fromString("6e2c9a10-4b7f-4d91-9c3a-1f8e5d2b0a77");
    private static final UUID ATTACK_SPEED_MODIFIER_UUID =
            UUID.fromString("7f3d0b21-5c80-4e02-ad4b-2a9f6e3c1b88");
    private static final String SPEED_MODIFIER_NAME = "jjkz_instant_domain_slowmo_speed";
    private static final String ATTACK_SPEED_MODIFIER_NAME = "jjkz_instant_domain_slowmo_attack";
    private static final double MULTIPLY_TO_ZERO = -1.0D;

    /** 领域仍开启时的定格。 */
    private static final ConcurrentMap<UUID, FreezeState> ACTIVE = new ConcurrentHashMap<>();
    /** 关域后的延续定格（remainingTicks 倒数）。 */
    private static final ConcurrentMap<UUID, AftermathState> AFTERMATH = new ConcurrentHashMap<>();

    private InstantDomainSlowMo()
    {
    }

    static void refresh(ServerLevel level, BlockPos centerPos, Set<UUID> trackedTargets)
    {
        if (level == null || centerPos == null || trackedTargets == null)
        {
            return;
        }

        Vec3 center = Vec3.atCenterOf(centerPos);
        double radiusSqr = RADIUS * RADIUS;
        AABB box = new AABB(center, center).inflate(RADIUS);
        Set<UUID> inRange = new HashSet<>();

        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive))
        {
            if (living.distanceToSqr(center) > radiusSqr)
            {
                continue;
            }
            /* 后遗症阶段被再次扫进半径时，先并入本场 active（用已有 freeze 状态续计时）。 */
            promoteAftermathIntoActiveIfPresent(living);
            applyActive(living);
            inRange.add(living.getUUID());
            trackedTargets.add(living.getUUID());
        }

        for (UUID id : Set.copyOf(trackedTargets))
        {
            if (inRange.contains(id))
            {
                continue;
            }
            if (level.getEntity(id) instanceof LivingEntity living)
            {
                stripActive(living);
            }
            else
            {
                releaseOrphanActive(id, level);
            }
            trackedTargets.remove(id);
        }
    }

    /**
     * 领域正常关闭：施术者立刻解除；其余目标按停留时长 ×6 进入后遗症控制，
     * 并对玩家按停留秒数 ×2 增加脑损（无提示）。
     */
    static void beginAftermath(ServerLevel level, Set<UUID> trackedTargets, @Nullable UUID casterUuid)
    {
        if (trackedTargets == null || trackedTargets.isEmpty())
        {
            return;
        }
        for (UUID id : Set.copyOf(trackedTargets))
        {
            FreezeState state = ACTIVE.remove(id);
            if (state == null)
            {
                trackedTargets.remove(id);
                continue;
            }
            if (casterUuid != null && casterUuid.equals(id))
            {
                if (level != null && level.getEntity(id) instanceof LivingEntity living)
                {
                    releaseFreeze(living, state);
                }
                else
                {
                    unlockInputOrphan(id, level, state);
                }
                trackedTargets.remove(id);
                continue;
            }

            /* 后遗症脑损：停留秒数 ×2（瞬发）；仅玩家；无提示。 */
            if (level != null && level.getEntity(id) instanceof ServerPlayer victim)
            {
                PlayerBrainDamageData.addFromLimitlessDomainDwell(victim, state.elapsedTicks(), true);
            }
            else if (level != null && level.getServer() != null)
            {
                ServerPlayer online = level.getServer().getPlayerList().getPlayer(id);
                if (online != null)
                {
                    PlayerBrainDamageData.addFromLimitlessDomainDwell(online, state.elapsedTicks(), true);
                }
            }

            int postTicks = Math.min(state.elapsedTicks() * POST_DOMAIN_LOCK_MULTIPLIER, POST_DOMAIN_LOCK_MAX_TICKS);
            if (postTicks <= 0)
            {
                if (level != null && level.getEntity(id) instanceof LivingEntity living)
                {
                    releaseFreeze(living, state);
                }
                else
                {
                    unlockInputOrphan(id, level, state);
                }
                trackedTargets.remove(id);
                continue;
            }

            AFTERMATH.put(id, new AftermathState(
                    state.aiPausedByUs(), state.originNoAi(), state.inputLocked(), postTicks));
            if (level != null && level.getEntity(id) instanceof LivingEntity living)
            {
                maintainFreezePose(living, state.aiPausedByUs());
            }
            trackedTargets.remove(id);
        }
        trackedTargets.clear();
    }

    /**
     * 立刻卸掉领域内 active 定格（不停机后遗症）；用于非瞬发路径的空表清理等。
     */
    static void clearActive(ServerLevel level, Set<UUID> trackedTargets)
    {
        if (trackedTargets == null || trackedTargets.isEmpty())
        {
            return;
        }
        for (UUID id : Set.copyOf(trackedTargets))
        {
            FreezeState state = ACTIVE.remove(id);
            if (state == null)
            {
                continue;
            }
            if (level != null && level.getEntity(id) instanceof LivingEntity living)
            {
                releaseFreeze(living, state);
            }
            else
            {
                unlockInputOrphan(id, level, state);
            }
        }
        trackedTargets.clear();
    }

    /** 停机 / 会话重置：卸掉 active + aftermath。 */
    static void clearAll(MinecraftServer server)
    {
        for (UUID id : Set.copyOf(ACTIVE.keySet()))
        {
            FreezeState state = ACTIVE.remove(id);
            if (state == null)
            {
                continue;
            }
            LivingEntity living = findLiving(server, id);
            if (living != null)
            {
                releaseFreeze(living, state);
            }
            else
            {
                unlockInputOrphan(id, null, state);
                if (state.inputLocked() && server != null)
                {
                    ServerPlayer online = server.getPlayerList().getPlayer(id);
                    if (online != null)
                    {
                        ModNetwork.sendPlayerInputLockSync(online, false);
                    }
                }
            }
        }
        for (UUID id : Set.copyOf(AFTERMATH.keySet()))
        {
            AftermathState state = AFTERMATH.remove(id);
            if (state == null)
            {
                continue;
            }
            LivingEntity living = findLiving(server, id);
            if (living != null)
            {
                releaseFreeze(living, toFreeze(state));
            }
            else if (state.inputLocked() && server != null)
            {
                ServerPlayer online = server.getPlayerList().getPlayer(id);
                if (online != null)
                {
                    ModNetwork.sendPlayerInputLockSync(online, false);
                }
            }
        }
    }

    private static void promoteAftermathIntoActiveIfPresent(LivingEntity living)
    {
        AftermathState aftermath = AFTERMATH.remove(living.getUUID());
        if (aftermath == null)
        {
            return;
        }
        ACTIVE.put(living.getUUID(), new FreezeState(
                aftermath.aiPausedByUs(),
                aftermath.originNoAi(),
                aftermath.inputLocked(),
                0));
    }

    private static void applyActive(LivingEntity living)
    {
        putMultiplyToZero(living, Attributes.MOVEMENT_SPEED, SPEED_MODIFIER_UUID, SPEED_MODIFIER_NAME);
        putMultiplyToZero(living, Attributes.ATTACK_SPEED, ATTACK_SPEED_MODIFIER_UUID, ATTACK_SPEED_MODIFIER_NAME);

        FreezeState prev = ACTIVE.get(living.getUUID());
        boolean aiPausedByUs = prev != null && prev.aiPausedByUs();
        boolean originNoAi = prev != null && prev.originNoAi();
        boolean inputLocked = prev != null && prev.inputLocked();
        int elapsed = prev != null ? prev.elapsedTicks() : 0;

        if (living instanceof Mob mob)
        {
            if (prev == null)
            {
                originNoAi = mob.isNoAi();
                if (!originNoAi)
                {
                    mob.setNoAi(true);
                    aiPausedByUs = true;
                }
            }
            else if (aiPausedByUs)
            {
                mob.setNoAi(true);
            }
        }

        if (living instanceof ServerPlayer serverPlayer && !inputLocked)
        {
            ModNetwork.sendPlayerInputLockSync(serverPlayer, true);
            inputLocked = true;
        }

        maintainFreezePose(living, aiPausedByUs);
        ACTIVE.put(living.getUUID(), new FreezeState(aiPausedByUs, originNoAi, inputLocked, elapsed + 1));
    }

    private static void maintainFreezePose(LivingEntity living, boolean keepNoAi)
    {
        if (keepNoAi && living instanceof Mob mob)
        {
            mob.setNoAi(true);
        }
        if (living instanceof ServerPlayer serverPlayer)
        {
            serverPlayer.setDeltaMovement(Vec3.ZERO);
            serverPlayer.hurtMarked = true;
            serverPlayer.connection.teleport(
                    serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                    serverPlayer.getYRot(), serverPlayer.getXRot());
        }
        else
        {
            living.setDeltaMovement(Vec3.ZERO);
            living.hurtMarked = true;
            living.teleportTo(living.getX(), living.getY(), living.getZ());
        }
    }

    private static void stripActive(LivingEntity living)
    {
        FreezeState state = ACTIVE.remove(living.getUUID());
        if (state != null)
        {
            releaseFreeze(living, state);
        }
    }

    private static void releaseFreeze(LivingEntity living, FreezeState state)
    {
        removeModifier(living, Attributes.MOVEMENT_SPEED, SPEED_MODIFIER_UUID);
        removeModifier(living, Attributes.ATTACK_SPEED, ATTACK_SPEED_MODIFIER_UUID);
        if (living instanceof Mob mob && state.aiPausedByUs())
        {
            mob.setNoAi(state.originNoAi());
        }
        if (state.inputLocked() && living instanceof ServerPlayer serverPlayer)
        {
            ModNetwork.sendPlayerInputLockSync(serverPlayer, false);
        }
    }

    private static void releaseOrphanActive(UUID id, ServerLevel level)
    {
        FreezeState state = ACTIVE.remove(id);
        if (state != null)
        {
            unlockInputOrphan(id, level, state);
        }
    }

    private static void unlockInputOrphan(UUID id, @Nullable ServerLevel level, FreezeState state)
    {
        if (!state.inputLocked())
        {
            return;
        }
        MinecraftServer server = level != null ? level.getServer() : null;
        if (server == null)
        {
            return;
        }
        ServerPlayer online = server.getPlayerList().getPlayer(id);
        if (online != null)
        {
            ModNetwork.sendPlayerInputLockSync(online, false);
        }
    }

    @Nullable
    private static LivingEntity findLiving(MinecraftServer server, UUID id)
    {
        if (server == null)
        {
            return null;
        }
        for (ServerLevel level : server.getAllLevels())
        {
            if (level.getEntity(id) instanceof LivingEntity living)
            {
                return living;
            }
        }
        return null;
    }

    private static FreezeState toFreeze(AftermathState state)
    {
        return new FreezeState(state.aiPausedByUs(), state.originNoAi(), state.inputLocked(), 0);
    }

    private static void putMultiplyToZero(
            LivingEntity living, Attribute attribute, UUID uuid, String name)
    {
        AttributeInstance instance = living.getAttribute(attribute);
        if (instance == null)
        {
            return;
        }
        instance.removeModifier(uuid);
        instance.addTransientModifier(new AttributeModifier(
                uuid, name, MULTIPLY_TO_ZERO, AttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    private static void removeModifier(LivingEntity living, Attribute attribute, UUID uuid)
    {
        AttributeInstance instance = living.getAttribute(attribute);
        if (instance != null)
        {
            instance.removeModifier(uuid);
        }
    }

    private static boolean isControlled(UUID id)
    {
        return ACTIVE.containsKey(id) || AFTERMATH.containsKey(id);
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event)
    {
        LivingEntity living = event.getEntity();
        if (living.level().isClientSide)
        {
            return;
        }
        UUID id = living.getUUID();

        AftermathState aftermath = AFTERMATH.get(id);
        if (aftermath != null)
        {
            int remaining = aftermath.remainingTicks() - 1;
            maintainFreezePose(living, aftermath.aiPausedByUs());
            putMultiplyToZero(living, Attributes.MOVEMENT_SPEED, SPEED_MODIFIER_UUID, SPEED_MODIFIER_NAME);
            putMultiplyToZero(living, Attributes.ATTACK_SPEED, ATTACK_SPEED_MODIFIER_UUID, ATTACK_SPEED_MODIFIER_NAME);
            if (remaining <= 0)
            {
                AFTERMATH.remove(id);
                releaseFreeze(living, toFreeze(aftermath));
            }
            else
            {
                AFTERMATH.put(id, aftermath.withRemainingTicks(remaining));
            }
            return;
        }

        FreezeState active = ACTIVE.get(id);
        if (active == null)
        {
            return;
        }
        living.setDeltaMovement(Vec3.ZERO);
        living.hurtMarked = true;
        if (active.aiPausedByUs() && living instanceof Mob mob)
        {
            mob.setNoAi(true);
        }
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event)
    {
        if (isControlled(event.getEntity().getUUID()))
        {
            event.setNewSpeed(0.0F);
        }
    }

    @SubscribeEvent
    public static void onJump(LivingEvent.LivingJumpEvent event)
    {
        LivingEntity living = event.getEntity();
        if (!isControlled(living.getUUID()))
        {
            return;
        }
        living.setDeltaMovement(Vec3.ZERO);
        living.hurtMarked = true;
    }

    private record FreezeState(boolean aiPausedByUs, boolean originNoAi, boolean inputLocked, int elapsedTicks)
    {
    }

    private record AftermathState(boolean aiPausedByUs, boolean originNoAi, boolean inputLocked, int remainingTicks)
    {
        AftermathState withRemainingTicks(int ticks)
        {
            return new AftermathState(aiPausedByUs, originNoAi, inputLocked, ticks);
        }
    }
}
