package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.effect.CorpseDesecrationFoodRules;
import com.darkness.jujutsukaisenzone.effect.SinfulOneEffect;
import com.darkness.jujutsukaisenzone.player.PlayerCrimeRecordData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 食用怪物掉落食物时，记录「侮辱尸体」罪行。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SinfulOneCorpseDesecrationEvents
{
    private SinfulOneCorpseDesecrationEvents()
    {
    }

    @SubscribeEvent
    public static void onUseItemFinish(LivingEntityUseItemEvent.Finish event)
    {
        if (event.getEntity().level().isClientSide())
        {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player))
        {
            return;
        }
        if (!isEligiblePlayer(player))
        {
            return;
        }

        ItemStack stack = event.getItem();
        if (!CorpseDesecrationFoodRules.isMonsterDropFood(stack))
        {
            return;
        }

        SinfulOneEffect.apply(
                player,
                PlayerCrimeRecordData.Category.CORPSE_DESECRATION,
                CorpseDesecrationFoodRules.CRIME_DESCRIPTION,
                player.blockPosition());
    }

    private static boolean isEligiblePlayer(ServerPlayer player)
    {
        return !player.isCreative() && !player.isSpectator();
    }
}
