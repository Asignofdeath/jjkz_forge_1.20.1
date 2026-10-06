package com.darkness.jujutsukaisenzone.talent;

import com.darkness.jujutsukaisenzone.player.BarrierTechniqueLevel;
import com.darkness.jujutsukaisenzone.player.PlayerBarrierTechniqueData;
import com.darkness.jujutsukaisenzone.player.PlayerDomainExpansionData;
import net.minecraft.server.level.ServerPlayer;

public final class InnateTalentGeniusGrants
{
    private InnateTalentGeniusGrants()
    {
    }

    public static void applyOnTechniqueAwaken(ServerPlayer player)
    {
        if (!PlayerBarrierTechniqueData.hasLearned(player))
        {
            PlayerBarrierTechniqueData.setLevel(player, BarrierTechniqueLevel.BEGINNER);
        }
        if (!PlayerDomainExpansionData.hasLearned(player))
        {
            PlayerDomainExpansionData.setLearned(player, true);
        }
    }
}
