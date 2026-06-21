package com.darkness.jujutsukaisenzone.client;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.client.gui.SkillBarConfigScreen;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 客户端技能快捷栏缓存补同步：登录时服务端包可能早于连接就绪而丢失，
 * 在术式已知且缓存缺失时主动向服务端请求配置。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientSkillBarSyncHandler
{
    private static final int RETRY_INTERVAL_TICKS = 20;

    private static int retryCooldown;

    private ClientSkillBarSyncHandler()
    {
    }

    /** 术式同步到达后立即请求一次快捷栏配置。 */
    public static void onTechniqueSynced(String techniqueId)
    {
        if (techniqueId == null || techniqueId.isBlank()
                || PlayerTechniqueData.TECHNIQUE_NONE.equals(techniqueId))
        {
            return;
        }
        retryCooldown = 0;
        requestIfMissing(techniqueId);
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event)
    {
        ClientSkillBarCache.clear();
        retryCooldown = 0;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END)
        {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null)
        {
            retryCooldown = 0;
            return;
        }

        String techniqueId = PlayerTechniqueData.getInnateTechnique(mc.player);
        if (techniqueId.isBlank() || PlayerTechniqueData.TECHNIQUE_NONE.equals(techniqueId))
        {
            return;
        }
        if (ClientSkillBarCache.has(techniqueId))
        {
            return;
        }
        if (retryCooldown > 0)
        {
            retryCooldown--;
            return;
        }

        retryCooldown = RETRY_INTERVAL_TICKS;
        requestIfMissing(techniqueId);
    }

    private static void requestIfMissing(String techniqueId)
    {
        if (ClientSkillBarCache.has(techniqueId))
        {
            return;
        }
        SkillBarConfigScreen.requestConfigFromServer(techniqueId);
    }
}
