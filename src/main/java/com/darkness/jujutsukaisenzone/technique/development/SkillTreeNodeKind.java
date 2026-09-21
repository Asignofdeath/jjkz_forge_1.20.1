package com.darkness.jujutsukaisenzone.technique.development;

/**
 * 术式开发树节点类型。
 */
public enum SkillTreeNodeKind
{
    /** 生得术式内的可解锁招式，对应 {@link com.darkness.jujutsukaisenzone.technique.skillset.TechniqueSkill}。 */
    SKILL,
    /** 外部引用节点（如反转术式），状态来自其它玩家数据，不在本树内消耗术式情报解锁。 */
    REFERENCE,
    /** 说明节点：不可解锁，满足可见条件后才出现在开发树中。 */
    INFO
}
