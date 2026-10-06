package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.client.gui.quest.MainQuestJoinOfferScreen;
import com.darkness.jujutsukaisenzone.gameplay.ModRules;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 服务端 → 客户端：第一次进世界询问；管理员可带上当前全服规则。 */
public class OpenMainQuestJoinOfferS2CPacket
{
    private final boolean openIfClosed;
    private final boolean admin;
    private final boolean aerialEnabled;
    private final boolean skillNoCooldown;
    private final byte hungerOrdinal;
    private final boolean stylizedEnabled;

    public OpenMainQuestJoinOfferS2CPacket(
            boolean openIfClosed,
            boolean admin,
            boolean aerialEnabled,
            boolean skillNoCooldown,
            byte hungerOrdinal,
            boolean stylizedEnabled)
    {
        this.openIfClosed = openIfClosed;
        this.admin = admin;
        this.aerialEnabled = aerialEnabled;
        this.skillNoCooldown = skillNoCooldown;
        this.hungerOrdinal = hungerOrdinal;
        this.stylizedEnabled = stylizedEnabled;
    }

    public static OpenMainQuestJoinOfferS2CPacket firstJoin(ServerPlayer player)
    {
        return snapshot(player, true);
    }

    public static OpenMainQuestJoinOfferS2CPacket refresh(ServerPlayer player)
    {
        return snapshot(player, false);
    }

    private static OpenMainQuestJoinOfferS2CPacket snapshot(ServerPlayer player, boolean openIfClosed)
    {
        boolean admin = player.hasPermissions(net.minecraft.commands.Commands.LEVEL_GAMEMASTERS);
        ModRules.HungerConsumption.Mode hunger = ModRules.HungerConsumption.getMode();
        return new OpenMainQuestJoinOfferS2CPacket(
                openIfClosed,
                admin,
                ModRules.AerialCombat.isEnabled(),
                ModRules.SkillNoCooldown.isEnabled(),
                (byte) hunger.ordinal(),
                ModRules.StylizedDamageStyles.isEnabled());
    }

    public static void encode(OpenMainQuestJoinOfferS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeBoolean(packet.openIfClosed);
        buffer.writeBoolean(packet.admin);
        buffer.writeBoolean(packet.aerialEnabled);
        buffer.writeBoolean(packet.skillNoCooldown);
        buffer.writeByte(packet.hungerOrdinal);
        buffer.writeBoolean(packet.stylizedEnabled);
    }

    public static OpenMainQuestJoinOfferS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new OpenMainQuestJoinOfferS2CPacket(
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readByte(),
                buffer.readBoolean());
    }

    public static void handle(OpenMainQuestJoinOfferS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> open(packet));
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void open(OpenMainQuestJoinOfferS2CPacket packet)
    {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof MainQuestJoinOfferScreen screen)
        {
            screen.applySnapshot(
                    packet.admin,
                    packet.aerialEnabled,
                    packet.skillNoCooldown,
                    packet.hungerOrdinal,
                    packet.stylizedEnabled);
            return;
        }
        if (!packet.openIfClosed)
        {
            return;
        }
        minecraft.setScreen(new MainQuestJoinOfferScreen(
                packet.admin,
                packet.aerialEnabled,
                packet.skillNoCooldown,
                packet.hungerOrdinal,
                packet.stylizedEnabled));
    }
}
