package com.darkness.jujutsukaisenzone.network;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.network.packet.CommandWheelActionC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncTenShadowsStanceS2CPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * 指令圆盘专用通道（与主 {@link ModNetwork} 分离，避免改动巨型注册表）。
 */
public final class CommandWheelNetwork
{
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(JuJutsuKaisenZone.MODID, "command_wheel"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    private CommandWheelNetwork()
    {
    }

    public static void register()
    {
        CHANNEL.messageBuilder(CommandWheelActionC2SPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(CommandWheelActionC2SPacket::encode)
                .decoder(CommandWheelActionC2SPacket::decode)
                .consumerMainThread(CommandWheelActionC2SPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncTenShadowsStanceS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncTenShadowsStanceS2CPacket::encode)
                .decoder(SyncTenShadowsStanceS2CPacket::decode)
                .consumerMainThread(SyncTenShadowsStanceS2CPacket::handle)
                .add();
    }

    public static void sendActionToServer(String entryId, @Nullable UUID targetUuid)
    {
        CHANNEL.send(PacketDistributor.SERVER.noArg(), new CommandWheelActionC2SPacket(entryId, targetUuid));
    }

    public static void sendStanceSync(ServerPlayer player, boolean guard)
    {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncTenShadowsStanceS2CPacket(guard));
    }
}
