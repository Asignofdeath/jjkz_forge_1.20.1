package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.Config;
import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.player.PlayerCursedGradeData;
import com.darkness.jujutsukaisenzone.player.PlayerFactionData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * 击杀获取咒术经验事件。
 * <p>
 * 属于"咒术师"、"诅咒师"、"咒灵"阵营的玩家击杀生物时获得咒术经验。
 * <ul>
 *   <li>原版生物：按最大生命值计算，上限由配置控制</li>
 *   <li>玩家互杀：根据对方咒术等级给予 30~300 经验</li>
 *   <li>最后一击者获得主要比例，周围同阵营玩家平分剩余部分</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CursedGradeKillEvents
{
    /** 玩家互杀经验表：index = rank（无级~特级） */
    private static final int[] PLAYER_KILL_EXP = {30, 50, 80, 120, 180, 300};

    private CursedGradeKillEvents()
    {
    }

    /**
     * 生物死亡时，若击杀者属于咒术阵营，则分配咒术经验。
     */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event)
    {
        if (event.getEntity().level().isClientSide)
        {
            return;
        }

        // 确认攻击者是玩家
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer))
        {
            return;
        }

        // 确认击杀者属于咒术阵营
        String killerFaction = PlayerFactionData.getFactionId(killer);
        if (!isCursedFaction(killerFaction))
        {
            return;
        }

        LivingEntity victim = event.getEntity();
        int totalExp = calculateExp(victim);
        if (totalExp <= 0)
        {
            return;
        }

        distributeExp(killer, victim, totalExp, killerFaction);
    }

    /**
     * 判断是否为咒术阵营（咒术师、诅咒师、咒灵）。
     */
    private static boolean isCursedFaction(String factionId)
    {
        return PlayerFactionData.FACTION_SORCERER.equals(factionId)
                || PlayerFactionData.FACTION_CURSE_USER.equals(factionId)
                || PlayerFactionData.FACTION_CURSE_SPIRIT.equals(factionId);
    }

    /**
     * 计算被击杀对象提供的总经验值。
     */
    private static int calculateExp(LivingEntity victim)
    {
        // 玩家互杀：根据对方等级
        if (victim instanceof Player victimPlayer)
        {
            int rank = PlayerCursedGradeData.getRank(victimPlayer);
            if (rank < 0 || rank >= PLAYER_KILL_EXP.length)
            {
                rank = 0;
            }
            return PLAYER_KILL_EXP[rank];
        }

        // 原版生物：按最大生命值计算，向上取整，上限由配置控制
        float maxHealth = victim.getMaxHealth();
        int exp = (int) Math.ceil(maxHealth / 10.0F);
        return Math.min(exp, Config.killExpMaxFromMob);
    }

    /**
     * 分配经验给击杀者和周围同阵营玩家。
     */
    private static void distributeExp(ServerPlayer killer, LivingEntity victim, int totalExp, String killerFaction)
    {
        Vec3 victimPos = victim.position();
        double radius = Config.killExpRadius;
        double radiusSq = radius * radius;

        // 收集半径内同阵营的其他玩家（不含击杀者）
        List<ServerPlayer> nearbyAllies = new ArrayList<>();
        for (ServerPlayer other : killer.serverLevel().players())
        {
            if (other == killer)
            {
                continue;
            }
            if (!other.isAlive())
            {
                continue;
            }
            String otherFaction = PlayerFactionData.getFactionId(other);
            if (!killerFaction.equals(otherFaction))
            {
                continue;
            }
            if (other.position().distanceToSqr(victimPos) > radiusSq)
            {
                continue;
            }
            nearbyAllies.add(other);
        }

        // 没有同阵营队友：击杀者获得全部
        if (nearbyAllies.isEmpty())
        {
            PlayerCursedGradeData.addExperience(killer, totalExp);
            return;
        }

        // 有队友：按比例分配
        int killerExp = (int) Math.round(totalExp * Config.killExpLastHitRatio);
        int poolExp = totalExp - killerExp;

        // 击杀者获得主要部分
        if (killerExp > 0)
        {
            PlayerCursedGradeData.addExperience(killer, killerExp);
        }

        // 剩余部分平分给周围同阵营玩家（含击杀者再分一份，即击杀者拿双份）
        int shareCount = nearbyAllies.size() + 1; // +1 表示击杀者也参与平分
        int shareExp = poolExp / shareCount;
        int remainder = poolExp % shareCount;

        if (shareExp > 0)
        {
            // 击杀者再拿一份
            PlayerCursedGradeData.addExperience(killer, shareExp);

            // 周围队友各拿一份
            for (ServerPlayer ally : nearbyAllies)
            {
                PlayerCursedGradeData.addExperience(ally, shareExp);
            }
        }

        // 余数给击杀者（作为最后一击奖励）
        if (remainder > 0)
        {
            PlayerCursedGradeData.addExperience(killer, remainder);
        }
    }
}
