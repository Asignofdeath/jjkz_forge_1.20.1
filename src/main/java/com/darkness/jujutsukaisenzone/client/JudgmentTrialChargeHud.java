package com.darkness.jujutsukaisenzone.client;

import com.darkness.jujutsukaisenzone.technique.skill.JudgmentTrialSkillLogic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/**
 * 「审判」蓄力条状态；仅由服务端 S2C 包与 session 同步驱动显示，绘制由 {@link JjkzChargeBarOverlay} 调度。
 */
public final class JudgmentTrialChargeHud
{
    private static final int FILL_COLOR = 0xFFCCAA44;

    private static boolean visible;
    private static int lockChargeStartTick = -1;
    private static float serverProgress = -1.0F;

    private JudgmentTrialChargeHud()
    {
    }

    /** Session 开始：显示 0% 等待锁定。 */
    public static void showAwaitingLock()
    {
        visible = true;
        serverProgress = 0.0F;
        lockChargeStartTick = -1;
    }

    public static void setChargeProgress(float chargeProgress)
    {
        visible = true;
        serverProgress = Mth.clamp(chargeProgress, 0.0F, 1.0F);
        if (lockChargeStartTick < 0)
        {
            lockChargeStartTick = currentPlayerTick();
        }
    }

    public static void hide()
    {
        visible = false;
        lockChargeStartTick = -1;
        serverProgress = -1.0F;
    }

    public static boolean isVisible()
    {
        return visible;
    }

    static void render(
            GuiGraphics graphics,
            Minecraft minecraft,
            int screenWidth,
            int screenHeight,
            float partialTick,
            int offsetY)
    {
        if (!visible)
        {
            return;
        }

        float progress = resolveProgress(partialTick);
        JjkzChargeBarHud.drawCentered(graphics, minecraft, screenWidth, screenHeight, "审判", progress, FILL_COLOR, offsetY);
    }

    private static int currentPlayerTick()
    {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null ? minecraft.player.tickCount : 0;
    }

    private static float resolveProgress(float partialTick)
    {
        if (!visible)
        {
            return 0.0F;
        }
        if (serverProgress >= 0.0F)
        {
            return serverProgress;
        }
        if (lockChargeStartTick < 0)
        {
            return 0.0F;
        }
        float heldTicks = currentPlayerTick() - lockChargeStartTick + partialTick;
        return Mth.clamp(heldTicks / (float) JudgmentTrialSkillLogic.LOCK_CHARGE_TICKS, 0.0F, 1.0F);
    }
}
