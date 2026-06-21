package com.darkness.jujutsukaisenzone.technique.domain;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.compat.EpicFightCompat;
import com.darkness.jujutsukaisenzone.entity.SpecialEffectEntity;
import com.darkness.jujutsukaisenzone.player.PlayerDomainExpansionData;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 「生得领域」演出：10 秒内展示领域球与天空盒，结束后解锁领域展开。
 */
public final class InnateDomainExperienceManager
{
    public static final int SESSION_DURATION_TICKS = 200;
    public static final int COLLECT_RADIUS = 24;
    public static final int DOMAIN_RADIUS = AbstractDomainExpansion.DOMAIN_RADIUS;

    private static final int SLOWNESS_AMPLIFIER = 5;
    private static final ResourceLocation LIMITLESS_INNATE_SKYBOX =
            ResourceLocation.fromNamespaceAndPath(JuJutsuKaisenZone.MODID, "textures/environment/stat_sky2");
    private static final ResourceLocation SHRINE_INNATE_SKYBOX =
            ResourceLocation.fromNamespaceAndPath(JuJutsuKaisenZone.MODID, "textures/environment/shrine_cubemap");

    private static final ConcurrentMap<UUID, InnateDomainSession> SESSIONS = new ConcurrentHashMap<>();

    private InnateDomainExperienceManager()
    {
    }

    public static boolean isSessionActive(UUID playerId)
    {
        return SESSIONS.containsKey(playerId);
    }

    public static void start(ServerPlayer player)
    {
        if (!player.isAlive() || isSessionActive(player.getUUID()))
        {
            return;
        }
        ServerLevel level = player.serverLevel();
        BlockPos origin = player.blockPosition();
        BlockPos center = new BlockPos(
                origin.getX(),
                level.getMaxBuildHeight() - 1 - DOMAIN_RADIUS,
                origin.getZ());
        ResourceLocation skybox = resolveSkyboxTexture(player);
        if (skybox == null)
        {
            return;
        }

        AbstractDomainExpansion domain = DomainExpansionRouter.getDomainForPlayer(player);
        AbstractDomainExpansion.InnateArenaHandle arena = domain.buildInnateDomainArena(
                player, level, center, DOMAIN_RADIUS, skybox);
        if (arena == null)
        {
            return;
        }

        Map<UUID, EntityRestore> restores = collectAndTeleportParticipants(player, level, origin, center);
        InnateDomainSession session = new InnateDomainSession(
                arena,
                domain,
                level.dimension(),
                restores,
                EpicFightCompat.isBattleMode(player),
                SESSION_DURATION_TICKS);
        SESSIONS.put(player.getUUID(), session);
        PlayerDomainExpansionData.setInnateDomainSessionActive(player, true);
        applySessionEffects(player, session);
        for (EntityRestore restore : restores.values())
        {
            Entity entity = level.getEntity(restore.entityId);
            if (entity instanceof LivingEntity living)
            {
                applyLivingSessionEffects(living);
            }
        }
        EpicFightCompat.setBattleMode(player, false);
    }

    public static void tick(ServerPlayer player)
    {
        InnateDomainSession session = SESSIONS.get(player.getUUID());
        if (session == null)
        {
            return;
        }
        if (!player.isAlive())
        {
            endSession(player, session, false);
            return;
        }
        applySessionEffects(player, session);
        session.remainingTicks--;
        if (session.remainingTicks <= 0)
        {
            endSession(player, session, true);
        }
    }

    public static void abortIfActive(ServerPlayer player)
    {
        InnateDomainSession session = SESSIONS.remove(player.getUUID());
        if (session != null)
        {
            endSession(player, session, false);
        }
    }

    private static void endSession(ServerPlayer player, InnateDomainSession session, boolean unlockLearned)
    {
        SESSIONS.remove(player.getUUID());
        PlayerDomainExpansionData.setInnateDomainSessionActive(player, false);

        ServerLevel level = player.server.getLevel(session.dimension);
        if (level != null)
        {
            restoreParticipants(level, session);
            session.domain.dismantleInnateDomainArena(player, session.arena);
        }

        EpicFightCompat.setBattleMode(player, session.wasEpicFightMode);
        player.setInvulnerable(false);
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);

