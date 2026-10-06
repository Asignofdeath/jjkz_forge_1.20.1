package com.darkness.jujutsukaisenzone.item;

import com.darkness.jujutsukaisenzone.talent.InnateTalent;
import com.darkness.jujutsukaisenzone.talent.InnateTalentData;
import com.darkness.jujutsukaisenzone.talent.InnateTalentSource;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 咒力玉石：特级咒灵「珑璁」残秽晶体。吃完在池内均分抽一条天赋，来源改为后天。
 */
public class CursedEnergyJadeItem extends Item
{
    public static final FoodProperties FOOD = new FoodProperties.Builder()
            .nutrition(1)
            .saturationMod(0.1F)
            .alwaysEat()
            .fast()
            .build();

    private final InnateTalent[] pool;

    public CursedEnergyJadeItem(Properties properties, InnateTalent... pool)
    {
        super(properties.food(FOOD));
        this.pool = pool;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity)
    {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (level.isClientSide || !(entity instanceof ServerPlayer player) || pool.length == 0)
        {
            return result;
        }
        InnateTalent picked = InnateTalent.pickEqual(player.getRandom(), pool);
        InnateTalentData.set(player, picked, InnateTalentSource.ACQUIRED);
        player.sendSystemMessage(Component.translatable(
                "message.jujutsukaisenzone.innate_talent.acquired",
                picked.displayComponent()).withStyle(ChatFormatting.LIGHT_PURPLE));
        return result;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag)
    {
        tooltip.add(Component.translatable("item.jujutsukaisenzone.cursed_energy_jade.lore")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.jujutsukaisenzone.cursed_energy_jade.effect")
                .withStyle(ChatFormatting.DARK_GRAY));
        if (pool.length == 1)
        {
            tooltip.add(Component.translatable(
                    "item.jujutsukaisenzone.cursed_energy_jade.pool_single",
                    pool[0].displayComponent()).withStyle(ChatFormatting.GOLD));
            return;
        }
        StringBuilder names = new StringBuilder();
        for (int i = 0; i < pool.length; i++)
        {
            if (i > 0)
            {
                names.append(" / ");
            }
            names.append(pool[i].displayComponent().getString());
        }
        tooltip.add(Component.translatable(
                "item.jujutsukaisenzone.cursed_energy_jade.pool_multi",
                names.toString()).withStyle(ChatFormatting.GOLD));
    }
}
