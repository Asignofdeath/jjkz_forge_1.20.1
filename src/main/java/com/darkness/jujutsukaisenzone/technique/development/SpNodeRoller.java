package com.darkness.jujutsukaisenzone.technique.development;

import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

/**
 * 按货架档位权重 roll SP 节点实例。
 */
public final class SpNodeRoller
{
    private SpNodeRoller()
    {
    }

    public static List<SpNodeInstance> rollMany(RandomSource random, int count, int createdTier)
    {
        List<SpNodeInstance> result = new ArrayList<>(Math.max(0, count));
        for (int i = 0; i < count; i++)
        {
            result.add(rollOne(random, createdTier));
        }
        return result;
    }

    public static SpNodeInstance rollOne(RandomSource random, int createdTier)
    {
        boolean offAxis = random.nextDouble() < SpNodeShelfRules.offAxisChance(createdTier);
        SpNodeFamily family = offAxis ? pickOffAxis(random) : pickPower(random);
        boolean medium = random.nextFloat() < 0.35F;
        int size = medium ? 2 : 1;
        int spCost = size;
        int phys = 0;
        int tech = 0;
        int attr = 0;
        switch (family)
        {
            case PHYSICAL_POWER -> phys = medium ? rand(random, 9, 12) : rand(random, 4, 6);
            case TECHNIQUE_POWER -> tech = medium ? rand(random, 9, 12) : rand(random, 4, 6);
            case BALANCED ->
            {
                if (medium)
                {
                    phys = rand(random, 4, 5);
                    tech = rand(random, 4, 5);
                }
                else
                {
                    phys = rand(random, 2, 3);
                    tech = rand(random, 2, 3);
                }
            }
            case BREAK_POTENCY -> attr = medium ? rand(random, 18, 25) : rand(random, 8, 12);
            case PHYSICAL_RESISTANCE, TECHNIQUE_RESISTANCE,
                 PHYSICAL_PENETRATION, TECHNIQUE_PENETRATION ->
                    attr = medium ? rand(random, 7, 10) : rand(random, 3, 5);
            case REVERSE_EFFICIENCY, DOMAIN_CHARGE ->
                    attr = medium ? rand(random, 12, 18) : rand(random, 5, 8);
            case CRIT_CHANCE -> attr = medium ? rand(random, 4, 6) : rand(random, 2, 3);
        }
        return SpNodeInstance.create(family, size, spCost, phys, tech, attr, createdTier);
    }

    private static SpNodeFamily pickPower(RandomSource random)
    {
        float roll = random.nextFloat();
        if (roll < 0.40F)
        {
            return SpNodeFamily.PHYSICAL_POWER;
        }
        if (roll < 0.80F)
        {
            return SpNodeFamily.TECHNIQUE_POWER;
        }
        return SpNodeFamily.BALANCED;
    }

    private static SpNodeFamily pickOffAxis(RandomSource random)
    {
        // Break 权重略高（唯一破韧来源）
        float roll = random.nextFloat();
        if (roll < 0.28F)
        {
            return SpNodeFamily.BREAK_POTENCY;
        }
        if (roll < 0.40F)
        {
            return SpNodeFamily.PHYSICAL_RESISTANCE;
        }
        if (roll < 0.52F)
        {
            return SpNodeFamily.TECHNIQUE_RESISTANCE;
        }
        if (roll < 0.62F)
        {
            return SpNodeFamily.PHYSICAL_PENETRATION;
        }
        if (roll < 0.72F)
        {
            return SpNodeFamily.TECHNIQUE_PENETRATION;
        }
        if (roll < 0.82F)
        {
            return SpNodeFamily.REVERSE_EFFICIENCY;
        }
        if (roll < 0.92F)
        {
            return SpNodeFamily.DOMAIN_CHARGE;
        }
        return SpNodeFamily.CRIT_CHANCE;
    }

    private static int rand(RandomSource random, int minInclusive, int maxInclusive)
    {
        if (maxInclusive <= minInclusive)
        {
            return minInclusive;
        }
        return minInclusive + random.nextInt(maxInclusive - minInclusive + 1);
    }
}
