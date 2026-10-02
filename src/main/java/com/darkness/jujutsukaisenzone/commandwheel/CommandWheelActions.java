package com.darkness.jujutsukaisenzone.commandwheel;

import com.darkness.jujutsukaisenzone.compat.EpicFightCompat;
import com.darkness.jujutsukaisenzone.entity.MahoragaEntity;
import com.darkness.jujutsukaisenzone.player.PlayerDomainModeData;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import com.darkness.jujutsukaisenzone.player.PlayerTenShadowsModeData;
import com.darkness.jujutsukaisenzone.player.PlayerTenShadowsStanceData;
import com.darkness.jujutsukaisenzone.technique.domain.DomainExpansionRouter;
import com.darkness.jujutsukaisenzone.technique.skill.TenShadowsShikigamiAiLogic;
import com.darkness.jujutsukaisenzone.technique.skill.TenShadowsSummonLogic;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * 指令圆盘 C2S 服务端分发（通用页 + 术式专属页）。
 */
public final class CommandWheelActions
{
    private static final double ATTACK_ORDER_RANGE = 64.0D;

    private CommandWheelActions()
    {
    }

    public static void handle(ServerPlayer player, String entryId, @Nullable UUID targetUuid)
    {
        if (player == null || entryId == null || entryId.isEmpty())
        {
            return;
        }
        if (!player.isAlive() || player.isSpectator())
        {
            return;
        }
        if (CommandWheelIds.MAHORAGA_RIDE.equals(entryId))
        {
            rideMahoraga(player, targetUuid);
            return;
        }
        if (!EpicFightCompat.isBattleMode(player))
        {
            return;
        }
        switch (entryId)
        {
            case CommandWheelIds.DOMAIN_EXPANSION -> DomainExpansionRouter.getDomainForPlayer(player).cast(player);
            case CommandWheelIds.DOMAIN_MODE -> toggleDomainMode(player);
            case CommandWheelIds.TEN_SHADOWS_SHIKIGAMI_MODE -> toggleShikigamiMode(player);
            case CommandWheelIds.TEN_SHADOWS_STANCE_TOGGLE -> toggleStance(player);
            case CommandWheelIds.TEN_SHADOWS_ATTACK_ORDER -> attackOrder(player, targetUuid);
            default ->
            {
            }
        }
    }

    private static void toggleDomainMode(ServerPlayer player)
    {
        if (!PlayerDomainModeData.canUseDomainModeWheel(player))
        {
            return;
        }
        PlayerDomainModeData.Mode mode = PlayerDomainModeData.toggle(player);
        player.displayClientMessage(
                Component.translatable(PlayerDomainModeData.langKey(mode))
                        .withStyle(mode == PlayerDomainModeData.Mode.NORMAL
                                ? ChatFormatting.GRAY
                                : ChatFormatting.AQUA),
                true);
    }

    /**
     * 圆盘打开时准星命中即可锁定 UUID；确认时不再要求准星仍打在身上
     *（选扇区会偏视角，且魔虚罗可能走开），只校验归属与合理距离。
     */
    private static final double MAHORAGA_RIDE_CONFIRM_RANGE = 32.0D;

    private static void rideMahoraga(ServerPlayer player, @Nullable UUID targetUuid)
    {
        if (targetUuid == null || !PlayerTechniqueData.isTenShadows(player))
        {
            return;
        }
        Entity target = player.serverLevel().getEntity(targetUuid);
        if (!(target instanceof MahoragaEntity mahoraga)
                || !mahoraga.isAlive()
                || player.distanceToSqr(mahoraga) > MAHORAGA_RIDE_CONFIRM_RANGE * MAHORAGA_RIDE_CONFIRM_RANGE
                || !TenShadowsSummonLogic.isOwnedBy(mahoraga, player.getUUID()))
        {
            return;
        }
        if (player.getVehicle() == mahoraga)
        {
            player.stopRiding();
            return;
        }
        if (player.isPassenger() || mahoraga.isVehicle())
        {
            return;
        }
        mahoraga.beginRide(player);
    }

    private static void toggleShikigamiMode(ServerPlayer player)
    {
        if (!PlayerTechniqueData.isTenShadows(player))
        {
            return;
        }
        boolean active = PlayerTenShadowsModeData.toggle(player);
        player.displayClientMessage(
                Component.literal(active ? "影之式神 · 展开" : "影之式神 · 收回")
                        .withStyle(active ? ChatFormatting.DARK_PURPLE : ChatFormatting.GRAY),
                true);
    }

    private static void toggleStance(ServerPlayer player)
    {
        if (!PlayerTechniqueData.isTenShadows(player))
        {
            return;
        }
        PlayerTenShadowsStanceData.Stance stance = PlayerTenShadowsStanceData.toggle(player);
        boolean guard = stance == PlayerTenShadowsStanceData.Stance.GUARD;
        player.displayClientMessage(
                Component.translatable(guard
                                ? "command_wheel.jujutsukaisenzone.ten_shadows_stance_guard"
                                : "command_wheel.jujutsukaisenzone.ten_shadows_stance_assist")
                        .withStyle(guard ? ChatFormatting.AQUA : ChatFormatting.GOLD),
                true);
        if (guard)
        {
            TenShadowsShikigamiAiLogic.clearOrderedTargets(player);
        }
    }

    private static void attackOrder(ServerPlayer player, @Nullable UUID targetUuid)
    {
        if (!PlayerTechniqueData.isTenShadows(player) || !PlayerTenShadowsStanceData.isAssist(player))
        {
            player.displayClientMessage(
                    Component.translatable("command_wheel.jujutsukaisenzone.ten_shadows_attack_need_assist")
                            .withStyle(ChatFormatting.YELLOW),
                    true);
            return;
        }
        LivingEntity target = resolveEpicFightLockTarget(player, targetUuid);
        if (target == null)
        {
            player.displayClientMessage(
                    Component.translatable("command_wheel.jujutsukaisenzone.ten_shadows_attack_no_target")
                            .withStyle(ChatFormatting.YELLOW),
                    true);
            return;
        }
        if (TenShadowsSummonLogic.isOwnedBy(target, player.getUUID())
                || target.getUUID().equals(player.getUUID()))
        {
            return;
        }
        int ordered = TenShadowsShikigamiAiLogic.orderAttack(player, target);
        if (ordered <= 0)
        {
            player.displayClientMessage(
                    Component.translatable("command_wheel.jujutsukaisenzone.ten_shadows_attack_no_summons")
                            .withStyle(ChatFormatting.YELLOW),
                    true);
            return;
        }
        player.displayClientMessage(
                Component.translatable("command_wheel.jujutsukaisenzone.ten_shadows_attack_ok", target.getDisplayName())
                        .withStyle(ChatFormatting.GOLD),
                true);
    }

    @Nullable
    private static LivingEntity resolveEpicFightLockTarget(ServerPlayer player, @Nullable UUID targetUuid)
    {
        if (targetUuid == null)
        {
            return null;
        }
        Entity entity = player.serverLevel().getEntity(targetUuid);
        if (!(entity instanceof LivingEntity living)
                || !living.isAlive()
                || living.level() != player.level()
                || living.distanceToSqr(player) > ATTACK_ORDER_RANGE * ATTACK_ORDER_RANGE)
        {
            return null;
        }
        return living;
    }
}
