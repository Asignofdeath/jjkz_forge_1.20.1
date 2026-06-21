package com.darkness.jujutsukaisenzone.technique;

import com.darkness.jujutsukaisenzone.Config;
import com.darkness.jujutsukaisenzone.player.PlayerBarrierTechniqueData;
import com.darkness.jujutsukaisenzone.player.PlayerBlackFlashData;
import com.darkness.jujutsukaisenzone.player.PlayerDomainExpansionData;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import com.darkness.jujutsukaisenzone.technique.BarrierTechniqueServerLogic;
import com.darkness.jujutsukaisenzone.technique.domain.DomainExpansionRouter;
import com.darkness.jujutsukaisenzone.technique.domain.InnateDomainExperienceManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * 领域展开习得：研读 {@link com.darkness.jujutsukaisenzone.registry.ModItems#DOMAIN_STUDYBOOK} 提升概率，
 * 入睡或残血受击时可能进入「生得领域」演出并解锁领域展开。
 */
public final class DomainExpansionLearnLogic
{
    public static final double BASE_ENTRY_CHANCE = 0.008D;
    public static final double STUDYBOOK_BONUS_PER_USE = 0.03D;
    public static final int MAX_STUDYBOOK_USES = 3;

    private DomainExpansionLearnLogic()
    {
    }

    public static boolean meetsStudybookBarrierPrerequisite(ServerPlayer player)
    {
        return PlayerBarrierTechniqueData.hasLearned(player);
    }

    public static boolean hasInnateDomainEntryOpportunity(ServerPlayer player)
    {
        if (PlayerDomainExpansionData.hasLearned(player))
        {
            return false;
        }
        if (!PlayerBarrierTechniqueData.hasLearned(player))
        {
            return false;
        }
        String technique = PlayerTechniqueData.getInnateTechnique(player);
        return !PlayerTechniqueData.TECHNIQUE_NONE.equals(technique);
    }

    public static double getEntryChance(ServerPlayer player)
    {
        return BASE_ENTRY_CHANCE + PlayerDomainExpansionData.getStudybookBoostUses(player) * STUDYBOOK_BONUS_PER_USE;
    }

    public static boolean canRollEntry(ServerPlayer player)
    {
        if (!hasInnateDomainEntryOpportunity(player))
        {
            return false;
        }
        if (PlayerDomainExpansionData.isInnateDomainSessionActive(player)
                || InnateDomainExperienceManager.isSessionActive(player.getUUID()))
        {
            return false;
        }
        if (DomainExpansionRouter.isPositionInAnyDomain(player.serverLevel(), player.blockPosition()))
        {
            return false;
        }
        if (BarrierTechniqueServerLogic.getInstance().isPositionInBarrier(
                player.serverLevel(), player.blockPosition()))
        {
            return false;
        }
        return true;
    }

    public static void tryEnterOnSleep(ServerPlayer player)
    {
        if (!canRollEntry(player))
        {
            return;
        }
        rollAndMaybeEnter(player);
    }

    public static void tryEnterOnNearDeathDamage(ServerPlayer player, float damageAmount)
    {
        if (damageAmount <= 0.0F || !canRollEntry(player))
        {
            return;
        }
        if (!PlayerBlackFlashData.isLowHp(player, Config.blackFlashLowHpThreshold))
        {
            return;
        }
        rollAndMaybeEnter(player);
    }

    private static void rollAndMaybeEnter(ServerPlayer player)
    {
        if (player.getRandom().nextDouble() >= getEntryChance(player))
        {
            return;
        }
        InnateDomainExperienceManager.start(player);
    }

    /**
     * @return 是否成功消耗一次研读
     */
    public static boolean tryUseStudybook(ServerPlayer player)
    {
        if (!meetsStudybookBarrierPrerequisite(player))
        {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                    "message.jujutsukaisenzone.domain_studybook.unreadable"));
            return false;
        }
        if (PlayerDomainExpansionData.hasLearned(player))
        {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                    "message.jujutsukaisenzone.domain_studybook.already_learned"));
            return false;
        }
        int uses = PlayerDomainExpansionData.getStudybookBoostUses(player);
        if (uses >= MAX_STUDYBOOK_USES)
        {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                    "message.jujutsukaisenzone.domain_studybook.max_uses"));
            return false;
        }
        int newUses = uses + 1;
        PlayerDomainExpansionData.setStudybookBoostUses(player, newUses);
        player.sendSystemMessage(Component.translatable(
                "message.jujutsukaisenzone.domain_studybook.read_" + newUses));
        return true;
    }
}
