package com.darkness.jujutsukaisenzone.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.ClientCommandHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayDeque;
import java.util.Locale;

/**
 * 诛伏赐死结界构建完成时，在<strong>客户端</strong>以本地玩家身份执行 CMDCam 的 {@code /cam} 命令。
 * <p>
 * 命令队列与间隔同 {@link LimitlessDomainCmdCamClient}。结界完成（t=0）立即编排双镜头路径；
 * {@value #TICKS_BEFORE_PLAYBACK} tick（3 秒）后 {@code cam load} + 回放（A 3s → B 2s）。
 */
public final class JudgmentDomainCmdCamClient
{
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int TICKS_AFTER_NORMAL_COMMAND = 1;
    private static final int TICKS_AFTER_CAM_SAVE = 5;
    private static final int TICKS_AFTER_LOAD_CAMERA_A = 5;
    private static final int TICKS_AFTER_START_CAMERA_A_PLAYBACK = 60;
    private static final int TICKS_AFTER_LOAD_CAMERA_B = 5;
    /** 结界完成到回放：60 tick = 3 秒（自 t=0 起计，与路径编排并行） */
    private static final int TICKS_BEFORE_PLAYBACK = 60;

    public static final String CMDCAM_PATH_A_NAME = "judgment_domain_cameraA";
    public static final String CMDCAM_PATH_B_NAME = "judgment_domain_cameraB";
    private static final String CAM_START_PLAYBACK_A = "cam start 3s";
    private static final String CAM_START_PLAYBACK_B = "cam start 2s";

    private static final double HEIGHT_HIGH = 20.0D;
    private static final double HEIGHT_LOW = 2.0D;
    private static final double PLAYER_CAMERA_HEIGHT_OFFSET = 1.5D;
    private static final double LEFT_DIST_FAR = 10.0D;
    private static final double LEFT_DIST_NEAR = 2.0D;
    private static final float PITCH_DOWN = 90.0F;
    private static final float CAMERA_A_YAW = 0.0F;
    private static final float ROLL = 0.0F;
    private static final float FOV_DEFAULT = 70.0F;

    private static final ArrayDeque<String> CAM_COMMAND_QUEUE = new ArrayDeque<>();
    private static int ticksUntilNextCamCommand = 0;
    private static int playbackDelayTicks;
    private static boolean pathsReady;
    private static boolean playbackTriggered;

    private JudgmentDomainCmdCamClient()
    {
    }

    /**
     * 编排（t=0 立即开始）：双镜头 save → clear<br>
     * 回放（t=60）：load A → start 3s →（60 tick）→ load B → start 2s
     */
    public static void scheduleCameraPerformance(BlockPos domainCenter)
    {
        playbackDelayTicks = TICKS_BEFORE_PLAYBACK;
        pathsReady = false;
        playbackTriggered = false;
        synchronized (CAM_COMMAND_QUEUE)
        {
            CAM_COMMAND_QUEUE.clear();
            ticksUntilNextCamCommand = 0;
        }
        beginCameraSetup(domainCenter);
        LOGGER.info("[CMDCam] Judgment domain: setup started, playback in {} ticks.", TICKS_BEFORE_PLAYBACK);
    }

    static void tickScheduledCamCommands()
    {
        tickPlaybackCountdown();

        synchronized (CAM_COMMAND_QUEUE)
        {
            if (ticksUntilNextCamCommand > 0)
            {
                ticksUntilNextCamCommand--;
                if (ticksUntilNextCamCommand > 0)
                {
                    return;
                }
            }
            if (CAM_COMMAND_QUEUE.isEmpty())
            {
                return;
            }
            String cmd = CAM_COMMAND_QUEUE.pollFirst();
            if (cmd == null)
            {
                return;
            }
            runCamImmediate(cmd);
            if (cmd.equals("cam save " + CMDCAM_PATH_B_NAME))
            {
                pathsReady = true;
                tryEnqueuePlayback();
            }
            if (!CAM_COMMAND_QUEUE.isEmpty())
            {
                ticksUntilNextCamCommand = ticksAfterCommand(cmd);
            }
        }
    }

    private static void tickPlaybackCountdown()
    {
        if (playbackDelayTicks <= 0)
        {
            return;
        }

        playbackDelayTicks--;
        if (playbackDelayTicks > 0)
        {
            return;
        }

        LOGGER.info("[CMDCam] Judgment domain: 3s elapsed since barrier complete.");
        tryEnqueuePlayback();
    }

    private static void tryEnqueuePlayback()
    {
        if (playbackTriggered || !pathsReady || playbackDelayTicks > 0)
        {
            return;
        }

        playbackTriggered = true;
        synchronized (CAM_COMMAND_QUEUE)
        {
            CAM_COMMAND_QUEUE.addLast("cam load " + CMDCAM_PATH_A_NAME);
            CAM_COMMAND_QUEUE.addLast(CAM_START_PLAYBACK_A);
            CAM_COMMAND_QUEUE.addLast("cam load " + CMDCAM_PATH_B_NAME);
            CAM_COMMAND_QUEUE.addLast(CAM_START_PLAYBACK_B);
        }
    }

