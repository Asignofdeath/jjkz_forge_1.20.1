package com.darkness.jujutsukaisenzone.faction;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 咒灵 / 诅咒师遇到咒术师时的临时合作。
 */
public final class FactionTempCoop
{
    public static final double CHANCE = 0.30D;
    public static final int DURATION_TICKS = 20 * 20;
    public static final double RADIUS = 32.0D;
    public static final int COOLDOWN_TICKS = 20 * 60;

    private static final Map<String, Long> COOLDOWN_UNTIL = new HashMap<>();
    private static final List<Session> SESSIONS = new ArrayList<>();

    private FactionTempCoop()
    {
    }

    public static void tryRoll(LivingEntity roller, LivingEntity sorcerer)
    {
        if (roller == null || sorcerer == null || roller.level().isClientSide)
        {
            return;
        }
        String rollerFaction = JjkzFactions.resolve(roller);
        if (!JjkzFactions.CURSE_SPIRIT.equals(rollerFaction)
                && !JjkzFactions.CURSE_USER.equals(rollerFaction))
        {
            return;
        }
        if (!JjkzFactions.SORCERER.equals(JjkzFactions.resolve(sorcerer)))
        {
            return;
        }
        long now = roller.level().getGameTime();
        purge(now);
        String key = roller.getUUID() + "|" + sorcerer.getUUID();
        Long readyAt = COOLDOWN_UNTIL.get(key);
        if (readyAt != null && now < readyAt)
        {
            return;
        }
        COOLDOWN_UNTIL.put(key, now + COOLDOWN_TICKS);
        if (roller.getRandom().nextDouble() >= CHANCE)
        {
            return;
        }
        SESSIONS.add(new Session(sorcerer.getUUID(), now + DURATION_TICKS));
    }

    public static boolean isCooperating(LivingEntity a, LivingEntity b)
    {
        if (a == null || b == null || a.level().isClientSide || a.level() != b.level())
        {
            return false;
        }
        String fa = JjkzFactions.resolve(a);
        String fb = JjkzFactions.resolve(b);
        boolean mixed = (JjkzFactions.CURSE_SPIRIT.equals(fa) && JjkzFactions.CURSE_USER.equals(fb))
                || (JjkzFactions.CURSE_USER.equals(fa) && JjkzFactions.CURSE_SPIRIT.equals(fb));
        if (!mixed)
        {
            return false;
        }
        long now = a.level().getGameTime();
        purge(now);
        if (!(a.level() instanceof ServerLevel level))
        {
            return false;
        }
        for (Session session : SESSIONS)
        {
            LivingEntity sorcerer = find(level, session.sorcererId);
            if (sorcerer == null || !sorcerer.isAlive())
            {
                continue;
            }
            if (a.distanceTo(sorcerer) <= RADIUS && b.distanceTo(sorcerer) <= RADIUS)
            {
                return true;
            }
        }
        return false;
    }

    private static LivingEntity find(ServerLevel level, UUID id)
    {
        Player player = level.getPlayerByUUID(id);
        if (player != null)
        {
            return player;
        }
        var entity = level.getEntity(id);
        return entity instanceof LivingEntity living ? living : null;
    }

    private static void purge(long now)
    {
        Iterator<Session> it = SESSIONS.iterator();
        while (it.hasNext())
        {
            if (now >= it.next().expireTick)
            {
                it.remove();
            }
        }
        COOLDOWN_UNTIL.entrySet().removeIf(entry -> now >= entry.getValue());
    }

    private record Session(UUID sorcererId, long expireTick)
    {
    }
}
