package com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability.mahoraga;

import com.darkness.jujutsukaisenzone.player.PlayerFactionData;
import com.darkness.jujutsukaisenzone.technique.TechniqueDamageSources;
import com.darkness.jujutsukaisenzone.technique.skillset.SkillDamageType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;

/**
 * 退魔之剑：秒杀原版敌对怪物，以及咒灵阵营玩家。
 * <p>
 * 咒灵生物尚未实装；日后实体可用同一判定扩展。
 */
public final class MahoragaExtermination
{
    private MahoragaExtermination()
    {
    }

    public static boolean isExterminable(LivingEntity target)
    {
        if (target == null || !target.isAlive())
        {
            return false;
        }
        if (target instanceof ServerPlayer player)
        {
            return PlayerFactionData.FACTION_CURSE_SPIRIT.equals(PlayerFactionData.getFactionId(player));
        }
        return target instanceof Monster || target instanceof Enemy;
    }

    /** @return true 若已秒杀 */
    public static boolean tryKill(LivingEntity target)
    {
        if (!isExterminable(target))
        {
            return false;
        }
        target.kill();
        return true;
    }

    /**
     * @param fallbackSkillRatio 非秒杀目标时的体术技能倍率
     */
    public static void applySwordHit(Mob mahoraga, LivingEntity target, float fallbackSkillRatio)
    {
        if (tryKill(target))
        {
            return;
        }
        TechniqueDamageSources.deal(target, mahoraga, fallbackSkillRatio, SkillDamageType.MARTIAL);
    }
}
