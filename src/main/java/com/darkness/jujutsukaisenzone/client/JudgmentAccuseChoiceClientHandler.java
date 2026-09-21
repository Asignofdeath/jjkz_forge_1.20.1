package com.darkness.jujutsukaisenzone.client;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.network.ModNetwork;
import com.darkness.jujutsukaisenzone.technique.skill.JudgmentAccuseResponse;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * 「指控」：被告按 1/2/3 提交辩护选择。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class JudgmentAccuseChoiceClientHandler
{
    private static boolean choiceSent;

    private JudgmentAccuseChoiceClientHandler()
    {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END)
        {
            return;
        }

        if (!JudgmentAccuseChoiceClientState.isActive())
        {
            choiceSent = false;
            JudgmentAccuseChoiceHud.hide();
            return;
        }

        JudgmentAccuseChoiceHud.show();
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event)
    {
        if (event.getAction() != GLFW.GLFW_PRESS)
        {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null)
        {
            return;
        }

        if (!JudgmentAccuseChoiceClientState.isActive() || choiceSent)
        {
            return;
        }

        JudgmentAccuseResponse response = switch (event.getKey())
        {
            case GLFW.GLFW_KEY_1 -> JudgmentAccuseResponse.DENY;
            case GLFW.GLFW_KEY_2 -> JudgmentAccuseResponse.LIE;
            case GLFW.GLFW_KEY_3 -> JudgmentAccuseResponse.CONFESS;
            default -> null;
        };
        if (response == null)
        {
            return;
        }

        choiceSent = true;
        ModNetwork.sendJudgmentAccuseChoiceToServer(response.id());
    }
}
