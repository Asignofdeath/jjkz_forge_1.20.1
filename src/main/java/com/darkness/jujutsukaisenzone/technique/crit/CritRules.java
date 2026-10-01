package com.darkness.jujutsukaisenzone.technique.crit;

import com.darkness.jujutsukaisenzone.registry.ModAttributes;
import com.darkness.jujutsukaisenzone.technique.TechniqueDamageSources;
import com.darkness.jujutsukaisenzone.technique.skillset.SkillDamageType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 暴击规则：几率属性 + 随机浮动乘区；无暴击伤害属性。
 * <p>
 * 暴击成功后对伤害做偏斜随机增幅（小放大概率、大放极稀有），兼作「随机浮动乘区」。
 * 黑闪须先暴击（见 {@code BlackFlashEvents}）；强制暴击由 {@link #forceNext} / {@link #consumeForced}。
 */
public final class CritRules
{
    /** 属性默认 5.0 = 5%。 */
    public static final double DEFAULT_CHANCE_POINTS = 5.0D;
    /** 硬顶（百分比点数）。 */
    public static final double CHANCE_HARD_CAP_POINTS = 60.0D;

    private static final Map<UUID, Integer> FORCED_NEXT = new HashMap<>();
    /** 强制暴击时若 &gt;0，用固定倍率代替随机浮动。 */
    private static final Map<UUID, Float> FORCED_FIXED_MULT = new HashMap<>();

    private CritRules()
    {
    }

    /**
     * 哪些分型「有资格」参与暴击：体术与术式。
     * <p>
     * 复合刻意排除：处刑必杀是致死量改写，不该再叠一道暴击浮动。
     * 黑闪主拳本身是体术分型，靠暴击门槛触发，不走复合。
     * <p>
     * ⚠️ 这只是「分型层面的资格」。术式里高频出伤的招式很多，是否<b>真的</b>放行由出伤点表态，
     * 见 {@code TechniqueDamageSources#critAllowed}（术式默认不放行，单发招式走 {@code dealCrit}）。
     */
    public static boolean canCrit(@Nullable SkillDamageType type)
    {
        return type == SkillDamageType.MARTIAL || type == SkillDamageType.TECHNIQUE;
    }

    /**
     * 当前暴击率（0~1），来自属性；无属性时回退默认。
     */
    public static float critChance(@Nullable LivingEntity attacker)
    {
        if (attacker == null)
        {
            return (float) (DEFAULT_CHANCE_POINTS / 100.0D);
        }
        AttributeInstance instance = attacker.getAttribute(ModAttributes.CRIT_CHANCE.get());
        double points = instance == null ? DEFAULT_CHANCE_POINTS : instance.getValue();
        points = Mth.clamp(points, 0.0D, CHANCE_HARD_CAP_POINTS);
        return (float) (points / 100.0D);
    }

    /** 标记下一次可暴击伤害强制暴击（可叠层）；乘区仍走随机浮动。 */
    public static void forceNext(ServerPlayer player)
    {
        FORCED_NEXT.merge(player.getUUID(), 1, Integer::sum);
    }

    /**
     * 标记下一次强制暴击，并用固定倍率结算（例如十划弱点）。
     * {@code fixedMult <= 0} 时与 {@link #forceNext(ServerPlayer)} 相同。
     */
    public static void forceNext(ServerPlayer player, float fixedMult)
    {
        UUID id = player.getUUID();
        FORCED_NEXT.merge(id, 1, Integer::sum);
        if (fixedMult > 0.0F)
        {
            FORCED_FIXED_MULT.put(id, fixedMult);
        }
    }

    public static boolean consumeForced(ServerPlayer player)
    {
        UUID id = player.getUUID();
        Integer left = FORCED_NEXT.get(id);
        if (left == null || left <= 0)
        {
            return false;
        }
        if (left <= 1)
        {
            FORCED_NEXT.remove(id);
        }
        else
        {
            FORCED_NEXT.put(id, left - 1);
        }
        return true;
    }

    public static void clearForced(ServerPlayer player)
    {
        UUID id = player.getUUID();
        FORCED_NEXT.remove(id);
        FORCED_FIXED_MULT.remove(id);
    }

    /**
     * 结算暴击乘区：判定 + 随机浮动。成功时写入 {@link HitCritState} 并返回新伤害。
     */
    public static float applyCritZone(
            ServerPlayer attacker,
            SkillDamageType type,
            float amount,
            RandomSource random)
    {
        if (amount <= 0.0F || !canCrit(type))
        {
            return amount;
        }
        // ⚠️ 这道闸门必须在 consumeForced 之前：被拒的这一击如果已经吃掉了强制暴击标记，
        // 十划弱点击破注入的那一次强制暴击就白丢了（标记是可叠层的全局状态，不是本击属性）。
        if (!TechniqueDamageSources.critAllowed(type))
        {
            return amount;
        }

        boolean forced = consumeForced(attacker);
        boolean crit = forced || random.nextFloat() < critChance(attacker);
        if (!crit)
        {
            return amount;
        }

        if (forced)
        {
            HitCritState.markForced();
        }
        Float fixed = FORCED_FIXED_MULT.remove(attacker.getUUID());
        float variance = (fixed != null && fixed > 0.0F)
                ? fixed
                : rollVarianceMultiplier(random);
        // 记下本击分型：SE 只认体术暴击、黑闪也只从体术系暴击升级（见 HitCritState#critType）。
        HitCritState.markCrit(type, variance);
        return amount * variance;
    }

    /**
     * 偏斜随机增幅：轻/中/重/极四档。
     */
    public static float rollVarianceMultiplier(RandomSource random)
    {
        float roll = random.nextFloat();
        if (roll < 0.55F)
        {
            return lerp(random, 1.10F, 1.25F);
        }
        if (roll < 0.85F)
        {
            return lerp(random, 1.25F, 1.45F);
        }
        if (roll < 0.97F)
        {
            return lerp(random, 1.45F, 1.70F);
        }
        return lerp(random, 1.70F, 2.00F);
    }

    private static float lerp(RandomSource random, float min, float max)
    {
        return min + random.nextFloat() * (max - min);
    }
}
