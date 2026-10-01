package com.darkness.jujutsukaisenzone.attribute;

import com.darkness.jujutsukaisenzone.gameplay.CursedSpiritHealthTable;
import com.darkness.jujutsukaisenzone.mixin.RangedAttributeAccessor;
import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import org.slf4j.Logger;

/**
 * 抬高原版 {@link Attributes#MAX_HEALTH} 的 {@link RangedAttribute} 上限。
 * <p>
 * 原版钳制为 1～1024；本模组玩家特级 / 咒灵特级生命远超该值，启动时改为与
 * {@link CursedSpiritHealthTable#SPECIAL} 上限对齐。
 */
public final class MaxHealthAttributeCap
{
    private static final Logger LOGGER = LogUtils.getLogger();

    /** 须 ≥ 咒灵特级生命上限（200 万）。 */
    public static final double RAISED_MAX = CursedSpiritHealthTable.SPECIAL.max();

    private MaxHealthAttributeCap()
    {
    }

    public static void apply()
    {
        if (!(Attributes.MAX_HEALTH instanceof RangedAttribute ranged))
        {
            LOGGER.warn("Attributes.MAX_HEALTH is not a RangedAttribute; cannot raise cap");
            return;
        }
        RangedAttributeAccessor accessor = (RangedAttributeAccessor) (Object) ranged;
        double before = accessor.jjkz$getMaxValue();
        if (before >= RAISED_MAX)
        {
            return;
        }
        accessor.jjkz$setMaxValue(RAISED_MAX);
        LOGGER.info("Raised generic.max_health attribute cap: {} → {}", before, RAISED_MAX);
    }
}
