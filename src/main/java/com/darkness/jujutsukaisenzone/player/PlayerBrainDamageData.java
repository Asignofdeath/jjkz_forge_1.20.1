package com.darkness.jujutsukaisenzone.player;

import com.darkness.jujutsukaisenzone.Config;
import com.darkness.jujutsukaisenzone.effect.TechniqueMeltdownEffect;
import com.darkness.jujutsukaisenzone.technique.se.MeltdownRepairSeMechanic;
import com.darkness.jujutsukaisenzone.technique.se.SeMechanicRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;
import java.util.UUID;

/**
 * 领域相关「脑损」：今日免费展开额度内不增加；超额递增；跨游戏日回复；满值禁止开域。
 * <p>
 * 第 3 期：通用技能「修复术式熔断」也会增加脑损（按今日修复次数递增）。
 */
public final class PlayerBrainDamageData
{
    private static final String CLIENT_TAG_DAMAGE = "jujutsukaisenzone_brain_damage";
    private static final String CLIENT_TAG_EXPANDS_TODAY = "jujutsukaisenzone_domain_expands_today";
    private static final String CLIENT_TAG_MELTDOWN_REPAIRS = "jujutsukaisenzone_meltdown_repairs_today";

    private static final UUID SPEED_DEBUFF_UUID = UUID.fromString("6b2c9a01-4001-4000-8000-000000000001");
    private static final UUID ATTACK_DEBUFF_UUID = UUID.fromString("6b2c9a01-4002-4000-8000-000000000002");

    private PlayerBrainDamageData()
    {
    }

    public static int getBrainDamage(Player player)
    {
        if (player instanceof ServerPlayer serverPlayer)
        {
            return clampDamage(player, getData(serverPlayer).brainDamage);
        }
        return clampDamage(player, player.getPersistentData().getInt(CLIENT_TAG_DAMAGE));
    }

    public static int getExpandsToday(Player player)
    {
        if (player instanceof ServerPlayer serverPlayer)
        {
            syncExpandDay(serverPlayer);
            return Math.max(0, getData(serverPlayer).domainExpandsToday);
        }
        return Math.max(0, player.getPersistentData().getInt(CLIENT_TAG_EXPANDS_TODAY));
    }

    public static int getMeltdownRepairsToday(Player player)
    {
        if (player instanceof ServerPlayer serverPlayer)
        {
            syncRepairDay(serverPlayer);
            return Math.max(0, getData(serverPlayer).meltdownRepairsToday);
        }
        return Math.max(0, player.getPersistentData().getInt(CLIENT_TAG_MELTDOWN_REPAIRS));
    }

    public static int getFreeDailyQuota()
    {
        return Math.max(0, Config.domainBrainFreeDailyExpansions);
    }

    public static int getBrainMax(Player player)
    {
        int base = Config.domainBrainMax > 0 ? Config.domainBrainMax : 100;
        int penalty = (int) Math.ceil(PlayerSeLoadoutData.sumPenaltyAmount(
                player, com.darkness.jujutsukaisenzone.technique.se.PanelStat.ApplyKind.BRAIN_DAMAGE_MAX_SUBTRACT));
        return Math.max(1, base - penalty);
    }

    public static boolean isFull(Player player)
    {
        return getBrainDamage(player) >= getBrainMax(player);
    }

    /**
     * 展开前检查：满脑损则造成伤害并拦截（返回 false）。
     * 未满返回 true（允许继续走咒力与展开逻辑）。
     * 创造模式不受脑损满拦截。
     */
    public static boolean tryAllowDomainExpansion(ServerPlayer player)
    {
        if (!Config.domainBrainEnabled)
        {
            return true;
        }
        if (player.isCreative())
        {
            return true;
        }
        syncExpandDay(player);
        if (!isFull(player))
        {
            return true;
        }
        float damage = (float) Config.domainBrainFullAttemptDamage;
        if (damage > 0.0F)
        {
            player.hurt(player.damageSources().magic(), damage);
        }
        player.displayClientMessage(
                Component.literal("脑损过重，强行展开领域会对大脑造成进一步伤害")
                        .withStyle(ChatFormatting.DARK_RED),
                true);
        return false;
    }

    /**
     * 领域成功展开后调用：今日次数 +1；超过免费额度则按档加脑损。
     * 创造模式不累计次数、不加脑损。
     */
    public static void onDomainExpanded(ServerPlayer player)
    {
        if (!Config.domainBrainEnabled)
        {
            return;
        }
        if (player.isCreative())
        {
            return;
        }
        syncExpandDay(player);
        JjkzPlayerDataStorage.PlayerDataEntry data = getData(player);
        data.domainExpandsToday = Math.max(0, data.domainExpandsToday) + 1;
        int expands = data.domainExpandsToday;
        int free = getFreeDailyQuota();
        if (expands > free)
        {
            int overageIndex = expands - free; // 1 = 第4次当 free=3
            int gain = resolveOverageGain(overageIndex);
            if (gain > 0)
            {
                int before = data.brainDamage;
                data.brainDamage = clampDamage(player, data.brainDamage + gain);
                player.displayClientMessage(
                        Component.literal("今日领域展开过多，脑损 +" + (data.brainDamage - before)
                                        + "（当前 " + data.brainDamage + "/" + getBrainMax(player) + "）")
                                .withStyle(ChatFormatting.RED),
                        true);
            }
        }
        markDirty(player);
        refreshDebuffs(player);
        syncToClient(player);
    }

