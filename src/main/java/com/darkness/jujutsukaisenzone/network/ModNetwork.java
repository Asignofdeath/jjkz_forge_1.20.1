package com.darkness.jujutsukaisenzone.network;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.network.packet.AdjustCastRangeTierC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.AerialMultiJumpC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.SelectStarMarkC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.StarMarkClearC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.AdjustBarrierRadiusTierC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.BarrierTechniqueCastC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.DomainExpansionCastC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.RepairBarrierC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.ReverseTechniqueTickC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.ShrineDomainTeleportC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.JudgmentTrialOpeningPerformanceS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.JudgmentJudgeAngryPerformanceS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.JudgmentDomainCmdCamSetupS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncJudgmentDomainColorCycleOverlayS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.JudgmentAccuseChoiceC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.JudgmentTrialLockOnC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.ShrineDomainCmdCamSetupS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.ShrineDomainLockOnC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncJudgmentAccuseChoiceS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncJudgmentTrialActiveS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncJudgmentTrialChargeS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncShrineDomainActiveS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncShrinePowerSlashChargeS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncShrineSlashLoopSoundStartS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncShrineSlashLoopSoundStopS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.OwnerOnlyPhotonMountS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.OwnerOnlyPhotonRemoveS2CPacket;
import com.darkness.jujutsukaisenzone.photon.OwnerOnlyPhotonFxParams;
import com.darkness.jujutsukaisenzone.api.screen.ScreenShakeKind;
import net.minecraft.resources.ResourceLocation;
import com.darkness.jujutsukaisenzone.network.packet.SyncShrineDomainScreenShakeS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncGhostFaceWobbleS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncBlackFlashScreenShakeS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncFugaBoomScreenShakeS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncCursedEnergyS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncCombatPowerS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncCursedGradeS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncExecutionersSwordActivationS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncFactionS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncPlayerInputLockS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncReverseTechniqueS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncFugaBoomFlashSequenceS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncFullScreenWorldFlashS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncLimitlessDomainAmbientSoundStartS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.LimitlessDomainCmdCamSetupAS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncLimitlessDomainAmbientSoundStopS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncLimitlessDomainScreenShakeS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncColoredWorldOverlayAnimationS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncPurpleOverlayAnimationS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncWhiteOverlayAnimationS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncShrineCastFilterS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncWhiteOverlayS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncMaxOutputChargeInterruptS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncTechniqueIntelS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncTechniqueS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.TechniqueHoldReleaseC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.TechniqueMaxOutputC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncBarrierRulesC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncBarrierRulesS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncBarrierTechniqueLearnedS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncDomainExpansionLearnedS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.TechniqueSpecialMoveC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.TechniqueSkillUseC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncSkillBarConfigC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncSkillBarConfigRequestC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncSkillBarConfigS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncSkillUnlocksS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncShrineBlackFlashMilestoneS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.UnlockSkillC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.UnlockSkillS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncMimicryS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncSeanceTechniqueLearnedS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncStrengthenTechniqueLearnedS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.EmotionalReleaseC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncEmotionalReleaseStateS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.ToggleMimicryC2SPacket;
import com.darkness.jujutsukaisenzone.player.PlayerMimicryData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.network.simple.SimpleChannel.MessageBuilder;

