package com.darkness.jujutsukaisenzone.network.api;

import com.darkness.jujutsukaisenzone.network.NetworkSenders;
import com.darkness.jujutsukaisenzone.network.packet.OwnerOnlyPhotonMountS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.OwnerOnlyPhotonRemoveS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncBlackFlashScreenShakeS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncCinematicLetterboxS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncColoredWorldOverlayAnimationS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncDomainScanWaveS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncSpaceWarpS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncDomainWhiteCurtainElevateS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncDomainWhiteCurtainS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncInstantDomainCrossResidualS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncOpenDomainCubemapS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncLimitlessDomainGarbledBurstS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncFugaBoomFlashSequenceS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncFugaBoomScreenShakeS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncFullScreenWorldFlashS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncGhostFaceWobbleS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncJudgmentDomainColorCycleOverlayS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncLimitlessDomainAmbientSoundStartS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncLimitlessDomainAmbientSoundStopS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncLimitlessDomainScreenShakeS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncPlayerInputLockS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncPurpleOverlayAnimationS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncShrineBlackFlashMilestoneS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncShrineCastFilterS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncShrineDomainScreenShakeS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncShrineSlashLoopSoundStartS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncShrineSlashLoopSoundStopS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncSingleScreenShakeS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncStylizedDamageStylesRuleS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncWhiteOverlayAnimationS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncWhiteOverlayS2CPacket;
import com.darkness.jujutsukaisenzone.api.screen.ScreenShakeKind;
import com.darkness.jujutsukaisenzone.photon.OwnerOnlyPhotonFxParams;
import com.darkness.jujutsukaisenzone.player.PlayerMimicryData;
import com.darkness.jujutsukaisenzone.player.StarMark;
import com.darkness.jujutsukaisenzone.technique.development.SpNodeInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import java.util.UUID;

/** Domain specific send API for visual packets. */
public final class VisualNetwork
{
    private VisualNetwork()
    {
    }

    public static void syncStylizedDamageStylesRule(ServerPlayer player, boolean enabled)
    {
        NetworkSenders.toPlayer(player,
                new SyncStylizedDamageStylesRuleS2CPacket(enabled));
    }

    public static void syncStylizedDamageStylesRuleToAll(boolean enabled)
    {
        NetworkSenders.toAll(
                new SyncStylizedDamageStylesRuleS2CPacket(enabled));
    }

    public static void sendBlackFlashScreenShakeSync(ServerPlayer player, int durationTicks, float maxDegrees)
    {
        NetworkSenders.toPlayer(player,
                new SyncBlackFlashScreenShakeS2CPacket(durationTicks, maxDegrees)
        );
    }

    public static void sendScreenShake(
            ServerPlayer player,
            ScreenShakeKind kind,
            int durationTicks,
            float amplitude,
            boolean sustainedActive)
    {
        switch (kind)
        {
            case DECAY -> NetworkSenders.toPlayer(player,
                    new SyncLimitlessDomainScreenShakeS2CPacket(durationTicks, amplitude)
            );
            case CONSTANT -> NetworkSenders.toPlayer(player,
                    new SyncFugaBoomScreenShakeS2CPacket(durationTicks, amplitude)
            );
            case SUSTAINED -> NetworkSenders.toPlayer(player,
                    new SyncShrineDomainScreenShakeS2CPacket(sustainedActive, amplitude)
            );
            case SMOOTH_WOBBLE -> NetworkSenders.toPlayer(player,
                    new SyncGhostFaceWobbleS2CPacket(durationTicks, amplitude)
            );
            case SINGLE -> NetworkSenders.toPlayer(player,
                    new SyncSingleScreenShakeS2CPacket(durationTicks, amplitude)
            );
        }
    }

    public static void sendScreenShake(ServerPlayer player, ScreenShakeKind kind, int durationTicks, float amplitude)
    {
        sendScreenShake(player, kind, durationTicks, amplitude, false);
    }

    public static void sendShrineBlackFlashMilestoneSync(ServerPlayer player, int count)
    {
        NetworkSenders.toPlayer(player,
                new SyncShrineBlackFlashMilestoneS2CPacket(count)
        );
    }

    public static void sendJudgmentDomainColorCycleOverlaySync(ServerPlayer player)
    {
        NetworkSenders.toPlayer(player,
                new SyncJudgmentDomainColorCycleOverlayS2CPacket()
        );
    }

    public static void sendWhiteOverlaySync(ServerPlayer player, boolean enabled, float alpha)
    {
        NetworkSenders.toPlayer(player, new SyncWhiteOverlayS2CPacket(enabled, alpha));
    }

    public static void sendWhiteOverlayAnimationSync(ServerPlayer player, int totalTicks, int fadeInTicks, int fadeOutTicks, float peakAlpha)
    {
        NetworkSenders.toPlayer(player,
                new SyncWhiteOverlayAnimationS2CPacket(totalTicks, fadeInTicks, fadeOutTicks, peakAlpha)
        );
    }

    public static void sendPurpleOverlayAnimationSync(ServerPlayer player)
    {
        NetworkSenders.toPlayer(player, new SyncPurpleOverlayAnimationS2CPacket());
    }

