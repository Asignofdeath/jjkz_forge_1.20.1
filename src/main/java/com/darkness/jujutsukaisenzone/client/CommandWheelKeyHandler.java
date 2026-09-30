package com.darkness.jujutsukaisenzone.client;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.client.gui.commandwheel.CommandWheelScreen;
import com.darkness.jujutsukaisenzone.commandwheel.CommandWheelCatalog;
import com.darkness.jujutsukaisenzone.compat.EpicFightCompat;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * 指令圆盘热键：按住打开 {@link CommandWheelScreen}，松手在 Screen 内确认。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class CommandWheelKeyHandler
{
    public static final KeyMapping KEY_MAPPING = new KeyMapping(
            "key.jujutsukaisenzone.command_wheel",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "key.categories.jujutsukaisenzone"
    );

    private static boolean wasDown;

    private CommandWheelKeyHandler()
    {
    }

    /**
     * 物理按键是否仍按下。圆盘 Screen 打开后 {@link KeyMapping#isDown()} 在
     * {@link KeyConflictContext#IN_GAME} 下会立刻变 false，不能用来检测松手。
     */
    public static boolean isBoundKeyPhysicallyDown()
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getWindow() == null)
        {
            return false;
        }
        long window = mc.getWindow().getWindow();
        InputConstants.Key key = KEY_MAPPING.getKey();
        if (key.getType() == InputConstants.Type.MOUSE)
        {
            return GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
        }
        return GLFW.glfwGetKey(window, key.getValue()) == GLFW.GLFW_PRESS;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END)
        {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null)
        {
            wasDown = false;
            return;
        }
        /* 打开用物理键：避免与 Screen 内状态打架 */
        boolean down = isBoundKeyPhysicallyDown();
        if (down && !wasDown)
        {
            if (mc.screen == null
                    && (EpicFightCompat.isBattleMode(mc.player)
                        || CommandWheelCatalog.isAimingAtMahoraga(mc.player, mc.hitResult))
                    && mc.player.isAlive()
                    && !mc.player.isSpectator())
            {
                mc.setScreen(new CommandWheelScreen());
            }
        }
        wasDown = down;
    }

    @Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Register
    {
        private Register()
        {
        }

        @SubscribeEvent
        public static void onRegisterKeys(RegisterKeyMappingsEvent event)
        {
            event.register(KEY_MAPPING);
        }
    }
}
