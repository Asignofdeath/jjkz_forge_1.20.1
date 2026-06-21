package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.client.ClientSkillBarCache;
import com.darkness.jujutsukaisenzone.client.gui.SkillBarConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.Arrays;
import java.util.function.Supplier;

/**
 * 服务端 → 客户端：同步指定术式的 9 栏位快捷栏配置。
 */
public class SyncSkillBarConfigS2CPacket
{
    private final String techniqueId;
    private final String[] skillIds; // 索引 0~8 对应槽位 1~9

    public SyncSkillBarConfigS2CPacket(String techniqueId, String[] skillIds)
    {
        this.techniqueId = techniqueId;
        this.skillIds = skillIds;
    }

    public static void encode(SyncSkillBarConfigS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeUtf(packet.techniqueId);
        for (int i = 0; i < 9; i++)
        {
            buffer.writeUtf(packet.skillIds[i]);
        }
    }

    public static SyncSkillBarConfigS2CPacket decode(FriendlyByteBuf buffer)
    {
        String tid = buffer.readUtf();
        String[] ids = new String[9];
        for (int i = 0; i < 9; i++)
        {
            ids[i] = buffer.readUtf();
        }
        return new SyncSkillBarConfigS2CPacket(tid, ids);
    }

    @OnlyIn(Dist.CLIENT)
    public static void handle(SyncSkillBarConfigS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            ClientSkillBarCache.put(packet.techniqueId, packet.skillIds);
            SkillBarConfigScreen.onConfigSync(packet.techniqueId, packet.skillIds);
        });
        context.setPacketHandled(true);
    }
}
