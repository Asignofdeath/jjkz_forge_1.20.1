package com.darkness.jujutsukaisenzone.technique.skill;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 一场调伏仪式的会话态（不写 NBT）。
 */
public final class TenShadowsRitualSession
{
    public enum Mode
    {
        SOLO,
        MULTI
    }

    private final UUID casterId;
    private final TenShadowsShikigami shikigami;
    private final Mode mode;
    private final ResourceKey<Level> dimension;
    private final Set<UUID> participantIds = new HashSet<>();
    private final Set<UUID> falseDeadIds = new HashSet<>();
    private final Set<UUID> ritualEntityIds = new HashSet<>();
    private final TenShadowsRitualChunkForce.Tracker chunkTracker = TenShadowsRitualChunkForce.create();
    @Nullable
    private UUID priorityTargetId;
    /** 仪式式神上次对任意实体造成伤害时的 level.getGameTime()。 */
    private long lastDamageGameTime;
    private boolean closed;

    public TenShadowsRitualSession(
            UUID casterId,
            TenShadowsShikigami shikigami,
            Mode mode,
            List<UUID> participants,
            ResourceKey<Level> dimension)
    {
        this.casterId = casterId;
        this.shikigami = shikigami;
        this.mode = mode;
        this.dimension = dimension;
        this.participantIds.addAll(participants);
    }

    public UUID casterId()
    {
        return casterId;
    }

    public TenShadowsShikigami shikigami()
    {
        return shikigami;
    }

    public Mode mode()
    {
        return mode;
    }

    public ResourceKey<Level> dimension()
    {
        return dimension;
    }

    public boolean isMulti()
    {
        return mode == Mode.MULTI;
    }

    public boolean isClosed()
    {
        return closed;
    }

    public void markClosed()
    {
        this.closed = true;
    }

    public Set<UUID> participantIds()
    {
        return participantIds;
    }

    public Set<UUID> falseDeadIds()
    {
        return falseDeadIds;
    }

    public Set<UUID> ritualEntityIds()
    {
        return ritualEntityIds;
    }

    public TenShadowsRitualChunkForce.Tracker chunkTracker()
    {
        return chunkTracker;
    }

    @Nullable
    public UUID priorityTargetId()
    {
        return priorityTargetId;
    }

    public void setPriorityTargetId(@Nullable UUID priorityTargetId)
    {
        this.priorityTargetId = priorityTargetId;
    }

    public long lastDamageGameTime()
    {
        return lastDamageGameTime;
    }

    public void setLastDamageGameTime(long lastDamageGameTime)
    {
        this.lastDamageGameTime = lastDamageGameTime;
    }

    public void markDamageDealt(long gameTime)
    {
        this.lastDamageGameTime = gameTime;
    }

    public boolean isParticipant(UUID playerId)
    {
        return participantIds.contains(playerId);
    }

    public boolean isFalseDead(UUID playerId)
    {
        return falseDeadIds.contains(playerId);
    }

    public void addRitualEntity(UUID entityId)
    {
        ritualEntityIds.add(entityId);
    }

    public boolean isRitualEntity(UUID entityId)
    {
        return ritualEntityIds.contains(entityId);
    }

    public boolean allParticipantsFalseDead()
    {
        return falseDeadIds.containsAll(participantIds);
    }
}
