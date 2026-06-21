package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.technique.skillset.TechniqueSkillSetRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 最大输出蓄力：按下开始蓄力 / 未满松手取消 / 蓄满松手释放。
 */
public class TechniqueMaxOutputC2SPacket
{
    public static final byte ACTION_CHARGE_BEGIN = 0;
    public static final byte ACTION_CHARGE_CANCEL = 1;
    public static final byte ACTION_CAST = 2;

    private final byte action;
    private final int slot;

    public TechniqueMaxOutputC2SPacket(byte action, int slot)
    {
        this.action = action;
        this.slot = slot;
    }

    public static void encode(TechniqueMaxOutputC2SPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeByte(packet.action);
        buffer.writeVarInt(packet.slot);
    }

    public static TechniqueMaxOutputC2SPacket decode(FriendlyByteBuf buffer)
    {
        return new TechniqueMaxOutputC2SPacket(buffer.readByte(), buffer.readVarInt());
    }

    public static void handle(TechniqueMaxOutputC2SPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            ServerPlayer player = context.getSender();
            if (player == null || packet.slot < 1 || packet.slot > 9)
            {
                return;
            }
            if (packet.action == ACTION_CHARGE_BEGIN)
            {
                TechniqueSkillSetRegistry.beginMaxOutputCharge(player, packet.slot);
            }
            else if (packet.action == ACTION_CHARGE_CANCEL)
            {
                TechniqueSkillSetRegistry.cancelMaxOutputCharge(player, packet.slot);
            }
            else if (packet.action == ACTION_CAST)
            {
                TechniqueSkillSetRegistry.castMaxOutput(player, packet.slot);
            }
        });
        context.setPacketHandled(true);
    }
}
