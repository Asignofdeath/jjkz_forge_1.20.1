package com.darkness.jujutsukaisenzone.entity.data;

/**
 * 无下限术式「苍」特效参数。
 */
public final class LimitlessBlueConfig
{
    private LimitlessBlueConfig() {}

    /** 苍锚点存活时长（tick） */
    public static final int LIFETIME_TICKS = 30;
    /**
     * 【配置项·默认值】苍吸附半径（格）。
     * <p>
     * 对应 {@code [skillCombat.limitlessBlue].pullRadiusBaseline}；运行时真值由
     * {@code LimitlessBlueSkillGrowth#pullRadius} 给出。⛔ 业务代码不要直接引用本常量
     * ——绕过 {@code SkillCombatConfigRegistry} 就会同时丢掉配置修改与咒术等级成长。
     */
    public static final double PULL_RADIUS = 20.0D;
    /** 【配置项·默认值】苍吸附半径每升一级咒术等级的增量（格）。 */
    public static final double PULL_RADIUS_PER_RANK = 1.0D;
    /** Shift+1 时施法者靠近苍中心到此半径内即停止吸附 */
    public static final double OWNER_PULL_STOP_RADIUS = 1.75D;
    /** 苍伤害判定半径（格） */
    public static final double DAMAGE_RADIUS = 7.0D;
    /** 苍吸附强度系数 */
    public static final double PULL_STRENGTH = 0.99D;
    /**
     * 【配置项·默认值】苍每间隔伤害的<b>技能倍率</b>（1.0 = 100% 术式强度）。
     * <p>
     * 对应 {@code [skillCombat.limitlessBlue].damageBaseline}；运行时真值由
     * {@code LimitlessBlueSkillGrowth#damage} 给出，⛔ 业务代码不要直接引用本常量。
     */
    public static final float DAMAGE = 0.40F;
    /** 【配置项·默认值】苍伤害倍率每升一级咒术等级的增量（强度面板开启时通常不生效）。 */
    public static final float DAMAGE_PER_RANK = 0.10F;
    /** 苍伤害间隔（tick） */
    public static final int DAMAGE_INTERVAL = 4;
    /** 每波随机采样尝试次数 */
    public static final int BLOCK_PULL_SAMPLE_ATTEMPTS = 192;
    /** 每波最多转化方块数 */
    public static final int BLOCK_PULL_MAX_PER_WAVE = 24;
    /** 方块转化间隔（tick） */
    public static final int BLOCK_PULL_CONVERT_INTERVAL = 2;
    /** 苍吸附方块强度 */
    public static final double BLOCK_PULL_STRENGTH = 0.72D;
    /** 苍吸附方块：距离越近额外加速系数 */
    public static final double BLOCK_PULL_DISTANCE_BONUS = 0.015D;
    /** 方块进入此距离后尝试填入中心球体 */
    public static final double BLOCK_BALL_ABSORB_DISTANCE = 1.35D;
    /** 中心球体最大半径（格） */
    public static final int BLOCK_BALL_MAX_RADIUS = 5;
}
