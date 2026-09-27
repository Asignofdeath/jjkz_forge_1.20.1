package com.darkness.jujutsukaisenzone.technique;

import com.darkness.jujutsukaisenzone.player.PlayerMeltdownRepairData;
import com.darkness.jujutsukaisenzone.player.PlayerReverseTechniqueData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * 修复术式熔断自然领悟：须先习得反转术式；在遭受术式熔断时有概率悟通强行修复之法。
 */
public final class MeltdownRepairLearnLogic
{
    /** 技能 ID（通用技能，无等级；供技能树 REFERENCE / UnlockCondition 使用）。 */
    public static final String SKILL_ID = "meltdown_repair";

    /** 遭受熔断时的基础领悟概率（5%）。熔断本身已稀有，高于反转术式的残血触发。 */
    public static final double BASE_LEARN_CHANCE = 0.05D;

    private MeltdownRepairLearnLogic()
    {
    }

    public static boolean meetsPrerequisite(ServerPlayer player)
    {
        return PlayerReverseTechniqueData.hasReverseTechnique(player);
    }

    public static void tryLearnOnMeltdownApplied(ServerPlayer player)
    {
        if (PlayerMeltdownRepairData.hasLearned(player))
        {
            return;
        }
        if (!meetsPrerequisite(player))
        {
            return;
        }
        if (player.getRandom().nextDouble() >= BASE_LEARN_CHANCE)
        {
            return;
        }
        learn(player);
    }

    public static void learn(ServerPlayer player)
    {
        PlayerMeltdownRepairData.setLearned(player, true);
        player.sendSystemMessage(Component.translatable("message.jujutsukaisenzone.meltdown_repair.learned"));
    }
}
