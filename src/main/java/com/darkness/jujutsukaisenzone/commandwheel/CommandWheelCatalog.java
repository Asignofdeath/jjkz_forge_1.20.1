package com.darkness.jujutsukaisenzone.commandwheel;

import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import com.darkness.jujutsukaisenzone.technique.skillset.TechniqueSkillSetRegistry;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * 拼装当前玩家可见的圆盘条目：仅当前生得术式专用指令。
 */
public final class CommandWheelCatalog
{
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
}
