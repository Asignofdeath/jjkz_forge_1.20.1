package com.darkness.jujutsukaisenzone.effect;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;

/**
 * 判定被击杀者是否为「中立 / 友好」目标。
 */
public final class SinfulOneKillRules
{
    private SinfulOneKillRules()
    {
    }

    public static boolean isNeutralOrFriendlyVictim(LivingEntity victim, LivingEntity killer)
    {
        if (victim == killer || killer == null)
        {
            return false;
        }
        if (victim instanceof Player victimPlayer)
        {
            return isFriendlyPlayer(victimPlayer, killer);
        }
        if (victim instanceof Mob mob)
        {
            return isNeutralOrFriendlyMob(mob, killer);
        }
        return false;
    }

    private static boolean isFriendlyPlayer(Player victim, LivingEntity killer)
    {
        if (!(killer instanceof Player killerPlayer))
        {
            return false;
        }
        return victim.isAlliedTo(killer);
    }

    private static boolean isNeutralOrFriendlyMob(Mob victim, LivingEntity killer)
    {
        if (wasHostileTo(victim, killer))
        {
            return false;
        }
        MobCategory category = victim.getType().getCategory();
        if (category != MobCategory.MONSTER)
        {
            return true;
        }
        return isNeutralMonster(victim, killer);
    }

    private static boolean isNeutralMonster(Mob victim, LivingEntity killer)
    {
        if (victim instanceof EnderMan enderman)
        {
            return !enderman.isCreepy();
        }
        if (victim instanceof Wolf wolf)
        {
            return !wolf.isAngry();
        }
        if (victim instanceof ZombifiedPiglin piglin)
        {
            return piglin.getRemainingPersistentAngerTime() <= 0;
        }
        if (victim instanceof Bee bee)
        {
            return bee.getRemainingPersistentAngerTime() <= 0;
        }
        if (victim instanceof PolarBear bear)
        {
            return !bear.isAngry();
        }
        return false;
    }

    private static boolean wasHostileTo(LivingEntity victim, LivingEntity killer)
    {
        if (victim instanceof Mob mob)
        {
            return mob.getTarget() == killer;
        }
        return false;
    }
}
