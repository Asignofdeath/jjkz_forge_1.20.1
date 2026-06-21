package com.darkness.jujutsukaisenzone.client;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.client.renderer.FullScreenWorldFlashRenderer;
import com.darkness.jujutsukaisenzone.client.renderer.WhiteOverlayLayerRenderer;
import com.darkness.jujutsukaisenzone.entity.data.ShrineFugaConfig;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 「灶」开爆炸：先全屏闪白，结束后无缝衔接世界层（方块之上）闪白。
 */
public final class FugaBoomFlashClientEffects
{
    private static boolean awaitingWorldLayerFlash;

    private FugaBoomFlashClientEffects()
    {
    }

    public static void playSequence()
    {
        awaitingWorldLayerFlash = true;
        FullScreenWorldFlashRenderer.playFadeAnimation(
                ShrineFugaConfig.BOOM_WHITE_FLASH_TOTAL_TICKS,
                ShrineFugaConfig.BOOM_WHITE_FLASH_FADE_IN_TICKS,
                ShrineFugaConfig.BOOM_WHITE_FLASH_FADE_OUT_TICKS,
                ShrineFugaConfig.BOOM_WHITE_FLASH_PEAK_ALPHA
        );
    }

    public static void cancelPending()
    {
        awaitingWorldLayerFlash = false;
    }

    @Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class Subscriber
    {
        private Subscriber()
        {
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event)
        {
            if (event.phase != TickEvent.Phase.END || !awaitingWorldLayerFlash)
            {
                return;
            }
            if (FullScreenWorldFlashRenderer.isAnimating())
            {
                return;
            }

            awaitingWorldLayerFlash = false;
            WhiteOverlayLayerRenderer.playFadeAnimation(
                    ShrineFugaConfig.BOOM_WORLD_LAYER_FLASH_TOTAL_TICKS,
                    ShrineFugaConfig.BOOM_WORLD_LAYER_FLASH_FADE_IN_TICKS,
                    ShrineFugaConfig.BOOM_WORLD_LAYER_FLASH_FADE_OUT_TICKS,
                    ShrineFugaConfig.BOOM_WORLD_LAYER_FLASH_PEAK_ALPHA
            );
        }
    }
}
