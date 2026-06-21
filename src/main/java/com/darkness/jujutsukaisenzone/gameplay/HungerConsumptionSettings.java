package com.darkness.jujutsukaisenzone.gameplay;

public final class HungerConsumptionSettings
{
    public enum Mode
    {
        NONE(0.0f, "不消耗"),
        LOW(0.1f, "0.1倍"),
        VANILLA(1.0f, "原版");

        private final float multiplier;
        private final String zhLabel;

        Mode(float multiplier, String zhLabel)
        {
            this.multiplier = multiplier;
            this.zhLabel = zhLabel;
        }

        public float multiplier()
        {
            return multiplier;
        }

        public String zhLabel()
        {
            return zhLabel;
        }
    }

    private static volatile Mode currentMode = Mode.LOW;

    private HungerConsumptionSettings()
    {
    }

    public static Mode getMode()
    {
        return currentMode;
    }

    public static void setMode(Mode mode)
    {
        currentMode = mode == null ? Mode.LOW : mode;
    }
}
