package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.client.JudgmentTrialChargeHud;
import com.darkness.jujutsukaisenzone.client.JudgmentTrialClientState;
import com.darkness.jujutsukaisenzone.client.JudgmentTrialLockOnClientHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 同步「审判」锁定阶段是否激活。 */
public class SyncJudgmentTrialActiveS2CPacket
{
    private final boolean active;

    public SyncJudgmentTrialActiveS2CPacket(boolean active)
    {
        this.active = active;
    }

    public static void encode(SyncJudgmentTrialActiveS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeBoolean(packet.active);
    }

    public static SyncJudgmentTrialActiveS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncJudgmentTrialActiveS2CPacket(buffer.readBoolean());
    }

    public static void handle(SyncJudgmentTrialActiveS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            JudgmentTrialClientState.setTrialLockActive(packet.active);
            if (packet.active)
            {
                JudgmentTrialChargeHud.showAwaitingLock();
                JudgmentTrialLockOnClientHandler.onSessionStarted();
            }
            else
            {
                JudgmentTrialChargeHud.hide();
                JudgmentTrialLockOnClientHandler.onSessionEnded();
            }
        });
        context.setPacketHandled(true);
    }
}
