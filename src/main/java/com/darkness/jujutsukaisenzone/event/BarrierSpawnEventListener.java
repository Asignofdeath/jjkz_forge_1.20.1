package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.technique.BarrierTechniqueServerLogic;
import com.darkness.jujutsukaisenzone.technique.domain.DomainExpansionRouter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class BarrierSpawnEventListener
{
    private BarrierSpawnEventListener()
    {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPositionCheck(MobSpawnEvent.PositionCheck event)
    {
        if (event.getResult() == Result.DENY)
        {
            return;
        }

        ServerLevel level = getServerLevel(event.getLevel());
        if (level == null)
        {
            return;
        }

        BlockPos pos = BlockPos.containing(event.getX(), event.getY(), event.getZ());
        // 检查位置是否在启用了"阻止生物生成"的结界或领域内
        if (BarrierTechniqueServerLogic.staticIsPositionBlockingMobSpawn(level, pos)
                || DomainExpansionRouter.isPositionBlockingMobSpawn(level, pos))
        {
            event.setResult(Result.DENY);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event)
    {
        if (event.isSpawnCancelled())
        {
            return;
        }

        ServerLevel level = getServerLevel(event.getLevel());
        if (level == null)
        {
            return;
        }

        BlockPos pos = BlockPos.containing(event.getX(), event.getY(), event.getZ());
        // 检查位置是否在启用了"阻止生物生成"的结界或领域内
        if (BarrierTechniqueServerLogic.staticIsPositionBlockingMobSpawn(level, pos)
                || DomainExpansionRouter.isPositionBlockingMobSpawn(level, pos))
        {
            event.setSpawnCancelled(true);
        }
    }

    private static ServerLevel getServerLevel(ServerLevelAccessor accessor)
    {
        if (accessor instanceof ServerLevel serverLevel)
        {
            return serverLevel;
        }
        return null;
    }
}
