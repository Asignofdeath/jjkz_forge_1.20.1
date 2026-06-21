package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.entity.util.LimitlessRedBlockLaunch;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 赫爆炸：只在爆炸触发瞬间对施法者屏蔽“爆炸伤害”。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RedExplosionImmunityEvents
{
    private static final String TAG_TEMP_RED_EXPLOSION_IMMUNE = "jujutsukaisenzone_red_explosion_immune";

    private RedExplosionImmunityEvents()
    {
    }

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event)
    {
        // 在爆炸结算阶段直接剔除带标记实体，使其不被该次爆炸选中。
        event.getAffectedEntities().removeIf(RedExplosionImmunityEvents::hasTempExplosionImmunity);

        if (event.getLevel().isClientSide() || !(event.getLevel() instanceof ServerLevel level))
        {
            return;
        }

        if (LimitlessRedBlockLaunch.isRedExplosion(event.getExplosion()))
        {
            LimitlessRedBlockLaunch.launchFromExplosion(level, event.getExplosion(), event.getAffectedBlocks());
        }
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event)
    {
        LivingEntity target = event.getEntity();
        if (hasTempExplosionImmunity(target) && event.getSource().is(DamageTypeTags.IS_EXPLOSION))
        {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event)
    {
        LivingEntity target = event.getEntity();
        if (hasTempExplosionImmunity(target) && event.getSource().is(DamageTypeTags.IS_EXPLOSION))
        {
            event.setCanceled(true);
        }
    }

    private static boolean hasTempExplosionImmunity(Entity entity)
    {
        return entity.getPersistentData().getBoolean(TAG_TEMP_RED_EXPLOSION_IMMUNE);
    }
}
