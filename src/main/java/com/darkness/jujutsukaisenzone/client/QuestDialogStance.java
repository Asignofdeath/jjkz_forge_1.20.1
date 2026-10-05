package com.darkness.jujutsukaisenzone.client;

/**
 * 对白角色名相对主角的态度染色（电影黑幕 / COD 字幕共用）。
 */
public enum QuestDialogStance
{
    /** 友好：淡蓝。 */
    FRIENDLY(0x7EC8E3),
    /** 中立：淡灰。 */
    NEUTRAL(0xB0B0B0),
    /** 敌对：红。 */
    HOSTILE(0xE74C3C);

    private final int rgb;

    QuestDialogStance(int rgb)
    {
        this.rgb = rgb & 0xFFFFFF;
    }

    public int rgb()
    {
        return this.rgb;
    }
}
