package com.darkness.jujutsukaisenzone.gameplay;

import net.minecraft.core.particles.DustParticleOptions;
import org.joml.Vector3f;

import javax.annotation.Nullable;

/**
 * 天元之楔结界内咒灵刷新点等级。
 */
public enum CurseSpiritSpawnGrade
{
    GRADE_3("三级", new Vector3f(1.0F, 1.0F, 1.0F)),
    GRADE_2("二级", new Vector3f(0.72F, 0.2F, 1.0F)),
    GRADE_1("一级", new Vector3f(1.0F, 0.55F, 0.1F)),
    SPECIAL("特级", new Vector3f(1.0F, 0.15F, 0.15F));

    private static final float DUST_SCALE = 1.15F;

    private final String displayName;
    private final Vector3f color;

    CurseSpiritSpawnGrade(String displayName, Vector3f color)
    {
        this.displayName = displayName;
        this.color = color;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public DustParticleOptions dustParticle()
    {
        return new DustParticleOptions(color, DUST_SCALE);
    }

    @Nullable
    public static CurseSpiritSpawnGrade fromInput(@Nullable String raw)
    {
        if (raw == null || raw.isEmpty())
        {
            return null;
        }
        return switch (raw)
        {
            case "三级", "grade_3", "3" -> GRADE_3;
            case "二级", "grade_2", "2" -> GRADE_2;
            case "一级", "grade_1", "1" -> GRADE_1;
            case "特级", "special", "0" -> SPECIAL;
            default -> null;
        };
    }
}
