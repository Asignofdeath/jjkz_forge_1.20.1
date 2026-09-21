package com.darkness.jujutsukaisenzone.client;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.technique.skill.JudgmentTrialSkillLogic;
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
 * 审判者愤怒演出：审判者正前方 2 格、高度 7 格正对正脸的 CMDCam 镜头（{@code cam start 2s}）。
 * <p>
 * 编排方式参照 {@link JudgmentTrialOpeningCmdCamClient}。
 */
public final class JudgmentJudgeAngryCmdCamClient
{
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int TICKS_AFTER_NORMAL_COMMAND = 2;
    /** 愤怒镜头期间隐藏聊天栏，与 {@link JudgmentTrialSkillLogic#DEATH_VERDICT_TITLE_DELAY_TICKS} 同步。 */
    private static final int HIDE_CHAT_TICKS = JudgmentTrialSkillLogic.DEATH_VERDICT_TITLE_DELAY_TICKS;

    private static final String CAM_START_PLAYBACK = "cam start 2s";

    /** 镜头相对审判者实体的水平前方距离（格）。 */
    private static final double FORWARD_DIST = 2.0D;
    /** 镜头与注视点相对审判者实体的高度（格）；审判者模型身高 4 格，镜头再上调 3 格。 */
    private static final double JUDGE_CAMERA_HEIGHT = 7.0D;
    private static final float ROLL = 0.0F;
    private static final float FOV_DEFAULT = 70.0F;

    private static int hideChatTicks;

    private JudgmentJudgeAngryCmdCamClient()
    {
    }

    public static void playJudgeFaceCamera(double judgeX, double judgeY, double judgeZ, float judgeYRot)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null)
        {
            LOGGER.warn("[CMDCam] Judge angry camera skipped: local player is null.");
            return;
        }
        if (ClientCommandHandler.getDispatcher() == null)
        {
            LOGGER.warn("[CMDCam] Judge angry camera skipped: client command dispatcher not ready.");
            return;
        }

        hideChatTicks = HIDE_CHAT_TICKS;
        List<String> commands = buildCommandList(judgeX, judgeY, judgeZ, judgeYRot);
        LOGGER.info("[CMDCam] Judge angry: scheduling {} camera commands.", commands.size());
        scheduleCommandChain(commands, 0, 0);
    }

    private static List<String> buildCommandList(double judgeX, double judgeY, double judgeZ, float judgeYRot)
    {
        double lookX = judgeX;
        double lookY = judgeY + JUDGE_CAMERA_HEIGHT;
        double lookZ = judgeZ;

        Vec3 forward = horizontalForward(judgeYRot);
        double camX = judgeX + forward.x * FORWARD_DIST;
        double camZ = judgeZ + forward.z * FORWARD_DIST;
        double camY = judgeY + JUDGE_CAMERA_HEIGHT;
        float yaw = yawToward(camX, camY, camZ, lookX, lookY, lookZ);
        float pitch = pitchToward(camX, camY, camZ, lookX, lookY, lookZ);
        String camAdd = camAddWithOrientation(camX, camY, camZ, yaw, pitch, FOV_DEFAULT);

        List<String> commands = new ArrayList<>(4);
        commands.add("cam mode outside");
        commands.add("cam clear");
        commands.add(camAdd);
        commands.add(CAM_START_PLAYBACK);
        return commands;
    }

    private static String camAddWithOrientation(double x, double y, double z, float yaw, float pitch, float fov)
    {
        return String.format(
                Locale.ROOT,
                "cam add %.3f %.3f %.3f %.1f %.1f %.1f %.1f",
                x, y, z, yaw, pitch, ROLL, fov);
    }

    private static Vec3 horizontalForward(float yRot)
    {
        double yawRad = Math.toRadians(yRot);
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

    private static void scheduleCommandChain(List<String> commands, int index, int ticksBeforeRun)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null)
        {
            return;
        }
        if (index >= commands.size())
        {
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

            if (index + 1 < commands.size())
            {
                scheduleCommandChain(commands, index + 1, TICKS_AFTER_NORMAL_COMMAND);
            }
        });
    }

    private static void runCamImmediate(String commandWithoutSlash)
    {
        boolean consumed = ClientCommandHandler.runCommand(commandWithoutSlash);
        if (!consumed)
        {
            LOGGER.warn("[CMDCam] Judge angry command not handled (is CMDCam installed?): {}",
                    commandWithoutSlash);
            return;
        }
        LOGGER.info("[CMDCam] Judge angry: {}", commandWithoutSlash);
    }

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
            if (hideChatTicks > 0)
            {
                hideChatTicks--;
            }
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
