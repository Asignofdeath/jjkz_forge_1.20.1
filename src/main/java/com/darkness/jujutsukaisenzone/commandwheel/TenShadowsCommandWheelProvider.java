package com.darkness.jujutsukaisenzone.commandwheel;

import com.darkness.jujutsukaisenzone.compat.EpicFightCompat;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import com.darkness.jujutsukaisenzone.player.PlayerTenShadowsModeData;
import com.darkness.jujutsukaisenzone.player.PlayerTenShadowsStanceData;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 十种影法术专用圆盘条目。
 */
public final class TenShadowsCommandWheelProvider
{
    public static final List<CommandWheelEntry> ENTRIES = List.of(
            new CommandWheelEntry(
                    CommandWheelIds.TEN_SHADOWS_SHIKIGAMI_MODE,
                    player -> Component.translatable(
                            PlayerTenShadowsModeData.isShikigamiMode(player)
                                    ? "command_wheel.jujutsukaisenzone.ten_shadows_mode_on"
                                    : "command_wheel.jujutsukaisenzone.ten_shadows_mode_off"),
                    TenShadowsCommandWheelProvider::isTenShadowsBattle,
                    player -> true,
                    PlayerTenShadowsModeData::isShikigamiMode),
            new CommandWheelEntry(
                    CommandWheelIds.TEN_SHADOWS_STANCE_TOGGLE,
                    player -> Component.translatable(
                            PlayerTenShadowsStanceData.isGuard(player)
                                    ? "command_wheel.jujutsukaisenzone.ten_shadows_stance_guard"
                                    : "command_wheel.jujutsukaisenzone.ten_shadows_stance_assist"),
                    TenShadowsCommandWheelProvider::isTenShadowsBattle,
                    player -> true,
                    PlayerTenShadowsStanceData::isGuard),
            new CommandWheelEntry(
                    CommandWheelIds.TEN_SHADOWS_ATTACK_ORDER,
                    player -> Component.translatable("command_wheel.jujutsukaisenzone.ten_shadows_attack"),
                    TenShadowsCommandWheelProvider::isTenShadowsBattle,
                    player -> PlayerTenShadowsStanceData.isAssist(player),
                    player -> false)
    );

    private TenShadowsCommandWheelProvider()
    {
    }

    private static boolean isTenShadowsBattle(net.minecraft.world.entity.player.Player player)
    {
        return EpicFightCompat.isBattleMode(player) && PlayerTechniqueData.isTenShadows(player);
    }
}
