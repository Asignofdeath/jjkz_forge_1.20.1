package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.compat.StylizedDamageCompat;
import com.darkness.jujutsukaisenzone.compat.StylizedDamagePresence;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 服务端 → 客户端：同步「StylizedDamage 样式联动」全服规则。
 */
public final class SyncStylizedDamageStylesRuleS2CPacket
{
    private final boolean enabled;

    public SyncStylizedDamageStylesRuleS2CPacket(boolean enabled)
    {
        this.enabled = enabled;
    }

    public static void encode(SyncStylizedDamageStylesRuleS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeBoolean(packet.enabled);
    }

    public static SyncStylizedDamageStylesRuleS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncStylizedDamageStylesRuleS2CPacket(buffer.readBoolean());
    }

    public static void handle(SyncStylizedDamageStylesRuleS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            if (!StylizedDamagePresence.isLoaded())
            {
                return;
            }
            // 样式/选择器始终注册并重建引擎；enabled 只表示服务端是否投影叙事 ID
            StylizedDamageCompat.registerIfNeeded();
            StylizedDamageCompat.applyServerEnabled(packet.enabled);
        });
        context.setPacketHandled(true);
    }
}
