package com.darkness.jujutsukaisenzone.network;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.api.screen.ScreenShakeKind;
import com.darkness.jujutsukaisenzone.network.api.DomainNetwork;
import com.darkness.jujutsukaisenzone.network.api.PlayerSyncNetwork;
import com.darkness.jujutsukaisenzone.network.api.SkillNetwork;
import com.darkness.jujutsukaisenzone.network.api.TechniqueNetwork;
import com.darkness.jujutsukaisenzone.network.api.VisualNetwork;
import com.darkness.jujutsukaisenzone.photon.OwnerOnlyPhotonFxParams;
import com.darkness.jujutsukaisenzone.player.StarMark;
import com.darkness.jujutsukaisenzone.technique.development.SpNodeInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.List;
import java.util.UUID;

public final class ModNetwork
{
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(JuJutsuKaisenZone.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private ModNetwork()
    {
    }

    public static void register()
    {
        NetworkRegistration.register(CHANNEL);
    }

    public static void sendCombatPowerSync(ServerPlayer player, int physicalPower, int techniquePower)
    {
        PlayerSyncNetwork.sendCombatPowerSync(player, physicalPower, techniquePower);
    }

    public static void sendCursedEnergySync(ServerPlayer player, int current, int max)
    {
        PlayerSyncNetwork.sendCursedEnergySync(player, current, max);
    }

    public static void sendFactionSync(ServerPlayer player, String factionId)
    {
        PlayerSyncNetwork.sendFactionSync(player, factionId);
    }

    public static void sendCursedGradeSync(ServerPlayer player, int experience)
    {
        PlayerSyncNetwork.sendCursedGradeSync(player, experience);
    }

    public static void sendReverseTechniqueSync(ServerPlayer player, boolean learned)
    {
        TechniqueNetwork.sendReverseTechniqueSync(player, learned);
    }

    public static void sendBarrierTechniqueLearnedSync(ServerPlayer player, boolean learned, int tier)
    {
        DomainNetwork.sendBarrierTechniqueLearnedSync(player, learned, tier);
    }

    public static void sendDomainExpansionLearnedSync(ServerPlayer player, boolean learned)
    {
        DomainNetwork.sendDomainExpansionLearnedSync(player, learned);
    }

    public static void sendMeltdownRepairLearnedSync(ServerPlayer player, boolean learned)
    {
        DomainNetwork.sendMeltdownRepairLearnedSync(player, learned);
    }

    public static void sendBrainDamageSync(ServerPlayer player, int brainDamage, int expandsToday)
    {
        DomainNetwork.sendBrainDamageSync(player, brainDamage, expandsToday);
    }

    public static void sendDomainResourcesSync(ServerPlayer player, int brainDamage, int expandsToday, int domainGauge, int meltdownRepairsToday)
    {
        DomainNetwork.sendDomainResourcesSync(player, brainDamage, expandsToday, domainGauge, meltdownRepairsToday);
    }

    public static void sendSeanceTechniqueLearnedSync(ServerPlayer player, boolean learned, int tier)
    {
        TechniqueNetwork.sendSeanceTechniqueLearnedSync(player, learned, tier);
    }

    public static void sendStrengthenTechniqueLearnedSync(ServerPlayer player, boolean learned, int tier, boolean active)
    {
        TechniqueNetwork.sendStrengthenTechniqueLearnedSync(player, learned, tier, active);
    }

    public static void sendTechniqueSync(ServerPlayer player, String techniqueId)
    {
        TechniqueNetwork.sendTechniqueSync(player, techniqueId);
    }

    public static void sendTechniqueIntelSync(ServerPlayer player, java.util.Map<String, Integer> values)
    {
        TechniqueNetwork.sendTechniqueIntelSync(player, values);
    }

    public static void sendSpSync(ServerPlayer player, int available, int total, int liberation, int allocated)
    {
        SkillNetwork.sendSpSync(player, available, total, liberation, allocated);
    }

    public static void sendSpNodesSync(
            ServerPlayer player,
            java.util.List<SpNodeInstance> lit,
            java.util.List<SpNodeInstance> offers,
            int quality,
            int allowance,
            int tier,
            int xp)
    {
        SkillNetwork.sendSpNodesSync(player, lit, offers, quality, allowance, tier, xp);
    }

    public static void sendSeLoadoutSync(ServerPlayer player, java.util.Map<String, com.darkness.jujutsukaisenzone.player.SeEquippedEntry> entries)
    {
        SkillNetwork.sendSeLoadoutSync(player, entries);
    }

    public static void sendSkillLevelsSync(ServerPlayer player, java.util.Map<String, Integer> levelTags)
    {
        SkillNetwork.sendSkillLevelsSync(player, levelTags);
    }

    public static void sendUpgradeSkillRequest(String techniqueId, String skillId)
    {
        SkillNetwork.sendUpgradeSkillRequest(techniqueId, skillId);
    }

    public static void sendUpgradeSkillResult(
            ServerPlayer player,
            com.darkness.jujutsukaisenzone.technique.development.SkillUpgradeResult result,
            String techniqueId,
            String skillId,
            String levelTag,
            int level)
    {
        SkillNetwork.sendUpgradeSkillResult(player, result, techniqueId, skillId, levelTag, level);
    }

    public static void sendSkillUnlocksSync(ServerPlayer player, java.util.Map<String, Boolean> unlockTags)
    {
        SkillNetwork.sendSkillUnlocksSync(player, unlockTags);
    }

    public static void sendUnlockSkillRequest(String techniqueId, String nodeId)
    {
        SkillNetwork.sendUnlockSkillRequest(techniqueId, nodeId);
    }

    public static void sendUnlockSpNodeRequest(String nodeId)
    {
        SkillNetwork.sendUnlockSpNodeRequest(nodeId);
    }

    public static void sendResetSpNodesRequest()
    {
        SkillNetwork.sendResetSpNodesRequest();
    }

    public static void sendRefreshSpOffersRequest()
    {
        SkillNetwork.sendRefreshSpOffersRequest();
    }

    public static void sendEquipSeRequest(String seId)
    {
        SkillNetwork.sendEquipSeRequest(seId);
    }

    public static void sendEquipSeRequest(String seId, int inventorySlot)
    {
        SkillNetwork.sendEquipSeRequest(seId, inventorySlot);
    }

    public static void sendUnequipSeRequest(String seId)
    {
        SkillNetwork.sendUnequipSeRequest(seId);
    }

    public static void sendCursedEnergyTraitSync(ServerPlayer player, String traitId)
    {
        PlayerSyncNetwork.sendCursedEnergyTraitSync(player, traitId);
    }

    public static void syncStylizedDamageStylesRule(ServerPlayer player, boolean enabled)
    {
        VisualNetwork.syncStylizedDamageStylesRule(player, enabled);
    }

    public static void syncStylizedDamageStylesRuleToAll(boolean enabled)
    {
        VisualNetwork.syncStylizedDamageStylesRuleToAll(enabled);
    }

    public static void sendAerialMultiJumpRequest()
    {
        PlayerSyncNetwork.sendAerialMultiJumpRequest();
    }

    public static void sendSelectStarMarkToServer(int delta)
    {
        PlayerSyncNetwork.sendSelectStarMarkToServer(delta);
    }

    public static void sendStarMarkClearToServer()
    {
        PlayerSyncNetwork.sendStarMarkClearToServer();
    }

    public static void sendStarMarksSync(ServerPlayer owner, List<StarMark> marks)
    {
        PlayerSyncNetwork.sendStarMarksSync(owner, marks);
    }

    public static void sendStarMarksSyncTo(ServerPlayer viewer, ServerPlayer owner, List<StarMark> marks)
    {
        PlayerSyncNetwork.sendStarMarksSyncTo(viewer, owner, marks);
    }

    public static void sendUnlockSkillResult(
            ServerPlayer player,
            com.darkness.jujutsukaisenzone.technique.development.SkillUnlockResult result,
            String techniqueId,
            String nodeId,
            String unlockTag)
    {
        SkillNetwork.sendUnlockSkillResult(player, result, techniqueId, nodeId, unlockTag);
    }

    public static void sendReverseTechniqueTickToServer()
    {
        TechniqueNetwork.sendReverseTechniqueTickToServer();
    }

    public static void sendEmotionalReleaseToServer(byte action)
    {
        TechniqueNetwork.sendEmotionalReleaseToServer(action);
    }

    public static void sendEmotionalReleaseStateSync(
            ServerPlayer player,
            boolean charging,
            long chargeStartGameTime,
            long cooldownUntilGameTime)
    {
        TechniqueNetwork.sendEmotionalReleaseStateSync(player, charging, chargeStartGameTime, cooldownUntilGameTime);
    }

    public static void sendBlackFlashScreenShakeSync(ServerPlayer player, int durationTicks, float maxDegrees)
    {
        VisualNetwork.sendBlackFlashScreenShakeSync(player, durationTicks, maxDegrees);
    }

    public static void sendScreenShake(
            ServerPlayer player,
            ScreenShakeKind kind,
            int durationTicks,
            float amplitude,
            boolean sustainedActive)
    {
        VisualNetwork.sendScreenShake(player, kind, durationTicks, amplitude, sustainedActive);
    }

    public static void sendScreenShake(ServerPlayer player, ScreenShakeKind kind, int durationTicks, float amplitude)
    {
        VisualNetwork.sendScreenShake(player, kind, durationTicks, amplitude);
    }

    public static void sendShrineBlackFlashMilestoneSync(ServerPlayer player, int count)
    {
        VisualNetwork.sendShrineBlackFlashMilestoneSync(player, count);
    }

    public static void sendTechniqueSkillUseToServer(int slot)
    {
        SkillNetwork.sendTechniqueSkillUseToServer(slot);
    }

    public static void sendTechniqueSpecialMoveToServer(int modifierMask, int baseSlot)
    {
        TechniqueNetwork.sendTechniqueSpecialMoveToServer(modifierMask, baseSlot);
    }

    public static void sendAdjustCastRangeTierToServer(int delta)
    {
        TechniqueNetwork.sendAdjustCastRangeTierToServer(delta);
    }

    public static void sendAdjustBarrierRadiusTierToServer(int delta)
    {
        DomainNetwork.sendAdjustBarrierRadiusTierToServer(delta);
    }

    public static void sendTechniqueMaxOutputToServer(byte action, int slot)
    {
        TechniqueNetwork.sendTechniqueMaxOutputToServer(action, slot);
    }

    public static void sendTechniqueHoldReleaseToServer(byte action, int slot)
    {
        TechniqueNetwork.sendTechniqueHoldReleaseToServer(action, slot);
    }

    public static void sendMaxOutputChargeInterruptSync(ServerPlayer player)
    {
        TechniqueNetwork.sendMaxOutputChargeInterruptSync(player);
    }

    public static void sendBarrierTechniqueCastToServer()
    {
        DomainNetwork.sendBarrierTechniqueCastToServer();
    }

    public static void sendBarrierRulesToServer(boolean blockMobSpawn, boolean autoRepairBarrier)
    {
        DomainNetwork.sendBarrierRulesToServer(blockMobSpawn, autoRepairBarrier);
    }

    public static void sendRepairBarrierToServer()
    {
        DomainNetwork.sendRepairBarrierToServer();
    }

    public static void sendRepairMeltdownToServer()
    {
        DomainNetwork.sendRepairMeltdownToServer();
    }

    public static void sendDomainExpansionCastToServer()
    {
        DomainNetwork.sendDomainExpansionCastToServer();
    }

    public static void sendShrineDomainTeleportToServer(float facingYaw)
    {
        DomainNetwork.sendShrineDomainTeleportToServer(facingYaw);
    }

    public static void sendShrineDomainLockOnToServer(boolean locking, java.util.UUID targetUuid)
    {
        DomainNetwork.sendShrineDomainLockOnToServer(locking, targetUuid);
    }

    public static void sendShrineDomainActiveSync(ServerPlayer player, boolean active)
    {
        DomainNetwork.sendShrineDomainActiveSync(player, active);
    }

    public static void sendShrinePowerSlashChargeSync(ServerPlayer player, boolean visible, float progress)
    {
        DomainNetwork.sendShrinePowerSlashChargeSync(player, visible, progress);
    }

    public static void sendJudgmentTrialLockOnToServer(boolean locking, java.util.UUID targetUuid)
    {
        DomainNetwork.sendJudgmentTrialLockOnToServer(locking, targetUuid);
    }

    public static void sendJudgmentTrialActiveSync(ServerPlayer player, boolean active)
    {
        DomainNetwork.sendJudgmentTrialActiveSync(player, active);
    }

    public static void sendJudgmentTrialChargeSync(ServerPlayer player, boolean visible, float progress)
    {
        DomainNetwork.sendJudgmentTrialChargeSync(player, visible, progress);
    }

    public static void sendJudgmentAccuseChoiceSync(
            ServerPlayer player,
            boolean active,
            int remainingTicks,
            String crimeChinese,
            String crimeEnglish)
    {
        DomainNetwork.sendJudgmentAccuseChoiceSync(player, active, remainingTicks, crimeChinese, crimeEnglish);
    }

    public static void sendJudgmentAccuseChoiceToServer(int responseId)
    {
        DomainNetwork.sendJudgmentAccuseChoiceToServer(responseId);
    }

    public static void sendJudgmentDomainCmdCamSetup(ServerPlayer caster, net.minecraft.core.BlockPos domainCenter)
    {
        DomainNetwork.sendJudgmentDomainCmdCamSetup(caster, domainCenter);
    }

    public static void sendJudgmentDomainColorCycleOverlaySync(ServerPlayer player)
    {
        VisualNetwork.sendJudgmentDomainColorCycleOverlaySync(player);
    }

    public static void sendJudgmentTrialOpeningPerformance(ServerPlayer caster, float playerYRot)
    {
        DomainNetwork.sendJudgmentTrialOpeningPerformance(caster, playerYRot);
    }

    public static void sendJudgmentJudgeAngryPerformance(ServerPlayer caster, double judgeX, double judgeY, double judgeZ, float judgeYRot)
    {
        DomainNetwork.sendJudgmentJudgeAngryPerformance(caster, judgeX, judgeY, judgeZ, judgeYRot);
    }

    public static void sendExecutionersSwordActivationSync(ServerPlayer player)
    {
        DomainNetwork.sendExecutionersSwordActivationSync(player);
    }

    public static void sendWhiteOverlaySync(ServerPlayer player, boolean enabled, float alpha)
    {
        VisualNetwork.sendWhiteOverlaySync(player, enabled, alpha);
    }

    public static void sendWhiteOverlayAnimationSync(ServerPlayer player, int totalTicks, int fadeInTicks, int fadeOutTicks, float peakAlpha)
    {
        VisualNetwork.sendWhiteOverlayAnimationSync(player, totalTicks, fadeInTicks, fadeOutTicks, peakAlpha);
    }

    public static void sendPurpleOverlayAnimationSync(ServerPlayer player)
    {
        VisualNetwork.sendPurpleOverlayAnimationSync(player);
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
        VisualNetwork.sendColoredWorldOverlayAnimationSync(player, colorR, colorG, colorB, totalTicks, fadeInTicks, fadeOutTicks, peakAlpha);
    }

    public static void sendFullScreenWorldFlashAnimationSync(ServerPlayer player, int totalTicks, int fadeInTicks, int fadeOutTicks, float peakAlpha)
    {
        VisualNetwork.sendFullScreenWorldFlashAnimationSync(player, totalTicks, fadeInTicks, fadeOutTicks, peakAlpha);
    }

    public static void sendDomainWhiteCurtainSync(ServerPlayer player, int durationTicks)
    {
        VisualNetwork.sendDomainWhiteCurtainSync(player, durationTicks);
    }

    public static void sendDomainWhiteCurtainElevateSync(ServerPlayer player)
    {
        VisualNetwork.sendDomainWhiteCurtainElevateSync(player);
    }

    public static void sendCinematicLetterboxSync(ServerPlayer player, int totalTicks, int fadeInTicks, int fadeOutTicks, float barFraction)
    {
        VisualNetwork.sendCinematicLetterboxSync(player, totalTicks, fadeInTicks, fadeOutTicks, barFraction);
    }

    public static void sendDomainScanWaveSync(ServerPlayer player, double centerX, double centerY, double centerZ, int totalTicks, float peakRadius)
    {
        VisualNetwork.sendDomainScanWaveSync(player, centerX, centerY, centerZ, totalTicks, peakRadius);
    }

    public static void sendSpaceWarpSync(ServerPlayer player, boolean enabled, float intensity)
    {
        VisualNetwork.sendSpaceWarpSync(player, enabled, intensity);
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
        VisualNetwork.sendInstantDomainCrossResidualSync(player, x, y, z, durationTicks, revolutions, textureFolder);
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
        VisualNetwork.sendOpenDomainCubemapSync(player, active, x, y, z, radius, textureFolder);
    }

    public static void sendLimitlessDomainGarbledBurst(ServerPlayer player, double x, double y, double z)
    {
        VisualNetwork.sendLimitlessDomainGarbledBurst(player, x, y, z);
    }

    public static void sendFugaBoomFlashSequenceSync(ServerPlayer player)
    {
        VisualNetwork.sendFugaBoomFlashSequenceSync(player);
    }

    public static void sendLimitlessDomainAmbientSoundStart(ServerPlayer player, UUID casterUuid, double x, double y, double z)
    {
        VisualNetwork.sendLimitlessDomainAmbientSoundStart(player, casterUuid, x, y, z);
    }

    public static void sendLimitlessDomainAmbientSoundStop(ServerPlayer player, UUID casterUuid)
    {
        VisualNetwork.sendLimitlessDomainAmbientSoundStop(player, casterUuid);
    }

    public static void sendLimitlessDomainCmdCamSetupA(ServerPlayer caster, Vec3 playerPos)
    {
        DomainNetwork.sendLimitlessDomainCmdCamSetupA(caster, playerPos);
    }

    public static void sendShrineDomainCmdCamSetup(ServerPlayer caster, Vec3 playerPos, float playerYRot)
    {
        DomainNetwork.sendShrineDomainCmdCamSetup(caster, playerPos, playerYRot);
    }

    public static void sendShrineCastFilterSync(ServerPlayer player, boolean enabled)
    {
        VisualNetwork.sendShrineCastFilterSync(player, enabled);
    }

    public static void sendShrineSlashLoopSoundStart(ServerPlayer player, UUID casterUuid)
    {
        VisualNetwork.sendShrineSlashLoopSoundStart(player, casterUuid);
    }

    public static void sendShrineSlashLoopSoundStop(ServerPlayer player, UUID casterUuid)
    {
        VisualNetwork.sendShrineSlashLoopSoundStop(player, casterUuid);
    }

    public static void sendOwnerOnlyPhotonMount(ServerPlayer player, int entityId, ResourceLocation fxId, OwnerOnlyPhotonFxParams params)
    {
        VisualNetwork.sendOwnerOnlyPhotonMount(player, entityId, fxId, params);
    }

    public static void sendOwnerOnlyPhotonRemove(ServerPlayer player, int entityId, ResourceLocation fxId, boolean force)
    {
        VisualNetwork.sendOwnerOnlyPhotonRemove(player, entityId, fxId, force);
    }

    public static void sendPlayerInputLockSync(ServerPlayer player, boolean locked)
    {
        VisualNetwork.sendPlayerInputLockSync(player, locked);
    }

    public static void sendSkillBarConfigToServer(String techniqueId, String[] skillIds)
    {
        SkillNetwork.sendSkillBarConfigToServer(techniqueId, skillIds);
    }

    public static void sendSkillBarConfigSync(ServerPlayer player, String techniqueId, String[] skillIds)
    {
        SkillNetwork.sendSkillBarConfigSync(player, techniqueId, skillIds);
    }

    public static void sendTenShadowsModeSync(ServerPlayer player, boolean active)
    {
        TechniqueNetwork.sendTenShadowsModeSync(player, active);
    }

    public static void sendTenShadowsShikigamiToServer(int shikigamiIndex)
    {
        TechniqueNetwork.sendTenShadowsShikigamiToServer(shikigamiIndex);
    }

    public static void sendShikigamiAbilityToServer(int shikigamiIndex, int modifierMask)
    {
        TechniqueNetwork.sendShikigamiAbilityToServer(shikigamiIndex, modifierMask);
    }

    public static void sendTenShadowsContractSync(ServerPlayer player, int tamedMask)
    {
        TechniqueNetwork.sendTenShadowsContractSync(player, tamedMask);
    }

    public static void sendTenShadowsFalseDeathSync(net.minecraft.world.entity.LivingEntity entity, boolean active)
    {
        TechniqueNetwork.sendTenShadowsFalseDeathSync(entity, active);
    }

    public static void sendToggleMimicryToServer()
    {
        TechniqueNetwork.sendToggleMimicryToServer();
    }

    public static void sendMimicrySync(ServerPlayer subject)
    {
        TechniqueNetwork.sendMimicrySync(subject);
    }

    public static void sendMimicrySyncTo(ServerPlayer receiver, ServerPlayer subject)
    {
        TechniqueNetwork.sendMimicrySyncTo(receiver, subject);
    }

}
