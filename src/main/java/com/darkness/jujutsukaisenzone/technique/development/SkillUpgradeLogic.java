package com.darkness.jujutsukaisenzone.technique.development;

import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import com.darkness.jujutsukaisenzone.technique.development.SkillUpgradeCost.CursedObjectIngredient;
import com.darkness.jujutsukaisenzone.technique.skillset.TechniqueSkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * 主动技能升级：消耗原版经验等级 + 多种咒物，写入 {@code levelTag}。
 */
public final class SkillUpgradeLogic
{
    private SkillUpgradeLogic()
    {
    }

    public static int getSkillLevel(Player player, TechniqueSkill skill)
    {
        if (skill == null)
        {
            return 0;
        }
        String key = skill.levelTag();
        if (key == null || key.isEmpty())
        {
            return 0;
        }
        if (!player.getPersistentData().contains(key))
        {
            return 1;
        }
        return SkillLevelRules.clampLevel(player.getPersistentData().getInt(key));
    }

    public static int getSkillLevel(Player player, String techniqueId, String skillId)
    {
        return SkillUnlockLogic.findSkillDefinition(techniqueId, skillId)
                .map(skill -> getSkillLevel(player, skill))
                .orElse(0);
    }

    /**
     * 相对 1 级的升级档数（Lv1→0，Lv5→4）。非玩家 / 未知技能按 0。
     */
    public static int upgradeSteps(Player player, String techniqueId, String skillId)
    {
        return Math.max(0, getSkillLevel(player, techniqueId, skillId) - 1);
    }

    /**
     * 技能等级伤害倍率（{@link SkillLevelRules#powerMultiplier}）。非玩家按 1.0。
     */
    public static float powerMultiplier(@Nullable LivingEntity owner, String techniqueId, String skillId)
    {
        if (!(owner instanceof Player player))
        {
            return 1.0F;
        }
        return SkillLevelRules.powerMultiplier(getSkillLevel(player, techniqueId, skillId));
    }

    /**
     * {@code 有效技能倍率 = 基倍率 × 等级倍率}，供出伤点在 {@code deal} / Resolver 之前调用。
     */
    public static float scaleRatio(@Nullable LivingEntity owner, String techniqueId, String skillId, float baseRatio)
    {
        return baseRatio * powerMultiplier(owner, techniqueId, skillId);
    }

    public static void setSkillLevel(Player player, TechniqueSkill skill, int level)
    {
        String key = skill.levelTag();
        if (key == null || key.isEmpty())
        {
            return;
        }
        player.getPersistentData().putInt(key, SkillLevelRules.clampLevel(level));
    }

    public static boolean canUpgradeSkill(Player player, String techniqueId, String skillId)
    {
        if (!SkillUpgradeCostRegistry.isUpgradeable(skillId))
        {
            return false;
        }
        Optional<TechniqueSkill> skillOpt = SkillUnlockLogic.findSkillDefinition(techniqueId, skillId);
        if (skillOpt.isEmpty())
        {
            return false;
        }
        TechniqueSkill skill = skillOpt.get();
        if (!SkillUnlockLogic.isSkillUnlocked(player, techniqueId, skillId, skill))
        {
            return false;
        }
        int level = getSkillLevel(player, skill);
        if (SkillLevelRules.isMaxLevel(level))
        {
            return false;
        }
        Optional<SkillUpgradeCost> costOpt = SkillUpgradeCostRegistry.costForNextLevel(skillId, level);
        if (costOpt.isEmpty())
        {
            return false;
        }
        return meetsCost(player, costOpt.get());
    }

    public static SkillUpgradeResult tryUpgradeSkill(ServerPlayer player, String techniqueId, String skillId)
    {
        if (!techniqueId.equals(PlayerTechniqueData.getInnateTechnique(player)))
        {
            return SkillUpgradeResult.WRONG_TECHNIQUE;
        }
        if (!SkillUpgradeCostRegistry.isUpgradeable(skillId))
        {
            return SkillUpgradeResult.NOT_UPGRADEABLE;
        }
        Optional<TechniqueSkill> skillOpt = SkillUnlockLogic.findSkillDefinition(techniqueId, skillId);
        if (skillOpt.isEmpty())
        {
            return SkillUpgradeResult.UNKNOWN_SKILL;
        }
        TechniqueSkill skill = skillOpt.get();
        if (skill.levelTag() == null || skill.levelTag().isEmpty())
        {
            return SkillUpgradeResult.NOT_UPGRADEABLE;
        }
        if (!SkillUnlockLogic.isSkillUnlocked(player, techniqueId, skillId, skill))
        {
            return SkillUpgradeResult.NOT_UNLOCKED;
        }

        int level = getSkillLevel(player, skill);
        if (SkillLevelRules.isMaxLevel(level))
        {
            return SkillUpgradeResult.ALREADY_MAX;
        }

        Optional<SkillUpgradeCost> costOpt = SkillUpgradeCostRegistry.costForNextLevel(skillId, level);
        if (costOpt.isEmpty())
        {
            return SkillUpgradeResult.ALREADY_MAX;
        }
        SkillUpgradeCost cost = costOpt.get();
        boolean creativeFree = player.isCreative();
        if (!creativeFree)
        {
            if (player.experienceLevel < cost.vanillaXpLevels())
            {
                return SkillUpgradeResult.INSUFFICIENT_VANILLA_XP;
            }
            if (!hasAllCursedObjects(player, cost))
            {
                return SkillUpgradeResult.MISSING_CURSED_OBJECT;
            }
            if (cost.vanillaXpLevels() > 0)
            {
                player.giveExperienceLevels(-cost.vanillaXpLevels());
            }
            for (CursedObjectIngredient ingredient : cost.cursedObjects())
            {
                consumeItem(player, ingredient.item().get(), ingredient.count());
            }
        }
        setSkillLevel(player, skill, level + 1);
        SkillUnlockLogic.syncLevelsToClient(player);
        return SkillUpgradeResult.SUCCESS;
    }

    public static boolean meetsCost(Player player, SkillUpgradeCost cost)
    {
        if (player.isCreative())
        {
            return true;
        }
        if (player.experienceLevel < cost.vanillaXpLevels())
        {
            return false;
        }
        return hasAllCursedObjects(player, cost);
    }

    public static boolean hasAllCursedObjects(Player player, SkillUpgradeCost cost)
    {
        for (CursedObjectIngredient ingredient : cost.cursedObjects())
        {
            if (countItem(player, ingredient.item().get()) < ingredient.count())
            {
                return false;
            }
        }
        return true;
    }

    public static int countItem(Player player, Item item)
    {
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++)
        {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item))
            {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static void consumeItem(Player player, Item item, int amount)
    {
        int remaining = amount;
        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++)
        {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.is(item))
            {
                continue;
            }
            int take = Math.min(remaining, stack.getCount());
            stack.shrink(take);
            remaining -= take;
        }
        if (player instanceof ServerPlayer serverPlayer)
        {
            serverPlayer.inventoryMenu.broadcastChanges();
        }
    }
}
