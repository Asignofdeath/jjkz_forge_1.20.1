package com.darkness.jujutsukaisenzone.technique.skill;

import com.darkness.jujutsukaisenzone.entity.JadeDogEntity;
import com.darkness.jujutsukaisenzone.player.PlayerTenShadowsRosterData;
import com.darkness.jujutsukaisenzone.player.PlayerTenShadowsStanceData;
import com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability.GreatSerpentAmbushLogic;
import com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability.PiercingOxChargeLogic;
import com.darkness.jujutsukaisenzone.entity.MahoragaEntity;
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
    /** 式神自身受击后的反击目标（高优先）。 */
    public static final String TAG_RETALIATE_TARGET = "jujutsukaisenzone_ten_shadows_retaliate_target";

    /** 超过此距离开始走向主人。 */
    private static final double FOLLOW_START = 5.0D;
    /** 魔虚罗：更远才跟，少瞎走好播 idle。 */
    private static final double MAHORAGA_FOLLOW_START = 12.0D;
    /** 魔虚罗护卫环绕：偏离这么远才补位。 */
    private static final double MAHORAGA_ORBIT_TOLERANCE_SQR = 16.0D; // 4 格
    /** 普通式神护卫环绕容差。 */
    private static final double ORBIT_TOLERANCE_SQR = 2.25D; // 1.5 格
    /** 超过此距离传送回主人附近（仅护卫）。 */
    private static final double TELEPORT_DISTANCE = 18.0D;
    /** 协同作战主动索敌半径。 */
    private static final double ASSIST_AGGRO = 28.0D;
    private static final double ASSIST_AGGRO_SQR = ASSIST_AGGRO * ASSIST_AGGRO;
    /** 护卫环绕半径。 */
    private static final double GUARD_ORBIT = 4.5D;
    /** 护卫：仇恨锁定主人时的拦截半径（不必等主人挨打）。 */
    private static final double GUARD_THREAT_RANGE = 16.0D;
    private static final double GUARD_THREAT_RANGE_SQR = GUARD_THREAT_RANGE * GUARD_THREAT_RANGE;

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
                if (GreatSerpentAmbushLogic.isAmbushing(mob) || PiercingOxChargeLogic.isCharging(mob))
                {
                    continue;
                }
                if (mob instanceof JadeDogEntity jadeDog && jadeDog.isEating())
                {
                    continue;
                }
                if (mob instanceof MahoragaEntity mahoraga && mahoraga.isRideMode())
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
        /* 护卫姿态才拉回主人附近；协同作战不传送，避免打断追击 */
        if (guard && distSqr > TELEPORT_DISTANCE * TELEPORT_DISTANCE)
        {
            teleportNearOwner(owner, mob, slotIndex, totalCount, true);
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
        LivingEntity retaliate = resolveRetaliateTarget(owner, mob);
        if (ordered != null)
        {
            setAttackTarget(mob, ordered);
        }
        else if (retaliate != null)
        {
            /* 自身受击反击：高于普通索敌 */
            setAttackTarget(mob, retaliate);
        }
        else
        {
            LivingEntity current = mob.getTarget();
            if (current != null && (!current.isAlive() || isFriendly(owner, current)))
            {
                clearTarget(mob);
                clearOrdered(mob);
                current = null;
            }
            /* 协同：主动搜寻附近敌对并进攻 */
            if (current == null)
            {
                LivingEntity hunt = findAssistTarget(owner, mob);
                if (hunt != null)
                {
                    setAttackTarget(mob, hunt);
                }
            }
        }

        LivingEntity target = mob.getTarget();
        if (target != null && target.isAlive())
        {
            /* 大蛇 / 蝦蟇无常规近战：只保持仇恨供招式，不追咬 */
            if (isAbilityOnlyMeleeLocked(mob))
            {
                if (distSqr > followStartSqr(mob))
                {
                    mob.getNavigation().moveTo(owner, 1.05D);
                }
                else
                {
                    stopIfIdleMahoraga(mob);
                }
                return;
            }
            mob.getNavigation().moveTo(target, chaseSpeed(mob, target));
            return;
        }
        if (distSqr > followStartSqr(mob))
        {
            mob.getNavigation().moveTo(owner, followSpeed(mob));
        }
        else
        {
            stopIfIdleMahoraga(mob);
        }
    }

    private static double followStartSqr(Mob mob)
    {
        double start = mob instanceof MahoragaEntity ? MAHORAGA_FOLLOW_START : FOLLOW_START;
        return start * start;
    }

    private static double orbitToleranceSqr(Mob mob)
    {
        return mob instanceof MahoragaEntity ? MAHORAGA_ORBIT_TOLERANCE_SQR : ORBIT_TOLERANCE_SQR;
    }

    /** 魔虚罗无战时停导航，避免贴着主人细碎挪位盖住 idle。 */
    private static void stopIfIdleMahoraga(Mob mob)
    {
        if (mob instanceof MahoragaEntity && mob.getTarget() == null)
        {
            mob.getNavigation().stop();
        }
    }

    private static double chaseSpeed(Mob mob, LivingEntity target)
    {
        if (mob instanceof MahoragaEntity mahoraga)
        {
            return mahoraga.navigationSpeedForChase(target);
        }
        return 1.15D;
    }

    private static double followSpeed(Mob mob)
    {
        if (mob instanceof MahoragaEntity mahoraga)
        {
            return mahoraga.navigationSpeedForFollow();
        }
        return 1.1D;
    }

    private static boolean isAbilityOnlyMeleeLocked(Mob mob)
    {
        int index = mob.getPersistentData().getInt(TenShadowsSummonLogic.TAG_INDEX);
        return index == TenShadowsShikigami.GREAT_SERPENT.index()
                || index == TenShadowsShikigami.TOAD.index()
                || index == TenShadowsShikigami.ROUND_DEER.index();
    }

    private static void tickGuard(ServerPlayer owner, Mob mob, int slotIndex, int totalCount)
    {
        clearOrdered(mob);

        /* 最高优先：仇恨锁定主人且进入威胁距离 → 立刻拦截 */
        LivingEntity priorityThreat = findOwnerAggroThreat(owner, mob);
        if (priorityThreat != null)
        {
            chaseGuardTarget(owner, mob, slotIndex, totalCount, priorityThreat);
            return;
        }

        /* 次高优先：自身受击反击 */
        LivingEntity retaliate = resolveRetaliateTarget(owner, mob);
        if (retaliate != null)
        {
            setAttackTarget(mob, retaliate);
            chaseGuardTarget(owner, mob, slotIndex, totalCount, retaliate);
            return;
        }

        LivingEntity current = mob.getTarget();
        if (current != null)
        {
            /* 无高优先威胁时：丢弃已失效或未锁定主人的目标 */
            if (!current.isAlive()
                    || isFriendly(owner, current)
                    || current.distanceToSqr(owner) > GUARD_THREAT_RANGE_SQR
                    || !isAggroOnOwner(owner, current))
            {
                clearTarget(mob);
                current = null;
            }
        }

        if (current != null)
        {
            chaseGuardTarget(owner, mob, slotIndex, totalCount, current);
            return;
        }

        Vec3 orbit = orbitPoint(owner, slotIndex, totalCount);
        if (mob.distanceToSqr(orbit) > orbitToleranceSqr(mob))
        {
            mob.getNavigation().moveTo(orbit.x, orbit.y, orbit.z, followSpeed(mob));
        }
        else
        {
            stopIfIdleMahoraga(mob);
        }
    }

    private static void chaseGuardTarget(
            ServerPlayer owner, Mob mob, int slotIndex, int totalCount, LivingEntity target)
    {
        setAttackTarget(mob, target);
        if (isAbilityOnlyMeleeLocked(mob))
        {
            Vec3 orbit = orbitPoint(owner, slotIndex, totalCount);
            if (mob.distanceToSqr(orbit) > orbitToleranceSqr(mob))
            {
                mob.getNavigation().moveTo(orbit.x, orbit.y, orbit.z, 1.05D);
            }
            return;
        }
        mob.getNavigation().moveTo(target, chaseSpeed(mob, target));
    }

    /** 协同：大范围主动索敌。 */
    private static LivingEntity findAssistTarget(ServerPlayer owner, Mob mob)
    {
        List<LivingEntity> nearby = owner.level().getEntitiesOfClass(
                LivingEntity.class,
                mob.getBoundingBox().inflate(ASSIST_AGGRO),
                candidate -> candidate.isAlive()
                        && candidate != owner
                        && candidate != mob
                        && !isFriendly(owner, candidate)
                        && mob.distanceToSqr(candidate) <= ASSIST_AGGRO_SQR
                        && isHostileThreat(candidate)
                        && mob.hasLineOfSight(candidate));
        return nearestTo(mob, nearby);
    }

    /**
     * 护卫高优先：已仇恨锁定主人，且距主人小于 {@link #GUARD_THREAT_RANGE}。
     * 不要求视线，也不必等主人先挨打。
     */
    private static LivingEntity findOwnerAggroThreat(ServerPlayer owner, Mob mob)
    {
        List<LivingEntity> nearby = owner.level().getEntitiesOfClass(
                LivingEntity.class,
                owner.getBoundingBox().inflate(GUARD_THREAT_RANGE),
                candidate -> candidate.isAlive()
                        && candidate != owner
                        && candidate != mob
                        && !isFriendly(owner, candidate)
                        && candidate.distanceToSqr(owner) <= GUARD_THREAT_RANGE_SQR
                        && isAggroOnOwner(owner, candidate));
        return nearestTo(owner, nearby);
    }

    private static LivingEntity nearestTo(Entity origin, List<LivingEntity> candidates)
    {
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity candidate : candidates)
        {
            double d = origin.distanceToSqr(candidate);
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
                    clearRetaliate(mob);
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
        /* 协同主动参战；护卫在主人挨打时也拦截 */
        if (!PlayerTenShadowsStanceData.isAssist(owner) && !PlayerTenShadowsStanceData.isGuard(owner))
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
                    if (PlayerTenShadowsStanceData.isGuard(owner))
                    {
                        setAttackTarget(mob, attacker);
                    }
                    else if (mob.getTarget() == null)
                    {
                        setAttackTarget(mob, attacker);
                    }
                }
            }
        }
    }

    /**
     * 式神自身受击：将攻击者记为高优先反击目标（协同 / 护卫均生效）。
     */
    public static void onShikigamiHurt(Mob shikigami, LivingEntity attacker)
    {
        if (shikigami == null || attacker == null || !attacker.isAlive() || shikigami.level().isClientSide)
        {
            return;
        }
        if (!TenShadowsSummonLogic.isFriendlyOwned(shikigami))
        {
            return;
        }
        UUID ownerId = TenShadowsSummonLogic.getOwnerId(shikigami);
        if (ownerId == null)
        {
            return;
        }
        if (attacker.getUUID().equals(ownerId) || TenShadowsSummonLogic.isOwnedBy(attacker, ownerId))
        {
            return;
        }
        shikigami.getPersistentData().putUUID(TAG_RETALIATE_TARGET, attacker.getUUID());
        setAttackTarget(shikigami, attacker);
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

    private static LivingEntity resolveRetaliateTarget(ServerPlayer owner, Mob mob)
    {
        if (!mob.getPersistentData().hasUUID(TAG_RETALIATE_TARGET))
        {
            return null;
        }
        UUID targetId = mob.getPersistentData().getUUID(TAG_RETALIATE_TARGET);
        Entity entity = owner.serverLevel().getEntity(targetId);
        if (!(entity instanceof LivingEntity living) || !living.isAlive() || isFriendly(owner, living))
        {
            clearRetaliate(mob);
            return null;
        }
        return living;
    }

    private static void clearRetaliate(Mob mob)
    {
        mob.getPersistentData().remove(TAG_RETALIATE_TARGET);
    }

    private static boolean isFriendly(ServerPlayer owner, LivingEntity other)
    {
        if (other.getUUID().equals(owner.getUUID()))
        {
            return true;
        }
        return TenShadowsSummonLogic.isOwnedBy(other, owner.getUUID());
    }

    /** 协同索敌：敌对生物 / 其他玩家。 */
    private static boolean isHostileThreat(LivingEntity candidate)
    {
        if (candidate instanceof net.minecraft.world.entity.player.Player)
        {
            return true;
        }
        return candidate instanceof net.minecraft.world.entity.monster.Enemy;
    }

    /** 是否已将主人设为仇恨 / 战斗目标（含中立生物的持久愤怒）。 */
    private static boolean isAggroOnOwner(ServerPlayer owner, LivingEntity candidate)
    {
        if (candidate instanceof Mob mob && mob.getTarget() == owner)
        {
            return true;
        }
        if (candidate instanceof net.minecraft.world.entity.NeutralMob neutral)
        {
            java.util.UUID anger = neutral.getPersistentAngerTarget();
            if (anger != null && anger.equals(owner.getUUID()))
            {
                return true;
            }
        }
        if (candidate instanceof Warden warden)
        {
            return warden.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET)
                    .filter(t -> t == owner)
                    .isPresent();
        }
        return false;
    }

    private static void setAttackTarget(Mob mob, LivingEntity target)
    {
        mob.setTarget(target);
        if (mob instanceof MahoragaEntity mahoraga)
        {
            mahoraga.beginChase(target);
        }
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
        if (mob instanceof MahoragaEntity mahoraga)
        {
            mahoraga.clearChaseLock();
        }
        if (mob instanceof Warden warden)
        {
            warden.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
            warden.getBrain().eraseMemory(MemoryModuleType.ROAR_TARGET);
            warden.getBrain().eraseMemory(MemoryModuleType.DISTURBANCE_LOCATION);
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
