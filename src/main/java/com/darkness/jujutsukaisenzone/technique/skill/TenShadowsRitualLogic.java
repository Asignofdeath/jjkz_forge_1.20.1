package com.darkness.jujutsukaisenzone.technique.skill;

import com.darkness.jujutsukaisenzone.compat.EpicFightCompat;
import com.darkness.jujutsukaisenzone.entity.JadeDogEntity;
import com.darkness.jujutsukaisenzone.network.ModNetwork;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import com.darkness.jujutsukaisenzone.player.PlayerTenShadowsContractData;
import com.darkness.jujutsukaisenzone.player.PlayerTenShadowsModeData;
import com.darkness.jujutsukaisenzone.technique.skill.tenshadows.TenShadowsShikigamiStatsApplier;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 调伏仪式：单人击败即调伏；多人可点名任意多名生物/玩家。 */
public final class TenShadowsRitualLogic
{
    public static final String TAG_RITUAL = "jujutsukaisenzone_ten_shadows_ritual";
    public static final double CROSSHAIR_RANGE = 32.0D;
    public static final int MAX_INVITEES = 32;
    /** 式神与追杀目标超过此距离（格）则传送到目标附近。 */
    public static final double TELEPORT_DISTANCE = 48.0D;
    /** 仪式式神连续未造成伤害超过此时长（tick）→ 结束仪式。 */
    public static final int NO_DAMAGE_TIMEOUT_TICKS = 20 * 120;

    private static final Map<UUID, TenShadowsRitualSession> BY_CASTER = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> PARTICIPANT_TO_CASTER = new ConcurrentHashMap<>();
    private static final Map<UUID, InviteDraft> DRAFTS = new ConcurrentHashMap<>();

    private TenShadowsRitualLogic()
    {
    }

    private record InviteDraft(int shikigamiIndex, LinkedHashSet<UUID> invitees)
    {
    }

    public static void clearDraft(UUID casterId)
    {
        DRAFTS.remove(casterId);
    }

    public static TenShadowsRitualSession getByCaster(UUID casterId)
    {
        return BY_CASTER.get(casterId);
    }

    public static TenShadowsRitualSession findForParticipant(UUID participantId)
    {
        UUID casterId = PARTICIPANT_TO_CASTER.get(participantId);
        return casterId == null ? null : BY_CASTER.get(casterId);
    }

    public static boolean isInRitual(UUID participantId)
    {
        TenShadowsRitualSession session = findForParticipant(participantId);
        return session != null && !session.isClosed();
    }

    public static boolean isFalseDead(UUID participantId)
    {
        TenShadowsRitualSession session = findForParticipant(participantId);
        return session != null && !session.isClosed() && session.isFalseDead(participantId);
    }

    public static void handleShikigamiKey(ServerPlayer player, int shikigamiIndex)
    {
        if (!canUseKeys(player))
        {
            return;
        }
        if (isFalseDead(player.getUUID()))
        {
            return;
        }
        TenShadowsShikigami shikigami = TenShadowsShikigami.byIndex(shikigamiIndex);
        if (shikigami == null)
        {
            return;
        }
        PlayerTenShadowsContractData.ensureInitialized(player);
        if (PlayerTenShadowsContractData.isTamed(player, shikigami))
        {
            DRAFTS.remove(player.getUUID());
            TenShadowsSummonLogic.toggleShikigami(player, shikigamiIndex);
            return;
        }
        TenShadowsRitualSession existing = getByCaster(player.getUUID());
        if (existing != null && !existing.isClosed())
        {
            playerMsg(player, "调伏仪式进行中", ChatFormatting.YELLOW);
            return;
        }
        InviteDraft draft = DRAFTS.get(player.getUUID());
        LivingEntity crosshair = resolveCrosshairLiving(player, CROSSHAIR_RANGE);
        /* 已有本式神点名时：对准空处，或再对准已点名目标 → 开多人（避免近距离测时准星一直打在对方身上开不了） */
        if (draft != null
                && draft.shikigamiIndex() == shikigamiIndex
                && !draft.invitees().isEmpty()
                && (crosshair == null || draft.invitees().contains(crosshair.getUUID())))
        {
            tryStartMulti(player, shikigami, List.copyOf(draft.invitees()));
            return;
        }
        if (crosshair != null)
        {
            addInvitee(player, shikigami, crosshair);
            return;
        }
        if (draft != null && !draft.invitees().isEmpty())
        {
            playerMsg(player, "点名名单属于其他式神键，请用对应键开仪式或重新点名", ChatFormatting.YELLOW);
            return;
        }
        tryStartSolo(player, shikigami);
    }

