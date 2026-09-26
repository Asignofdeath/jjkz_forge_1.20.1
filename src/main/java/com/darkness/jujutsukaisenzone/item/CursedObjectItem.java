package com.darkness.jujutsukaisenzone.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 咒物：技能升级消耗材料。显示名用日文，tooltip 带中文意译与等级。
 */
public class CursedObjectItem extends Item
{
    private final String chineseGloss;
    private final CursedObjectGrade grade;

    public CursedObjectItem(Properties properties, String chineseGloss, CursedObjectGrade grade)
    {
        super(properties);
        this.chineseGloss = chineseGloss == null ? "" : chineseGloss;
        this.grade = grade == null ? CursedObjectGrade.COMMON : grade;
    }

    public CursedObjectGrade grade()
    {
        return grade;
    }

    public String chineseGloss()
    {
        return chineseGloss;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag)
    {
        tooltip.add(grade.tooltipLine());
        if (!chineseGloss.isEmpty())
        {
            tooltip.add(Component.literal(chineseGloss).withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.literal("スキル強化用の呪物。").withStyle(ChatFormatting.DARK_GRAY));
        com.darkness.jujutsukaisenzone.technique.se.SeEffectRegistry.findByItem(this).ifPresent(def ->
                tooltip.add(Component.literal("SE侵入: " + def.seId()).withStyle(ChatFormatting.GOLD)));
    }
}
