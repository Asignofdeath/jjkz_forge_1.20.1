package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.client.ShrinePowerSlashChargeHud;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 同步强力斩击锁定蓄力进度（0~1）；visible=false 时隐藏蓄力条。 */
public class SyncShrinePowerSlashChargeS2CPacket
{
    private final boolean visible;
    private final float progress;

    public SyncShrinePowerSlashChargeS2CPacket(boolean visible, float progress)
    {
        this.visible = visible;
        this.progress = progress;
    }

    public static void encode(SyncShrinePowerSlashChargeS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeBoolean(packet.visible);
        buffer.writeFloat(packet.progress);
    }

    public static SyncShrinePowerSlashChargeS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncShrinePowerSlashChargeS2CPacket(buffer.readBoolean(), buffer.readFloat());
    }

    public static void handle(SyncShrinePowerSlashChargeS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            if (packet.visible)
            {
                ShrinePowerSlashChargeHud.setChargeProgress(packet.progress);
            }
            else
            {
                ShrinePowerSlashChargeHud.hide();
            }
        });
        context.setPacketHandled(true);
    }
}