    /**
     * 修复术式熔断：扣咒力、清除熔断、按今日第几次修复加脑损。
     * 满脑损禁止修复；无熔断 / 未习得该通用技能 / 咒力不足则失败且不扣脑损。
     */
    public static boolean tryRepairMeltdown(ServerPlayer player)
    {
        if (!Config.domainMeltdownRepairEnabled)
        {
            player.displayClientMessage(
                    Component.literal("修复熔断未启用")
                            .withStyle(ChatFormatting.GRAY),
                    true);
            return false;
        }
        if (!PlayerMeltdownRepairData.hasLearned(player))
        {
            player.displayClientMessage(
                    Component.literal("尚未习得修复术式熔断")
                            .withStyle(ChatFormatting.RED),
                    true);
            return false;
        }
        if (!TechniqueMeltdownEffect.isActive(player))
        {
            player.displayClientMessage(
                    Component.literal("当前没有术式熔断")
                            .withStyle(ChatFormatting.GRAY),
                    true);
            return false;
        }
        if (isFull(player))
        {
            player.displayClientMessage(
                    Component.literal("脑损已满，无法强行修复熔断")
                            .withStyle(ChatFormatting.DARK_RED),
                    true);
            return false;
        }

        int cost = Math.max(0, Config.domainMeltdownRepairCost);
        Optional<MeltdownRepairSeMechanic> repairMechanic =
                SeMechanicRegistry.findEquipped(player, MeltdownRepairSeMechanic.class);
        if (repairMechanic.isPresent())
        {
            cost = Math.max(0, (int) Math.round(cost * repairMechanic.get().repairCostMultiplier()));
        }
        if (!PlayerCursedEnergyData.tryConsume(player, cost))
        {
            player.displayClientMessage(
                    Component.literal("咒力不足，无法修复熔断（需要 " + cost + "）")
                            .withStyle(ChatFormatting.RED),
                    true);
            return false;
        }

        TechniqueMeltdownEffect.clear(player);

        syncRepairDay(player);
        JjkzPlayerDataStorage.PlayerDataEntry data = getData(player);
        data.meltdownRepairsToday = Math.max(0, data.meltdownRepairsToday) + 1;
        int repairIndex = data.meltdownRepairsToday;
        int gain = 0;
        if (!player.isCreative())
        {
            gain = resolveRepairGain(repairIndex);
            if (data.brainDamage >= Config.domainMeltdownRepairHighBrainThreshold)
            {
                gain += Math.max(0, Config.domainMeltdownRepairHighBrainExtra);
            }
            if (repairMechanic.isPresent())
            {
                gain = Math.max(0, (int) Math.round(gain * repairMechanic.get().repairBrainGainMultiplier()));
            }
        }
        int before = data.brainDamage;
        data.brainDamage = clampDamage(player, data.brainDamage + gain);
        int actualGain = data.brainDamage - before;

        markDirty(player);
        refreshDebuffs(player);
        syncToClient(player);

        if (player.isCreative())
        {
            player.displayClientMessage(
                    Component.literal("术式熔断已修复（创造模式不增加脑损）")
                            .withStyle(ChatFormatting.LIGHT_PURPLE),
                    true);
        }
        else
        {
            player.displayClientMessage(
                    Component.literal("术式熔断已修复，脑损 +" + actualGain
                                    + "（当前 " + data.brainDamage + "/" + getBrainMax(player)
                                    + "，今日第 " + repairIndex + " 次）")
                            .withStyle(ChatFormatting.LIGHT_PURPLE),
                    true);
        }
        return true;
    }

    /** 跨过若干游戏日时回复脑损（每过一日回复 {@link Config#domainBrainRecoverPerDay}）。 */
    public static void recoverForDaysPassed(ServerPlayer player, long daysPassed)
    {
        if (!Config.domainBrainEnabled || daysPassed <= 0L)
        {
            return;
        }
        JjkzPlayerDataStorage.PlayerDataEntry data = getData(player);
        int before = data.brainDamage;
        if (before <= 0)
        {
            syncExpandDay(player);
            syncRepairDay(player);
            return;
        }
        long totalRecover = daysPassed * (long) Config.domainBrainRecoverPerDay;
        data.brainDamage = clampDamage(player, before - (int) Math.min(before, totalRecover));
        syncExpandDay(player);
        syncRepairDay(player);
        markDirty(player);
        refreshDebuffs(player);
        syncToClient(player);
        if (data.brainDamage < before)
        {
            player.sendSystemMessage(Component.literal(
                    "经过休息，脑损缓解 " + (before - data.brainDamage)
                            + "（当前 " + data.brainDamage + "/" + getBrainMax(player) + "）"));
        }
    }

