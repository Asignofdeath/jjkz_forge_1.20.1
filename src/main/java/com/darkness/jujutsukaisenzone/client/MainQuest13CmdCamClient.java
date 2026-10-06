package com.darkness.jujutsukaisenzone.client;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 主线 1.3 五条悟镜头的 CMDCam 编排（客户端执行 {@code /cam}，无前导斜杠）。
 * <p>
 * <b>两段镜头，段间顺序播放</b>（各自独立 {@code replaceAndStart}）：
 * <ul>
 *   <li><b>背影低角度</b>（阶段 4）：镜头贴近玩家、略微仰视五条悟的背影；</li>
 *   <li><b>正脸特写</b>（阶段 5）：切到五条悟正前方、能看到上半身的近景。</li>
 * </ul>
 * 站位与服务端算好的「五条悟坐标 / 朝向」一致，避免客户端各自推算产生偏差。
 * <p>
 * <b>每段都是单组路径</b>（{@code mode} → {@code clear} → {@code add}… → {@code smooth_start false}
 * → {@code start}），不走 {@code save}/{@code load}（见 {@code docs/cmdcam.md}）。
 * {@code cam smooth_start false} <b>两段都要带</b>：它禁止镜头从玩家相机插值到第一点，
 * 保证每段一开播就<b>直接落在第一点位</b>、不从玩家视角飞过去。
 */
public final class MainQuest13CmdCamClient
{
    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * 关掉「平滑起步」：禁止镜头从<b>玩家当前相机</b>插值到路径第一点，直接落在第一点开播。
     * <p>
     * 两段镜头（背影 / 正脸）都是<b>单组</b>路径，各自独立
     * {@code replaceAndStart}，故每段都要带这一条。
     */
    private static final String CAM_DISABLE_SMOOTH_START = "cam smooth_start false";

    /** 背影镜头回放时长（醒来眨眼 4s + 句 1～3）。 */
    private static final String CAM_START_BACK = "cam start 18s";
    /** 正脸特写回放时长（覆盖慢放摘眼罩 + 句 4～5，约 17s）。 */
    private static final String CAM_START_FACE = "cam start 18s";
    private static final float FOV_BACK = 60.0F;
    private static final float FOV_FACE = 42.0F;
    private static final float ROLL = 0.0F;

    private static final CmdCamScheduler CAM =
            CmdCamScheduler.create(MainQuest13CmdCamClient::ticksAfterCommand);

    private MainQuest13CmdCamClient()
    {
    }

    /**
     * 阶段 4：布置「五条悟背影」低角度镜头。
     *
     * @param playerFeetY 玩家脚部世界 Y（镜头贴地）
     * @param px/pz        玩家世界 X/Z
     * @param gojoX/gojoZ  五条悟世界 X/Z
     */
    public static void setupBackView(double px, double playerFeetY, double pz, double gojoX, double gojoZ)
    {
        if (!ready())
        {
            return;
        }
        // 镜头 1：玩家头侧、贴地略抬（倒地者视线高度）。
        double c1x = px;
        double c1y = playerFeetY + 0.55D;
        double c1z = pz;
        // 镜头 2：向五条悟方向推近一点，仍保持低位仰视。
        double c2x = px + (gojoX - px) * 0.22D;
        double c2z = pz + (gojoZ - pz) * 0.22D;
        double c2y = playerFeetY + 0.65D;

        CAM.replaceAndStart(
                "cam mode outside",
                "cam clear",
                camAddLookingAt(c1x, c1y, c1z, gojoX, playerFeetY + 1.25D, gojoZ, FOV_BACK),
                camAddLookingAt(c2x, c2y, c2z, gojoX, playerFeetY + 1.15D, gojoZ, FOV_BACK),
                CAM_DISABLE_SMOOTH_START,
                CAM_START_BACK);
        LOGGER.info("[CMDCam] Main 1.3 back-view camera queued.");
    }

