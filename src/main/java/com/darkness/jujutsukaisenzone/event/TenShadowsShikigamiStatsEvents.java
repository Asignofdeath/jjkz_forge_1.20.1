package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.technique.skill.tenshadows.TenShadowsShikigamiStatsApplier;
import com.darkness.jujutsukaisenzone.technique.skill.tenshadows.TenShadowsShikigamiStatsReloadListener;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TenShadowsShikigamiStatsEvents
{
    private TenShadowsShikigamiStatsEvents()
    {
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event)
    {
        event.addListener(new TenShadowsShikigamiStatsReloadListener());
    }

    /** 晚于原版 mob_combat_stats，用快照盖回十影面板。 */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onEntityJoinLevel(EntityJoinLevelEvent event)
    {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof LivingEntity living))
        {
            return;
        }
        if (living instanceof Player)
        {
            return;
        }
        if (TenShadowsShikigamiStatsApplier.isTenShadowsStatEntity(living))
        {
            TenShadowsShikigamiStatsApplier.reapplyFromEntity(living);
        }
    }
}
