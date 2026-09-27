package com.darkness.jujutsukaisenzone.technique.se.mechanic;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.technique.se.WorkEfficiencySeMechanic;
import net.minecraft.network.chat.Component;

/**
 * 填满的工时单：单次耗咒力达标即进入与「无可谅解」相同的工作效率状态。
 */
public final class FilledTimesheetMechanic implements WorkEfficiencySeMechanic
{
    public static final String SE_ID = "filled_timesheet";

    /** 对齐一文字量级咒力，避免轻量消费误触。 */
    public static final int TRIGGER_CONSUME_THRESHOLD = 40;

    @Override
    public String seId()
    {
        return SE_ID;
    }

    @Override
    public int triggerConsumeThreshold()
    {
        return TRIGGER_CONSUME_THRESHOLD;
    }

    @Override
    public Component effectDescription()
    {
        return Component.translatable(
                "gui." + JuJutsuKaisenZone.MODID + ".se.effect." + SE_ID);
    }
}
