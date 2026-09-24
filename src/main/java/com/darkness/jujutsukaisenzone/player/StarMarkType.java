package com.darkness.jujutsukaisenzone.player;

/**
 * 「星星标记」的五种标记，取自南十字座五星。
 * <p>
 * 枚举顺序即原作顺序（Imai → Acrux → Mimosa → Ginan → Gacrux），因此相邻判定就是
 * {@code Math.abs(ordinal 差) == 1}，见 {@link #isAdjacentTo(StarMarkType)}。
 * <p>
 * 「唯一」约束落在目标身上而不是星身上：同一目标至多一个标记，而同一颗星可以打给多个目标。
 * 切换「当前标记」在战斗模式下走中键调节模式（见 {@code TechniqueSkillKeyEvents}）。
 */
public enum StarMarkType
{
    IMAI("imai", "Imai", "薄荷青", 0x5FE8D5),
    ACRUX("acrux", "Acrux", "樱粉", 0xFFB4CE),
    MIMOSA("mimosa", "Mimosa", "柠檬黄", 0xF5E36B),
    GINAN("ginan", "Ginan", "薰衣草紫", 0xC0A8FF),
    GACRUX("gacrux", "Gacrux", "冰蓝", 0x9FDDFF);

    private static final StarMarkType[] VALUES = values();

    private final String id;
    private final String starName;
    private final String colorName;
    private final int rgb;

    StarMarkType(String id, String starName, String colorName, int rgb)
    {
        this.id = id;
        this.starName = starName;
        this.colorName = colorName;
        this.rgb = rgb & 0xFFFFFF;
    }

    /** 存档中使用的稳定 id。 */
    public String id()
    {
        return id;
    }

    public String starName()
    {
        return starName;
    }

    public String colorName()
    {
        return colorName;
    }

    public int rgb()
    {
        return rgb;
    }

    /** 展示名，如 {@code ★Imai 薄荷青}。 */
    public String displayName()
    {
        return "★" + starName + " " + colorName;
    }

    public float red()
    {
        return ((rgb >> 16) & 0xFF) / 255.0F;
    }

    public float green()
    {
        return ((rgb >> 8) & 0xFF) / 255.0F;
    }

    public float blue()
    {
        return (rgb & 0xFF) / 255.0F;
    }

    /** 两颗星在顺序上是否相邻（相邻可以自由靠近）。 */
    public boolean isAdjacentTo(StarMarkType other)
    {
        return other != null && Math.abs(ordinal() - other.ordinal()) == 1;
    }

    // ======================== 解锁位图（跨序的「经过」状态） ========================

    /**
     * 位图中是否包含这颗星，即「携带方能否接近它」。
     * <p>
     * 位图的第 {@code i} 位对应 {@code ordinal()} 为 {@code i} 的星。
     * 位图本身不存在标记上，而是存在星域实例里、按标记索引，见 {@code StarMarkRules}。
     */
    public boolean isUnlockedIn(int mask)
    {
        return (mask & (1 << ordinal())) != 0;
    }

    /**
     * 初始解锁位图：自己的星，加上顺序上相邻的星。
     * <p>
     * 于是「本轮星域展开刚开始时」天然可以接近相邻星，却接近不了任何跨序星——
     * 后者必须靠 {@link #unlock(int, StarMarkType)} 逐级解锁。
     */
    public int initialUnlockMask()
    {
        return unlock(0, this);
    }

    /**
     * 把某颗星连同它顺序上相邻的两颗并入位图（链的两端只有一侧相邻）。
     * <p>
     * 这就是「经过」的结算：接触一颗星就解锁它和它的邻居，因此整条链只能逐级推进；
     * 反过来，同一颗星自己的相邻关系本来就已包含在初始位图里，所以同星之间的接触是空操作。
     * <p>
     * 注意这里只是「并入」的算法本身——真正吃接触的只有<b>本来就够得着</b>的星（被排斥的星硬挤进去
     * 不算经过），而且一轮星域实例内每颗星只授予一次、过期不补发，两条都由 {@code StarMarkRules} 把关。
     * <p>
     * 位图只在<b>单次星域展开</b>内累积：展开时清零（{@code StarMarkRules#activateGate}），
     * 所以这里的「只增不减」是相对一轮而言，而不是相对标记的整个生命周期。
     */
    public static int unlock(int mask, StarMarkType type)
    {
        int ordinal = type.ordinal();
        int next = mask | (1 << ordinal);
        if (ordinal > 0)
        {
            next |= 1 << (ordinal - 1);
        }
        if (ordinal + 1 < VALUES.length)
        {
            next |= 1 << (ordinal + 1);
        }
        return next;
    }

    /** 按 id 查标记；未知 id 返回 {@link #IMAI}。 */
    public static StarMarkType byId(String id)
    {
        if (id != null)
        {
            for (StarMarkType type : VALUES)
            {
                if (type.id.equals(id))
                {
                    return type;
                }
            }
        }
        return IMAI;
    }

    /** 切换当前标记：delta 为正表示上一个，为负表示下一个；循环。 */
    public static StarMarkType cycle(int currentIndex, int delta)
    {
        int size = VALUES.length;
        int next = ((currentIndex + delta) % size + size) % size;
        return VALUES[next];
    }

    public static int indexOf(StarMarkType type)
    {
        return type == null ? 0 : type.ordinal();
    }

    public static int count()
    {
        return VALUES.length;
    }
}
