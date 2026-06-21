package com.darkness.jujutsukaisenzone.client;

import com.darkness.jujutsukaisenzone.api.screen.ScreenShakeKind;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 屏幕振动通用客户端 API。
 * <p>
 * 服务端触发请使用 {@link com.darkness.jujutsukaisenzone.network.ModNetwork#sendScreenShake}。
 */
@OnlyIn(Dist.CLIENT)
public final class ScreenShakeApi
{
    private ScreenShakeApi()
    {
    }

    public static void apply(ScreenShakeKind kind, int durationTicks, float amplitude, boolean sustainedActive)
    {
        ScreenShakeController.apply(kind, durationTicks, amplitude, sustainedActive);
    }

    public static void beginDecay(int durationTicks, float maxDegrees)
    {
        ScreenShakeController.beginDecay(durationTicks, maxDegrees);
    }

    public static void beginConstant(int durationTicks, float maxDegrees)
    {
        ScreenShakeController.beginConstant(durationTicks, maxDegrees);
    }

    public static void setSustained(boolean active, float maxDegrees)
    {
        ScreenShakeController.setSustained(active, maxDegrees);
    }

    public static void beginSmoothWobble(int durationTicks, float maxAmplitude)
    {
        ScreenShakeController.beginSmoothWobble(durationTicks, maxAmplitude);
    }

    public static void resetAll()
    {
        ScreenShakeController.resetAll();
    }
}
