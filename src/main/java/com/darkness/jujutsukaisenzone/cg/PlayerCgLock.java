package com.darkness.jujutsukaisenzone.cg;

import com.darkness.jujutsukaisenzone.network.ModNetwork;
import com.darkness.jujutsukaisenzone.network.NetworkSenders;
import com.darkness.jujutsukaisenzone.network.packet.SyncCgLookClampS2CPacket;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * COD 式第一人称 CG 锁：锁走、钉站位、朝向只在客户端按鼠标增量夹锥。
 * 禁止每 tick 传送，否则会和第一人称抢朝向。
 */
public final class PlayerCgLock
{
    public static final float DEFAULT_YAW_HALF = 25.0F;
    public static final float DEFAULT_PITCH_HALF = 12.0F;
    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    private PlayerCgLock()
    {
    }

    public static boolean isActive(ServerPlayer player)
    {
        return SESSIONS.containsKey(player.getUUID());
    }

    /** 只钉站位、锁走，不夹视角（巡镜期间用）。 */
    public static void pinOnly(ServerPlayer player, Vec3 standPos)
    {
        SESSIONS.put(player.getUUID(), new Session(standPos.x, standPos.y, standPos.z));
        ModNetwork.sendPlayerInputLockSync(player, true);
        NetworkSenders.toPlayer(player, SyncCgLookClampS2CPacket.clear());
        snapPos(player);
    }

    /** 钉站位、锁走，朝向对准 {@code lookTarget} 后夹锥。 */
    public static void beginLookAt(
            ServerPlayer player, Vec3 standPos, Vec3 lookTarget, float yawHalf, float pitchHalf)
    {
        Vec3 eyes = new Vec3(standPos.x, standPos.y + player.getEyeHeight(), standPos.z);
        float[] look = lookAngles(eyes, lookTarget);
        begin(player, standPos, look[0], look[1], yawHalf, pitchHalf);
    }

    public static void begin(
            ServerPlayer player, Vec3 standPos, float yaw, float pitch, float yawHalf, float pitchHalf)
    {
        SESSIONS.put(player.getUUID(), new Session(standPos.x, standPos.y, standPos.z));
        ModNetwork.sendPlayerInputLockSync(player, true);
        player.connection.teleport(standPos.x, standPos.y, standPos.z, yaw, pitch);
        NetworkSenders.toPlayer(player, new SyncCgLookClampS2CPacket(true, yaw, pitch, yawHalf, pitchHalf));
    }

    public static void tick(ServerPlayer player)
    {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null)
        {
            return;
        }
        snapPos(player);
    }

    public static void end(ServerPlayer player)
    {
        if (SESSIONS.remove(player.getUUID()) == null)
        {
            return;
        }
        NetworkSenders.toPlayer(player, SyncCgLookClampS2CPacket.clear());
        ModNetwork.sendPlayerInputLockSync(player, false);
    }

    public static float[] lookAngles(Vec3 from, Vec3 to)
    {
        Vec3 delta = to.subtract(from);
        double horiz = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float yaw = Mth.wrapDegrees((float) (Mth.atan2(delta.z, delta.x) * (180.0D / Math.PI)) - 90.0F);
        float pitch = Mth.wrapDegrees((float) (-(Mth.atan2(delta.y, horiz) * (180.0D / Math.PI))));
        return new float[] {yaw, pitch};
    }

    private static void snapPos(ServerPlayer player)
    {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null)
        {
            return;
        }
        player.setDeltaMovement(Vec3.ZERO);
        player.xxa = 0.0F;
        player.zza = 0.0F;
        player.setPos(session.x, session.y, session.z);
    }

    private record Session(double x, double y, double z)
    {
    }
}