    private static void addInvitee(ServerPlayer caster, TenShadowsShikigami shikigami, LivingEntity target)
    {
        if (!canStart(caster, shikigami))
        {
            return;
        }
        if (!isValidInvitee(caster, target))
        {
            playerMsg(caster, "无法点名该目标", ChatFormatting.YELLOW);
            return;
        }
        if (isInRitual(target.getUUID()))
        {
            playerMsg(caster, "目标已在其他调伏仪式中", ChatFormatting.YELLOW);
            return;
        }
        InviteDraft draft = DRAFTS.compute(caster.getUUID(), (id, old) ->
        {
            if (old == null || old.shikigamiIndex() != shikigami.index())
            {
                return new InviteDraft(shikigami.index(), new LinkedHashSet<>());
            }
            return old;
        });
        if (draft.invitees().size() >= MAX_INVITEES)
        {
            playerMsg(caster, "点名已满（最多 " + MAX_INVITEES + "）", ChatFormatting.YELLOW);
            return;
        }
        draft.invitees().add(target.getUUID());
        playerMsg(caster, "点名 · " + targetName(target) + "（" + draft.invitees().size()
                + "）· 再按同键开始多人调伏（可对准空处或已点名目标）", ChatFormatting.DARK_PURPLE);
        if (target instanceof ServerPlayer invitedPlayer)
        {
            playerMsg(invitedPlayer, "你被十影术师点名为调伏仪式参与者", ChatFormatting.DARK_RED);
        }
    }

    public static void tryStartSolo(ServerPlayer caster, TenShadowsShikigami shikigami)
    {
        if (!canStart(caster, shikigami))
        {
            return;
        }
        DRAFTS.remove(caster.getUUID());
        List<UUID> participants = List.of(caster.getUUID());
        TenShadowsRitualSession session = new TenShadowsRitualSession(
                caster.getUUID(), shikigami, TenShadowsRitualSession.Mode.SOLO, participants, caster.level().dimension());
        if (!spawnRitualShikigami(caster, session))
        {
            return;
        }
        registerSession(session);
        broadcast(session, Component.literal("调伏仪式 · 单人 · " + shikigami.displayName()).withStyle(ChatFormatting.DARK_PURPLE));
    }

    public static void tryStartMulti(ServerPlayer caster, TenShadowsShikigami shikigami, List<UUID> inviteeIds)
    {
        if (!canStart(caster, shikigami))
        {
            return;
        }
        LinkedHashSet<UUID> participants = new LinkedHashSet<>();
        participants.add(caster.getUUID());
        ServerLevel level = caster.serverLevel();
        for (UUID id : inviteeIds)
        {
            LivingEntity living = findLiving(level, id);
            if (living == null || !isValidInvitee(caster, living) || isInRitual(id))
            {
                continue;
            }
            participants.add(id);
        }
        if (participants.size() < 2)
        {
            playerMsg(caster, "没有有效的被点名者，无法开启多人调伏", ChatFormatting.YELLOW);
            return;
        }
        DRAFTS.remove(caster.getUUID());
        TenShadowsRitualSession session = new TenShadowsRitualSession(
                caster.getUUID(), shikigami, TenShadowsRitualSession.Mode.MULTI, List.copyOf(participants), caster.level().dimension());
        if (!spawnRitualShikigami(caster, session))
        {
            return;
        }
        registerSession(session);
        broadcast(session, Component.literal("调伏仪式 · 多人×" + participants.size() + " · " + shikigami.displayName() + "（无法调伏；全灭即死）").withStyle(ChatFormatting.DARK_RED));
        for (UUID id : participants)
        {
            if (id.equals(caster.getUUID()))
            {
                continue;
            }
            LivingEntity living = findLiving(level, id);
            if (living instanceof ServerPlayer invitedPlayer)
            {
                playerMsg(invitedPlayer, "你被强制卷入十种影法术的调伏仪式", ChatFormatting.DARK_RED);
            }
        }
    }
    public static void onRitualEntityDied(LivingEntity entity)
    {
        if (entity.level().isClientSide || !isRitualEntity(entity))
        {
            return;
        }
        CompoundTag data = entity.getPersistentData();
        if (!data.hasUUID(TenShadowsSummonLogic.TAG_OWNER))
        {
            return;
        }
        UUID casterId = data.getUUID(TenShadowsSummonLogic.TAG_OWNER);
        TenShadowsRitualSession session = getByCaster(casterId);
        if (session == null || session.isClosed() || !session.isRitualEntity(entity.getUUID()))
        {
            return;
        }
        ServerPlayer caster = serverPlayer(session.casterId());
        ServerLevel level = caster != null ? caster.serverLevel() : null;
        if (level != null)
        {
            for (UUID id : session.ritualEntityIds())
            {
                Entity other = level.getEntity(id);
                if (other != null && other.isAlive() && !other.getUUID().equals(entity.getUUID()))
                {
                    return;
                }
            }
        }
        if (session.isMulti())
        {
            voidMultiRitual(session, "式神已被祓除 · 仪式作废（未调伏）");
        }
        else
        {
            completeSoloTame(session);
        }
    }

