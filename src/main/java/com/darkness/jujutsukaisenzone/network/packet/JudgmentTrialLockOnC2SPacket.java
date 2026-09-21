package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.technique.skill.JudgmentTrialSkillLogic;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/** 「审判」：同步 Epic Fight 锁定状态与目标。 */
public class JudgmentTrialLockOnC2SPacket
{
    private final boolean locking;
    private final UUID targetUuid;

    public JudgmentTrialLockOnC2SPacket(boolean locking, UUID targetUuid)
    {
        this.locking = locking;
        this.targetUuid = targetUuid;
    }

    public static void encode(JudgmentTrialLockOnC2SPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeBoolean(packet.locking);
        buffer.writeBoolean(packet.targetUuid != null);
        if (packet.targetUuid != null)
        {
            buffer.writeUUID(packet.targetUuid);
        }
    }

    public static JudgmentTrialLockOnC2SPacket decode(FriendlyByteBuf buffer)
    {
        boolean locking = buffer.readBoolean();
        UUID targetUuid = buffer.readBoolean() ? buffer.readUUID() : null;
        return new JudgmentTrialLockOnC2SPacket(locking, targetUuid);
    }

    public static void handle(JudgmentTrialLockOnC2SPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            ServerPlayer player = context.getSender();
            if (player == null)
            {
                return;
            }
            JudgmentTrialSkillLogic.updateLockOn(player, packet.locking, packet.targetUuid);
        });
        context.setPacketHandled(true);
    }
}
