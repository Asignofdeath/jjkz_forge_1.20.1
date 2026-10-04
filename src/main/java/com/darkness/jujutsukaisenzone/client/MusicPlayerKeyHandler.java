package com.darkness.jujutsukaisenzone.client;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.client.gui.music.MusicPlayerUI;
import com.darkness.jujutsukaisenzone.client.music.JjkzMusicDirector;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * 打开「音乐鉴赏」界面（默认 M）。关界面时立刻停鉴赏曲。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, value = Dist.CLIENT)
public final class MusicPlayerKeyHandler
{
    public static final KeyMapping KEY_MAPPING = new KeyMapping(
            "key.jujutsukaisenzone.music_player",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            "key.categories.jujutsukaisenzone"
    );

    private static boolean wasKeyDown;

    private MusicPlayerKeyHandler()
    {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END)
        {
            return;
        }

        JjkzMusicDirector.tick();

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null)
        {
            wasKeyDown = false;
            return;
        }

        boolean isDown = KEY_MAPPING.isDown();
        if (isDown && !wasKeyDown)
        {
            MusicPlayerUI.open();
        }
        wasKeyDown = isDown;
    }

    @SubscribeEvent
    public static void onScreenClosing(ScreenEvent.Closing event)
    {
        if (MusicPlayerUI.isSessionOpen())
        {
            MusicPlayerUI.onSessionClosed();
        }
    }
}
