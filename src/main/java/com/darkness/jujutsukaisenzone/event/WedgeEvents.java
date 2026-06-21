package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.entity.WedgeEntity;
import com.darkness.jujutsukaisenzone.gameplay.TengenWedgeBarrierLogic;
import com.darkness.jujutsukaisenzone.gameplay.TengenWedgeBarrierState;
import com.darkness.jujutsukaisenzone.gameplay.TengenWedgeCurseSpiritSpawns;
import com.darkness.jujutsukaisenzone.gameplay.TengenWedgeScoreboard;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WedgeEvents
{
    private WedgeEvents()
    {
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event)
    {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof WedgeEntity wedge))
        {
            return;
        }
        String factionId = wedge.getFactionId();
        if (!WedgeEntity.isPlayableFaction(factionId))
        {
            event.setCanceled(true);
            return;
        }
        if (WedgeEntity.countInFaction(event.getLevel(), factionId, wedge)
                >= WedgeEntity.MAX_PER_FACTION_PER_DIMENSION)
        {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide())
        {
            return;
        }
        if (event.level instanceof ServerLevel serverLevel)
        {
            TengenWedgeBarrierLogic.tickLevel(serverLevel);
            TengenWedgeCurseSpiritSpawns.tickLevel(serverLevel);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide)
        {
            return;
        }
        if (!(event.player instanceof ServerPlayer player))
        {
            return;
        }
        if (player.tickCount % 10 != 0)
        {
            return;
        }

        TengenWedgeBarrierLogic.TengenWedgeBarrier participantBarrier =
                TengenWedgeBarrierLogic.findParticipantBarrierInside(player);
        if (participantBarrier != null)
        {
            TengenWedgeScoreboard.updateTeammates(
                    player,
                    TengenWedgeBarrierLogic.collectTeammateNames(player, participantBarrier));
            return;
        }

        TengenWedgeBarrierLogic.TengenWedgeBarrier barrier = TengenWedgeBarrierLogic.findOwnedBarrierInside(player);
        if (barrier == null)
        {
            TengenWedgeScoreboard.hide(player);
            return;
        }

        if (barrier.getState() == TengenWedgeBarrierState.IN_PROGRESS)
        {
            TengenWedgeScoreboard.hide(player);
            return;
        }

        int wedgeCount = TengenWedgeBarrierLogic.countWedgesInside(
                player.serverLevel(),
                barrier.getCenter(),
                barrier.getRadius());
        int spawnCount = TengenWedgeCurseSpiritSpawns.countForBarrier(barrier);
        TengenWedgeScoreboard.update(player, barrier.getCompletionPercent(), wedgeCount, spawnCount);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player)
        {
            TengenWedgeScoreboard.hide(player);
        }
    }
}
