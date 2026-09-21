package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.effect.IllegalBorderCrossingRules;
import com.darkness.jujutsukaisenzone.effect.SinfulOneEffect;
import com.darkness.jujutsukaisenzone.player.PlayerCrimeRecordData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 从主世界进入下界或末地时，记录「非法穿越边境」罪行。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SinfulOneBorderCrossingEvents
{
    private SinfulOneBorderCrossingEvents()
    {
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player))
        {
            return;
        }
        if (!isEligiblePlayer(player))
        {
            return;
        }

        String description = IllegalBorderCrossingRules.getCrimeDescription(event.getFrom(), event.getTo());
        if (description == null)
        {
            return;
        }

        SinfulOneEffect.apply(
                player,
                PlayerCrimeRecordData.Category.ILLEGAL_BORDER_CROSSING,
                description,
                player.blockPosition());
    }

    private static boolean isEligiblePlayer(ServerPlayer player)
    {
        return !player.isCreative() && !player.isSpectator();
    }
}
