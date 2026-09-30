package com.darkness.jujutsukaisenzone.player;

import com.darkness.jujutsukaisenzone.Config;
import com.darkness.jujutsukaisenzone.network.ModNetwork;
import com.darkness.jujutsukaisenzone.registry.ModAttributes;
import com.darkness.jujutsukaisenzone.technique.domain.DomainExpansionRouter;
import com.darkness.jujutsukaisenzone.technique.se.DomainChargeCurveSeMechanic;
import com.darkness.jujutsukaisenzone.technique.se.DomainGaugeSeMechanic;
import com.darkness.jujutsukaisenzone.technique.se.SeMechanicRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

/**
 * 领域量表：战斗灌充，满值才能展开；展开抽干。
 * 充能效率随脑损下降；脑损满、或领域主人仍在维持活跃领域时冻结灌充。
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
        return getMax(null);
    }

    /** 满条值 = 配置 + SE「领域量表满条」代价合计（更难攒满）。 */
    public static int getMax(Player player)
    {
        int base = Config.domainGaugeMax > 0 ? Config.domainGaugeMax : 100;
        if (player == null)
        {
            return base;
        }
        int bonus = (int) Math.ceil(PlayerSeLoadoutData.sumPenaltyAmount(
                player, com.darkness.jujutsukaisenzone.technique.se.PanelStat.ApplyKind.DOMAIN_GAUGE_MAX_ADD));
        return Math.max(1, base + bonus);
    }

    public static int getGauge(Player player)
    {
        if (player instanceof ServerPlayer serverPlayer)
        {
            return clamp(player, getData(serverPlayer).domainGauge);
        }
        return clamp(player, player.getPersistentData().getInt(CLIENT_TAG_GAUGE));
    }

    public static boolean isFull(Player player)
    {
        if (!enabled())
        {
            return true;
        }
        return getGauge(player) >= getMax(player);
    }

    /**
     * 展开前检查：量表未满则提示并返回 false（不应扣咒力）。
     * 创造模式不要求量表满。
     */
    public static boolean tryAllowDomainExpansion(ServerPlayer player)
    {
        if (!enabled())
        {
            return true;
        }
        if (player.isCreative())
        {
            return true;
        }
        if (isFull(player))
        {
            return true;
        }
        player.displayClientMessage(
                Component.literal("领域量表不足（" + getGauge(player) + "/" + getMax(player) + "），需通过战斗积攒")
                        .withStyle(ChatFormatting.RED),
                true);
        return false;
    }

    /** 成功展开后抽干量表（创造模式不消耗）。 */
    public static void consumeOnExpand(ServerPlayer player)
    {
        if (!enabled())
        {
            return;
        }
        if (player.isCreative())
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
        if (!tryConsumeHitChargeCooldown(player))
        {
            return;
        }
        addChargeRaw(player, Config.domainGaugeTechniqueHitGain);
    }

    /**
     * 体术命中灌充——仅当该玩家装载了 {@link DomainGaugeSeMechanic} 时生效。
     * <p>
     * 与术式命中<b>共用</b>同一冷却；未装备时立即返回，不碰全局默认规则。
     * {@code COMPOSITE} 不要调用本方法（黑闪已有独立灌充）。
     */
    public static void addChargeFromMartialHit(ServerPlayer player)
    {
        if (!enabled())
        {
            return;
        }
        Optional<DomainGaugeSeMechanic> mechanic =
                SeMechanicRegistry.findEquipped(player, DomainGaugeSeMechanic.class);
        if (mechanic.isEmpty())
        {
            return;
        }
        float ratio = mechanic.get().martialChargeRatio();
        if (ratio <= 0.0F)
        {
            return;
        }
        if (!tryConsumeHitChargeCooldown(player))
        {
            return;
        }
        addChargeRaw(player, Config.domainGaugeTechniqueHitGain * (double) ratio);
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

    /** @return false 若仍在命中灌充冷却中 */
    private static boolean tryConsumeHitChargeCooldown(ServerPlayer player)
    {
        long now = player.level().getGameTime();
        long until = player.getPersistentData().getLong(TAG_HIT_CHARGE_COOLDOWN_UNTIL);
        if (now < until)
        {
            return false;
        }
        player.getPersistentData().putLong(TAG_HIT_CHARGE_COOLDOWN_UNTIL,
                now + Math.max(0, Config.domainGaugeTechniqueHitCooldownTicks));
        return true;
    }

    /**
     * 最终充能效率 = 脑损档位 × 面板「领域量表充能效率」属性。
     * 满脑损、或领域主人仍在维持活跃领域时冻结灌充（返回 0）。
     */
    public static double chargeEfficiency(Player player)
    {
        if (player instanceof ServerPlayer serverPlayer
                && DomainExpansionRouter.hasActiveDomain(serverPlayer))
        {
            return 0.0D;
        }
        if (PlayerBrainDamageData.isFull(player))
        {
            return 0.0D;
        }
        double brain = brainDamageEfficiency(player);
        double panel = ModAttributes.domainGaugeChargeEfficiencyMultiplier(player);
        return brain * panel;
    }

    /** 仅脑损带来的充能系数（不含属性面板）。可被 {@link DomainChargeCurveSeMechanic} 改写中档。 */
    public static double brainDamageEfficiency(Player player)
    {
        int damage = PlayerBrainDamageData.getBrainDamage(player);
        double base;
        if (damage >= 75)
        {
            base = Config.domainGaugeEfficiency75;
        }
        else if (damage >= 50)
        {
            base = Config.domainGaugeEfficiency50;
        }
        else if (damage >= 25)
        {
            base = Config.domainGaugeEfficiency25;
        }
        else
        {
            return 1.0D;
        }
        Optional<DomainChargeCurveSeMechanic> curve =
                SeMechanicRegistry.findEquipped(player, DomainChargeCurveSeMechanic.class);
        if (curve.isEmpty())
        {
            return base;
        }
        return curve.get().adjustBrainTierEfficiency(damage, base);
    }

    public static void setGauge(ServerPlayer player, int value)
    {
        getData(player).domainGauge = clamp(player, value);
        markDirty(player);
        syncToClient(player);
    }

    public static void applyClientSnapshot(Player player, int gauge)
    {
        player.getPersistentData().putInt(CLIENT_TAG_GAUGE, clamp(player, gauge));
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
        if (before >= getMax(player))
        {
            return;
        }
        int gain = Math.max(1, (int) Math.round(baseGain * efficiency));
        JjkzPlayerDataStorage.PlayerDataEntry data = getData(player);
        data.domainGauge = clamp(player, before + gain);
        markDirty(player);
        syncToClient(player);
        if (before < getMax(player) && data.domainGauge >= getMax(player))
        {
            player.displayClientMessage(
                    Component.literal("领域量表已满，可以展开领域")
                            .withStyle(ChatFormatting.LIGHT_PURPLE),
                    true);
        }
    }

    private static int clamp(Player player, int value)
    {
        return Math.max(0, Math.min(getMax(player), value));
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
