package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.event.TechniqueSkillKeyEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 服务端强制打断最大输出蓄力时同步客户端，隐藏蓄力条并清理本地输入状态。 */
public final class SyncMaxOutputChargeInterruptS2CPacket
{
    public SyncMaxOutputChargeInterruptS2CPacket()
    {
    }

    public static void encode(SyncMaxOutputChargeInterruptS2CPacket packet, FriendlyByteBuf buffer)
    {
    }

    public static SyncMaxOutputChargeInterruptS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncMaxOutputChargeInterruptS2CPacket();
    }

    public static void handle(SyncMaxOutputChargeInterruptS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(TechniqueSkillKeyEvents::forceInterruptLocalCharge);
        context.setPacketHandled(true);
    }
}
