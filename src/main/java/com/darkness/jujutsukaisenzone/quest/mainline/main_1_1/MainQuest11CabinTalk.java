package com.darkness.jujutsukaisenzone.quest.mainline.main_1_1;

import com.darkness.jujutsukaisenzone.cg.PlayerCgLock;
import com.darkness.jujutsukaisenzone.entity.CutsceneActorEntity;
import com.darkness.jujutsukaisenzone.network.NetworkSenders;
import com.darkness.jujutsukaisenzone.network.packet.SyncQuestLetterboxS2CPacket;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * 主线 1.1 三组巡镜结束后的电影黑幕对话 + Geo 角色轨（物理朝向 / 动画分开）。
 * 这套对话不再加 cmdcam。
 */
public final class MainQuest11CabinTalk
{
    public static final int LINE_TICKS = 70;
    public static final int GAP_TICKS = 10;
    /** 语音播完后再留的字幕余韵。 */
    private static final int VOICE_HOLD_TICKS = 10;
    public static final String[] DIALOG_KEYS = {
            "quest.jujutsukaisenzone.main_1_1.talk.1",
            "quest.jujutsukaisenzone.main_1_1.talk.2",
            "quest.jujutsukaisenzone.main_1_1.talk.3",
            "quest.jujutsukaisenzone.main_1_1.talk.4",
            "quest.jujutsukaisenzone.main_1_1.talk.5",
            "quest.jujutsukaisenzone.main_1_1.talk.6",
            "quest.jujutsukaisenzone.main_1_1.talk.7",
            "quest.jujutsukaisenzone.main_1_1.talk.8",
            "quest.jujutsukaisenzone.main_1_1.talk.9",
            "quest.jujutsukaisenzone.main_1_1.talk.10",
            "quest.jujutsukaisenzone.main_1_1.talk.11"
    };
    /** 各句字幕停留刻：有语音 = ogg 时长 + 余韵；玩家无语音句用 {@link #LINE_TICKS}。 */
    private static final int[] LINE_HOLD_TICKS = {
            129 + VOICE_HOLD_TICKS,
            33 + VOICE_HOLD_TICKS,
            75 + VOICE_HOLD_TICKS,
            101 + VOICE_HOLD_TICKS,
            109 + VOICE_HOLD_TICKS,
            LINE_TICKS,
            29 + VOICE_HOLD_TICKS,
            165 + VOICE_HOLD_TICKS,
            LINE_TICKS,
            26 + VOICE_HOLD_TICKS,
            116 + VOICE_HOLD_TICKS
    };

    private static final int DIALOG_END_TICKS = computeDialogEndTicks();
    private static final Map<UUID, TalkSession> SESSIONS = new ConcurrentHashMap<>();

    private MainQuest11CabinTalk()
    {
    }

    public static boolean isRunning(ServerPlayer player)
    {
        return SESSIONS.containsKey(player.getUUID());
    }

    public static void start(ServerPlayer player)
    {
        TalkSession session = new TalkSession();
        session.shownCaption = 0;
        SESSIONS.put(player.getUUID(), session);
        beginCgLook(player);
        showLine(player, 0);
        applyLineStart(player, 0);
    }

    public static void abort(ServerPlayer player)
    {
        if (SESSIONS.remove(player.getUUID()) == null)
        {
            return;
        }
        NetworkSenders.toPlayer(player, SyncQuestLetterboxS2CPacket.clear());
        PlayerCgLock.end(player);
    }

    public static void tick(ServerPlayer player)
    {
        TalkSession session = SESSIONS.get(player.getUUID());
        if (session == null)
        {
            return;
        }
        PlayerCgLock.tick(player);

        int captionIndex = captionIndexForTick(session.ticks);
        if (captionIndex != session.shownCaption)
        {
            session.shownCaption = captionIndex;
            session.midApplied = false;
            if (captionIndex >= 0)
            {
                showLine(player, captionIndex);
                applyLineStart(player, captionIndex);
            }
            else
            {
                NetworkSenders.toPlayer(player, new SyncQuestLetterboxS2CPacket(true, ""));
            }
        }
        else if (captionIndex >= 0)
        {
            int into = intoLineForTick(session.ticks);
            int hold = LINE_HOLD_TICKS[captionIndex];
            if (!session.midApplied && into >= hold / 2 && into < hold)
            {
                session.midApplied = true;
                applyLineMid(player, captionIndex);
            }
        }

        session.ticks++;
        if (session.ticks >= DIALOG_END_TICKS)
        {
            SESSIONS.remove(player.getUUID());
            NetworkSenders.toPlayer(player, SyncQuestLetterboxS2CPacket.clear());
            PlayerCgLock.end(player);
        }
    }

    /** 对准 KiRo / 小石子中点（截图那一帧：左工作台、右床）。 */
    private static void beginCgLook(ServerPlayer player)
    {
        CutsceneActorEntity kiro = MainQuest11CabinActors.actor(player, CutsceneActorEntity.APPEARANCE_KIRO);
        CutsceneActorEntity shi = MainQuest11CabinActors.actor(player, CutsceneActorEntity.APPEARANCE_XIAOSHIZI);
        Vec3 eyes = player.position().add(0.0D, player.getEyeHeight(), 0.0D);
        Vec3 target;
        if (kiro != null && shi != null)
        {
            Vec3 kiroHead = kiro.position().add(0.0D, 1.4D, 0.0D);
            Vec3 shiHead = shi.position().add(0.0D, 1.0D, 0.0D);
            target = kiroHead.add(shiHead).scale(0.5D);
        }
        else if (kiro != null)
        {
            target = kiro.position().add(0.0D, 1.4D, 0.0D);
        }
        else if (shi != null)
        {
            target = shi.position().add(0.0D, 1.0D, 0.0D);
        }
        else
        {
            target = eyes.add(player.getLookAngle());
        }
        PlayerCgLock.beginLookAt(
                player,
                player.position(),
                target,
                PlayerCgLock.DEFAULT_YAW_HALF,
                PlayerCgLock.DEFAULT_PITCH_HALF);
    }

