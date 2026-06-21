package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.network.ModNetwork;
import com.darkness.jujutsukaisenzone.technique.development.SkillUnlockLogic;
import com.darkness.jujutsukaisenzone.technique.development.SkillUnlockResult;
import com.darkness.jujutsukaisenzone.technique.development.SkillTreeNodeDef;
import com.darkness.jujutsukaisenzone.technique.development.TechniqueSkillTreeRegistry;
import com.darkness.jujutsukaisenzone.technique.skillset.TechniqueSkill;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端 → 服务端：在术式开发界面请求解锁技能节点。
 */
public final class UnlockSkillC2SPacket
{
    private final String techniqueId;
    private final String nodeId;

    public UnlockSkillC2SPacket(String techniqueId, String nodeId)
    {
        this.techniqueId = techniqueId;
        this.nodeId = nodeId;
    }

    public static void encode(UnlockSkillC2SPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeUtf(packet.techniqueId);
        buffer.writeUtf(packet.nodeId);
    }

    public static UnlockSkillC2SPacket decode(FriendlyByteBuf buffer)
    {
        return new UnlockSkillC2SPacket(buffer.readUtf(), buffer.readUtf());
    }

    public static void handle(UnlockSkillC2SPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            ServerPlayer player = context.getSender();
            if (player == null)
            {
                return;
            }

            SkillUnlockResult result = SkillUnlockLogic.tryUnlockSkill(player, packet.techniqueId, packet.nodeId);
            String unlockTag = TechniqueSkillTreeRegistry.get(packet.techniqueId)
                    .flatMap(tree -> tree.findNode(packet.nodeId))
                    .filter(SkillTreeNodeDef::isSkillNode)
                    .flatMap(node -> SkillUnlockLogic.findSkillDefinition(packet.techniqueId, node.skillId()))
                    .map(TechniqueSkill::unlockTag)
                    .orElse("");
            if (result == SkillUnlockResult.SUCCESS || result == SkillUnlockResult.ALREADY_UNLOCKED)
            {
                com.darkness.jujutsukaisenzone.technique.skillset.TechniqueSkillSetRegistry.syncSkillBarToClient(player);
            }
            ModNetwork.sendUnlockSkillResult(player, result, packet.techniqueId, packet.nodeId, unlockTag);
        });
        context.setPacketHandled(true);
    }
}
