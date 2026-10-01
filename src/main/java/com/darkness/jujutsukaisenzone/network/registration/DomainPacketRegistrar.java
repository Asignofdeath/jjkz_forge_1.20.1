package com.darkness.jujutsukaisenzone.network.registration;

import com.darkness.jujutsukaisenzone.network.packet.AdjustBarrierRadiusTierC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.BarrierTechniqueCastC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncBarrierRulesC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncBarrierRulesS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncBarrierTechniqueLearnedS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncDomainExpansionLearnedS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncMeltdownRepairLearnedS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncBrainDamageS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.RepairBarrierC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.RepairMeltdownC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.DomainExpansionCastC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.LimitlessDomainCmdCamSetupAS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.ShrineDomainTeleportC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.ShrineDomainCmdCamSetupS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.ShrineDomainLockOnC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncShrineDomainActiveS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncShrinePowerSlashChargeS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.JudgmentTrialLockOnC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncJudgmentTrialActiveS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncJudgmentTrialChargeS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncJudgmentAccuseChoiceS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.JudgmentAccuseChoiceC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.JudgmentDomainCmdCamSetupS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.JudgmentTrialOpeningPerformanceS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.JudgmentJudgeAngryPerformanceS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncExecutionersSwordActivationS2CPacket;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.simple.SimpleChannel;

public final class DomainPacketRegistrar
{
    private DomainPacketRegistrar()
    {
    }

