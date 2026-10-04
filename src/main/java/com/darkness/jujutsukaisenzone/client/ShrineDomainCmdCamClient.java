package com.darkness.jujutsukaisenzone.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Locale;

/**
 * 伏魔神龛结界构建完成时，在<strong>客户端</strong>以本地玩家身份执行 CMDCam 的 {@code /cam} 命令（Forge 客户端命令分发器，无前导斜杠）。
 * <p>
 * 命令队列与间隔同 {@link CmdCamScheduler}；结界完成（t=0）起计 {@value #TICKS_BEFORE_CAM_START} tick（3 秒）后才 {@code cam load} + {@code cam start 2s}。
 */
public final class ShrineDomainCmdCamClient
{
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int TICKS_AFTER_LOAD_CAMERA = 5;
    /** 结界完成到回放：60 tick = 3 秒（自 t=0 起计，与路径编排并行） */
    private static final int TICKS_BEFORE_CAM_START = 60;

    public static final String CMDCAM_PATH_NAME = "shrine_domain_camera";
    private static final String CAM_START_PLAYBACK = "cam start 2s";
    private static final String CAM_DISABLE_SMOOTH_START = "cam smooth_start false";

    private static final float SHRINE_CAM_YAW = 180.0F;
    private static final double FORWARD_DIST_NEAR = 3.0D;
    private static final double FORWARD_DIST_FAR = 12.0D;
    private static final double HEIGHT_HIGH = 7.0D;
    private static final double HEIGHT_LOW = 1.0D;
    private static final float PITCH_HIGH = -18.0F;
    private static final float PITCH_NEAR = 0.0F;
    private static final float PITCH_FAR = 0.0F;
    private static final float ROLL = 0.0F;
    private static final float FOV_DEFAULT = 70.0F;
    private static final float FOV_ZOOM_IN = 65.0F;
    private static final float FOV_ZOOM_OUT = 100.0F;

    private static final CmdCamScheduler CAM = CmdCamScheduler.create(ShrineDomainCmdCamClient::ticksAfterCommand)
            .afterEach(ShrineDomainCmdCamClient::onCamCommandRan)
            .beforeEachTick(ShrineDomainCmdCamClient::tickCamStartCountdown);

    private static int camStartDelayTicks;
    private static boolean pathSaved;
    private static boolean playbackTriggered;

    private ShrineDomainCmdCamClient()
    {
    }

    /**
     * 编排（t=0 立即开始）：{@code mode outside} → {@code clear} → 3×{@code add} → {@code save} → {@code clear}<br>
     * 回放（t=60）：{@code load shrine_domain_camera} → {@code smooth_start false} → {@code start 2s}
     */
    public static void applyCameraPathSetup(double px, double py, double pz, float yRot)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null)
        {
            return;
        }
        if (!CmdCamScheduler.dispatcherReady())
        {
            LOGGER.warn("[CMDCam] Client command dispatcher not ready; skipped shrine domain camera setup.");
            return;
        }

        camStartDelayTicks = TICKS_BEFORE_CAM_START;
        pathSaved = false;
        playbackTriggered = false;

        Vec3 forward = horizontalForward(yRot);
        double x1 = px + forward.x * FORWARD_DIST_NEAR;
        double y1 = py + HEIGHT_HIGH;
        double z1 = pz + forward.z * FORWARD_DIST_NEAR;
        double x2 = px + forward.x * FORWARD_DIST_NEAR;
        double y2 = py + HEIGHT_LOW;
        double z2 = pz + forward.z * FORWARD_DIST_NEAR;
        double x3 = px + forward.x * FORWARD_DIST_FAR;
        double y3 = py + HEIGHT_LOW;
        double z3 = pz + forward.z * FORWARD_DIST_FAR;

        CAM.replaceAndStart(
                "cam mode outside",
                "cam clear",
                camAddWithOrientation(x1, y1, z1, PITCH_HIGH, FOV_DEFAULT),
                camAddWithOrientation(x2, y2, z2, PITCH_NEAR, FOV_ZOOM_IN),
                camAddWithOrientation(x3, y3, z3, PITCH_FAR, FOV_ZOOM_OUT),
                "cam save " + CMDCAM_PATH_NAME,
                "cam clear");

        LOGGER.info("[CMDCam] Shrine domain: setup started, playback in {} ticks.", TICKS_BEFORE_CAM_START);
    }

    private static void onCamCommandRan(String cmd)
    {
        if (cmd.equals("cam save " + CMDCAM_PATH_NAME))
        {
            pathSaved = true;
            tryEnqueuePlayback();
        }
    }

    private static void tickCamStartCountdown()
    {
        if (camStartDelayTicks <= 0)
        {
            return;
        }

        camStartDelayTicks--;
        if (camStartDelayTicks > 0)
        {
            return;
        }

        LOGGER.info("[CMDCam] Shrine domain: 3s elapsed since barrier complete.");
        tryEnqueuePlayback();
    }

    private static void tryEnqueuePlayback()
    {
        if (playbackTriggered || !pathSaved || camStartDelayTicks > 0)
        {
            return;
        }

        playbackTriggered = true;
        CAM.append("cam load " + CMDCAM_PATH_NAME, CAM_DISABLE_SMOOTH_START, CAM_START_PLAYBACK);
    }

    private static String camAddWithOrientation(double x, double y, double z, float pitch, float fov)
    {
        return String.format(
                Locale.ROOT,
                "cam add %.3f %.3f %.3f %.1f %.1f %.1f %.1f",
                x, y, z, SHRINE_CAM_YAW, pitch, ROLL, fov);
    }

    private static Vec3 horizontalForward(float yRot)
    {
        double yawRad = Math.toRadians(yRot);
        return new Vec3(-Math.sin(yawRad), 0.0D, Math.cos(yawRad));
    }

    private static int ticksAfterCommand(String commandJustRun)
    {
        if (commandJustRun.startsWith("cam save "))
        {
            return CmdCamScheduler.TICKS_AFTER_SAVE_OR_LOAD;
        }
        if (commandJustRun.startsWith("cam load " + CMDCAM_PATH_NAME))
        {
            return TICKS_AFTER_LOAD_CAMERA;
        }
        return CmdCamScheduler.TICKS_AFTER_NORMAL_COMMAND;
    }
}
