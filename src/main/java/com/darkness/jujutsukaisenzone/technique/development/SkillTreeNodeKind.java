package com.darkness.jujutsukaisenzone.technique.development;

/**
 * 术式开发树节点类型。
 */
public enum SkillTreeNodeKind
{
    /** 生得术式内的可解锁招式，对应 {@link com.darkness.jujutsukaisenzone.technique.skillset.TechniqueSkill}。 */
    SKILL,
    /**
     * 特别招式节点：由「修饰键 + 技能键」触发、依附在某个 {@link #SKILL} 节点上的派生招式。
     * <p>
     * 它没有自己的基础字段，等级 / 咒力 / 冷却 / 解锁状态一律跟随原技能，
     * 因此**不可在本树内单独解锁**，节点仅用于在开发界面中展示「原技能还有哪些变体」。
     */
    SPECIAL_MOVE,
    /** 外部引用节点（如反转术式），状态来自其它玩家数据，不在本树内消耗术式情报解锁。 */
    REFERENCE,
    /** 说明节点：不可解锁，满足可见条件后才出现在开发树中。 */
    INFO
}