public final class ModNetwork
{
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(JuJutsuKaisenZone.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    private ModNetwork()
    {
    }

    public static void register()
    {
        CHANNEL.messageBuilder(SyncCursedEnergyS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncCursedEnergyS2CPacket::encode)
                .decoder(SyncCursedEnergyS2CPacket::decode)
                .consumerMainThread(SyncCursedEnergyS2CPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncFactionS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncFactionS2CPacket::encode)
                .decoder(SyncFactionS2CPacket::decode)
                .consumerMainThread(SyncFactionS2CPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncCursedGradeS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncCursedGradeS2CPacket::encode)
                .decoder(SyncCursedGradeS2CPacket::decode)
                .consumerMainThread(SyncCursedGradeS2CPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncReverseTechniqueS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncReverseTechniqueS2CPacket::encode)
                .decoder(SyncReverseTechniqueS2CPacket::decode)
                .consumerMainThread(SyncReverseTechniqueS2CPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncTechniqueS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncTechniqueS2CPacket::encode)
                .decoder(SyncTechniqueS2CPacket::decode)
                .consumerMainThread(SyncTechniqueS2CPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncMaxOutputChargeInterruptS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncMaxOutputChargeInterruptS2CPacket::encode)
                .decoder(SyncMaxOutputChargeInterruptS2CPacket::decode)
                .consumerMainThread(SyncMaxOutputChargeInterruptS2CPacket::handle)
                .add();
        CHANNEL.messageBuilder(ReverseTechniqueTickC2SPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ReverseTechniqueTickC2SPacket::encode)
                .decoder(ReverseTechniqueTickC2SPacket::decode)
                .consumerMainThread(ReverseTechniqueTickC2SPacket::handle)
                .add();
        CHANNEL.messageBuilder(TechniqueSkillUseC2SPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(TechniqueSkillUseC2SPacket::encode)
                .decoder(TechniqueSkillUseC2SPacket::decode)
                .consumerMainThread(TechniqueSkillUseC2SPacket::handle)
                .add();
        CHANNEL.messageBuilder(TechniqueSpecialMoveC2SPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(TechniqueSpecialMoveC2SPacket::encode)
                .decoder(TechniqueSpecialMoveC2SPacket::decode)
                .consumerMainThread(TechniqueSpecialMoveC2SPacket::handle)
                .add();
        CHANNEL.messageBuilder(AdjustCastRangeTierC2SPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(AdjustCastRangeTierC2SPacket::encode)
                .decoder(AdjustCastRangeTierC2SPacket::decode)
                .consumerMainThread(AdjustCastRangeTierC2SPacket::handle)
                .add();
        CHANNEL.messageBuilder(AdjustBarrierRadiusTierC2SPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(AdjustBarrierRadiusTierC2SPacket::encode)
                .decoder(AdjustBarrierRadiusTierC2SPacket::decode)
                .consumerMainThread(AdjustBarrierRadiusTierC2SPacket::handle)
                .add();
        CHANNEL.messageBuilder(TechniqueMaxOutputC2SPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(TechniqueMaxOutputC2SPacket::encode)
                .decoder(TechniqueMaxOutputC2SPacket::decode)
                .consumerMainThread(TechniqueMaxOutputC2SPacket::handle)
                .add();
        CHANNEL.messageBuilder(TechniqueHoldReleaseC2SPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(TechniqueHoldReleaseC2SPacket::encode)
                .decoder(TechniqueHoldReleaseC2SPacket::decode)
                .consumerMainThread(TechniqueHoldReleaseC2SPacket::handle)
                .add();
        CHANNEL.messageBuilder(BarrierTechniqueCastC2SPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(BarrierTechniqueCastC2SPacket::encode)
                .decoder(BarrierTechniqueCastC2SPacket::decode)
                .consumerMainThread(BarrierTechniqueCastC2SPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncBarrierRulesC2SPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SyncBarrierRulesC2SPacket::encode)
                .decoder(SyncBarrierRulesC2SPacket::decode)
                .consumerMainThread(SyncBarrierRulesC2SPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncBarrierRulesS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncBarrierRulesS2CPacket::encode)
                .decoder(SyncBarrierRulesS2CPacket::decode)
                .consumerMainThread(SyncBarrierRulesS2CPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncBarrierTechniqueLearnedS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncBarrierTechniqueLearnedS2CPacket::encode)
                .decoder(SyncBarrierTechniqueLearnedS2CPacket::decode)
                .consumerMainThread(SyncBarrierTechniqueLearnedS2CPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncDomainExpansionLearnedS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncDomainExpansionLearnedS2CPacket::encode)
                .decoder(SyncDomainExpansionLearnedS2CPacket::decode)
                .consumerMainThread(SyncDomainExpansionLearnedS2CPacket::handle)
                .add();
        CHANNEL.messageBuilder(RepairBarrierC2SPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RepairBarrierC2SPacket::encode)
                .decoder(RepairBarrierC2SPacket::decode)
                .consumerMainThread(RepairBarrierC2SPacket::handle)
                .add();
        CHANNEL.messageBuilder(DomainExpansionCastC2SPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(DomainExpansionCastC2SPacket::encode)
                .decoder(DomainExpansionCastC2SPacket::decode)
                .consumerMainThread(DomainExpansionCastC2SPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncWhiteOverlayS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncWhiteOverlayS2CPacket::encode)
                .decoder(SyncWhiteOverlayS2CPacket::decode)
                .consumerMainThread(SyncWhiteOverlayS2CPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncWhiteOverlayAnimationS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncWhiteOverlayAnimationS2CPacket::encode)
                .decoder(SyncWhiteOverlayAnimationS2CPacket::decode)
                .consumerMainThread(SyncWhiteOverlayAnimationS2CPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncPurpleOverlayAnimationS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncPurpleOverlayAnimationS2CPacket::encode)
                .decoder(SyncPurpleOverlayAnimationS2CPacket::decode)
                .consumerMainThread(SyncPurpleOverlayAnimationS2CPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncColoredWorldOverlayAnimationS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncColoredWorldOverlayAnimationS2CPacket::encode)
                .decoder(SyncColoredWorldOverlayAnimationS2CPacket::decode)
                .consumerMainThread(SyncColoredWorldOverlayAnimationS2CPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncFullScreenWorldFlashS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncFullScreenWorldFlashS2CPacket::encode)
                .decoder(SyncFullScreenWorldFlashS2CPacket::decode)
                .consumerMainThread(SyncFullScreenWorldFlashS2CPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncFugaBoomFlashSequenceS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncFugaBoomFlashSequenceS2CPacket::encode)
                .decoder(SyncFugaBoomFlashSequenceS2CPacket::decode)
                .consumerMainThread(SyncFugaBoomFlashSequenceS2CPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncLimitlessDomainAmbientSoundStartS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncLimitlessDomainAmbientSoundStartS2CPacket::encode)
                .decoder(SyncLimitlessDomainAmbientSoundStartS2CPacket::decode)
                .consumerMainThread(SyncLimitlessDomainAmbientSoundStartS2CPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncLimitlessDomainAmbientSoundStopS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncLimitlessDomainAmbientSoundStopS2CPacket::encode)
                .decoder(SyncLimitlessDomainAmbientSoundStopS2CPacket::decode)
                .consumerMainThread(SyncLimitlessDomainAmbientSoundStopS2CPacket::handle)
                .add();
        CHANNEL.messageBuilder(LimitlessDomainCmdCamSetupAS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(LimitlessDomainCmdCamSetupAS2CPacket::encode)
                .decoder(LimitlessDomainCmdCamSetupAS2CPacket::decode)
                .consumerMainThread(LimitlessDomainCmdCamSetupAS2CPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncLimitlessDomainScreenShakeS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncLimitlessDomainScreenShakeS2CPacket::encode)
                .decoder(SyncLimitlessDomainScreenShakeS2CPacket::decode)
                .consumerMainThread(SyncLimitlessDomainScreenShakeS2CPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncPlayerInputLockS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncPlayerInputLockS2CPacket::encode)
                .decoder(SyncPlayerInputLockS2CPacket::decode)
                .consumerMainThread(SyncPlayerInputLockS2CPacket::handle)
                .add();
        CHANNEL.messageBuilder(ShrineDomainTeleportC2SPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ShrineDomainTeleportC2SPacket::encode)
                .decoder(ShrineDomainTeleportC2SPacket::decode)
                .consumerMainThread(ShrineDomainTeleportC2SPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncShrineDomainScreenShakeS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncShrineDomainScreenShakeS2CPacket::encode)
                .decoder(SyncShrineDomainScreenShakeS2CPacket::decode)
                .consumerMainThread(SyncShrineDomainScreenShakeS2CPacket::handle)
                .add();
        CHANNEL.messageBuilder(ShrineDomainCmdCamSetupS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ShrineDomainCmdCamSetupS2CPacket::encode)
                .decoder(ShrineDomainCmdCamSetupS2CPacket::decode)
                .consumerMainThread(ShrineDomainCmdCamSetupS2CPacket::handle)
                .add();
        CHANNEL.messageBuilder(ShrineDomainLockOnC2SPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ShrineDomainLockOnC2SPacket::encode)
                .decoder(ShrineDomainLockOnC2SPacket::decode)
                .consumerMainThread(ShrineDomainLockOnC2SPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncShrineDomainActiveS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncShrineDomainActiveS2CPacket::encode)
                .decoder(SyncShrineDomainActiveS2CPacket::decode)
                .consumerMainThread(SyncShrineDomainActiveS2CPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncShrinePowerSlashChargeS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncShrinePowerSlashChargeS2CPacket::encode)
                .decoder(SyncShrinePowerSlashChargeS2CPacket::decode)
                .consumerMainThread(SyncShrinePowerSlashChargeS2CPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncShrineSlashLoopSoundStartS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncShrineSlashLoopSoundStartS2CPacket::encode)
                .decoder(SyncShrineSlashLoopSoundStartS2CPacket::decode)
                .consumerMainThread(SyncShrineSlashLoopSoundStartS2CPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncShrineSlashLoopSoundStopS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncShrineSlashLoopSoundStopS2CPacket::encode)
                .decoder(SyncShrineSlashLoopSoundStopS2CPacket::decode)
                .consumerMainThread(SyncShrineSlashLoopSoundStopS2CPacket::handle)
                .add();
        CHANNEL.messageBuilder(OwnerOnlyPhotonMountS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OwnerOnlyPhotonMountS2CPacket::encode)
                .decoder(OwnerOnlyPhotonMountS2CPacket::decode)
                .consumerMainThread(OwnerOnlyPhotonMountS2CPacket::handle)
                .add();
        CHANNEL.messageBuilder(OwnerOnlyPhotonRemoveS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OwnerOnlyPhotonRemoveS2CPacket::encode)
                .decoder(OwnerOnlyPhotonRemoveS2CPacket::decode)
                .consumerMainThread(OwnerOnlyPhotonRemoveS2CPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncGhostFaceWobbleS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncGhostFaceWobbleS2CPacket::encode)
                .decoder(SyncGhostFaceWobbleS2CPacket::decode)
                .consumerMainThread(SyncGhostFaceWobbleS2CPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncShrineCastFilterS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncShrineCastFilterS2CPacket::encode)
                .decoder(SyncShrineCastFilterS2CPacket::decode)
                .consumerMainThread(SyncShrineCastFilterS2CPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncSkillBarConfigC2SPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SyncSkillBarConfigC2SPacket::encode)
                .decoder(SyncSkillBarConfigC2SPacket::decode)
                .consumerMainThread(SyncSkillBarConfigC2SPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncSkillBarConfigS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncSkillBarConfigS2CPacket::encode)
                .decoder(SyncSkillBarConfigS2CPacket::decode)
                .consumerMainThread(SyncSkillBarConfigS2CPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncSkillBarConfigRequestC2SPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SyncSkillBarConfigRequestC2SPacket::encode)
                .decoder(SyncSkillBarConfigRequestC2SPacket::decode)
                .consumerMainThread(SyncSkillBarConfigRequestC2SPacket::handle)
                .add();
        CHANNEL.messageBuilder(ToggleMimicryC2SPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ToggleMimicryC2SPacket::encode)
                .decoder(ToggleMimicryC2SPacket::decode)
                .consumerMainThread(ToggleMimicryC2SPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncMimicryS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncMimicryS2CPacket::encode)
                .decoder(SyncMimicryS2CPacket::decode)
                .consumerMainThread(SyncMimicryS2CPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncSeanceTechniqueLearnedS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncSeanceTechniqueLearnedS2CPacket::encode)
                .decoder(SyncSeanceTechniqueLearnedS2CPacket::decode)
                .consumerMainThread(SyncSeanceTechniqueLearnedS2CPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncStrengthenTechniqueLearnedS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncStrengthenTechniqueLearnedS2CPacket::encode)
                .decoder(SyncStrengthenTechniqueLearnedS2CPacket::decode)
                .consumerMainThread(SyncStrengthenTechniqueLearnedS2CPacket::handle)
                .add();
        CHANNEL.messageBuilder(EmotionalReleaseC2SPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(EmotionalReleaseC2SPacket::encode)
                .decoder(EmotionalReleaseC2SPacket::decode)
                .consumerMainThread(EmotionalReleaseC2SPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncEmotionalReleaseStateS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncEmotionalReleaseStateS2CPacket::encode)
                .decoder(SyncEmotionalReleaseStateS2CPacket::decode)
                .consumerMainThread(SyncEmotionalReleaseStateS2CPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncBlackFlashScreenShakeS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncBlackFlashScreenShakeS2CPacket::encode)
                .decoder(SyncBlackFlashScreenShakeS2CPacket::decode)
                .consumerMainThread(SyncBlackFlashScreenShakeS2CPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncFugaBoomScreenShakeS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncFugaBoomScreenShakeS2CPacket::encode)
                .decoder(SyncFugaBoomScreenShakeS2CPacket::decode)
                .consumerMainThread(SyncFugaBoomScreenShakeS2CPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncTechniqueIntelS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncTechniqueIntelS2CPacket::encode)
                .decoder(SyncTechniqueIntelS2CPacket::decode)
                .consumerMainThread(SyncTechniqueIntelS2CPacket::handle)
                .add();
        CHANNEL.messageBuilder(UnlockSkillC2SPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(UnlockSkillC2SPacket::encode)
                .decoder(UnlockSkillC2SPacket::decode)
                .consumerMainThread(UnlockSkillC2SPacket::handle)
                .add();
        CHANNEL.messageBuilder(UnlockSkillS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(UnlockSkillS2CPacket::encode)
                .decoder(UnlockSkillS2CPacket::decode)
                .consumerMainThread(UnlockSkillS2CPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncSkillUnlocksS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncSkillUnlocksS2CPacket::encode)
                .decoder(SyncSkillUnlocksS2CPacket::decode)
                .consumerMainThread(SyncSkillUnlocksS2CPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncShrineBlackFlashMilestoneS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncShrineBlackFlashMilestoneS2CPacket::encode)
                .decoder(SyncShrineBlackFlashMilestoneS2CPacket::decode)
                .consumerMainThread(SyncShrineBlackFlashMilestoneS2CPacket::handle)
                .add();
        CHANNEL.messageBuilder(JudgmentTrialLockOnC2SPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(JudgmentTrialLockOnC2SPacket::encode)
                .decoder(JudgmentTrialLockOnC2SPacket::decode)
                .consumerMainThread(JudgmentTrialLockOnC2SPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncJudgmentTrialActiveS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncJudgmentTrialActiveS2CPacket::encode)
                .decoder(SyncJudgmentTrialActiveS2CPacket::decode)
                .consumerMainThread(SyncJudgmentTrialActiveS2CPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncJudgmentTrialChargeS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncJudgmentTrialChargeS2CPacket::encode)
                .decoder(SyncJudgmentTrialChargeS2CPacket::decode)
                .consumerMainThread(SyncJudgmentTrialChargeS2CPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncJudgmentAccuseChoiceS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncJudgmentAccuseChoiceS2CPacket::encode)
                .decoder(SyncJudgmentAccuseChoiceS2CPacket::decode)
                .consumerMainThread(SyncJudgmentAccuseChoiceS2CPacket::handle)
                .add();
        CHANNEL.messageBuilder(JudgmentAccuseChoiceC2SPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(JudgmentAccuseChoiceC2SPacket::encode)
                .decoder(JudgmentAccuseChoiceC2SPacket::decode)
                .consumerMainThread(JudgmentAccuseChoiceC2SPacket::handle)
                .add();
        CHANNEL.messageBuilder(JudgmentDomainCmdCamSetupS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(JudgmentDomainCmdCamSetupS2CPacket::encode)
                .decoder(JudgmentDomainCmdCamSetupS2CPacket::decode)
                .consumerMainThread(JudgmentDomainCmdCamSetupS2CPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncJudgmentDomainColorCycleOverlayS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncJudgmentDomainColorCycleOverlayS2CPacket::encode)
                .decoder(SyncJudgmentDomainColorCycleOverlayS2CPacket::decode)
                .consumerMainThread(SyncJudgmentDomainColorCycleOverlayS2CPacket::handle)
                .add();
        CHANNEL.messageBuilder(JudgmentTrialOpeningPerformanceS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(JudgmentTrialOpeningPerformanceS2CPacket::encode)
                .decoder(JudgmentTrialOpeningPerformanceS2CPacket::decode)
                .consumerMainThread(JudgmentTrialOpeningPerformanceS2CPacket::handle)
                .add();
        CHANNEL.messageBuilder(JudgmentJudgeAngryPerformanceS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(JudgmentJudgeAngryPerformanceS2CPacket::encode)
                .decoder(JudgmentJudgeAngryPerformanceS2CPacket::decode)
                .consumerMainThread(JudgmentJudgeAngryPerformanceS2CPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncExecutionersSwordActivationS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncExecutionersSwordActivationS2CPacket::encode)
                .decoder(SyncExecutionersSwordActivationS2CPacket::decode)
                .consumerMainThread(SyncExecutionersSwordActivationS2CPacket::handle)
                .add();
        CHANNEL.messageBuilder(AerialMultiJumpC2SPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(AerialMultiJumpC2SPacket::encode)
                .decoder(AerialMultiJumpC2SPacket::decode)
                .consumerMainThread(AerialMultiJumpC2SPacket::handle)
                .add();
        CHANNEL.messageBuilder(SelectStarMarkC2SPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SelectStarMarkC2SPacket::encode)
                .decoder(SelectStarMarkC2SPacket::decode)
                .consumerMainThread(SelectStarMarkC2SPacket::handle)
                .add();
        CHANNEL.messageBuilder(StarMarkClearC2SPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(StarMarkClearC2SPacket::encode)
                .decoder(StarMarkClearC2SPacket::decode)
                .consumerMainThread(StarMarkClearC2SPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncCombatPowerS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncCombatPowerS2CPacket::encode)
                .decoder(SyncCombatPowerS2CPacket::decode)
                .consumerMainThread(SyncCombatPowerS2CPacket::handle)
                .add();
    }

    public static void sendCombatPowerSync(ServerPlayer player, int physicalPower, int techniquePower)
    {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new SyncCombatPowerS2CPacket(physicalPower, techniquePower));
    }

    public static void sendCursedEnergySync(ServerPlayer player, int current, int max)
    {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncCursedEnergyS2CPacket(current, max));
    }

    public static void sendFactionSync(ServerPlayer player, String factionId)
    {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncFactionS2CPacket(factionId == null ? "" : factionId));
    }

    public static void sendCursedGradeSync(ServerPlayer player, int experience)
    {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncCursedGradeS2CPacket(experience));
    }

    public static void sendReverseTechniqueSync(ServerPlayer player, boolean learned)
    {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncReverseTechniqueS2CPacket(learned));
    }

    public static void sendBarrierTechniqueLearnedSync(ServerPlayer player, boolean learned, int tier)
    {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new SyncBarrierTechniqueLearnedS2CPacket(learned, tier));
    }

    public static void sendDomainExpansionLearnedSync(ServerPlayer player, boolean learned)
    {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncDomainExpansionLearnedS2CPacket(learned));
    }

    public static void sendSeanceTechniqueLearnedSync(ServerPlayer player, boolean learned, int tier)
    {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncSeanceTechniqueLearnedS2CPacket(learned, tier));
    }

    public static void sendStrengthenTechniqueLearnedSync(ServerPlayer player, boolean learned, int tier, boolean active)
    {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new SyncStrengthenTechniqueLearnedS2CPacket(learned, tier, active));
    }

    public static void sendTechniqueSync(ServerPlayer player, String techniqueId)
    {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncTechniqueS2CPacket(techniqueId == null ? "" : techniqueId));
    }

    public static void sendTechniqueIntelSync(ServerPlayer player, java.util.Map<String, Integer> values)
    {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncTechniqueIntelS2CPacket(values));
    }

    public static void sendSkillUnlocksSync(ServerPlayer player, java.util.Map<String, Boolean> unlockTags)
    {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncSkillUnlocksS2CPacket(unlockTags));
    }

    public static void sendUnlockSkillRequest(String techniqueId, String nodeId)
    {
        CHANNEL.send(PacketDistributor.SERVER.noArg(), new UnlockSkillC2SPacket(techniqueId, nodeId));
    }

    public static void sendAerialMultiJumpRequest()
    {
        CHANNEL.send(PacketDistributor.SERVER.noArg(), new AerialMultiJumpC2SPacket());
    }

    /** 客户端 → 服务端：切换星星标记的当前标记（delta 为 ±1）。 */
    public static void sendSelectStarMarkToServer(int delta)
    {
        CHANNEL.send(PacketDistributor.SERVER.noArg(), new SelectStarMarkC2SPacket(delta));
    }

    /** 客户端 → 服务端：快速双击 Shift 清除星星标记（单独清除 / 全部清除由服务端按准星判定）。 */
    public static void sendStarMarkClearToServer()
    {
        CHANNEL.send(PacketDistributor.SERVER.noArg(), new StarMarkClearC2SPacket());
    }

    public static void sendUnlockSkillResult(
            ServerPlayer player,
            com.darkness.jujutsukaisenzone.technique.development.SkillUnlockResult result,
            String techniqueId,
            String nodeId,
            String unlockTag)
    {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new UnlockSkillS2CPacket(result, techniqueId, nodeId, unlockTag));
    }

    public static void sendReverseTechniqueTickToServer()
    {
        CHANNEL.send(PacketDistributor.SERVER.noArg(), new ReverseTechniqueTickC2SPacket());
    }

    public static void sendEmotionalReleaseToServer(byte action)
    {
        CHANNEL.send(PacketDistributor.SERVER.noArg(), new EmotionalReleaseC2SPacket(action));
    }

    public static void sendEmotionalReleaseStateSync(
            ServerPlayer player,
            boolean charging,
            long chargeStartGameTime,
            long cooldownUntilGameTime)
    {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncEmotionalReleaseStateS2CPacket(charging, chargeStartGameTime, cooldownUntilGameTime)
        );
    }

    /** 黑闪触发时向攻击者客户端发送屏幕振动。 */
    public static void sendBlackFlashScreenShakeSync(ServerPlayer player, int durationTicks, float maxDegrees)
    {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncBlackFlashScreenShakeS2CPacket(durationTicks, maxDegrees)
        );
    }

    /**
     * 向玩家客户端发送屏幕振动。
     *
     * @param kind             振动类型
     * @param durationTicks    {@link ScreenShakeKind#DECAY} / {@link ScreenShakeKind#CONSTANT} /
     *                         {@link ScreenShakeKind#SMOOTH_WOBBLE} 的持续时间；{@link ScreenShakeKind#SUSTAINED} 可传 0
     * @param amplitude        最大偏转角度（度）
     * @param sustainedActive  仅 {@link ScreenShakeKind#SUSTAINED} 使用
     */
    public static void sendScreenShake(
            ServerPlayer player,
            ScreenShakeKind kind,
            int durationTicks,
            float amplitude,
            boolean sustainedActive)
    {
        switch (kind)
        {
            case DECAY -> CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new SyncLimitlessDomainScreenShakeS2CPacket(durationTicks, amplitude)
            );
            case CONSTANT -> CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new SyncFugaBoomScreenShakeS2CPacket(durationTicks, amplitude)
            );
            case SUSTAINED -> CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new SyncShrineDomainScreenShakeS2CPacket(sustainedActive, amplitude)
            );
            case SMOOTH_WOBBLE -> CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new SyncGhostFaceWobbleS2CPacket(durationTicks, amplitude)
            );
        }
    }

    public static void sendScreenShake(ServerPlayer player, ScreenShakeKind kind, int durationTicks, float amplitude)
    {
        sendScreenShake(player, kind, durationTicks, amplitude, false);
    }

    public static void sendShrineBlackFlashMilestoneSync(ServerPlayer player, int count)
    {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncShrineBlackFlashMilestoneS2CPacket(count)
        );
    }

