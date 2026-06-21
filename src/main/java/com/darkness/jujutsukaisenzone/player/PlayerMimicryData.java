package com.darkness.jujutsukaisenzone.player;

import com.darkness.jujutsukaisenzone.network.ModNetwork;
import com.darkness.jujutsukaisenzone.player.mimicry.MimicryCharacterRegistry;
import com.darkness.jujutsukaisenzone.technique.TechniqueDailyUpgradeLogic;
import com.darkness.jujutsukaisenzone.technique.skillset.TechniqueSkillSetRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * 降灵术拟态开关状态：服务端写入 {@link JjkzPlayerDataStorage}，客户端通过 persistentData 镜像。
 * <p>
 * 角色外观由当前生得术式推导；维持消耗由 {@link com.darkness.jujutsukaisenzone.technique.SeanceTechniqueServerLogic} 处理。
 */
public final class PlayerMimicryData
{
    private static final String TAG_MIMICRY_ACTIVE = "jujutsukaisenzone_mimicry_active";

    private PlayerMimicryData()
    {
    }

    public static boolean isActive(Player player)
    {
        if (player instanceof ServerPlayer serverPlayer)
        {
            return JjkzPlayerDataStorage.get(serverPlayer).getOrCreate(serverPlayer).mimicryActive;
        }
        return player.getPersistentData().getBoolean(TAG_MIMICRY_ACTIVE);
    }

    /**
     * 客户端镜像拟态开关（由 Phase 2 的 S2C 包调用）。
     */
    public static void applyClientActive(Player player, boolean active)
    {
        player.getPersistentData().putBoolean(TAG_MIMICRY_ACTIVE, active);
    }

    /**
     * 当前术式对应的拟态角色 ID；未开启拟态或无映射时返回 null。
     */
    public static String getActiveCharacterId(Player player)
    {
        if (!isActive(player))
        {
            return null;
        }
        return MimicryCharacterRegistry.resolveCharacterId(PlayerTechniqueData.getInnateTechnique(player));
    }

    /**
     * 切换拟态开关。仅在服务端调用。
     *
     * @return true 表示切换成功；false 表示当前术式无法拟态（如无术式、无角色映射）
     */
    public static boolean toggle(ServerPlayer player)
    {
        if (!PlayerSeanceTechniqueData.hasLearned(player))
        {
            player.displayClientMessage(
                    Component.literal("你还没有习得降灵术").withStyle(ChatFormatting.RED),
                    true
            );
            return false;
        }

        if (isActive(player))
        {
            setActive(player, false);
            player.displayClientMessage(
                    Component.literal("降灵术已解除").withStyle(ChatFormatting.GRAY),
                    true
            );
            return true;
        }

        String techniqueId = PlayerTechniqueData.getInnateTechnique(player);
        if (MimicryCharacterRegistry.resolveFromTechnique(techniqueId).isEmpty())
        {
            player.displayClientMessage(
                    Component.literal("当前术式无法降灵").withStyle(ChatFormatting.RED),
                    true
            );
            return false;
        }

        setActive(player, true);
        TechniqueDailyUpgradeLogic.onSeanceTechniqueUsed(player);
        player.displayClientMessage(
                Component.literal("降灵术已发动").withStyle(ChatFormatting.AQUA),
                true
        );
        return true;
    }

    /**
     * 强制关闭拟态（术式变为 none 等场景）。
     */
    public static void forceDisable(ServerPlayer player)
    {
        if (!isActive(player))
        {
            return;
        }
        setActive(player, false);
    }

    /**
     * 生得术式变更后的拟态状态维护。
     * <p>
     * - 新术式为 none 或无映射 → 强制关闭
     * - 拟态仍开启且新术式有映射 → 保持开启（外观由新术式推导，Phase 2 负责广播）
     */
    public static void onTechniqueChanged(ServerPlayer player, String newTechniqueId)
    {
        if (!isActive(player))
        {
            return;
        }

        if (MimicryCharacterRegistry.resolveCharacterId(newTechniqueId) == null)
        {
            forceDisable(player);
            return;
        }
        ModNetwork.sendMimicrySync(player);
    }

    private static void setActive(ServerPlayer player, boolean active)
    {
        JjkzPlayerDataStorage.PlayerDataEntry data = JjkzPlayerDataStorage.get(player).getOrCreate(player);
        if (data.mimicryActive == active)
        {
            return;
        }
        data.mimicryActive = active;
        JjkzPlayerDataStorage.get(player).setDirty();
        ModNetwork.sendMimicrySync(player);
        CursedGradeBonuses.applyAll(player);
        if (PlayerTechniqueData.TECHNIQUE_SHRINE.equals(PlayerTechniqueData.getInnateTechnique(player)))
        {
            TechniqueSkillSetRegistry.syncSkillBarToClient(player);
        }
    }
}
