package com.darkness.jujutsukaisenzone.player;

import com.darkness.jujutsukaisenzone.Config;
import com.darkness.jujutsukaisenzone.network.ModNetwork;
import com.darkness.jujutsukaisenzone.registry.ModAttributes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * 领域量表：战斗灌充，满值才能展开；展开抽干。
 * 充能效率随脑损下降；脑损满时冻结灌充。
 */
public final class PlayerDomainGaugeData
{
    private static final String CLIENT_TAG_GAUGE = "jujutsukaisenzone_domain_gauge";
    private static final String TAG_HIT_CHARGE_COOLDOWN_UNTIL = "jujutsukaisenzone_domain_gauge_hit_cd";

    private PlayerDomainGaugeData()
    {
    }

    public static boolean enabled()
    {
        return Config.domainGaugeEnabled;
    }

    public static int getMax()
    {
        return Config.domainGaugeMax > 0 ? Config.domainGaugeMax : 100;
    }

    public static int getGauge(Player player)
    {
        if (player instanceof ServerPlayer serverPlayer)
        {
            return clamp(getData(serverPlayer).domainGauge);
        }
        return clamp(player.getPersistentData().getInt(CLIENT_TAG_GAUGE));
    }

    public static boolean isFull(Player player)
    {
        if (!enabled())
        {
            return true;
        }
        return getGauge(player) >= getMax();
    }

    /**
     * 展开前检查：量表未满则提示并返回 false（不应扣咒力）。
     */
    public static boolean tryAllowDomainExpansion(ServerPlayer player)
    {
        if (!enabled())
        {
            return true;
        }
        if (isFull(player))
        {
            return true;
        }
        player.displayClientMessage(
                Component.literal("领域量表不足（" + getGauge(player) + "/" + getMax() + "），需通过战斗积攒")
                        .withStyle(ChatFormatting.RED),
                true);
        return false;
    }

    /** 成功展开后抽干量表。 */
    public static void consumeOnExpand(ServerPlayer player)
    {
        if (!enabled())
        {
            return;
        }
        JjkzPlayerDataStorage.PlayerDataEntry data = getData(player);
        data.domainGauge = 0;
        markDirty(player);
        syncToClient(player);
    }

    /** 术式命中灌充（带内置冷却，防高频小伤刷满）。 */
    public static void addChargeFromTechniqueHit(ServerPlayer player)
    {
        if (!enabled())
        {
            return;
        }
        long now = player.level().getGameTime();
        long until = player.getPersistentData().getLong(TAG_HIT_CHARGE_COOLDOWN_UNTIL);
        if (now < until)
        {
            return;
        }
        player.getPersistentData().putLong(TAG_HIT_CHARGE_COOLDOWN_UNTIL,
                now + Math.max(0, Config.domainGaugeTechniqueHitCooldownTicks));
        addChargeRaw(player, Config.domainGaugeTechniqueHitGain);
    }

    /** 黑闪触发灌充。 */
    public static void addChargeFromBlackFlash(ServerPlayer player)
    {
        if (!enabled())
        {
            return;
        }
        addChargeRaw(player, Config.domainGaugeBlackFlashGain);
    }

    /**
     * 最终充能效率 = 脑损档位 × 面板「领域量表充能效率」属性。
     * 满脑损冻结灌充（返回 0）。
     */
    public static double chargeEfficiency(Player player)
    {
        if (PlayerBrainDamageData.isFull(player))
        {
            return 0.0D;
        }
        double brain = brainDamageEfficiency(player);
        double panel = ModAttributes.domainGaugeChargeEfficiencyMultiplier(player);
        return brain * panel;
    }

    /** 仅脑损带来的充能系数（不含属性面板）。 */
    public static double brainDamageEfficiency(Player player)
    {
        int damage = PlayerBrainDamageData.getBrainDamage(player);
        if (damage >= 75)
        {
            return Config.domainGaugeEfficiency75;
        }
        if (damage >= 50)
        {
            return Config.domainGaugeEfficiency50;
        }
        if (damage >= 25)
        {
            return Config.domainGaugeEfficiency25;
        }
        return 1.0D;
    }

    public static void setGauge(ServerPlayer player, int value)
    {
        getData(player).domainGauge = clamp(value);
        markDirty(player);
        syncToClient(player);
    }

    public static void applyClientSnapshot(Player player, int gauge)
    {
        player.getPersistentData().putInt(CLIENT_TAG_GAUGE, clamp(gauge));
    }

    public static void syncToClient(ServerPlayer player)
    {
        ModNetwork.sendDomainResourcesSync(
                player,
                PlayerBrainDamageData.getBrainDamage(player),
                PlayerBrainDamageData.getExpandsToday(player),
                getGauge(player),
                PlayerBrainDamageData.getMeltdownRepairsToday(player));
    }

    private static void addChargeRaw(ServerPlayer player, double baseGain)
    {
        if (baseGain <= 0.0D)
        {
            return;
        }
        double efficiency = chargeEfficiency(player);
        if (efficiency <= 0.0D)
        {
            return;
        }
        int before = getGauge(player);
        if (before >= getMax())
        {
            return;
        }
        int gain = Math.max(1, (int) Math.round(baseGain * efficiency));
        JjkzPlayerDataStorage.PlayerDataEntry data = getData(player);
        data.domainGauge = clamp(before + gain);
        markDirty(player);
        syncToClient(player);
        if (before < getMax() && data.domainGauge >= getMax())
        {
            player.displayClientMessage(
                    Component.literal("领域量表已满，可以展开领域")
                            .withStyle(ChatFormatting.LIGHT_PURPLE),
                    true);
        }
    }

    private static int clamp(int value)
    {
        return Math.max(0, Math.min(getMax(), value));
    }

    private static JjkzPlayerDataStorage.PlayerDataEntry getData(ServerPlayer player)
    {
        PlayerCursedEnergyData.initializeIfAbsent(player);
        return JjkzPlayerDataStorage.get(player).getOrCreate(player);
    }

    private static void markDirty(ServerPlayer player)
    {
        JjkzPlayerDataStorage.get(player).setDirty();
    }
}
