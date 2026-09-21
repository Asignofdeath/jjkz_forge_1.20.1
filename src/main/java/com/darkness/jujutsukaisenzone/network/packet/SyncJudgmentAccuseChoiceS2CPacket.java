package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.client.JudgmentAccuseChoiceClientState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class SyncJudgmentAccuseChoiceS2CPacket
{
    private final boolean active;
    private final int remainingTicks;
    private final String crimeChinese;
    private final String crimeEnglish;

    public SyncJudgmentAccuseChoiceS2CPacket(boolean active, int remainingTicks, String crimeChinese, String crimeEnglish)
    {
        this.active = active;
        this.remainingTicks = remainingTicks;
        this.crimeChinese = crimeChinese;
        this.crimeEnglish = crimeEnglish;
    }

    public static void encode(SyncJudgmentAccuseChoiceS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeBoolean(packet.active);
        buffer.writeVarInt(packet.remainingTicks);
        buffer.writeUtf(packet.crimeChinese);
        buffer.writeUtf(packet.crimeEnglish);
    }

    public static SyncJudgmentAccuseChoiceS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncJudgmentAccuseChoiceS2CPacket(
                buffer.readBoolean(),
                buffer.readVarInt(),
                buffer.readUtf(),
                buffer.readUtf());
    }

    public static void handle(SyncJudgmentAccuseChoiceS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> JudgmentAccuseChoiceClientState.applySync(
                packet.active,
                packet.remainingTicks,
                packet.crimeChinese,
                packet.crimeEnglish));
        context.setPacketHandled(true);
    }
}
