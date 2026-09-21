package com.darkness.jujutsukaisenzone.player;



import com.darkness.jujutsukaisenzone.network.ModNetwork;

import com.darkness.jujutsukaisenzone.technique.TechniqueDailyUpgradeLogic;

import com.darkness.jujutsukaisenzone.technique.skillset.TechniqueSkillSetRegistry;

import net.minecraft.ChatFormatting;

import net.minecraft.network.chat.Component;

import net.minecraft.server.level.ServerPlayer;

import net.minecraft.world.entity.player.Player;



/**

 * 强化术等级与开关状态。

 */

public final class PlayerStrengthenTechniqueData

{

    private static final String CLIENT_TAG_LEARNED = "jujutsukaisenzone_strengthen_technique_learned";

    private static final String CLIENT_TAG_TIER = "jujutsukaisenzone_strengthen_technique_tier";

    private static final String CLIENT_TAG_ACTIVE = "jujutsukaisenzone_strengthen_technique_active";



    private PlayerStrengthenTechniqueData()

    {

    }



    public static StrengthenTechniqueLevel getLevel(ServerPlayer player)

    {

        JjkzPlayerDataStorage.PlayerDataEntry data = getData(player);

        return StrengthenTechniqueLevel.fromString(data.strengthenTechniqueLevel);

    }



    public static StrengthenTechniqueLevel getLevel(Player player)

    {

        if (player instanceof ServerPlayer serverPlayer)

        {

            return getLevel(serverPlayer);

        }

        return StrengthenTechniqueLevel.fromTier(player.getPersistentData().getInt(CLIENT_TAG_TIER));

    }



    public static void setLevel(ServerPlayer player, StrengthenTechniqueLevel level)

    {

        JjkzPlayerDataStorage.PlayerDataEntry data = getData(player);

        data.strengthenTechniqueLevel = level.name();

        if (level == StrengthenTechniqueLevel.UNLEARNED)

        {

            forceDisable(player);

        }

        markDirty(player);

        syncToClient(player);

        TechniqueSkillSetRegistry.syncSkillBarToClient(player);

        CursedGradeBonuses.applyAll(player);

    }



    public static boolean hasLearned(Player player)

    {

        if (player instanceof ServerPlayer serverPlayer)

        {

            return getLevel(serverPlayer) != StrengthenTechniqueLevel.UNLEARNED;

        }

        return player.getPersistentData().getBoolean(CLIENT_TAG_LEARNED);

    }



    public static boolean isActive(Player player)

    {

        if (player instanceof ServerPlayer serverPlayer)

        {

            return getData(serverPlayer).strengthenActive;

        }

        return player.getPersistentData().getBoolean(CLIENT_TAG_ACTIVE);

    }



    public static double getMaintainCostReduction(ServerPlayer player)

    {

        return getLevel(player).getMaintainCostReduction();

    }



    public static void applyClientState(Player player, boolean learned, int tier, boolean active)

    {

        player.getPersistentData().putBoolean(CLIENT_TAG_LEARNED, learned);

        player.getPersistentData().putInt(CLIENT_TAG_TIER, Math.max(0, tier));

        player.getPersistentData().putBoolean(CLIENT_TAG_ACTIVE, active);

    }



    public static boolean toggle(ServerPlayer player)

    {

        if (!hasLearned(player))

        {

            player.displayClientMessage(

                    Component.literal("你还没有习得强化术").withStyle(ChatFormatting.RED),

                    true

            );

            return false;

        }



        if (isActive(player))

        {

            setActive(player, false);

            player.displayClientMessage(

                    Component.literal("强化术已解除").withStyle(ChatFormatting.GRAY),

                    true

            );

            return true;

        }



        setActive(player, true);

        TechniqueDailyUpgradeLogic.onStrengthenTechniqueUsed(player);

        player.displayClientMessage(

                Component.literal("强化术已发动").withStyle(ChatFormatting.GOLD),

                true

        );

        return true;

    }



    public static void forceDisable(ServerPlayer player)

    {

        if (!isActive(player))

        {

            return;

        }

        setActive(player, false);

    }



    private static void setActive(ServerPlayer player, boolean active)

    {

        JjkzPlayerDataStorage.PlayerDataEntry data = getData(player);

        if (data.strengthenActive == active)

        {

            return;

        }

        data.strengthenActive = active;

        markDirty(player);

        syncToClient(player);

        CursedGradeBonuses.applyAll(player);

    }



    private static void syncToClient(ServerPlayer player)

    {

        ModNetwork.sendStrengthenTechniqueLearnedSync(

                player,

                hasLearned(player),

                getLevel(player).getTier(),

                isActive(player));

    }



    public static int getUpgradeChancePercent(ServerPlayer player)

    {

        return Math.max(0, Math.min(8, getData(player).strengthenTechniqueUpgradeChance));

    }



    public static void setUpgradeChancePercent(ServerPlayer player, int percent)

    {

        getData(player).strengthenTechniqueUpgradeChance = Math.max(0, Math.min(8, percent));

        markDirty(player);

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


