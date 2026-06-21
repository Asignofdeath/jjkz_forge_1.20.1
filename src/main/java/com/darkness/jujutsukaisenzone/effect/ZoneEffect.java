package com.darkness.jujutsukaisenzone.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * ZONE 状态药水效果。
 * <p>
 * 仅用作 HUD 图标显示，无实际效果。
 * 所有 ZONE 实际效果（攻击加成、伤害减免、黑闪概率加成）均在
 * {@link com.darkness.jujutsukaisenzone.event.BlackFlashEvents} 中处理。
 */
public class ZoneEffect extends MobEffect
{
    public ZoneEffect()
    {
        super(MobEffectCategory.BENEFICIAL, 0x1A0033);
    }
}
