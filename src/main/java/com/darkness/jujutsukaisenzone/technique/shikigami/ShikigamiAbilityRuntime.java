package com.darkness.jujutsukaisenzone.technique.shikigami;

import com.darkness.jujutsukaisenzone.network.ModNetwork;
import com.darkness.jujutsukaisenzone.player.PlayerCursedEnergyData;
import com.darkness.jujutsukaisenzone.player.PlayerShikigamiCooldownData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 式神招式统一结算：在场 → 公用 CD → 咒力 → 效果 → 写桶。
 * <p>
 * 与热键栏特别招式经济链路无关。
 */
public final class ShikigamiAbilityRuntime
{
    /** 本 tick 术师主动放过的「玩家 UUID → 槽位 id」。自动侧同 tick 跳过该槽。 */
    private static final Map<UUID, PlayerCastFrame> PLAYER_CASTS = new ConcurrentHashMap<>();

    private ShikigamiAbilityRuntime()
    {
    }

    public static boolean tryCast(
            ServerPlayer owner,
            ShikigamiMode mode,
            ShikigamiAbility ability,
            ShikigamiAbilitySource source)
    {
        if (owner == null || mode == null || ability == null || source == null)
        {
            return false;
        }
        if (!mode.isActive(owner) && source == ShikigamiAbilitySource.PLAYER)
        {
            return false;
        }
        if (source == ShikigamiAbilitySource.PLAYER && !ability.castMode().allowsPlayer())
        {
            return false;
        }
        if (source == ShikigamiAbilitySource.AUTO && !ability.castMode().allowsAuto())
        {
            return false;
        }
        if (!mode.hasAliveOnField(owner, ability.shikigamiSlotId()))
        {
            if (source == ShikigamiAbilitySource.PLAYER)
            {
                owner.displayClientMessage(
                        Component.literal("场上没有该式神").withStyle(ChatFormatting.GRAY),
                        true);
            }
            return false;
        }

        long now = owner.level().getGameTime();
        int remain = PlayerShikigamiCooldownData.getRemainingTicks(
                owner, mode.modeId(), ability.shikigamiSlotId(), now);
        if (remain > 0)
        {
            if (source == ShikigamiAbilitySource.PLAYER)
            {
                owner.displayClientMessage(
                        Component.literal("式神冷却中：" + formatSeconds(remain) + " 秒")
                                .withStyle(ChatFormatting.GRAY),
                        true);
            }
            return false;
        }

        int cost = ability.cursedEnergyCost();
        boolean charged = false;
        if (cost > 0 && !owner.isCreative())
        {
            if (!PlayerCursedEnergyData.tryConsume(owner, cost))
            {
                if (source == ShikigamiAbilitySource.PLAYER)
                {
                    owner.displayClientMessage(
                            Component.literal("咒力不足").withStyle(ChatFormatting.RED),
                            true);
                }
                return false;
            }
            charged = true;
        }

        boolean ok;
        try
        {
            ok = ability.effect().execute(owner, ability, source);
        }
        catch (RuntimeException ex)
        {
            if (charged)
            {
                refund(owner, cost);
            }
            throw ex;
        }

        if (!ok)
        {
            if (charged)
            {
                refund(owner, cost);
            }
            return false;
        }

        if (ability.cooldownTicks() > 0)
        {
            PlayerShikigamiCooldownData.setCooldown(
                    owner,
                    mode.modeId(),
                    ability.shikigamiSlotId(),
                    now + ability.cooldownTicks());
        }
        if (source == ShikigamiAbilitySource.PLAYER)
        {
            markPlayerCast(owner, ability.shikigamiSlotId());
        }
        return true;
    }

    /** 客户端主动包入口：按槽索引 + 修饰键掩码查找并释放。 */
    public static boolean tryPlayerCast(ServerPlayer owner, int shikigamiIndex, int modifierMask)
    {
        if (owner == null || modifierMask == 0)
        {
            return false;
        }
        Optional<ShikigamiMode> modeOpt = ShikigamiModeRegistry.findActive(owner);
        if (modeOpt.isEmpty())
        {
            return false;
        }
        ShikigamiMode mode = modeOpt.get();
        Optional<String> slotOpt = mode.slotId(shikigamiIndex);
        if (slotOpt.isEmpty())
        {
            return false;
        }
        Optional<ShikigamiAbility> abilityOpt = mode.findPlayerAbility(slotOpt.get(), modifierMask);
        if (abilityOpt.isEmpty())
        {
            return false;
        }
        return tryCast(owner, mode, abilityOpt.get(), ShikigamiAbilitySource.PLAYER);
    }

