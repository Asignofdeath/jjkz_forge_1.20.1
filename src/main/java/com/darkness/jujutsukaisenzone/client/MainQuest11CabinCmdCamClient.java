package com.darkness.jujutsukaisenzone.client;

import com.darkness.jujutsukaisenzone.quest.mainline.MainQuestDefs;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 主线 1.1 小屋巡镜：命令字符串在此编排，执行走 {@link CmdCamScheduler}。
 * <p>
 * 黑屏期布<strong>三组</strong>：小屋 / 小石子焦点 / KiRo 焦点，用 {@code cam save}/{@code cam load}。
 * 揭幕回放：组1 {@code start 6s}、组2/3 {@code start 3s}。
 * {@code start} 后等到下一 {@code load} 的间隔 = 时长 − load 的 5 tick，让下一组 {@code start} 落在上一组结束当拍，避免还机位。
 * {@code cam smooth_start false} 只在第一组开头。
 */
public final class MainQuest11CabinCmdCamClient
{
    private static final Logger LOGGER = LogManager.getLogger();

    public static final String PATH_CABIN = "jjkz_main11_cabin";
    public static final String PATH_XIAOSHIZI = "jjkz_main11_xiaoshizi";
    public static final String PATH_KIRO = "jjkz_main11_kiro";

    private static final String CAM_START_GROUP_1 = "cam start 6s";
    private static final String CAM_START_GROUP_23 = "cam start 3s";
    private static final String CAM_DISABLE_SMOOTH_START = "cam smooth_start false";
    private static final int TICKS_AFTER_LOAD = 5;
    /** 6s=120，减去随后 {@code load} 的 5 tick，组2 的 {@code start} 落在 120。 */
    private static final int TICKS_AFTER_START_GROUP_1 = 120 - TICKS_AFTER_LOAD;
    /** 3s=60，减去随后 {@code load} 的 5 tick。 */
    private static final int TICKS_AFTER_START_GROUP_23 = 60 - TICKS_AFTER_LOAD;
    private static final float ROLL = 0.0F;
    private static final float FOV_WIDE = 80.0F;
    private static final float FOV_DEFAULT = 70.0F;
    private static final float FOV_NEAR = 58.0F;
    private static final float FOV_FOCUS = 50.0F;
    private static final float FOV_FOCUS_CLOSE = 42.0F;

    private static final CmdCamScheduler CAM = CmdCamScheduler.create(MainQuest11CabinCmdCamClient::ticksAfterCommand);

    private MainQuest11CabinCmdCamClient()
    {
    }

