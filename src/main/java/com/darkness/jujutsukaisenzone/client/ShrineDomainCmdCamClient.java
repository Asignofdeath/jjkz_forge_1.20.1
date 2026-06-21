package com.darkness.jujutsukaisenzone.client;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 伏魔神龛领域构建完成时，在<strong>客户端</strong>以本地玩家身份执行 CMDCam 的 {@code /cam} 命令（Forge 客户端命令分发器，无前导斜杠）。
 * <p>
 * 流程：先设置路径点，领域构建完成后第 3 秒（60 tick）自动执行 {@code cam start 2s} 回放运镜。
 * 路径点编排沿用 {@link Minecraft#execute(Runnable)} 链式调度，长延迟倒计时改用 {@code ClientTickEvent} 逐 tick 递减。
 */
public final class ShrineDomainCmdCamClient
{
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int TICKS_AFTER_NORMAL_COMMAND = 2;
    /** cam start 前的等待 tick 数：60 tick = 3 秒 */
    private static final int TICKS_BEFORE_CAM_START = 60;
    /** 镜头设置期间隐藏聊天栏的 tick 数：200 tick = 10 秒 */
    private static final int HIDE_CHAT_TICKS = 200;

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

    /** cam start 倒计时：>0 时每 {@code ClientTickEvent.Phase.END} 递减，归零时执行 {@code cam start 2s} */
    private static int camStartDelayTicks;
    /** 聊天栏隐藏倒计时：>0 时取消 {@link VanillaGuiOverlay#CHAT_PANEL} 渲染 */
    private static int hideChatTicks;

    private ShrineDomainCmdCamClient()
    {
    }

    /**
     * @param px   玩家实体 X
     * @param py   玩家实体 Y
     * @param pz   玩家实体 Z
     * @param yRot 玩家水平朝向（用于计算「前方」偏移）
     */
    public static void applyCameraPathSetup(double px, double py, double pz, float yRot)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null)
        {
            LOGGER.warn("[CMDCam] Shrine domain camera setup skipped: local player is null.");
            return;
        }
        if (ClientCommandHandler.getDispatcher() == null)
        {
            LOGGER.warn("[CMDCam] Client command dispatcher not ready; skipped shrine domain camera setup.");
            return;
        }

        List<String> commands = buildCommandList(px, py, pz, yRot);
        LOGGER.info("[CMDCam] Shrine domain camera setup: scheduling {} path commands, cam start in {} ticks.",
                commands.size(), TICKS_BEFORE_CAM_START);

        // 开始设置镜头时隐藏聊天栏 10 秒
        hideChatTicks = HIDE_CHAT_TICKS;

        scheduleCommandChain(commands, 0, 0);
    }

    private static List<String> buildCommandList(double px, double py, double pz, float yRot)
    {
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

        List<String> commands = new ArrayList<>(5);
        commands.add("cam mode outside");
        commands.add("cam clear");
        commands.add(camAddWithOrientation(x1, y1, z1, PITCH_HIGH, FOV_DEFAULT));
        commands.add(camAddWithOrientation(x2, y2, z2, PITCH_NEAR, FOV_ZOOM_IN));
        commands.add(camAddWithOrientation(x3, y3, z3, PITCH_FAR, FOV_ZOOM_OUT));
        return commands;
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

    /**
     * 路径点命令链式调度，命令间间隔 {@value TICKS_AFTER_NORMAL_COMMAND} tick。
     * <p>
     * 链结束后启动 cam start 倒计时。
     */
    private static void scheduleCommandChain(List<String> commands, int index, int ticksBeforeRun)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || index >= commands.size())
        {
            if (index >= commands.size())
            {
                // 路径点全部设置完毕，启动 cam start 倒计时
                camStartDelayTicks = TICKS_BEFORE_CAM_START;
                LOGGER.info("[CMDCam] Shrine domain path points set. cam start will fire in {} ticks.",
                        TICKS_BEFORE_CAM_START);
            }
            return;
        }

        mc.execute(() -> {
            if (ticksBeforeRun > 0)
            {
                scheduleCommandChain(commands, index, ticksBeforeRun - 1);
                return;
            }

            String command = commands.get(index);
            runCamImmediate(command);

            int delayBeforeNext = index + 1 < commands.size()
                    ? TICKS_AFTER_NORMAL_COMMAND
                    : 0;
            scheduleCommandChain(commands, index + 1, delayBeforeNext);
        });
    }

    private static void runCamImmediate(String commandWithoutSlash)
    {
        boolean consumed = ClientCommandHandler.runCommand(commandWithoutSlash);
        if (!consumed)
        {
            LOGGER.warn("[CMDCam] Shrine domain command not handled (is CMDCam installed?): {}", commandWithoutSlash);
            return;
        }
        LOGGER.info("[CMDCam] Shrine domain: {}", commandWithoutSlash);
    }

    // ==================== 客户端事件处理（cam start 倒计时 + 聊天栏隐藏） ====================

    @Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class ClientEventHandlers
    {
        private ClientEventHandlers()
        {
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event)
        {
            if (event.phase != TickEvent.Phase.END)
            {
                return;
            }

            // 聊天栏隐藏倒计时
            if (hideChatTicks > 0)
            {
                hideChatTicks--;
            }

            // cam start 倒计时
            if (camStartDelayTicks <= 0)
            {
                return;
            }

            camStartDelayTicks--;
            if (camStartDelayTicks > 0)
            {
                return;
            }

            // 倒计时归零，执行 cam start
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || ClientCommandHandler.getDispatcher() == null)
            {
                LOGGER.warn("[CMDCam] Shrine domain cam start skipped: player/dispatcher not ready.");
                return;
            }

            mc.execute(() -> {
                boolean consumed = ClientCommandHandler.runCommand("cam start 2s");
                if (consumed)
                {
                    LOGGER.info("[CMDCam] Shrine domain: cam start 2s");
                }
                else
                {
                    LOGGER.warn("[CMDCam] Shrine domain cam start not handled (is CMDCam installed?).");
                }
            });
        }

        @SubscribeEvent
        public static void onRenderGuiOverlayPre(RenderGuiOverlayEvent.Pre event)
        {
            if (hideChatTicks <= 0)
            {
                return;
            }
            if (event.getOverlay().equals(VanillaGuiOverlay.CHAT_PANEL.type()))
            {
                event.setCanceled(true);
            }
        }
    }
}