    /**
     * 自动侧：对主人当前模式场上各式神，按优先级尝试一招。
     * 本 tick 已主动放过的槽跳过。
     */
    public static void tickAuto(ServerPlayer owner)
    {
        if (owner == null || !owner.isAlive())
        {
            return;
        }
        Optional<ShikigamiMode> modeOpt = ShikigamiModeRegistry.findActive(owner);
        if (modeOpt.isEmpty())
        {
            /* 模式关闭时场上式神仍可自动出手：找十种影等「有场上式神」的模式 */
            modeOpt = findModeWithField(owner);
        }
        if (modeOpt.isEmpty())
        {
            return;
        }
        ShikigamiMode mode = modeOpt.get();
        long now = owner.level().getGameTime();
        java.util.LinkedHashSet<String> slots = new java.util.LinkedHashSet<>();
        for (ShikigamiAbility probe : mode.abilities())
        {
            slots.add(probe.shikigamiSlotId());
        }
        for (String slotId : slots)
        {
            if (!mode.hasAliveOnField(owner, slotId))
            {
                continue;
            }
            if (playerCastSlotThisTick(owner, slotId))
            {
                continue;
            }
            if (!PlayerShikigamiCooldownData.isReady(owner, mode.modeId(), slotId, now))
            {
                continue;
            }
            for (ShikigamiAbility ability : mode.autoAbilities(slotId))
            {
                if (tryCast(owner, mode, ability, ShikigamiAbilitySource.AUTO))
                {
                    break;
                }
            }
        }
    }

    /**
     * 自动招不要求模式展开（收回模式后场上式神仍应能撕咬）。
     * 若当前无 active 模式，回退到已注册且场上有式神的模式。
     */
    private static Optional<ShikigamiMode> findModeWithField(ServerPlayer owner)
    {
        for (ShikigamiMode mode : ShikigamiModeRegistry.all())
        {
            for (ShikigamiAbility ability : mode.abilities())
            {
                if (mode.hasAliveOnField(owner, ability.shikigamiSlotId()))
                {
                    return Optional.of(mode);
                }
            }
        }
        return Optional.empty();
    }

    private static void markPlayerCast(ServerPlayer owner, String shikigamiSlotId)
    {
        long now = owner.level().getGameTime();
        PLAYER_CASTS.compute(owner.getUUID(), (id, frame) ->
        {
            if (frame == null || frame.gameTime != now)
            {
                frame = new PlayerCastFrame(now);
            }
            frame.slotIds.add(shikigamiSlotId);
            return frame;
        });
    }

    private static boolean playerCastSlotThisTick(ServerPlayer owner, String shikigamiSlotId)
    {
        PlayerCastFrame frame = PLAYER_CASTS.get(owner.getUUID());
        if (frame == null || frame.gameTime != owner.level().getGameTime())
        {
            return false;
        }
        return frame.slotIds.contains(shikigamiSlotId);
    }

    private static void refund(ServerPlayer player, int amount)
    {
        if (amount <= 0)
        {
            return;
        }
        PlayerCursedEnergyData.setCurrent(player, PlayerCursedEnergyData.getCurrent(player) + amount);
        ModNetwork.sendCursedEnergySync(player, PlayerCursedEnergyData.getCurrent(player), PlayerCursedEnergyData.getMax(player));
    }

    private static String formatSeconds(int ticks)
    {
        return String.format("%.1f", ticks / 20.0D);
    }

    private static final class PlayerCastFrame
    {
        private final long gameTime;
        private final java.util.HashSet<String> slotIds = new java.util.HashSet<>();

        private PlayerCastFrame(long gameTime)
        {
            this.gameTime = gameTime;
        }
    }
}
