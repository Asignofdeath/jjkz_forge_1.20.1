package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.network.ModNetwork;
import com.darkness.jujutsukaisenzone.technique.development.SkillUnlockLogic;
import com.darkness.jujutsukaisenzone.technique.development.SkillUpgradeLogic;
import com.darkness.jujutsukaisenzone.technique.development.SkillUpgradeResult;
import com.darkness.jujutsukaisenzone.technique.skillset.TechniqueSkill;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端 → 服务端：请求升级已解锁的主动技能。
 */
public final class UpgradeSkillC2SPacket
{
    private final String techniqueId;
    private final String skillId;

    public UpgradeSkillC2SPacket(String techniqueId, String skillId)
    {
        this.techniqueId = techniqueId;
        this.skillId = skillId;
    }

    public static void encode(UpgradeSkillC2SPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeUtf(packet.techniqueId);
        buffer.writeUtf(packet.skillId);
    }

    public static UpgradeSkillC2SPacket decode(FriendlyByteBuf buffer)
    {
        return new UpgradeSkillC2SPacket(buffer.readUtf(), buffer.readUtf());
    }

    public static void handle(UpgradeSkillC2SPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            ServerPlayer player = context.getSender();
            if (player == null)
            {
                return;
            }
            SkillUpgradeResult result = SkillUpgradeLogic.tryUpgradeSkill(player, packet.techniqueId, packet.skillId);
            String levelTag = SkillUnlockLogic.findSkillDefinition(packet.techniqueId, packet.skillId)
                    .map(TechniqueSkill::levelTag)
                    .orElse("");
            int level = SkillUpgradeLogic.getSkillLevel(player, packet.techniqueId, packet.skillId);
            ModNetwork.sendUpgradeSkillResult(player, result, packet.techniqueId, packet.skillId, levelTag, level);
        });
        context.setPacketHandled(true);
    }
}
