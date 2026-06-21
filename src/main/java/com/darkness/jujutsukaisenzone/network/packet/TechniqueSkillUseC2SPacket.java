package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.technique.skillset.TechniqueSkillSetRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class TechniqueSkillUseC2SPacket
{
    private final int slot;

    public TechniqueSkillUseC2SPacket(int slot)
    {
        this.slot = slot;
    }

    public static void encode(TechniqueSkillUseC2SPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(packet.slot);
    }

    public static TechniqueSkillUseC2SPacket decode(FriendlyByteBuf buffer)
    {
        return new TechniqueSkillUseC2SPacket(buffer.readVarInt());
    }

    public static void handle(TechniqueSkillUseC2SPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            ServerPlayer player = context.getSender();
            if (player != null)
            {
                TechniqueSkillSetRegistry.useSkillSlot(player, packet.slot);
            }
        });
        context.setPacketHandled(true);
    }
}
