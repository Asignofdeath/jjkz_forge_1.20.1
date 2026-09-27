package com.darkness.jujutsukaisenzone.item;

import com.darkness.jujutsukaisenzone.player.CursedEnergyTrait;
import com.darkness.jujutsukaisenzone.technique.se.PanelStat;
import com.darkness.jujutsukaisenzone.technique.se.SeEffectRegistry;
import com.darkness.jujutsukaisenzone.technique.se.SePenalty;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;

/**
 * 咒物：技能升级消耗材料。
 * <p>
 * 若在 {@link SeEffectRegistry} 登记为 SE 载体（SE 咒物）：可右键「咒力探查」写入特质与代价；
 * 未探查可堆叠，探查后不可再叠。普通咒物无探查、无 SE 代价。
 */
public class CursedObjectItem extends Item
{
    /** 咒力探查读条时长（tick）。 */
    public static final int PROBE_DURATION_TICKS = 40;

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

    /** 是否在 SE 注册表中有对应机制（SE 咒物）。 */
    public boolean isSeCarrier()
    {
        return SeEffectRegistry.findByItem(this).isPresent();
    }

    @Override
    public int getMaxStackSize(ItemStack stack)
    {
        if (isSeCarrier() && CursedObjectSeTrait.isProbed(stack))
        {
            CursedObjectSeTrait.ensureInstanceId(stack);
            return 1;
        }
        return super.getMaxStackSize(stack);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack)
    {
        return isSeCarrier() ? UseAnim.BOW : UseAnim.NONE;
    }

    @Override
    public int getUseDuration(ItemStack stack)
    {
        return isSeCarrier() ? PROBE_DURATION_TICKS : 0;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);
        if (!isSeCarrier())
        {
            return InteractionResultHolder.pass(stack);
        }
        if (CursedObjectSeTrait.isProbed(stack))
        {
            if (!level.isClientSide)
            {
                player.displayClientMessage(
                        Component.translatable("message.jujutsukaisenzone.cursed_object.already_probed")
                                .withStyle(ChatFormatting.GRAY),
                        true);
            }
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity)
    {
        if (level.isClientSide || !(entity instanceof ServerPlayer player) || !isSeCarrier())
        {
            return stack;
        }
        if (CursedObjectSeTrait.isProbed(stack))
        {
            return stack;
        }

        ItemStack toProbe = stack.getCount() <= 1 ? stack : stack.split(1);
        boolean ok = CursedObjectSeTrait.probe(toProbe, grade, level.getRandom());
        if (!ok)
        {
            if (stack != toProbe && !toProbe.isEmpty())
            {
                if (!player.getInventory().add(toProbe))
                {
                    player.drop(toProbe, false);
                }
            }
            return stack;
        }

        player.displayClientMessage(
                Component.translatable("message.jujutsukaisenzone.cursed_object.probed")
                        .withStyle(ChatFormatting.AQUA),
                true);

        if (stack == toProbe)
        {
            return toProbe;
        }
        if (!player.getInventory().add(toProbe))
        {
            player.drop(toProbe, false);
        }
        return stack;
    }

    @Override
    public Rarity getRarity(ItemStack stack)
    {
        return switch (grade)
        {
            case COMMON -> Rarity.COMMON;
            case RARE -> Rarity.RARE;
            case SPECIAL -> Rarity.UNCOMMON;
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag)
    {
        boolean se = isSeCarrier();
        tooltip.add(Component.translatable(se
                        ? "item.jujutsukaisenzone.cursed_object.kind.se"
                        : "item.jujutsukaisenzone.cursed_object.kind.normal")
                .withStyle(se ? ChatFormatting.GOLD : ChatFormatting.GRAY));
        tooltip.add(grade.tooltipLine());
        if (!chineseGloss.isEmpty())
        {
            tooltip.add(Component.literal(chineseGloss).withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.literal("スキル強化用の呪物。").withStyle(ChatFormatting.DARK_GRAY));

        if (!se)
        {
            return;
        }

        SeEffectRegistry.findByItem(this).ifPresent(def ->
                tooltip.add(Component.translatable("item.jujutsukaisenzone.cursed_object.se_id", def.seId())
                        .withStyle(ChatFormatting.DARK_AQUA)));

        if (!CursedObjectSeTrait.isProbed(stack))
        {
            tooltip.add(Component.translatable("item.jujutsukaisenzone.cursed_object.unprobed")
                    .withStyle(ChatFormatting.YELLOW));
            tooltip.add(Component.translatable("item.jujutsukaisenzone.cursed_object.probe_hint")
                    .withStyle(ChatFormatting.DARK_GRAY));
            return;
        }

        CursedEnergyTrait trait = CursedObjectSeTrait.getTrait(stack);
        if (trait != null)
        {
            tooltip.add(Component.translatable("item.jujutsukaisenzone.cursed_object.trait", trait.displayName())
                    .withStyle(ChatFormatting.AQUA));
        }
        SePenalty penalty = CursedObjectSeTrait.getPenalty(stack);
        if (penalty != null)
        {
            String signed = penalty.stat().applyKind() == PanelStat.ApplyKind.DOMAIN_GAUGE_MAX_ADD
                    ? "+" + formatPenaltyAmount(penalty.amount())
                    : "−" + formatPenaltyAmount(penalty.amount());
            tooltip.add(Component.translatable(
                            "item.jujutsukaisenzone.cursed_object.penalty",
                            penalty.stat().displayName(),
                            signed)
                    .withStyle(ChatFormatting.RED));
        }
    }

    private static String formatPenaltyAmount(double amount)
    {
        if (Math.abs(amount - Math.rint(amount)) < 1.0E-6)
        {
            return Long.toString(Math.round(amount));
        }
        return String.format(Locale.ROOT, "%.1f", amount);
    }
}
