package com.darkness.jujutsukaisenzone.technique;

import com.darkness.jujutsukaisenzone.compat.EpicFightCompat;
import com.darkness.jujutsukaisenzone.technique.aerial.AerialCombatSkillRegistry;
import com.darkness.jujutsukaisenzone.technique.aerial.AerialCombatSkills;
import com.darkness.jujutsukaisenzone.network.ModNetwork;
import com.darkness.jujutsukaisenzone.player.PlayerCursedEnergyData;
import com.darkness.jujutsukaisenzone.player.PlayerCursedGradeData;
import com.darkness.jujutsukaisenzone.player.PlayerEmotionalReleaseData;
import com.darkness.jujutsukaisenzone.player.PlayerFactionData;
import com.darkness.jujutsukaisenzone.player.PlayerMaxOutputChargeData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * 释放情绪：按住 Z 蓄力恢复咒力，挂载阵营色 Photon FX，蓄满或打断后进入冷却。
 */
public final class EmotionalReleaseServerLogic
{
    public static final int MAX_CHARGE_TICKS = 160;
    public static final int COOLDOWN_TICKS = 80;
    public static final int REGEN_INTERVAL_TICKS = 20;

    /** 每秒恢复咒力（index = {@link PlayerCursedGradeData#getRawRank}）。 */
    private static final int[] REGEN_PER_SECOND = {2, 4, 6, 8, 10, 14};

    private static final String FX_BLUE = "photon:cursed_blaze_blue";
    private static final String FX_RED = "photon:cursed_blaze_red";
    private static final String FX_PURPLE = "photon:cursed_blaze_purple";

    private static final String PHOTON_ATTACH_SUFFIX = " entity @s 0 0 0 0 0 0 1 1 1 0 false false xrot";

    private EmotionalReleaseServerLogic()
    {
    }

    public static boolean canBegin(ServerPlayer player)
    {
        if (!EpicFightCompat.isBattleMode(player))
        {
            return false;
        }
        if (PlayerCursedEnergyData.isHeavenlyRestricted(player))
        {
            return false;
        }
        if (PlayerEmotionalReleaseData.isCharging(player))
        {
            return false;
        }
        if (PlayerMaxOutputChargeData.isCharging(player))
        {
            return false;
        }
        long gameTime = player.level().getGameTime();
        if (PlayerEmotionalReleaseData.isOnCooldown(player, gameTime))
        {
            return false;
        }
        return PlayerCursedEnergyData.getCurrent(player) < PlayerCursedEnergyData.getMax(player);
    }

    public static void tryBegin(ServerPlayer player)
    {
        if (!canBegin(player))
        {
            return;
        }
        ServerLevel level = player.serverLevel();
        long gameTime = level.getGameTime();
        String fxId = resolveFxId(player);
        PlayerEmotionalReleaseData.beginCharge(player, gameTime, fxId);
        runPhotonCommand(player, "photon fx " + fxId + PHOTON_ATTACH_SUFFIX);
        ModNetwork.sendEmotionalReleaseStateSync(player, true, gameTime, PlayerEmotionalReleaseData.getCooldownUntilTick(player));
    }

    public static void tryRelease(ServerPlayer player)
    {
        if (!PlayerEmotionalReleaseData.isCharging(player))
        {
            return;
        }
        finishCharge(player, false);
    }

    public static void tickMaintain(ServerPlayer player)
    {
        long gameTime = player.level().getGameTime();
        if (!PlayerEmotionalReleaseData.isCharging(player))
        {
            return;
        }
        if (!EpicFightCompat.isBattleMode(player))
        {
            finishCharge(player, false);
            return;
        }
        if (isMoving(player))
        {
            finishCharge(player, false);
            return;
        }
        maintainAirSuspend(player);
        long startTick = PlayerEmotionalReleaseData.getChargeStartTick(player);
        long elapsed = gameTime - startTick;
        if (elapsed >= MAX_CHARGE_TICKS)
        {
            finishCharge(player, true);
            return;
        }
        applyRegenIfDue(player, gameTime);
    }

    public static void interruptFromDamage(ServerPlayer player)
    {
        if (!PlayerEmotionalReleaseData.isCharging(player))
        {
            return;
        }
        finishCharge(player, false);
    }

    private static void finishCharge(ServerPlayer player, boolean reachedMaxDuration)
    {
        player.setNoGravity(false);
        String fxId = PlayerEmotionalReleaseData.getMountedFxId(player);
        if (!fxId.isEmpty())
        {
            runPhotonCommand(player, "photon fx remove entity @s false " + fxId);
        }
        PlayerEmotionalReleaseData.clearCharge(player);
        long cooldownUntil = player.level().getGameTime() + COOLDOWN_TICKS;
        PlayerEmotionalReleaseData.setCooldownUntil(player, cooldownUntil);
        ModNetwork.sendEmotionalReleaseStateSync(player, false, 0L, cooldownUntil);
    }

    private static void applyRegenIfDue(ServerPlayer player, long gameTime)
    {
        long lastRegen = PlayerEmotionalReleaseData.getLastRegenTick(player);
        if (gameTime - lastRegen < REGEN_INTERVAL_TICKS)
        {
            return;
        }
        PlayerEmotionalReleaseData.setLastRegenTick(player, gameTime);

        int current = PlayerCursedEnergyData.getCurrent(player);
        int max = PlayerCursedEnergyData.getMax(player);
        if (current >= max)
        {
            return;
        }
        int rank = PlayerCursedGradeData.getRawRank(player);
        int amount = regenPerSecond(rank);
        int next = Math.min(max, current + amount);
        if (next != current)
        {
            PlayerCursedEnergyData.setCurrent(player, next);
            ModNetwork.sendCursedEnergySync(player, next, max);
        }
    }

    private static int regenPerSecond(int rank)
    {
        if (rank < 0 || rank >= REGEN_PER_SECOND.length)
        {
            return REGEN_PER_SECOND[0];
        }
        return REGEN_PER_SECOND[rank];
    }

    private static boolean isMoving(ServerPlayer player)
    {
        return Math.abs(player.xxa) > 0.01F || Math.abs(player.zza) > 0.01F;
    }

    /** 空中蓄力时停滞；落地、创造飞行或鞘翅时不生效。需已解锁「咒力悬空」。 */
    private static void maintainAirSuspend(ServerPlayer player)
    {
        if (!AerialCombatSkillRegistry.isUnlocked(player, AerialCombatSkills.SKILL_CURSE_LEVITATION))
        {
            player.setNoGravity(false);
            return;
        }
        if (player.onGround() || player.getAbilities().flying || player.isFallFlying())
        {
            player.setNoGravity(false);
            return;
        }
        player.setNoGravity(true);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
        player.hurtMarked = true;
    }

    public static String resolveFxId(ServerPlayer player)
    {
        return switch (PlayerFactionData.getFactionId(player))
        {
            case PlayerFactionData.FACTION_CURSE_USER -> FX_RED;
            case PlayerFactionData.FACTION_CURSE_SPIRIT -> FX_PURPLE;
            default -> FX_BLUE;
        };
    }

    private static void runPhotonCommand(ServerPlayer player, String command)
    {
        if (player.getServer() == null)
        {
            return;
        }
        CommandSourceStack source = player.createCommandSourceStack().withPermission(2).withSuppressedOutput();
        player.getServer().getCommands().performPrefixedCommand(source, command);
    }
}