    public static void onParticipantLethal(LivingEntity participant)
    {
        TenShadowsRitualSession session = findForParticipant(participant.getUUID());
        if (session == null || session.isClosed() || !session.isMulti())
        {
            return;
        }
        if (session.isFalseDead(participant.getUUID()))
        {
            return;
        }
        enterFalseDeath(session, participant);
        if (session.allParticipantsFalseDead())
        {
            wipeAll(session);
        }
    }

    public static void onParticipantTrueDeathOutsideFalseDeath(LivingEntity participant)
    {
        TenShadowsRitualSession session = findForParticipant(participant.getUUID());
        if (session == null || session.isClosed())
        {
            return;
        }
        if (session.isMulti())
        {
            if (!session.isFalseDead(participant.getUUID()))
            {
                session.falseDeadIds().add(participant.getUUID());
            }
            if (session.allParticipantsFalseDead())
            {
                wipeAll(session);
            }
            return;
        }
        if (participant.getUUID().equals(session.casterId()))
        {
            failSolo(session, "调伏失败 · 术师倒下");
        }
    }

    public static void onLogout(ServerPlayer player)
    {
        DRAFTS.remove(player.getUUID());
        TenShadowsRitualSession asCaster = getByCaster(player.getUUID());
        if (asCaster != null && !asCaster.isClosed())
        {
            if (asCaster.isMulti())
            {
                voidMultiRitual(asCaster, "术师离线 · 仪式作废");
            }
            else
            {
                failSolo(asCaster, "调伏中断 · 术师离线");
            }
            return;
        }
        TenShadowsRitualSession asParticipant = findForParticipant(player.getUUID());
        if (asParticipant != null && !asParticipant.isClosed() && asParticipant.isMulti())
        {
            asParticipant.falseDeadIds().add(player.getUUID());
            if (asParticipant.allParticipantsFalseDead())
            {
                wipeAll(asParticipant);
            }
        }
    }

    public static boolean isRitualEntity(Entity entity)
    {
        return entity != null && entity.getPersistentData().getBoolean(TAG_RITUAL);
    }

    private static boolean canUseKeys(ServerPlayer player)
    {
        return EpicFightCompat.isBattleMode(player)
                && PlayerTechniqueData.isTenShadows(player)
                && PlayerTenShadowsModeData.isShikigamiMode(player)
                && player.isAlive()
                && !player.isSpectator();
    }

    private static boolean canStart(ServerPlayer caster, TenShadowsShikigami shikigami)
    {
        if (!canUseKeys(caster))
        {
            return false;
        }
        if (PlayerTenShadowsContractData.isTamed(caster, shikigami))
        {
            playerMsg(caster, "该式神已调伏", ChatFormatting.YELLOW);
            return false;
        }
        TenShadowsRitualSession existing = getByCaster(caster.getUUID());
        if (existing != null && !existing.isClosed())
        {
            playerMsg(caster, "已有进行中的调伏仪式", ChatFormatting.YELLOW);
            return false;
        }
        return true;
    }

    private static boolean isValidInvitee(ServerPlayer caster, LivingEntity target)
    {
        if (target == null || !target.isAlive() || target.isRemoved())
        {
            return false;
        }
        if (target.getUUID().equals(caster.getUUID()))
        {
            return false;
        }
        if (target instanceof ServerPlayer sp && sp.isSpectator())
        {
            return false;
        }
        if (isRitualEntity(target))
        {
            return false;
        }
        if (target.getPersistentData().hasUUID(TenShadowsSummonLogic.TAG_OWNER)
                && target.getPersistentData().getUUID(TenShadowsSummonLogic.TAG_OWNER).equals(caster.getUUID())
                && !isRitualEntity(target))
        {
            return false;
        }
        return true;
    }

