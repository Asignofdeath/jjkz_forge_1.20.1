package com.darkness.jujutsukaisenzone.blackhole;

/**
 * 黑洞_A 视觉参数（演示用常量，后续接 CfgSpec 配置层时替换）。
 */
public final class BlackHoleVisualConfig
{
    /** 全屏引力透镜开关 */
    public static final boolean ENABLE_LENSING = true;
    /** 吸积盘 / 喷流开关 */
    public static final boolean ENABLE_ACCRETION_DISK = true;
    /** 引力偏折强度倍率（原模组默认 1.5） */
    public static final float LENSING_STRENGTH = 1.5F;
    /** 同时参与透镜的最大黑洞数（shader uniform 上限 8） */
    public static final int MAX_LENSED_BLACK_HOLES = 4;
    /** 吸积盘角向分段数 */
    public static final int ACCRETION_DISK_SEGMENTS = 96;
    /** 吸积盘径向环数 */
    public static final int ACCRETION_DISK_RINGS = 12;
    /** 方块化模式（切比雪夫几何），演示默认关闭 */
    public static final boolean MINECRAFTIFY = false;

    private BlackHoleVisualConfig()
    {
    }
}
