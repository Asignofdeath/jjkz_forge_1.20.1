package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.client.PlayerInputLockController;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 同步玩家输入锁定状态到客户端。
 * <p>
 * 业务意图：服务端可在特定演出期间禁用玩家本地输入，避免客户端预测输入破坏镜头表现。
 */
public class SyncPlayerInputLockS2CPacket
{
    private final boolean locked;

    public SyncPlayerInputLockS2CPacket(boolean locked)
    {
        this.locked = locked;
    }

    public static void encode(SyncPlayerInputLockS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeBoolean(packet.locked);
    }

    public static SyncPlayerInputLockS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncPlayerInputLockS2CPacket(buffer.readBoolean());
    }

    public static void handle(SyncPlayerInputLockS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> PlayerInputLockController.setLocked(packet.locked));
        context.setPacketHandled(true);
    }
}
