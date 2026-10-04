package com.darkness.jujutsukaisenzone.client;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.client.gui.quest.QuestJournalUI;
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
 * 打开「任务」全屏界面（默认 J，可在控制设置中改键）。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, value = Dist.CLIENT)
public final class QuestJournalKeyHandler
{
    public static final KeyMapping KEY_MAPPING = new KeyMapping(
            "key.jujutsukaisenzone.quest_journal",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_J,
            "key.categories.jujutsukaisenzone"
    );

    private static boolean wasKeyDown;

    private QuestJournalKeyHandler()
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
            QuestJournalUI.open();
        }
        wasKeyDown = isDown;
    }
}
