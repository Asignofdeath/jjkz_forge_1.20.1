package com.darkness.jujutsukaisenzone.entity.data;

/**
 * 无下限术式「极之番——暗星」（黑洞_A 时空坍缩体）参数。
 */
public final class BlackHoleSkillConfig
{
    private BlackHoleSkillConfig() {}

    /** 飞行速度（格 / tick） */
    public static final double FLIGHT_SPEED = 1.2D;
    /** 最大飞行时长（tick），超时原地落定 */
    public static final int MAX_FLIGHT_TICKS = 60;
    /** 驻留坍缩时长（tick） */
    public static final int DURATION_TICKS = 100;
    /** 驻留伤害间隔（tick）。≥11 保证每次真实结算都不被原版无敌帧吞掉。 */
    public static final int DAMAGE_INTERVAL = 15;
    /** 引力作用半径 = 史瓦西阴影半径 × 该系数 */
    public static final double ZONE_RADIUS_FACTOR = 1.5D;
    /** 飞行擦伤判定半径 = 史瓦西阴影半径 × 该系数 */
    public static final double SCRATCH_RADIUS_FACTOR = 1.2D;
    /** 非玩家实体的牵引加速度（格 / tick² 上限近似，直接加在速度上） */
    public static final double PULL_ACCELERATION = 0.08D;
    /** 出手点 = 眼睛位置沿视线前移该距离 */
    public static final double SPAWN_FROM_EYE = 1.5D;

    /**
     * 【配置项·默认值】飞行擦伤的<b>技能倍率</b>（1.0 = 100% 术式强度）。
     * <p>
     * 对应 {@code [skillCombat.limitlessDarkStar].scratchDamage}；运行时真值由
     * {@code LimitlessDarkStarSkillGrowth#scratchDamage} 给出，⛔ 业务代码不要直接引用本常量。
     */
    public static final float SCRATCH_DAMAGE = 0.40F;
    /** 【配置项·默认值】飞行擦伤倍率每升一级咒术等级的增量（强度面板开启时通常不生效）。 */
    public static final float SCRATCH_DAMAGE_PER_RANK = 0.05F;
    /** 【配置项·默认值】驻留坍缩每跳伤害的技能倍率。 */
    public static final float DAMAGE = 0.50F;
    /** 【配置项·默认值】驻留伤害倍率每升一级咒术等级的增量。 */
    public static final float DAMAGE_PER_RANK = 0.08F;
}
