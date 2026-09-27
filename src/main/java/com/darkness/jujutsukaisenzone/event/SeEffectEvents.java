package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.player.PlayerCritPowerStackData;
import com.darkness.jujutsukaisenzone.technique.crit.HitCritState;
import com.darkness.jujutsukaisenzone.technique.se.MartialCritSeMechanic;
import com.darkness.jujutsukaisenzone.technique.se.SeMechanicRegistry;
import com.darkness.jujutsukaisenzone.technique.skillset.SkillDamageType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * SE 条件附加式调度：读乘区暴击结果。
 * <p>
 * 机制修改式 SE 不走这里，由各 Rules / Data 主动问 {@link SeMechanicRegistry}。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SeEffectEvents
{
    private SeEffectEvents()
    {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLivingHurt(LivingHurtEvent event)
    {
        if (event.getEntity().level().isClientSide || event.getAmount() <= 0.0F)
        {
            return;
        }
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker))
        {
            return;
        }
        LivingEntity target = event.getEntity();
        if (target == attacker)
        {
            return;
        }

        SkillDamageType critType = HitCritState.critType();
        // 皮质手表：体术 / 术式暴击都叠（含黑闪，因 wasCrit 仍为 true）。
        if (HitCritState.wasCrit()
                && (critType == SkillDamageType.MARTIAL || critType == SkillDamageType.TECHNIQUE))
        {
            PlayerCritPowerStackData.onCrit(attacker);
        }

        // 点燃类：只认体术暴击，且黑闪不算（高频术式暴击不能刷点燃）。
        if (!HitCritState.wasCritForEffects() || critType != SkillDamageType.MARTIAL)
        {
            return;
        }

        for (MartialCritSeMechanic mechanic : SeMechanicRegistry.allEquipped(attacker, MartialCritSeMechanic.class))
        {
            int seconds = mechanic.igniteSeconds();
            if (seconds > 0)
            {
                target.setSecondsOnFire(seconds);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide)
        {
            return;
        }
        if (event.player instanceof ServerPlayer serverPlayer)
        {
            PlayerCritPowerStackData.tick(serverPlayer);
        }
    }
}
