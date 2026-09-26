package com.darkness.jujutsukaisenzone.item;

import com.darkness.jujutsukaisenzone.player.PlayerCursedEnergyData;
import com.darkness.jujutsukaisenzone.player.PlayerFactionData;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import com.darkness.jujutsukaisenzone.player.PlayerTenShadowsContractData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class TenShadowsTechniqueBinderItem extends Item
{
    public TenShadowsTechniqueBinderItem(Properties properties)
    {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        ItemStack itemStack = player.getItemInHand(hand);

        if (!level.isClientSide)
        {
            if (PlayerCursedEnergyData.isHeavenlyRestricted(player))
            {
                player.sendSystemMessage(Component.literal("天与咒缚状态下无法绑定生得术式"));
                return InteractionResultHolder.fail(itemStack);
            }
            if (!PlayerFactionData.canBindInnateTechnique(player))
            {
                player.sendSystemMessage(Component.literal("仅咒术师、诅咒师、咒灵阵营可绑定生得术式"));
                return InteractionResultHolder.fail(itemStack);
            }
            PlayerTechniqueData.setInnateTechnique(player, PlayerTechniqueData.TECHNIQUE_TEN_SHADOWS);
            player.sendSystemMessage(Component.literal("你的生得术式已绑定为：十种影法术"));
        }

        return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide);
    }
}
