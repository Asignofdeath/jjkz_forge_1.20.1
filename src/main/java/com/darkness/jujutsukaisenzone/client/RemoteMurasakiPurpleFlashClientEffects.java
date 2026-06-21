package com.darkness.jujutsukaisenzone.client;



import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;

import com.darkness.jujutsukaisenzone.client.renderer.FullScreenPurpleFlashRenderer;

import com.darkness.jujutsukaisenzone.client.renderer.PurpleOverlayLayerRenderer;

import com.darkness.jujutsukaisenzone.entity.data.LimitlessRemoteMurasakiConfig;

import net.minecraftforge.api.distmarker.Dist;

import net.minecraftforge.event.TickEvent;

import net.minecraftforge.eventbus.api.SubscribeEvent;

import net.minecraftforge.fml.common.Mod;



/**

 * 超远程虚式茈起手：先全屏紫色滤镜，结束后无缝衔接世界层紫色滤镜（参考「灶」开爆炸闪白链路）。

 */

public final class RemoteMurasakiPurpleFlashClientEffects

{

    private static boolean awaitingWorldLayerFlash;



    private RemoteMurasakiPurpleFlashClientEffects()

    {

    }



    public static void playSequence()

    {

        awaitingWorldLayerFlash = true;

        FullScreenPurpleFlashRenderer.setAnimationCompleteListener(RemoteMurasakiPurpleFlashClientEffects::startWorldLayerFlash);

        FullScreenPurpleFlashRenderer.playFadeAnimation(

                LimitlessRemoteMurasakiConfig.START_PURPLE_FULLSCREEN_TOTAL_TICKS,

                LimitlessRemoteMurasakiConfig.START_PURPLE_FULLSCREEN_FADE_IN_TICKS,

                LimitlessRemoteMurasakiConfig.START_PURPLE_FULLSCREEN_FADE_OUT_TICKS,

                LimitlessRemoteMurasakiConfig.START_PURPLE_FULLSCREEN_PEAK_ALPHA

        );

    }



    private static void startWorldLayerFlash()

    {

        if (!awaitingWorldLayerFlash)

        {

            return;

        }

        awaitingWorldLayerFlash = false;

        FullScreenPurpleFlashRenderer.setAnimationCompleteListener(null);

        PurpleOverlayLayerRenderer.playFadeAnimation(

                LimitlessRemoteMurasakiConfig.START_PURPLE_WORLD_LAYER_TOTAL_TICKS,

                LimitlessRemoteMurasakiConfig.START_PURPLE_WORLD_LAYER_FADE_IN_TICKS,

                LimitlessRemoteMurasakiConfig.START_PURPLE_WORLD_LAYER_FADE_OUT_TICKS,

                LimitlessRemoteMurasakiConfig.START_PURPLE_WORLD_LAYER_PEAK_ALPHA

        );

    }



    @Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)

    public static final class Subscriber

    {

        private Subscriber()

        {

        }



        /** 渲染回调未触发时的兜底（例如当前帧未绘制世界）。 */

        @SubscribeEvent

        public static void onClientTick(TickEvent.ClientTickEvent event)

        {

            if (event.phase != TickEvent.Phase.END || !awaitingWorldLayerFlash)

            {

                return;

            }

            if (FullScreenPurpleFlashRenderer.isAnimating())

            {

                return;

            }



            startWorldLayerFlash();

        }

    }

}


