package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.Config;
import com.darkness.jujutsukaisenzone.network.NetworkSenders;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 服务端 → 客户端：把服务端当前生效的数值推给刚登录的客户端。
 * <p>
 * 联机时「以房主 / 服主的配置文件为唯一真相」就靠这一个包落地：客户端收到后<b>整个盖掉本地</b>
 * （包括它自己 {@code settings.toml} 里写的覆盖），见 {@link Config#applyServerOverrides(Map)}。
 * <p>
 * 只搬可覆盖项（数值与字符串），布尔开关与列表不进包 —— 它们两边都来自同一份代码默认值，
 * 玩家也改不动，本来就不可能分叉。
 */
public class SyncModConfigS2CPacket
{
    private final Map<String, String> values;

    public SyncModConfigS2CPacket(Map<String, String> values)
    {
        // 保持插入顺序便于排查日志；这份表之后不再改动。
        this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    public static void encode(SyncModConfigS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(packet.values.size());
        for (Map.Entry<String, String> entry : packet.values.entrySet())
        {
            buffer.writeUtf(entry.getKey());
            buffer.writeUtf(entry.getValue());
        }
    }

    public static SyncModConfigS2CPacket decode(FriendlyByteBuf buffer)
    {
        int size = buffer.readVarInt();
        Map<String, String> values = new LinkedHashMap<>();
        for (int i = 0; i < size; i++)
        {
            values.put(buffer.readUtf(), buffer.readUtf());
        }
        return new SyncModConfigS2CPacket(values);
    }

    public static void handle(SyncModConfigS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            // 单机 / 局域网房主：客户端与服务端在同一个 JVM 里，Config 的静态字段本来就是同一份，
            // 服务端那边已经按本地文件灌过了 ⇒ 这里再盖一次纯属白费（还要重跑一遍 loadDefaults）。
            if (Minecraft.getInstance().hasSingleplayerServer())
            {
                return;
            }
            Config.applyServerOverrides(packet.values);
        });
        context.setPacketHandled(true);
    }

    public static void send(ServerPlayer player)
    {
        NetworkSenders.toPlayer(player, new SyncModConfigS2CPacket(Config.exportServerOverrides()));
    }
}
