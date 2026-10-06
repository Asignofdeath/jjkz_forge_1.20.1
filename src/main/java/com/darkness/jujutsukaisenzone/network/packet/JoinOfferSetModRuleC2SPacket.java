package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.compat.StylizedDamageIntegrationRegistrar;
import com.darkness.jujutsukaisenzone.gameplay.ModRules;
import com.darkness.jujutsukaisenzone.network.ModNetwork;
import com.darkness.jujutsukaisenzone.network.NetworkSenders;
import net.minecraft.commands.Commands;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 客户端 → 服务端：第一次进世界界面改全服 ModRules。 */
public class JoinOfferSetModRuleC2SPacket
{
    public static final byte RULE_AERIAL = 0;
    public static final byte RULE_SKILL_NO_COOLDOWN = 1;
    public static final byte RULE_HUNGER = 2;
    public static final byte RULE_STYLIZED = 3;

    private final byte rule;
    private final int value;

    public JoinOfferSetModRuleC2SPacket(byte rule, int value)
    {
        this.rule = rule;
        this.value = value;
    }

    public static void encode(JoinOfferSetModRuleC2SPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeByte(packet.rule);
        buffer.writeVarInt(packet.value);
    }

    public static JoinOfferSetModRuleC2SPacket decode(FriendlyByteBuf buffer)
    {
        return new JoinOfferSetModRuleC2SPacket(buffer.readByte(), buffer.readVarInt());
    }

    public static void handle(JoinOfferSetModRuleC2SPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            ServerPlayer player = context.getSender();
            if (player == null || !player.hasPermissions(Commands.LEVEL_GAMEMASTERS))
            {
                return;
            }
            apply(player, packet.rule, packet.value);
            NetworkSenders.toPlayer(player, OpenMainQuestJoinOfferS2CPacket.refresh(player));
        });
        context.setPacketHandled(true);
    }

    private static void apply(ServerPlayer player, byte rule, int value)
    {
        if (rule == RULE_AERIAL)
        {
            String failure = ModRules.AerialCombat.trySetEnabled(value != 0);
            if (failure != null)
            {
                player.sendSystemMessage(Component.literal(failure));
            }
            return;
        }
        if (rule == RULE_SKILL_NO_COOLDOWN)
        {
            ModRules.SkillNoCooldown.setEnabled(value != 0);
            return;
        }
        if (rule == RULE_HUNGER)
        {
            ModRules.HungerConsumption.Mode[] modes = ModRules.HungerConsumption.Mode.values();
            if (value < 0 || value >= modes.length)
            {
                return;
            }
            ModRules.HungerConsumption.setMode(modes[value]);
            return;
        }
        if (rule == RULE_STYLIZED)
        {
            boolean enabled = value != 0;
            String failure = ModRules.StylizedDamageStyles.trySetEnabled(enabled);
            if (failure != null)
            {
                player.sendSystemMessage(Component.literal(failure));
                return;
            }
            StylizedDamageIntegrationRegistrar.registerStylesIfPresent();
            ModNetwork.syncStylizedDamageStylesRuleToAll(enabled);
        }
    }
}
