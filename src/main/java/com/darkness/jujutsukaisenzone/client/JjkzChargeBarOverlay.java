package com.darkness.jujutsukaisenzone.client;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 统一绘制「最大输出」「审判」等自绘蓄力条，单一订阅点避免漏渲染。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class JjkzChargeBarOverlay
{
    private JjkzChargeBarOverlay()
    {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onRenderGui(RenderGuiEvent.Post event)
    {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || minecraft.screen != null)
        {
            return;
        }

        int screenWidth = event.getWindow().getGuiScaledWidth();
        int screenHeight = event.getWindow().getGuiScaledHeight();
        float partialTick = event.getPartialTick();

        MaxOutputChargeHud.render(event.getGuiGraphics(), minecraft, screenWidth, screenHeight, partialTick);

        int trialOffsetY = MaxOutputChargeHud.isVisible() ? 28 : 0;
        JudgmentTrialChargeHud.render(event.getGuiGraphics(), minecraft, screenWidth, screenHeight, partialTick, trialOffsetY);
    }
}
