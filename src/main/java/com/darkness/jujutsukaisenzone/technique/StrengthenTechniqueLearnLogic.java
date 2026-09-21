package com.darkness.jujutsukaisenzone.technique;

import com.darkness.jujutsukaisenzone.player.PlayerCursedGradeData;
import com.darkness.jujutsukaisenzone.player.PlayerStrengthenTechniqueData;
import com.darkness.jujutsukaisenzone.player.StrengthenTechniqueLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * 强化术习得：真实咒术等级达到二级时自动解锁初习。
 */
public final class StrengthenTechniqueLearnLogic
{
    private StrengthenTechniqueLearnLogic()
    {
    }

    public static void tryUnlockForGrade(ServerPlayer player)
    {
        if (PlayerStrengthenTechniqueData.hasLearned(player))
        {
            return;
        }
        if (PlayerCursedGradeData.getRawRank(player) < PlayerCursedGradeData.RANK_GRADE_2)
        {
            return;
        }

        PlayerStrengthenTechniqueData.setLevel(player, StrengthenTechniqueLevel.BEGINNER);
        player.sendSystemMessage(Component.translatable("message.jujutsukaisenzone.strengthen_technique.learned"));
    }
}
