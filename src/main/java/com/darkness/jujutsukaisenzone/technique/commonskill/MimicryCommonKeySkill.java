package com.darkness.jujutsukaisenzone.technique.commonskill;

import com.darkness.jujutsukaisenzone.network.ModNetwork;
import com.darkness.jujutsukaisenzone.player.PlayerSeanceTechniqueData;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import com.darkness.jujutsukaisenzone.player.mimicry.MimicryCharacterRegistry;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/**
 * 降灵术独立按键（默认 O）：按下瞬间切换当前术式对应角色外观。
 * <p>
 * 需先习得降灵术；维持期间按等级减免咒力消耗。不要求 Epic Fight 战斗模式。
 */
public final class MimicryCommonKeySkill implements ICommonKeySkill
{
    public static final MimicryCommonKeySkill INSTANCE = new MimicryCommonKeySkill();

    private final KeyMapping mimicryKey = new KeyMapping(
            "key.jujutsukaisenzone.mimicry",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_O,
            "key.categories.jujutsukaisenzone"
    );

    private MimicryCommonKeySkill()
    {
    }

    @Override
    public String skillId()
    {
        return "mimicry";
    }

    @Override
    public KeyMapping keyMapping()
    {
        return mimicryKey;
    }

    @Override
    public boolean canTriggerClient(LocalPlayer player)
    {
        if (!player.isAlive() || !PlayerSeanceTechniqueData.hasLearned(player))
        {
            return false;
        }
        String techniqueId = PlayerTechniqueData.getInnateTechnique(player);
        return MimicryCharacterRegistry.resolveFromTechnique(techniqueId).isPresent();
    }

    @Override
    public boolean triggerOnPress()
    {
        return true;
    }

    @Override
    public void onKeyPressClientTick(Minecraft minecraft, LocalPlayer player)
    {
        ModNetwork.sendToggleMimicryToServer();
    }

    @Override
    public void onKeyHoldClientTick(Minecraft minecraft, LocalPlayer player)
    {
    }
}
