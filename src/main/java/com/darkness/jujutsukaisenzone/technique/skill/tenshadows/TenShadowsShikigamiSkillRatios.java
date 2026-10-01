package com.darkness.jujutsukaisenzone.technique.skill.tenshadows;

/**
 * 十影式神招式技能倍率（显式设计，不是旧绝对伤换算）。
 * <p>
 * 出伤：{@code 式神对应通道强度 × 本表倍率}。对照玩家主动：法槌 30%、解柱 50%、无可谅解 90%。
 * 式神面板低于主人，自动技略低于玩家轻击；主动/大招贴近玩家中高档。魔虚罗用固定面板，倍率单独一档。
 */
public final class TenShadowsShikigamiSkillRatios
{
    /** 玉犬撕咬（自动、近距、可多只同时咬）。 */
    public static final float DIVINE_DOGS_BITE = 0.35F;
    /** 玉犬扑击。 */
    public static final float DIVINE_DOGS_POUNCE = 0.50F;

    /** 蛤蟆牵制：控制为主，附带术式伤。 */
    public static final float TOAD_RESTRAIN = 0.20F;

    /** 鵺雷击 AOE。 */
    public static final float NUE_LIGHTNING = 0.65F;
    /** 鵺俯冲撞击。 */
    public static final float NUE_DIVE = 0.60F;

    /** 大蛇破土。 */
    public static final float GREAT_SERPENT_AMBUSH = 0.80F;

    /** 贯牛冲锋路径。 */
    public static final float PIERCING_OX_CHARGE = 0.90F;

    /** 满象近距顶击。 */
    public static final float MAX_ELEPHANT_RAM = 0.55F;
    /** 满象水柱。 */
    public static final float MAX_ELEPHANT_WATER_JET = 0.60F;
    /** 满象陨石。 */
    public static final float MAX_ELEPHANT_METEOR = 1.20F;

    /** 魔虚罗砸地 AOE。 */
    public static final float MAHORAGA_SLAM = 0.85F;
    /** 魔虚罗击飞。 */
    public static final float MAHORAGA_LAUNCH = 0.65F;
    /** 魔虚罗投掷。 */
    public static final float MAHORAGA_THROW = 0.75F;
    /** 魔虚罗连拳每一击。 */
    public static final float MAHORAGA_PUNCH_HIT = 0.35F;
    /** 魔虚罗挥剑（非秒杀）。 */
    public static final float MAHORAGA_SWORD = 0.55F;
    /** 魔虚罗劈径（非秒杀）。 */
    public static final float MAHORAGA_WHEEL_SLASH = 1.00F;
    /** 魔虚罗声波。 */
    public static final float MAHORAGA_SONIC = 1.00F;
    /** 骑乘近战拳。 */
    public static final float MAHORAGA_RIDE_PUNCH = 0.70F;
    /** 骑乘踢击。 */
    public static final float MAHORAGA_RIDE_KICK = 0.60F;

    private TenShadowsShikigamiSkillRatios()
    {
    }
}
