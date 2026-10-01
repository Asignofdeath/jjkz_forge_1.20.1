package com.darkness.jujutsukaisenzone.technique.skill;

import com.darkness.jujutsukaisenzone.player.JjkzPlayerDataDisplay;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import com.darkness.jujutsukaisenzone.technique.development.SkillUpgradeLogic;
import com.darkness.jujutsukaisenzone.technique.skillset.JudgmentTechniqueSkillSet;
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
    /** 准星选目标最大距离（格）——Lv1 基值。 */
    private static final double TARGET_MAX_DISTANCE = 32.0D;

    private JudgmentViewFileSkillLogic()
    {
    }

    public static boolean hasCrosshairPlayerTarget(ServerPlayer caster)
    {
        return resolveCrosshairPlayer(caster, targetMaxDistance(caster)) != null;
    }

    public static boolean tryViewFile(ServerPlayer caster)
    {
        ServerPlayer target = resolveCrosshairPlayer(caster, targetMaxDistance(caster));
        if (target == null)
        {
            caster.displayClientMessage(Component.literal("请对准一名玩家").withStyle(ChatFormatting.YELLOW), true);
            return false;
        }

        JjkzPlayerDataDisplay.sendToViewer(caster, target);
        return true;
    }

    /** 准星距离：Lv1=32，每级 +2。 */
    public static double targetMaxDistance(ServerPlayer caster)
    {
        return TARGET_MAX_DISTANCE
                + 2.0D * SkillUpgradeLogic.upgradeSteps(
                caster, PlayerTechniqueData.TECHNIQUE_JUDGMENT, JudgmentTechniqueSkillSet.SKILL_VIEW_FILE);
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
