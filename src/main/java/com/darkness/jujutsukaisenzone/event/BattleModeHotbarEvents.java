package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.compat.EpicFightCompat;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yesman.epicfight.client.events.engine.ControlEngine;

/**
 * Epic Fight 战斗模式下屏蔽数字键 1~3 切换快捷栏（与术式槽热键共用；4~9 仍可切栏）。
 * <p>
 * 在客户端 tick 的 {@link TickEvent.Phase#START} 执行，早于本 tick 内 {@code handleKeybinds} 对热键栏的处理。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class BattleModeHotbarEvents
{
    private BattleModeHotbarEvents()
    {
    }

    @SuppressWarnings("deprecation") // Epic Fight 公开 API，仍用于与其它模块一致的按键复位
    @SubscribeEvent
    public static void onClientTickStart(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.START)
        {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null)
        {
            return;
        }
        if (!EpicFightCompat.isBattleMode(mc.player))
        {
            return;
        }
        var hotbarKeys = mc.options.keyHotbarSlots;
        for (int i = 0; i < 3 && i < hotbarKeys.length; i++)
        {
            ControlEngine.makeUnpressed(hotbarKeys[i]);
        }
    }
}
