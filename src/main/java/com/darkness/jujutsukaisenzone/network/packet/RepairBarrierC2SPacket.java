package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.technique.BarrierTechniqueServerLogic;
import com.darkness.jujutsukaisenzone.technique.domain.DomainExpansionRouter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RepairBarrierC2SPacket
{
    public RepairBarrierC2SPacket()
    {
    }

    public static void encode(RepairBarrierC2SPacket packet, FriendlyByteBuf buffer)
    {
    }

    public static RepairBarrierC2SPacket decode(FriendlyByteBuf buffer)
    {
        return new RepairBarrierC2SPacket();
    }

    public static void handle(RepairBarrierC2SPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            ServerPlayer player = context.getSender();
            if (player == null)
            {
                return;
            }
            /* 同时修补普通结界与所有类型领域结界（含天空盒） */
            BarrierTechniqueServerLogic.staticRepairBarrierGaps(player);
            DomainExpansionRouter.repairBarrierGapsAll(player);
        });
        context.setPacketHandled(true);
    }
}
