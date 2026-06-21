package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.player.PlayerFactionData;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FactionEvents
{
    private FactionEvents()
    {
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerLoggedInEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player))
        {
            return;
        }
        if (PlayerFactionData.hasFaction(player))
        {
            return;
        }
        player.sendSystemMessage(buildFactionChoiceMessage());
    }
    private static MutableComponent buildFactionChoiceMessage()
    {
        MutableComponent line = Component.literal("选择阵营：");
        line.append(clickableLabel("【咒术师】", PlayerFactionData.FACTION_SORCERER));
        line.append(Component.literal("，"));
        line.append(clickableLabel("【诅咒师】", PlayerFactionData.FACTION_CURSE_USER));
        line.append(Component.literal("，"));
        line.append(clickableLabel("【咒灵】", PlayerFactionData.FACTION_CURSE_SPIRIT));
        line.append(Component.literal("，"));
        line.append(clickableLabel("【平民】", PlayerFactionData.FACTION_CIVILIAN));
        line.append(Component.literal("。需要教程帮助时可输入 /jjkz ask"));
        return line;
    }

    private static MutableComponent clickableLabel(String label, String factionId)
    {
        String command = "/jjkz select " + factionId;
        Style style = Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("点击选择该阵营")));
        return Component.literal(label).withStyle(style);
    }
}
