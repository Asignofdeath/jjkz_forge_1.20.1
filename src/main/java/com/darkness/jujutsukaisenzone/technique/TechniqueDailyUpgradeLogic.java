package com.darkness.jujutsukaisenzone.technique;

import com.darkness.jujutsukaisenzone.player.BarrierTechniqueLevel;
import com.darkness.jujutsukaisenzone.player.PlayerBarrierTechniqueData;
import com.darkness.jujutsukaisenzone.player.PlayerBrainDamageData;
import com.darkness.jujutsukaisenzone.player.PlayerSeanceTechniqueData;
import com.darkness.jujutsukaisenzone.player.PlayerStrengthenTechniqueData;
import com.darkness.jujutsukaisenzone.player.SeanceTechniqueLevel;
import com.darkness.jujutsukaisenzone.player.StrengthenTechniqueLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * 结界术 / 降灵术 / 强化术：每次使用 +1% 日终升级概率（上限 8%），世界自然过日后 Roll 升级并清零。
 */
public final class TechniqueDailyUpgradeLogic
{
    public static final int CHANCE_PER_USE_PERCENT = 1;
    public static final int MAX_UPGRADE_CHANCE_PERCENT = 8;

    private TechniqueDailyUpgradeLogic()
    {
    }

    public static void onBarrierTechniqueUsed(ServerPlayer player)
    {
        if (!PlayerBarrierTechniqueData.hasLearned(player))
        {
            return;
        }
        incrementBarrierUpgradeChance(player);
    }

    public static void onSeanceTechniqueUsed(ServerPlayer player)
    {
        if (!PlayerSeanceTechniqueData.hasLearned(player))
        {
            return;
        }
        incrementSeanceUpgradeChance(player);
    }

    public static void onStrengthenTechniqueUsed(ServerPlayer player)
    {
        if (!PlayerStrengthenTechniqueData.hasLearned(player))
        {
            return;
        }
        incrementStrengthenUpgradeChance(player);
    }

    public static void checkGameDayEnd(ServerPlayer player)
    {
        long currentDay = player.server.overworld().getDayTime() / 24000L;
        long lastDay = PlayerBarrierTechniqueData.getLastProcessedGameDay(player);
        if (lastDay < 0L)
        {
            PlayerBarrierTechniqueData.setLastProcessedGameDay(player, currentDay);
            return;
        }
        if (currentDay <= lastDay)
        {
            return;
        }

        resolveBarrierDayEnd(player);
        resolveSeanceDayEnd(player);
        resolveStrengthenDayEnd(player);
        PlayerBrainDamageData.recoverForDaysPassed(player, currentDay - lastDay);
        com.darkness.jujutsukaisenzone.player.SpNodeShelfLogic.onDayEnd(player);
        PlayerBarrierTechniqueData.setLastProcessedGameDay(player, currentDay);
    }

    private static void incrementStrengthenUpgradeChance(ServerPlayer player)
    {
        if (PlayerStrengthenTechniqueData.getLevel(player) == StrengthenTechniqueLevel.COMPLETE)
        {
            return;
        }
        int current = PlayerStrengthenTechniqueData.getUpgradeChancePercent(player);
        if (current >= MAX_UPGRADE_CHANCE_PERCENT)
        {
            return;
        }
        PlayerStrengthenTechniqueData.setUpgradeChancePercent(player,
                Math.min(MAX_UPGRADE_CHANCE_PERCENT,
                        current + com.darkness.jujutsukaisenzone.talent.InnateTalentData.geniusDailyUpgradeGain(
                                player, CHANCE_PER_USE_PERCENT)));
    }

    private static void resolveStrengthenDayEnd(ServerPlayer player)
    {
        int chance = PlayerStrengthenTechniqueData.getUpgradeChancePercent(player);
        PlayerStrengthenTechniqueData.setUpgradeChancePercent(player, 0);
        if (chance <= 0 || !PlayerStrengthenTechniqueData.hasLearned(player))
        {
            return;
        }
        if (player.getRandom().nextInt(100) >= chance)
        {
            return;
        }
        StrengthenTechniqueLevel next = nextStrengthenLevel(PlayerStrengthenTechniqueData.getLevel(player));
        if (next == null)
        {
            return;
        }
        PlayerStrengthenTechniqueData.setLevel(player, next);
        player.sendSystemMessage(Component.translatable(
                "message.jujutsukaisenzone.strengthen_technique.upgraded",
                getStrengthenLevelDisplayName(next)));
    }

    private static StrengthenTechniqueLevel nextStrengthenLevel(StrengthenTechniqueLevel current)
    {
        return switch (current)
        {
            case BEGINNER -> StrengthenTechniqueLevel.MASTERED;
            case MASTERED -> StrengthenTechniqueLevel.COMPLETE;
            default -> null;
        };
    }

