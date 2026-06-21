package com.darkness.jujutsukaisenzone.item;

import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueIntelData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class TechniqueIntelligenceItem extends Item
{
    public static final int INTEL_PER_USE = 20;

    public TechniqueIntelligenceItem(Properties properties)
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

        String techniqueId = PlayerTechniqueData.getInnateTechnique(serverPlayer);
        if (techniqueId.isBlank() || PlayerTechniqueData.TECHNIQUE_NONE.equals(techniqueId))
        {
            serverPlayer.sendSystemMessage(Component.translatable(
                    "message.jujutsukaisenzone.technique_intelligence.no_technique"));
            return InteractionResultHolder.fail(stack);
        }

        PlayerTechniqueIntelData.addIntel(serverPlayer, techniqueId, INTEL_PER_USE);
        int total = PlayerTechniqueIntelData.getIntel(serverPlayer, techniqueId);
        serverPlayer.sendSystemMessage(Component.translatable(
                "message.jujutsukaisenzone.technique_intelligence.used", INTEL_PER_USE, total));

        if (!player.getAbilities().instabuild)
        {
            stack.shrink(1);
        }
        return InteractionResultHolder.success(stack);
    }
}