    private static void beginCameraSetup(BlockPos center)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null)
        {
            LOGGER.warn("[CMDCam] Judgment domain camera setup skipped: local player is null.");
            return;
        }
        if (ClientCommandHandler.getDispatcher() == null)
        {
            LOGGER.warn("[CMDCam] Client command dispatcher not ready; skipped judgment domain camera setup.");
            return;
        }

        JudgmentDomainChatHudSuppressor.startHideForJudgmentDomainCamera();

        double px = mc.player.getX();
        double py = mc.player.getY();
        double pz = mc.player.getZ();
        float yRot = mc.player.getYRot();
        double lookX = px;
        double lookY = py + PLAYER_CAMERA_HEIGHT_OFFSET;
        double lookZ = pz;

        double cx = center.getX() + 0.5D;
        double cz = center.getZ() + 0.5D;
        double a1y = center.getY() + HEIGHT_HIGH;
        double a2y = center.getY() + HEIGHT_LOW;

        Vec3 left = horizontalLeft(yRot);
        double b1x = px + left.x * LEFT_DIST_FAR;
        double b1z = pz + left.z * LEFT_DIST_FAR;
        double b1y = py + PLAYER_CAMERA_HEIGHT_OFFSET;
        double b2x = px + left.x * LEFT_DIST_NEAR;
        double b2z = pz + left.z * LEFT_DIST_NEAR;
        double b2y = py + PLAYER_CAMERA_HEIGHT_OFFSET;

        float b1Yaw = yawToward(b1x, b1y, b1z, lookX, lookY, lookZ);
        float b1Pitch = pitchToward(b1x, b1y, b1z, lookX, lookY, lookZ);
        float b2Yaw = yawToward(b2x, b2y, b2z, lookX, lookY, lookZ);
        float b2Pitch = pitchToward(b2x, b2y, b2z, lookX, lookY, lookZ);

        synchronized (CAM_COMMAND_QUEUE)
        {
            CAM_COMMAND_QUEUE.clear();
            ticksUntilNextCamCommand = 0;

            CAM_COMMAND_QUEUE.add("cam mode outside");
            CAM_COMMAND_QUEUE.add("cam clear");
            CAM_COMMAND_QUEUE.add(camAddWithOrientation(cx, a1y, cz, CAMERA_A_YAW, PITCH_DOWN, FOV_DEFAULT));
            CAM_COMMAND_QUEUE.add(camAddWithOrientation(cx, a2y, cz, CAMERA_A_YAW, PITCH_DOWN, FOV_DEFAULT));
            CAM_COMMAND_QUEUE.add("cam save " + CMDCAM_PATH_A_NAME);
            CAM_COMMAND_QUEUE.add("cam clear");
            CAM_COMMAND_QUEUE.add(camAddWithOrientation(b1x, b1y, b1z, b1Yaw, b1Pitch, FOV_DEFAULT));
            CAM_COMMAND_QUEUE.add(camAddWithOrientation(b2x, b2y, b2z, b2Yaw, b2Pitch, FOV_DEFAULT));
            CAM_COMMAND_QUEUE.add("cam save " + CMDCAM_PATH_B_NAME);
            CAM_COMMAND_QUEUE.add("cam clear");

            String first = CAM_COMMAND_QUEUE.pollFirst();
            if (first != null)
            {
                runCamImmediate(first);
                if (!CAM_COMMAND_QUEUE.isEmpty())
                {
                    ticksUntilNextCamCommand = ticksAfterCommand(first);
                }
            }
        }
    }

    private static String camAddWithOrientation(
            double x, double y, double z, float yaw, float pitch, float fov)
    {
        return String.format(
                Locale.ROOT,
                "cam add %.3f %.3f %.3f %.1f %.1f %.1f %.1f",
                x, y, z, yaw, pitch, ROLL, fov);
    }

    private static Vec3 horizontalLeft(float yRot)
    {
        double yawRad = Math.toRadians(yRot - 90.0F);
        return new Vec3(-Math.sin(yawRad), 0.0D, Math.cos(yawRad));
    }

    private static float yawToward(double fromX, double fromY, double fromZ, double toX, double toY, double toZ)
    {
        double dx = toX - fromX;
        double dz = toZ - fromZ;
        return (float) Math.toDegrees(Math.atan2(-dx, dz));
    }

    private static float pitchToward(double fromX, double fromY, double fromZ, double toX, double toY, double toZ)
    {
        double dx = toX - fromX;
        double dy = toY - fromY;
        double dz = toZ - fromZ;
        double horiz = Math.sqrt(dx * dx + dz * dz);
        return (float) -Math.toDegrees(Math.atan2(dy, horiz));
    }

    private static int ticksAfterCommand(String commandJustRun)
    {
        if (commandJustRun.startsWith("cam save "))
        {
            return TICKS_AFTER_CAM_SAVE;
        }
        if (commandJustRun.equals(CAM_START_PLAYBACK_A))
        {
            return TICKS_AFTER_START_CAMERA_A_PLAYBACK;
        }
        if (commandJustRun.startsWith("cam load " + CMDCAM_PATH_A_NAME))
        {
            return TICKS_AFTER_LOAD_CAMERA_A;
        }
        if (commandJustRun.startsWith("cam load " + CMDCAM_PATH_B_NAME))
        {
            return TICKS_AFTER_LOAD_CAMERA_B;
        }
        return TICKS_AFTER_NORMAL_COMMAND;
    }

    private static void runCamImmediate(String commandWithoutSlash)
    {
        boolean consumed = ClientCommandHandler.runCommand(commandWithoutSlash);
        if (!consumed)
        {
            LOGGER.warn("[CMDCam] Command not handled as client command (is CMDCam installed on this client?): {}",
                    commandWithoutSlash);
        }
    }
}
