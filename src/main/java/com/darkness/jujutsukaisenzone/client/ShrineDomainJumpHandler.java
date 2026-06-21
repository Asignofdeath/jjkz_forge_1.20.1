package com.darkness.jujutsukaisenzone.client;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 伏魔神龛领域内：检测施法者单击空格并请求传送到神龛顶部。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ShrineDomainJumpHandler
{
    private ShrineDomainJumpHandler()
    {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END)
        {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null || PlayerInputLockController.isLocked())
        {
            return;
        }

        if (minecraft.options.keyJump.consumeClick())
        {
            ModNetwork.sendShrineDomainTeleportToServer(minecraft.player.getYRot());
        }
    }
}
