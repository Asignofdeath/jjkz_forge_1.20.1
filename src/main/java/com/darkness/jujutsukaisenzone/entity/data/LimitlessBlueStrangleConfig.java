package com.darkness.jujutsukaisenzone.entity.data;

/**
 * 无下限术式「苍之绞杀」特效参数。
 */
public final class LimitlessBlueStrangleConfig
{
    private LimitlessBlueStrangleConfig() {}

    /** 绞杀持续时长（tick） */
    public static final int DURATION_TICKS = 60;
    /** 绞杀伤害间隔（tick）。≥11 保证每次真实结算都不被原版无敌帧吞掉。 */
    public static final int DAMAGE_INTERVAL = 15;
    /** 施法索敌距离（格） */
    public static final double CAST_RANGE = 12.0D;
    /** 索敌视线夹角余弦阈值 */
    public static final double TARGET_MIN_DOT = 0.60D;
    /**
     * 【配置项·默认值】每次绞杀伤害的<b>技能倍率</b>（1.0 = 100% 术式强度）。
     * <p>
     * 对应 {@code [skillCombat.limitlessBlueStrangle].damageBaseline}；运行时真值由
     * {@code LimitlessBlueStrangleSkillGrowth#damage} 给出，⛔ 业务代码不要直接引用本常量。
     */
    public static final float DAMAGE = 0.80F;
    /** 【配置项·默认值】绞杀伤害倍率每升一级咒术等级的增量（强度面板开启时通常不生效）。 */
    public static final float DAMAGE_PER_RANK = 0.10F;
    /** 每次命中加的切痕基础层数（大量切痕 → 断肢） */
    public static final float CUT_MARKS_BASE = 6.0F;
}
