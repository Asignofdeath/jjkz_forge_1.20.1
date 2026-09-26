package com.darkness.jujutsukaisenzone.technique.shikigami;

/**
 * 式神模式招式的释放方。
 * <ul>
 *   <li>{@link #ACTIVE}：仅术师（修饰键 + 式神槽）</li>
 *   <li>{@link #AUTO}：仅场上式神 AI</li>
 *   <li>{@link #BOTH}：两边均可调用同一套结算</li>
 * </ul>
 */
public enum ShikigamiCastMode
{
    ACTIVE,
    AUTO,
    BOTH;

    public boolean allowsPlayer()
    {
        return this == ACTIVE || this == BOTH;
    }

    public boolean allowsAuto()
    {
        return this == AUTO || this == BOTH;
    }
}
