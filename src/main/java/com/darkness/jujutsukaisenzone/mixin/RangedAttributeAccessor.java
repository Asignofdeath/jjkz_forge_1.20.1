package com.darkness.jujutsukaisenzone.mixin;

import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 放开 {@link RangedAttribute} 的 max 字段，用于抬高原版 {@code MAX_HEALTH} 的 1024 硬顶。
 */
@Mixin(RangedAttribute.class)
public interface RangedAttributeAccessor
{
    @Accessor("maxValue")
    double jjkz$getMaxValue();

    @Accessor("maxValue")
    @Mutable
    void jjkz$setMaxValue(double maxValue);
}