    private static String getStrengthenLevelDisplayName(StrengthenTechniqueLevel level)
    {
        return switch (level)
        {
            case BEGINNER -> "初习";
            case MASTERED -> "掌握";
            case COMPLETE -> "完全掌握";
            default -> level.name();
        };
    }

    private static void incrementBarrierUpgradeChance(ServerPlayer player)
    {
        if (PlayerBarrierTechniqueData.getLevel(player) == BarrierTechniqueLevel.COMPLETE)
        {
            return;
        }
        int current = PlayerBarrierTechniqueData.getUpgradeChancePercent(player);
        if (current >= MAX_UPGRADE_CHANCE_PERCENT)
        {
            return;
        }
        PlayerBarrierTechniqueData.setUpgradeChancePercent(player,
                Math.min(MAX_UPGRADE_CHANCE_PERCENT,
                        current + com.darkness.jujutsukaisenzone.talent.InnateTalentData.geniusDailyUpgradeGain(
                                player, CHANCE_PER_USE_PERCENT)));
    }

    private static void incrementSeanceUpgradeChance(ServerPlayer player)
    {
        if (PlayerSeanceTechniqueData.getLevel(player) == SeanceTechniqueLevel.COMPLETE)
        {
            return;
        }
        int current = PlayerSeanceTechniqueData.getUpgradeChancePercent(player);
        if (current >= MAX_UPGRADE_CHANCE_PERCENT)
        {
            return;
        }
        PlayerSeanceTechniqueData.setUpgradeChancePercent(player,
                Math.min(MAX_UPGRADE_CHANCE_PERCENT,
                        current + com.darkness.jujutsukaisenzone.talent.InnateTalentData.geniusDailyUpgradeGain(
                                player, CHANCE_PER_USE_PERCENT)));
    }

    private static void resolveBarrierDayEnd(ServerPlayer player)
    {
        int chance = PlayerBarrierTechniqueData.getUpgradeChancePercent(player);
        PlayerBarrierTechniqueData.setUpgradeChancePercent(player, 0);
        if (chance <= 0 || !PlayerBarrierTechniqueData.hasLearned(player))
        {
            return;
        }
        if (player.getRandom().nextInt(100) >= chance)
        {
            return;
        }
        BarrierTechniqueLevel next = nextBarrierLevel(PlayerBarrierTechniqueData.getLevel(player));
        if (next == null)
        {
            return;
        }
        PlayerBarrierTechniqueData.setLevel(player, next);
        player.sendSystemMessage(Component.translatable(
                "message.jujutsukaisenzone.barrier_technique.upgraded",
                getBarrierLevelDisplayName(next)));
    }

    private static void resolveSeanceDayEnd(ServerPlayer player)
    {
        int chance = PlayerSeanceTechniqueData.getUpgradeChancePercent(player);
        PlayerSeanceTechniqueData.setUpgradeChancePercent(player, 0);
        if (chance <= 0 || !PlayerSeanceTechniqueData.hasLearned(player))
        {
            return;
        }
        if (player.getRandom().nextInt(100) >= chance)
        {
            return;
        }
        SeanceTechniqueLevel next = nextSeanceLevel(PlayerSeanceTechniqueData.getLevel(player));
        if (next == null)
        {
            return;
        }
        PlayerSeanceTechniqueData.setLevel(player, next);
        player.sendSystemMessage(Component.translatable(
                "message.jujutsukaisenzone.seance_technique.upgraded",
                getSeanceLevelDisplayName(next)));
    }

    private static BarrierTechniqueLevel nextBarrierLevel(BarrierTechniqueLevel current)
    {
        return switch (current)
        {
            case BEGINNER -> BarrierTechniqueLevel.MASTERED;
            case MASTERED -> BarrierTechniqueLevel.COMPLETE;
            default -> null;
        };
    }

    private static SeanceTechniqueLevel nextSeanceLevel(SeanceTechniqueLevel current)
    {
        return switch (current)
        {
            case BEGINNER -> SeanceTechniqueLevel.MASTERED;
            case MASTERED -> SeanceTechniqueLevel.COMPLETE;
            default -> null;
        };
    }

    private static String getBarrierLevelDisplayName(BarrierTechniqueLevel level)
    {
        return switch (level)
        {
            case BEGINNER -> "初习";
            case MASTERED -> "掌握";
            case COMPLETE -> "完全掌握";
            default -> level.name();
        };
    }

    private static String getSeanceLevelDisplayName(SeanceTechniqueLevel level)
    {
        return switch (level)
        {
            case BEGINNER -> "初习";
            case MASTERED -> "掌握";
            case COMPLETE -> "完全掌握";
            default -> level.name();
        };
    }
}
