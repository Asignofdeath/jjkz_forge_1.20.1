package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.technique.crit.HitCritState;
import com.darkness.jujutsukaisenzone.technique.se.MartialCritSeMechanic;
import com.darkness.jujutsukaisenzone.technique.se.SeMechanicRegistry;
import com.darkness.jujutsukaisenzone.technique.skillset.SkillDamageType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * SE 条件附加式调度：读乘区暴击结果（黑闪不算暴击触发）。
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
        // ⚠️ 必须同时校验分型：只认体术暴击。
        // 放开术式暴击后，只判 wasCritForEffects 会让术式暴击也点亮点燃类效果，
        // 而术式的高频出伤会把它刷成常驻。
        if (!HitCritState.wasCritForEffects() || HitCritState.critType() != SkillDamageType.MARTIAL)
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
}
