package com.darkness.jujutsukaisenzone.technique;

import com.darkness.jujutsukaisenzone.Config;
import com.darkness.jujutsukaisenzone.player.PlayerBlackFlashData;
import com.darkness.jujutsukaisenzone.player.PlayerCursedEnergyData;
import com.darkness.jujutsukaisenzone.player.PlayerReverseTechniqueData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * 反转术式自然领悟：残血受击概率触发；{@link com.darkness.jujutsukaisenzone.registry.ModItems#REVERSE_STUDYBOOK} 可提升概率。
 */
public final class ReverseTechniqueLearnLogic
{
    /** 咒力总量须严格大于该值（即 ≥ 1001）。 */
    public static final int MIN_MAX_CURSED_ENERGY_EXCLUSIVE = 1000;
    /** 基础领悟概率（0.8%）。 */
    public static final double BASE_LEARN_CHANCE = 0.008D;
    /** 每次研读理论书额外概率（2%）。 */
    public static final double STUDYBOOK_BONUS_PER_USE = 0.02D;
    /** 理论书最多有效研读次数（累计 +6%）。 */
    public static final int MAX_STUDYBOOK_USES = 3;

    private ReverseTechniqueLearnLogic()
    {
    }

    public static boolean meetsCursedEnergyPrerequisite(Player player)
    {
        return PlayerCursedEnergyData.getMax(player) > MIN_MAX_CURSED_ENERGY_EXCLUSIVE;
    }

    public static int getStudybookBoostUses(Player player)
    {
        if (!(player instanceof ServerPlayer serverPlayer))
        {
            return 0;
        }
        return PlayerReverseTechniqueData.getStudybookBoostUses(serverPlayer);
    }

    public static double getLearnChance(Player player)
    {
        return com.darkness.jujutsukaisenzone.talent.InnateTalentData.geniusLearnChance(
                player, BASE_LEARN_CHANCE + getStudybookBoostUses(player) * STUDYBOOK_BONUS_PER_USE);
    }

    public static void tryLearnFromDamage(ServerPlayer player, float damageAmount)
    {
        if (damageAmount <= 0.0F)
        {
            return;
        }
        if (PlayerReverseTechniqueData.hasReverseTechnique(player))
        {
            return;
        }
        if (!meetsCursedEnergyPrerequisite(player))
        {
            return;
        }
        if (!PlayerBlackFlashData.isLowHp(player, Config.blackFlashLowHpThreshold))
        {
            return;
        }
        if (player.getRandom().nextDouble() >= getLearnChance(player))
        {
            return;
        }
        learn(player);
    }

    public static void learn(ServerPlayer player)
    {
        PlayerReverseTechniqueData.setLearned(player, true);
        player.sendSystemMessage(Component.translatable("message.jujutsukaisenzone.reverse_technique.learned"));
    }

    /**
     * @return 是否成功消耗一次研读（已习得或已达上限时返回 false）
     */
    public static boolean tryUseStudybook(ServerPlayer player)
    {
        if (PlayerReverseTechniqueData.hasReverseTechnique(player))
        {
            player.sendSystemMessage(Component.translatable("message.jujutsukaisenzone.reverse_studybook.already_learned"));
            return false;
        }
        int uses = PlayerReverseTechniqueData.getStudybookBoostUses(player);
        if (uses >= MAX_STUDYBOOK_USES)
        {
            player.sendSystemMessage(Component.translatable("message.jujutsukaisenzone.reverse_studybook.max_uses"));
            return false;
        }
        int newUses = uses + 1;
        PlayerReverseTechniqueData.setStudybookBoostUses(player, newUses);
        player.sendSystemMessage(Component.translatable(
                "message.jujutsukaisenzone.reverse_studybook.read_" + newUses));
        return true;
    }
}
