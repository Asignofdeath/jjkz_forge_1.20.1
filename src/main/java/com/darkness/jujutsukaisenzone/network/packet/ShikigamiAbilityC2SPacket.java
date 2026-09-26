package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.technique.shikigami.ShikigamiAbilityRuntime;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 式神模式：修饰键 + 数字键 1～0 → 主动式神招式。
 * <p>
 * 与 {@link TechniqueSpecialMoveC2SPacket}（热键栏特别招式）平行，互不混用。
 */
public class ShikigamiAbilityC2SPacket
{
    private final int shikigamiIndex;
    private final int modifierMask;

    public ShikigamiAbilityC2SPacket(int shikigamiIndex, int modifierMask)
    {
        this.shikigamiIndex = shikigamiIndex;
        this.modifierMask = modifierMask;
    }

    public static void encode(ShikigamiAbilityC2SPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(packet.shikigamiIndex);
        buffer.writeVarInt(packet.modifierMask);
    }

    public static ShikigamiAbilityC2SPacket decode(FriendlyByteBuf buffer)
    {
        return new ShikigamiAbilityC2SPacket(buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(ShikigamiAbilityC2SPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            ServerPlayer player = context.getSender();
            if (player == null)
            {
                return;
            }
            if (packet.shikigamiIndex < 0 || packet.shikigamiIndex > 9 || packet.modifierMask == 0)
            {
                return;
            }
            ShikigamiAbilityRuntime.tryPlayerCast(player, packet.shikigamiIndex, packet.modifierMask);
        });
        context.setPacketHandled(true);
    }
}
