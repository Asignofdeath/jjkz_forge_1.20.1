package com.darkness.jujutsukaisenzone.technique.skill;

import com.darkness.jujutsukaisenzone.dismemberment.DismembermentSystem;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 御厨子切痕：解按碰撞命中点选肢；捌 / 领域周期随机肢加痕。
 * <p>
 * 只调用 {@link DismembermentSystem#applyCutMarks}；满痕断肢由其它时机（或后续收刀）处理。
 */
public final class ShrineCutMarkLogic
{
    /** 解：基础切痕。 */
    public static final float KAI_BASE = 5.0F;
    /** 捌：每次周期伤害基础切痕。 */
    public static final float HACHI_BASE = 3.0F;
    /** 领域普通斩：基础切痕。 */
    public static final float DOMAIN_SLASH_BASE = 3.0F;
    /** 领域强力斩：基础切痕。 */
    public static final float DOMAIN_POWER_SLASH_BASE = 5.0F;

    private static final RatioBodyPart[] SEVERABLE = {
            RatioBodyPart.HEAD,
            RatioBodyPart.LEFT_ARM,
            RatioBodyPart.RIGHT_ARM,
            RatioBodyPart.LEFT_LEG,
            RatioBodyPart.RIGHT_LEG
    };

    private ShrineCutMarkLogic()
    {
    }

    /** 命中点距哪个可断肢锚点最近（相对目标碰撞箱上的部位锚点）。 */
    public static RatioBodyPart locatePartFromHit(LivingEntity target, Vec3 hitPos)
    {
        RatioBodyPart best = RatioBodyPart.HEAD;
        double bestDist = Double.MAX_VALUE;
        for (RatioBodyPart part : SEVERABLE)
        {
            double dist = part.worldAnchor(target).distanceToSqr(hitPos);
            if (dist < bestDist)
            {
                bestDist = dist;
                best = part;
            }
        }
        return best;
    }

    public static RatioBodyPart randomSeverablePart(RandomSource random)
    {
        return SEVERABLE[random.nextInt(SEVERABLE.length)];
    }

    /** 解命中：按采样点相对碰撞箱选肢加痕。 */
    public static int applyFromKaiHit(
            @Nullable LivingEntity owner,
            LivingEntity target,
            Vec3 hitPos,
            float damageDealt)
    {
        if (target == null || hitPos == null)
        {
            return 0;
        }
        RatioBodyPart part = locatePartFromHit(target, hitPos);
        return DismembermentSystem.applyCutMarks(owner, target, part, KAI_BASE, damageDealt);
    }

    /** 捌 / 领域：随机一肢加痕。 */
    public static int applyRandomLimb(
            @Nullable LivingEntity owner,
            LivingEntity target,
            float baseMarks,
            float damageDealt)
    {
        if (target == null || !target.isAlive())
        {
            return 0;
        }
        RatioBodyPart part = randomSeverablePart(target.getRandom());
        return DismembermentSystem.applyCutMarks(owner, target, part, baseMarks, damageDealt);
    }
}
