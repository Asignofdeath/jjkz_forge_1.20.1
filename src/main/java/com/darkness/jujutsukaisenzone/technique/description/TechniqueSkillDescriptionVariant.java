package com.darkness.jujutsukaisenzone.technique.description;

/**
 * 随玩家上下文（目前只有「机制型技能是否已解锁」）切换的技能描述：正文语言键、正文参数与简洁页语言键。
 * <p>
 * 存在的理由：审判术式把【死刑】与它的概率加成拆成了「量刑」「从重论处」两个机制型技能，未解锁时
 * 这些内容在实战里根本不会发生。描述若照满配形态写，玩家读到的就是一段「现在做不到」的规则
 * （见 {@code JudgmentTechniqueSkillSet#SKILL_SENTENCING} / {@code #SKILL_AGGRAVATION}）。
 * 因此做法是<b>换文案</b>，而不是在一段正文里堆满「需解锁」备注。
 */
public record TechniqueSkillDescriptionVariant(String descriptionKey, Object[] descriptionArgs, String briefKey)
{
    public TechniqueSkillDescriptionVariant
    {
        descriptionArgs = descriptionArgs == null ? new Object[0] : descriptionArgs.clone();
    }

    /**
     * @param descriptionKey 正文键，必填；为 null / 空串时整个变体作废，回退默认键。
     * @param briefKey       简洁页键；为 null / 空串表示沿用默认的 {@code .brief} 键。
     * @param descriptionArgs 正文参数，顺序必须与所选 lang 键里 {@code %s} 的出现次序一致。
     */
    public static TechniqueSkillDescriptionVariant of(
            String descriptionKey,
            String briefKey,
            Object... descriptionArgs)
    {
        return new TechniqueSkillDescriptionVariant(descriptionKey, descriptionArgs, briefKey);
    }
}
