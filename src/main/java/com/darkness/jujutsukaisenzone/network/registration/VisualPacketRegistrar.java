package com.darkness.jujutsukaisenzone.network.registration;

import com.darkness.jujutsukaisenzone.network.packet.SyncWhiteOverlayS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncWhiteOverlayAnimationS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncPurpleOverlayAnimationS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncColoredWorldOverlayAnimationS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncFullScreenWorldFlashS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncFugaBoomFlashSequenceS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncLimitlessDomainAmbientSoundStartS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncLimitlessDomainAmbientSoundStopS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncLimitlessDomainScreenShakeS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncPlayerInputLockS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncShrineDomainScreenShakeS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncShrineSlashLoopSoundStartS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncShrineSlashLoopSoundStopS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.OwnerOnlyPhotonMountS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.OwnerOnlyPhotonRemoveS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncGhostFaceWobbleS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncShrineCastFilterS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncBlackFlashScreenShakeS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncFugaBoomScreenShakeS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncStylizedDamageStylesRuleS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncShrineBlackFlashMilestoneS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncJudgmentDomainColorCycleOverlayS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncDomainWhiteCurtainS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncDomainWhiteCurtainElevateS2CPacket;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.simple.SimpleChannel;

public final class VisualPacketRegistrar
{
    private VisualPacketRegistrar()
    {
    }

