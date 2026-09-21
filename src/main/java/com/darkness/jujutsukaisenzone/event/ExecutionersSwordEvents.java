package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.effect.DeathSentenceEffect;
import com.darkness.jujutsukaisenzone.technique.skill.JudgmentWeaponLogic;
import com.darkness.jujutsukaisenzone.technique.skill.JudgmentSoundLogic;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 处刑人之剑：对处于【死刑】状态下的生物绝对必杀。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ExecutionersSwordEvents
{
    private static final int REVERT_CHECK_INTERVAL_TICKS = 20;

    private ExecutionersSwordEvents()
    {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
    {
        if (event.getEntity().level().isClientSide() || !(event.getEntity() instanceof ServerPlayer player))
        {
            return;
        }
        JudgmentWeaponLogic.normalizeInventoryGavels(player);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END || event.side.isClient())
        {
            return;
        }
        if (!(event.player instanceof ServerPlayer player))
        {
            return;
        }
        if (player.tickCount % REVERT_CHECK_INTERVAL_TICKS != 0)
        {
            return;
        }
        JudgmentWeaponLogic.tickWeaponRevert(player);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHurt(LivingHurtEvent event)
    {
        if (event.getEntity().level().isClientSide())
        {
            return;
        }
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker))
        {
            return;
        }
        LivingEntity target = event.getEntity();
        if (!DeathSentenceEffect.isActive(target))
        {
            return;
        }
        if (!JudgmentWeaponLogic.isWieldingExecutionersSword(attacker))
        {
            return;
        }

        event.setAmount(JudgmentWeaponLogic.lethalDamageAmount(target));
        JudgmentSoundLogic.playExecution(attacker, target);
    }
}
