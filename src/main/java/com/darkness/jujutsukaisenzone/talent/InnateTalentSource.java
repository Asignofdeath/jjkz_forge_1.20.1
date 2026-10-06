package com.darkness.jujutsukaisenzone.talent;

/**
 * 天赋来源：首次进世界抽中为生得；吃咒力玉石后一律后天。空槽不显示。
 */
public enum InnateTalentSource
{
    NONE,
    INNATE,
    ACQUIRED;

    public static InnateTalentSource byName(String raw)
    {
        if (raw == null || raw.isBlank())
        {
            return NONE;
        }
        try
        {
            return InnateTalentSource.valueOf(raw);
        }
        catch (IllegalArgumentException ignored)
        {
            return NONE;
        }
    }
}
