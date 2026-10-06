package com.darkness.jujutsukaisenzone.player;

import com.darkness.jujutsukaisenzone.network.ModNetwork;
import com.darkness.jujutsukaisenzone.technique.skillset.TechniqueSkillSetRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public final class PlayerTechniqueData
{
    public static final String TECHNIQUE_NONE = "none";
    public static final String TECHNIQUE_LIMITLESS = "limitless";
    public static final String TECHNIQUE_SHRINE = "shrine";
    public static final String TECHNIQUE_JUDGMENT = "judgment";
    public static final String TECHNIQUE_LOVE_RENDEZVOUS = "love_rendezvous";
    public static final String TECHNIQUE_RATIO = "ratio";
    public static final String TECHNIQUE_TEN_SHADOWS = "ten_shadows";
    private static final String TAG_INNATE_TECHNIQUE = "jujutsukaisenzone_innate_technique";

    private PlayerTechniqueData()
    {
    }

    public static void setInnateTechnique(Player player, String techniqueId)
    {
        String normalized = normalizeTechniqueId(techniqueId);
        if (!TECHNIQUE_NONE.equals(normalized) && !PlayerFactionData.canBindInnateTechnique(player))
        {
            return;
        }
        if (player instanceof ServerPlayer serverPlayer)
        {
            JjkzPlayerDataStorage.PlayerDataEntry data = JjkzPlayerDataStorage.get(serverPlayer).getOrCreate(serverPlayer);
            data.innateTechnique = normalized;
            JjkzPlayerDataStorage.get(serverPlayer).setDirty();
            ModNetwork.sendTechniqueSync(serverPlayer, normalized);
            TechniqueSkillSetRegistry.syncSkillBarToClient(serverPlayer);
            PlayerMimicryData.onTechniqueChanged(serverPlayer, normalized);
            PlayerTenShadowsModeData.clearIfTechniqueChanged(serverPlayer, normalized);
            PlayerDomainModeData.clearIfTechniqueChanged(serverPlayer, normalized);
            if (!TECHNIQUE_NONE.equals(normalized))
            {
                com.darkness.jujutsukaisenzone.talent.InnateTalentLottery.grantGeniusCommonsIfNeeded(serverPlayer);
            }
            return;
        }
        player.getPersistentData().putString(TAG_INNATE_TECHNIQUE, normalized);
    }

    public static void applyClientTechnique(Player player, String techniqueId)
    {
        player.getPersistentData().putString(TAG_INNATE_TECHNIQUE, normalizeTechniqueId(techniqueId));
    }

    public static String getInnateTechnique(Player player)
    {
        if (player instanceof ServerPlayer serverPlayer)
        {
            String techniqueId = JjkzPlayerDataStorage.get(serverPlayer).getOrCreate(serverPlayer).innateTechnique;
            return normalizeTechniqueId(techniqueId);
        }
        String techniqueId = player.getPersistentData().getString(TAG_INNATE_TECHNIQUE);
        return normalizeTechniqueId(techniqueId);
    }

    public static boolean isLimitless(Player player)
    {
        return TECHNIQUE_LIMITLESS.equals(getInnateTechnique(player));
    }

    public static boolean isShrine(Player player)
    {
        return TECHNIQUE_SHRINE.equals(getInnateTechnique(player));
    }

    public static boolean isJudgment(Player player)
    {
        return TECHNIQUE_JUDGMENT.equals(getInnateTechnique(player));
    }

    public static boolean isLoveRendezvous(Player player)
    {
        return TECHNIQUE_LOVE_RENDEZVOUS.equals(getInnateTechnique(player));
    }

    public static boolean isRatio(Player player)
    {
        return TECHNIQUE_RATIO.equals(getInnateTechnique(player));
    }

    public static boolean isTenShadows(Player player)
    {
        return TECHNIQUE_TEN_SHADOWS.equals(getInnateTechnique(player));
    }

    /**
     * 随机觉醒生得术式。需已是可绑定阵营；已有术式、天与咒缚或不可绑定阵营时跳过。
     * 选阵营时不会自动调用，由叙事/指令在需要时触发。
     *
     * @return 是否成功觉醒
     */
    public static boolean tryRandomAwakenInnateTechnique(ServerPlayer player)
    {
        if (!PlayerFactionData.canBindInnateTechnique(player))
        {
            return false;
        }
        if (PlayerCursedEnergyData.isHeavenlyRestricted(player))
        {
            player.sendSystemMessage(Component.literal("天与咒缚状态下无法觉醒生得术式"));
            return false;
        }
        if (!TECHNIQUE_NONE.equals(getInnateTechnique(player)))
        {
            return false;
        }

        List<String> pool = TechniqueSkillSetRegistry.getRegisteredTechniqueIds();
        if (pool.isEmpty())
        {
            return false;
        }

        String chosen = pool.get(player.getRandom().nextInt(pool.size()));
        setInnateTechnique(player, chosen);
        player.sendSystemMessage(Component.literal("你觉醒了生得术式：【" + getDisplayName(chosen) + "】"));
        return true;
    }

    public static String getDisplayName(String techniqueId)
    {
        return switch (normalizeTechniqueId(techniqueId))
        {
            case TECHNIQUE_LIMITLESS -> "无下限";
            case TECHNIQUE_SHRINE -> "御厨子";
            case TECHNIQUE_JUDGMENT -> "审判";
            case TECHNIQUE_LOVE_RENDEZVOUS -> "星间飞行";
            case TECHNIQUE_RATIO -> "十划咒法";
            case TECHNIQUE_TEN_SHADOWS -> "十种影法术";
            default -> techniqueId;
        };
    }
    private static String normalizeTechniqueId(String techniqueId)
    {
        if (techniqueId == null || techniqueId.isBlank())
        {
            return TECHNIQUE_NONE;
        }
        if ("sukuna".equals(techniqueId))
        {
            return TECHNIQUE_SHRINE;
        }
        return techniqueId;
    }
}
