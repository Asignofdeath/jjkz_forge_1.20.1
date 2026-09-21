package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.effect.SinfulOneEffect;
import com.darkness.jujutsukaisenzone.player.PlayerCrimeRecordData;
import com.darkness.jujutsukaisenzone.player.PlayerIllegalMiningData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 累计挖掉超过 10 颗（第 11 颗起）矿石方块时，令玩家获得【罪恶者】并记录「非法采矿」罪行。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SinfulOneOreMiningEvents
{
    private SinfulOneOreMiningEvents()
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
        if (!state.is(Tags.Blocks.ORES))
        {
            return;
        }

        BlockPos brokenPos = event.getPos();
        int count = PlayerIllegalMiningData.incrementAndGet(player);
        while (count >= PlayerIllegalMiningData.ORES_PER_CRIME)
        {
            count -= PlayerIllegalMiningData.ORES_PER_CRIME;
            SinfulOneEffect.apply(
                    player,
                    PlayerCrimeRecordData.Category.ILLEGAL_MINING,
                    PlayerIllegalMiningData.CRIME_DESCRIPTION,
                    brokenPos);
        }
        PlayerIllegalMiningData.setOresBroken(player, count);
    }

    private static boolean isEligiblePlayer(ServerPlayer player)
    {
        return !player.isCreative() && !player.isSpectator();
    }
}
