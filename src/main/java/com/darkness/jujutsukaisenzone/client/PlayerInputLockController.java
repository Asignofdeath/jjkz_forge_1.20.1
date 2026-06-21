package com.darkness.jujutsukaisenzone.client;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 客户端输入锁控制器。
 * <p>
 * 业务意图：当服务端要求“玩家锁定”时，在客户端层面将移动/跳跃/潜行输入清零，
 * 配合服务端强制传送与朝向锁，避免本地输入导致的抖动和视觉穿插。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class PlayerInputLockController
{
    private static boolean locked = false;

    private PlayerInputLockController()
    {
    }

    public static void setLocked(boolean isLocked)
    {
        locked = isLocked;
    }

    public static boolean isLocked()
    {
        return locked;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END || !locked)
        {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || player.input == null)
        {
            return;
        }

        player.input.leftImpulse = 0.0F;
        player.input.forwardImpulse = 0.0F;
        player.input.jumping = false;
        player.input.shiftKeyDown = false;
    }
}
