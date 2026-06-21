package com.darkness.jujutsukaisenzone.gameplay;

/**
 * 天元之楔结界玩法状态。
 */
public enum TengenWedgeBarrierState
{
    /** 结界已构建完成，等待 start。 */
    PREPARING("准备中"),
    /** 玩法已开始。 */
    IN_PROGRESS("进行中");

    private final String displayName;

    TengenWedgeBarrierState(String displayName)
    {
        this.displayName = displayName;
    }

    public String getDisplayName()
    {
        return displayName;
    }
}
