package com.darkness.jujutsukaisenzone.entity.data;

/**
 * 御厨子术式「捌」特效参数。
 */
public final class ShrineHachiConfig
{
    private ShrineHachiConfig() {}

    /** 捌持续时长（tick） */
    public static final int LIFETIME_TICKS = 60;
    /** 捌伤害间隔（tick） */
    public static final int DAMAGE_INTERVAL = 5;
    /**
     * 【配置项·默认值】捌每间隔伤害值。
     * <p>
     * 运行时真值由 {@code ShrineSkillGrowth#hachiDamage} 给出，⛔ 业务代码不要直接引用本常量。
     */
    public static final float DAMAGE = 0.25F;
    /** 【配置项·默认值】捌伤害每升一级咒术等级的增量。 */
    public static final float DAMAGE_PER_RANK = 0.10F;
    /** 捌 Photon 挂载命令 */
    public static final String PHOTON_MOUNT = "photon fx photon:slash_hachi entity @s 0 0 0 0 0 0 0.5 0.5 0.5 0 false false none";
    /** 捌 Photon 移除命令 */
    public static final String PHOTON_REMOVE = "photon fx remove entity @s false photon:slash_hachi";
}
