package com.darkness.jujutsukaisenzone.technique.commonskill;

import com.darkness.jujutsukaisenzone.compat.EpicFightCompat;
import com.darkness.jujutsukaisenzone.network.ModNetwork;
import com.darkness.jujutsukaisenzone.player.PlayerReverseTechniqueData;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/**
 * 反转术式独立按键技能（默认 G）。
 * <p>
 * 业务意图：该技能不隶属于任何生得术式，独立于术式槽位系统。
 * 按住 G 触发反转术式；按住 Ctrl+G 触发结界修补。
 */
public final class ReverseTechniqueCommonKeySkill implements ICommonKeySkill
{
    public static final ReverseTechniqueCommonKeySkill INSTANCE = new ReverseTechniqueCommonKeySkill();

    private final KeyMapping reverseTechniqueKey = new KeyMapping(
            "key.jujutsukaisenzone.reverse_technique",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            "key.categories.jujutsukaisenzone"
    );

    private ReverseTechniqueCommonKeySkill()
    {
    }

    @Override
    public String skillId()
    {
        return "reverse_technique";
    }

    @Override
    public KeyMapping keyMapping()
    {
        return reverseTechniqueKey;
    }

    @Override
    public boolean canTriggerClient(LocalPlayer player)
    {
        return player.isAlive()
                && EpicFightCompat.isBattleMode(player)
                && PlayerReverseTechniqueData.hasReverseTechnique(player);
    }

    @Override
    public void onKeyHoldClientTick(Minecraft minecraft, LocalPlayer player)
    {
        if (isRepairModifierPressed() && canTriggerClient(player))
        {
            ModNetwork.sendRepairBarrierToServer();
        }
        else
        {
            ModNetwork.sendReverseTechniqueTickToServer();
        }
    }

    private static boolean isRepairModifierPressed()
    {
        long window = Minecraft.getInstance().getWindow().getWindow();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
    }
}
