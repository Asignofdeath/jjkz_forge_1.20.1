package com.darkness.jujutsukaisenzone.client;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayDeque;

/**
 * 无量空处结界构建完成时，在<strong>客户端</strong>以本地玩家身份执行 CMDCam 的 {@code /cam} 命令（Forge 客户端命令分发器，无前导斜杠）。
 * <p>
 * 命令按队列执行：默认间隔 {@link #TICKS_AFTER_NORMAL_COMMAND} 刻；{@code cam save} 之后 {@link #TICKS_AFTER_CAM_SAVE} 刻；另有镜头回放段的专用间隔常量。
 */
public final class LimitlessDomainCmdCamClient
{
    private static final Logger LOGGER = LogManager.getLogger();
    /** 普通命令执行完毕后，再隔多少刻执行下一条。 */
    private static final int TICKS_AFTER_NORMAL_COMMAND = 1;
    /** {@code cam save} 执行完毕后，再隔多少刻执行下一条。 */
    private static final int TICKS_AFTER_CAM_SAVE = 5;
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

    private static final ArrayDeque<String> CAM_COMMAND_QUEUE = new ArrayDeque<>();
    /** 距离下一条命令还剩多少客户端刻；{@code 0} 表示当前不在等待间隔（由 tick 驱动取下一条）。 */
    private static int ticksUntilNextCamCommand = 0;

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
     * 默认相邻间隔 {@value #TICKS_AFTER_NORMAL_COMMAND} 刻；{@code cam save} 之后 {@value #TICKS_AFTER_CAM_SAVE} 刻；回放段间隔见上。
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
        if (ClientCommandHandler.getDispatcher() == null)
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

        synchronized (CAM_COMMAND_QUEUE)
        {
            CAM_COMMAND_QUEUE.clear();
            ticksUntilNextCamCommand = 0;
            CAM_COMMAND_QUEUE.add("cam mode outside");
            CAM_COMMAND_QUEUE.add("cam clear");
            CAM_COMMAND_QUEUE.add("cam add " + a1x + " " + a1y + " " + a1z);
            CAM_COMMAND_QUEUE.add("cam add " + a2x + " " + a2y + " " + a2z + " " + CAMERA_A_POINT2_YAW + " " + CAMERA_A_POINT2_PITCH);
            CAM_COMMAND_QUEUE.add("cam save " + CMDCAM_PATH_A_NAME);
            CAM_COMMAND_QUEUE.add("cam clear");
            CAM_COMMAND_QUEUE.add("cam add " + b1x + " " + b1y + " " + b1z);
            CAM_COMMAND_QUEUE.add("cam add " + b1x + " " + b1y + " " + b1z + " " + CAMERA_B_POINT2_YAW + " " + CAMERA_B_POINT2_PITCH);
            CAM_COMMAND_QUEUE.add("cam save " + CMDCAM_PATH_B_NAME);
            CAM_COMMAND_QUEUE.add("cam clear");
            CAM_COMMAND_QUEUE.add("cam load " + CMDCAM_PATH_A_NAME);
            CAM_COMMAND_QUEUE.add(CAM_START_PLAYBACK_A);
            CAM_COMMAND_QUEUE.add("cam load " + CMDCAM_PATH_B_NAME);
            CAM_COMMAND_QUEUE.add(CAM_START_PLAYBACK_B);

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

    static void tickScheduledCamCommands()
    {
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
            if (!CAM_COMMAND_QUEUE.isEmpty())
            {
                ticksUntilNextCamCommand = ticksAfterCommand(cmd);
            }
        }
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

    /**
     * @param commandWithoutSlash 见 {@link ClientCommandHandler#runCommand(String)}
     */
    private static void runCamImmediate(String commandWithoutSlash)
    {
        boolean consumed = ClientCommandHandler.runCommand(commandWithoutSlash);
        if (!consumed)
        {
            LOGGER.warn("[CMDCam] Command not handled as client command (is CMDCam installed on this client?): {}", commandWithoutSlash);
        }
    }

    @Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class CamCommandScheduleTicker
    {
        private CamCommandScheduleTicker()
        {
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event)
        {
            if (event.phase != TickEvent.Phase.END)
            {
                return;
            }
            tickScheduledCamCommands();
        }
    }
}
