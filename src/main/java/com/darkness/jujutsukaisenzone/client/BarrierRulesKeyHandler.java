package com.darkness.jujutsukaisenzone.client;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.client.gui.BarrierRulesScreen;
import com.darkness.jujutsukaisenzone.player.PlayerBarrierTechniqueData;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * 按 Shift+B 打开结界规则配置界面
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, value = Dist.CLIENT)
public class BarrierRulesKeyHandler
{
    public static final KeyMapping KEY_MAPPING = new KeyMapping(
            "key.jujutsukaisenzone.barrier_rules",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            "key.categories.jujutsukaisenzone"
    );

    private static boolean wasTriggered = false;

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event)
    {
        event.register(KEY_MAPPING);
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
            wasTriggered = false;
            return;
        }

        if (!KEY_MAPPING.isDown())
        {
            wasTriggered = false;
            return;
        }

        if (wasTriggered)
        {
            return;
        }

        Options options = minecraft.options;
        if (options != null && options.keyShift != null && options.keyShift.isDown())
        {
            wasTriggered = true;
            if (!PlayerBarrierTechniqueData.hasLearned(minecraft.player))
            {
                minecraft.player.displayClientMessage(
                        Component.literal("你还没有掌握结界术").withStyle(ChatFormatting.RED),
                        true
                );
                return;
            }
            minecraft.setScreen(new BarrierRulesScreen());
        }
    }
}
