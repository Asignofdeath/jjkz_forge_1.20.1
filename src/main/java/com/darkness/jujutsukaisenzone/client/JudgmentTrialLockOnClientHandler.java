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

 * 「审判」：将 Epic Fight 锁定目标同步至服务端；蓄力条仅由服务端 S2C 包控制，此处不触碰 HUD。

 */

@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)

public final class JudgmentTrialLockOnClientHandler

{

    private static UUID lastSentTargetUuid;

    private static boolean lastSentLocking;



    private JudgmentTrialLockOnClientHandler()

    {

    }



    public static void onSessionEnded()

    {

        lastSentLocking = false;

        lastSentTargetUuid = null;

    }



    /** Session 开启时立刻上报当前 EF 锁定（支持先锁定再按技能）。 */

    public static void onSessionStarted()

    {

        lastSentLocking = false;

        lastSentTargetUuid = null;

        publishCurrentLockState();

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



        if (!JudgmentTrialClientState.isTrialLockActive()

                || !PlayerTechniqueData.isJudgment(minecraft.player)

                || !EpicFightCompat.isBattleMode(minecraft.player))

        {

            clearLockSyncIfNeeded();

            return;

        }



        publishCurrentLockState();

    }



    private static void publishCurrentLockState()

    {

        EpicFightCameraAPI cameraApi = EpicFightCameraAPI.getInstance();

        boolean locking = cameraApi.isLockingOnTarget();

        LivingEntity target = locking ? cameraApi.getFocusingEntity() : null;

        UUID targetUuid = target != null && target.isAlive() ? target.getUUID() : null;



        if (locking && targetUuid != null)

        {

            if (!lastSentLocking || !Objects.equals(lastSentTargetUuid, targetUuid))

            {

                ModNetwork.sendJudgmentTrialLockOnToServer(true, targetUuid);

                lastSentLocking = true;

                lastSentTargetUuid = targetUuid;

            }

            return;

        }



        if (lastSentLocking)

        {

            ModNetwork.sendJudgmentTrialLockOnToServer(false, null);

            lastSentLocking = false;

            lastSentTargetUuid = null;

        }

    }



    private static void clearLockSyncIfNeeded()

    {

        if (lastSentLocking)

        {

            ModNetwork.sendJudgmentTrialLockOnToServer(false, null);

            lastSentLocking = false;

            lastSentTargetUuid = null;

        }

    }

}


