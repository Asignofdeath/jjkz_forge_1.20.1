package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.technique.skill.TenShadowsShikigami;
import com.darkness.jujutsukaisenzone.technique.skill.TenShadowsSummonLogic;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Unit;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 十种影法术魔虚罗占位（坚守者）：关黑暗 / 失明，并禁止挖地逃走。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TenShadowsWardenEvents
{
    /** 略大于原版坚守者黑暗作用范围，避免漏拦。 */
    private static final double DARKNESS_DENY_RANGE = 24.0D;

    private TenShadowsWardenEvents()
    {
    }

    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event)
    {
        MobEffectInstance instance = event.getEffectInstance();
        if (instance == null || !isVisionDebuff(instance))
        {
            return;
        }
        if (isNearTenShadowsMahoraga(event.getEntity()))
        {
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event)
    {
        MobEffectInstance instance = event.getEffectInstance();
        if (instance == null || !isVisionDebuff(instance))
        {
            return;
        }
        LivingEntity entity = event.getEntity();
        if (!isNearTenShadowsMahoraga(entity))
        {
            return;
        }
        entity.removeEffect(MobEffects.DARKNESS);
        entity.removeEffect(MobEffects.BLINDNESS);
    }

    /** 持续刷新挖地冷却，并打断已开始的挖掘 / 钻出姿势。 */
    @SubscribeEvent
    public static void onMahoragaTick(LivingEvent.LivingTickEvent event)
    {
        if (!(event.getEntity() instanceof Warden warden) || warden.level().isClientSide)
        {
            return;
        }
        if (!isTenShadowsMahoraga(warden))
        {
            return;
        }
        // setDigCooldown() 在当前映射下不可见，直接写脑记忆
        warden.getBrain().setMemoryWithExpiry(MemoryModuleType.DIG_COOLDOWN, Unit.INSTANCE, 1200L);
        Pose pose = warden.getPose();
        if (pose == Pose.DIGGING || pose == Pose.EMERGING)
        {
            warden.setPose(Pose.STANDING);
        }
    }

    private static boolean isVisionDebuff(MobEffectInstance instance)
    {
        return instance.getEffect() == MobEffects.DARKNESS
                || instance.getEffect() == MobEffects.BLINDNESS;
    }

    private static boolean isNearTenShadowsMahoraga(LivingEntity target)
    {
        if (target == null || !(target.level() instanceof ServerLevel level))
        {
            return false;
        }
        AABB box = target.getBoundingBox().inflate(DARKNESS_DENY_RANGE);
        for (Warden warden : level.getEntitiesOfClass(Warden.class, box))
        {
            if (isTenShadowsMahoraga(warden))
            {
                return true;
            }
        }
        return false;
    }

    private static boolean isTenShadowsMahoraga(Warden warden)
    {
        if (!warden.getPersistentData().hasUUID(TenShadowsSummonLogic.TAG_OWNER))
        {
            return false;
        }
        return warden.getPersistentData().getInt(TenShadowsSummonLogic.TAG_INDEX)
                == TenShadowsShikigami.MAHORAGA.index();
    }
}
