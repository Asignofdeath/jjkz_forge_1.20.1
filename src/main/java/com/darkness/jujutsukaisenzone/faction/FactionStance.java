package com.darkness.jujutsukaisenzone.faction;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;

/**
 * 本模组自定义实体的立场。不改其它生物的 AI。
 */
public final class FactionStance
{
    private FactionStance()
    {
    }

    /** 攻击方是否把目标当敌人（仅应由本模组自定义实体调用）。 */
    public static boolean canAggress(LivingEntity attacker, LivingEntity target)
    {
        if (attacker == null || target == null || attacker == target)
        {
            return false;
        }
        if (target instanceof ArmorStand || target.isInvulnerable())
        {
            return false;
        }
        return !isNeutral(attacker, target);
    }

    public static boolean isNeutral(LivingEntity attacker, LivingEntity target)
    {
        String from = JjkzFactions.resolve(attacker);
        String to = JjkzFactions.resolve(target);
        if (from.equals(to) && JjkzFactions.isThree(from))
        {
            return true;
        }
        if (JjkzFactions.SORCERER.equals(from) && JjkzFactions.CIVILIAN.equals(to))
        {
            return true;
        }
        if (JjkzFactions.isThree(from) && JjkzFactions.MYSTERIOUS.equals(to))
        {
            return true;
        }
        return FactionTempCoop.isCooperating(attacker, target);
    }
}
