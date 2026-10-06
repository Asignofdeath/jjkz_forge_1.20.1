package com.darkness.jujutsukaisenzone.player;

import com.darkness.jujutsukaisenzone.effect.ConfiscationEffect;
import com.darkness.jujutsukaisenzone.network.ModNetwork;
import com.darkness.jujutsukaisenzone.technique.se.SeMechanicRegistry;
import com.darkness.jujutsukaisenzone.technique.se.WorkEfficiencySeMechanic;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;

public final class PlayerCursedEnergyData
{
    public static final int HEAVENLY_RESTRICTION_THRESHOLD = 50;
    private static final String TAG_MAX = "jujutsukaisenzone_cursed_energy_max";
    private static final String TAG_CURRENT = "jujutsukaisenzone_cursed_energy_current";
    private static final int MIN_MAX_CURSED_ENERGY = 0;
    private static final int MAX_MAX_CURSED_ENERGY = 512;

    private PlayerCursedEnergyData()
    {
    }

    public static void initializeIfAbsent(Player player)
    {
        if (!(player instanceof ServerPlayer serverPlayer))
        {
            return;
        }
        JjkzPlayerDataStorage.PlayerDataEntry data = getServerData(serverPlayer);
        if (data.cursedEnergyInitialized)
        {
            return;
        }

        RandomSource random = player.getRandom();
        int baseMax = random.nextInt(MAX_MAX_CURSED_ENERGY - MIN_MAX_CURSED_ENERGY + 1) + MIN_MAX_CURSED_ENERGY;
        data.cursedEnergyInitialized = true;
        data.cursedEnergyBaseMax = baseMax;
        com.darkness.jujutsukaisenzone.talent.InnateTalentLottery.rollOnFirstEnergyInit(serverPlayer, baseMax);
        int effective = computeEffectiveMax(player, baseMax);
        data.cursedEnergyCurrent = effective;
        markDirty(serverPlayer);
    }

    public static int getMax(Player player)
    {
        if (player instanceof ServerPlayer serverPlayer)
        {
            int base = getBaseMax(serverPlayer);
            return computeEffectiveMax(player, base);
        }
        return Math.max(0, player.getPersistentData().getInt(TAG_MAX));
    }

    private static int computeEffectiveMax(Player player, int baseMax)
    {
        if (com.darkness.jujutsukaisenzone.talent.InnateTalentData.is(
                player, com.darkness.jujutsukaisenzone.talent.InnateTalent.HEAVENLY_RESTRICTION))
        {
            return 0;
        }
        int rank = PlayerCursedGradeData.getRawRank(player);
        int effective = Math.max(0, baseMax + CursedGradeBonuses.getMaxEnergyBonus(rank));
        int sePenalty = (int) Math.ceil(PlayerSeLoadoutData.sumPenaltyAmount(
                player, com.darkness.jujutsukaisenzone.technique.se.PanelStat.ApplyKind.CURSED_ENERGY_MAX_SUBTRACT));
        return Math.max(0, effective - sePenalty);
    }

    public static boolean isHeavenlyRestricted(Player player)
    {
        return com.darkness.jujutsukaisenzone.talent.InnateTalentData.is(
                player, com.darkness.jujutsukaisenzone.talent.InnateTalent.HEAVENLY_RESTRICTION);
    }

    private static int getBaseMax(ServerPlayer player)
    {
        return Math.max(0, getServerData(player).cursedEnergyBaseMax);
    }

    public static int getCurrent(Player player)
    {
        int max = getMax(player);
        int current = player instanceof ServerPlayer serverPlayer
                ? getServerData(serverPlayer).cursedEnergyCurrent
                : player.getPersistentData().getInt(TAG_CURRENT);
        return Math.max(0, Math.min(current, max));
    }

