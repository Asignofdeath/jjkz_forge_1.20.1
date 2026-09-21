package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.gameplay.ModRules;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class HungerEvents
{
    private static final Map<UUID, Float> LAST_EXHAUSTION = new ConcurrentHashMap<>();

    private HungerEvents()
    {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide())
        {
            return;
        }

        Player player = event.player;
        UUID uuid = player.getUUID();
        var foodData = player.getFoodData();
        float current = foodData.getExhaustionLevel();
        Float previousObj = LAST_EXHAUSTION.putIfAbsent(uuid, current);
        float previous = previousObj == null ? current : previousObj;

        float gained = current - previous;
        float multiplier = ModRules.HungerConsumption.getMode().multiplier();
        if (gained > 0.0f && multiplier < 1.0f)
        {
            float target = previous + gained * multiplier;
            // 通过负值抵消超出的 Exhaustion，达到全局倍率效果。
            foodData.addExhaustion(target - current);
            current = foodData.getExhaustionLevel();
        }

        LAST_EXHAUSTION.put(uuid, current);
    }
}
