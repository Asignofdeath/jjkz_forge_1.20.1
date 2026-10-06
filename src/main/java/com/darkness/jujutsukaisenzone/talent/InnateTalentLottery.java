package com.darkness.jujutsukaisenzone.talent;

import com.darkness.jujutsukaisenzone.player.PlayerCursedEnergyData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;

/**
 * 首次初始化咒力时抽天赋。旧档已初始化咒力的不补抽。
 */
public final class InnateTalentLottery
{
    private InnateTalentLottery()
    {
    }

    public static void rollOnFirstEnergyInit(ServerPlayer player, int cursedEnergyBaseMax)
    {
        if (cursedEnergyBaseMax < PlayerCursedEnergyData.HEAVENLY_RESTRICTION_THRESHOLD)
        {
            InnateTalentData.set(player, InnateTalent.HEAVENLY_RESTRICTION, InnateTalentSource.INNATE);
            return;
        }
        RandomSource random = player.getRandom();
        int roll = random.nextInt(100);
        InnateTalent picked;
        if (roll < 3)
        {
            picked = InnateTalent.SIX_EYES;
        }
        else if (roll < 13)
        {
            picked = InnateTalent.IRON_BODY;
        }
        else if (roll < 23)
        {
            picked = InnateTalent.GENIUS;
        }
        else if (roll < 33)
        {
            picked = InnateTalent.AGILE;
        }
        else
        {
            picked = null;
        }
        InnateTalentData.set(player, picked, picked == null ? InnateTalentSource.NONE : InnateTalentSource.INNATE);
    }

    public static void grantGeniusCommonsIfNeeded(Player player)
    {
        if (!(player instanceof ServerPlayer serverPlayer) || !InnateTalentData.is(serverPlayer, InnateTalent.GENIUS))
        {
            return;
        }
        InnateTalentGeniusGrants.applyOnTechniqueAwaken(serverPlayer);
    }
}
