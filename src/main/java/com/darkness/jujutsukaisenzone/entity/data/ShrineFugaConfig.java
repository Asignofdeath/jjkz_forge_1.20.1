package com.darkness.jujutsukaisenzone.entity.data;

/**
 * 御厨子术式「「灶」开」特效参数。
 */
public final class ShrineFugaConfig
{
    private ShrineFugaConfig()
    {
    }

    /** 蓄力所需按住时长：2 秒（20 tick/s），满后松手发射。 */
    public static final int CHARGE_TICKS = 40;
    /** 火焰箭最大射程（格） */
    public static final double MAX_RANGE = 50.0D;
    /** 火焰箭飞行速度（格/tick） */
    public static final double PROJECTILE_SPEED = 2.0D;
    /** 发射后该距离内不做碰撞判定，避免锚点/玩家身体导致秒炸 */
    public static final double COLLISION_MIN_TRAVEL = 2.0D;
    /** 玩家面前锚点前方距离（格） */
    public static final double ANCHOR_FORWARD_DISTANCE = 2.0D;
    /** 锚点高度比例（相对脚底到头顶） */
    public static final double ANCHOR_HEIGHT_RATIO = 0.55D;
    /** 爆炸持续伤害半径（格） */
    public static final double BOOM_RADIUS = 30.0D;
    /** 爆炸方块清除半径（格） */
    public static final double BOOM_BLOCK_BREAK_RADIUS = 15.0D;
    /** 每完成一层壳层向外扩张的距离（格） */
    public static final double BOOM_BLOCK_CLEAR_SHELL_STEP = 0.75D;
    /** 每 tick 最多替换为空气的方块数，避免卡顿 */
    public static final int BOOM_BLOCK_CLEAR_MAX_BLOCKS_PER_TICK = 600;
    /** 爆炸区域每 tick 伤害 */
    public static final float BOOM_DAMAGE_PER_TICK = 400.0F;
    /** 爆炸持续时长（tick） */
    public static final int BOOM_LIFETIME_TICKS = 60;
    /** 爆炸屏幕振动半径（格） */
    public static final double BOOM_SCREEN_SHAKE_RADIUS = 55.0D;
    /** 爆炸屏幕振动时长：4 秒（20 tick/s） */
    public static final int BOOM_SCREEN_SHAKE_TICKS = 80;
    /** 爆炸屏幕振动最大偏转（度） */
    public static final float BOOM_SCREEN_SHAKE_MAX_DEG = 11.0F;
    /** 爆炸瞬间全屏闪白时长：0.5 秒（20 tick/s）；淡出为 0，以便峰值白度无缝交给世界层。 */
    public static final int BOOM_WHITE_FLASH_TOTAL_TICKS = 10;
    public static final int BOOM_WHITE_FLASH_FADE_IN_TICKS = 2;
    public static final int BOOM_WHITE_FLASH_FADE_OUT_TICKS = 0;
    public static final float BOOM_WHITE_FLASH_PEAK_ALPHA = 1.0F;
    /** 全屏闪白结束后衔接的世界层闪白：0.8 秒（20 tick/s） */
    public static final int BOOM_WORLD_LAYER_FLASH_TOTAL_TICKS = 16;
    /** 无缝衔接：世界层闪白不做淡入，直接以峰值透明度开始 */
    public static final int BOOM_WORLD_LAYER_FLASH_FADE_IN_TICKS = 0;
    public static final int BOOM_WORLD_LAYER_FLASH_FADE_OUT_TICKS = 16;
    public static final float BOOM_WORLD_LAYER_FLASH_PEAK_ALPHA = 1.0F;

    /** auto rotation 使用 look：蓄力与飞行阶段均挂载同一特效 */
    public static final String PHOTON_FUGA_COMMAND = "photon fx photon:fuga entity @s 0 0 0 0 0 0 1 1 1 0 false false look";
    public static final String PHOTON_FUGA_REMOVE_COMMAND = "photon fx remove entity @s false photon:fuga";
    public static final String PHOTON_BOOM_COMMAND = "photon fx photon:fuga_boom entity @s";
    public static final String PHOTON_BOOM_REMOVE_COMMAND = "photon fx remove entity @s false photon:fuga_boom";
}
