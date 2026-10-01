package com.darkness.jujutsukaisenzone.technique;

import com.darkness.jujutsukaisenzone.technique.skillset.SkillDamageType;
import net.minecraft.server.level.ServerPlayer;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 分型匹配的「下一击增伤」登记处——乘区系统的施术者增强区入口之一。
 * <p>
 * 回答的是以前回答不了的问题：「给这个玩家的<b>下一次术式伤害</b> +X%，体术不受影响」。
 * 分型是<strong>叙事</strong>标签（体 / 术 / 复），与 DamageSource 通道解耦。
 * 技能 / 机制在服务端 {@link #push} 登记加成；伤害落地时由乘区管线
 * {@code TechniqueDamageZoneEvents} 按分型 {@link #consume} 消耗。
 * <p>
 * ⚠️ 纯内存态（静态 Map，服务端主线程独占），<b>不落盘</b>：这是「下一击」性质的
 * 瞬时增益，掉线残留只会白白过期，跨死亡也不该保留——与 {@code PlayerBlackFlashData}
 * 的战斗态同一立场，登出时由 {@code BlackFlashEvents#onPlayerLoggedOut} 清空。
 * <p>
 * 消耗语义（严格匹配）：
 * <ul>
 *   <li>体术伤害只消耗登记为 {@link SkillDamageType#MARTIAL} 的加成</li>
 *   <li>术式伤害只消耗登记为 {@link SkillDamageType#TECHNIQUE} 的加成</li>
 *   <li>复合伤害（处刑人之剑必杀、十划碎块等）一次性消耗全部三类加成</li>
 * </ul>
 * 同分型多次登记按加算叠加（同一乘区内相加，服从「乘区内加算、乘区间乘算」的纪律）。
 */
public final class TechniqueDamageBoosts
{
    /** 每位玩家待消耗的分型加成（百分点，0.5 = +50%）。键为玩家 UUID。 */
    private static final Map<UUID, EnumMap<SkillDamageType, Float>> PENDING = new HashMap<>();

    private TechniqueDamageBoosts()
    {
    }

    /**
     * 登记「该玩家下一次 matching 分型伤害 +bonus」。
     *
     * @param type  加成适用的分型
     * @param bonus 加成比例（0.5 = +50%）；非正值直接忽略
     */
    public static void push(ServerPlayer player, SkillDamageType type, float bonus)
    {
        if (bonus <= 0.0F)
        {
            return;
        }
        PENDING.computeIfAbsent(player.getUUID(), uuid -> new EnumMap<>(SkillDamageType.class))
                .merge(type, bonus, Float::sum);
    }

    /**
     * 取出并清空该玩家匹配 {@code resolvedType} 的待消耗加成。
     *
     * @return 加成总和；无加成返回 0
     */
    public static float consume(ServerPlayer player, SkillDamageType resolvedType)
    {
        EnumMap<SkillDamageType, Float> boosts = PENDING.get(player.getUUID());
        if (boosts == null || boosts.isEmpty())
        {
            return 0.0F;
        }

        float total = 0.0F;
        if (resolvedType == SkillDamageType.COMPOSITE)
        {
            // 复合伤害体术、术式两者都是 ⇒ 三类加成一并消耗
            for (SkillDamageType type : SkillDamageType.values())
            {
                Float bonus = boosts.remove(type);
                if (bonus != null)
                {
                    total += bonus;
                }
            }
        }
        else
        {
            Float bonus = boosts.remove(resolvedType);
            if (bonus != null)
            {
                total += bonus;
            }
        }

        if (boosts.isEmpty())
        {
            PENDING.remove(player.getUUID());
        }
        return total;
    }

    /** 登出 / 战斗态重置时清空，避免静态 Map 残留。 */
    public static void clear(ServerPlayer player)
    {
        PENDING.remove(player.getUUID());
    }
}
