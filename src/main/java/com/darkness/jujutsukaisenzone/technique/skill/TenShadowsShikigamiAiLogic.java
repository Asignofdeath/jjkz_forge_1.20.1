package com.darkness.jujutsukaisenzone.technique.skill;

import com.darkness.jujutsukaisenzone.player.PlayerTenShadowsRosterData;
import com.darkness.jujutsukaisenzone.player.PlayerTenShadowsStanceData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 已召出友方式神的跟随 / 协同 / 护卫行为。
 */
public final class TenShadowsShikigamiAiLogic
{
    public static final String TAG_ORDERED_TARGET = "jujutsukaisenzone_ten_shadows_ordered_target";

    /** 超过此距离开始走向主人。 */
    private static final double FOLLOW_START = 5.0D;
    /** 超过此距离传送回主人附近。 */
    private static final double TELEPORT_DISTANCE = 18.0D;
    /** 护卫环绕半径。 */
    private static final double GUARD_ORBIT = 4.5D;
    /** 护卫警戒半径（相对主人）。 */
    private static final double GUARD_ALERT = 8.0D;
    private static final double GUARD_ALERT_SQR = GUARD_ALERT * GUARD_ALERT;

    private TenShadowsShikigamiAiLogic()
    {
    }

    public static void tickOwner(ServerPlayer owner)
    {
        if (owner == null || !owner.isAlive())
        {
            return;
        }
        ServerLevel level = owner.serverLevel();
        Map<Integer, List<UUID>> roster = PlayerTenShadowsRosterData.snapshot(owner);
        if (roster.isEmpty())
        {
            return;
        }
        boolean guard = PlayerTenShadowsStanceData.isGuard(owner);
        int slot = 0;
        int total = 0;
        for (List<UUID> ids : roster.values())
        {
            total += ids.size();
        }
        for (Map.Entry<Integer, List<UUID>> entry : roster.entrySet())
        {
            for (UUID id : entry.getValue())
            {
                Entity entity = level.getEntity(id);
                if (!(entity instanceof Mob mob) || !mob.isAlive())
                {
                    continue;
                }
                if (!TenShadowsSummonLogic.isOwnedBy(mob, owner.getUUID()))
                {
                    continue;
                }
                tickOne(owner, mob, guard, slot, Math.max(1, total));
                slot++;
            }
        }
    }

    private static void tickOne(ServerPlayer owner, Mob mob, boolean guard, int slotIndex, int totalCount)
    {
        if (mob instanceof Wolf wolf)
        {
            wolf.setOrderedToSit(false);
        }

        double distSqr = mob.distanceToSqr(owner);
        if (distSqr > TELEPORT_DISTANCE * TELEPORT_DISTANCE)
        {
            teleportNearOwner(owner, mob, slotIndex, totalCount, guard);
        }

        if (guard)
        {
            tickGuard(owner, mob, slotIndex, totalCount);
        }
        else
        {
            tickAssist(owner, mob, distSqr);
        }
    }

    private static void tickAssist(ServerPlayer owner, Mob mob, double distSqr)
    {
        LivingEntity ordered = resolveOrderedTarget(owner, mob);
        if (ordered != null)
        {
            setAttackTarget(mob, ordered);
        }
        else
        {
            LivingEntity current = mob.getTarget();
            if (current != null && (!current.isAlive() || isFriendly(owner, current)))
            {
                clearTarget(mob);
                clearOrdered(mob);
            }
        }

        LivingEntity target = mob.getTarget();
        if (target != null && target.isAlive())
        {
            mob.getNavigation().moveTo(target, 1.15D);
            return;
        }
        if (distSqr > FOLLOW_START * FOLLOW_START)
        {
            mob.getNavigation().moveTo(owner, 1.1D);
        }
    }

