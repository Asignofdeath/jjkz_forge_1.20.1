package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.technique.skillset.TechniqueSkillSetRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 长按蓄力、松手释放：按下开始蓄力 / 未满松手取消 / 蓄满松手发射。
 */
public final class TechniqueHoldReleaseC2SPacket
{
    public static final byte ACTION_BEGIN = 0;
    public static final byte ACTION_CANCEL = 1;
    public static final byte ACTION_RELEASE = 2;

    private final byte action;
    private final int slot;

    public TechniqueHoldReleaseC2SPacket(byte action, int slot)
    {
        this.action = action;
        this.slot = slot;
    }

    public static void encode(TechniqueHoldReleaseC2SPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeByte(packet.action);
        buffer.writeVarInt(packet.slot);
    }

    public static TechniqueHoldReleaseC2SPacket decode(FriendlyByteBuf buffer)
    {
        return new TechniqueHoldReleaseC2SPacket(buffer.readByte(), buffer.readVarInt());
    }

    public static void handle(TechniqueHoldReleaseC2SPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            ServerPlayer player = context.getSender();
            if (player == null || packet.slot < 1 || packet.slot > 9)
            {
                return;
            }
            if (packet.action == ACTION_BEGIN)
            {
                TechniqueSkillSetRegistry.beginHoldReleaseCharge(player, packet.slot);
            }
            else if (packet.action == ACTION_CANCEL)
            {
                TechniqueSkillSetRegistry.cancelHoldReleaseCharge(player, packet.slot);
            }
            else if (packet.action == ACTION_RELEASE)
            {
                TechniqueSkillSetRegistry.releaseHoldReleaseCharge(player, packet.slot);
            }
        });
        context.setPacketHandled(true);
    }
}
