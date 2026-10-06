package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.quest.mainline.MainQuestLogic;
import com.darkness.jujutsukaisenzone.quest.mainline.main_1_1.MainQuest11Intro;
import com.darkness.jujutsukaisenzone.quest.mainline.main_1_1.MainQuest11VillageClear;
import com.darkness.jujutsukaisenzone.quest.mainline.main_1_1.MainQuest11VillageNightApproach;
import com.darkness.jujutsukaisenzone.quest.mainline.main_1_2.MainQuest12Anomaly;
import com.darkness.jujutsukaisenzone.quest.mainline.main_1_2.MainQuest12VillageTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import com.darkness.jujutsukaisenzone.quest.mainline.main_1_3.MainQuest13Performance;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import javax.annotation.Nullable;

@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class QuestEvents
{
    private QuestEvents()
    {
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player))
        {
            return;
        }
        MainQuestLogic.bootstrapIfNeeded(player);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player)
        {
            MainQuest11Intro.abort(player);
            MainQuest12Anomaly.abort(player);
            MainQuest13Performance.abort(player);
            MainQuest11VillageNightApproach.clear(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide)
        {
            return;
        }
        if (!(event.player instanceof ServerPlayer serverPlayer))
        {
            return;
        }
        MainQuest11Intro.tick(serverPlayer);
        MainQuest11VillageNightApproach.tick(serverPlayer);
        MainQuest12Anomaly.tick(serverPlayer);
        MainQuest12Anomaly.tickHealthLock(serverPlayer);
        MainQuest13Performance.tick(serverPlayer);
        if (serverPlayer.tickCount % 100 != 0)
        {
            return;
        }
        MainQuestLogic.ensureLocateForTracked(serverPlayer, false);
        MainQuest11VillageClear.tick(serverPlayer);
    }

    /** 最后钳制伤害，避免被其它乘区再抬高后破锁。 */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingHurt(LivingHurtEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide)
        {
            return;
        }
        float clamped = MainQuest12Anomaly.onLockedOrAmbushHurt(player, event.getAmount());
        if (clamped != event.getAmount())
        {
            event.setAmount(clamped);
        }
    }

    @SubscribeEvent
    public static void onContainerOpen(PlayerContainerEvent.Open event)
    {
        if (event.getEntity().level().isClientSide || !(event.getEntity() instanceof ServerPlayer player))
        {
            return;
        }
        if (!MainQuest12Anomaly.isExplorePhase(player))
        {
            return;
        }
        BlockPos pos = resolveContainerBlockPos(event.getContainer());
        if (pos == null)
        {
            return;
        }
        MainQuest12VillageTriggers.onContainerOpen(player, pos);
    }

    @Nullable
    private static BlockPos resolveContainerBlockPos(AbstractContainerMenu menu)
    {
        if (!(menu instanceof ChestMenu chestMenu))
        {
            return null;
        }
        Container container = chestMenu.getContainer();
        if (container instanceof ChestBlockEntity chest || container instanceof BarrelBlockEntity barrel)
        {
            return ((BlockEntity) container).getBlockPos();
        }
        return null;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDeath(LivingDeathEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide)
        {
            return;
        }
        if (MainQuest12Anomaly.preventDeath(player))
        {
            event.setCanceled(true);
        }
    }
}
