package com.darkness.jujutsukaisenzone.technique.skill;

import com.darkness.jujutsukaisenzone.player.PlayerRatioOvertimeData;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import com.darkness.jujutsukaisenzone.technique.development.SkillUnlockLogic;
import com.darkness.jujutsukaisenzone.technique.skillset.RatioTechniqueSkillSet;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 十划加班作息：上班（日间）削弱 / 加班（夜间）强化；违约后当日两边都不生效。
 */
public final class RatioOvertimeLogic
{
    /** 日时 0～12000 为上班，其后为加班（含黄昏夜凌晨）。 */
    public static final long WORK_DAYTIME_END = 12000L;

    /** 遵守束缚时：上班输出乘区。 */
    public static final float WORK_DAMAGE_MULT = 0.95F;
    /** 遵守束缚时：加班输出乘区。 */
    public static final float OVERTIME_DAMAGE_MULT = 1.10F;

    private RatioOvertimeLogic()
    {
    }

    public static boolean hasVowUnlocked(Player player)
    {
        return SkillUnlockLogic.isSkillUnlockedById(
                player, PlayerTechniqueData.TECHNIQUE_RATIO, RatioTechniqueSkillSet.SKILL_OVERTIME_VOW);
    }

    public static boolean isScheduleActive(Player player)
    {
        return PlayerTechniqueData.isRatio(player)
                && hasVowUnlocked(player)
                && !PlayerRatioOvertimeData.isVowBroken(player);
    }

    public static boolean isWorkHours(Level level)
    {
        long t = level.getDayTime() % 24000L;
        if (t < 0L)
        {
            t += 24000L;
        }
        return t < WORK_DAYTIME_END;
    }

    public static boolean isOvertimeHours(Level level)
    {
        return !isWorkHours(level);
    }

    /** 无可谅解是否可用。 */
    public static boolean canUseAbsolution(Player player)
    {
        return isScheduleActive(player) && isOvertimeHours(player.level());
    }

    public static float outgoingDamageMultiplier(Player attacker)
    {
        if (!isScheduleActive(attacker))
        {
            return 1.0F;
        }
        if (isWorkHours(attacker.level()))
        {
            return WORK_DAMAGE_MULT;
        }
        return OVERTIME_DAMAGE_MULT;
    }
}
