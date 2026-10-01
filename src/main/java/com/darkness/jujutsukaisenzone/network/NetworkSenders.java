package com.darkness.jujutsukaisenzone.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.PacketDistributor;

/** Common targets for packets sent on the main channel. */
public final class NetworkSenders
{
    private NetworkSenders()
    {
    }

    public static void toPlayer(ServerPlayer player, Object packet)
    {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void toServer(Object packet)
    {
        ModNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(), packet);
    }

    public static void toAll(Object packet)
    {
        ModNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
    }

    public static void toDimension(ServerPlayer player, Object packet)
    {
        ModNetwork.CHANNEL.send(PacketDistributor.DIMENSION.with(player.level()::dimension), packet);
    }

    public static void toTrackingAndSelf(Entity entity, Object packet)
    {
        ModNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), packet);
    }
}
