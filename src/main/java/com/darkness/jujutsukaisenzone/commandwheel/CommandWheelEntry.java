package com.darkness.jujutsukaisenzone.commandwheel;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 圆盘扇区：客户端展示用；真正执行走 {@link CommandWheelActions}。
 */
public record CommandWheelEntry(
        String id,
        Function<Player, Component> label,
        Predicate<Player> visible,
        Predicate<Player> enabled,
        Predicate<Player> active)
{
    public Component labelFor(Player player)
    {
        return label.apply(player);
    }

    public boolean isVisible(Player player)
    {
        return visible.test(player);
    }

    public boolean isEnabled(Player player)
    {
        return enabled.test(player);
    }

    public boolean isActive(Player player)
    {
        return active.test(player);
    }
}
