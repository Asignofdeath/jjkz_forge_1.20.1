package com.darkness.jujutsukaisenzone.player;

import com.darkness.jujutsukaisenzone.advancement.GradeAdvancements;
import com.darkness.jujutsukaisenzone.network.ModNetwork;
import com.darkness.jujutsukaisenzone.technique.StrengthenTechniqueLearnLogic;
import com.darkness.jujutsukaisenzone.player.mimicry.MimicryCharacterRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * 咒术经验与咒术等级（四级→特级）。经验阈值：50 / 500 / 1500 / 2800 / 4000。
 */
public final class PlayerCursedGradeData
{
    private static final String TAG_EXPERIENCE = "jujutsukaisenzone_curse_grade_xp";

    public static final int XP_GRADE_4 = 50;
    public static final int XP_GRADE_3 = 500;
    public static final int XP_GRADE_2 = 1500;
    public static final int XP_GRADE_1 = 2800;
    public static final int XP_SPECIAL = 4000;

    /** 未达四级阈值 */
    public static final int RANK_NONE = 0;
    public static final int RANK_GRADE_4 = 1;
    public static final int RANK_GRADE_3 = 2;
    public static final int RANK_GRADE_2 = 3;
    public static final int RANK_GRADE_1 = 4;
    public static final int RANK_SPECIAL = 5;

    private PlayerCursedGradeData()
    {
    }

    public static int getExperience(Player player)
    {
        if (player instanceof ServerPlayer serverPlayer)
        {
            return Math.max(0, JjkzPlayerDataStorage.get(serverPlayer).getOrCreate(serverPlayer).curseGradeExperience);
        }
        return Math.max(0, player.getPersistentData().getInt(TAG_EXPERIENCE));
    }

    public static void setExperience(Player player, int value)
    {
        int clamped = Math.max(0, value);
        if (player instanceof ServerPlayer serverPlayer)
        {
            JjkzPlayerDataStorage.PlayerDataEntry data = JjkzPlayerDataStorage.get(serverPlayer).getOrCreate(serverPlayer);
            data.curseGradeExperience = clamped;
            JjkzPlayerDataStorage.get(serverPlayer).setDirty();
            ModNetwork.sendCursedGradeSync(serverPlayer, clamped);
            GradeAdvancements.syncForPlayer(serverPlayer);
            CursedGradeBonuses.applyAll(serverPlayer);
            PlayerCursedEnergyData.recalculateAfterGradeChange(serverPlayer);
            PlayerHealthData.syncFromEntity(serverPlayer);
            StrengthenTechniqueLearnLogic.tryUnlockForGrade(serverPlayer);
            com.darkness.jujutsukaisenzone.player.SpNodeShelfLogic.syncWithExperience(serverPlayer);
            com.darkness.jujutsukaisenzone.entity.JadeDogEntity.refreshMaturityForOwner(serverPlayer);
            return;
        }
        player.getPersistentData().putInt(TAG_EXPERIENCE, clamped);
    }

    public static void addExperience(Player player, int delta)
    {
        setExperience(player, getExperience(player) + delta);
    }

    public static void applySyncedExperience(Player player, int value)
    {
        player.getPersistentData().putInt(TAG_EXPERIENCE, Math.max(0, value));
    }

    /**
     * 降灵术拟态五条悟/宿傩期间，若实际等级未达特级，则战斗属性临时按特级计算（咒力总量和回复速率不变）。
     */
    public static int getRank(Player player)
    {
        int raw = getRawRank(player);
        if (raw >= RANK_SPECIAL)
        {
            return raw;
        }
        if (!PlayerMimicryData.isActive(player))
        {
            return raw;
        }
        String characterId = MimicryCharacterRegistry.resolveCharacterId(
                PlayerTechniqueData.getInnateTechnique(player));
        if (MimicryCharacterRegistry.CHARACTER_GOJO.equals(characterId)
                || MimicryCharacterRegistry.CHARACTER_SUKUNA.equals(characterId))
        {
            return RANK_SPECIAL;
        }
        return raw;
    }

    /** 始终返回存储数据对应的真实等级，不受拟态加成影响。咒力总量和回复速率应使用此方法。 */
    public static int getRawRank(Player player)
    {
        return rankFromExperience(getExperience(player));
    }

    public static int rankFromExperience(int xp)
    {
        if (xp >= XP_SPECIAL)
        {
            return RANK_SPECIAL;
        }
        if (xp >= XP_GRADE_1)
        {
            return RANK_GRADE_1;
        }
        if (xp >= XP_GRADE_2)
        {
            return RANK_GRADE_2;
        }
        if (xp >= XP_GRADE_3)
        {
            return RANK_GRADE_3;
        }
        if (xp >= XP_GRADE_4)
        {
            return RANK_GRADE_4;
        }
        return RANK_NONE;
    }

    /** 作弊设级：将经验设为该等级对应阈值（与升级规则一致）。 */
    public static int experienceForCheatRank(String rankLiteral)
    {
        return switch (rankLiteral)
        {
            case "四级", "grade_4" -> XP_GRADE_4;
            case "三级", "grade_3" -> XP_GRADE_3;
            case "二级", "grade_2" -> XP_GRADE_2;
            case "一级", "grade_1" -> XP_GRADE_1;
            case "特级", "special" -> XP_SPECIAL;
            default -> -1;
        };
    }

    public static String getRankDisplayName(int rank)
    {
        return switch (rank)
        {
            case RANK_GRADE_4 -> "四级";
            case RANK_GRADE_3 -> "三级";
            case RANK_GRADE_2 -> "二级";
            case RANK_GRADE_1 -> "一级";
            case RANK_SPECIAL -> "特级";
            default -> "无";
        };
    }
}
