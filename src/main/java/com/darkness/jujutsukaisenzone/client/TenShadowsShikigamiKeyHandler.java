package com.darkness.jujutsukaisenzone.client;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.compat.EpicFightCompat;
import com.darkness.jujutsukaisenzone.network.ModNetwork;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import com.darkness.jujutsukaisenzone.player.PlayerTenShadowsModeData;
import com.darkness.jujutsukaisenzone.technique.shikigami.ShikigamiAbility;
import com.darkness.jujutsukaisenzone.technique.skill.TenShadowsShikigami;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * 影之式神模式：数字行 1～0。
 * <ul>
 *   <li>裸键：召出 / 收回式神</li>
 *   <li>修饰键 + 数字键：式神主动招（见 {@code ShikigamiAbilityRuntime}）</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class TenShadowsShikigamiKeyHandler
{
    private static final boolean[] LAST_KEY_DOWN = new boolean[TenShadowsShikigami.values().length];

    private TenShadowsShikigamiKeyHandler()
    {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END)
        {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null)
        {
            resetKeyStates();
            return;
        }
        if (!EpicFightCompat.isBattleMode(minecraft.player)
                || !PlayerTechniqueData.isTenShadows(minecraft.player)
                || !PlayerTenShadowsModeData.isShikigamiMode(minecraft.player))
        {
            resetKeyStates();
            return;
        }

        long window = minecraft.getWindow().getWindow();
        int modifierMask = readModifierMask(window);
        if (modifierMask != 0)
        {
            for (TenShadowsShikigami shikigami : TenShadowsShikigami.values())
            {
                boolean down = GLFW.glfwGetKey(window, shikigami.glfwKey()) == GLFW.GLFW_PRESS;
                boolean wasDown = LAST_KEY_DOWN[shikigami.index()];
                if (down && !wasDown)
                {
                    ModNetwork.sendShikigamiAbilityToServer(shikigami.index(), modifierMask);
                }
                LAST_KEY_DOWN[shikigami.index()] = down;
            }
            return;
        }

        for (TenShadowsShikigami shikigami : TenShadowsShikigami.values())
        {
            boolean down = GLFW.glfwGetKey(window, shikigami.glfwKey()) == GLFW.GLFW_PRESS;
            boolean wasDown = LAST_KEY_DOWN[shikigami.index()];
            if (down && !wasDown)
            {
                ModNetwork.sendTenShadowsShikigamiToServer(shikigami.index());
            }
            LAST_KEY_DOWN[shikigami.index()] = down;
        }
    }

    private static int readModifierMask(long window)
    {
        int mask = 0;
        if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT))
        {
            mask |= ShikigamiAbility.MOD_SHIFT;
        }
        if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_CONTROL))
        {
            mask |= ShikigamiAbility.MOD_CTRL;
        }
        if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_ALT)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_ALT))
        {
            mask |= ShikigamiAbility.MOD_ALT;
        }
        return mask;
    }

    private static void resetKeyStates()
    {
        for (int i = 0; i < LAST_KEY_DOWN.length; i++)
        {
            LAST_KEY_DOWN[i] = false;
        }
    }
}
