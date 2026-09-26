package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.client.gui.technique.TechniqueDevelopmentCanvasWidget;
import com.darkness.jujutsukaisenzone.technique.development.SkillLevelRules;
import com.darkness.jujutsukaisenzone.technique.development.SkillUpgradeResult;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 服务端 → 客户端：技能升级结果与 levelTag 同步。
 */
public final class UpgradeSkillS2CPacket
{
    private final SkillUpgradeResult result;
    private final String techniqueId;
    private final String skillId;
    private final String levelTag;
    private final int level;

    public UpgradeSkillS2CPacket(
            SkillUpgradeResult result,
            String techniqueId,
            String skillId,
            String levelTag,
            int level)
    {
        this.result = result;
        this.techniqueId = techniqueId;
        this.skillId = skillId;
        this.levelTag = levelTag == null ? "" : levelTag;
        this.level = SkillLevelRules.clampLevel(level);
    }

    public static void encode(UpgradeSkillS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeEnum(packet.result);
        buffer.writeUtf(packet.techniqueId);
        buffer.writeUtf(packet.skillId);
        buffer.writeUtf(packet.levelTag);
        buffer.writeVarInt(packet.level);
    }

    public static UpgradeSkillS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new UpgradeSkillS2CPacket(
                buffer.readEnum(SkillUpgradeResult.class),
                buffer.readUtf(),
                buffer.readUtf(),
                buffer.readUtf(),
                buffer.readVarInt());
    }

    public static void handle(UpgradeSkillS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            Player player = Minecraft.getInstance().player;
            if (player == null)
            {
                return;
            }
            if ((packet.result == SkillUpgradeResult.SUCCESS || packet.result == SkillUpgradeResult.ALREADY_MAX)
                    && !packet.levelTag.isBlank())
            {
                player.getPersistentData().putInt(packet.levelTag, packet.level);
            }
            if (packet.result != SkillUpgradeResult.ALREADY_MAX)
            {
                player.displayClientMessage(packet.result.toMessage(), false);
            }
            TechniqueDevelopmentCanvasWidget.refreshIfOpen();
        });
        context.setPacketHandled(true);
    }
}
