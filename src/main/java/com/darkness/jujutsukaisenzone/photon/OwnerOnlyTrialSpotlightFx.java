package com.darkness.jujutsukaisenzone.photon;

import com.darkness.jujutsukaisenzone.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * 诛伏赐死领域聚光灯：通过 OwnerOnly S2C 包在观众客户端挂载/卸载，
 * 替代服务端 {@code photon fx} 命令，避免关领域后客户端 cache 残留。
 */
public final class OwnerOnlyTrialSpotlightFx
{
    private OwnerOnlyTrialSpotlightFx()
    {
    }

    public static void mount(LivingEntity target, Iterable<ServerPlayer> viewers)
    {
        if (!target.isAlive())
        {
            return;
        }

        for (ServerPlayer viewer : viewers)
        {
            ModNetwork.sendOwnerOnlyPhotonMount(
                    viewer,
                    target.getId(),
                    JudgmentFxIds.SPOTLIGHT,
                    OwnerOnlyPhotonFxParams.SPOTLIGHT);
        }
    }

    public static void remove(Entity target, Iterable<ServerPlayer> viewers)
    {
        for (ServerPlayer viewer : viewers)
        {
            ModNetwork.sendOwnerOnlyPhotonRemove(viewer, target.getId(), JudgmentFxIds.SPOTLIGHT, true);
        }
    }
}
