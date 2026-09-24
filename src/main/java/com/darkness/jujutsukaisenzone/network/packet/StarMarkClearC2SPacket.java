package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.technique.skillset.LoveRendezvousTechniqueSkillSet;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端 → 服务端：战斗模式下快速双击 Shift 触发的「星星标记」清除。
 * <p>
 * 刻意不带任何字段：清除走哪个分支（单独清除 / 全部清除）完全由服务端按准星自行判定，
 * 客户端只上报「发生了双击」——与「看向方块按技能键打标」同一个思路，避免客户端参与目标选择。
 * 服务端会在 {@link LoveRendezvousTechniqueSkillSet#clearByCrosshair(ServerPlayer)} 里
 * 重新校验战斗模式与生得术式。
 */
public class StarMarkClearC2SPacket
{
    public StarMarkClearC2SPacket()
    {
    }

    public static void encode(StarMarkClearC2SPacket packet, FriendlyByteBuf buffer)
    {
        // 无字段
    }

    public static StarMarkClearC2SPacket decode(FriendlyByteBuf buffer)
    {
        return new StarMarkClearC2SPacket();
    }

    public static void handle(StarMarkClearC2SPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            ServerPlayer player = context.getSender();
            if (player == null)
            {
                return;
            }
            LoveRendezvousTechniqueSkillSet.INSTANCE.clearByCrosshair(player);
        });
        context.setPacketHandled(true);
    }
}
