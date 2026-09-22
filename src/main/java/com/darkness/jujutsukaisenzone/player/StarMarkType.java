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

    /** 两颗星是否跨序（跨序之间无法靠近，这是术式的核心规则）。 */
    public boolean isNonAdjacentTo(StarMarkType other)
    {
        return other != null && this != other && Math.abs(ordinal() - other.ordinal()) >= 2;
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
