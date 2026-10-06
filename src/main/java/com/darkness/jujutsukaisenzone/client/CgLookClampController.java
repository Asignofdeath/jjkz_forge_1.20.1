package com.darkness.jujutsukaisenzone.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/**
 * COD 式 CG：只裁 {@link Entity#turn} 的鼠标增量。顶到锥边即停，不改 body/head、不事后拽视角。
 */
public final class CgLookClampController
{
    private static boolean active;
    private static float baseYaw;
    private static float basePitch;
    private static float yawHalf;
    private static float pitchHalf;

    private CgLookClampController()
    {
    }

    public static boolean isActive()
    {
        return active;
    }

    public static void set(
            boolean isActive, float yaw, float pitch, float yawLimit, float pitchLimit)
    {
        active = isActive;
        baseYaw = yaw;
        basePitch = pitch;
        yawHalf = yawLimit;
        pitchHalf = pitchLimit;
        if (active)
        {
            snapToBase();
        }
    }

    public static void clear()
    {
        active = false;
    }

    /**
     * @return true 表示已接管本次 {@code Entity#turn}，原版不要再加角度
     */
    public static boolean consumeTurn(Entity entity, double yaw, double pitch)
    {
        if (!active)
        {
            return false;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player != entity)
        {
            return false;
        }
        float addYaw = (float) yaw * 0.15F;
        float addPitch = (float) pitch * 0.15F;
        float clampedYaw = clampYaw(player.getYRot() + addYaw);
        float clampedPitch = clampPitch(player.getXRot() + addPitch);
        float appliedYaw = clampedYaw - player.getYRot();
        float appliedPitch = clampedPitch - player.getXRot();
        player.setYRot(clampedYaw);
        player.setXRot(clampedPitch);
        player.yRotO += appliedYaw;
        player.xRotO += appliedPitch;
        return true;
    }

    private static void snapToBase()
    {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null)
        {
            return;
        }
        player.setYRot(baseYaw);
        player.setXRot(basePitch);
        player.yRotO = baseYaw;
        player.xRotO = basePitch;
    }

    private static float clampYaw(float yaw)
    {
        return baseYaw + Mth.clamp(Mth.wrapDegrees(yaw - baseYaw), -yawHalf, yawHalf);
    }

    private static float clampPitch(float pitch)
    {
        return basePitch + Mth.clamp(Mth.wrapDegrees(pitch - basePitch), -pitchHalf, pitchHalf);
    }
}
