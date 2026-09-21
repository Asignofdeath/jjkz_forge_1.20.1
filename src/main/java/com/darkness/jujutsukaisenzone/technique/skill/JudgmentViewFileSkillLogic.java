package com.darkness.jujutsukaisenzone.technique.skill;

import com.darkness.jujutsukaisenzone.player.JjkzPlayerDataDisplay;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 「查看档案」：准星选取玩家，在施法者聊天栏展示其 JJKZ 数据。
 */
public final class JudgmentViewFileSkillLogic
{
    /** 准星选目标最大距离（格）。 */
    private static final double TARGET_MAX_DISTANCE = 32.0D;

    private JudgmentViewFileSkillLogic()
    {
    }

    public static boolean hasCrosshairPlayerTarget(ServerPlayer caster)
    {
        return resolveCrosshairPlayer(caster, TARGET_MAX_DISTANCE) != null;
    }

    public static boolean tryViewFile(ServerPlayer caster)
    {
        ServerPlayer target = resolveCrosshairPlayer(caster, TARGET_MAX_DISTANCE);
        if (target == null)
        {
            caster.displayClientMessage(Component.literal("请对准一名玩家").withStyle(ChatFormatting.YELLOW), true);
            return false;
        }

        JjkzPlayerDataDisplay.sendToViewer(caster, target);
        return true;
    }

    private static ServerPlayer resolveCrosshairPlayer(ServerPlayer player, double maxDistance)
    {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(maxDistance));
        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(maxDistance)).inflate(1.0D);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                player,
                eye,
                end,
                searchBox,
                entity -> entity instanceof ServerPlayer targetPlayer
                        && targetPlayer.isAlive()
                        && !targetPlayer.isRemoved()
                        && !targetPlayer.getUUID().equals(player.getUUID())
                        && !targetPlayer.isSpectator()
                        && entity.isPickable(),
                maxDistance * maxDistance);
        if (hit != null && hit.getEntity() instanceof ServerPlayer targetPlayer)
        {
            return targetPlayer;
        }
        return null;
    }
}
