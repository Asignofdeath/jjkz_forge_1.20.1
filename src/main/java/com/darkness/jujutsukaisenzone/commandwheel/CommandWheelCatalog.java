package com.darkness.jujutsukaisenzone.commandwheel;

import com.darkness.jujutsukaisenzone.compat.EpicFightCompat;
import com.darkness.jujutsukaisenzone.entity.MahoragaEntity;
import com.darkness.jujutsukaisenzone.player.PlayerDomainModeData;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import com.darkness.jujutsukaisenzone.technique.skillset.TechniqueSkillSetRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;

import java.util.ArrayList;
import java.util.List;

/**
 * 拼装当前玩家可见的圆盘条目：通用页 + 当前生得术式专属页。
 */
public final class CommandWheelCatalog
{
    public static final double MAHORAGA_RANGE = 6.0D;

    private static final CommandWheelEntry DOMAIN_EXPANSION = new CommandWheelEntry(
            CommandWheelIds.DOMAIN_EXPANSION,
            player -> Component.translatable("command_wheel.jujutsukaisenzone.domain_expansion"),
            EpicFightCompat::isBattleMode,
            player -> true,
            player -> false);

    private static final CommandWheelEntry DOMAIN_MODE = new CommandWheelEntry(
            CommandWheelIds.DOMAIN_MODE,
            player -> Component.translatable(PlayerDomainModeData.isInstant(player)
                    ? "command_wheel.jujutsukaisenzone.domain_mode_instant"
                    : "command_wheel.jujutsukaisenzone.domain_mode_normal"),
            player -> EpicFightCompat.isBattleMode(player) && PlayerDomainModeData.canUseInstantMode(player),
            player -> true,
            PlayerDomainModeData::isInstant);

    private static final CommandWheelEntry MAHORAGA_RIDE = new CommandWheelEntry(
            CommandWheelIds.MAHORAGA_RIDE,
            player -> Component.translatable("command_wheel.jujutsukaisenzone.mahoraga_ride"),
            player -> true,
            player -> true,
            player -> false);

    private CommandWheelCatalog()
    {
    }

    /** 通用页条目（全术式共用）。 */
    public static List<CommandWheelEntry> generalEntries(Player player)
    {
        List<CommandWheelEntry> list = new ArrayList<>();
        if (DOMAIN_EXPANSION.isVisible(player))
        {
            list.add(DOMAIN_EXPANSION);
        }
        if (DOMAIN_MODE.isVisible(player))
        {
            list.add(DOMAIN_MODE);
        }
        return list;
    }

    /** 当前生得术式专属页条目。 */
    public static List<CommandWheelEntry> techniqueEntries(Player player)
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

    public static List<CommandWheelEntry> entriesFor(Player player, CommandWheelPage page)
    {
        return switch (page)
        {
            case GENERAL -> generalEntries(player);
            case TECHNIQUE -> techniqueEntries(player);
        };
    }

    public static List<CommandWheelEntry> entriesFor(Player player, boolean mahoragaTargeted)
    {
        return mahoragaTargeted ? List.of(MAHORAGA_RIDE) : generalEntries(player);
    }

    public static boolean isAimingAtMahoraga(Player player, net.minecraft.world.phys.HitResult hit)
    {
        return hit instanceof EntityHitResult entityHit
                && entityHit.getEntity() instanceof MahoragaEntity mahoraga
                && mahoraga.isAlive()
                && player.distanceToSqr(mahoraga) <= MAHORAGA_RANGE * MAHORAGA_RANGE;
    }
}
