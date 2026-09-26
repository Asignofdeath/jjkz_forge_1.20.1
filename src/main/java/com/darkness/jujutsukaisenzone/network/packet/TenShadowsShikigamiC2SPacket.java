package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.technique.skill.TenShadowsRitualLogic;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 影之式神模式：数字键 1～0 对应索引 0～9，召出或收回式神。 */
public class TenShadowsShikigamiC2SPacket
{
    private final int shikigamiIndex;

    public TenShadowsShikigamiC2SPacket(int shikigamiIndex)
    {
        this.shikigamiIndex = shikigamiIndex;
    }

    public static void encode(TenShadowsShikigamiC2SPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(packet.shikigamiIndex);
    }

    public static TenShadowsShikigamiC2SPacket decode(FriendlyByteBuf buffer)
    {
        return new TenShadowsShikigamiC2SPacket(buffer.readVarInt());
    }

    public static void handle(TenShadowsShikigamiC2SPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            ServerPlayer player = context.getSender();
            if (player == null)
            {
                return;
            }
            if (packet.shikigamiIndex < 0 || packet.shikigamiIndex > 9)
            {
                return;
            }
            TenShadowsRitualLogic.handleShikigamiKey(player, packet.shikigamiIndex);
        });
        context.setPacketHandled(true);
    }
}
