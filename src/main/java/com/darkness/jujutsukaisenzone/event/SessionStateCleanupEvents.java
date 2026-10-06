package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.technique.BarrierTechniqueServerLogic;
import com.darkness.jujutsukaisenzone.technique.domain.AiDomainCasterRegistry;
import com.darkness.jujutsukaisenzone.technique.domain.AiDomainModeData;
import com.darkness.jujutsukaisenzone.technique.domain.DomainExpansionRouter;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 会话级领域 / 结界状态的生命周期清理。
 * <p>
 * 领域与结界逻辑由进程级静态单例持有活跃实例（{@code ACTIVE_DOMAINS} / {@code ACTIVE_BARRIERS} /
 * {@code BARRIER_RADIUS_TIER}）。Forge 在退出世界 / 切换存档时不会重新加载模组类，故必须在会话结束时
 * 显式收尾，否则会有三类后果：
 * <ul>
 *   <li>被领域替换的方块（{@code removedBlocks}）留在世界且无人还原；</li>
 *   <li>残留的 {@code BarrierInstance} 在新会话中以「幽灵领域 / 幽灵结界」匹配同维度同坐标继续生效；</li>
 *   <li>尺寸档位跨存档串档、方块快照永久驻留内存。</li>
 * </ul>
 * 登出时先正常关闭（触发 {@code onBarrierClosed} 回调 + 方块还原）；停机时再对可能残留的实例做兜底还原，
 * 最后清空静态容器。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SessionStateCleanupEvents
{
    private SessionStateCleanupEvents()
    {
    }

    /**
     * 玩家登出：关闭其仍在持有的领域与结界（含回调与方块还原）。
     */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player)
        {
            DomainExpansionRouter.closeAllForPlayer(player);
            BarrierTechniqueServerLogic.closeAllForPlayer(player);
        }
    }

    /**
     * 服务器停机：还原所有残留领域 / 结界方块，然后清空进程级静态容器。
     */
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event)
    {
        MinecraftServer server = event.getServer();

        // 1) 仍在线者走完整关闭路径（触发 onBarrierClosed，清掉按施术者记录的各种状态）
        for (ServerPlayer player : server.getPlayerList().getPlayers())
        {
            DomainExpansionRouter.closeAllForPlayer(player);
            BarrierTechniqueServerLogic.closeAllForPlayer(player);
        }
        // 2) 兜底：所有者已离线的残留实例仅做世界侧方块还原
        DomainExpansionRouter.restoreAllWorldAll(server);
        BarrierTechniqueServerLogic.restoreAllWorld(server);
        // 3) 清空进程级静态容器，杜绝跨存档残留
        AiDomainCasterRegistry.clearAll();
        AiDomainModeData.clearAll();
        DomainExpansionRouter.resetAllSessionState();
        BarrierTechniqueServerLogic.resetSessionState();
    }
}
