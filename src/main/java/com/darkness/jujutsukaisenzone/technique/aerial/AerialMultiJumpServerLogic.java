package com.darkness.jujutsukaisenzone.technique.aerial;

import com.darkness.jujutsukaisenzone.compat.EpicFightCompat;
import net.minecraft.server.level.ServerPlayer;

/**
 * 空战多段跳：空中按跳跃键再次起跳，消耗 Epic Fight 体力。
 */
public final class AerialMultiJumpServerLogic
{
    private AerialMultiJumpServerLogic()
    {
    }

    public static void tryJump(ServerPlayer player)
    {
        if (!AerialCombatSkillRegistry.isUnlocked(player, AerialCombatSkills.SKILL_AERIAL_MULTI_JUMP)
                || player.onGround()
                || player.getAbilities().flying
                || player.isFallFlying())
        {
            return;
        }
        if (!EpicFightCompat.tryConsumeAerialMultiJumpStamina(player))
        {
            return;
        }

        float jumpPower = 0.42F + player.getJumpBoostPower();
        player.setDeltaMovement(player.getDeltaMovement().x, jumpPower, player.getDeltaMovement().z);
        player.fallDistance = 0.0F;
        player.hasImpulse = true;
        player.hurtMarked = true;
    }
}
