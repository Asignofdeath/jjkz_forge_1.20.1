package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 击杀怪物概率掉落「术式情报」物品。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TechniqueIntelligenceDropEvents
{
    private static final float DROP_CHANCE = 0.05F;

    private TechniqueIntelligenceDropEvents()
    {
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event)
    {
        if (event.getEntity().level().isClientSide)
        {
            return;
        }
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer))
        {
            return;
        }
        if (event.getEntity() instanceof Player)
        {
            return;
        }
        if (!(event.getEntity() instanceof Mob))
        {
            return;
        }
        if (killer.getRandom().nextFloat() >= DROP_CHANCE)
        {
            return;
        }

        ItemStack drop = new ItemStack(ModItems.TECHNIQUE_INTELLIGENCE.get());
        event.getEntity().spawnAtLocation(drop);
    }
}
