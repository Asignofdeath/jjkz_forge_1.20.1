package com.darkness.jujutsukaisenzone.quest;

import com.darkness.jujutsukaisenzone.locate.PlayerLocateGuidance;
import com.darkness.jujutsukaisenzone.locate.StructureLocateTarget;
import com.darkness.jujutsukaisenzone.network.NetworkSenders;
import com.darkness.jujutsukaisenzone.network.packet.SyncQuestMissionBriefingS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncQuestProgressS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncQuestTrackS2CPacket;
import com.darkness.jujutsukaisenzone.player.PlayerQuestData;
import java.util.ArrayList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;

/**
 * 主线任务运行时：追踪、locate 联动、靠近完成。
 */
public final class MainQuestLogic
{
    private MainQuestLogic()
    {
    }

    /**
     * 登录同步进度。已撤下的任务 id（如 MVP 的 main_1_1）从存档清掉，避免挡住真主线。
     */
    public static void bootstrapIfNeeded(ServerPlayer player)
    {
        if (!PlayerQuestData.isBootstrapped(player))
        {
            PlayerQuestData.setBootstrapped(player, true);
        }
        dropStaleQuestProgress(player);
        syncProgress(player);
        // 先强制清客户端引导 HUD，再按进度决定要不要重新开导航（防换世界残留）
        PlayerLocateGuidance.clearSilent(player);
        if (MainQuestDefs.MAIN_1_1_ID.equals(PlayerQuestData.getTrackedId(player)))
        {
            MainQuest11Intro.startIfNeeded(player);
        }
        ensureLocateForTracked(player, false);
    }

    /**
     * @return true 若成功开始追踪
     */
    public static boolean track(ServerPlayer player, String questId, boolean announce)
    {
        MainQuestDef def = MainQuestDefs.byId(questId);
        if (def == null)
        {
            if (announce)
            {
                player.sendSystemMessage(Component.translatable("message.jujutsukaisenzone.quest.unknown"));
            }
            return false;
        }
        MainQuestStatus status = PlayerQuestData.status(player, def);
        if (status == MainQuestStatus.LOCKED)
        {
            if (announce)
            {
                player.sendSystemMessage(Component.translatable("message.jujutsukaisenzone.quest.locked"));
            }
            return false;
        }
        if (status == MainQuestStatus.COMPLETED)
        {
            if (announce)
            {
                player.sendSystemMessage(Component.translatable("message.jujutsukaisenzone.quest.already_completed"));
            }
            return false;
        }

        String previous = PlayerQuestData.getTrackedId(player);
        if (previous != null && !previous.equals(def.id()))
        {
            clearLocateIfOwnedByQuest(player, previous);
        }

        PlayerQuestData.setTrackedId(player, def.id());
        syncProgress(player);
        if (MainQuestDefs.MAIN_1_1_ID.equals(def.id()))
        {
            MainQuest11Intro.startIfNeeded(player);
        }
        ensureLocateForTracked(player, announce);
        // 常规追踪立刻播简报；main_1_1 尚未揭幕巡镜时延后到 finishBlackout
        if (announce && !shouldDeferMissionBriefing(player, def))
        {
            sendMissionBriefing(player, def);
        }
        if (announce)
        {
            player.sendSystemMessage(Component.translatable("message.jujutsukaisenzone.quest.tracking",
                    Component.translatable(def.titleKey())));
        }
        return true;
    }

    /** COD 风格任务简报（S2C）。 */
    public static void sendMissionBriefing(ServerPlayer player, MainQuestDef def)
    {
        NetworkSenders.toPlayer(player, SyncQuestMissionBriefingS2CPacket.show(
                def.titleKey(),
                def.briefingOrgKey(),
                def.briefingLocationKey()));
    }

    /**
     * main_1_1 黑屏+第一组运镜尚未播完时，简报延到揭幕运镜，避免被黑屏盖住。
     */
    private static boolean shouldDeferMissionBriefing(ServerPlayer player, MainQuestDef def)
    {
        return MainQuestDefs.MAIN_1_1_ID.equals(def.id())
                && !PlayerQuestData.isMain11IntroDone(player);
    }

    public static void untrack(ServerPlayer player, boolean announce)
    {
        String tracked = PlayerQuestData.getTrackedId(player);
        if (tracked == null)
        {
            if (announce)
            {
                player.sendSystemMessage(Component.translatable("message.jujutsukaisenzone.quest.not_tracking"));
            }
            return;
        }
        clearLocateIfOwnedByQuest(player, tracked);
        MainQuest11Intro.abort(player);
        PlayerQuestData.setTrackedId(player, null);
        syncProgress(player);
        if (announce)
        {
            player.sendSystemMessage(Component.translatable("message.jujutsukaisenzone.quest.untracked"));
        }
    }

    /**
     * locate 抵达时由 {@link PlayerLocateGuidance} 调用。
     *
     * @return true 表示已由任务系统处理（调用方勿再发「已抵达」）
     */
    public static boolean onLocateArrived(ServerPlayer player, String locateTargetId)
    {
        String tracked = PlayerQuestData.getTrackedId(player);
        if (tracked == null)
        {
            return false;
        }
        MainQuestDef def = MainQuestDefs.byId(tracked);
        if (def == null || !def.hasLocateTarget())
        {
            return false;
        }
        StructureLocateTarget target = def.locateTarget();
        if (target == null || !target.id().equals(locateTargetId))
        {
            return false;
        }
        complete(player, def.id(), true);
        return true;
    }

