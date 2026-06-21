package com.darkness.jujutsukaisenzone.compat;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

/**
 * 在 Common Setup 阶段按 ModList 动态注册 ParCool 联动，避免未安装时加载 ParCool 相关类。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ParCoolIntegrationRegistrar
{
    private static final Logger LOGGER = LogUtils.getLogger();

    private ParCoolIntegrationRegistrar()
    {
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event)
    {
        event.enqueueWork(() ->
        {
            if (!ParCoolPresence.isAerialIntegrationAvailable())
            {
                LOGGER.info("ParCool aerial integration not loaded (parcool and/or epicparcool missing).");
                return;
            }
            MinecraftForge.EVENT_BUS.register(new ParCoolAerialBridge());
            LOGGER.info("ParCool aerial integration enabled.");
        });
    }
}
