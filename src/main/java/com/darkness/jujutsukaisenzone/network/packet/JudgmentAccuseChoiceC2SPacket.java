package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.technique.domain.JudgmentDomainTrialLogic;
import com.darkness.jujutsukaisenzone.technique.skill.JudgmentAccuseSkillLogic;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class JudgmentAccuseChoiceC2SPacket
{
    private final int responseId;

    public JudgmentAccuseChoiceC2SPacket(int responseId)
    {
        this.responseId = responseId;
    }

    public static void encode(JudgmentAccuseChoiceC2SPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(packet.responseId);
    }

    public static JudgmentAccuseChoiceC2SPacket decode(FriendlyByteBuf buffer)
    {
        return new JudgmentAccuseChoiceC2SPacket(buffer.readVarInt());
    }

    public static void handle(JudgmentAccuseChoiceC2SPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            ServerPlayer player = context.getSender();
            if (player == null)
            {
                return;
            }
            if (!JudgmentDomainTrialLogic.submitPlayerResponse(player, packet.responseId))
            {
                JudgmentAccuseSkillLogic.submitPlayerResponse(player, packet.responseId);
            }
        });
        context.setPacketHandled(true);
    }
}
