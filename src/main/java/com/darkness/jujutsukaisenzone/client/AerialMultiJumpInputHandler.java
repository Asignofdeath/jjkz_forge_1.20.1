package com.darkness.jujutsukaisenzone.client;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.network.ModNetwork;
import com.darkness.jujutsukaisenzone.technique.aerial.AerialCombatSkillRegistry;
import com.darkness.jujutsukaisenzone.technique.aerial.AerialCombatSkills;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 空中单击空格时请求多段跳。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class AerialMultiJumpInputHandler
{
    private AerialMultiJumpInputHandler()
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
        if (minecraft.player == null
                || minecraft.screen != null
                || PlayerInputLockController.isLocked())
        {
            return;
        }
        if (!AerialCombatSkillRegistry.isUnlocked(minecraft.player, AerialCombatSkills.SKILL_AERIAL_MULTI_JUMP))
        {
            return;
        }
        if (minecraft.player.onGround() || !minecraft.options.keyJump.consumeClick())
        {
            return;
        }

        ModNetwork.sendAerialMultiJumpRequest();
    }
}
