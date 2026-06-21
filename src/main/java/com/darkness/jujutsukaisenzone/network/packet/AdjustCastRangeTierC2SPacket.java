package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.technique.skillset.TechniqueSkillSetRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class AdjustCastRangeTierC2SPacket
{
    private final int delta;

    public AdjustCastRangeTierC2SPacket(int delta)
    {
        this.delta = delta;
    }

    public static void encode(AdjustCastRangeTierC2SPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(packet.delta);
    }

    public static AdjustCastRangeTierC2SPacket decode(FriendlyByteBuf buffer)
    {
        return new AdjustCastRangeTierC2SPacket(buffer.readVarInt());
    }

    public static void handle(AdjustCastRangeTierC2SPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            ServerPlayer player = context.getSender();
            if (player == null)
            {
                return;
            }
            int safeDelta = Integer.compare(packet.delta, 0);
            if (safeDelta == 0)
            {
                return;
            }
            int tier = TechniqueSkillSetRegistry.adjustCastRangeTier(player, safeDelta);
            int percent = tier * 25;
            player.displayClientMessage(Component.literal("施法距离档位：" + tier + "（" + percent + "%）"), true);
        });
        context.setPacketHandled(true);
    }
}
