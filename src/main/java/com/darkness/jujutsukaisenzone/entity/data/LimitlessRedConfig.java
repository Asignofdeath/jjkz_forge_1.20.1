package com.darkness.jujutsukaisenzone.entity.data;

/**
 * 无下限术式「赫」特效参数。
 */
public final class LimitlessRedConfig
{
    private LimitlessRedConfig() {}

    /** 赫发射前摇（tick），2.5 秒 */
    public static final int ARROW_DELAY_TICKS = 50;
    /** 赫箭飞行速度（格/tick） */
    public static final float ARROW_SPEED = 4.0F;
    /** 赫箭射击散布 */
    public static final float ARROW_INACCURACY = 0.0F;
    /** 赫箭飞行 Photon 命令 */
    public static final String PHOTON_FLYOBJ_COMMAND = "photon fx photon:limitless_newaka_flyobj entity @s";
    /** 赫箭命中爆炸 Photon 命令 */
    public static final String PHOTON_EXPLOSION_COMMAND = "photon fx photon:limitless_newaka_explostion_somke entity @s";
    /** 蓄力挂点偏移阶段切换时刻（tick），按下赫后满 1.6 秒进入第二阶段 */
    public static final int CHARGE_OFFSET_PHASE_SWITCH_TICKS = 32;
    /** 第一阶段（0~1.6 秒）：沿视线前方偏移（格） */
    public static final double CHARGE_FORWARD_OFFSET_EARLY = 0.5D;
    /** 第二阶段（1.6 秒后）：沿视线前方偏移（格） */
    public static final double CHARGE_FORWARD_OFFSET_LATE = 1.0D;
    /** 第二阶段（1.6 秒后）：向下偏移（格） */
    public static final double CHARGE_DOWN_OFFSET_LATE = 0.35D;
    /** 蓄力特效相对施法者视角的左向偏移（格，正值向左） */
    public static final double CHARGE_POSITION_LEFT_OFFSET = 0.25D;
    /** 赫箭从眼部前方生成的距离 */
    public static final double SPAWN_FROM_EYE = 1.8D;
    /** 赫箭生成 Y 偏移 */
    public static final double SPAWN_Y_OFFSET = -0.28D;
    /** 赫箭从锚点前方偏移 */
    public static final double ARROW_SPAWN_FORWARD = 0.8D;
    /** 赫箭生成 Y 偏移 */
    public static final double ARROW_SPAWN_Y_OFFSET = 0.12D;
    /**
     * 【配置项·默认值】赫箭命中爆炸威力。
     * <p>
     * 运行时真值由 {@code LimitlessRedSkillGrowth#explosionPower} 给出，
     * ⛔ 业务代码不要直接引用本常量。
     */
    public static final float ARROW_EXPLOSION_POWER = 14.0F;
    /** 【配置项·默认值】赫爆炸威力每升一级咒术等级的增量。 */
    public static final float EXPLOSION_POWER_PER_RANK = 2.0F;
    /** 【配置项·默认值】赫箭爆炸击飞半径；运行时真值走 {@code LimitlessRedSkillGrowth#knockbackRadius}。 */
    public static final double ARROW_KNOCKBACK_RADIUS = 18.0D;
    /** 【配置项·默认值】赫击飞范围每升一级咒术等级的增量（格）。 */
    public static final double KNOCKBACK_RADIUS_PER_RANK = 0.0D;
    /** 赫箭爆炸击飞基础强度 */
    public static final double ARROW_KNOCKBACK_BASE = 3.2D;
    /** 赫箭爆炸击飞上抛分量 */
    public static final double ARROW_KNOCKBACK_UP = 1.15D;
    /** 赫箭爆炸后存活时长（tick），承载后续 Photon */
    public static final int ARROW_POST_EXPLOSION_LIFE_TICKS = 10;
    /** 额外 TNT 式大爆炸烟尘（{@link net.minecraft.core.particles.ParticleTypes#EXPLOSION_EMITTER}）数量 */
    public static final int EXTRA_TNT_EXPLOSION_EMITTER_COUNT = 3;
    /** 额外 TNT 式爆炸火花（{@link net.minecraft.core.particles.ParticleTypes#EXPLOSION}）数量 */
    public static final int EXTRA_TNT_EXPLOSION_BURST_COUNT = 32;
    /** 额外爆炸火花散布半径（格） */
    public static final double EXTRA_TNT_EXPLOSION_BURST_SPREAD = 1.2D;

    /** 赫爆炸最多弹飞的方块数量 */
    public static final int BLOCK_LAUNCH_MAX_COUNT = 32;
    /** 赫爆炸弹飞方块的最小距离（格），爆炸中心附近不弹飞 */
    public static final double BLOCK_LAUNCH_MIN_DISTANCE = 1.5D;
    /** 赫爆炸弹飞方块的水平初速度（格/tick） */
    public static final double BLOCK_LAUNCH_SPEED = 2.8D;
    /** 赫爆炸弹飞方块的上抛初速度（格/tick） */
    public static final double BLOCK_LAUNCH_UP_SPEED = 1.15D;
    /** 弹飞方块落地伤害：每格下落伤害 */
    public static final float BLOCK_LAUNCH_HURT_DAMAGE_PER_DISTANCE = 2.0F;
    /** 弹飞方块落地伤害上限 */
    public static final int BLOCK_LAUNCH_HURT_MAX = 40;
}
