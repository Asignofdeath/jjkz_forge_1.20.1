package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.technique.skill.TenShadowsSummonLogic;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/**
 * 已调伏友方式神：禁止把主人当仇恨目标，也禁止打伤主人。
 * <p>
 * 占位体多为原版敌对生物（Phantom / Hoglin / Warden 等），默认 AI 会咬主人。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TenShadowsAllyEvents
{
    private TenShadowsAllyEvents()
    {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onChangeTarget(LivingChangeTargetEvent event)
    {
        LivingEntity self = event.getEntity();
        if (self.level().isClientSide || !TenShadowsSummonLogic.isFriendlyOwned(self))
        {
            return;
        }
        LivingEntity newTarget = event.getNewTarget();
        if (newTarget != null && isOwnerOrAlly(self, newTarget))
        {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onHurt(LivingHurtEvent event)
    {
        if (event.getEntity().level().isClientSide)
        {
            return;
        }
        LivingEntity victim = event.getEntity();
        Entity attacker = event.getSource().getEntity();
        if (!(attacker instanceof LivingEntity livingAttacker))
        {
            return;
        }
        if (TenShadowsSummonLogic.isFriendlyOwned(livingAttacker) && isOwnerOrAlly(livingAttacker, victim))
        {
            event.setCanceled(true);
            clearHostileTarget(livingAttacker);
        }
    }

    @SubscribeEvent
    public static void onTick(LivingEvent.LivingTickEvent event)
    {
        if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide)
        {
            return;
        }
        if (!TenShadowsSummonLogic.isFriendlyOwned(mob))
        {
            return;
        }
        LivingEntity target = mob.getTarget();
        if (target != null && isOwnerOrAlly(mob, target))
        {
            clearHostileTarget(mob);
        }
    }

    private static boolean isOwnerOrAlly(LivingEntity shikigami, LivingEntity other)
    {
        UUID ownerId = TenShadowsSummonLogic.getOwnerId(shikigami);
        if (ownerId == null || other == null)
        {
            return false;
        }
        if (ownerId.equals(other.getUUID()))
        {
            return true;
        }
        /* 同主人友方式神互不攻击 */
        return TenShadowsSummonLogic.isOwnedBy(other, ownerId);
    }

    private static void clearHostileTarget(LivingEntity entity)
    {
        if (entity instanceof Mob mob)
        {
            mob.setTarget(null);
        }
        if (entity instanceof Warden warden)
        {
            warden.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
            warden.getBrain().eraseMemory(MemoryModuleType.ROAR_TARGET);
        }
    }
}