    public static void setup(
            Vec3 playerPos, float playerYRot, Vec3 lookCenter, Vec3 xiaoshiziLook, Vec3 kiroLook)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null)
        {
            LOGGER.warn("[CMDCam] Main 1.1 cabin camera skipped: local player is null.");
            return;
        }
        if (!CmdCamScheduler.dispatcherReady())
        {
            LOGGER.warn("[CMDCam] Main 1.1 cabin camera skipped: client command dispatcher not ready.");
            return;
        }

        Vec3 forward = horizontalForward(playerYRot);
        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
        Vec3 house = lookCenter;
        Vec3 playerLook = new Vec3(playerPos.x, playerPos.y + 1.6D, playerPos.z);

        Vec3 cabin1 = house.subtract(forward.scale(16.0D)).add(0.0D, 10.0D, 0.0D);
        Vec3 cabin2 = house.subtract(forward.scale(10.0D)).add(right.scale(6.0D)).add(0.0D, 3.0D, 0.0D);
        Vec3 cabin3 = playerLook.add(forward.scale(3.0D));

        List<String> commands = new ArrayList<>();
        commands.add("cam mode outside");
        commands.add("cam clear");
        commands.add(camAddLookingAt(cabin1, house, FOV_WIDE));
        commands.add(camAddLookingAt(cabin2, house, FOV_DEFAULT));
        commands.add(camAddLookingAt(cabin3, playerLook, FOV_NEAR));
        commands.add("cam save " + PATH_CABIN);

        appendFocusGroup(commands, PATH_XIAOSHIZI, xiaoshiziLook, forward, right);
        appendFocusGroup(commands, PATH_KIRO, kiroLook, forward, right);

        commands.add("cam load " + PATH_CABIN);
        CAM.replaceAndStart(commands);
        LOGGER.info("[CMDCam] Main 1.1 three camera groups saved during blackout.");
    }

    public static void startPlayback()
    {
        if (!CmdCamScheduler.dispatcherReady())
        {
            LOGGER.warn("[CMDCam] Main 1.1 cabin camera start skipped: client command dispatcher not ready.");
            return;
        }
        // 与组1起播同步打开简报（绕过 hideGui；服务端包作备份）
        QuestMissionBriefingOverlay.show(
                MainQuestDefs.MAIN_1_1.titleKey(),
                MainQuestDefs.MAIN_1_1.briefingOrgKey(),
                MainQuestDefs.MAIN_1_1.briefingLocationKey());
        CAM.replaceAndStart(
                CAM_DISABLE_SMOOTH_START,
                CAM_START_GROUP_1,
                "cam load " + PATH_XIAOSHIZI,
                CAM_START_GROUP_23,
                "cam load " + PATH_KIRO,
                CAM_START_GROUP_23);
        LOGGER.info("[CMDCam] Main 1.1 sequential three-group playback queued.");
    }

    private static void appendFocusGroup(
            List<String> commands, String pathName, Vec3 look, Vec3 forward, Vec3 right)
    {
        Vec3 p1 = look.subtract(forward.scale(3.2D)).add(right.scale(1.4D)).add(0.0D, 1.1D, 0.0D);
        Vec3 p2 = look.subtract(forward.scale(2.2D)).add(0.0D, 0.7D, 0.0D);
        Vec3 p3 = look.subtract(forward.scale(1.5D)).add(right.scale(-0.4D)).add(0.0D, 0.45D, 0.0D);
        commands.add("cam clear");
        commands.add(camAddLookingAt(p1, look, FOV_NEAR));
        commands.add(camAddLookingAt(p2, look, FOV_FOCUS));
        commands.add(camAddLookingAt(p3, look, FOV_FOCUS_CLOSE));
        commands.add("cam save " + pathName);
    }

    private static int ticksAfterCommand(String commandJustRun)
    {
        if (commandJustRun.startsWith("cam save "))
        {
            return CmdCamScheduler.TICKS_AFTER_SAVE_OR_LOAD;
        }
        if (commandJustRun.equals(CAM_START_GROUP_1))
        {
            return TICKS_AFTER_START_GROUP_1;
        }
        if (commandJustRun.equals(CAM_START_GROUP_23))
        {
            return TICKS_AFTER_START_GROUP_23;
        }
        if (commandJustRun.startsWith("cam load "))
        {
            return TICKS_AFTER_LOAD;
        }
        return CmdCamScheduler.TICKS_AFTER_NORMAL_COMMAND;
    }

    private static String camAddLookingAt(Vec3 from, Vec3 to, float fov)
    {
        return String.format(
                Locale.ROOT,
                "cam add %.3f %.3f %.3f %.1f %.1f %.1f %.1f",
                from.x, from.y, from.z,
                yawToward(from, to), pitchToward(from, to), ROLL, fov);
    }

    private static Vec3 horizontalForward(float yRot)
    {
        double yawRad = Math.toRadians(yRot);
        return new Vec3(-Math.sin(yawRad), 0.0D, Math.cos(yawRad));
    }

    private static float yawToward(Vec3 from, Vec3 to)
    {
        double dx = to.x - from.x;
        double dz = to.z - from.z;
        return (float) Math.toDegrees(Math.atan2(-dx, dz));
    }

    private static float pitchToward(Vec3 from, Vec3 to)
    {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        double horiz = Math.sqrt(dx * dx + dz * dz);
        return (float) -Math.toDegrees(Math.atan2(dy, horiz));
    }
}
