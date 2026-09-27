package com.darkness.jujutsukaisenzone.compat;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.gameplay.ModRules;
import com.darkness.jujutsukaisenzone.network.ModNetwork;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import org.slf4j.Logger;

/**
 * StylizedDamage 联动：只要模组在就注册样式/选择器并重建引擎；
 * {@link ModRules.StylizedDamageStyles} 只控制服务端是否投影叙事 message_id。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class StylizedDamageIntegrationRegistrar
{
    private static final Logger LOGGER = LogUtils.getLogger();

    private StylizedDamageIntegrationRegistrar()
    {
    }

    /** 注册样式 + 合并选择器进对方引擎（供指令 / setup / 进服同步调用）。 */
    public static void registerStylesIfPresent()
    {
        if (!StylizedDamagePresence.isLoaded())
        {
            return;
        }
        StylizedDamageCompat.registerIfNeeded();
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event)
    {
        // 延后到工作队列：尽量等 StylizedDamage 的 commonSetup 先 initialize
        event.enqueueWork(StylizedDamageIntegrationRegistrar::tryRegister);
    }

    @SubscribeEvent
    public static void onLoadComplete(FMLLoadCompleteEvent event)
    {
        // 最终兜底：保证引擎已合并 API 选择器
        event.enqueueWork(StylizedDamageIntegrationRegistrar::tryRegister);
    }

    private static void tryRegister()
    {
        if (!StylizedDamagePresence.isLoaded())
        {
            LOGGER.info("StylizedDamage integration not loaded (stylizeddamage missing).");
            return;
        }
        registerStylesIfPresent();
        LOGGER.info("StylizedDamage styles/selectors registered (projection gated by modRules).");
    }

    @Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class ForgeEvents
    {
        private ForgeEvents()
        {
        }

        @SubscribeEvent
        public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
        {
            if (!(event.getEntity() instanceof ServerPlayer serverPlayer))
            {
                return;
            }
            // 进服再推一次：覆盖对方 /stylizeddamage reload 冲掉 API 选择器的情况
            ModNetwork.syncStylizedDamageStylesRule(serverPlayer, ModRules.StylizedDamageStyles.isEnabled());
        }
    }
}
