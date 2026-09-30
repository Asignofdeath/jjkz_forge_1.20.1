package com.darkness.jujutsukaisenzone.client;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.entity.MahoragaEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 骑乘魔虚罗行走时给本地玩家一点轻微镜头摇晃。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class MahoragaRideCameraClient
{
    /** 最大俯仰摇晃（度）。 */
    private static final float MAX_PITCH_DEG = 0.7F;
    /** 最大横滚摇晃（度）。 */
    private static final float MAX_ROLL_DEG = 0.35F;
    /** 最大偏航摇晃（度）。 */
    private static final float MAX_YAW_DEG = 0.28F;
    /** 步伐角频率（相对 entity tick）。 */
    private static final float STEP_RATE = 0.42F;
    /** 低于此水平速度不摇。 */
    private static final double MIN_SPEED = 0.02D;

    private MahoragaRideCameraClient()
    {
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.isPaused())
        {
            return;
        }
        LocalPlayer player = mc.player;
        if (player == null || !(player.getVehicle() instanceof MahoragaEntity mahoraga)
                || !mahoraga.isAlive() || !mahoraga.isRideMode())
        {
            return;
        }
        double speed = mahoraga.getDeltaMovement().horizontalDistance();
        if (speed < MIN_SPEED && Math.abs(player.xxa) < 0.05F && Math.abs(player.zza) < 0.05F)
        {
            return;
        }
        float strength = Mth.clamp((float) (Math.max(speed, 0.08D) / 0.22D), 0.25F, 1.0F);
        float phase = (mahoraga.tickCount + (float) event.getPartialTick()) * STEP_RATE;
        float pitch = Mth.sin(phase) * MAX_PITCH_DEG * strength;
        float roll = Mth.sin(phase * 0.5F + 0.7F) * MAX_ROLL_DEG * strength;
        float yaw = Mth.cos(phase * 0.5F) * MAX_YAW_DEG * strength;
        event.setPitch(Mth.clamp(event.getPitch() + pitch, -89.9F, 89.9F));
        event.setRoll(event.getRoll() + roll);
        event.setYaw(event.getYaw() + yaw);
    }
}