    public static void sendColoredWorldOverlayAnimationSync(
            ServerPlayer player,
            int colorR,
            int colorG,
            int colorB,
            int totalTicks,
            int fadeInTicks,
            int fadeOutTicks,
            float peakAlpha)
    {
        NetworkSenders.toPlayer(player,
                new SyncColoredWorldOverlayAnimationS2CPacket(
                        colorR, colorG, colorB, totalTicks, fadeInTicks, fadeOutTicks, peakAlpha)
        );
    }

    public static void sendFullScreenWorldFlashAnimationSync(ServerPlayer player, int totalTicks, int fadeInTicks, int fadeOutTicks, float peakAlpha)
    {
        NetworkSenders.toPlayer(player,
                new SyncFullScreenWorldFlashS2CPacket(totalTicks, fadeInTicks, fadeOutTicks, peakAlpha)
        );
    }

    public static void sendDomainWhiteCurtainSync(ServerPlayer player, int durationTicks)
    {
        NetworkSenders.toPlayer(player,
                new SyncDomainWhiteCurtainS2CPacket(durationTicks)
        );
    }

    public static void sendDomainWhiteCurtainElevateSync(ServerPlayer player)
    {
        NetworkSenders.toPlayer(player,
                new SyncDomainWhiteCurtainElevateS2CPacket()
        );
    }

    public static void sendCinematicLetterboxSync(ServerPlayer player, int totalTicks, int fadeInTicks, int fadeOutTicks, float barFraction)
    {
        NetworkSenders.toPlayer(player,
                new SyncCinematicLetterboxS2CPacket(totalTicks, fadeInTicks, fadeOutTicks, barFraction)
        );
    }

    public static void sendDomainScanWaveSync(ServerPlayer player, double centerX, double centerY, double centerZ, int totalTicks, float peakRadius)
    {
        NetworkSenders.toPlayer(player,
                new SyncDomainScanWaveS2CPacket(centerX, centerY, centerZ, totalTicks, peakRadius)
        );
    }

    public static void sendSpaceWarpSync(ServerPlayer player, boolean enabled, float intensity)
    {
        NetworkSenders.toPlayer(player, new SyncSpaceWarpS2CPacket(enabled, intensity));
    }

    public static void sendInstantDomainCrossResidualSync(
            ServerPlayer player,
            double x,
            double y,
            double z,
            int durationTicks,
            float revolutions,
            ResourceLocation textureFolder)
    {
        NetworkSenders.toPlayer(player,
                new SyncInstantDomainCrossResidualS2CPacket(x, y, z, durationTicks, revolutions, textureFolder)
        );
    }

    public static void sendOpenDomainCubemapSync(
            ServerPlayer player,
            boolean active,
            double x,
            double y,
            double z,
            int radius,
            ResourceLocation textureFolder)
    {
        NetworkSenders.toPlayer(player,
                new SyncOpenDomainCubemapS2CPacket(active, x, y, z, radius, textureFolder)
        );
    }

    public static void sendLimitlessDomainGarbledBurst(ServerPlayer player, double x, double y, double z)
    {
        NetworkSenders.toPlayer(player, new SyncLimitlessDomainGarbledBurstS2CPacket(x, y, z));
    }

    public static void sendFugaBoomFlashSequenceSync(ServerPlayer player)
    {
        NetworkSenders.toPlayer(player, new SyncFugaBoomFlashSequenceS2CPacket());
    }

    public static void sendLimitlessDomainAmbientSoundStart(ServerPlayer player, UUID casterUuid, double x, double y, double z)
    {
        NetworkSenders.toPlayer(player,
                new SyncLimitlessDomainAmbientSoundStartS2CPacket(casterUuid, x, y, z)
        );
    }

    public static void sendLimitlessDomainAmbientSoundStop(ServerPlayer player, UUID casterUuid)
    {
        NetworkSenders.toPlayer(player,
                new SyncLimitlessDomainAmbientSoundStopS2CPacket(casterUuid)
        );
    }

    public static void sendShrineCastFilterSync(ServerPlayer player, boolean enabled)
    {
        NetworkSenders.toPlayer(player,
                new SyncShrineCastFilterS2CPacket(enabled)
        );
    }

    public static void sendShrineSlashLoopSoundStart(ServerPlayer player, UUID casterUuid)
    {
        NetworkSenders.toPlayer(player,
                new SyncShrineSlashLoopSoundStartS2CPacket(casterUuid)
        );
    }

    public static void sendShrineSlashLoopSoundStop(ServerPlayer player, UUID casterUuid)
    {
        NetworkSenders.toPlayer(player,
                new SyncShrineSlashLoopSoundStopS2CPacket(casterUuid)
        );
    }

    public static void sendOwnerOnlyPhotonMount(ServerPlayer player, int entityId, ResourceLocation fxId, OwnerOnlyPhotonFxParams params)
    {
        NetworkSenders.toPlayer(player,
                new OwnerOnlyPhotonMountS2CPacket(entityId, fxId, params)
        );
    }

    public static void sendOwnerOnlyPhotonRemove(ServerPlayer player, int entityId, ResourceLocation fxId, boolean force)
    {
        NetworkSenders.toPlayer(player,
                new OwnerOnlyPhotonRemoveS2CPacket(entityId, force, fxId)
        );
    }

    public static void sendPlayerInputLockSync(ServerPlayer player, boolean locked)
    {
        NetworkSenders.toPlayer(player, new SyncPlayerInputLockS2CPacket(locked));
    }
}
