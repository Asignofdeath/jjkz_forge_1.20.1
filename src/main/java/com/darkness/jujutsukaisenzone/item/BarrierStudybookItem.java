package com.darkness.jujutsukaisenzone.item;

import com.darkness.jujutsukaisenzone.technique.BarrierTechniqueLearnLogic;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class BarrierStudybookItem extends Item
{
    public BarrierStudybookItem(Properties properties)
    {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer))
        {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (!BarrierTechniqueLearnLogic.tryUseStudybook(serverPlayer))
        {
            return InteractionResultHolder.fail(stack);
        }
        if (!player.getAbilities().instabuild)
        {
            stack.shrink(1);
        }
        return InteractionResultHolder.success(stack);
    }
}
