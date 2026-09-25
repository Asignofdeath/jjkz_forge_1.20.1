package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.player.PlayerCombatPowerData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 强度面板下发：体术强度 / 术式强度。 */
public class SyncCombatPowerS2CPacket
{
    private final int physicalPower;
    private final int techniquePower;

    public SyncCombatPowerS2CPacket(int physicalPower, int techniquePower)
    {
        this.physicalPower = physicalPower;
        this.techniquePower = techniquePower;
    }

    public static void encode(SyncCombatPowerS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeInt(packet.physicalPower);
        buffer.writeInt(packet.techniquePower);
    }

    public static SyncCombatPowerS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncCombatPowerS2CPacket(buffer.readInt(), buffer.readInt());
    }

    public static void handle(SyncCombatPowerS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            Player player = Minecraft.getInstance().player;
            if (player == null)
            {
                return;
            }
            PlayerCombatPowerData.applyClientSnapshot(player, packet.physicalPower, packet.techniquePower);
        });
        context.setPacketHandled(true);
    }
}
