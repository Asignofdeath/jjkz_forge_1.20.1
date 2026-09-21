package com.darkness.jujutsukaisenzone.technique.skillset;

import com.darkness.jujutsukaisenzone.effect.TechniqueMeltdownEffect;
import com.darkness.jujutsukaisenzone.technique.commonskill.CommonTechniqueSkillRegistry;
import com.darkness.jujutsukaisenzone.network.ModNetwork;
import com.darkness.jujutsukaisenzone.technique.domain.JudgmentDomainExpansion;
import com.darkness.jujutsukaisenzone.technique.development.SkillUnlockLogic;
import com.darkness.jujutsukaisenzone.technique.development.TechniqueSkillTreeRegistry;
import com.darkness.jujutsukaisenzone.player.PlayerHoldReleaseChargeData;
import com.darkness.jujutsukaisenzone.player.PlayerMaxOutputChargeData;
import com.darkness.jujutsukaisenzone.player.PlayerSkillBarConfig;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 按 {@link PlayerTechniqueData#getInnateTechnique} 分发到对应术式实现。
 */
public final class TechniqueSkillSetRegistry
{
    private static final Logger LOGGER = LoggerFactory.getLogger(TechniqueSkillSetRegistry.class);
    private static final Map<String, IInnateTechniqueSkillSet> REGISTRY = new ConcurrentHashMap<>();

    private TechniqueSkillSetRegistry()
    {
    }

    public static void register(IInnateTechniqueSkillSet skillSet)
    {
        String id = skillSet.techniqueId();
        IInnateTechniqueSkillSet previous = REGISTRY.put(id, skillSet);
        if (previous != null)
        {
            LOGGER.warn("术式技能集重复注册，已覆盖：{}", id);
        }
        skillSet.getSkillTree().ifPresent(TechniqueSkillTreeRegistry::register);
    }

    public static Optional<IInnateTechniqueSkillSet> get(String techniqueId)
    {
        if (techniqueId == null || techniqueId.isBlank() || PlayerTechniqueData.TECHNIQUE_NONE.equals(techniqueId))
        {
            return Optional.empty();
        }
        return Optional.ofNullable(REGISTRY.get(techniqueId));
    }

    /** 已注册的可觉醒生得术式 id 列表（用于阵营选择后随机觉醒）。 */
    public static List<String> getRegisteredTechniqueIds()
    {
        return List.copyOf(REGISTRY.keySet());
    }

    /**
     * 按快捷栏位编号释放技能：先查 {@link PlayerSkillBarConfig} 获取该栏位配置的 skillId，
     * 再调用 {@link IInnateTechniqueSkillSet#useSkill} 按 skillId 驱动释放。
     * 未配置则无操作。
     */
    public static void useSkillSlot(ServerPlayer player, int slot)
    {
        if (isTechniqueSkillUseBlocked(player))
        {
            return;
        }
        if (PlayerMaxOutputChargeData.isCharging(player))
        {
            return;
        }
        if (PlayerHoldReleaseChargeData.isCharging(player))
        {
            return;
        }
        String techniqueId = PlayerTechniqueData.getInnateTechnique(player);
        if (techniqueId.isBlank() || PlayerTechniqueData.TECHNIQUE_NONE.equals(techniqueId))
        {
            return;
        }
        String skillId = PlayerSkillBarConfig.getSkillId(player, techniqueId, slot);
        if (skillId.isEmpty())
        {
            return;
        }
        if (CommonTechniqueSkillRegistry.isRegistered(skillId))
        {
            if (!SkillUnlockLogic.findSkillDefinition(techniqueId, skillId)
                    .filter(skill -> CommonTechniqueSkillRegistry.isUnlocked(player, skill.skillId()))
                    .isPresent())
            {
                return;
            }
            CommonTechniqueSkillRegistry.useSkill(player, skillId);
            return;
        }
        get(techniqueId).ifPresent(set ->
        {
            if (!SkillUnlockLogic.findSkillDefinition(techniqueId, skillId)
                    .filter(skill -> SkillUnlockLogic.isSkillBarEligible(player, techniqueId, skill, set))
                    .isPresent())
            {
                return;
            }
            if (set.getHoldReleaseChargeTicksBySkill(player, skillId) > 0)
            {
                return;
            }
            set.useSkill(player, skillId);
        });
    }

    /**
     * 修饰键 + 快捷栏位触发特别招式：先查该栏位配置的 skillId，
     * 再调用 {@link IInnateTechniqueSkillSet#useSpecialMove} 按 skillId 路由变体释放。
     */
    public static void useSpecialMove(ServerPlayer player, SpecialMoveInput input)
    {
        if (isTechniqueSkillUseBlocked(player))
        {
            return;
        }
        if (PlayerMaxOutputChargeData.isCharging(player))
        {
            return;
        }
        if (PlayerHoldReleaseChargeData.isCharging(player))
        {
            return;
        }
        String techniqueId = PlayerTechniqueData.getInnateTechnique(player);
        if (techniqueId.isBlank() || PlayerTechniqueData.TECHNIQUE_NONE.equals(techniqueId))
        {
            return;
        }
        String skillId = PlayerSkillBarConfig.getSkillId(player, techniqueId, input.baseSlot());
        if (skillId.isEmpty())
        {
            return;
        }
        get(techniqueId).ifPresent(set -> set.useSpecialMove(player, skillId, input.modifierMask()));
    }

    /** 最大输出蓄力开始：查快捷栏配置判定该栏位技能是否支持蓄力，支持则开始计时。 */
    public static void beginMaxOutputCharge(ServerPlayer player, int slot)
    {
        if (isTechniqueSkillUseBlocked(player))
        {
            return;
        }
        String techniqueId = PlayerTechniqueData.getInnateTechnique(player);
        if (techniqueId.isBlank() || PlayerTechniqueData.TECHNIQUE_NONE.equals(techniqueId)) return;
        String skillId = PlayerSkillBarConfig.getSkillId(player, techniqueId, slot);
        if (skillId.isEmpty()) return;
        if (PlayerMaxOutputChargeData.isCharging(player)) return;
        if (PlayerHoldReleaseChargeData.isCharging(player)) return;
        get(techniqueId).ifPresent(set -> set.beginMaxOutputChargeBySkill(player, skillId, slot));
    }

    public static void beginHoldReleaseCharge(ServerPlayer player, int slot)
    {
        if (isTechniqueSkillUseBlocked(player))
        {
            return;
        }
        String techniqueId = PlayerTechniqueData.getInnateTechnique(player);
        if (techniqueId.isBlank() || PlayerTechniqueData.TECHNIQUE_NONE.equals(techniqueId))
        {
            return;
        }
        String skillId = PlayerSkillBarConfig.getSkillId(player, techniqueId, slot);
        if (skillId.isEmpty())
        {
            return;
        }
        if (PlayerMaxOutputChargeData.isCharging(player) || PlayerHoldReleaseChargeData.isCharging(player))
        {
            return;
        }
        get(techniqueId).ifPresent(set ->
        {
            if (set.getHoldReleaseChargeTicksBySkill(player, skillId) <= 0)
            {
                return;
            }
            if (!SkillUnlockLogic.findSkillDefinition(techniqueId, skillId)
                    .filter(skill -> SkillUnlockLogic.isSkillBarEligible(player, techniqueId, skill, set))
                    .isPresent())
            {
                return;
            }
            set.beginHoldReleaseChargeBySkill(player, skillId, slot);
        });
    }

    public static void cancelHoldReleaseCharge(ServerPlayer player, int slot)
    {
        if (!PlayerHoldReleaseChargeData.isCharging(player) || PlayerHoldReleaseChargeData.getChargingSlot(player) != slot)
        {
            return;
        }
        String techniqueId = PlayerTechniqueData.getInnateTechnique(player);
        String skillId = PlayerHoldReleaseChargeData.getChargingSkillId(player);
        get(techniqueId).ifPresent(set -> set.cancelHoldReleaseChargeBySkill(player, skillId, slot));
    }

    public static void releaseHoldReleaseCharge(ServerPlayer player, int slot)
    {
        if (isTechniqueSkillUseBlocked(player))
        {
            return;
        }
        if (!PlayerHoldReleaseChargeData.isCharging(player) || PlayerHoldReleaseChargeData.getChargingSlot(player) != slot)
        {
            return;
        }
        String techniqueId = PlayerTechniqueData.getInnateTechnique(player);
        String skillId = PlayerHoldReleaseChargeData.getChargingSkillId(player);
        get(techniqueId).ifPresent(set -> set.releaseHoldReleaseChargeBySkill(player, skillId, slot));
    }

    public static void cancelMaxOutputCharge(ServerPlayer player, int slot)
    {
        get(PlayerTechniqueData.getInnateTechnique(player)).ifPresent(set -> set.cancelMaxOutputCharge(player, slot));
    }

    /** 最大输出释放：蓄力满后查快捷栏配置获取 skillId，调用 skillId 驱动的最大输出释放。 */
    public static void castMaxOutput(ServerPlayer player, int slot)
    {
        if (isTechniqueSkillUseBlocked(player))
        {
            return;
        }
        String techniqueId = PlayerMaxOutputChargeData.getChargeTechniqueId(player);
        if (techniqueId.isBlank()) techniqueId = PlayerTechniqueData.getInnateTechnique(player);
        if (techniqueId.isBlank() || PlayerTechniqueData.TECHNIQUE_NONE.equals(techniqueId))
        {
            PlayerMaxOutputChargeData.clear(player);
            return;
        }
        String skillId = PlayerSkillBarConfig.getSkillId(player, techniqueId, slot);
        if (skillId.isEmpty())
        {
            PlayerMaxOutputChargeData.clear(player);
            return;
        }
        get(techniqueId).ifPresentOrElse(
                set -> set.castMaxOutputBySkill(player, skillId, slot),
                () -> PlayerMaxOutputChargeData.clear(player)
        );
    }

    /** 受击打断：清空蓄力状态。 */
    public static void interruptMaxOutputFromDamage(ServerPlayer player)
    {
        if (!PlayerMaxOutputChargeData.isCharging(player))
        {
            return;
        }
        PlayerMaxOutputChargeData.clear(player);
        ModNetwork.sendMaxOutputChargeInterruptSync(player);
        player.sendSystemMessage(Component.literal("最大输出蓄力被打断。"));
    }

    /** 受击打断长按蓄力技能。 */
    public static void interruptHoldReleaseFromDamage(ServerPlayer player)
    {
        if (!PlayerHoldReleaseChargeData.isCharging(player))
        {
            return;
        }
        int slot = PlayerHoldReleaseChargeData.getChargingSlot(player);
        cancelHoldReleaseCharge(player, slot);
        ModNetwork.sendMaxOutputChargeInterruptSync(player);
        player.sendSystemMessage(Component.literal("蓄力被打断。"));
    }

    public static int adjustCastRangeTier(ServerPlayer player, int delta)
    {
        Optional<IInnateTechniqueSkillSet> set = get(PlayerTechniqueData.getInnateTechnique(player));
        if (set.isEmpty() || !set.get().supportsCastRangeTiers())
        {
            return set.map(s -> s.getCastRangeTier(player)).orElse(4);
        }
        return set.get().adjustCastRangeTier(player, delta);
    }

    public static int getCastRangeTier(ServerPlayer player)
    {
        return get(PlayerTechniqueData.getInnateTechnique(player))
                .map(s -> s.getCastRangeTier(player))
                .orElse(4);
    }

    /** 确保玩家当前术式的快捷栏配置有默认值（仅填充空栏位）。 */
    public static void ensureSkillBarDefaults(ServerPlayer player)
    {
        String techniqueId = PlayerTechniqueData.getInnateTechnique(player);
        if (techniqueId.isBlank() || PlayerTechniqueData.TECHNIQUE_NONE.equals(techniqueId))
        {
            return;
        }
        get(techniqueId).ifPresent(set ->
        {
            PlayerSkillBarConfig.fillDefaults(
                    player,
                    techniqueId,
                    SkillUnlockLogic.getUnlockedConfigurableSkills(player, techniqueId));
            PlayerSkillBarConfig.sanitizeSlots(player, techniqueId);
        });
    }

    /** 将当前术式快捷栏配置同步到客户端。 */
    public static void syncSkillBarToClient(ServerPlayer player)
    {
        String techniqueId = PlayerTechniqueData.getInnateTechnique(player);
        if (techniqueId.isBlank() || PlayerTechniqueData.TECHNIQUE_NONE.equals(techniqueId))
        {
            return;
        }
        ensureSkillBarDefaults(player);
        String[] skillIds = new String[9];
        for (int i = 0; i < 9; i++)
        {
            skillIds[i] = PlayerSkillBarConfig.getSkillId(player, techniqueId, i + 1);
        }
        ModNetwork.sendSkillBarConfigSync(player, techniqueId, skillIds);
    }

    private static boolean isTechniqueSkillUseBlocked(ServerPlayer player)
    {
        return TechniqueMeltdownEffect.blockTechniqueSkillUse(player)
                || JudgmentDomainExpansion.blockTechniqueSkillUse(player);
    }
}
