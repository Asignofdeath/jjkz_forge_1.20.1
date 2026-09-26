package com.darkness.jujutsukaisenzone.commandwheel;

/**
 * 指令圆盘条目 id（C2S 与目录共用，服务端按 id 分发）。
 * <p>
 * 仅术式专用指令；有独立快捷键的共通技能不进圆盘。
 */
public final class CommandWheelIds
{
    public static final String TEN_SHADOWS_SHIKIGAMI_MODE = "ten_shadows.shikigami_mode";
    public static final String TEN_SHADOWS_STANCE_TOGGLE = "ten_shadows.stance_toggle";
    public static final String TEN_SHADOWS_ATTACK_ORDER = "ten_shadows.attack_order";

    private CommandWheelIds()
    {
    }
}
