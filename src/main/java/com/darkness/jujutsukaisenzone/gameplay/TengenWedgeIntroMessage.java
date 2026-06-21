package com.darkness.jujutsukaisenzone.gameplay;

import com.darkness.jujutsukaisenzone.player.PlayerFactionData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * 天元之楔开始时向参与玩家发送的背景介绍。
 */
public final class TengenWedgeIntroMessage
{
    private TengenWedgeIntroMessage()
    {
    }

    public static void sendToParticipant(ServerPlayer player, String factionId)
    {
        String factionName = PlayerFactionData.getDisplayName(factionId);
        player.sendSystemMessage(Component.literal("========== 天元之楔 ==========").withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.literal("你将为【" + factionName + "】阵营参战。").withStyle(ChatFormatting.YELLOW));
        player.sendSystemMessage(Component.literal(
                "天元大人是一位活了上千年的不死术师，依靠其强大的结界术，布置了覆盖日本全国的「结界」。"));
        player.sendSystemMessage(Component.literal(
                "他死后若干年，一个特殊的结界被发现，此结界中无法死去，且其中存在三件名为楔的特级咒具，"
                        + "当它们结合时，就能掌控全日本的结界。"));
        player.sendSystemMessage(Component.literal(
                "各方势力均为此而来，各自占据着一个楔，夺取所有的楔，为你的阵营获利吧。")
                .withStyle(ChatFormatting.AQUA));
    }
}
