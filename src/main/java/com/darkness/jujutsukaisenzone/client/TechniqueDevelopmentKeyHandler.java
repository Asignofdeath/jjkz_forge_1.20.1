package com.darkness.jujutsukaisenzone.client;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.client.gui.technique.TechniqueDevelopmentUI;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * 打开「术式开发」全屏画布（默认 Y，可在控制设置中改键）。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, value = Dist.CLIENT)
public final class TechniqueDevelopmentKeyHandler
{
    public static final KeyMapping KEY_MAPPING = new KeyMapping(
            "key.jujutsukaisenzone.technique_development",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Y,
            "key.categories.jujutsukaisenzone"
    );

    private static boolean wasKeyDown;

    private TechniqueDevelopmentKeyHandler()
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
            wasKeyDown = false;
            return;
        }

        boolean isDown = KEY_MAPPING.isDown();
        if (isDown && !wasKeyDown)
        {
            TechniqueDevelopmentUI.open();
        }
        wasKeyDown = isDown;
    }
}