    public static void register(SimpleChannel channel)
    {
        channel.messageBuilder(SyncWhiteOverlayS2CPacket.class, 28, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncWhiteOverlayS2CPacket::encode)
                .decoder(SyncWhiteOverlayS2CPacket::decode)
                .consumerMainThread(SyncWhiteOverlayS2CPacket::handle)
                .add();
        channel.messageBuilder(SyncWhiteOverlayAnimationS2CPacket.class, 29, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncWhiteOverlayAnimationS2CPacket::encode)
                .decoder(SyncWhiteOverlayAnimationS2CPacket::decode)
                .consumerMainThread(SyncWhiteOverlayAnimationS2CPacket::handle)
                .add();
        channel.messageBuilder(SyncPurpleOverlayAnimationS2CPacket.class, 30, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncPurpleOverlayAnimationS2CPacket::encode)
                .decoder(SyncPurpleOverlayAnimationS2CPacket::decode)
                .consumerMainThread(SyncPurpleOverlayAnimationS2CPacket::handle)
                .add();
        channel.messageBuilder(SyncColoredWorldOverlayAnimationS2CPacket.class, 31, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncColoredWorldOverlayAnimationS2CPacket::encode)
                .decoder(SyncColoredWorldOverlayAnimationS2CPacket::decode)
                .consumerMainThread(SyncColoredWorldOverlayAnimationS2CPacket::handle)
                .add();
        channel.messageBuilder(SyncFullScreenWorldFlashS2CPacket.class, 32, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncFullScreenWorldFlashS2CPacket::encode)
                .decoder(SyncFullScreenWorldFlashS2CPacket::decode)
                .consumerMainThread(SyncFullScreenWorldFlashS2CPacket::handle)
                .add();
        channel.messageBuilder(SyncFugaBoomFlashSequenceS2CPacket.class, 33, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncFugaBoomFlashSequenceS2CPacket::encode)
                .decoder(SyncFugaBoomFlashSequenceS2CPacket::decode)
                .consumerMainThread(SyncFugaBoomFlashSequenceS2CPacket::handle)
                .add();
        channel.messageBuilder(SyncLimitlessDomainAmbientSoundStartS2CPacket.class, 34, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncLimitlessDomainAmbientSoundStartS2CPacket::encode)
                .decoder(SyncLimitlessDomainAmbientSoundStartS2CPacket::decode)
                .consumerMainThread(SyncLimitlessDomainAmbientSoundStartS2CPacket::handle)
                .add();
        channel.messageBuilder(SyncLimitlessDomainAmbientSoundStopS2CPacket.class, 35, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncLimitlessDomainAmbientSoundStopS2CPacket::encode)
                .decoder(SyncLimitlessDomainAmbientSoundStopS2CPacket::decode)
                .consumerMainThread(SyncLimitlessDomainAmbientSoundStopS2CPacket::handle)
                .add();
        channel.messageBuilder(SyncLimitlessDomainScreenShakeS2CPacket.class, 37, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncLimitlessDomainScreenShakeS2CPacket::encode)
                .decoder(SyncLimitlessDomainScreenShakeS2CPacket::decode)
                .consumerMainThread(SyncLimitlessDomainScreenShakeS2CPacket::handle)
                .add();
        channel.messageBuilder(SyncPlayerInputLockS2CPacket.class, 38, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncPlayerInputLockS2CPacket::encode)
                .decoder(SyncPlayerInputLockS2CPacket::decode)
                .consumerMainThread(SyncPlayerInputLockS2CPacket::handle)
                .add();
        channel.messageBuilder(SyncShrineDomainScreenShakeS2CPacket.class, 40, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncShrineDomainScreenShakeS2CPacket::encode)
                .decoder(SyncShrineDomainScreenShakeS2CPacket::decode)
                .consumerMainThread(SyncShrineDomainScreenShakeS2CPacket::handle)
                .add();
        channel.messageBuilder(SyncShrineSlashLoopSoundStartS2CPacket.class, 45, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncShrineSlashLoopSoundStartS2CPacket::encode)
                .decoder(SyncShrineSlashLoopSoundStartS2CPacket::decode)
                .consumerMainThread(SyncShrineSlashLoopSoundStartS2CPacket::handle)
                .add();
        channel.messageBuilder(SyncShrineSlashLoopSoundStopS2CPacket.class, 46, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncShrineSlashLoopSoundStopS2CPacket::encode)
                .decoder(SyncShrineSlashLoopSoundStopS2CPacket::decode)
                .consumerMainThread(SyncShrineSlashLoopSoundStopS2CPacket::handle)
                .add();
        channel.messageBuilder(OwnerOnlyPhotonMountS2CPacket.class, 47, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OwnerOnlyPhotonMountS2CPacket::encode)
                .decoder(OwnerOnlyPhotonMountS2CPacket::decode)
                .consumerMainThread(OwnerOnlyPhotonMountS2CPacket::handle)
                .add();
        channel.messageBuilder(OwnerOnlyPhotonRemoveS2CPacket.class, 48, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OwnerOnlyPhotonRemoveS2CPacket::encode)
                .decoder(OwnerOnlyPhotonRemoveS2CPacket::decode)
                .consumerMainThread(OwnerOnlyPhotonRemoveS2CPacket::handle)
                .add();
        channel.messageBuilder(SyncGhostFaceWobbleS2CPacket.class, 49, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncGhostFaceWobbleS2CPacket::encode)
                .decoder(SyncGhostFaceWobbleS2CPacket::decode)
                .consumerMainThread(SyncGhostFaceWobbleS2CPacket::handle)
                .add();
        channel.messageBuilder(SyncShrineCastFilterS2CPacket.class, 50, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncShrineCastFilterS2CPacket::encode)
                .decoder(SyncShrineCastFilterS2CPacket::decode)
                .consumerMainThread(SyncShrineCastFilterS2CPacket::handle)
                .add();
        channel.messageBuilder(SyncBlackFlashScreenShakeS2CPacket.class, 60, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncBlackFlashScreenShakeS2CPacket::encode)
                .decoder(SyncBlackFlashScreenShakeS2CPacket::decode)
                .consumerMainThread(SyncBlackFlashScreenShakeS2CPacket::handle)
                .add();
        channel.messageBuilder(SyncFugaBoomScreenShakeS2CPacket.class, 61, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncFugaBoomScreenShakeS2CPacket::encode)
                .decoder(SyncFugaBoomScreenShakeS2CPacket::decode)
                .consumerMainThread(SyncFugaBoomScreenShakeS2CPacket::handle)
                .add();
        channel.messageBuilder(SyncStylizedDamageStylesRuleS2CPacket.class, 67, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncStylizedDamageStylesRuleS2CPacket::encode)
                .decoder(SyncStylizedDamageStylesRuleS2CPacket::decode)
                .consumerMainThread(SyncStylizedDamageStylesRuleS2CPacket::handle)
                .add();
        channel.messageBuilder(SyncShrineBlackFlashMilestoneS2CPacket.class, 79, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncShrineBlackFlashMilestoneS2CPacket::encode)
                .decoder(SyncShrineBlackFlashMilestoneS2CPacket::decode)
                .consumerMainThread(SyncShrineBlackFlashMilestoneS2CPacket::handle)
                .add();
        channel.messageBuilder(SyncJudgmentDomainColorCycleOverlayS2CPacket.class, 86, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncJudgmentDomainColorCycleOverlayS2CPacket::encode)
                .decoder(SyncJudgmentDomainColorCycleOverlayS2CPacket::decode)
                .consumerMainThread(SyncJudgmentDomainColorCycleOverlayS2CPacket::handle)
                .add();
        channel.messageBuilder(SyncDomainWhiteCurtainS2CPacket.class, 95, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncDomainWhiteCurtainS2CPacket::encode)
                .decoder(SyncDomainWhiteCurtainS2CPacket::decode)
                .consumerMainThread(SyncDomainWhiteCurtainS2CPacket::handle)
                .add();
        channel.messageBuilder(SyncDomainWhiteCurtainElevateS2CPacket.class, 96, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncDomainWhiteCurtainElevateS2CPacket::encode)
                .decoder(SyncDomainWhiteCurtainElevateS2CPacket::decode)
                .consumerMainThread(SyncDomainWhiteCurtainElevateS2CPacket::handle)
                .add();
    }
}
