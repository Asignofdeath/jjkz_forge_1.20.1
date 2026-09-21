package com.darkness.jujutsukaisenzone.technique;

import com.darkness.jujutsukaisenzone.compat.EpicFightCompat;
import com.darkness.jujutsukaisenzone.player.PlayerCursedEnergyData;
import com.darkness.jujutsukaisenzone.player.PlayerStrengthenTechniqueData;
import com.darkness.jujutsukaisenzone.player.StrengthenTechniqueBonuses;
import com.darkness.jujutsukaisenzone.player.StrengthenTechniqueLevel;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * 强化术释放与维持逻辑（由技能快捷栏触发开关）。
 */
public final class StrengthenTechniqueServerLogic
{
    private static final double TICKS_PER_SECOND = 20.0;
    /** 维持强化术每秒消耗咒力基数。 */
    public static final double MAINTAIN_COST_PER_SECOND = 12.0;

    private StrengthenTechniqueServerLogic()
    {
    }

    public static int getMaintainCostPerSecond(Player player)
    {
        return getMaintainCostPerSecond(PlayerStrengthenTechniqueData.getLevel(player));
    }

    public static int getMaintainCostPerSecond(StrengthenTechniqueLevel level)
    {
        return (int) Math.ceil(MAINTAIN_COST_PER_SECOND * (1.0D - level.getMaintainCostReduction()));
    }

    public static void cast(ServerPlayer player)
    {
        if (!PlayerStrengthenTechniqueData.hasLearned(player) || !EpicFightCompat.isBattleMode(player))
        {
            return;
        }
        PlayerStrengthenTechniqueData.toggle(player);
    }

    public static void tickMaintain(ServerPlayer player)
    {
        if (!PlayerStrengthenTechniqueData.isActive(player))
        {
            return;
        }
        if (!PlayerStrengthenTechniqueData.hasLearned(player) || !EpicFightCompat.isBattleMode(player))
        {
            PlayerStrengthenTechniqueData.forceDisable(player);
            return;
        }

        StrengthenTechniqueBonuses.applyJumpBoostTick(player);

        if (player.tickCount % (int) TICKS_PER_SECOND != 0)
        {
            return;
        }

        int costThisSecond = getMaintainCostPerSecond(player);
        if (costThisSecond <= 0)
        {
            return;
        }

        if (!PlayerCursedEnergyData.tryConsume(player, costThisSecond))
        {
            PlayerStrengthenTechniqueData.forceDisable(player);
            player.displayClientMessage(
                    Component.literal("咒力耗尽，强化术解除").withStyle(ChatFormatting.RED),
                    true
            );
            return;
        }
    }
}