        if (unlockLearned)
        {
            PlayerDomainExpansionData.setLearned(player, true);
            player.sendSystemMessage(Component.translatable("message.jujutsukaisenzone.domain_expansion.learned"));
        }
    }

    private static void applySessionEffects(ServerPlayer player, InnateDomainSession session)
    {
        player.setInvulnerable(true);
        player.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN,
                40,
                SLOWNESS_AMPLIFIER,
                false,
                false,
                false));
    }

    private static void applyLivingSessionEffects(LivingEntity living)
    {
        living.setInvulnerable(true);
        living.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN,
                40,
                SLOWNESS_AMPLIFIER,
                false,
                false,
                false));
    }

    private static Map<UUID, EntityRestore> collectAndTeleportParticipants(
            ServerPlayer player,
            ServerLevel level,
            BlockPos origin,
            BlockPos center)
    {
        Map<UUID, EntityRestore> restores = new HashMap<>();
        double collectRadius = COLLECT_RADIUS;
        AABB query = new AABB(
                origin.getX() - collectRadius,
                origin.getY() - collectRadius,
                origin.getZ() - collectRadius,
                origin.getX() + collectRadius + 1.0D,
                origin.getY() + collectRadius + 1.0D,
                origin.getZ() + collectRadius + 1.0D);

        List<LivingEntity> participants = new ArrayList<>();
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, query, candidate ->
        {
            if (!candidate.isAlive() || candidate instanceof SpecialEffectEntity)
            {
                return false;
            }
            return isWithinHorizontalRadius(candidate.position(), origin, collectRadius);
        }))
        {
            participants.add(entity);
        }

        Vec3 anchor = domainStandPosition(center, 0);
        int scatterIndex = 0;
        for (LivingEntity entity : participants)
        {
            Vec3 restorePos = entity.position();
            restores.put(entity.getUUID(), new EntityRestore(
                    entity.getId(),
                    restorePos,
                    entity.getYRot(),
                    entity.getXRot(),
                    entity.isInvulnerable(),
                    entity.level().dimension()));

            Vec3 target = scatterIndex == 0
                    ? anchor
                    : domainStandPosition(center, scatterIndex);
            scatterIndex++;
            teleportLiving(entity, target);
        }
        return restores;
    }

    private static void restoreParticipants(ServerLevel level, InnateDomainSession session)
    {
        for (EntityRestore restore : session.restores.values())
        {
            Entity entity = level.getEntity(restore.entityId);
            if (entity == null)
            {
                continue;
            }
            if (!entity.level().dimension().equals(restore.dimension))
            {
                continue;
            }
            entity.teleportTo(restore.position.x, restore.position.y, restore.position.z);
            entity.setYRot(restore.yRot);
            entity.setXRot(restore.xRot);
            entity.setInvulnerable(restore.wasInvulnerable);
            if (entity instanceof LivingEntity living)
            {
                living.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            }
        }
    }

    private static int innateDomainFloorY(BlockPos center)
    {
        return center.getY() - DOMAIN_RADIUS + 1;
    }

    private static Vec3 domainStandPosition(BlockPos center, int scatterIndex)
    {
        double baseY = innateDomainFloorY(center) + 1.0D;
        double baseX = center.getX() + 0.5D;
        double baseZ = center.getZ() + 0.5D;
        if (scatterIndex <= 0)
        {
            return new Vec3(baseX, baseY, baseZ);
        }
        double angle = scatterIndex * 1.7D;
        double radius = 1.5D + (scatterIndex % 4) * 1.25D;
        return new Vec3(baseX + Math.cos(angle) * radius, baseY, baseZ + Math.sin(angle) * radius);
    }

    private static void teleportLiving(LivingEntity entity, Vec3 target)
    {
        entity.teleportTo(target.x, target.y, target.z);
        if (entity instanceof ServerPlayer serverPlayer)
        {
            serverPlayer.connection.teleport(target.x, target.y, target.z, entity.getYRot(), entity.getXRot());
        }
    }

    private static boolean isWithinHorizontalRadius(Vec3 pos, BlockPos origin, double radius)
    {
        double dx = pos.x - (origin.getX() + 0.5D);
        double dz = pos.z - (origin.getZ() + 0.5D);
        return dx * dx + dz * dz <= radius * radius
                && pos.y >= origin.getY() - radius
                && pos.y <= origin.getY() + radius + 1.0D;
    }

    private static ResourceLocation resolveSkyboxTexture(ServerPlayer player)
    {
        if (PlayerTechniqueData.isLimitless(player))
        {
            return LIMITLESS_INNATE_SKYBOX;
        }
        if (PlayerTechniqueData.isShrine(player))
        {
            return SHRINE_INNATE_SKYBOX;
        }
        return null;
    }

    private static final class InnateDomainSession
    {
        private final AbstractDomainExpansion.InnateArenaHandle arena;
        private final AbstractDomainExpansion domain;
        private final ResourceKey<Level> dimension;
        private final Map<UUID, EntityRestore> restores;
        private final boolean wasEpicFightMode;
        private int remainingTicks;

        private InnateDomainSession(
                AbstractDomainExpansion.InnateArenaHandle arena,
                AbstractDomainExpansion domain,
                ResourceKey<Level> dimension,
                Map<UUID, EntityRestore> restores,
                boolean wasEpicFightMode,
                int remainingTicks)
        {
            this.arena = arena;
            this.domain = domain;
            this.dimension = dimension;
            this.restores = restores;
            this.wasEpicFightMode = wasEpicFightMode;
            this.remainingTicks = remainingTicks;
        }
    }

    private static final class EntityRestore
    {
        private final int entityId;
        private final Vec3 position;
        private final float yRot;
        private final float xRot;
        private final boolean wasInvulnerable;
        private final ResourceKey<Level> dimension;

        private EntityRestore(
                int entityId,
                Vec3 position,
                float yRot,
                float xRot,
                boolean wasInvulnerable,
                ResourceKey<Level> dimension)
        {
            this.entityId = entityId;
            this.position = position;
            this.yRot = yRot;
            this.xRot = xRot;
            this.wasInvulnerable = wasInvulnerable;
            this.dimension = dimension;
        }
    }
}
