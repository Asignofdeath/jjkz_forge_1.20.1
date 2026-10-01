package com.darkness.jujutsukaisenzone.gameplay;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.jetbrains.annotations.Nullable;

/**
 * 咒灵最大生命量级表（设计锚，供实体接入时读取）。
 * <p>
 * 对齐句：同级咒灵生命 ≈ 同级玩家的 1～3 倍；高一级起跳一个数量级；
 * 特级不拆档，个体在宽区间内取值。
 * <table>
 *   <caption>量级</caption>
 *   <tr><th>等级</th><th>区间</th></tr>
 *   <tr><td>四级</td><td>300～800</td></tr>
 *   <tr><td>三级</td><td>2 000～8 000</td></tr>
 *   <tr><td>二级</td><td>20 000～80 000</td></tr>
 *   <tr><td>一级</td><td>80 000～150 000</td></tr>
 *   <tr><td>特级</td><td>200 000～2 000 000</td></tr>
 * </table>
 */
public final class CursedSpiritHealthTable
{
    /** 四级：数百 */
    public static final Band GRADE_4 = new Band(300.0D, 800.0D);
    /** 三级：数千 */
    public static final Band GRADE_3 = new Band(2_000.0D, 8_000.0D);
    /** 二级：数万 */
    public static final Band GRADE_2 = new Band(20_000.0D, 80_000.0D);
    /** 一级：约十万 */
    public static final Band GRADE_1 = new Band(80_000.0D, 150_000.0D);
    /** 特级：20万～200万（同级跨度大，不拆档） */
    public static final Band SPECIAL = new Band(200_000.0D, 2_000_000.0D);

    private CursedSpiritHealthTable()
    {
    }

    public static Band band(CursedSpiritGrade grade)
    {
        return switch (grade)
        {
            case GRADE_4 -> GRADE_4;
            case GRADE_3 -> GRADE_3;
            case GRADE_2 -> GRADE_2;
            case GRADE_1 -> GRADE_1;
            case SPECIAL -> SPECIAL;
        };
    }

    public static Band band(CurseSpiritSpawnGrade spawnGrade)
    {
        return band(CursedSpiritGrade.fromSpawnGrade(spawnGrade));
    }

    @Nullable
    public static Band bandForRank(int rank)
    {
        CursedSpiritGrade grade = CursedSpiritGrade.fromRank(rank);
        return grade == null ? null : band(grade);
    }

    /**
     * 把实体最大生命设为区间内一点，并回满当前生命。
     * 无 {@link Attributes#MAX_HEALTH} 时返回 false。
     */
    public static boolean applyMaxHealth(LivingEntity entity, CursedSpiritGrade grade, RandomSource random)
    {
        return applyMaxHealth(entity, band(grade).roll(random));
    }

    public static boolean applyMaxHealth(LivingEntity entity, double maxHealth)
    {
        AttributeInstance attr = entity.getAttribute(Attributes.MAX_HEALTH);
        if (attr == null)
        {
            return false;
        }
        double clamped = Math.max(1.0D, maxHealth);
        attr.setBaseValue(clamped);
        entity.setHealth((float) clamped);
        return true;
    }

    /**
     * 某一等级的最大生命闭区间 [min, max]。
     */
    public record Band(double min, double max)
    {
        public Band
        {
            if (min <= 0.0D || max < min)
            {
                throw new IllegalArgumentException("invalid health band: " + min + ".." + max);
            }
        }

        /** 区间中点，作「典型」锚。 */
        public double typical()
        {
            return (min + max) * 0.5D;
        }

        /** 在 [min, max] 内均匀随机。 */
        public double roll(RandomSource random)
        {
            if (max <= min)
            {
                return min;
            }
            return Mth.lerp(random.nextDouble(), min, max);
        }

        /** 按 0～1 插值：0=下限，1=上限（特级弱/强个体定值用）。 */
        public double lerp(double t)
        {
            return Mth.lerp(Mth.clamp(t, 0.0D, 1.0D), min, max);
        }

        public double clamp(double value)
        {
            return Mth.clamp(value, min, max);
        }

        public boolean contains(double value)
        {
            return value >= min && value <= max;
        }
    }
}
