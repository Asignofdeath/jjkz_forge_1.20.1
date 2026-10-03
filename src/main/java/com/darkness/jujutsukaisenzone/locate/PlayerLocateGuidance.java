package com.darkness.jujutsukaisenzone.locate;

import com.darkness.jujutsukaisenzone.network.NetworkSenders;
import com.darkness.jujutsukaisenzone.network.packet.SyncLocateGuidanceS2CPacket;
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

    public record Session(String targetId, String displayName, BlockPos targetPos)
    {
    }

    /**
     * 查找并开始引导。成功返回 true。
     */
    public static boolean start(ServerPlayer player, StructureLocateTarget target)
    {
        return start(player, target, StructureLocator.DEFAULT_RADIUS_CHUNKS);
    }

    public static boolean start(ServerPlayer player, StructureLocateTarget target, int radiusChunks)
    {
        ServerLevel level = player.serverLevel();
        Optional<BlockPos> found = StructureLocator.findNearest(level, player.blockPosition(), target, radiusChunks);
        if (found.isEmpty())
        {
            player.sendSystemMessage(Component.literal(
                    "附近未找到已生成的「" + target.displayName()
                            + "」模板（搜索已加载区块，半径约 " + Math.min(radiusChunks, 48)
                            + " 区块）。空村不会作为目标。"));
            return false;
        }
        BlockPos pos = found.get();
        Session session = new Session(target.id(), target.displayName(), pos.immutable());
        SESSIONS.put(player.getUUID(), session);
        sync(player, session);
        player.sendSystemMessage(Component.literal(
                "已定位「" + target.displayName() + "」：["
                        + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]。屏幕将显示坐标差。"));
        return true;
    }

    public static void clear(ServerPlayer player)
    {
        if (SESSIONS.remove(player.getUUID()) != null)
        {
            NetworkSenders.toPlayer(player, SyncLocateGuidanceS2CPacket.clear());
            player.sendSystemMessage(Component.literal("已清除结构引导。"));
        }
    }

    public static void clearSilent(ServerPlayer player)
    {
        if (SESSIONS.remove(player.getUUID()) != null)
        {
            NetworkSenders.toPlayer(player, SyncLocateGuidanceS2CPacket.clear());
        }
    }

    @Nullable
    public static Session get(ServerPlayer player)
    {
        return SESSIONS.get(player.getUUID());
    }

    /** 靠近目标时自动结束。 */
    public static void tickPlayer(ServerPlayer player)
    {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null)
        {
            return;
        }
        if (player.blockPosition().distSqr(session.targetPos()) <= ARRIVE_DISTANCE * ARRIVE_DISTANCE)
        {
            SESSIONS.remove(player.getUUID());
            NetworkSenders.toPlayer(player, SyncLocateGuidanceS2CPacket.clear());
            player.sendSystemMessage(Component.literal("已抵达「" + session.displayName() + "」。"));
        }
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
