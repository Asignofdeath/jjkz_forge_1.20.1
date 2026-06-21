package com.darkness.jujutsukaisenzone.technique.commonskill;

import com.darkness.jujutsukaisenzone.compat.EpicFightCompat;
import com.darkness.jujutsukaisenzone.network.ModNetwork;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/**
 * 结界术独立按键技能（默认 B）。
 * <p>
 * 实现意图：
 * - 单独按下 B 键：触发普通结界术
 * - Alt + B 键：触发领域展开
 * <p>
 * 按下瞬间触发一次，避免按住键时重复构建导致高频改方块。
 */
public final class BarrierTechniqueCommonKeySkill implements ICommonKeySkill
{
    public static final BarrierTechniqueCommonKeySkill INSTANCE = new BarrierTechniqueCommonKeySkill();

    private final KeyMapping barrierTechniqueKey = new KeyMapping(
            "key.jujutsukaisenzone.barrier_technique",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            "key.categories.jujutsukaisenzone"
    );

    private BarrierTechniqueCommonKeySkill()
    {
    }

    @Override
    public String skillId()
    {
        return "barrier_technique";
    }

    @Override
    public KeyMapping keyMapping()
    {
        return barrierTechniqueKey;
    }

    @Override
    public boolean canTriggerClient(LocalPlayer player)
    {
        return player.isAlive() && EpicFightCompat.isBattleMode(player);
    }

    @Override
    public boolean triggerOnPress()
    {
        return true;
    }

    @Override
    public void onKeyPressClientTick(Minecraft minecraft, LocalPlayer player)
    {
        // 检查是否按下了 Alt 键
        long window = minecraft.getWindow().getWindow();
        boolean altDown = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS
                       || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_ALT) == GLFW.GLFW_PRESS;

        if (altDown)
        {
            // Alt + 结界术键：触发领域展开
            ModNetwork.sendDomainExpansionCastToServer();
        }
        else
        {
            // 单独结界术键：触发普通结界术
            ModNetwork.sendBarrierTechniqueCastToServer();
        }
    }

    @Override
    public void onKeyHoldClientTick(Minecraft minecraft, LocalPlayer player)
    {
    }
}
