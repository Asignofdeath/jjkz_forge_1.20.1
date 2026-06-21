package com.darkness.jujutsukaisenzone.technique;

import com.darkness.jujutsukaisenzone.player.BarrierTechniqueLevel;
import com.darkness.jujutsukaisenzone.player.PlayerBarrierTechniqueData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * 结界术习得：研读三次 {@link com.darkness.jujutsukaisenzone.registry.ModItems#BARRIER_STUDYBOOK} 后，下次起床解锁初习。
 */
public final class BarrierTechniqueLearnLogic
{
    public static final int MAX_STUDYBOOK_READS = 3;

    private BarrierTechniqueLearnLogic()
    {
    }

    /**
     * @return 是否成功消耗一次研读（已习得或已读满等待起床时返回 false）
     */
    public static boolean tryUseStudybook(ServerPlayer player)
    {
        if (PlayerBarrierTechniqueData.hasLearned(player))
        {
            player.sendSystemMessage(Component.translatable("message.jujutsukaisenzone.barrier_studybook.already_learned"));
            return false;
        }
        int reads = PlayerBarrierTechniqueData.getStudybookReadCount(player);
        if (reads >= MAX_STUDYBOOK_READS)
        {
            player.sendSystemMessage(Component.translatable("message.jujutsukaisenzone.barrier_studybook.pending_wake"));
            return false;
        }
        int newReads = reads + 1;
        PlayerBarrierTechniqueData.setStudybookReadCount(player, newReads);
        player.sendSystemMessage(Component.translatable(
                "message.jujutsukaisenzone.barrier_studybook.read_" + newReads));
        if (newReads >= MAX_STUDYBOOK_READS)
        {
            PlayerBarrierTechniqueData.setPendingWakeUnlock(player, true);
        }
        return true;
    }

    public static void tryUnlockOnWakeUp(ServerPlayer player)
    {
        if (!PlayerBarrierTechniqueData.isPendingWakeUnlock(player))
        {
            return;
        }
        PlayerBarrierTechniqueData.setPendingWakeUnlock(player, false);
        if (PlayerBarrierTechniqueData.hasLearned(player))
        {
            return;
        }
        PlayerBarrierTechniqueData.setLevel(player, BarrierTechniqueLevel.BEGINNER);
        player.sendSystemMessage(Component.translatable("message.jujutsukaisenzone.barrier_technique.learned"));
    }
}
