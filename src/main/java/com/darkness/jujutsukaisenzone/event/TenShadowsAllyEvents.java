package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.technique.skill.TenShadowsSummonLogic;
import net.minecraft.server.level.ServerLevel;
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

import java.util.Optional;
import java.util.UUID;

/**
 * 已调伏友方式神：禁止把主人当仇恨目标，也禁止打伤主人。
 * <p>
 * 占位体多为原版敌对生物（Phantom / Hoglin / Warden 等），默认 AI 会咬主人。
 * 魔虚罗（坚守者）还会因脚步振动对主人积攒愤怒并追击——需每 tick 清除。
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
            if (self instanceof Warden warden)
            {
                warden.clearAnger(newTarget);
                clearWardenHuntMemories(warden);
            }
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
            clearHostileTowardAllies(livingAttacker);
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
            clearHostileTowardAllies(mob);
        }
        if (mob instanceof Warden warden)
        {
            suppressWardenAngerAtAllies(warden);
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

    private static void clearHostileTowardAllies(LivingEntity entity)
    {
        if (entity instanceof Mob mob)
        {
            mob.setTarget(null);
        }
        if (entity instanceof Warden warden)
        {
            clearWardenHuntMemories(warden);
            suppressWardenAngerAtAllies(warden);
        }
    }

    /**
     * 清除坚守者对主人 / 友方式神的愤怒与扰动追踪，避免「打不到但仍追着打」。
     */
    private static void suppressWardenAngerAtAllies(Warden warden)
    {
        UUID ownerId = TenShadowsSummonLogic.getOwnerId(warden);
        if (ownerId == null || !(warden.level() instanceof ServerLevel level))
        {
            return;
        }
        Entity owner = level.getPlayerByUUID(ownerId);
        if (owner == null)
        {
            owner = level.getEntity(ownerId);
        }
        if (owner != null)
        {
            warden.clearAnger(owner);
        }

        Optional<LivingEntity> active = warden.getAngerManagement().getActiveEntity();
        if (active.isPresent() && isOwnerOrAlly(warden, active.get()))
        {
            warden.clearAnger(active.get());
            clearWardenHuntMemories(warden);
        }

        warden.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).ifPresent(attack ->
        {
            if (isOwnerOrAlly(warden, attack))
            {
                clearWardenHuntMemories(warden);
                warden.setTarget(null);
            }
        });
        warden.getBrain().getMemory(MemoryModuleType.ROAR_TARGET).ifPresent(roar ->
        {
            if (isOwnerOrAlly(warden, roar))
            {
                clearWardenHuntMemories(warden);
            }
        });
    }

    private static void clearWardenHuntMemories(Warden warden)
    {
        warden.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        warden.getBrain().eraseMemory(MemoryModuleType.ROAR_TARGET);
        warden.getBrain().eraseMemory(MemoryModuleType.DISTURBANCE_LOCATION);
    }
}
