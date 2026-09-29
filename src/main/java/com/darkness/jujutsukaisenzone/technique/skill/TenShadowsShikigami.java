package com.darkness.jujutsukaisenzone.technique.skill;

import com.darkness.jujutsukaisenzone.registry.ModEntities;
import net.minecraft.world.entity.EntityType;
import org.lwjgl.glfw.GLFW;

/**
 * 十种影法术式神槽位：数字行 1～0 ↔ 索引 0～9。
 * <p>
 * {@link #placeholderType()} 为召出实体类型；未换模的式神仍用原版占位。
 * 玉犬一对共用键 1；虎葬占键 9。
 */
public enum TenShadowsShikigami
{
    DIVINE_DOGS(0, GLFW.GLFW_KEY_1, "divine_dogs", "玉犬", EntityType.WOLF),
    NUE(1, GLFW.GLFW_KEY_2, "nue", "鵺", EntityType.PHANTOM),
    TOAD(2, GLFW.GLFW_KEY_3, "toad", "蝦蟇", EntityType.FROG),
    GREAT_SERPENT(3, GLFW.GLFW_KEY_4, "great_serpent", "大蛇", EntityType.CAVE_SPIDER),
    MAX_ELEPHANT(4, GLFW.GLFW_KEY_5, "max_elephant", "满象", EntityType.RAVAGER),
    RABBIT_ESCAPE(5, GLFW.GLFW_KEY_6, "rabbit_escape", "脱兔", EntityType.RABBIT),
    ROUND_DEER(6, GLFW.GLFW_KEY_7, "round_deer", "圆鹿", EntityType.GOAT),
    PIERCING_OX(7, GLFW.GLFW_KEY_8, "piercing_ox", "贯牛", EntityType.HOGLIN),
    TIGER_FUNERAL(8, GLFW.GLFW_KEY_9, "tiger_funeral", "虎葬", EntityType.POLAR_BEAR),
    MAHORAGA(9, GLFW.GLFW_KEY_0, "mahoraga", "魔虚罗", EntityType.WARDEN);

    private final int index;
    private final int glfwKey;
    private final String id;
    private final String displayName;
    private final EntityType<?> placeholderType;

    TenShadowsShikigami(int index, int glfwKey, String id, String displayName, EntityType<?> placeholderType)
    {
        this.index = index;
        this.glfwKey = glfwKey;
        this.id = id;
        this.displayName = displayName;
        this.placeholderType = placeholderType;
    }

    public int index()
    {
        return index;
    }

    public int glfwKey()
    {
        return glfwKey;
    }

    /** 数据包 JSON 键（如 {@code divine_dogs}）。 */
    public String id()
    {
        return id;
    }

    public String displayName()
    {
        return displayName;
    }

    public EntityType<?> placeholderType()
    {
        if (this == DIVINE_DOGS)
        {
            return ModEntities.JADE_DOG.get();
        }
        if (this == MAHORAGA)
        {
            return ModEntities.MAHORAGA.get();
        }
        return placeholderType;
    }

    public static TenShadowsShikigami byId(String id)
    {
        if (id == null || id.isEmpty())
        {
            return null;
        }
        for (TenShadowsShikigami shikigami : values())
        {
            if (shikigami.id.equals(id))
            {
                return shikigami;
            }
        }
        return null;
    }

    /** 一次召出数量：玉犬一对；脱兔群；其余单体。 */
    public int spawnCount()
    {
        return switch (this)
        {
            case DIVINE_DOGS -> 2;
            case RABBIT_ESCAPE -> 8;
            default -> 1;
        };
    }

    public static TenShadowsShikigami byIndex(int index)
    {
        for (TenShadowsShikigami shikigami : values())
        {
            if (shikigami.index == index)
            {
                return shikigami;
            }
        }
        return null;
    }
}
