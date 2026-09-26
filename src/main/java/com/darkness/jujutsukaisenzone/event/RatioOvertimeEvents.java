package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.player.PlayerCursedEnergyData;
import com.darkness.jujutsukaisenzone.player.PlayerRatioOvertimeData;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import com.darkness.jujutsukaisenzone.technique.skill.RatioOvertimeLogic;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/**
 * 十划加班作息：输出乘区、工作效率（耗咒力转体术攻击）、时段提示。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RatioOvertimeEvents
{
    private static final UUID EFFICIENCY_ATTACK_UUID =
            UUID.fromString("a91c6f2e-4b33-4e1a-9f0c-7d2e8b1a4c55");
    private static final String EFFICIENCY_ATTACK_NAME = "jjkz_ratio_efficiency_attack";
    /** 每消耗 5 咒力 → +2 攻击力。 */
    private static final int CE_PER_STEP = 5;
    private static final double ATTACK_PER_STEP = 2.0D;

    private RatioOvertimeEvents()
    {
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onLivingHurt(LivingHurtEvent event)
    {
        if (event.getEntity().level().isClientSide)
        {
            return;
        }
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker))
        {
            return;
        }
        float mult = RatioOvertimeLogic.outgoingDamageMultiplier(attacker);
        if (Math.abs(mult - 1.0F) > 0.0001F)
        {
            event.setAmount(event.getAmount() * mult);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide)
        {
            return;
        }
        if (!(event.player instanceof ServerPlayer player))
        {
            return;
        }
        if (!PlayerTechniqueData.isRatio(player))
        {
            clearEfficiencyModifier(player);
            return;
        }

        tickEfficiency(player);
        maybeAnnounceSchedule(player);
    }

    private static void tickEfficiency(ServerPlayer player)
    {
        if (!PlayerRatioOvertimeData.isEfficiencyActive(player))
        {
            clearEfficiencyModifier(player);
            PlayerRatioOvertimeData.clearEfficiency(player);
            return;
        }

        int lastCe = PlayerRatioOvertimeData.getAndUpdateLastCe(player);
        int nowCe = PlayerCursedEnergyData.getCurrent(player);
        int spent = Math.max(0, lastCe - nowCe);
        if (spent >= CE_PER_STEP)
        {
            int steps = spent / CE_PER_STEP;
            PlayerRatioOvertimeData.addEfficiencyBonus(player, steps * ATTACK_PER_STEP);
        }

        applyEfficiencyModifier(player, PlayerRatioOvertimeData.getEfficiencyBonus(player));
    }

    private static void applyEfficiencyModifier(ServerPlayer player, double bonus)
    {
        AttributeInstance attack = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack == null)
        {
            return;
        }
        attack.removeModifier(EFFICIENCY_ATTACK_UUID);
        if (bonus > 0.0D)
        {
            attack.addTransientModifier(new AttributeModifier(
                    EFFICIENCY_ATTACK_UUID,
                    EFFICIENCY_ATTACK_NAME,
                    bonus,
                    AttributeModifier.Operation.ADDITION));
        }
    }

    private static void clearEfficiencyModifier(Player player)
    {
        AttributeInstance attack = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null)
        {
            attack.removeModifier(EFFICIENCY_ATTACK_UUID);
        }
    }

    private static void maybeAnnounceSchedule(ServerPlayer player)
    {
        if (!RatioOvertimeLogic.isScheduleActive(player))
        {
            return;
        }
        // 每 100 tick 最多提示一次状态变化：用 dayTime 桶
        long bucket = player.level().getDayTime() / 100L;
        String key = "jujutsukaisenzone_ratio_schedule_bucket";
        long last = player.getPersistentData().getLong(key);
        if (last == bucket)
        {
            return;
        }
        boolean work = RatioOvertimeLogic.isWorkHours(player.level());
        boolean wasWork = player.getPersistentData().getBoolean("jujutsukaisenzone_ratio_was_work");
        if (player.getPersistentData().contains("jujutsukaisenzone_ratio_was_work") && wasWork != work)
        {
            player.displayClientMessage(
                    Component.literal(work ? "上班时间：输出略降" : "加班时间：输出提升 · 可使用无可谅解")
                            .withStyle(work ? ChatFormatting.GRAY : ChatFormatting.GOLD),
                    true);
        }
        player.getPersistentData().putLong(key, bucket);
        player.getPersistentData().putBoolean("jujutsukaisenzone_ratio_was_work", work);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event)
    {
        clearEfficiencyModifier(event.getEntity());
    }
}
