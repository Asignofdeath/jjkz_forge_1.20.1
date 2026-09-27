package com.darkness.jujutsukaisenzone.technique.se;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * 「收刀」开刀印：按攻击者 → 目标记过期时刻（游戏刻）。
 * <p>
 * 仅服务端内存；不落盘。卸下 SE 后不再写入，已有印记自然过期。
 */
public final class ShrineFinishCutMarks
{
    /** ownerUuid → (targetUuid → expireGameTime) */
    private static final Map<UUID, Map<UUID, Long>> MARKS = new HashMap<>();

    private ShrineFinishCutMarks()
    {
    }

    /**
     * 解命中成功后尝试打标：仅当 owner 为已装载 {@link ShrineFinishCutSeMechanic} 的玩家。
     */
    public static void tryMarkFromKaiHit(LivingEntity owner, LivingEntity target)
    {
        if (!(owner instanceof ServerPlayer player) || target == null || !target.isAlive())
        {
            return;
        }
        if (target == player)
        {
            return;
        }
        var mechanic = SeMechanicRegistry.findEquipped(player, ShrineFinishCutSeMechanic.class);
        if (mechanic.isEmpty())
        {
            return;
        }
        long expireAt = player.level().getGameTime() + Math.max(1, mechanic.get().markDurationTicks());
        MARKS.computeIfAbsent(player.getUUID(), id -> new HashMap<>())
                .put(target.getUUID(), expireAt);
        pruneOwner(player.getUUID(), player.level().getGameTime());
    }

    public static boolean hasMark(ServerPlayer owner, LivingEntity target)
    {
        if (owner == null || target == null)
        {
            return false;
        }
        Map<UUID, Long> byTarget = MARKS.get(owner.getUUID());
        if (byTarget == null)
        {
            return false;
        }
        Long expireAt = byTarget.get(target.getUUID());
        if (expireAt == null)
        {
            return false;
        }
        long now = owner.level().getGameTime();
        if (now >= expireAt)
        {
            byTarget.remove(target.getUUID());
            if (byTarget.isEmpty())
            {
                MARKS.remove(owner.getUUID());
            }
            return false;
        }
        return true;
    }

    /** 捌成功挂上后消耗印记（无印则无操作）。 */
    public static void consume(ServerPlayer owner, LivingEntity target)
    {
        if (owner == null || target == null)
        {
            return;
        }
        Map<UUID, Long> byTarget = MARKS.get(owner.getUUID());
        if (byTarget == null)
        {
            return;
        }
        byTarget.remove(target.getUUID());
        if (byTarget.isEmpty())
        {
            MARKS.remove(owner.getUUID());
        }
    }

    private static void pruneOwner(UUID ownerId, long now)
    {
        Map<UUID, Long> byTarget = MARKS.get(ownerId);
        if (byTarget == null)
        {
            return;
        }
        Iterator<Map.Entry<UUID, Long>> it = byTarget.entrySet().iterator();
        while (it.hasNext())
        {
            if (now >= it.next().getValue())
            {
                it.remove();
            }
        }
        if (byTarget.isEmpty())
        {
            MARKS.remove(ownerId);
        }
    }
}