    /** 药品 / 食物回复。返回实际减少量。 */
    public static int heal(ServerPlayer player, int amount)
    {
        if (amount <= 0)
        {
            return 0;
        }
        JjkzPlayerDataStorage.PlayerDataEntry data = getData(player);
        int before = data.brainDamage;
        if (before <= 0)
        {
            return 0;
        }
        data.brainDamage = clampDamage(player, before - amount);
        markDirty(player);
        refreshDebuffs(player);
        syncToClient(player);
        return before - data.brainDamage;
    }

    public static void setBrainDamage(ServerPlayer player, int value)
    {
        getData(player).brainDamage = clampDamage(player, value);
        markDirty(player);
        refreshDebuffs(player);
        syncToClient(player);
    }

    public static void applyClientSnapshot(Player player, int brainDamage, int expandsToday, int meltdownRepairsToday)
    {
        player.getPersistentData().putInt(CLIENT_TAG_DAMAGE, clampDamage(player, brainDamage));
        player.getPersistentData().putInt(CLIENT_TAG_EXPANDS_TODAY, Math.max(0, expandsToday));
        player.getPersistentData().putInt(CLIENT_TAG_MELTDOWN_REPAIRS, Math.max(0, meltdownRepairsToday));
    }

    public static void syncToClient(ServerPlayer player)
    {
        PlayerDomainGaugeData.syncToClient(player);
    }

    /**
     * 分级负面（属性层）：50+ 降攻击；75+ 再降移速。满值不再叠加其它，开域已被拦截。
     */
    public static void refreshDebuffs(ServerPlayer player)
    {
        var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        var attack = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (speed != null)
        {
            speed.removeModifier(SPEED_DEBUFF_UUID);
        }
        if (attack != null)
        {
            attack.removeModifier(ATTACK_DEBUFF_UUID);
        }
        if (!Config.domainBrainEnabled)
        {
            return;
        }
        int damage = getBrainDamage(player);
        if (damage >= 75 && speed != null)
        {
            speed.addPermanentModifier(new AttributeModifier(
                    SPEED_DEBUFF_UUID, "jjkz_brain_damage_speed", -0.15D, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
        if (damage >= 50 && attack != null)
        {
            attack.addPermanentModifier(new AttributeModifier(
                    ATTACK_DEBUFF_UUID, "jjkz_brain_damage_attack", -0.20D, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
        if (damage >= 75 && attack != null)
        {
            // 75+ 再额外压一点攻击（先移除再以更高值挂一次）
            attack.removeModifier(ATTACK_DEBUFF_UUID);
            attack.addPermanentModifier(new AttributeModifier(
                    ATTACK_DEBUFF_UUID, "jjkz_brain_damage_attack", -0.35D, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    private static int resolveOverageGain(int overageIndex)
    {
        // overageIndex 1 = 免费额度后的第 1 次（即今日第 free+1 次）
        return switch (overageIndex)
        {
            case 1 -> Config.domainBrainOverageGain1;
            case 2 -> Config.domainBrainOverageGain2;
            case 3 -> Config.domainBrainOverageGain3;
            default -> Config.domainBrainOverageGainRest;
        };
    }

    /** repairIndex：今日第几次成功修复（从 1 起）。 */
    private static int resolveRepairGain(int repairIndex)
    {
        return switch (repairIndex)
        {
            case 1 -> Config.domainMeltdownRepairGain1;
            case 2 -> Config.domainMeltdownRepairGain2;
            case 3 -> Config.domainMeltdownRepairGain3;
            default -> Config.domainMeltdownRepairGainRest;
        };
    }

    private static void syncExpandDay(ServerPlayer player)
    {
        long day = currentGameDay(player);
        JjkzPlayerDataStorage.PlayerDataEntry data = getData(player);
        if (data.domainExpandCountDay != day)
        {
            data.domainExpandCountDay = day;
            data.domainExpandsToday = 0;
            markDirty(player);
        }
    }

    private static void syncRepairDay(ServerPlayer player)
    {
        long day = currentGameDay(player);
        JjkzPlayerDataStorage.PlayerDataEntry data = getData(player);
        if (data.meltdownRepairCountDay != day)
        {
            data.meltdownRepairCountDay = day;
            data.meltdownRepairsToday = 0;
            markDirty(player);
        }
    }

    private static long currentGameDay(ServerPlayer player)
    {
        return player.server.overworld().getDayTime() / 24000L;
    }

    private static int clampDamage(Player player, int value)
    {
        return Math.max(0, Math.min(getBrainMax(player), value));
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
