package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.technique.skillset.SpecialMoveInput;
import com.darkness.jujutsukaisenzone.technique.skillset.TechniqueSkillSetRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端「修饰键 + 数字键」触发的特别招式（不占独立槽位）。
 */
public class TechniqueSpecialMoveC2SPacket
{
    private final int modifierMask;
    private final int baseSlot;

    public TechniqueSpecialMoveC2SPacket(int modifierMask, int baseSlot)
    {
        this.modifierMask = modifierMask;
        this.baseSlot = baseSlot;
    }

    public static void encode(TechniqueSpecialMoveC2SPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(packet.modifierMask);
        buffer.writeVarInt(packet.baseSlot);
    }

    public static TechniqueSpecialMoveC2SPacket decode(FriendlyByteBuf buffer)
    {
        return new TechniqueSpecialMoveC2SPacket(buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(TechniqueSpecialMoveC2SPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            ServerPlayer player = context.getSender();
            if (player == null)
            {
                return;
            }
            if (packet.baseSlot < 1 || packet.baseSlot > 9 || packet.modifierMask == 0)
            {
                return;
            }
            TechniqueSkillSetRegistry.useSpecialMove(player, new SpecialMoveInput(packet.modifierMask, packet.baseSlot));
        });
        context.setPacketHandled(true);
    }
}

