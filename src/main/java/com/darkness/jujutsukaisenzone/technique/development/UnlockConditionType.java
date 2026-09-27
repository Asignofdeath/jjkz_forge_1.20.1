package com.darkness.jujutsukaisenzone.technique.development;

/**
 * 解锁条件类型（可序列化标识）。
 */
public enum UnlockConditionType
{
    HAS_REVERSE_TECHNIQUE,
    HAS_COMMON_SKILL,
    MIN_CURSED_RANK,
    /** 已习得领域展开（「生得领域」演出完成）。 */
    HAS_DOMAIN_EXPANSION,
    /** 已习得修复术式熔断（通用技能，无等级）。 */
    HAS_MELTDOWN_REPAIR,
    /** 说明节点默认可见（绑定该术式后即可显示）。 */
    ALWAYS_VISIBLE
}
