package com.darkness.jujutsukaisenzone.network.packet;



import com.darkness.jujutsukaisenzone.player.PlayerSeanceTechniqueData;

import net.minecraft.client.Minecraft;

import net.minecraft.network.FriendlyByteBuf;

import net.minecraft.world.entity.player.Player;

import net.minecraftforge.network.NetworkEvent;



import java.util.function.Supplier;



/** 服务端→客户端：同步降灵术习得状态与等级 tier。 */

public class SyncSeanceTechniqueLearnedS2CPacket

{

    private final boolean learned;

    private final int tier;



    public SyncSeanceTechniqueLearnedS2CPacket(boolean learned, int tier)

    {

        this.learned = learned;

        this.tier = tier;

    }



    public static void encode(SyncSeanceTechniqueLearnedS2CPacket packet, FriendlyByteBuf buffer)

    {

        buffer.writeBoolean(packet.learned);

        buffer.writeVarInt(packet.tier);

    }



    public static SyncSeanceTechniqueLearnedS2CPacket decode(FriendlyByteBuf buffer)

    {

        return new SyncSeanceTechniqueLearnedS2CPacket(buffer.readBoolean(), buffer.readVarInt());

    }



    public static void handle(SyncSeanceTechniqueLearnedS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)

    {

        NetworkEvent.Context context = contextSupplier.get();

        context.enqueueWork(() ->

        {

            Player player = Minecraft.getInstance().player;

            if (player != null)

            {

                PlayerSeanceTechniqueData.applyClientLearned(player, packet.learned, packet.tier);

            }

        });

        context.setPacketHandled(true);

    }

}

