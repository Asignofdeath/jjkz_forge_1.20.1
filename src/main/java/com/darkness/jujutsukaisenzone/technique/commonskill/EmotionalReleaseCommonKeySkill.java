package com.darkness.jujutsukaisenzone.technique.commonskill;

import com.darkness.jujutsukaisenzone.client.EmotionalReleaseClientState;
import com.darkness.jujutsukaisenzone.compat.EpicFightCompat;
import com.darkness.jujutsukaisenzone.network.ModNetwork;
import com.darkness.jujutsukaisenzone.network.packet.EmotionalReleaseC2SPacket;
import com.darkness.jujutsukaisenzone.player.PlayerCursedEnergyData;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/**
 * 释放情绪：按住 Z 蓄力恢复咒力（Epic Fight 战斗模式下）。
 */
public final class EmotionalReleaseCommonKeySkill implements ICommonKeySkill
{
    public static final EmotionalReleaseCommonKeySkill INSTANCE = new EmotionalReleaseCommonKeySkill();

    private final KeyMapping emotionalReleaseKey = new KeyMapping(
            "key.jujutsukaisenzone.emotional_release",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Z,
            "key.categories.jujutsukaisenzone"
    );

    private boolean beginSent;

    private EmotionalReleaseCommonKeySkill()
    {
    }

    @Override
    public String skillId()
    {
        return "emotional_release";
    }

    @Override
    public KeyMapping keyMapping()
    {
        return emotionalReleaseKey;
    }

    @Override
    public boolean canTriggerClient(LocalPlayer player)
    {
        if (!player.isAlive() || !EpicFightCompat.isBattleMode(player))
        {
            return false;
        }
        if (PlayerCursedEnergyData.isHeavenlyRestricted(player))
        {
            return false;
        }
        if (EmotionalReleaseClientState.isCharging())
        {
            return true;
        }
        if (PlayerCursedEnergyData.getCurrent(player) >= PlayerCursedEnergyData.getMax(player))
        {
            return false;
        }
        long gameTime = player.level().getGameTime();
        return !EmotionalReleaseClientState.isOnCooldown(gameTime);
    }

    @Override
    public void onKeyHoldClientTick(Minecraft minecraft, LocalPlayer player)
    {
        if (!beginSent && !EmotionalReleaseClientState.isCharging())
        {
            ModNetwork.sendEmotionalReleaseToServer(EmotionalReleaseC2SPacket.ACTION_BEGIN);
            beginSent = true;
        }
    }

    @Override
    public void onKeyReleaseClientTick(Minecraft minecraft, LocalPlayer player)
    {
        beginSent = false;
        if (EmotionalReleaseClientState.isCharging())
        {
            ModNetwork.sendEmotionalReleaseToServer(EmotionalReleaseC2SPacket.ACTION_RELEASE);
        }
    }

    /** 按键状态被外部重置时（如受击打断）清理本地发送标记。 */
    public void resetBeginSent()
    {
        beginSent = false;
    }
}
