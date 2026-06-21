package com.darkness.jujutsukaisenzone.technique;

import com.darkness.jujutsukaisenzone.player.PlayerSeanceTechniqueData;
import com.darkness.jujutsukaisenzone.player.SeanceTechniqueLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * 降灵术习得：在地狱研读三次 {@link com.darkness.jujutsukaisenzone.registry.ModItems#SOUL_STUDYBOOK} 后解锁初习。
 */
public final class SeanceTechniqueLearnLogic
{
    public static final int MAX_STUDYBOOK_READS = 3;

    private SeanceTechniqueLearnLogic()
    {
    }

    public static boolean isInNether(ServerPlayer player)
    {
        return player.serverLevel().dimension() == Level.NETHER;
    }

    /**
     * @return 是否成功消耗一次研读
     */
    public static boolean tryUseStudybook(ServerPlayer player)
    {
        if (!isInNether(player))
        {
            player.sendSystemMessage(Component.translatable(
                    "message.jujutsukaisenzone.soul_studybook.wrong_dimension"));
            return false;
        }
        if (PlayerSeanceTechniqueData.hasLearned(player))
        {
            player.sendSystemMessage(Component.translatable(
                    "message.jujutsukaisenzone.soul_studybook.already_learned"));
            return false;
        }
        int reads = PlayerSeanceTechniqueData.getStudybookReadCount(player);
        if (reads >= MAX_STUDYBOOK_READS)
        {
            player.sendSystemMessage(Component.translatable(
                    "message.jujutsukaisenzone.soul_studybook.max_uses"));
            return false;
        }
        int newReads = reads + 1;
        PlayerSeanceTechniqueData.setStudybookReadCount(player, newReads);
        player.sendSystemMessage(Component.translatable(
                "message.jujutsukaisenzone.soul_studybook.read_" + newReads));
        if (newReads >= MAX_STUDYBOOK_READS)
        {
            learn(player);
        }
        return true;
    }

    private static void learn(ServerPlayer player)
    {
        if (PlayerSeanceTechniqueData.hasLearned(player))
        {
            return;
        }
        PlayerSeanceTechniqueData.setLevel(player, SeanceTechniqueLevel.BEGINNER);
        player.sendSystemMessage(Component.translatable("message.jujutsukaisenzone.seance_technique.learned"));
    }
}
