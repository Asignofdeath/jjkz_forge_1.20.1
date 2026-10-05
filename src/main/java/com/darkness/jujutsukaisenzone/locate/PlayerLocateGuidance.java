package com.darkness.jujutsukaisenzone.locate;

import com.darkness.jujutsukaisenzone.network.NetworkSenders;
import com.darkness.jujutsukaisenzone.network.packet.SyncLocateGuidanceS2CPacket;
import com.darkness.jujutsukaisenzone.quest.mainline.MainQuestLogic;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;

/**
 * 玩家当前结构引导会话（服务端权威）。
 */
public final class PlayerLocateGuidance
{
    /** 到达目标多近（格）后自动结束引导。 */
    public static final double ARRIVE_DISTANCE = 8.0D;

    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    private PlayerLocateGuidance()
    {
    }

    public record Session(String targetId, String displayName, BlockPos targetPos, double arriveDistance)
    {
    }

    public static boolean start(ServerPlayer player, StructureLocateTarget target)
    {
        return start(player, target, StructureLocator.DEFAULT_RADIUS_CHUNKS, true);
    }

    public static boolean start(ServerPlayer player, StructureLocateTarget target, boolean announce)
    {
        return start(player, target, StructureLocator.DEFAULT_RADIUS_CHUNKS, announce);
    }

    public static boolean start(ServerPlayer player, StructureLocateTarget target, int radiusChunks)
    {
        return start(player, target, radiusChunks, true);
    }

    public static boolean start(
            ServerPlayer player, StructureLocateTarget target, int radiusChunks, boolean announce)
    {
        ServerLevel level = player.serverLevel();
        Optional<BlockPos> found = StructureLocator.findNearest(level, player.blockPosition(), target, radiusChunks);
        if (found.isEmpty())
        {
            if (announce)
            {
                player.sendSystemMessage(Component.literal(
                        "附近未找到「" + target.displayName()
                                + "」（半径约 " + radiusChunks
                                + " 区块）。若在超平坦，需使用已包含该结构集的新世界；连络所在村旁约 48–64 格处。"));
            }
            return false;
        }
        return startFixed(player, target, found.get(), announce);
    }

    /**
     * 用已锁定的坐标开导航，不再 {@code findNearest}（叙事村选定后不可漂移）。
     */
    public static boolean startFixed(
            ServerPlayer player, StructureLocateTarget target, BlockPos fixedPos, boolean announce)
    {
        if (fixedPos == null)
        {
            return false;
        }
        BlockPos pos = fixedPos.immutable();
        Session session = new Session(
                target.id(), target.displayName(), pos, target.arriveDistance());
        SESSIONS.put(player.getUUID(), session);
        sync(player, session);
        if (announce)
        {
            player.sendSystemMessage(Component.literal(
                    "已定位「" + target.displayName() + "」：["
                            + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]。屏幕将显示坐标差。"));
        }
        return true;
    }

    public static void clear(ServerPlayer player)
    {
        boolean had = SESSIONS.remove(player.getUUID()) != null;
        NetworkSenders.toPlayer(player, SyncLocateGuidanceS2CPacket.clear());
        if (had)
        {
            player.sendSystemMessage(Component.literal("已清除结构引导。"));
        }
    }

    /** 清服务端会话，并始终清客户端 HUD（无会话时也发 clear，避免换世界残留）。 */
    public static void clearSilent(ServerPlayer player)
    {
        SESSIONS.remove(player.getUUID());
        NetworkSenders.toPlayer(player, SyncLocateGuidanceS2CPacket.clear());
    }

    @Nullable
    public static Session get(ServerPlayer player)
    {
        return SESSIONS.get(player.getUUID());
    }

    /** 靠近目标时自动结束；若由主线任务追踪，则交给任务系统完成。 */
    public static void tickPlayer(ServerPlayer player)
    {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null)
        {
            return;
        }
        double arrive = session.arriveDistance();
        if (player.blockPosition().distSqr(session.targetPos()) > arrive * arrive)
        {
            return;
        }
        SESSIONS.remove(player.getUUID());
        NetworkSenders.toPlayer(player, SyncLocateGuidanceS2CPacket.clear());
        if (MainQuestLogic.onLocateArrived(player, session.targetId()))
        {
            return;
        }
        player.sendSystemMessage(Component.literal("已抵达「" + session.displayName() + "」。"));
    }

    private static void sync(ServerPlayer player, Session session)
    {
        NetworkSenders.toPlayer(player, new SyncLocateGuidanceS2CPacket(
                true,
                session.displayName(),
                session.targetPos().getX(),
                session.targetPos().getY(),
                session.targetPos().getZ()));
    }
}
