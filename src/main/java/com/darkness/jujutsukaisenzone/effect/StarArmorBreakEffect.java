package com.darkness.jujutsukaisenzone.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * 「星蚀」：护甲被星力侵蚀，护甲值下降。属于 Mimosa 柠檬黄那颗星的引爆效果（另外附带原版发光）。
 * <p>
 * 走 {@link MobEffect#addAttributeModifier} 而不是自己往实体上挂修饰符——原版会在效果生效期间自动
 * 附加、结束时自动摘除，等级变化时也会刷新，因此不需要手写 UUID 生命周期管理。
 * <p>
 * ⚠️ 修饰符的数值由 {@code MobEffectInstance} 按 {@code amplifier + 1} 缩放：一级（amplifier 0）
 * 是 {@link #ARMOR_REDUCTION}，二级就是它的两倍。
 */
public final class StarArmorBreakEffect extends MobEffect
{
    /**
     * 固定 UUID 的字符串形式：同一个效果每次应用都必须用同一个 id，否则会往实体身上挂上重复的修饰符
     * （原版按 id 去重，随机 id 会越挂越多）。
     * <p>
     * ⚠️ 1.20.1 的 {@link MobEffect#addAttributeModifier} 第二个参数是 {@code String}（内部自行
     * {@code UUID.fromString}），不是 {@code UUID} 对象——传 UUID 会编译不过。
     */
    private static final String ARMOR_MODIFIER_UUID = "6f2b9c1e-3d4a-4b8f-9c7e-1a5d2e8b4f30";

    /** 一级降 4 点护甲（护甲值的语义：每点约合 4% 减伤）。 */
    public static final double ARMOR_REDUCTION = -4.0D;

    public StarArmorBreakEffect()
    {
        super(MobEffectCategory.HARMFUL, 0xF5E36B);
        this.addAttributeModifier(
                Attributes.ARMOR,
                ARMOR_MODIFIER_UUID,
                ARMOR_REDUCTION,
                AttributeModifier.Operation.ADDITION);
    }
}
