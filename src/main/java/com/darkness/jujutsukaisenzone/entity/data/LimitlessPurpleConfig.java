package com.darkness.jujutsukaisenzone.entity.data;

/**
 * 无下限术式「茈」特效参数。
 */
public final class LimitlessPurpleConfig
{
    private LimitlessPurpleConfig() {}

    /** 茈蓄力时长（tick） */
    public static final int CHARGE_TICKS = 46;
    /** 茈箭飞行速度 */
    public static final float ARROW_SPEED = 2.0F;
    /** 茈箭射击散布 */
    public static final float ARROW_INACCURACY = 0.0F;
    /** 茈胸前锚点高度比例（相对脚底到头顶） */
    public static final double ANCHOR_CHEST_HEIGHT_RATIO = 0.45D;
    /** 茈胸前锚点前方距离 */
    public static final double ANCHOR_FORWARD_DISTANCE = 3.0D;
    /** 茈蓄力锚点 Photon 命令 */
    public static final String PHOTON_ANCHOR_COMMAND = "photon fx photon:limitless_purple_mini_fire entity @s";
    /** 茈箭 Photon 命令 */
    public static final String PHOTON_ARROW_COMMAND = "photon fx photon:limitless_purple_mini entity @s";
    /** 茈箭从锚点前方偏移 */
    public static final double ARROW_SPAWN_FORWARD = 0.55D;
    /** 茈箭生成 Y 偏移 */
    public static final double ARROW_SPAWN_Y_OFFSET = 0.08D;
    /** 茈箭球体方块擦除半径 */
    public static final double ARROW_BLOCK_ERASE_RADIUS = 2.0D;
    /** 茈箭球体每 tick 伤害 */
    public static final float ARROW_SPHERE_DAMAGE_PER_TICK = 20.0F;
}
