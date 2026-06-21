package com.darkness.jujutsukaisenzone.technique;

import com.darkness.jujutsukaisenzone.player.PlayerCursedEnergyData;
import com.darkness.jujutsukaisenzone.player.PlayerMimicryData;
import com.darkness.jujutsukaisenzone.player.PlayerSeanceTechniqueData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * 降灵术（拟态）服务端逻辑：维持期间按秒消耗咒力，等级越高减免越多。
 */
public final class SeanceTechniqueServerLogic
{
    private static final double TICKS_PER_SECOND = 20.0;
    /** 维持拟态每秒消耗咒力基数（与结界术不同，拟态无半径系数）。 */
    private static final double MAINTAIN_COST_PER_SECOND = 2.0;

    private SeanceTechniqueServerLogic()
    {
    }

    public static void tickMaintain(ServerPlayer player)
    {
        if (!PlayerMimicryData.isActive(player))
        {
            return;
        }

        double costPerTick = MAINTAIN_COST_PER_SECOND / TICKS_PER_SECOND;
        double reduction = PlayerSeanceTechniqueData.getMaintainCostReduction(player);
        costPerTick = costPerTick * (1.0 - reduction);
        int costThisTick = (int) Math.ceil(costPerTick);
        if (costThisTick <= 0)
        {
            return;
        }

        if (!PlayerCursedEnergyData.tryConsume(player, costThisTick))
        {
            PlayerMimicryData.forceDisable(player);
            player.displayClientMessage(
                    Component.literal("咒力耗尽，降灵术解除").withStyle(ChatFormatting.RED),
                    true
            );
        }
    }
}
