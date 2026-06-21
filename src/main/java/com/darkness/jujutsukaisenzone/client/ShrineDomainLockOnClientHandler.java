package com.darkness.jujutsukaisenzone.client;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.compat.EpicFightCompat;
import com.darkness.jujutsukaisenzone.network.ModNetwork;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yesman.epicfight.api.client.camera.EpicFightCameraAPI;

import java.util.Objects;
import java.util.UUID;

/**
 * 伏魔神龛领域：将 Epic Fight 锁定目标同步至服务端，用于强力斩击蓄力。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ShrineDomainLockOnClientHandler
{
    private static UUID lastSentTargetUuid;
    private static boolean lastSentLocking;

    private ShrineDomainLockOnClientHandler()
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
        if (minecraft.player == null || minecraft.screen != null)
        {
            return;
        }

        if (!ShrineDomainClientState.isInOwnActiveShrineDomain()
                || !PlayerTechniqueData.isShrine(minecraft.player)
                || !EpicFightCompat.isBattleMode(minecraft.player))
        {
            if (lastSentLocking)
            {
                ShrinePowerSlashChargeHud.hide();
                ModNetwork.sendShrineDomainLockOnToServer(false, null);
                lastSentLocking = false;
                lastSentTargetUuid = null;
            }
            return;
        }

        EpicFightCameraAPI cameraApi = EpicFightCameraAPI.getInstance();
        boolean locking = cameraApi.isLockingOnTarget();
        LivingEntity target = locking ? cameraApi.getFocusingEntity() : null;
        UUID targetUuid = target != null && target.isAlive() ? target.getUUID() : null;

        if (locking && targetUuid != null)
        {
            if (!lastSentLocking || !Objects.equals(lastSentTargetUuid, targetUuid))
            {
                ShrinePowerSlashChargeHud.beginLockCharge();
                ModNetwork.sendShrineDomainLockOnToServer(true, targetUuid);
                lastSentLocking = true;
                lastSentTargetUuid = targetUuid;
            }
            return;
        }

        if (lastSentLocking)
        {
            ShrinePowerSlashChargeHud.hide();
            ModNetwork.sendShrineDomainLockOnToServer(false, null);
            lastSentLocking = false;
            lastSentTargetUuid = null;
        }
    }
}
