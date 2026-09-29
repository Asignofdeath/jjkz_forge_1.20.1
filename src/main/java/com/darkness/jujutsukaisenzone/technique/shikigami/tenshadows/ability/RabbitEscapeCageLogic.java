package com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability;

import com.darkness.jujutsukaisenzone.technique.shikigami.ShikigamiAbility;
import com.darkness.jujutsukaisenzone.technique.shikigami.ShikigamiAbilitySource;
import com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.TenShadowsFieldQuery;
import com.darkness.jujutsukaisenzone.technique.skill.TenShadowsRitualLogic;
import com.darkness.jujutsukaisenzone.technique.skill.TenShadowsShikigami;
import com.darkness.jujutsukaisenzone.technique.skill.TenShadowsSummonLogic;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * 脱兔 · 囚笼（主动，Ctrl+6）：脱兔围住准星目标并减速。
 */
public final class RabbitEscapeCageLogic
{
    public static final String TAG_CAGE_TICKS = "jujutsukaisenzone_rabbit_cage_ticks";
    public static final String TAG_CAGE_TARGET = "jujutsukaisenzone_rabbit_cage_target";

    private static final double RING_RADIUS = 2.4D;
    private static final int CAGE_TICKS = 20 * 5;
    private static final int SLOW_REFRESH = 20 * 2;

    private RabbitEscapeCageLogic()
    {
    }

    public static boolean cast(ServerPlayer owner, ShikigamiAbility ability, ShikigamiAbilitySource source)
    {
        if (source != ShikigamiAbilitySource.PLAYER)
        {
            return false;
        }
        List<Mob> rabbits = TenShadowsFieldQuery.aliveMobs(owner, TenShadowsShikigami.RABBIT_ESCAPE);
        if (rabbits.isEmpty())
        {
            owner.displayClientMessage(
                    Component.literal("脱兔 · 囚笼：请先召出脱兔").withStyle(ChatFormatting.GRAY),
                    true);
            return false;
        }
        LivingEntity target = TenShadowsRitualLogic.resolveCrosshairLiving(owner, 32.0D);
        if (target == null)
        {
            /* 退而取任意兔的仇恨 */
            for (Mob rabbit : rabbits)
            {
                LivingEntity t = rabbit.getTarget();
                if (t != null && t.isAlive() && !TenShadowsSummonLogic.isOwnedBy(t, owner.getUUID()))
                {
                    target = t;
                    break;
                }
            }
        }
        if (target == null || TenShadowsSummonLogic.isOwnedBy(target, owner.getUUID()))
        {
            owner.displayClientMessage(
                    Component.literal("脱兔 · 囚笼：请准星对准目标").withStyle(ChatFormatting.GRAY),
                    true);
            return false;
        }

        arrangeRing(rabbits, target);
        for (Mob rabbit : rabbits)
        {
            rabbit.setTarget(target);
            rabbit.getPersistentData().putInt(TAG_CAGE_TICKS, CAGE_TICKS);
            rabbit.getPersistentData().putUUID(TAG_CAGE_TARGET, target.getUUID());
        }
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOW_REFRESH, 3, false, true, true));
        owner.displayClientMessage(
                Component.literal("脱兔 · 囚笼 → " + target.getName().getString())
                        .withStyle(ChatFormatting.LIGHT_PURPLE),
                true);
        return true;
    }

    public static void tickRabbit(Mob rabbit)
    {
        if (rabbit.level().isClientSide || !(rabbit.level() instanceof ServerLevel level))
        {
            return;
        }
        if (!TenShadowsSummonLogic.isFriendlyOwned(rabbit)
                || rabbit.getPersistentData().getInt(TenShadowsSummonLogic.TAG_INDEX)
                != TenShadowsShikigami.RABBIT_ESCAPE.index())
        {
            return;
        }
        int remain = rabbit.getPersistentData().getInt(TAG_CAGE_TICKS);
        if (remain <= 0 || !rabbit.getPersistentData().hasUUID(TAG_CAGE_TARGET))
        {
            return;
        }
        remain--;
        rabbit.getPersistentData().putInt(TAG_CAGE_TICKS, remain);
        UUID targetId = rabbit.getPersistentData().getUUID(TAG_CAGE_TARGET);
        Entity entity = level.getEntity(targetId);
        if (!(entity instanceof LivingEntity target) || !target.isAlive())
        {
            clear(rabbit);
            return;
        }
        rabbit.setTarget(target);
        /* 维持环绕：每隔几 tick 校正位置 */
        if (remain % 8 == 0)
        {
            int index = Math.floorMod(rabbit.getId(), 8);
            double angle = (Math.PI * 2.0D * index) / 8.0D + remain * 0.08D;
            double x = target.getX() + Math.cos(angle) * RING_RADIUS;
            double z = target.getZ() + Math.sin(angle) * RING_RADIUS;
            rabbit.getNavigation().moveTo(x, target.getY(), z, 1.35D);
        }
        if (remain % 20 == 0)
        {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOW_REFRESH, 3, false, true, true));
        }
        if (remain <= 0)
        {
            clear(rabbit);
        }
    }

    private static void arrangeRing(List<Mob> rabbits, LivingEntity target)
    {
        int n = rabbits.size();
        for (int i = 0; i < n; i++)
        {
            Mob rabbit = rabbits.get(i);
            double angle = (Math.PI * 2.0D * i) / n;
            double x = target.getX() + Math.cos(angle) * RING_RADIUS;
            double z = target.getZ() + Math.sin(angle) * RING_RADIUS;
            rabbit.teleportTo(x, target.getY(), z);
            rabbit.setTarget(target);
        }
    }

    private static void clear(Mob rabbit)
    {
        rabbit.getPersistentData().remove(TAG_CAGE_TICKS);
        rabbit.getPersistentData().remove(TAG_CAGE_TARGET);
    }
}
