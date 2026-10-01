package com.darkness.jujutsukaisenzone.entity.data;

/**
 * 黑闪特效参数。
 */
public final class BlackFlashConfig
{
    private BlackFlashConfig() {}

    /** 黑闪 FX 锚点存活时长（tick） */
    public static final int FX_LIFETIME_TICKS = 30;
    /** 黑闪冲击波延迟（tick） */
    public static final int SHOCKWAVE_DELAY_TICKS = 10;
    /** 黑闪冲击波破坏半径 */
    public static final int SHOCKWAVE_RADIUS = 3;
    /** 黑闪冲击波方块破坏概率 */
    public static final double SHOCKWAVE_DESTROY_CHANCE = 0.30;
    /** 黑闪冲击波伤害值 */
    public static final float SHOCKWAVE_DAMAGE = 0.50F;
    /** 黑闪 Photon 命令 */
    public static final String PHOTON_COMMAND = "photon fx photon:black_flash entity @s 0 0 0 0 0 0 1 1 1 0 false false look";
    /** 黑闪屏幕振动持续 tick（40 = 2 秒） */
    public static final int SCREEN_SHAKE_DURATION_TICKS = 40;
    /** 黑闪屏幕振动首帧最大偏转（度） */
    public static final float SCREEN_SHAKE_MAX_DEGREES = 14.0F;
}
