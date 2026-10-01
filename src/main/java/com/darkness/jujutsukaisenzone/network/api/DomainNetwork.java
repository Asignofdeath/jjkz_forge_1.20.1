package com.darkness.jujutsukaisenzone.network.api;

import com.darkness.jujutsukaisenzone.network.NetworkSenders;
import com.darkness.jujutsukaisenzone.network.packet.AdjustBarrierRadiusTierC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.BarrierTechniqueCastC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.DomainExpansionCastC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.JudgmentAccuseChoiceC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.JudgmentDomainCmdCamSetupS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.JudgmentJudgeAngryPerformanceS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.JudgmentTrialLockOnC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.JudgmentTrialOpeningPerformanceS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.LimitlessDomainCmdCamSetupAS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.RepairBarrierC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.RepairMeltdownC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.ShrineDomainCmdCamSetupS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.ShrineDomainLockOnC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.ShrineDomainTeleportC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncBarrierRulesC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncBarrierTechniqueLearnedS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncBrainDamageS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncDomainExpansionLearnedS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncExecutionersSwordActivationS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncJudgmentAccuseChoiceS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncJudgmentTrialActiveS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncJudgmentTrialChargeS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncMeltdownRepairLearnedS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncShrineDomainActiveS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncShrinePowerSlashChargeS2CPacket;
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

/** Domain specific send API for domain packets. */
public final class DomainNetwork
{
    private DomainNetwork()
    {
    }

    public static void sendBarrierTechniqueLearnedSync(ServerPlayer player, boolean learned, int tier)
    {
        NetworkSenders.toPlayer(player,
                new SyncBarrierTechniqueLearnedS2CPacket(learned, tier));
    }

    public static void sendDomainExpansionLearnedSync(ServerPlayer player, boolean learned)
    {
        NetworkSenders.toPlayer(player, new SyncDomainExpansionLearnedS2CPacket(learned));
    }

    public static void sendMeltdownRepairLearnedSync(ServerPlayer player, boolean learned)
    {
        NetworkSenders.toPlayer(player, new SyncMeltdownRepairLearnedS2CPacket(learned));
    }

    public static void sendBrainDamageSync(ServerPlayer player, int brainDamage, int expandsToday)
    {
        sendDomainResourcesSync(player, brainDamage, expandsToday, com.darkness.jujutsukaisenzone.player.PlayerDomainGaugeData.getGauge(player), com.darkness.jujutsukaisenzone.player.PlayerBrainDamageData.getMeltdownRepairsToday(player));
    }

    public static void sendDomainResourcesSync(ServerPlayer player, int brainDamage, int expandsToday, int domainGauge, int meltdownRepairsToday)
    {
        NetworkSenders.toPlayer(player, new SyncBrainDamageS2CPacket(brainDamage, expandsToday, domainGauge, meltdownRepairsToday));
    }

    public static void sendAdjustBarrierRadiusTierToServer(int delta)
    {
        NetworkSenders.toServer( new AdjustBarrierRadiusTierC2SPacket(delta));
    }

    public static void sendBarrierTechniqueCastToServer()
    {
        NetworkSenders.toServer( new BarrierTechniqueCastC2SPacket());
    }

    public static void sendBarrierRulesToServer(boolean blockMobSpawn, boolean autoRepairBarrier)
    {
        NetworkSenders.toServer( new SyncBarrierRulesC2SPacket(blockMobSpawn, autoRepairBarrier));
    }

    public static void sendRepairBarrierToServer()
    {
        NetworkSenders.toServer( new RepairBarrierC2SPacket());
    }

    public static void sendRepairMeltdownToServer()
    {
        NetworkSenders.toServer( new RepairMeltdownC2SPacket());
    }

    public static void sendDomainExpansionCastToServer()
    {
        NetworkSenders.toServer( new DomainExpansionCastC2SPacket());
    }

    public static void sendShrineDomainTeleportToServer(float facingYaw)
    {
        NetworkSenders.toServer( new ShrineDomainTeleportC2SPacket(facingYaw));
    }

    public static void sendShrineDomainLockOnToServer(boolean locking, java.util.UUID targetUuid)
    {
        NetworkSenders.toServer( new ShrineDomainLockOnC2SPacket(locking, targetUuid));
    }

    public static void sendShrineDomainActiveSync(ServerPlayer player, boolean active)
    {
        NetworkSenders.toPlayer(player, new SyncShrineDomainActiveS2CPacket(active));
    }

    public static void sendShrinePowerSlashChargeSync(ServerPlayer player, boolean visible, float progress)
    {
        NetworkSenders.toPlayer(player,
                new SyncShrinePowerSlashChargeS2CPacket(visible, progress)
        );
    }

    public static void sendJudgmentTrialLockOnToServer(boolean locking, java.util.UUID targetUuid)
    {
        NetworkSenders.toServer( new JudgmentTrialLockOnC2SPacket(locking, targetUuid));
    }

    public static void sendJudgmentTrialActiveSync(ServerPlayer player, boolean active)
    {
        NetworkSenders.toPlayer(player, new SyncJudgmentTrialActiveS2CPacket(active));
    }

    public static void sendJudgmentTrialChargeSync(ServerPlayer player, boolean visible, float progress)
    {
        NetworkSenders.toPlayer(player,
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
        NetworkSenders.toPlayer(player,
                new SyncJudgmentAccuseChoiceS2CPacket(active, remainingTicks, crimeChinese, crimeEnglish)
        );
    }

    public static void sendJudgmentAccuseChoiceToServer(int responseId)
    {
        NetworkSenders.toServer( new JudgmentAccuseChoiceC2SPacket(responseId));
    }

    public static void sendJudgmentDomainCmdCamSetup(ServerPlayer caster, net.minecraft.core.BlockPos domainCenter)
    {
        NetworkSenders.toPlayer(caster,
                new JudgmentDomainCmdCamSetupS2CPacket(domainCenter)
        );
    }

    public static void sendJudgmentTrialOpeningPerformance(ServerPlayer caster, float playerYRot)
    {
        NetworkSenders.toPlayer(caster,
                new JudgmentTrialOpeningPerformanceS2CPacket(caster.position(), playerYRot)
        );
    }

    public static void sendJudgmentJudgeAngryPerformance(ServerPlayer caster, double judgeX, double judgeY, double judgeZ, float judgeYRot)
    {
        NetworkSenders.toPlayer(caster,
                new JudgmentJudgeAngryPerformanceS2CPacket(new net.minecraft.world.phys.Vec3(judgeX, judgeY, judgeZ), judgeYRot)
        );
    }

    public static void sendExecutionersSwordActivationSync(ServerPlayer player)
    {
        NetworkSenders.toPlayer(player, new SyncExecutionersSwordActivationS2CPacket());
    }

    public static void sendLimitlessDomainCmdCamSetupA(ServerPlayer caster, Vec3 playerPos)
    {
        NetworkSenders.toPlayer(caster,
                new LimitlessDomainCmdCamSetupAS2CPacket(playerPos)
        );
    }

    public static void sendShrineDomainCmdCamSetup(ServerPlayer caster, Vec3 playerPos, float playerYRot)
    {
        NetworkSenders.toPlayer(caster,
                new ShrineDomainCmdCamSetupS2CPacket(playerPos, playerYRot)
        );
    }
}
