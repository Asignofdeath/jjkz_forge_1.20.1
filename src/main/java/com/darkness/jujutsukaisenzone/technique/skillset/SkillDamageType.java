package com.darkness.jujutsukaisenzone.technique.skillset;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import net.minecraft.network.chat.Component;

/**
 * 技能的伤害分型。
 * <p>
 * 只做归类，不做数值结算——它回答的是「这个技能打出的伤害算什么」，供以后按分型挑目标的
 * 机制使用（例如「只加成体术伤害」「只加成术式伤害」）。
 * <p>
 * 判据不是技能叫什么，而是<b>伤害从哪条通道出去</b>：
 * <ul>
 *   <li>{@link #TECHNIQUE} —— 经 {@code TechniqueDamageSources.deal} 结算的术式伤害</li>
 *   <li>{@link #MARTIAL} —— 不经术式通道的物理伤害，如法槌投射物的 {@code thrown}</li>
 *   <li>{@link #COMPOSITE} —— 同一技能同时打出上述两者</li>
 * </ul>
 * 默认值是 {@link #TECHNIQUE}：本模组的技能绝大多数都是术式伤害，
 * 所以只有非术式的技能需要在构造 {@link TechniqueSkill} 时显式指定。
 */
public enum SkillDamageType
{
    /** 体术：纯物理打击，不走术式伤害通道。 */
    MARTIAL,
    /** 术式：经术式伤害通道结算，附带咒术归因。 */
    TECHNIQUE,
    /** 复合：体术与术式同时成立。 */
    COMPOSITE;

    private static final String LANG_PREFIX = "gui." + JuJutsuKaisenZone.MODID + ".damage_type.";

    /** 供悬停框 / 开发树展示用的语言键。 */
    public String translationKey()
    {
        return LANG_PREFIX + this.name().toLowerCase(java.util.Locale.ROOT);
    }

    public Component displayName()
    {
        return Component.translatable(translationKey());
    }
}
