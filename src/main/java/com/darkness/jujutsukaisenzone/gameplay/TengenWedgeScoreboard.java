package com.darkness.jujutsukaisenzone.gameplay;



import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;

import net.minecraft.network.chat.Component;

import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;

import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;

import net.minecraft.network.protocol.game.ClientboundSetScorePacket;

import net.minecraft.server.ServerScoreboard;

import net.minecraft.server.level.ServerPlayer;

import net.minecraft.world.scores.Objective;

import net.minecraft.world.scores.Scoreboard;

import net.minecraft.world.scores.criteria.ObjectiveCriteria;



import java.util.Arrays;

import java.util.List;

import java.util.Map;

import java.util.UUID;

import java.util.concurrent.ConcurrentHashMap;



/**

 * 天元之楔 setup 玩家在结界内时，向其客户端单独推送侧边栏计分板。

 */

public final class TengenWedgeScoreboard

{

    private static final Map<UUID, String[]> LAST_LINES = new ConcurrentHashMap<>();

    private static final Map<UUID, Boolean> SHOWING = new ConcurrentHashMap<>();



    private TengenWedgeScoreboard()

    {

    }



    public static void update(ServerPlayer player, int completionPercent, int wedgeCount, int spawnPointCount)

    {

        String completionLine = "结界完成度: " + completionPercent + "%";

        String wedgeLine = "楔: " + wedgeCount + "/3";

        String spawnLine = "咒灵刷新点: " + spawnPointCount + "个（至少需" + TengenWedgeCurseSpiritSpawns.MIN_REQUIRED + "个）";

        pushLines(player, Component.literal("天元之楔"), new String[]{completionLine, wedgeLine, spawnLine});

    }



    /** 进行中：显示状态与同阵营队友名称。 */

    public static void updateTeammates(ServerPlayer player, List<String> teammateNames)

    {

        String[] lines = new String[1 + teammateNames.size()];

        lines[0] = "状态: " + TengenWedgeBarrierState.IN_PROGRESS.getDisplayName();

        for (int i = 0; i < teammateNames.size(); i++)

        {

            lines[i + 1] = teammateNames.get(i);

        }

        pushLines(player, Component.literal("天元之楔"), lines);

    }



    public static void hide(ServerPlayer player)

    {

        UUID playerId = player.getUUID();

        if (!SHOWING.containsKey(playerId))

        {

            return;

        }

        SHOWING.remove(playerId);



        player.connection.send(new ClientboundSetDisplayObjectivePacket(Scoreboard.DISPLAY_SLOT_SIDEBAR, null));



        String[] last = LAST_LINES.remove(playerId);

        String objectiveId = objectiveId(playerId);

        if (last != null)

        {

            for (String line : last)

            {

                removeLine(player, objectiveId, line);

            }

        }



        ServerScoreboard scoreboard = player.server.getScoreboard();

        Objective objective = scoreboard.getObjective(objectiveId);

        if (objective != null)

        {

            scoreboard.removeObjective(objective);

        }

    }



    private static void pushLines(ServerPlayer player, Component title, String[] lines)

    {

        String[] last = LAST_LINES.get(player.getUUID());

        if (last != null && Arrays.equals(last, lines) && SHOWING.containsKey(player.getUUID()))

        {

            return;

        }



        ServerScoreboard scoreboard = player.server.getScoreboard();

        String objectiveId = objectiveId(player.getUUID());

        Objective objective = scoreboard.getObjective(objectiveId);

        boolean created = objective == null;

        if (created)

        {

            objective = scoreboard.addObjective(

                    objectiveId,

                    ObjectiveCriteria.DUMMY,

                    title,

                    ObjectiveCriteria.RenderType.INTEGER);

        }



        int method = created ? ClientboundSetObjectivePacket.METHOD_ADD : ClientboundSetObjectivePacket.METHOD_CHANGE;

        player.connection.send(new ClientboundSetObjectivePacket(objective, method));



        if (last != null)

        {

            for (String line : last)

            {

                removeLine(player, objectiveId, line);

            }

        }



        for (int i = 0; i < lines.length; i++)

        {

            sendLine(player, objectiveId, lines[i], lines.length - i);

        }

        player.connection.send(new ClientboundSetDisplayObjectivePacket(Scoreboard.DISPLAY_SLOT_SIDEBAR, objective));



        LAST_LINES.put(player.getUUID(), lines.clone());

        SHOWING.put(player.getUUID(), Boolean.TRUE);

    }



    private static void sendLine(ServerPlayer player, String objectiveId, String text, int score)

    {

        player.connection.send(new ClientboundSetScorePacket(

                ServerScoreboard.Method.CHANGE,

                objectiveId,

                text,

                score));

    }



    private static void removeLine(ServerPlayer player, String objectiveId, String text)

    {

        player.connection.send(new ClientboundSetScorePacket(

                ServerScoreboard.Method.REMOVE,

                objectiveId,

                text,

                0));

    }



    private static String objectiveId(UUID playerId)

    {

        return JuJutsuKaisenZone.MODID + "_tw_" + playerId.toString().substring(0, 8);

    }

}