    /**
     * 阶段 6：切到五条悟正脸特写（能看到上半身）。
     * <p>
     * 运镜不再是「钉死在正面」，而是：<b>先定格正脸</b>（摘眼罩的慢放发生在段首，必须先看住脸）
     * → <b>左右扫视一圈看周围</b>（原地转向 + 略降视线 + 稍稍拉远）→ <b>收回五条悟正脸特写</b>。
     * 机位始终在五条悟正前方，只做原地转向，故不需要额外目标坐标；
     * {@code cam add} 关键帧在 {@code cam start 18s} 内均分，总时长不变。
     *
     * @param gojoX/gojoFeetY/gojoZ 五条悟世界坐标
     * @param yaw                    五条悟朝向（镜头从正面拍）
     */
    public static void setupFaceCloseup(double gojoX, double gojoFeetY, double gojoZ, float yaw)
    {
        if (!ready())
        {
            return;
        }
        // 五条悟水平朝向的前向单位向量（MC yaw：0=+Z，90=-X）。
        double yawRad = Math.toRadians(yaw);
        double fwdX = -Math.sin(yawRad);
        double fwdZ = Math.cos(yawRad);
        // 镜头站在他正前方 3.2 格、抬到胸上高度（能看到上半身）。
        double cx = gojoX + fwdX * 3.2D;
        double cz = gojoZ + fwdZ * 3.2D;
        double cy = gojoFeetY + 1.35D;
        double lookY = gojoFeetY + 1.55D;

        // 正脸基准朝向：从机位指向五条悟面部。
        float faceYaw = wrapYaw(yawToward(cx, cz, gojoX, gojoZ));
        float facePitch = pitchToward(cx, cy, cz, gojoX, lookY, gojoZ);
        // 扫视幅度：左右各 55°，视线略下压 5°，扫视时视场角放松一点以多收周围。
        final float sweepYaw = 55.0F;
        final float sweepPitch = 5.0F;
        final float fovSweep = 56.0F;
        final float fovReturn = 48.0F;
        final double lift = 0.12D;

        CAM.replaceAndStart(
                "cam mode outside",
                "cam clear",
                // 1~2：定格正脸（摘眼罩慢放在段首展开，必须先看住脸）。
                camAdd(cx, cy, cz, faceYaw, facePitch, FOV_FACE),
                camAdd(cx, cy + lift, cz, faceYaw, facePitch, FOV_FACE),
                // 3：转向一侧看周围，视线略降、视场放松。
                camAdd(cx, cy + lift, cz, wrapYaw(faceYaw - sweepYaw), facePitch + sweepPitch, fovSweep),
                // 4：扫到另一侧。
                camAdd(cx, cy + lift, cz, wrapYaw(faceYaw + sweepYaw), facePitch + sweepPitch, fovSweep),
                // 5：转回正脸途中。
                camAdd(cx, cy + lift, cz, wrapYaw(faceYaw - 12.0F), facePitch, fovReturn),
                // 6：收在五条悟正脸特写。
                camAdd(cx, cy + lift, cz, faceYaw, facePitch, FOV_FACE),
                CAM_DISABLE_SMOOTH_START,
                CAM_START_FACE);
        LOGGER.info("[CMDCam] Main 1.3 face close-up camera queued.");
    }

    /** 正脸特写播放时长（tick），供服务端排下一阶段。 */
    public static int faceCloseupTicks()
    {
        return 341;
    }

    /** 背影镜头播放时长（tick）：醒来眨眼 80 + 句 1～3 的 265。 */
    public static int backViewTicks()
    {
        return 345;
    }

    private static boolean ready()
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null)
        {
            LOGGER.warn("[CMDCam] Main 1.3 camera skipped: local player is null.");
            return false;
        }
        if (!CmdCamScheduler.dispatcherReady())
        {
            LOGGER.warn("[CMDCam] Main 1.3 camera skipped: client command dispatcher not ready.");
            return false;
        }
        return true;
    }

    /**
     * 两段镜头都是<b>单组</b>路径，队列在 {@code start} 之后即空 ⇒ 该返回值只在「后面还有命令」时生效，
     * 对单组无意义。这里统一走默认间隔（{@link CmdCamScheduler#TICKS_AFTER_NORMAL_COMMAND}）。
     */
    private static int ticksAfterCommand(String commandJustRun)
    {
        return CmdCamScheduler.TICKS_AFTER_NORMAL_COMMAND;
    }

    private static String camAddLookingAt(
            double fromX, double fromY, double fromZ, double toX, double toY, double toZ, float fov)
    {
        return camAdd(
                fromX, fromY, fromZ,
                yawToward(fromX, fromZ, toX, toZ),
                pitchToward(fromX, fromY, fromZ, toX, toY, toZ),
                fov);
    }

    /** 直接给定朝向的关键帧（原地转向式扫视用，不依赖目标坐标）。 */
    private static String camAdd(double x, double y, double z, float yaw, float pitch, float fov)
    {
        return String.format(Locale.ROOT, "cam add %.3f %.3f %.3f %.1f %.1f %.1f %.1f",
                x, y, z, yaw, pitch, ROLL, fov);
    }

    /** 把偏航归一化到 [-180, 180]，避免叠加扫视幅度后越界。 */
    private static float wrapYaw(float yaw)
    {
        float wrapped = yaw % 360.0F;
        if (wrapped > 180.0F)
        {
            wrapped -= 360.0F;
        }
        else if (wrapped < -180.0F)
        {
            wrapped += 360.0F;
        }
        return wrapped;
    }

    private static float yawToward(double fromX, double fromZ, double toX, double toZ)
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
}