    private static void tickGuard(ServerPlayer owner, Mob mob, int slotIndex, int totalCount)
    {
        clearOrdered(mob);
        LivingEntity current = mob.getTarget();
        if (current != null)
        {
            if (!current.isAlive()
                    || isFriendly(owner, current)
                    || current.distanceToSqr(owner) > GUARD_ALERT_SQR)
            {
                clearTarget(mob);
                current = null;
            }
        }
        if (current == null)
        {
            LivingEntity alert = findAlertTarget(owner, mob);
            if (alert != null)
            {
                setAttackTarget(mob, alert);
                current = alert;
            }
        }

        if (current != null)
        {
            mob.getNavigation().moveTo(current, 1.2D);
            return;
        }

        Vec3 orbit = orbitPoint(owner, slotIndex, totalCount);
        if (mob.distanceToSqr(orbit) > 2.25D)
        {
            mob.getNavigation().moveTo(orbit.x, orbit.y, orbit.z, 1.05D);
        }
    }

    private static LivingEntity findAlertTarget(ServerPlayer owner, Mob mob)
    {
        List<LivingEntity> nearby = owner.level().getEntitiesOfClass(
                LivingEntity.class,
                owner.getBoundingBox().inflate(GUARD_ALERT),
                candidate -> candidate.isAlive()
                        && candidate != owner
                        && candidate != mob
                        && !isFriendly(owner, candidate)
                        && candidate.distanceToSqr(owner) <= GUARD_ALERT_SQR
                        && isThreat(owner, candidate)
                        && mob.hasLineOfSight(candidate));
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity candidate : nearby)
        {
            double d = candidate.distanceToSqr(owner);
            if (d < bestDist)
            {
                bestDist = d;
                best = candidate;
            }
        }
        return best;
    }

    private static Vec3 orbitPoint(ServerPlayer owner, int slotIndex, int totalCount)
    {
        double angle = (Math.PI * 2.0D * slotIndex) / totalCount + owner.tickCount * 0.02D;
        double x = owner.getX() + Math.cos(angle) * GUARD_ORBIT;
        double z = owner.getZ() + Math.sin(angle) * GUARD_ORBIT;
        return new Vec3(x, owner.getY(), z);
    }

    private static void teleportNearOwner(
            ServerPlayer owner, Mob mob, int slotIndex, int totalCount, boolean guard)
    {
        Vec3 dest = guard
                ? orbitPoint(owner, slotIndex, totalCount)
                : owner.position().add(
                (owner.getRandom().nextDouble() - 0.5D) * 2.0D,
                0.0D,
                (owner.getRandom().nextDouble() - 0.5D) * 2.0D);
        mob.teleportTo(dest.x, dest.y, dest.z);
        mob.getNavigation().stop();
    }

    public static int orderAttack(ServerPlayer owner, LivingEntity target)
    {
        ServerLevel level = owner.serverLevel();
        int count = 0;
        for (List<UUID> ids : PlayerTenShadowsRosterData.snapshot(owner).values())
        {
            for (UUID id : ids)
            {
                Entity entity = level.getEntity(id);
                if (!(entity instanceof Mob mob) || !mob.isAlive())
                {
                    continue;
                }
                if (!TenShadowsSummonLogic.isOwnedBy(mob, owner.getUUID()))
                {
                    continue;
                }
                mob.getPersistentData().putUUID(TAG_ORDERED_TARGET, target.getUUID());
                setAttackTarget(mob, target);
                count++;
            }
        }
        return count;
    }

    public static void clearOrderedTargets(ServerPlayer owner)
    {
        ServerLevel level = owner.serverLevel();
        for (List<UUID> ids : PlayerTenShadowsRosterData.snapshot(owner).values())
        {
            for (UUID id : ids)
            {
                Entity entity = level.getEntity(id);
                if (entity instanceof Mob mob)
                {
                    clearOrdered(mob);
                    clearTarget(mob);
                }
            }
        }
    }

    public static void onOwnerHurt(ServerPlayer owner, LivingEntity attacker)
    {
        if (attacker == null || !attacker.isAlive() || isFriendly(owner, attacker))
        {
            return;
        }
        if (!PlayerTenShadowsStanceData.isAssist(owner))
        {
            return;
        }
        ServerLevel level = owner.serverLevel();
        for (List<UUID> ids : PlayerTenShadowsRosterData.snapshot(owner).values())
        {
            for (UUID id : ids)
            {
                Entity entity = level.getEntity(id);
                if (entity instanceof Mob mob && mob.isAlive() && TenShadowsSummonLogic.isOwnedBy(mob, owner.getUUID()))
                {
                    if (mob.getTarget() == null)
                    {
                        setAttackTarget(mob, attacker);
                    }
                }
            }
        }
    }

    public static void onOwnerAttack(ServerPlayer owner, LivingEntity victim)
    {
        if (victim == null || !victim.isAlive() || isFriendly(owner, victim))
        {
            return;
        }
        if (!PlayerTenShadowsStanceData.isAssist(owner))
        {
            return;
        }
        ServerLevel level = owner.serverLevel();
        for (List<UUID> ids : PlayerTenShadowsRosterData.snapshot(owner).values())
        {
            for (UUID id : ids)
            {
                Entity entity = level.getEntity(id);
                if (entity instanceof Mob mob && mob.isAlive() && TenShadowsSummonLogic.isOwnedBy(mob, owner.getUUID()))
                {
                    setAttackTarget(mob, victim);
                }
            }
        }
    }

    private static LivingEntity resolveOrderedTarget(ServerPlayer owner, Mob mob)
    {
        if (!mob.getPersistentData().hasUUID(TAG_ORDERED_TARGET))
        {
            return null;
        }
        UUID targetId = mob.getPersistentData().getUUID(TAG_ORDERED_TARGET);
        Entity entity = owner.serverLevel().getEntity(targetId);
        if (!(entity instanceof LivingEntity living) || !living.isAlive() || isFriendly(owner, living))
        {
            clearOrdered(mob);
            return null;
        }
        return living;
    }

    private static boolean isFriendly(ServerPlayer owner, LivingEntity other)
    {
        if (other.getUUID().equals(owner.getUUID()))
        {
            return true;
        }
        return TenShadowsSummonLogic.isOwnedBy(other, owner.getUUID());
    }

    /** 护卫警戒：玩家、敌对生物，或正在以主人为目标的生物。 */
    private static boolean isThreat(ServerPlayer owner, LivingEntity candidate)
    {
        if (candidate instanceof net.minecraft.world.entity.player.Player)
        {
            return true;
        }
        if (candidate instanceof net.minecraft.world.entity.monster.Enemy)
        {
            return true;
        }
        return candidate instanceof Mob m && m.getTarget() == owner;
    }

    private static void setAttackTarget(Mob mob, LivingEntity target)
    {
        mob.setTarget(target);
        if (mob instanceof Warden warden)
        {
            warden.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, target);
        }
        if (mob instanceof Wolf wolf)
        {
            wolf.setTarget(target);
        }
    }

    private static void clearTarget(Mob mob)
    {
        mob.setTarget(null);
        if (mob instanceof Warden warden)
        {
            warden.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
            warden.getBrain().eraseMemory(MemoryModuleType.ROAR_TARGET);
        }
    }

    private static void clearOrdered(Mob mob)
    {
        mob.getPersistentData().remove(TAG_ORDERED_TARGET);
    }

    /** 名册里所有存活式神。 */
    public static List<Mob> listAliveSummons(ServerPlayer owner)
    {
        List<Mob> list = new ArrayList<>();
        ServerLevel level = owner.serverLevel();
        for (List<UUID> ids : PlayerTenShadowsRosterData.snapshot(owner).values())
        {
            for (UUID id : ids)
            {
                Entity entity = level.getEntity(id);
                if (entity instanceof Mob mob && mob.isAlive() && TenShadowsSummonLogic.isOwnedBy(mob, owner.getUUID()))
                {
                    list.add(mob);
                }
            }
        }
        return list;
    }
}
