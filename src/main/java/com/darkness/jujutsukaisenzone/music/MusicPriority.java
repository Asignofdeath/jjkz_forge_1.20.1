package com.darkness.jujutsukaisenzone.music;

/**
 * 模组 BGM 优先级：{@link #STORY} &gt; {@link #COMBAT} &gt; {@link #APPRECIATION}。
 * 同级可替换；低级请求在高级播放中会被丢弃。
 */
public enum MusicPriority
{
    APPRECIATION(0),
    COMBAT(1),
    STORY(2);

    private final int rank;

    MusicPriority(int rank)
    {
        this.rank = rank;
    }

    public int rank()
    {
        return this.rank;
    }

    /** 是否允许抢占 / 替换当前优先级（同级可替换）。 */
    public boolean canOverride(MusicPriority current)
    {
        return current == null || this.rank >= current.rank;
    }

    public static MusicPriority fromOrdinalSafe(int ordinal)
    {
        MusicPriority[] values = values();
        if (ordinal < 0 || ordinal >= values.length)
        {
            return APPRECIATION;
        }
        return values[ordinal];
    }
}
