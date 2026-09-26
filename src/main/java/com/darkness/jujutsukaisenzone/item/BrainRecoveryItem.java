package com.darkness.jujutsukaisenzone.item;

import com.darkness.jujutsukaisenzone.player.PlayerBrainDamageData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.IntSupplier;

/**
 * 回复领域脑损的消耗品（清神糖 / 安神丸）。
 */
public class BrainRecoveryItem extends Item
{
    private final IntSupplier healAmount;
    private final String descriptionKey;

    public BrainRecoveryItem(Properties properties, IntSupplier healAmount, String descriptionKey)
    {
        super(properties);
        this.healAmount = healAmount;
        this.descriptionKey = descriptionKey;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer))
        {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        int before = PlayerBrainDamageData.getBrainDamage(serverPlayer);
        if (before <= 0)
        {
            serverPlayer.displayClientMessage(
                    Component.translatable("message.jujutsukaisenzone.brain_damage.already_clear")
                            .withStyle(ChatFormatting.GRAY),
                    true);
            return InteractionResultHolder.fail(stack);
        }
        int healed = PlayerBrainDamageData.heal(serverPlayer, healAmount.getAsInt());
        if (healed <= 0)
        {
            return InteractionResultHolder.fail(stack);
        }
        if (!player.getAbilities().instabuild)
        {
            stack.shrink(1);
        }
        serverPlayer.displayClientMessage(
                Component.translatable(
                                "message.jujutsukaisenzone.brain_damage.healed",
                                healed,
                                PlayerBrainDamageData.getBrainDamage(serverPlayer))
                        .withStyle(ChatFormatting.GREEN),
                true);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag)
    {
        tooltip.add(Component.translatable(descriptionKey, healAmount.getAsInt())
                .withStyle(ChatFormatting.GRAY));
    }
}
