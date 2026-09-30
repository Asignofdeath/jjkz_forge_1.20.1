package com.darkness.jujutsukaisenzone.commandwheel;

import com.darkness.jujutsukaisenzone.entity.MahoragaEntity;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import com.darkness.jujutsukaisenzone.technique.skillset.TechniqueSkillSetRegistry;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;

import java.util.ArrayList;
import java.util.List;

/**
 * 拼装当前玩家可见的圆盘条目：仅当前生得术式专用指令。
 */
public final class CommandWheelCatalog
{
    public static final double MAHORAGA_RANGE = 6.0D;
    private static final CommandWheelEntry MAHORAGA_RIDE = new CommandWheelEntry(
            CommandWheelIds.MAHORAGA_RIDE,
            player -> net.minecraft.network.chat.Component.translatable("command_wheel.jujutsukaisenzone.mahoraga_ride"),
            player -> true,
            player -> true,
            player -> false);
    private CommandWheelCatalog()
    {
    }

    public static List<CommandWheelEntry> entriesFor(Player player)
    {
        List<CommandWheelEntry> list = new ArrayList<>();
        String techniqueId = PlayerTechniqueData.getInnateTechnique(player);
        if (techniqueId == null || techniqueId.isEmpty())
        {
            return list;
        }
        TechniqueSkillSetRegistry.get(techniqueId).ifPresent(set ->
        {
            for (CommandWheelEntry entry : set.commandWheelEntries())
            {
                if (entry.isVisible(player))
                {
                    list.add(entry);
                }
            }
        });
        return list;
    }

    public static List<CommandWheelEntry> entriesFor(Player player, boolean mahoragaTargeted)
    {
        return mahoragaTargeted ? List.of(MAHORAGA_RIDE) : entriesFor(player);
    }

    public static boolean isAimingAtMahoraga(Player player, net.minecraft.world.phys.HitResult hit)
    {
        return hit instanceof EntityHitResult entityHit
                && entityHit.getEntity() instanceof MahoragaEntity mahoraga
                && mahoraga.isAlive()
                && player.distanceToSqr(mahoraga) <= MAHORAGA_RANGE * MAHORAGA_RANGE;
    }
}