    private static void registerSession(TenShadowsRitualSession session)
    {
        ServerLevel level = resolveRitualLevel(session);
        if (level != null)
        {
            session.markDamageDealt(level.getGameTime());
        }
        BY_CASTER.put(session.casterId(), session);
        for (UUID id : session.participantIds())
        {
            PARTICIPANT_TO_CASTER.put(id, session.casterId());
        }
    }

    private static void unregisterSession(TenShadowsRitualSession session)
    {
        session.markClosed();
        BY_CASTER.remove(session.casterId(), session);
        for (UUID id : session.participantIds())
        {
            PARTICIPANT_TO_CASTER.remove(id, session.casterId());
        }
    }
    private static boolean spawnRitualShikigami(ServerPlayer caster, TenShadowsRitualSession session)
    {
        ServerLevel level = caster.serverLevel();
        TenShadowsShikigami shikigami = session.shikigami();
        Vec3 look = caster.getLookAngle();
        Vec3 base = caster.position().add(look.x * 2.0D, 0.0D, look.z * 2.0D);
        int count = Math.max(1, shikigami.spawnCount() == 8 ? 4 : shikigami.spawnCount());
        List<UUID> spawned = new ArrayList<>();
        for (int i = 0; i < count; i++)
        {
            double ox = (count == 1) ? 0.0D : (i % 2) * 1.2D - 0.6D;
            double oz = (count == 1) ? 0.0D : (i / 2) * 1.2D;
            Entity created = shikigami.placeholderType().create(level);
            if (!(created instanceof LivingEntity living))
            {
                continue;
            }
            living.moveTo(base.x + ox, caster.getY(), base.z + oz, caster.getYRot(), 0.0F);
            prepareHostile(caster, living, shikigami, i);
            if (!level.addFreshEntity(living))
            {
                continue;
            }
            session.addRitualEntity(living.getUUID());
            spawned.add(living.getUUID());
            retarget(living, session, level);
        }
        if (spawned.isEmpty())
        {
            playerMsg(caster, "调伏式神生成失败", ChatFormatting.RED);
            return false;
        }
        return true;
    }

    private static void prepareHostile(ServerPlayer caster, LivingEntity entity, TenShadowsShikigami shikigami, int spawnIndex)
    {
        String name = "未调伏·" + shikigami.displayName();
        if (shikigami == TenShadowsShikigami.DIVINE_DOGS)
        {
            name = spawnIndex == 0 ? "未调伏·玉犬·白" : "未调伏·玉犬·黑";
        }
        entity.setCustomName(Component.literal(name));
        entity.setCustomNameVisible(true);
        CompoundTag data = entity.getPersistentData();
        data.putBoolean(TAG_RITUAL, true);
        data.putUUID(TenShadowsSummonLogic.TAG_OWNER, caster.getUUID());
        data.putInt(TenShadowsSummonLogic.TAG_INDEX, shikigami.index());
        if (entity instanceof Mob mob)
        {
            mob.setPersistenceRequired();
            mob.setNoAi(false);
            mob.setTarget(caster);
        }
        if (entity instanceof JadeDogEntity jadeDog)
        {
            jadeDog.setWhiteVariant(spawnIndex == 0);
            jadeDog.applyMaturityFromOwner(caster);
            jadeDog.setTame(false);
            jadeDog.setPersistentAngerTarget(caster.getUUID());
            jadeDog.setRemainingPersistentAngerTime(20 * 60 * 20);
        }
        else if (entity instanceof Wolf wolf)
        {
            wolf.setTame(false);
            wolf.setPersistentAngerTarget(caster.getUUID());
            wolf.setRemainingPersistentAngerTime(20 * 60 * 20);
            wolf.setCollarColor(spawnIndex == 0
                    ? net.minecraft.world.item.DyeColor.WHITE
                    : net.minecraft.world.item.DyeColor.BLACK);
        }
        if (entity instanceof Hoglin hoglin)
        {
            hoglin.setImmuneToZombification(true);
        }
        TenShadowsShikigamiStatsApplier.applyUntamed(entity, shikigami);
    }

