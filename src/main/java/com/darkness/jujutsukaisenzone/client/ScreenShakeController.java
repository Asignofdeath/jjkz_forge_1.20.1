package com.darkness.jujutsukaisenzone.client;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.api.screen.ScreenShakeKind;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 客户端屏幕振动实现：在 {@link ViewportEvent.ComputeCameraAngles} 上叠加视角偏移。
 * <p>
 * 对外请通过 {@link ScreenShakeApi} 调用；服务端请通过 {@link com.darkness.jujutsukaisenzone.network.ModNetwork#sendScreenShake}。
 */
public final class ScreenShakeController
{
    private static long decayShakeEndMillis;
    private static long decayShakeDurationMillis;
    private static float decayShakeMaxDegrees;

    private static long constantShakeEndMillis;
    private static float constantShakeMaxDegrees;

    private static boolean sustainedActive;
    private static float sustainedMaxDegrees;

    /** 平滑晃动：墙钟截止，避免 CMDCam 等场景下 client tick 不递减导致无效果。 */
    private static long wobbleEndMillis;
    private static long wobbleDurationMillis;
    private static float wobbleMaxAmplitude;
    private static final float WOBBLE_OSCILLATIONS = 3.0F;

    private ScreenShakeController()
    {
    }

    public static void apply(ScreenShakeKind kind, int durationTicks, float amplitude, boolean sustainedActiveFlag)
    {
        switch (kind)
        {
            case DECAY -> beginDecay(durationTicks, amplitude);
            case CONSTANT -> beginConstant(durationTicks, amplitude);
            case SUSTAINED -> setSustained(sustainedActiveFlag, amplitude);
            case SMOOTH_WOBBLE -> beginSmoothWobble(durationTicks, amplitude);
        }
    }

    public static void beginDecay(int durationTicks, float maxDegrees)
    {
        if (durationTicks <= 0 || maxDegrees <= 0.0F)
        {
            return;
        }
        long durationMillis = (long) durationTicks * 50L;
        decayShakeEndMillis = System.currentTimeMillis() + durationMillis;
        decayShakeDurationMillis = durationMillis;
        decayShakeMaxDegrees = maxDegrees;
    }

    public static void stopDecay()
    {
        decayShakeEndMillis = 0L;
        decayShakeDurationMillis = 0L;
        decayShakeMaxDegrees = 0.0F;
    }

    private static boolean isDecayShakeActive()
    {
        return decayShakeEndMillis > System.currentTimeMillis();
    }

    public static void beginConstant(int durationTicks, float maxDegrees)
    {
        if (durationTicks <= 0 || maxDegrees <= 0.0F)
        {
            return;
        }
        constantShakeEndMillis = System.currentTimeMillis() + (long) durationTicks * 50L;
        constantShakeMaxDegrees = maxDegrees;
    }

    public static void stopConstant()
    {
        constantShakeEndMillis = 0L;
        constantShakeMaxDegrees = 0.0F;
    }

    private static boolean isConstantShakeActive()
    {
        return constantShakeEndMillis > System.currentTimeMillis();
    }

    public static void setSustained(boolean active, float maxDegrees)
    {
        sustainedActive = active;
        if (active && maxDegrees > 0.0F)
        {
            sustainedMaxDegrees = maxDegrees;
            beginDecay(4, maxDegrees);
        }
        else if (!active)
        {
            sustainedMaxDegrees = 0.0F;
        }
    }

    public static void beginSmoothWobble(int durationTicks, float maxAmplitude)
    {
        if (durationTicks <= 0 || maxAmplitude <= 0.0F)
        {
            return;
        }
        long durationMillis = (long) durationTicks * 50L;
        wobbleEndMillis = System.currentTimeMillis() + durationMillis;
        wobbleDurationMillis = durationMillis;
        wobbleMaxAmplitude = maxAmplitude;
    }

    public static void stopWobble()
    {
        wobbleEndMillis = 0L;
        wobbleDurationMillis = 0L;
        wobbleMaxAmplitude = 0.0F;
    }

    private static boolean isWobbleActive()
    {
        return wobbleEndMillis > System.currentTimeMillis();
    }

    public static void resetAll()
    {
        stopDecay();
        stopConstant();
        stopWobble();
        sustainedActive = false;
        sustainedMaxDegrees = 0.0F;
    }

    static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.isPaused())
        {
            return;
        }

        if (sustainedActive && sustainedMaxDegrees > 0.0F)
        {
            applyShakeOffset(event, sustainedMaxDegrees);
        }

        if (isDecayShakeActive())
        {
            long remainingMillis = decayShakeEndMillis - System.currentTimeMillis();
            float t = decayShakeDurationMillis > 0L
                    ? (float) remainingMillis / (float) decayShakeDurationMillis
                    : 0.0F;
            t = Mth.clamp(t, 0.0F, 1.0F);
            applyShakeOffset(event, decayShakeMaxDegrees * t * t);
        }
        else if (decayShakeEndMillis != 0L)
        {
            stopDecay();
        }

        if (isConstantShakeActive())
        {
            applyShakeOffset(event, constantShakeMaxDegrees);
        }
        else if (constantShakeEndMillis != 0L)
        {
            stopConstant();
        }

        if (isWobbleActive())
        {
            long remainingMillis = wobbleEndMillis - System.currentTimeMillis();
            float progress = wobbleDurationMillis > 0L
                    ? 1.0F - (float) remainingMillis / (float) wobbleDurationMillis
                    : 1.0F;
            float decay = wobbleDurationMillis > 0L
                    ? (float) remainingMillis / (float) wobbleDurationMillis
                    : 0.0F;
            decay *= decay;
            applyWobbleOffset(event, wobbleMaxAmplitude * decay, Mth.clamp(progress, 0.0F, 1.0F));
        }
        else if (wobbleEndMillis != 0L)
        {
            stopWobble();
        }
    }

    private static void applyShakeOffset(ViewportEvent.ComputeCameraAngles event, float amp)
    {
        if (amp <= 0.0F)
        {
            return;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        float dy = (random.nextFloat() - 0.5F) * 2.0F * amp;
        float dp = (random.nextFloat() - 0.5F) * 2.0F * amp * 0.55F;
        float dro = (random.nextFloat() - 0.5F) * 2.0F * amp * 0.35F;
        event.setYaw(event.getYaw() + dy);
        event.setPitch(Mth.clamp(event.getPitch() + dp, -89.9F, 89.9F));
        event.setRoll(event.getRoll() + dro);
    }

    private static void applyWobbleOffset(ViewportEvent.ComputeCameraAngles event, float amp, float progress)
    {
        if (amp <= 0.0F)
        {
            return;
        }

        double phase = progress * WOBBLE_OSCILLATIONS * 2.0 * Math.PI;
        event.setYaw(event.getYaw() + (float) Math.sin(phase) * amp);
        event.setPitch(Mth.clamp(event.getPitch() + (float) Math.cos(phase * 1.3) * amp * 0.5F, -89.9F, 89.9F));
        event.setRoll(event.getRoll() + (float) Math.sin(phase + Math.PI * 0.5) * amp * 0.3F);
    }

    @Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class Subscriber
    {
        private Subscriber()
        {
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event)
        {
            ScreenShakeController.onComputeCameraAngles(event);
        }

        @SubscribeEvent
        public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event)
        {
            ScreenShakeController.resetAll();
        }
    }
}