    public static void register(SimpleChannel channel)
    {
        channel.messageBuilder(AdjustBarrierRadiusTierC2SPacket.class, 10, NetworkDirection.PLAY_TO_SERVER)
                .encoder(AdjustBarrierRadiusTierC2SPacket::encode)
                .decoder(AdjustBarrierRadiusTierC2SPacket::decode)
                .consumerMainThread(AdjustBarrierRadiusTierC2SPacket::handle)
                .add();
        channel.messageBuilder(BarrierTechniqueCastC2SPacket.class, 13, NetworkDirection.PLAY_TO_SERVER)
                .encoder(BarrierTechniqueCastC2SPacket::encode)
                .decoder(BarrierTechniqueCastC2SPacket::decode)
                .consumerMainThread(BarrierTechniqueCastC2SPacket::handle)
                .add();
        channel.messageBuilder(SyncBarrierRulesC2SPacket.class, 14, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SyncBarrierRulesC2SPacket::encode)
                .decoder(SyncBarrierRulesC2SPacket::decode)
                .consumerMainThread(SyncBarrierRulesC2SPacket::handle)
                .add();
        channel.messageBuilder(SyncBarrierRulesS2CPacket.class, 15, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncBarrierRulesS2CPacket::encode)
                .decoder(SyncBarrierRulesS2CPacket::decode)
                .consumerMainThread(SyncBarrierRulesS2CPacket::handle)
                .add();
        channel.messageBuilder(SyncBarrierTechniqueLearnedS2CPacket.class, 16, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncBarrierTechniqueLearnedS2CPacket::encode)
                .decoder(SyncBarrierTechniqueLearnedS2CPacket::decode)
                .consumerMainThread(SyncBarrierTechniqueLearnedS2CPacket::handle)
                .add();
        channel.messageBuilder(SyncDomainExpansionLearnedS2CPacket.class, 17, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncDomainExpansionLearnedS2CPacket::encode)
                .decoder(SyncDomainExpansionLearnedS2CPacket::decode)
                .consumerMainThread(SyncDomainExpansionLearnedS2CPacket::handle)
                .add();
        channel.messageBuilder(SyncMeltdownRepairLearnedS2CPacket.class, 18, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncMeltdownRepairLearnedS2CPacket::encode)
                .decoder(SyncMeltdownRepairLearnedS2CPacket::decode)
                .consumerMainThread(SyncMeltdownRepairLearnedS2CPacket::handle)
                .add();
        channel.messageBuilder(SyncBrainDamageS2CPacket.class, 19, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncBrainDamageS2CPacket::encode)
                .decoder(SyncBrainDamageS2CPacket::decode)
                .consumerMainThread(SyncBrainDamageS2CPacket::handle)
                .add();
        channel.messageBuilder(RepairBarrierC2SPacket.class, 25, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RepairBarrierC2SPacket::encode)
                .decoder(RepairBarrierC2SPacket::decode)
                .consumerMainThread(RepairBarrierC2SPacket::handle)
                .add();
        channel.messageBuilder(RepairMeltdownC2SPacket.class, 26, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RepairMeltdownC2SPacket::encode)
                .decoder(RepairMeltdownC2SPacket::decode)
                .consumerMainThread(RepairMeltdownC2SPacket::handle)
                .add();
        channel.messageBuilder(DomainExpansionCastC2SPacket.class, 27, NetworkDirection.PLAY_TO_SERVER)
                .encoder(DomainExpansionCastC2SPacket::encode)
                .decoder(DomainExpansionCastC2SPacket::decode)
                .consumerMainThread(DomainExpansionCastC2SPacket::handle)
                .add();
        channel.messageBuilder(LimitlessDomainCmdCamSetupAS2CPacket.class, 36, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(LimitlessDomainCmdCamSetupAS2CPacket::encode)
                .decoder(LimitlessDomainCmdCamSetupAS2CPacket::decode)
                .consumerMainThread(LimitlessDomainCmdCamSetupAS2CPacket::handle)
                .add();
        channel.messageBuilder(ShrineDomainTeleportC2SPacket.class, 39, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ShrineDomainTeleportC2SPacket::encode)
                .decoder(ShrineDomainTeleportC2SPacket::decode)
                .consumerMainThread(ShrineDomainTeleportC2SPacket::handle)
                .add();
        channel.messageBuilder(ShrineDomainCmdCamSetupS2CPacket.class, 41, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ShrineDomainCmdCamSetupS2CPacket::encode)
                .decoder(ShrineDomainCmdCamSetupS2CPacket::decode)
                .consumerMainThread(ShrineDomainCmdCamSetupS2CPacket::handle)
                .add();
        channel.messageBuilder(ShrineDomainLockOnC2SPacket.class, 42, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ShrineDomainLockOnC2SPacket::encode)
                .decoder(ShrineDomainLockOnC2SPacket::decode)
                .consumerMainThread(ShrineDomainLockOnC2SPacket::handle)
                .add();
        channel.messageBuilder(SyncShrineDomainActiveS2CPacket.class, 43, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncShrineDomainActiveS2CPacket::encode)
                .decoder(SyncShrineDomainActiveS2CPacket::decode)
                .consumerMainThread(SyncShrineDomainActiveS2CPacket::handle)
                .add();
        channel.messageBuilder(SyncShrinePowerSlashChargeS2CPacket.class, 44, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncShrinePowerSlashChargeS2CPacket::encode)
                .decoder(SyncShrinePowerSlashChargeS2CPacket::decode)
                .consumerMainThread(SyncShrinePowerSlashChargeS2CPacket::handle)
                .add();
        channel.messageBuilder(JudgmentTrialLockOnC2SPacket.class, 80, NetworkDirection.PLAY_TO_SERVER)
                .encoder(JudgmentTrialLockOnC2SPacket::encode)
                .decoder(JudgmentTrialLockOnC2SPacket::decode)
                .consumerMainThread(JudgmentTrialLockOnC2SPacket::handle)
                .add();
        channel.messageBuilder(SyncJudgmentTrialActiveS2CPacket.class, 81, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncJudgmentTrialActiveS2CPacket::encode)
                .decoder(SyncJudgmentTrialActiveS2CPacket::decode)
                .consumerMainThread(SyncJudgmentTrialActiveS2CPacket::handle)
                .add();
        channel.messageBuilder(SyncJudgmentTrialChargeS2CPacket.class, 82, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncJudgmentTrialChargeS2CPacket::encode)
                .decoder(SyncJudgmentTrialChargeS2CPacket::decode)
                .consumerMainThread(SyncJudgmentTrialChargeS2CPacket::handle)
                .add();
        channel.messageBuilder(SyncJudgmentAccuseChoiceS2CPacket.class, 83, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncJudgmentAccuseChoiceS2CPacket::encode)
                .decoder(SyncJudgmentAccuseChoiceS2CPacket::decode)
                .consumerMainThread(SyncJudgmentAccuseChoiceS2CPacket::handle)
                .add();
        channel.messageBuilder(JudgmentAccuseChoiceC2SPacket.class, 84, NetworkDirection.PLAY_TO_SERVER)
                .encoder(JudgmentAccuseChoiceC2SPacket::encode)
                .decoder(JudgmentAccuseChoiceC2SPacket::decode)
                .consumerMainThread(JudgmentAccuseChoiceC2SPacket::handle)
                .add();
        channel.messageBuilder(JudgmentDomainCmdCamSetupS2CPacket.class, 85, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(JudgmentDomainCmdCamSetupS2CPacket::encode)
                .decoder(JudgmentDomainCmdCamSetupS2CPacket::decode)
                .consumerMainThread(JudgmentDomainCmdCamSetupS2CPacket::handle)
                .add();
        channel.messageBuilder(JudgmentTrialOpeningPerformanceS2CPacket.class, 87, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(JudgmentTrialOpeningPerformanceS2CPacket::encode)
                .decoder(JudgmentTrialOpeningPerformanceS2CPacket::decode)
                .consumerMainThread(JudgmentTrialOpeningPerformanceS2CPacket::handle)
                .add();
        channel.messageBuilder(JudgmentJudgeAngryPerformanceS2CPacket.class, 88, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(JudgmentJudgeAngryPerformanceS2CPacket::encode)
                .decoder(JudgmentJudgeAngryPerformanceS2CPacket::decode)
                .consumerMainThread(JudgmentJudgeAngryPerformanceS2CPacket::handle)
                .add();
        channel.messageBuilder(SyncExecutionersSwordActivationS2CPacket.class, 89, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncExecutionersSwordActivationS2CPacket::encode)
                .decoder(SyncExecutionersSwordActivationS2CPacket::decode)
                .consumerMainThread(SyncExecutionersSwordActivationS2CPacket::handle)
                .add();
    }
}
