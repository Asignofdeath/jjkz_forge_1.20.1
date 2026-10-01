package com.darkness.jujutsukaisenzone.commandwheel;

/**
 * 指令圆盘条目 id（C2S 与目录共用，服务端按 id 分发）。
 * <p>
 * 通用页放全术式共用指令；术式专属页由各 skillset 提供。
 */
public final class CommandWheelIds
{
    /** 通用页：领域展开（等价 Alt+B）。 */
    public static final String DOMAIN_EXPANSION = "general.domain_expansion";
    /** 通用页：领域模式（常规 / 瞬发）。 */
    public static final String DOMAIN_MODE = "general.domain_mode";

    public static final String TEN_SHADOWS_SHIKIGAMI_MODE = "ten_shadows.shikigami_mode";
    public static final String TEN_SHADOWS_STANCE_TOGGLE = "ten_shadows.stance_toggle";
    public static final String TEN_SHADOWS_ATTACK_ORDER = "ten_shadows.attack_order";
    public static final String MAHORAGA_RIDE = "mahoraga.ride";

    private CommandWheelIds()
    {
    }
}
