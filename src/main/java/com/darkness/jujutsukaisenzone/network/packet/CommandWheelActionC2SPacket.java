package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.commandwheel.CommandWheelActions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.Supplier;

/** 指令圆盘选中条目；进攻指令附带 Epic Fight 锁定目标 UUID。 */
public class CommandWheelActionC2SPacket
{
    private final String entryId;
    @Nullable
    private final UUID targetUuid;

    public CommandWheelActionC2SPacket(String entryId, @Nullable UUID targetUuid)
    {
        this.entryId = entryId == null ? "" : entryId;
        this.targetUuid = targetUuid;
    }

    public static void encode(CommandWheelActionC2SPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeUtf(packet.entryId, 64);
        buffer.writeBoolean(packet.targetUuid != null);
        if (packet.targetUuid != null)
        {
            buffer.writeUUID(packet.targetUuid);
        }
    }

    public static CommandWheelActionC2SPacket decode(FriendlyByteBuf buffer)
    {
        String entryId = buffer.readUtf(64);
        UUID targetUuid = buffer.readBoolean() ? buffer.readUUID() : null;
        return new CommandWheelActionC2SPacket(entryId, targetUuid);
    }

    public static void handle(CommandWheelActionC2SPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            ServerPlayer player = context.getSender();
            if (player == null)
            {
                return;
            }
            CommandWheelActions.handle(player, packet.entryId, packet.targetUuid);
        });
        context.setPacketHandled(true);
    }
}
