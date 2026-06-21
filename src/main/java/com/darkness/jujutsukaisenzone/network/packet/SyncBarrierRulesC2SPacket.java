package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.player.BarrierRulesData;
import com.darkness.jujutsukaisenzone.player.PlayerBarrierTechniqueData;
import com.darkness.jujutsukaisenzone.player.PlayerReverseTechniqueData;
import com.darkness.jujutsukaisenzone.technique.BarrierTechniqueServerLogic;
import com.darkness.jujutsukaisenzone.technique.domain.DomainExpansionRouter;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端→服务端：同步玩家结界规则
 */
public class SyncBarrierRulesC2SPacket
{
    private final boolean blockMobSpawn;
    private final boolean autoRepairBarrier;

    public SyncBarrierRulesC2SPacket(boolean blockMobSpawn, boolean autoRepairBarrier)
    {
        this.blockMobSpawn = blockMobSpawn;
        this.autoRepairBarrier = autoRepairBarrier;
    }

    public static void encode(SyncBarrierRulesC2SPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeBoolean(packet.blockMobSpawn);
        buffer.writeBoolean(packet.autoRepairBarrier);
    }

    public static SyncBarrierRulesC2SPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncBarrierRulesC2SPacket(buffer.readBoolean(), buffer.readBoolean());
    }

    public static void handle(SyncBarrierRulesC2SPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            ServerPlayer player = context.getSender();
            if (player == null)
            {
                return;
            }

            if (!PlayerBarrierTechniqueData.hasLearned(player))
            {
                return;
            }

            boolean autoRepairBarrier = packet.autoRepairBarrier
                    && PlayerReverseTechniqueData.hasReverseTechnique(player);

            boolean oldAutoRepair = BarrierRulesData.isAutoRepairBarrier(player.getUUID());
            BarrierRulesData.setBlockMobSpawn(player.getUUID(), packet.blockMobSpawn);
            BarrierRulesData.setAutoRepairBarrier(player.getUUID(), autoRepairBarrier);

            BarrierTechniqueServerLogic.staticUpdateBarrierRules(player);
            DomainExpansionRouter.updateBarrierRulesAll(player);

            if (autoRepairBarrier != oldAutoRepair)
            {
                String status = autoRepairBarrier ? "已启用" : "已禁用";
                player.displayClientMessage(
                        Component.literal("自动修补结界规则：" + status).withStyle(ChatFormatting.GREEN),
                        true
                );
            }

            if (autoRepairBarrier)
            {
                BarrierTechniqueServerLogic.staticRepairBarrierGaps(player);
                DomainExpansionRouter.repairBarrierGapsAll(player);
            }

            SyncBarrierRulesS2CPacket.send(player);
        });
        context.setPacketHandled(true);
    }
}
