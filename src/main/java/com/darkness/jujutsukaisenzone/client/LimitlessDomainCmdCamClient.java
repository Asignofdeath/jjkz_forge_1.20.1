package com.darkness.jujutsukaisenzone.client;

import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 无量空处结界构建完成时，在<strong>客户端</strong>以本地玩家身份执行 CMDCam 的 {@code /cam} 命令（Forge 客户端命令分发器，无前导斜杠）。
 * <p>
 * 命令按队列执行：默认间隔 {@link CmdCamScheduler#TICKS_AFTER_NORMAL_COMMAND} 刻；{@code cam save} 之后 {@link CmdCamScheduler#TICKS_AFTER_SAVE_OR_LOAD} 刻；另有镜头回放段的专用间隔常量。
 */
public final class LimitlessDomainCmdCamClient
{
    private static final Logger LOGGER = LogManager.getLogger();
    /** {@code cam load limitless_domain_cameraA} 之后到 {@code cam start 4s} 的间隔。 */
    private static final int TICKS_AFTER_LOAD_CAMERA_A = 5;
    /** {@code cam start 4s} 之后到 {@code cam load limitless_domain_cameraB} 的间隔。 */
    private static final int TICKS_AFTER_START_CAMERA_A_PLAYBACK = 80;
    /** {@code cam load limitless_domain_cameraB} 之后到 {@code cam start 6s} 的间隔。 */
    private static final int TICKS_AFTER_LOAD_CAMERA_B = 5;

    /** 与 CMDCam {@code /cam save} 一致的路径名。 */
    public static final String CMDCAM_PATH_A_NAME = "limitless_domain_cameraA";
    public static final String CMDCAM_PATH_B_NAME = "limitless_domain_cameraB";
    private static final String CAM_START_PLAYBACK_A = "cam start 4s";
    private static final String CAM_START_PLAYBACK_B = "cam start 6s";
    /** 镜头 A 点位 2：{@code /cam add} 在位置之后的水平角（yaw）、俯仰角（pitch），与 CMDCam 文档顺序一致。 */
    private static final float CAMERA_A_POINT2_YAW = 180.0F;
    private static final float CAMERA_A_POINT2_PITCH = 0.0F;
    /** 镜头 B 点位 2：与点位 1 同坐标，另设 yaw、pitch。 */
    private static final float CAMERA_B_POINT2_YAW = 90.0F;
    private static final float CAMERA_B_POINT2_PITCH = 0.0F;

    private static final CmdCamScheduler CAM = CmdCamScheduler.create(LimitlessDomainCmdCamClient::ticksAfterCommand);

    private LimitlessDomainCmdCamClient()
    {
    }

    /**
     * 1) {@code cam mode outside}<br>
     * 2) {@code cam clear}<br>
     * 3) 点位 1：玩家坐标 x 不变、z-1、y+1.5<br>
     * 4) 点位 2：玩家坐标 x 不变、z-15、y+1.5；朝向 yaw {@link #CAMERA_A_POINT2_YAW}、pitch {@link #CAMERA_A_POINT2_PITCH}（{@code /cam add} 位置后两参数）<br>
     * 5) {@code cam save limitless_domain_cameraA}（紧接在点位 2 的 {@code cam add} 之后；先于镜头 B 的 {@code cam clear}）<br>
     * 6) {@code cam clear}（开始编排镜头 B）<br>
     * 7) 镜头 B 点位 1：玩家坐标 x+5、z-18、y+1.5<br>
     * 8) 镜头 B 点位 2：同点位 1 坐标；yaw {@link #CAMERA_B_POINT2_YAW}、pitch {@link #CAMERA_B_POINT2_PITCH}<br>
     * 9) {@code cam save limitless_domain_cameraB}<br>
     * 10) {@code cam clear}（收尾：清空当前编排中的路径点）<br>
     * 11) {@code cam load limitless_domain_cameraA}（间隔 {@value #TICKS_AFTER_LOAD_CAMERA_A} 刻后）{@code cam start 4s}<br>
     * 12) 再间隔 {@value #TICKS_AFTER_START_CAMERA_A_PLAYBACK} 刻：{@code cam load limitless_domain_cameraB}<br>
     * 13) 间隔 {@value #TICKS_AFTER_LOAD_CAMERA_B} 刻：{@code cam start 6s}
     * <p>
     * 默认相邻间隔 {@link CmdCamScheduler#TICKS_AFTER_NORMAL_COMMAND} 刻；{@code cam save} 之后 {@link CmdCamScheduler#TICKS_AFTER_SAVE_OR_LOAD} 刻；回放段间隔见上。
     *
     * @param px 玩家实体 X（与服务端 {@link net.minecraft.world.entity.Entity#getX()} 一致）
     * @param py 玩家实体 Y
     * @param pz 玩家实体 Z
     */
    public static void applyCameraPathASetup(double px, double py, double pz)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null)
        {
            return;
        }
        if (!CmdCamScheduler.dispatcherReady())
        {
            LOGGER.warn("[CMDCam] Client command dispatcher not ready; skipped limitless domain camera setup.");
            return;
        }
        double a1x = px;
        double a1y = py + 1.5D;
        double a1z = pz - 1.0D;
        double a2x = px;
        double a2y = py + 1.5D;
        double a2z = pz - 15.0D;
        double b1x = px + 5.0D;
        double b1y = py + 1.5D;
        double b1z = pz - 18.0D;

        CAM.replaceAndStart(
                "cam mode outside",
                "cam clear",
                "cam add " + a1x + " " + a1y + " " + a1z,
                "cam add " + a2x + " " + a2y + " " + a2z + " " + CAMERA_A_POINT2_YAW + " " + CAMERA_A_POINT2_PITCH,
                "cam save " + CMDCAM_PATH_A_NAME,
                "cam clear",
                "cam add " + b1x + " " + b1y + " " + b1z,
                "cam add " + b1x + " " + b1y + " " + b1z + " " + CAMERA_B_POINT2_YAW + " " + CAMERA_B_POINT2_PITCH,
                "cam save " + CMDCAM_PATH_B_NAME,
                "cam clear",
                "cam load " + CMDCAM_PATH_A_NAME,
                CAM_START_PLAYBACK_A,
                "cam load " + CMDCAM_PATH_B_NAME,
                CAM_START_PLAYBACK_B);
    }

    private static int ticksAfterCommand(String commandJustRun)
    {
        if (commandJustRun.startsWith("cam save "))
        {
            return CmdCamScheduler.TICKS_AFTER_SAVE_OR_LOAD;
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
        return CmdCamScheduler.TICKS_AFTER_NORMAL_COMMAND;
    }
}
