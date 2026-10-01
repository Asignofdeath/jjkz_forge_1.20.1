package com.darkness.jujutsukaisenzone.technique.development;

/**
 * 引用节点所指向的外部资源类型。
 */
public enum SkillTreeReferenceKind
{
    /** {@link com.darkness.jujutsukaisenzone.technique.commonskill.ICommonKeySkill#skillId()} */
    COMMON_SKILL,
    /** 术式开发里程碑：{@code referenceId} 为所需黑闪次数（十进制字符串）。 */
    BLACK_FLASH_COUNT,
    /** 降灵术等级：{@code referenceId} 为 {@link com.darkness.jujutsukaisenzone.player.SeanceTechniqueLevel} 枚举名。 */
    SEANCE_TECHNIQUE_LEVEL,
    /** 结界术等级：{@code referenceId} 为 {@link com.darkness.jujutsukaisenzone.player.BarrierTechniqueLevel} 枚举名。 */
    BARRIER_TECHNIQUE_LEVEL,
    /** 咒术等级：{@code referenceId} 为 {@link com.darkness.jujutsukaisenzone.player.PlayerCursedGradeData} 的 rank 整数字符串。 */
    CURSED_GRADE
}