    public static void sendTechniqueSkillUseToServer(int slot)
    {
        CHANNEL.send(PacketDistributor.SERVER.noArg(), new TechniqueSkillUseC2SPacket(slot));
    }

    public static void sendTechniqueSpecialMoveToServer(int modifierMask, int baseSlot)
    {
        CHANNEL.send(PacketDistributor.SERVER.noArg(), new TechniqueSpecialMoveC2SPacket(modifierMask, baseSlot));
    }

    public static void sendAdjustCastRangeTierToServer(int delta)
    {
        CHANNEL.send(PacketDistributor.SERVER.noArg(), new AdjustCastRangeTierC2SPacket(delta));
    }

    public static void sendAdjustBarrierRadiusTierToServer(int delta)
    {
        CHANNEL.send(PacketDistributor.SERVER.noArg(), new AdjustBarrierRadiusTierC2SPacket(delta));
    }

    public static void sendTechniqueMaxOutputToServer(byte action, int slot)
    {
        CHANNEL.send(PacketDistributor.SERVER.noArg(), new TechniqueMaxOutputC2SPacket(action, slot));
    }

    public static void sendTechniqueHoldReleaseToServer(byte action, int slot)
    {
        CHANNEL.send(PacketDistributor.SERVER.noArg(), new TechniqueHoldReleaseC2SPacket(action, slot));
    }

