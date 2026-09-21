package com.darkness.jujutsukaisenzone.technique.skill;

import com.darkness.jujutsukaisenzone.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 审判术式音效：领域开庭、标题演出、没收、死刑与处刑人之剑必杀。
 */
public final class JudgmentSoundLogic
{
    /** 判处死刑后延迟播放 death penalty 音效（tick），与 {@link JudgmentTrialSkillLogic#DEATH_VERDICT_TITLE_DELAY_TICKS} 同步。 */
    public static final int DEATH_PENALTY_SOUND_DELAY_TICKS = JudgmentTrialSkillLogic.DEATH_VERDICT_TITLE_DELAY_TICKS;

    private record PendingDeathPenaltySound(UUID targetUuid, int remainingTicks)
    {
    }

    private static final ConcurrentMap<UUID, PendingDeathPenaltySound> pendingDeathPenaltyByCaster =
            new ConcurrentHashMap<>();

    private JudgmentSoundLogic()
    {
    }

    public static void playHoldACourtSessionAtDomainCenter(ServerLevel level, BlockPos center)
    {
        playAtDomainCenter(level, center, ModSounds.JUDGMENT_DOMAIN_HOLD_A_COURT_SESSION.get());
    }

    public static void playLayingStoneTilesAtDomainCenter(ServerLevel level, BlockPos center)
    {
        playAtDomainCenter(level, center, ModSounds.JUDGMENT_DOMAIN_LAYING_STONE_TILES.get());
    }

    private static void playAtDomainCenter(ServerLevel level, BlockPos center, SoundEvent sound)
    {
        double x = center.getX() + 0.5D;
        double y = center.getY() + 0.5D;
        double z = center.getZ() + 0.5D;
        level.playSound(null, x, y, z, sound, SoundSource.AMBIENT, 1.0F, 1.0F);
    }

    /** 大标题演出：仅该玩家客户端播放。 */
    public static void playBigSubtitleShow(ServerPlayer player)
    {
        playNotifyToPlayer(player, ModSounds.JUDGMENT_BIG_SUBTITLE_SHOW.get());
    }

    public static void playConfiscation(ServerPlayer caster, LivingEntity target)
    {
        playToCasterAndTarget(caster, target, ModSounds.JUDGMENT_CONFISCATION.get());
    }

    public static void playDeathPenalty(ServerPlayer caster, LivingEntity target)
    {
        playToCasterAndTarget(caster, target, ModSounds.JUDGMENT_DEATH_PENALTY.get());
    }

    public static void scheduleDeathPenalty(ServerPlayer caster, LivingEntity target)
    {
        pendingDeathPenaltyByCaster.put(
                caster.getUUID(),
                new PendingDeathPenaltySound(target.getUUID(), DEATH_PENALTY_SOUND_DELAY_TICKS));
    }

    public static void tickPendingDeathPenalty(ServerPlayer caster)
    {
        UUID uuid = caster.getUUID();
        PendingDeathPenaltySound pending = pendingDeathPenaltyByCaster.get(uuid);
        if (pending == null)
        {
            return;
        }

        if (pending.remainingTicks() <= 1)
        {
            pendingDeathPenaltyByCaster.remove(uuid);
            LivingEntity target = resolveTarget(caster, pending.targetUuid());
            playDeathPenalty(caster, target);
            return;
        }

        pendingDeathPenaltyByCaster.put(uuid, new PendingDeathPenaltySound(pending.targetUuid(), pending.remainingTicks() - 1));
    }

    public static void clearPendingDeathPenalty(UUID casterUuid)
    {
        pendingDeathPenaltyByCaster.remove(casterUuid);
    }

    private static LivingEntity resolveTarget(ServerPlayer caster, UUID targetUuid)
    {
        if (caster.getUUID().equals(targetUuid))
        {
            return caster;
        }
        if (caster.serverLevel().getEntity(targetUuid) instanceof LivingEntity living)
        {
            return living;
        }
        return null;
    }

    public static void playExecution(ServerPlayer attacker, LivingEntity target)
    {
        playToCasterAndTarget(attacker, target, ModSounds.JUDGMENT_EXECUTION.get());
    }

    private static void playToCasterAndTarget(ServerPlayer caster, LivingEntity target, SoundEvent sound)
    {
        if (caster != null)
        {
            playNotifyToPlayer(caster, sound);
        }
        if (target instanceof ServerPlayer targetPlayer)
        {
            if (caster == null || !targetPlayer.getUUID().equals(caster.getUUID()))
            {
                playNotifyToPlayer(targetPlayer, sound);
            }
            return;
        }
        if (target != null && target.level() instanceof ServerLevel level)
        {
            level.playSound(
                    null,
                    target.getX(),
                    target.getY(),
                    target.getZ(),
                    sound,
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F);
        }
    }

    private static void playNotifyToPlayer(ServerPlayer player, SoundEvent sound)
    {
        player.playNotifySound(sound, SoundSource.PLAYERS, 1.0F, 1.0F);
    }
}