    public static void setCurrent(Player player, int value)
    {
        int max = getMax(player);
        int clamped = Math.max(0, Math.min(value, max));
        if (player instanceof ServerPlayer serverPlayer)
        {
            getServerData(serverPlayer).cursedEnergyCurrent = clamped;
            markDirty(serverPlayer);
            cacheForClient(serverPlayer, clamped, max);
            return;
        }
        player.getPersistentData().putInt(TAG_CURRENT, clamped);
    }

    public static void setMaxAndCurrent(Player player, int maxValue, int currentValue)
    {
        int clampedMax = Math.max(0, maxValue);
        player.getPersistentData().putInt(TAG_MAX, clampedMax);
        player.getPersistentData().putInt(TAG_CURRENT, Math.max(0, Math.min(currentValue, clampedMax)));
    }

    /** 咒术等级变化后重算上限并同步（仅服务端）。 */
    public static void recalculateAfterGradeChange(ServerPlayer player)
    {
        int base = getBaseMax(player);
        int newMax = computeEffectiveMax(player, base);
        JjkzPlayerDataStorage.PlayerDataEntry data = getServerData(player);
        int cur = Math.max(0, Math.min(data.cursedEnergyCurrent, newMax));
        data.cursedEnergyCurrent = cur;
        markDirty(player);
        cacheForClient(player, cur, newMax);
        ModNetwork.sendCursedEnergySync(player, cur, newMax);
    }

    /**
     * 作弊用途：直接设置先天基础咒力总量，再按当前规则重算有效上限并同步。
     */
    public static void setBaseMaxForCheat(ServerPlayer player, int baseMax)
    {
        int clamped = Math.max(0, baseMax);
        JjkzPlayerDataStorage.PlayerDataEntry data = getServerData(player);
        data.cursedEnergyInitialized = true;
        data.cursedEnergyBaseMax = clamped;
        markDirty(player);
        recalculateAfterGradeChange(player);
    }

    public static boolean tryConsume(ServerPlayer player, int amount)
    {
        if (amount > 0 && ConfiscationEffect.blockCursedEnergyUse(player))
        {
            return false;
        }
        if (amount <= 0)
        {
            return true;
        }
        int billed = com.darkness.jujutsukaisenzone.talent.InnateTalentData.billCursedEnergy(player, amount);
        if (billed <= 0)
        {
            return true;
        }
        int cur = getCurrent(player);
        if (cur < billed)
        {
            return false;
        }
        setCurrent(player, cur - billed);
        ModNetwork.sendCursedEnergySync(player, getCurrent(player), getMax(player));
        maybeTriggerWorkEfficiencySe(player, billed);
        return true;
    }

    /**
     * 装载「工作效率」SE 时：单次消耗达阈值则开启 / 刷新与无可谅解相同的效率窗口。
     */
    private static void maybeTriggerWorkEfficiencySe(ServerPlayer player, int amount)
    {
        SeMechanicRegistry.findEquipped(player, WorkEfficiencySeMechanic.class).ifPresent(mechanic ->
        {
            if (amount < mechanic.triggerConsumeThreshold())
            {
                return;
            }
            boolean wasActive = PlayerRatioOvertimeData.isEfficiencyActive(player);
            PlayerRatioOvertimeData.startEfficiency(player);
            player.displayClientMessage(
                    Component.literal(wasActive ? "工作效率已刷新" : "工作效率")
                            .withStyle(ChatFormatting.GOLD),
                    true);
        });
    }

    private static JjkzPlayerDataStorage.PlayerDataEntry getServerData(ServerPlayer player)
    {
        return JjkzPlayerDataStorage.get(player).getOrCreate(player);
    }

    private static void markDirty(ServerPlayer player)
    {
        JjkzPlayerDataStorage.get(player).setDirty();
    }

    private static void cacheForClient(Player player, int current, int max)
    {
        player.getPersistentData().putInt(TAG_CURRENT, current);
        player.getPersistentData().putInt(TAG_MAX, max);
    }
}