    public static boolean isTrackingLocateTarget(ServerPlayer player, String locateTargetId)
    {
        String tracked = PlayerQuestData.getTrackedId(player);
        if (tracked == null)
        {
            return false;
        }
        MainQuestDef def = MainQuestDefs.byId(tracked);
        return def != null
                && def.hasLocateTarget()
                && def.locateTarget() != null
                && def.locateTarget().id().equals(locateTargetId);
    }

    public static void complete(ServerPlayer player, String questId, boolean announce)
    {
        MainQuestDef def = MainQuestDefs.byId(questId);
        if (def == null)
        {
            return;
        }
        if (PlayerQuestData.isCompleted(player, questId))
        {
            return;
        }
        PlayerQuestData.markCompleted(player, questId);
        if (questId.equals(PlayerQuestData.getTrackedId(player)))
        {
            clearLocateIfOwnedByQuest(player, questId);
            PlayerQuestData.setTrackedId(player, null);
        }
        syncProgress(player);
        if (announce)
        {
            player.sendSystemMessage(Component.translatable("message.jujutsukaisenzone.quest.completed",
                    Component.translatable(def.titleKey())));
        }
    }

    /** 调试：清空进度并重新 bootstrap。 */
    public static void resetAll(ServerPlayer player)
    {
        String tracked = PlayerQuestData.getTrackedId(player);
        if (tracked != null)
        {
            clearLocateIfOwnedByQuest(player, tracked);
        }
        MainQuest11Intro.abort(player);
        PlayerQuestData.clearAllProgress(player);
        syncProgress(player);
        bootstrapIfNeeded(player);
    }

    public static void ensureLocateForTracked(ServerPlayer player, boolean announce)
    {
        String tracked = PlayerQuestData.getTrackedId(player);
        if (tracked == null)
        {
            return;
        }
        // 1.1：小屋对话结束前绝不导航
        if (MainQuestDefs.MAIN_1_1_ID.equals(tracked))
        {
            if (MainQuest11Intro.isRunning(player) || !PlayerQuestData.isMain11TalkDone(player))
            {
                PlayerLocateGuidance.Session session = PlayerLocateGuidance.get(player);
                if (session != null && StructureLocateTarget.NEAREST_VILLAGE.id().equals(session.targetId()))
                {
                    PlayerLocateGuidance.clearSilent(player);
                }
                return;
            }
        }
        MainQuestDef def = MainQuestDefs.byId(tracked);
        if (def == null || !def.hasLocateTarget() || def.locateTarget() == null)
        {
            return;
        }
        // 始终用最新校正坐标刷新会话（原版 locate 常把 Y=0，需 refinement）
        if (PlayerLocateGuidance.start(player, def.locateTarget(), announce)
                && MainQuestDefs.MAIN_1_1_ID.equals(tracked))
        {
            MainQuest11VillageClear.clearNearestVillage(player);
        }
    }

    public static void syncProgress(ServerPlayer player)
    {
        syncTrackHud(player);
        String tracked = PlayerQuestData.getTrackedId(player);
        NetworkSenders.toPlayer(player, new SyncQuestProgressS2CPacket(
                tracked == null ? "" : tracked,
                new ArrayList<>(PlayerQuestData.completedIds(player))));
    }

    public static void syncTrackHud(ServerPlayer player)
    {
        String tracked = PlayerQuestData.getTrackedId(player);
        if (tracked == null)
        {
            NetworkSenders.toPlayer(player, SyncQuestTrackS2CPacket.clear());
            return;
        }
        MainQuestDef def = MainQuestDefs.byId(tracked);
        if (def == null || PlayerQuestData.isCompleted(player, tracked))
        {
            NetworkSenders.toPlayer(player, SyncQuestTrackS2CPacket.clear());
            return;
        }
        NetworkSenders.toPlayer(player, new SyncQuestTrackS2CPacket(
                true,
                def.id(),
                Component.translatable(def.titleKey()).getString(),
                Component.translatable(def.objectiveKey()).getString()));
    }

    private static void dropStaleQuestProgress(ServerPlayer player)
    {
        String tracked = PlayerQuestData.getTrackedId(player);
        if (tracked != null && MainQuestDefs.byId(tracked) == null)
        {
            PlayerLocateGuidance.clearSilent(player);
            PlayerQuestData.setTrackedId(player, null);
        }
        for (String id : new ArrayList<>(PlayerQuestData.completedIds(player)))
        {
            if (MainQuestDefs.byId(id) == null)
            {
                PlayerQuestData.clearCompleted(player, id);
            }
        }
    }

    private static void clearLocateIfOwnedByQuest(ServerPlayer player, @Nullable String questId)
    {
        MainQuestDef def = MainQuestDefs.byId(questId);
        if (def == null || !def.hasLocateTarget() || def.locateTarget() == null)
        {
            return;
        }
        PlayerLocateGuidance.Session session = PlayerLocateGuidance.get(player);
        if (session != null && def.locateTarget().id().equals(session.targetId()))
        {
            PlayerLocateGuidance.clearSilent(player);
        }
    }
}
