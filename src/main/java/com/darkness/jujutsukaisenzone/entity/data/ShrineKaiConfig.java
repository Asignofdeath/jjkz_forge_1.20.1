package com.darkness.jujutsukaisenzone.entity.data;

/**
 * 御厨子术式「解」特效参数。
 * <p>
 * 供 {@link com.darkness.jujutsukaisenzone.entity.SpecialEffectEntity}
 * 和 {@link com.darkness.jujutsukaisenzone.entity.SpecialEffectArrowEntity} 共用。
 */
public final class ShrineKaiConfig
{
    private ShrineKaiConfig() {}

    /** 解延迟发射箭的时间（tick） */
    public static final int LAUNCH_DELAY_TICKS = 8;
    /** 解箭飞行速度（格/tick） */
    public static final double ARROW_SPEED = 10.0D;
    /** 解切割 flyobj 最大射程（格）。 */
    public static final double FLYOBJ_MAX_RANGE = 50.0D;
    /** 解切割 flyobj 飞行速度（格/tick） */
    public static final double FLYOBJ_SPEED = 10.0D;
    /** 解柱形高度（格） */
    public static final int COLUMN_HEIGHT = 8;
    /** 解柱形采样半径（格） */
    public static final double COLUMN_RADIUS = 1.5D;
    /** 解柱内生物每 tick 伤害 */
    public static final float COLUMN_DAMAGE = 10.0F;
    /** 解到达终点后 Photon 存活时长（tick） */
    public static final int ARRIVAL_POST_PHOTON_TICKS = 40;

    // ---- Photon 命令 ----
    /** 解到达终点竖斩 Photon */
    public static final String PHOTON_KAI_ARRIVAL = "photon fx photon:slash_kai_1 entity @s 0 0 0 0 0 0 1 1 1 0 false false look";
    /** 解到达终点横斩 Photon */
    public static final String PHOTON_KAI_2_ARRIVAL = "photon fx photon:slash_kai_2 entity @s 0 0 0 0 0 0 1 1 1 0 false false look";
    /** 解切割 flyobj Photon */
    public static final String PHOTON_FLYOBJ_MOUNT = "photon fx photon:slash_kai_flyobj entity @s 0 0 0 0 0 0 1 1 1 0 false false look";
    /** 解切割 flyobj Photon 移除 */
    public static final String PHOTON_FLYOBJ_REMOVE = "photon fx remove entity @s false photon:slash_kai_flyobj";
}
