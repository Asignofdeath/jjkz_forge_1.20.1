package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.technique.domain.ShrineDomainExpansion;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 伏魔神龛领域内：施法者单击空格时请求传送到神龛碰撞箱顶部边缘。
 */
public class ShrineDomainTeleportC2SPacket
{
    private final float facingYaw;

    public ShrineDomainTeleportC2SPacket(float facingYaw)
    {
        this.facingYaw = facingYaw;
    }

    public float facingYaw()
    {
        return facingYaw;
    }

    public static void encode(ShrineDomainTeleportC2SPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeFloat(packet.facingYaw);
    }

    public static ShrineDomainTeleportC2SPacket decode(FriendlyByteBuf buffer)
    {
        return new ShrineDomainTeleportC2SPacket(buffer.readFloat());
    }

    public static void handle(ShrineDomainTeleportC2SPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            ServerPlayer player = context.getSender();
            if (player == null)
            {
                return;
            }
            ShrineDomainExpansion.getInstance().tryTeleportCasterToShrineTop(player, packet.facingYaw());
        });
        context.setPacketHandled(true);
    }
}