    private static void retarget(LivingEntity entity, TenShadowsRitualSession session, ServerLevel level)
    {
        if (!(entity instanceof Mob mob))
        {
            return;
        }
        LivingEntity priority = resolvePriorityTarget(session, level);
        if (priority != null)
        {
            mob.setTarget(priority);
            maybeTeleportToTarget(mob, priority);
            return;
        }
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (UUID id : session.participantIds())
        {
            if (session.isFalseDead(id))
            {
                continue;
            }
            LivingEntity living = findLiving(level, id);
            if (living == null || !living.isAlive() || living.level() != level)
            {
                continue;
            }
            double d = mob.distanceToSqr(living);
            if (d < bestDist)
            {
                bestDist = d;
                best = living;
            }
        }
        if (best != null)
        {
            mob.setTarget(best);
            maybeTeleportToTarget(mob, best);
        }
    }

    @javax.annotation.Nullable
    private static LivingEntity resolvePriorityTarget(TenShadowsRitualSession session, ServerLevel level)
    {
        UUID priorityId = session.priorityTargetId();
        if (priorityId == null)
        {
            return null;
        }
        if (session.isFalseDead(priorityId))
        {
            session.setPriorityTargetId(null);
            return null;
        }
        LivingEntity living = findLiving(level, priorityId);
        if (living == null || !living.isAlive() || living.level() != level)
        {
            session.setPriorityTargetId(null);
            return null;
        }
        return living;
    }

    private static void maybeTeleportToTarget(Mob mob, LivingEntity target)
    {
        if (!(target instanceof ServerPlayer) || mob.level() != target.level())
        {
            return;
        }
        if (mob.distanceToSqr(target) < TELEPORT_DISTANCE * TELEPORT_DISTANCE)
        {
            return;
        }
        Vec3 toward = target.position().subtract(mob.position());
        if (toward.lengthSqr() < 1.0E-4D)
        {
            toward = target.getLookAngle();
        }
        toward = toward.normalize();
        double tx = target.getX() - toward.x * 2.5D;
        double tz = target.getZ() - toward.z * 2.5D;
        mob.teleportTo(tx, target.getY(), tz);
    }

    /** 谁打了仪式式神（含非参与者）→ 优先仇恨。 */
    public static void onRitualEntityAttacked(LivingEntity ritualEntity, LivingEntity attacker)
    {
        if (ritualEntity == null || attacker == null || ritualEntity.level().isClientSide)
        {
            return;
        }
        if (!isRitualEntity(ritualEntity))
        {
            return;
        }
        CompoundTag data = ritualEntity.getPersistentData();
        if (!data.hasUUID(TenShadowsSummonLogic.TAG_OWNER))
        {
            return;
        }
        TenShadowsRitualSession session = getByCaster(data.getUUID(TenShadowsSummonLogic.TAG_OWNER));
        if (session == null || session.isClosed())
        {
            return;
        }
        if (session.isRitualEntity(attacker.getUUID()) || session.isFalseDead(attacker.getUUID()))
        {
            return;
        }
        session.setPriorityTargetId(attacker.getUUID());
        if (ritualEntity instanceof Mob mob)
        {
            mob.setTarget(attacker);
        }
    }

    /** 仪式式神对任意实体造成伤害 → 刷新无伤害超时计时。 */
    public static void onRitualEntityDealtDamage(LivingEntity ritualEntity)
    {
        if (ritualEntity == null || ritualEntity.level().isClientSide || !isRitualEntity(ritualEntity))
        {
            return;
        }
        CompoundTag data = ritualEntity.getPersistentData();
        if (!data.hasUUID(TenShadowsSummonLogic.TAG_OWNER))
        {
            return;
        }
        TenShadowsRitualSession session = getByCaster(data.getUUID(TenShadowsSummonLogic.TAG_OWNER));
        if (session == null || session.isClosed() || !session.isRitualEntity(ritualEntity.getUUID()))
        {
            return;
        }
        if (ritualEntity.level() instanceof ServerLevel level)
        {
            session.markDamageDealt(level.getGameTime());
        }
    }

    /** 参与者尝试离开仪式维度时拦截。 */
    public static boolean tryBlockDimensionTravel(Entity entity, ResourceKey<Level> destination)
    {
        if (entity == null || entity.level().isClientSide || destination == null)
        {
            return false;
        }
        TenShadowsRitualSession session = findForParticipant(entity.getUUID());
        if (session == null || session.isClosed() || session.dimension() == null)
        {
            return false;
        }
        if (destination.equals(session.dimension()))
        {
            return false;
        }
        if (entity instanceof ServerPlayer player)
        {
            playerMsg(player, "调伏仪式中无法离开此维度", ChatFormatting.DARK_RED);
        }
        return true;
    }

