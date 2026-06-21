package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.client.gui.technique.TechniqueDevelopmentCanvasWidget;
import com.darkness.jujutsukaisenzone.technique.development.SkillUnlockResult;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 服务端 → 客户端：术式开发解锁结果与 unlockTag 同步。
 */
public final class UnlockSkillS2CPacket
{
    private final SkillUnlockResult result;
    private final String techniqueId;
    private final String nodeId;
    private final String unlockTag;

    public UnlockSkillS2CPacket(SkillUnlockResult result, String techniqueId, String nodeId, String unlockTag)
    {
        this.result = result;
        this.techniqueId = techniqueId;
        this.nodeId = nodeId;
        this.unlockTag = unlockTag == null ? "" : unlockTag;
    }

    public static void encode(UnlockSkillS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeEnum(packet.result);
        buffer.writeUtf(packet.techniqueId);
        buffer.writeUtf(packet.nodeId);
        buffer.writeUtf(packet.unlockTag);
    }

    public static UnlockSkillS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new UnlockSkillS2CPacket(
                buffer.readEnum(SkillUnlockResult.class),
                buffer.readUtf(),
                buffer.readUtf(),
                buffer.readUtf());
    }

    public static void handle(UnlockSkillS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            Player player = Minecraft.getInstance().player;
            if (player == null)
            {
                return;
            }

            if ((packet.result == SkillUnlockResult.SUCCESS || packet.result == SkillUnlockResult.ALREADY_UNLOCKED)
                    && packet.unlockTag != null
                    && !packet.unlockTag.isBlank())
            {
                player.getPersistentData().putBoolean(packet.unlockTag, true);
            }

            if (packet.result != SkillUnlockResult.ALREADY_UNLOCKED)
            {
                player.displayClientMessage(packet.result.toMessage(), false);
            }
            TechniqueDevelopmentCanvasWidget.refreshIfOpen();
        });
        context.setPacketHandled(true);
    }
}
