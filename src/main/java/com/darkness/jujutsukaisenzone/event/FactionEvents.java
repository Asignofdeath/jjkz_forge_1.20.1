package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.player.PlayerFactionData;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;

/**
 * 阵营选择提示。开局不再自动弹出，叙事流程需要时再调用 {@link #sendFactionChoiceMessage}。
 */
public final class FactionEvents
{
    private FactionEvents()
    {
    }

    public static void sendFactionChoiceMessage(ServerPlayer player)
    {
        player.sendSystemMessage(buildFactionChoiceMessage());
    }

    public static MutableComponent buildFactionChoiceMessage()
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
