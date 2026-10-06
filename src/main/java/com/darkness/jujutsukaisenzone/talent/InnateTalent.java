package com.darkness.jujutsukaisenzone.talent;

import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

/**
 * 生得 / 后天天赋。一个实体最多一条。
 */
public enum InnateTalent
{
    HEAVENLY_RESTRICTION("heavenly_restriction", "天与咒缚"),
    SIX_EYES("six_eyes", "六眼"),
    IRON_BODY("iron_body", "铁躯"),
    GENIUS("genius", "天才"),
    AGILE("agile", "矫健");

    private final String id;
    private final String displayName;

    InnateTalent(String id, String displayName)
    {
        this.id = id;
        this.displayName = displayName;
    }

    public String id()
    {
        return id;
    }

    public String displayName()
    {
        return displayName;
    }

    public Component displayComponent()
    {
        return Component.translatable("talent.jujutsukaisenzone." + id);
    }

    @Nullable
    public static InnateTalent byId(@Nullable String id)
    {
        if (id == null || id.isBlank())
        {
            return null;
        }
        for (InnateTalent talent : values())
        {
            if (talent.id.equals(id))
            {
                return talent;
            }
        }
        return null;
    }

    public static InnateTalent pickEqual(RandomSource random, InnateTalent... pool)
    {
        if (pool == null || pool.length == 0)
        {
            return null;
        }
        return pool[random.nextInt(pool.length)];
    }
}
