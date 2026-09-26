package com.darkness.jujutsukaisenzone.technique.skillset;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import net.minecraft.network.chat.Component;

/**
 * 伤害分型：按<strong>叙事</strong>回答「这一击是什么能力造成的」，不是按 DamageSource 通道，
 * 也不是技能上的基础字段。
 * <p>
 * 粒度是<strong>每一次伤害结算</strong>。同一技能可多次出伤、每次不同类型；
 * 「复合」指<strong>同一次</strong>结算同时叠加体术能力与术式能力（如黑闪）。
 * <ul>
 *   <li>{@link #TECHNIQUE} —— 术式能力：咒力 / 咒术 / 术式相关（斩击、苍、法槌爆炸……）</li>
 *   <li>{@link #MARTIAL} —— 体术能力：纯物理动作（EpicFight、飞踢、冲撞……）</li>
 *   <li>{@link #COMPOSITE} —— 同一次伤害上体术与术式叠加（黑闪、处刑必杀……）</li>
 * </ul>
 * 挂在出伤点：{@code TechniqueDamageSources.deal(..., type)} 或 {@code runWithType}。
 * 技能描述正文直接写「N 点术式伤害」等，不另做技能级分型字段。
 */
public enum SkillDamageType
{
    /** 体术：本次伤害由体术能力造成。 */
    MARTIAL,
    /** 术式：本次伤害由术式能力造成。 */
    TECHNIQUE,
    /**
     * 复合：本次伤害同时叠加体术能力与术式能力。
     * <p>
     * ⚠️ 复合<b>不参与暴击</b>（见 {@code CritRules#canCrit}）。目前它只出现在黑闪与处刑必杀两处：
     * 前者本就以暴击为门槛、后者是致死量改写，都不该再叠一道暴击浮动。
     * 将来新增复合分型的技能时它会<b>静默吃不到暴击</b>——先决定要不要动 {@code canCrit}，别以为会自动生效。
     */
    COMPOSITE;

    private static final String LANG_PREFIX = "gui." + JuJutsuKaisenZone.MODID + ".damage_type.";

    /** 供需要单独显示分型名时用的语言键（描述正文一般直接写死「术式」「体术」）。 */
    public String translationKey()
    {
        return LANG_PREFIX + this.name().toLowerCase(java.util.Locale.ROOT);
    }

    public Component displayName()
    {
        return Component.translatable(translationKey());
    }
}
