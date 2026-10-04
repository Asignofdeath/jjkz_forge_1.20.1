package com.darkness.jujutsukaisenzone.quest;

/**
 * 主线任务对玩家可见的状态。
 */
public enum MainQuestStatus
{
    /** 前序未完成。 */
    LOCKED,
    /** 已解锁、未完成、未追踪。 */
    AVAILABLE,
    /** 当前追踪中。 */
    TRACKED,
    /** 已完成。 */
    COMPLETED
}
