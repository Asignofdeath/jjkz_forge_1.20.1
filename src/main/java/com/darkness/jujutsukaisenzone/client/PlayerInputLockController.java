package com.darkness.jujutsukaisenzone.client;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 客户端输入锁控制器。
 * <p>
 * 业务意图：当服务端要求“玩家锁定”时，在移动结算之前清掉 WASD / 跳 / 蹲，
 * 避免客户端先走一步再被清零。
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

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMovementInput(MovementInputUpdateEvent event)
    {
        if (!locked)
        {
            return;
        }
        event.getInput().up = false;
        event.getInput().down = false;
        event.getInput().left = false;
        event.getInput().right = false;
        event.getInput().jumping = false;
        event.getInput().shiftKeyDown = false;
        event.getInput().leftImpulse = 0.0F;
        event.getInput().forwardImpulse = 0.0F;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.START || !locked)
        {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Options options = minecraft.options;
        options.keyUp.setDown(false);
        options.keyDown.setDown(false);
        options.keyLeft.setDown(false);
        options.keyRight.setDown(false);
        options.keyJump.setDown(false);
        options.keyShift.setDown(false);
        LocalPlayer player = minecraft.player;
        if (player == null)
        {
            return;
        }
        if (player.input != null)
        {
            player.input.up = false;
            player.input.down = false;
            player.input.left = false;
            player.input.right = false;
            player.input.jumping = false;
            player.input.shiftKeyDown = false;
            player.input.leftImpulse = 0.0F;
            player.input.forwardImpulse = 0.0F;
        }
        player.xxa = 0.0F;
        player.zza = 0.0F;
        player.setDeltaMovement(Vec3.ZERO);
    }
}