    private static void completeSoloTame(TenShadowsRitualSession session)
    {
        ServerPlayer caster = serverPlayer(session.casterId());
        discardRitualEntities(session);
        unregisterSession(session);
        if (caster != null)
        {
            PlayerTenShadowsContractData.grantTamed(caster, session.shikigami());
            playerMsg(caster, "调伏成功 · " + session.shikigami().displayName(), ChatFormatting.GREEN);
        }
    }

    private static void failSolo(TenShadowsRitualSession session, String message)
    {
        discardRitualEntities(session);
        unregisterSession(session);
        ServerPlayer caster = serverPlayer(session.casterId());
        if (caster != null)
        {
            playerMsg(caster, message, ChatFormatting.RED);
        }
    }

    private static void voidMultiRitual(TenShadowsRitualSession session, String message)
    {
        if (session.isClosed())
        {
            return;
        }
        ServerLevel level = resolveRitualLevel(session);
        discardRitualEntities(session);
        List<UUID> participants = List.copyOf(session.participantIds());
        unregisterSession(session);
        for (UUID id : participants)
        {
            LivingEntity living = level == null ? null : findLiving(level, id);
            if (living == null)
            {
                continue;
            }
            releaseFalseDeathPresentation(living);
            if (living.isAlive())
            {
                living.setHealth(1.0F);
            }
            if (living instanceof ServerPlayer sp)
            {
                playerMsg(sp, message, ChatFormatting.GOLD);
            }
        }
    }

    private static void enterFalseDeath(TenShadowsRitualSession session, LivingEntity living)
    {
        session.falseDeadIds().add(living.getUUID());
        if (living.getUUID().equals(session.priorityTargetId()))
        {
            session.setPriorityTargetId(null);
        }
        living.setHealth(1.0F);
        living.setDeltaMovement(Vec3.ZERO);
        if (living instanceof ServerPlayer player)
        {
            ModNetwork.sendPlayerInputLockSync(player, true);
            playerMsg(player, "假死 · 调伏仪式（离体观战，等待最终裁决）", ChatFormatting.DARK_GRAY);
        }
        if (living instanceof Mob mob)
        {
            mob.setNoAi(true);
            mob.setTarget(null);
        }
        clearAggressorsTargeting(living);
        ModNetwork.sendTenShadowsFalseDeathSync(living, true);
        broadcast(session, Component.literal(targetName(living) + " 进入假死").withStyle(ChatFormatting.GRAY));
    }

    private static void clearFalseDeath(TenShadowsRitualSession session, LivingEntity living, boolean setHpOne)
    {
        session.falseDeadIds().remove(living.getUUID());
        releaseFalseDeathPresentation(living);
        if (setHpOne && living.isAlive())
        {
            living.setHealth(1.0F);
        }
    }

    /** 会话已注销后也可调用：解除假死表现（输入锁 / NoAI / 客户端同步）。 */
    private static void releaseFalseDeathPresentation(LivingEntity living)
    {
        if (living instanceof ServerPlayer player)
        {
            ModNetwork.sendPlayerInputLockSync(player, false);
        }
        if (living instanceof Mob mob)
        {
            mob.setNoAi(false);
        }
        ModNetwork.sendTenShadowsFalseDeathSync(living, false);
    }

    /** 假死权威：锁 1 血、清速度；不依赖 GameType。 */
    public static void tickFalseDeadParticipants()
    {
        for (TenShadowsRitualSession session : List.copyOf(BY_CASTER.values()))
        {
            if (session.isClosed() || !session.isMulti())
            {
                continue;
            }
            ServerPlayer caster = serverPlayer(session.casterId());
            ServerLevel level = caster != null ? caster.serverLevel() : null;
            if (level == null)
            {
                continue;
            }
            for (UUID id : List.copyOf(session.falseDeadIds()))
            {
                LivingEntity living = findLiving(level, id);
                if (living == null || !living.isAlive())
                {
                    continue;
                }
                if (living.getHealth() < 1.0F)
                {
                    living.setHealth(1.0F);
                }
                living.setDeltaMovement(Vec3.ZERO);
                living.hurtMarked = true;
                if (living instanceof ServerPlayer player)
                {
                    player.setDeltaMovement(Vec3.ZERO);
                }
                if (living.tickCount % 20 == 0)
                {
                    clearAggressorsTargeting(living);
                }
            }
        }
    }