    public static void sendMaxOutputChargeInterruptSync(ServerPlayer player)
    {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncMaxOutputChargeInterruptS2CPacket());
    }

    public static void sendBarrierTechniqueCastToServer()
    {
        CHANNEL.send(PacketDistributor.SERVER.noArg(), new BarrierTechniqueCastC2SPacket());
    }

    public static void sendBarrierRulesToServer(boolean blockMobSpawn, boolean autoRepairBarrier)
    {
        CHANNEL.send(PacketDistributor.SERVER.noArg(), new SyncBarrierRulesC2SPacket(blockMobSpawn, autoRepairBarrier));
    }

    public static void sendRepairBarrierToServer()
    {
        CHANNEL.send(PacketDistributor.SERVER.noArg(), new RepairBarrierC2SPacket());
    }

    public static void sendDomainExpansionCastToServer()
    {
        CHANNEL.send(PacketDistributor.SERVER.noArg(), new DomainExpansionCastC2SPacket());
    }

    public static void sendShrineDomainTeleportToServer(float facingYaw)
    {
        CHANNEL.send(PacketDistributor.SERVER.noArg(), new ShrineDomainTeleportC2SPacket(facingYaw));
    }

    public static void sendShrineDomainLockOnToServer(boolean locking, java.util.UUID targetUuid)
    {
        CHANNEL.send(PacketDistributor.SERVER.noArg(), new ShrineDomainLockOnC2SPacket(locking, targetUuid));
    }

    public static void sendShrineDomainActiveSync(ServerPlayer player, boolean active)
    {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncShrineDomainActiveS2CPacket(active));
    }

    public static void sendShrinePowerSlashChargeSync(ServerPlayer player, boolean visible, float progress)
    {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncShrinePowerSlashChargeS2CPacket(visible, progress)
        );
    }

    public static void sendJudgmentTrialLockOnToServer(boolean locking, java.util.UUID targetUuid)
    {
        CHANNEL.send(PacketDistributor.SERVER.noArg(), new JudgmentTrialLockOnC2SPacket(locking, targetUuid));
    }

    public static void sendJudgmentTrialActiveSync(ServerPlayer player, boolean active)
    {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncJudgmentTrialActiveS2CPacket(active));
    }

    public static void sendJudgmentTrialChargeSync(ServerPlayer player, boolean visible, float progress)
    {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncJudgmentTrialChargeS2CPacket(visible, progress)
        );
    }

    public static void sendJudgmentAccuseChoiceSync(
            ServerPlayer player,
            boolean active,
            int remainingTicks,
            String crimeChinese,
            String crimeEnglish)
    {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncJudgmentAccuseChoiceS2CPacket(active, remainingTicks, crimeChinese, crimeEnglish)
        );
    }

    public static void sendJudgmentAccuseChoiceToServer(int responseId)
    {
        CHANNEL.send(PacketDistributor.SERVER.noArg(), new JudgmentAccuseChoiceC2SPacket(responseId));
    }

    /**
     * 诛伏赐死领域结界构建完成瞬间：仅施法者客户端在领域中心上方编排 CMDCam 镜头演出。
     */
    public static void sendJudgmentDomainCmdCamSetup(ServerPlayer caster, net.minecraft.core.BlockPos domainCenter)
    {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> caster),
                new JudgmentDomainCmdCamSetupS2CPacket(domainCenter)
        );
    }

    /** 诛伏赐死：结界完成第 4 秒向领域内玩家同步红黄青循环闪现世界层滤镜。 */
    public static void sendJudgmentDomainColorCycleOverlaySync(ServerPlayer player)
    {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncJudgmentDomainColorCycleOverlayS2CPacket()
        );
    }

    /** 「开始审判」开庭演出：施法者客户端 CMDCam + 服务端已同步触发动画。 */
    public static void sendJudgmentTrialOpeningPerformance(ServerPlayer caster, float playerYRot)
    {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> caster),
                new JudgmentTrialOpeningPerformanceS2CPacket(caster.position(), playerYRot)
        );
    }

    /** 审判者愤怒演出：施法者客户端播放审判者正脸 CMDCam 镜头（2 秒）。 */
    public static void sendJudgmentJudgeAngryPerformance(ServerPlayer caster, double judgeX, double judgeY, double judgeZ, float judgeYRot)
    {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> caster),
                new JudgmentJudgeAngryPerformanceS2CPacket(new net.minecraft.world.phys.Vec3(judgeX, judgeY, judgeZ), judgeYRot)
        );
    }

    public static void sendExecutionersSwordActivationSync(ServerPlayer player)
    {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncExecutionersSwordActivationS2CPacket());
    }

    public static void sendWhiteOverlaySync(ServerPlayer player, boolean enabled, float alpha)
    {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncWhiteOverlayS2CPacket(enabled, alpha));
    }

    public static void sendWhiteOverlayAnimationSync(ServerPlayer player, int totalTicks, int fadeInTicks, int fadeOutTicks, float peakAlpha)
    {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncWhiteOverlayAnimationS2CPacket(totalTicks, fadeInTicks, fadeOutTicks, peakAlpha)
        );
    }

    public static void sendPurpleOverlayAnimationSync(ServerPlayer player)
    {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncPurpleOverlayAnimationS2CPacket());
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
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncColoredWorldOverlayAnimationS2CPacket(
                        colorR, colorG, colorB, totalTicks, fadeInTicks, fadeOutTicks, peakAlpha)
        );
    }

    /** 世界画面最上层（方块与实体之上）、HUD 之下的全屏白色闪光，与蓄力白色滤镜无关。 */
    public static void sendFullScreenWorldFlashAnimationSync(ServerPlayer player, int totalTicks, int fadeInTicks, int fadeOutTicks, float peakAlpha)
    {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncFullScreenWorldFlashS2CPacket(totalTicks, fadeInTicks, fadeOutTicks, peakAlpha)
        );
    }

    /** 「灶」开爆炸：全屏闪白后无缝衔接世界层闪白。 */
    public static void sendFugaBoomFlashSequenceSync(ServerPlayer player)
    {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncFugaBoomFlashSequenceS2CPacket());
    }

    public static void sendLimitlessDomainAmbientSoundStart(ServerPlayer player, UUID casterUuid, double x, double y, double z)
    {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncLimitlessDomainAmbientSoundStartS2CPacket(casterUuid, x, y, z)
        );
    }

    public static void sendLimitlessDomainAmbientSoundStop(ServerPlayer player, UUID casterUuid)
    {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncLimitlessDomainAmbientSoundStopS2CPacket(casterUuid)
        );
    }

    /**
     * 无量空处结界构建完成瞬间：仅施法者客户端执行 CMDCam 镜头 A 初始化（玩家实体坐标由服务端采样）。
     */
    public static void sendLimitlessDomainCmdCamSetupA(ServerPlayer caster, Vec3 playerPos)
    {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> caster),
                new LimitlessDomainCmdCamSetupAS2CPacket(playerPos)
        );
    }

    /**
     * 伏魔神龛结界构建完成瞬间：仅施法者客户端执行 CMDCam 镜头路径编排（玩家实体坐标与朝向由服务端采样）。
     */
    public static void sendShrineDomainCmdCamSetup(ServerPlayer caster, Vec3 playerPos, float playerYRot)
    {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> caster),
                new ShrineDomainCmdCamSetupS2CPacket(playerPos, playerYRot)
        );
    }

    public static void sendShrineCastFilterSync(ServerPlayer player, boolean enabled)
    {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncShrineCastFilterS2CPacket(enabled)
        );
    }

    public static void sendShrineSlashLoopSoundStart(ServerPlayer player, UUID casterUuid)
    {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncShrineSlashLoopSoundStartS2CPacket(casterUuid)
        );
    }

    public static void sendShrineSlashLoopSoundStop(ServerPlayer player, UUID casterUuid)
    {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncShrineSlashLoopSoundStopS2CPacket(casterUuid)
        );
    }

    public static void sendOwnerOnlyPhotonMount(ServerPlayer player, int entityId, ResourceLocation fxId, OwnerOnlyPhotonFxParams params)
    {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new OwnerOnlyPhotonMountS2CPacket(entityId, fxId, params)
        );
    }

    public static void sendOwnerOnlyPhotonRemove(ServerPlayer player, int entityId, ResourceLocation fxId, boolean force)
    {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new OwnerOnlyPhotonRemoveS2CPacket(entityId, force, fxId)
        );
    }

    public static void sendPlayerInputLockSync(ServerPlayer player, boolean locked)
    {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncPlayerInputLockS2CPacket(locked));
    }

    /** 客户端 → 服务端：保存技能快捷栏配置。 */
    public static void sendSkillBarConfigToServer(String techniqueId, String[] skillIds)
    {
        CHANNEL.send(PacketDistributor.SERVER.noArg(), new SyncSkillBarConfigC2SPacket(techniqueId, skillIds));
    }

    /** 服务端 → 客户端：同步技能快捷栏配置。 */
    public static void sendSkillBarConfigSync(ServerPlayer player, String techniqueId, String[] skillIds)
    {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncSkillBarConfigS2CPacket(techniqueId, skillIds));
    }

    /** 客户端 → 服务端：切换拟态开关。 */
    public static void sendToggleMimicryToServer()
    {
        CHANNEL.send(PacketDistributor.SERVER.noArg(), new ToggleMimicryC2SPacket());
    }

    /** 向追踪该玩家的所有客户端（含本人）广播拟态状态。 */
    public static void sendMimicrySync(ServerPlayer subject)
    {
        CHANNEL.send(
                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> subject),
                createMimicryPacket(subject)
        );
    }

    /** 向指定客户端同步另一名玩家的拟态状态（登录、进入视野等场景）。 */
    public static void sendMimicrySyncTo(ServerPlayer receiver, ServerPlayer subject)
    {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> receiver), createMimicryPacket(subject));
    }

    private static SyncMimicryS2CPacket createMimicryPacket(ServerPlayer subject)
    {
        boolean active = PlayerMimicryData.isActive(subject);
        String characterId = "";
        if (active)
        {
            String resolved = PlayerMimicryData.getActiveCharacterId(subject);
            characterId = resolved == null ? "" : resolved;
        }
        return new SyncMimicryS2CPacket(subject.getUUID(), active, characterId);
    }
}
