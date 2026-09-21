package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.client.JudgmentTrialChargeHud;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 同步「审判」锁定蓄力进度（0~1）；visible=false 时隐藏蓄力条。 */
public class SyncJudgmentTrialChargeS2CPacket
{
    private final boolean visible;
    private final float progress;

    public SyncJudgmentTrialChargeS2CPacket(boolean visible, float progress)
    {
        this.visible = visible;
        this.progress = progress;
    }

    public static void encode(SyncJudgmentTrialChargeS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeBoolean(packet.visible);
        buffer.writeFloat(packet.progress);
    }

    public static SyncJudgmentTrialChargeS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncJudgmentTrialChargeS2CPacket(buffer.readBoolean(), buffer.readFloat());
    }

    public static void handle(SyncJudgmentTrialChargeS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            if (packet.visible)
            {
                JudgmentTrialChargeHud.setChargeProgress(packet.progress);
            }
            else
            {
                JudgmentTrialChargeHud.hide();
            }
        });
        context.setPacketHandled(true);
    }
}