    /** 假死体不再被当作仇恨目标：清周围已盯上它的 Mob（含监守者记忆）。 */
    public static void clearAggressorsTargeting(LivingEntity victim)
    {
        if (!(victim.level() instanceof ServerLevel level))
        {
            return;
        }
        AABB box = victim.getBoundingBox().inflate(48.0D);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, box))
        {
            LivingEntity target = mob.getTarget();
            if (target == null || !target.getUUID().equals(victim.getUUID()))
            {
                continue;
            }
            clearMobTarget(mob);
        }
    }

    public static void clearMobTarget(Mob mob)
    {
        mob.setTarget(null);
        if (mob instanceof Warden warden)
        {
            warden.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
            warden.getBrain().eraseMemory(MemoryModuleType.ROAR_TARGET);
        }
    }

    private static void wipeAll(TenShadowsRitualSession session)
    {
        if (session.isClosed())
        {
            return;
        }
        ServerLevel level = resolveRitualLevel(session);
        discardRitualEntities(session);
        broadcast(session, Component.literal("调伏仪式终结 · 全员覆灭").withStyle(ChatFormatting.DARK_RED));
        List<UUID> participants = List.copyOf(session.participantIds());
        java.util.Set<UUID> wasFalseDead = java.util.Set.copyOf(session.falseDeadIds());
        /* 必须先注销会话：否则 living.kill() 会再进 LivingDeath → 假死/全灭，重入崩溃 */
        unregisterSession(session);
        for (UUID id : participants)
        {
            LivingEntity living = level == null ? null : findLiving(level, id);
            if (living == null)
            {
                continue;
            }
            if (wasFalseDead.contains(id))
            {
                releaseFalseDeathPresentation(living);
            }
            else if (living instanceof ServerPlayer player)
            {
                ModNetwork.sendPlayerInputLockSync(player, false);
            }
            if (living.isAlive())
            {
                living.kill();
            }
        }
    }

    private static void discardRitualEntities(TenShadowsRitualSession session)
    {
        ServerLevel level = resolveRitualLevel(session);
        if (level == null)
        {
            session.chunkTracker().releaseAll(null);
            session.ritualEntityIds().clear();
            return;
        }
        for (UUID id : List.copyOf(session.ritualEntityIds()))
        {
            session.chunkTracker().releaseEntity(level, id);
            Entity entity = level.getEntity(id);
            if (entity == null)
            {
                LivingEntity living = findLiving(level, id);
                entity = living;
            }
            if (entity != null)
            {
                entity.discard();
            }
        }
        session.chunkTracker().releaseAll(level);
        session.ritualEntityIds().clear();
        session.setPriorityTargetId(null);
    }

    @javax.annotation.Nullable
    private static ServerLevel resolveRitualLevel(TenShadowsRitualSession session)
    {
        ServerPlayer caster = serverPlayer(session.casterId());
        if (caster != null)
        {
            return caster.serverLevel();
        }
        net.minecraft.server.MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null || session.dimension() == null)
        {
            return null;
        }
        return server.getLevel(session.dimension());
    }

    private static void broadcast(TenShadowsRitualSession session, Component message)
    {
        ServerPlayer caster = serverPlayer(session.casterId());
        ServerLevel level = caster != null ? caster.serverLevel() : null;
        for (UUID id : session.participantIds())
        {
            LivingEntity living = level == null ? null : findLiving(level, id);
            if (living instanceof ServerPlayer sp)
            {
                sp.displayClientMessage(message, true);
            }
        }
    }

    private static void playerMsg(ServerPlayer player, String text, ChatFormatting color)
    {
        player.displayClientMessage(Component.literal(text).withStyle(color), true);
    }

    private static String targetName(LivingEntity living)
    {
        if (living.hasCustomName())
        {
            return living.getCustomName().getString();
        }
        if (living instanceof ServerPlayer player)
        {
            return player.getGameProfile().getName();
        }
        return living.getName().getString();
    }

    private static ServerPlayer serverPlayer(UUID id)
    {
        net.minecraft.server.MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null)
        {
            return null;
        }
        return server.getPlayerList().getPlayer(id);
    }

    private static LivingEntity findLiving(ServerLevel hint, UUID id)
    {
        if (hint == null || id == null)
        {
            return null;
        }
        Entity local = hint.getEntity(id);
        if (local instanceof LivingEntity living)
        {
            return living;
        }
        for (ServerLevel level : hint.getServer().getAllLevels())
        {
            Entity entity = level.getEntity(id);
            if (entity instanceof LivingEntity living)
            {
                return living;
            }
        }
        return null;
    }

    public static LivingEntity resolveCrosshairLiving(ServerPlayer player, double maxDistance)
    {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(maxDistance));
        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(maxDistance)).inflate(1.0D);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                player,
                eye,
                end,
                searchBox,
                entity -> entity instanceof LivingEntity living
                        && living.isAlive()
                        && !living.isRemoved()
                        && !living.getUUID().equals(player.getUUID())
                        && !(living instanceof ServerPlayer sp && sp.isSpectator())
                        && entity.isPickable(),
                maxDistance * maxDistance);
        if (hit != null && hit.getEntity() instanceof LivingEntity living)
        {
            return living;
        }
        return null;
    }

    /**
     * 残留仪式式神：会话已结束但仍带 {@link #TAG_RITUAL} 时丢弃，避免泄漏实体拖垮 tick。
     */
    public static void discardOrphanRitualEntity(LivingEntity entity)
    {
        if (entity == null || entity.level().isClientSide || !isRitualEntity(entity))
        {
            return;
        }
        CompoundTag data = entity.getPersistentData();
        if (!data.hasUUID(TenShadowsSummonLogic.TAG_OWNER))
        {
            entity.discard();
            return;
        }
        UUID ownerId = data.getUUID(TenShadowsSummonLogic.TAG_OWNER);
        TenShadowsRitualSession session = getByCaster(ownerId);
        if (session != null && !session.isClosed() && session.isRitualEntity(entity.getUUID()))
        {
            return;
        }
        entity.discard();
    }

    public static void tickSessions()
    {
        for (TenShadowsRitualSession session : List.copyOf(BY_CASTER.values()))
        {
            if (session.isClosed())
            {
                continue;
            }
            ServerLevel level = resolveRitualLevel(session);
            if (level == null)
            {
                continue;
            }
            tickUnloadFalseDeath(session, level);
            if (session.isClosed())
            {
                continue;
            }
            if (tickNoDamageTimeout(session, level))
            {
                continue;
            }
            for (UUID entityId : List.copyOf(session.ritualEntityIds()))
            {
                Entity entity = level.getEntity(entityId);
                if (!(entity instanceof LivingEntity living) || !living.isAlive())
                {
                    continue;
                }
                session.chunkTracker().tick(level, entity);
                retarget(living, session, level);
            }
        }
    }

    /**
     * 连续 {@link #NO_DAMAGE_TIMEOUT_TICKS} 未对任何实体造成伤害 →
     * 单人 failSolo，多人 voidMultiRitual（解除假死、不调伏、不全灭）。
     *
     * @return true 表示仪式已因此关闭
     */
    private static boolean tickNoDamageTimeout(TenShadowsRitualSession session, ServerLevel level)
    {
        long elapsed = level.getGameTime() - session.lastDamageGameTime();
        if (elapsed < NO_DAMAGE_TIMEOUT_TICKS)
        {
            return false;
        }
        if (session.isMulti())
        {
            voidMultiRitual(session, "仪式作废 · 式神超时未造成伤害（未调伏）");
        }
        else
        {
            failSolo(session, "调伏失败 · 式神超时未造成伤害");
        }
        return true;
    }

    /** 非玩家参与者离开全部已加载区块 → 立刻假死。 */
    private static void tickUnloadFalseDeath(TenShadowsRitualSession session, ServerLevel level)
    {
        if (!session.isMulti())
        {
            return;
        }
        for (UUID id : List.copyOf(session.participantIds()))
        {
            if (session.isFalseDead(id) || session.isClosed())
            {
                continue;
            }
            LivingEntity living = findLiving(level, id);
            if (living instanceof ServerPlayer)
            {
                continue;
            }
            if (living == null)
            {
                session.falseDeadIds().add(id);
                broadcast(session, Component.literal("非玩家参与者脱离加载区 · 假死").withStyle(ChatFormatting.GRAY));
                if (session.allParticipantsFalseDead())
                {
                    wipeAll(session);
                }
                continue;
            }
            if (living.level() instanceof ServerLevel entityLevel
                    && !entityLevel.isPositionEntityTicking(living.blockPosition()))
            {
                enterFalseDeath(session, living);
                if (session.allParticipantsFalseDead())
                {
                    wipeAll(session);
                }
            }
        }
    }
}
