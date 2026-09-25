package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.network.ModNetwork;
import com.darkness.jujutsukaisenzone.advancement.GradeAdvancements;
import com.darkness.jujutsukaisenzone.advancement.HeavenlyRestrictionAdvancements;
import com.darkness.jujutsukaisenzone.player.BarrierRulesData;
import com.darkness.jujutsukaisenzone.player.CursedGradeBonuses;
import com.darkness.jujutsukaisenzone.player.PlayerBarrierTechniqueData;
import com.darkness.jujutsukaisenzone.player.PlayerCursedEnergyData;
import com.darkness.jujutsukaisenzone.player.PlayerCombatPowerData;
import com.darkness.jujutsukaisenzone.player.PlayerCursedGradeData;
import com.darkness.jujutsukaisenzone.player.PlayerDomainExpansionData;
import com.darkness.jujutsukaisenzone.player.PlayerFactionData;
import com.darkness.jujutsukaisenzone.player.PlayerHealthData;
import com.darkness.jujutsukaisenzone.player.PlayerReverseTechniqueData;
import com.darkness.jujutsukaisenzone.player.PlayerShrineBlackFlashMilestoneData;
import com.darkness.jujutsukaisenzone.player.PlayerSkillBarConfig;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueIntelData;
import com.darkness.jujutsukaisenzone.player.PlayerSeanceTechniqueData;
import com.darkness.jujutsukaisenzone.player.PlayerStrengthenTechniqueData;
import com.darkness.jujutsukaisenzone.technique.StrengthenTechniqueLearnLogic;
import com.darkness.jujutsukaisenzone.technique.development.SkillUnlockLogic;
import com.darkness.jujutsukaisenzone.technique.TechniqueDailyUpgradeLogic;
import com.darkness.jujutsukaisenzone.technique.BarrierTechniqueServerLogic;
import com.darkness.jujutsukaisenzone.technique.skillset.TechniqueSkillSetRegistry;
import com.darkness.jujutsukaisenzone.technique.SeanceTechniqueServerLogic;
import com.darkness.jujutsukaisenzone.technique.StrengthenTechniqueServerLogic;
import com.darkness.jujutsukaisenzone.technique.domain.DomainExpansionRouter;
import com.darkness.jujutsukaisenzone.technique.skill.JudgmentAccuseSkillLogic;
import com.darkness.jujutsukaisenzone.technique.skill.JudgmentJudgeEntityLogic;
import com.darkness.jujutsukaisenzone.technique.skill.JudgmentSoundLogic;
import com.darkness.jujutsukaisenzone.technique.skill.JudgmentTrialSkillLogic;
import com.darkness.jujutsukaisenzone.technique.skill.JudgmentTrialStackVisuals;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = com.darkness.jujutsukaisenzone.JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CursedEnergyEvents
{
    private CursedEnergyEvents()
    {
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerLoggedInEvent event)
    {
        Player player = event.getEntity();
        PlayerCursedEnergyData.initializeIfAbsent(player);
        if (player instanceof ServerPlayer serverPlayer)
        {
            syncPlayerState(serverPlayer);
            // 单机/进世界时客户端连接可能尚未就绪，下一 tick 再同步一次快捷栏等状态。
            serverPlayer.server.execute(() -> syncPlayerState(serverPlayer));
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event)
    {
        if (event.getEntity() instanceof ServerPlayer serverPlayer)
        {
            PlayerCursedEnergyData.initializeIfAbsent(serverPlayer);
            // 技能解锁 / 等级写在 persistentData 根标签上，而 Forge 重生只会搬运 PlayerPersisted 子标签，
            // 不补这一步玩家一死就会丢掉全部技能成长（开发树、技能栏、空战能力全部退回未解锁）。
            SkillUnlockLogic.copyProgressionFrom(serverPlayer, event.getOriginal());
            PlayerSkillBarConfig.copyAllFrom(serverPlayer, event.getOriginal());
            syncPlayerState(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerRespawnEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer))
        {
            return;
        }
        // 死亡重生时客户端玩家实体会重建；这里追加一次完整同步，确保术式与招式组条件恢复。
        PlayerCursedEnergyData.initializeIfAbsent(serverPlayer);
        syncPlayerState(serverPlayer);
        PlayerHealthData.restoreToMax(serverPlayer);
        // 下一 tick 再同步一次，避免客户端重建玩家实体窗口期丢失阵营同步，导致咒力 GUI 颜色退回白色。
        serverPlayer.server.execute(() ->
        {
            syncPlayerState(serverPlayer);
            PlayerHealthData.restoreToMax(serverPlayer);
        });
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerChangedDimensionEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer))
        {
            return;
        }
        // 跨维度时客户端 LocalPlayer 会重建，PersistentData 清空；必须重新同步，
        // 否则客户端会把玩家当成无术式状态（术式、技能栏、阵营等全部丢失显示）。
        PlayerCursedEnergyData.initializeIfAbsent(serverPlayer);
        syncPlayerState(serverPlayer);
        // 下一 tick 再同步一次，避开客户端 LocalPlayer 重建窗口期。
        serverPlayer.server.execute(() -> syncPlayerState(serverPlayer));
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide)
        {
            return;
        }

        Player player = event.player;
        PlayerCursedEnergyData.initializeIfAbsent(player);

        if (player instanceof ServerPlayer serverPlayer)
        {
            int rank = PlayerCursedGradeData.getRank(player);
            CursedGradeBonuses.updateSprintSpeedBonus(serverPlayer);
            CursedGradeBonuses.applyResistanceTick(serverPlayer, rank);
            PlayerHealthData.syncFromEntity(serverPlayer);
            BarrierTechniqueServerLogic.staticTickMaintain(serverPlayer);
            SeanceTechniqueServerLogic.tickMaintain(serverPlayer);
            StrengthenTechniqueServerLogic.tickMaintain(serverPlayer);
            TechniqueDailyUpgradeLogic.checkGameDayEnd(serverPlayer);
            DomainExpansionRouter.tickMaintainAll(serverPlayer);
            JudgmentTrialSkillLogic.tickMaintain(serverPlayer);
            JudgmentTrialSkillLogic.tickPendingVerdictTitles(serverPlayer);
            JudgmentAccuseSkillLogic.tickMaintain(serverPlayer);
            JudgmentAccuseSkillLogic.tickPendingJudgeDismiss(serverPlayer);
            JudgmentJudgeEntityLogic.tickPendingAngryAnimation(serverPlayer);
            JudgmentSoundLogic.tickPendingDeathPenalty(serverPlayer);
            JudgmentTrialStackVisuals.tickForCaster(serverPlayer);
            if (BarrierRulesData.isAutoRepairBarrier(serverPlayer.getUUID()))
            {
                BarrierTechniqueServerLogic.staticRepairBarrierGaps(serverPlayer);
                DomainExpansionRouter.repairBarrierGapsAll(serverPlayer);
            }
            if (PlayerCursedEnergyData.isHeavenlyRestricted(serverPlayer))
            {
                HeavenlyRestrictionAdvancements.grant(serverPlayer);
            }
        }

        int interval = CursedGradeBonuses.getRegenIntervalTicks(PlayerCursedGradeData.getRawRank(player));
        if (player.tickCount % interval != 0)
        {
            return;
        }

        int current = PlayerCursedEnergyData.getCurrent(player);
        int max = PlayerCursedEnergyData.getMax(player);
        if (current < max)
        {
            PlayerCursedEnergyData.setCurrent(player, current + 1);
            if (player instanceof ServerPlayer serverPlayer)
            {
                ModNetwork.sendCursedEnergySync(serverPlayer, PlayerCursedEnergyData.getCurrent(player), max);
            }
        }
    }

    private static void syncPlayerState(ServerPlayer serverPlayer)
    {
        CursedGradeBonuses.applyAll(serverPlayer);
        PlayerHealthData.syncFromEntity(serverPlayer);
        PlayerCursedEnergyData.recalculateAfterGradeChange(serverPlayer);
        if (PlayerCursedEnergyData.isHeavenlyRestricted(serverPlayer))
        {
            HeavenlyRestrictionAdvancements.grant(serverPlayer);
        }
        ModNetwork.sendFactionSync(serverPlayer, PlayerFactionData.getFactionId(serverPlayer));
        ModNetwork.sendCursedGradeSync(serverPlayer, PlayerCursedGradeData.getExperience(serverPlayer));
        StrengthenTechniqueLearnLogic.tryUnlockForGrade(serverPlayer);
        ModNetwork.sendReverseTechniqueSync(serverPlayer, PlayerReverseTechniqueData.hasReverseTechnique(serverPlayer));
        PlayerBarrierTechniqueData.syncToClient(serverPlayer);
        ModNetwork.sendDomainExpansionLearnedSync(serverPlayer, PlayerDomainExpansionData.hasLearned(serverPlayer));
        ModNetwork.sendSeanceTechniqueLearnedSync(
                serverPlayer,
                PlayerSeanceTechniqueData.hasLearned(serverPlayer),
                PlayerSeanceTechniqueData.getLevel(serverPlayer).getTier());
        ModNetwork.sendStrengthenTechniqueLearnedSync(
                serverPlayer,
                PlayerStrengthenTechniqueData.hasLearned(serverPlayer),
                PlayerStrengthenTechniqueData.getLevel(serverPlayer).getTier(),
                PlayerStrengthenTechniqueData.isActive(serverPlayer));
        ModNetwork.sendTechniqueSync(serverPlayer, PlayerTechniqueData.getInnateTechnique(serverPlayer));
        PlayerTechniqueIntelData.syncToClient(serverPlayer);
        // 术式情报会影响术式强度，必须在它同步之后下发面板
        PlayerCombatPowerData.syncToClient(serverPlayer);
        SkillUnlockLogic.syncToClient(serverPlayer);
        ModNetwork.sendShrineBlackFlashMilestoneSync(
                serverPlayer,
                PlayerShrineBlackFlashMilestoneData.getCount(serverPlayer));
        TechniqueSkillSetRegistry.syncSkillBarToClient(serverPlayer);
        ModNetwork.sendMimicrySync(serverPlayer);
        GradeAdvancements.syncForPlayer(serverPlayer);
    }
}
