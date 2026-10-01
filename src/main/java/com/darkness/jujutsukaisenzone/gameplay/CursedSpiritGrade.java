package com.darkness.jujutsukaisenzone.gameplay;

import com.darkness.jujutsukaisenzone.player.PlayerCursedGradeData;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

/**
 * 咒灵战斗用等级（含四级）。与玩家 {@link PlayerCursedGradeData} 的 rank 序号对齐：
 * 四级=1 … 特级=5；特级<strong>不拆档</strong>，同级数值跨度由生命表区间表达。
 */
public enum CursedSpiritGrade
{
    GRADE_4(PlayerCursedGradeData.RANK_GRADE_4, "四级"),
    GRADE_3(PlayerCursedGradeData.RANK_GRADE_3, "三级"),
    GRADE_2(PlayerCursedGradeData.RANK_GRADE_2, "二级"),
    GRADE_1(PlayerCursedGradeData.RANK_GRADE_1, "一级"),
    SPECIAL(PlayerCursedGradeData.RANK_SPECIAL, "特级");

    private final int rank;
    private final String displayName;

    CursedSpiritGrade(int rank, String displayName)
    {
        this.rank = rank;
        this.displayName = displayName;
    }

    /** 与 {@link PlayerCursedGradeData#getRank} 相同的序号（1～5）。 */
    public int rank()
    {
        return rank;
    }

    public String displayName()
    {
        return displayName;
    }

    @Nullable
    public static CursedSpiritGrade fromRank(int rank)
    {
        return switch (rank)
        {
            case PlayerCursedGradeData.RANK_GRADE_4 -> GRADE_4;
            case PlayerCursedGradeData.RANK_GRADE_3 -> GRADE_3;
            case PlayerCursedGradeData.RANK_GRADE_2 -> GRADE_2;
            case PlayerCursedGradeData.RANK_GRADE_1 -> GRADE_1;
            case PlayerCursedGradeData.RANK_SPECIAL -> SPECIAL;
            default -> null;
        };
    }

    /** 天元之楔刷新点等级 → 战斗等级（刷新点无四级）。 */
    public static CursedSpiritGrade fromSpawnGrade(CurseSpiritSpawnGrade spawnGrade)
    {
        return switch (spawnGrade)
        {
            case GRADE_3 -> GRADE_3;
            case GRADE_2 -> GRADE_2;
            case GRADE_1 -> GRADE_1;
            case SPECIAL -> SPECIAL;
        };
    }

    @Nullable
    public static CursedSpiritGrade fromInput(@Nullable String raw)
    {
        if (raw == null || raw.isEmpty())
        {
            return null;
        }
        return switch (raw)
        {
            case "四级", "grade_4", "4" -> GRADE_4;
            case "三级", "grade_3", "3" -> GRADE_3;
            case "二级", "grade_2", "2" -> GRADE_2;
            case "一级", "grade_1", "1" -> GRADE_1;
            case "特级", "special", "0" -> SPECIAL;
            default -> null;
        };
    }
}
