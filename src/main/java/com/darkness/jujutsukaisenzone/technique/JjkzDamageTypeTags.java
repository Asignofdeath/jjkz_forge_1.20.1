package com.darkness.jujutsukaisenzone.technique;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;

/**
 * 模组自己的伤害类型标签。
 */
public final class JjkzDamageTypeTags
{
    /**
     * 本模组「术式通道」的伤害类型标签（数据包：{@code data/jujutsukaisenzone/tags/damage_type/is_technique.json}）。
     * <p>
     * 用它而不是直接比对 {@code ResourceKey}，是为了让以后新增的术式子类型（真有需要时）只要挂进这个标签
     * 就能自动被防御管线识别，不必再改判定代码。
     */
    public static final TagKey<DamageType> IS_TECHNIQUE =
            TagKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(JuJutsuKaisenZone.MODID, "is_technique"));

    private JjkzDamageTypeTags()
    {
    }
}