    private static void showLine(ServerPlayer player, int index)
    {
        NetworkSenders.toPlayer(player, new SyncQuestLetterboxS2CPacket(true, DIALOG_KEYS[index]));
    }

    private static int computeDialogEndTicks()
    {
        int total = 0;
        for (int i = 0; i < LINE_HOLD_TICKS.length; i++)
        {
            total += LINE_HOLD_TICKS[i];
            if (i < LINE_HOLD_TICKS.length - 1)
            {
                total += GAP_TICKS;
            }
        }
        return total;
    }

    private static int captionIndexForTick(int ticks)
    {
        int remaining = ticks;
        for (int i = 0; i < LINE_HOLD_TICKS.length; i++)
        {
            int hold = LINE_HOLD_TICKS[i];
            if (remaining < hold)
            {
                return i;
            }
            remaining -= hold;
            if (i == LINE_HOLD_TICKS.length - 1)
            {
                return i;
            }
            if (remaining < GAP_TICKS)
            {
                return -1;
            }
            remaining -= GAP_TICKS;
        }
        return LINE_HOLD_TICKS.length - 1;
    }

    private static int intoLineForTick(int ticks)
    {
        int remaining = ticks;
        for (int i = 0; i < LINE_HOLD_TICKS.length; i++)
        {
            int hold = LINE_HOLD_TICKS[i];
            if (remaining < hold)
            {
                return remaining;
            }
            remaining -= hold;
            if (i == LINE_HOLD_TICKS.length - 1)
            {
                return hold;
            }
            remaining -= GAP_TICKS;
        }
        return 0;
    }

    private static void applyLineStart(ServerPlayer player, int index)
    {
        CutsceneActorEntity kiro = MainQuest11CabinActors.actor(player, CutsceneActorEntity.APPEARANCE_KIRO);
        CutsceneActorEntity shi = MainQuest11CabinActors.actor(player, CutsceneActorEntity.APPEARANCE_XIAOSHIZI);
        Vec3 playerEyes = player.position().add(0.0D, player.getEyeHeight(), 0.0D);
        switch (index)
        {
            case 0 ->
            {
                if (kiro != null)
                {
                    kiro.setClip(CutsceneActorEntity.CLIP_KIRO_STAND_IDLE, true);
                    kiro.lookAt(playerEyes);
                }
            }
            case 1 ->
            {
                if (shi != null && kiro != null)
                {
                    shi.lookAt(kiro.position().add(0.0D, 1.4D, 0.0D));
                }
            }
            case 2 ->
            {
                if (kiro != null && shi != null)
                {
                    kiro.lookAt(shi.position().add(0.0D, 1.0D, 0.0D));
                }
            }
            case 3 ->
            {
                if (shi != null)
                {
                    shi.lookAt(playerEyes);
                    shi.playOnceThen(
                            CutsceneActorEntity.CLIP_XIAOSHIZI_LOOKDOWN,
                            CutsceneActorEntity.CLIP_XIAOSHIZI_SITTING_ON_THE_BED,
                            true,
                            122);
                }
            }
            case 4 ->
            {
                if (kiro != null && shi != null)
                {
                    kiro.lookAt(shi.position().add(0.0D, 1.0D, 0.0D));
                    kiro.playOnceThen(
                            CutsceneActorEntity.CLIP_KIRO_REACH_OUT,
                            CutsceneActorEntity.CLIP_KIRO_STAND_IDLE,
                            true,
                            80);
                }
            }
            case 6 ->
            {
                if (shi != null)
                {
                    shi.playOnceThen(
                            CutsceneActorEntity.CLIP_XIAOSHIZI_NOD,
                            CutsceneActorEntity.CLIP_XIAOSHIZI_SITTING_ON_THE_BED,
                            true,
                            81);
                }
            }
            case 10 ->
            {
                if (shi != null)
                {
                    shi.playOnceThen(
                            CutsceneActorEntity.CLIP_XIAOSHIZI_WAVE,
                            CutsceneActorEntity.CLIP_XIAOSHIZI_SITTING_ON_THE_BED,
                            true,
                            80);
                }
            }
            default ->
            {
            }
        }
    }

    private static void applyLineMid(ServerPlayer player, int index)
    {
        CutsceneActorEntity kiro = MainQuest11CabinActors.actor(player, CutsceneActorEntity.APPEARANCE_KIRO);
        CutsceneActorEntity shi = MainQuest11CabinActors.actor(player, CutsceneActorEntity.APPEARANCE_XIAOSHIZI);
        Vec3 playerEyes = player.position().add(0.0D, player.getEyeHeight(), 0.0D);
        switch (index)
        {
            case 1 ->
            {
                if (shi != null)
                {
                    shi.lookYaw(MainQuest11CabinActors.xiaoshiziRestYaw(player));
                }
            }
            case 4 ->
            {
                if (kiro != null)
                {
                    kiro.lookAt(playerEyes);
                }
            }
            default ->
            {
            }
        }
    }

    private static final class TalkSession
    {
        int ticks;
        int shownCaption = -1;
        boolean midApplied;
    }
}
