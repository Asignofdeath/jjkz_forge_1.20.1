package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.effect.SinfulOneEffect;
import com.darkness.jujutsukaisenzone.player.PlayerCrimeRecordData;
import com.darkness.jujutsukaisenzone.player.PlayerIllegalLoggingData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 累计挖掉 50 颗原木时，令玩家获得【罪恶者】并记录「盗伐林木」罪行。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SinfulOneLogMiningEvents
{
    private SinfulOneLogMiningEvents()
    {
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event)
    {
        if (event.getLevel().isClientSide())
        {
            return;
        }
        if (!(event.getPlayer() instanceof ServerPlayer player))
        {
            return;
        }
        if (!isEligiblePlayer(player))
        {
            return;
        }

        BlockState state = event.getState();
        if (!state.is(BlockTags.LOGS))
        {
            return;
        }

        BlockPos brokenPos = event.getPos();
        int count = PlayerIllegalLoggingData.incrementAndGet(player);
        while (count >= PlayerIllegalLoggingData.LOGS_PER_CRIME)
        {
            count -= PlayerIllegalLoggingData.LOGS_PER_CRIME;
            SinfulOneEffect.apply(
                    player,
                    PlayerCrimeRecordData.Category.ILLEGAL_LOGGING,
                    PlayerIllegalLoggingData.CRIME_DESCRIPTION,
                    brokenPos);
        }
        PlayerIllegalLoggingData.setLogsBroken(player, count);
    }

    private static boolean isEligiblePlayer(ServerPlayer player)
    {
        return !player.isCreative() && !player.isSpectator();
    }
}
