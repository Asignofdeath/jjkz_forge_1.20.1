package com.darkness.jujutsukaisenzone.client.renderer;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 资源重载钩子。
 * <p>
 * 扫描波走的是后处理链，链里编译好的 program 会随资源重载一起失效；但 {@code PostChain} 并
 * 不在原版的重建名单里，必须自己丢掉重建。这里只做「标记」，真正的重建推迟到下次播放时，
 * 避免在资源重载过程中做 GPU 操作。
 * <p>
 * 用单独的类是因为事件总线不同：渲染事件在 FORGE 总线，本事件在 MOD 总线，
 * 一个类只能标一个 {@code @Mod.EventBusSubscriber}。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class DomainScanWaveReloadListener
{
    private DomainScanWaveReloadListener()
    {
    }

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event)
    {
        DomainScanWaveRenderer.invalidate();
    }
}
