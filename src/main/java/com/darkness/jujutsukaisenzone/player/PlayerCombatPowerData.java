package com.darkness.jujutsukaisenzone.player;

import com.darkness.jujutsukaisenzone.Config;
import com.darkness.jujutsukaisenzone.network.ModNetwork;
import com.darkness.jujutsukaisenzone.player.CombatPowerStats.Contribution;
import com.darkness.jujutsukaisenzone.technique.skill.tenshadows.TenShadowsPresenceBonuses;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * 玩家「体术强度 / 术式强度」的唯一来源（服务端权威，客户端只读缓存）。
 * <p>
 * 所有获取途径集中登记在这里，后续加修炼书 / 装备 / 词条时只改一处：
 * <ul>
 *   <li>基础 + 咒术等级（拟态五条 / 宿傩已由 {@code PlayerCursedGradeData#getRank} 自动按特级算）</li>
 *   <li>天与咒缚：术式强度归零，体术强度按倍数放大</li>
 *   <li>反转术式已习得 → 术式强度</li>
 *   <li>术式情报（各术式累计）→ 术式强度</li>
 *   <li>存档内的修炼加成（{@code physicalPowerBonus} / {@code techniquePowerBonus}）</li>
 * </ul>
 */
public final class PlayerCombatPowerData
{
    private static final String TAG_PHYSICAL = "jujutsukaisenzone_physical_power";
    private static final String TAG_TECHNIQUE = "jujutsukaisenzone_technique_power";

    private PlayerCombatPowerData()
    {
    }

    public static int physicalPower(Player player)
    {
        return resolve(player).physicalPower();
    }

    public static int techniquePower(Player player)
    {
        return resolve(player).techniquePower();
    }

    public static CombatPowerStats resolve(Player player)
    {
        return player instanceof ServerPlayer serverPlayer ? resolveServer(serverPlayer) : resolveClient(player);
    }

    private static CombatPowerStats resolveServer(ServerPlayer player)
    {
        int rank = PlayerCursedGradeData.getRank(player);
        int base = (int) Math.round(Config.combatPowerBase + Config.combatPowerPerRank * rank);

        List<Contribution> physical = new ArrayList<>();
        List<Contribution> technique = new ArrayList<>();
        physical.add(new Contribution("基础 / 咒术等级", base));
        technique.add(new Contribution("基础 / 咒术等级", base));

        int physicalTotal = base;
        int techniqueTotal = base;

        boolean heavenly = PlayerCursedEnergyData.isHeavenlyRestricted(player);
        if (heavenly)
        {
            // 天与咒缚：没有咒力 ⇒ 术式强度恒为 0，体术强度整体放大。
            technique.clear();
            technique.add(new Contribution("天与咒缚（无咒力）", 0));
            techniqueTotal = 0;

            int boosted = (int) Math.round(physicalTotal * Config.combatPowerHeavenlyPhysicalMultiplier);
            physical.add(new Contribution("天与咒缚", boosted - physicalTotal));
            physicalTotal = boosted;
        }
        else if (PlayerReverseTechniqueData.hasReverseTechnique(player))
        {
            int reverse = (int) Math.round(Config.combatPowerReverseTechniqueBonus);
            if (reverse != 0)
            {
                technique.add(new Contribution("反转术式", reverse));
                techniqueTotal += reverse;
            }
        }

        int intelTotal = 0;
        for (int value : PlayerTechniqueIntelData.snapshot(player).values())
        {
            intelTotal += Math.max(0, value);
        }
        int intelBonus = (int) Math.round(Config.combatPowerIntelPerLevel * intelTotal);
        if (intelBonus != 0)
        {
            technique.add(new Contribution("术式情报 x" + intelTotal, intelBonus));
            techniqueTotal += intelBonus;
        }

        JjkzPlayerDataStorage.PlayerDataEntry data = JjkzPlayerDataStorage.get(player).getOrCreate(player);
        if (data.physicalPowerBonus != 0)
        {
            physical.add(new Contribution("修炼", data.physicalPowerBonus));
            physicalTotal += data.physicalPowerBonus;
        }
        if (data.techniquePowerBonus != 0)
        {
            technique.add(new Contribution("修炼", data.techniquePowerBonus));
            techniqueTotal += data.techniquePowerBonus;
        }

        int spPhys = PlayerSpNodeData.sumPhysicalBonus(player);
        int spTech = PlayerSpNodeData.sumTechniqueBonus(player);
        if (spPhys != 0)
        {
            physical.add(new Contribution("SP节点", spPhys));
            physicalTotal += spPhys;
        }
        if (spTech != 0)
        {
            technique.add(new Contribution("SP节点", spTech));
            techniqueTotal += spTech;
        }

        int dogBonus = TenShadowsPresenceBonuses.divineDogTechniqueBonus(player);
        if (dogBonus > 0)
        {
            technique.add(new Contribution("十影·玉犬", dogBonus));
            techniqueTotal += dogBonus;
        }
        if (TenShadowsPresenceBonuses.hasSerpent(player))
        {
            int physBoost = (int) Math.round(physicalTotal * TenShadowsPresenceBonuses.SERPENT_POWER_MULTIPLIER);
            int techBoost = (int) Math.round(techniqueTotal * TenShadowsPresenceBonuses.SERPENT_POWER_MULTIPLIER);
            if (physBoost != 0)
            {
                physical.add(new Contribution("十影·大蛇", physBoost));
                physicalTotal += physBoost;
            }
            if (techBoost != 0)
            {
                technique.add(new Contribution("十影·大蛇", techBoost));
                techniqueTotal += techBoost;
            }
        }

        return new CombatPowerStats(
                Math.max(0, physicalTotal),
                Math.max(0, techniqueTotal),
                List.copyOf(physical),
                List.copyOf(technique));
    }

    /** 客户端只读缓存；尚未同步过时返回 -1，消费方按未启用处理。 */
    private static CombatPowerStats resolveClient(Player player)
    {
        var tag = player.getPersistentData();
        if (!tag.contains(TAG_PHYSICAL) || !tag.contains(TAG_TECHNIQUE))
        {
            return new CombatPowerStats(-1, -1);
        }
        return new CombatPowerStats(tag.getInt(TAG_PHYSICAL), tag.getInt(TAG_TECHNIQUE));
    }

    /** 修炼书 / 道具 / 未来的装备词条统一走这里。 */
    public static void addBonus(ServerPlayer player, int physicalDelta, int techniqueDelta)
    {
        JjkzPlayerDataStorage.PlayerDataEntry data = JjkzPlayerDataStorage.get(player).getOrCreate(player);
        data.physicalPowerBonus = Math.max(0, data.physicalPowerBonus + physicalDelta);
        data.techniquePowerBonus = Math.max(0, data.techniquePowerBonus + techniqueDelta);
        JjkzPlayerDataStorage.get(player).setDirty();
        syncToClient(player);
    }

    public static void syncToClient(ServerPlayer player)
    {
        CombatPowerStats stats = resolveServer(player);
        cacheForClient(player, stats.physicalPower(), stats.techniquePower());
        ModNetwork.sendCombatPowerSync(player, stats.physicalPower(), stats.techniquePower());
    }

    public static void applyClientSnapshot(Player player, int physical, int technique)
    {
        cacheForClient(player, physical, technique);
    }

    private static void cacheForClient(Player player, int physical, int technique)
    {
        player.getPersistentData().putInt(TAG_PHYSICAL, physical);
        player.getPersistentData().putInt(TAG_TECHNIQUE, technique);
    }
}
