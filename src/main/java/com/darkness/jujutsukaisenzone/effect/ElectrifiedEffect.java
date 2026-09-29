package com.darkness.jujutsukaisenzone.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * 「感电」占位：肢体发麻，轻微减速。正式雷系逻辑（传导、叠层等）后续再扩。
 * <p>
 * 修饰符数值按 {@code amplifier + 1} 缩放（一级 = {@link #SPEED_REDUCTION}）。
 */
public final class ElectrifiedEffect extends MobEffect
{
    private static final String SPEED_MODIFIER_UUID = "a3c8e1f0-5b2d-4e9a-8f17-6d4c2b9a0e51";

    /** 一级移速约 -20%。 */
    public static final double SPEED_REDUCTION = -0.20D;

    public ElectrifiedEffect()
    {
        super(MobEffectCategory.HARMFUL, 0x5EC8FF);
        this.addAttributeModifier(
                Attributes.MOVEMENT_SPEED,
                SPEED_MODIFIER_UUID,
                SPEED_REDUCTION,
                AttributeModifier.Operation.MULTIPLY_TOTAL);
    }
}
