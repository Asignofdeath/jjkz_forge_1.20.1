package com.darkness.jujutsukaisenzone.network.api;

import com.darkness.jujutsukaisenzone.network.NetworkSenders;
import com.darkness.jujutsukaisenzone.network.packet.AdjustCastRangeTierC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.EmotionalReleaseC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.ReverseTechniqueTickC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.ShikigamiAbilityC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncEmotionalReleaseStateS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncMaxOutputChargeInterruptS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncMimicryS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncReverseTechniqueS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncSeanceTechniqueLearnedS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncStrengthenTechniqueLearnedS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncTechniqueIntelS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncTechniqueS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncTenShadowsContractS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncTenShadowsFalseDeathS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncTenShadowsModeS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.TechniqueHoldReleaseC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.TechniqueMaxOutputC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.TechniqueSpecialMoveC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.TenShadowsShikigamiC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.ToggleMimicryC2SPacket;
import com.darkness.jujutsukaisenzone.api.screen.ScreenShakeKind;
import com.darkness.jujutsukaisenzone.photon.OwnerOnlyPhotonFxParams;
import com.darkness.jujutsukaisenzone.player.PlayerMimicryData;
import com.darkness.jujutsukaisenzone.player.StarMark;
import com.darkness.jujutsukaisenzone.technique.development.SpNodeInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import java.util.UUID;

/** Domain specific send API for technique packets. */
public final class TechniqueNetwork
{
    private TechniqueNetwork()
    {
    }

    public static void sendReverseTechniqueSync(ServerPlayer player, boolean learned)
    {
        NetworkSenders.toPlayer(player, new SyncReverseTechniqueS2CPacket(learned));
    }

    public static void sendSeanceTechniqueLearnedSync(ServerPlayer player, boolean learned, int tier)
    {
        NetworkSenders.toPlayer(player, new SyncSeanceTechniqueLearnedS2CPacket(learned, tier));
    }

    public static void sendStrengthenTechniqueLearnedSync(ServerPlayer player, boolean learned, int tier, boolean active)
    {
        NetworkSenders.toPlayer(player,
                new SyncStrengthenTechniqueLearnedS2CPacket(learned, tier, active));
    }

    public static void sendTechniqueSync(ServerPlayer player, String techniqueId)
    {
        NetworkSenders.toPlayer(player, new SyncTechniqueS2CPacket(techniqueId == null ? "" : techniqueId));
    }

    public static void sendTechniqueIntelSync(ServerPlayer player, java.util.Map<String, Integer> values)
    {
        NetworkSenders.toPlayer(player, new SyncTechniqueIntelS2CPacket(values));
    }

    public static void sendReverseTechniqueTickToServer()
    {
        NetworkSenders.toServer( new ReverseTechniqueTickC2SPacket());
    }

    public static void sendEmotionalReleaseToServer(byte action)
    {
        NetworkSenders.toServer( new EmotionalReleaseC2SPacket(action));
    }

    public static void sendEmotionalReleaseStateSync(
            ServerPlayer player,
            boolean charging,
            long chargeStartGameTime,
            long cooldownUntilGameTime)
    {
        NetworkSenders.toPlayer(player,
                new SyncEmotionalReleaseStateS2CPacket(charging, chargeStartGameTime, cooldownUntilGameTime)
        );
    }

    public static void sendTechniqueSpecialMoveToServer(int modifierMask, int baseSlot)
    {
        NetworkSenders.toServer( new TechniqueSpecialMoveC2SPacket(modifierMask, baseSlot));
    }

    public static void sendAdjustCastRangeTierToServer(int delta)
    {
        NetworkSenders.toServer( new AdjustCastRangeTierC2SPacket(delta));
    }

    public static void sendTechniqueMaxOutputToServer(byte action, int slot)
    {
        NetworkSenders.toServer( new TechniqueMaxOutputC2SPacket(action, slot));
    }

    public static void sendTechniqueHoldReleaseToServer(byte action, int slot)
    {
        NetworkSenders.toServer( new TechniqueHoldReleaseC2SPacket(action, slot));
    }

    public static void sendMaxOutputChargeInterruptSync(ServerPlayer player)
    {
        NetworkSenders.toPlayer(player, new SyncMaxOutputChargeInterruptS2CPacket());
    }

    public static void sendTenShadowsModeSync(ServerPlayer player, boolean active)
    {
        NetworkSenders.toPlayer(player, new SyncTenShadowsModeS2CPacket(active));
    }

    public static void sendTenShadowsShikigamiToServer(int shikigamiIndex)
    {
        NetworkSenders.toServer( new TenShadowsShikigamiC2SPacket(shikigamiIndex));
    }

    public static void sendShikigamiAbilityToServer(int shikigamiIndex, int modifierMask)
    {
        NetworkSenders.toServer( new ShikigamiAbilityC2SPacket(shikigamiIndex, modifierMask));
    }

    public static void sendTenShadowsContractSync(ServerPlayer player, int tamedMask)
    {
        NetworkSenders.toPlayer(player, new SyncTenShadowsContractS2CPacket(tamedMask));
    }

    public static void sendTenShadowsFalseDeathSync(net.minecraft.world.entity.LivingEntity entity, boolean active)
    {
        NetworkSenders.toTrackingAndSelf(entity,
                new SyncTenShadowsFalseDeathS2CPacket(entity.getUUID(), active));
    }

    public static void sendToggleMimicryToServer()
    {
        NetworkSenders.toServer( new ToggleMimicryC2SPacket());
    }

    public static void sendMimicrySync(ServerPlayer subject)
    {
        NetworkSenders.toTrackingAndSelf(subject,
                createMimicryPacket(subject)
        );
    }

    public static void sendMimicrySyncTo(ServerPlayer receiver, ServerPlayer subject)
    {
        NetworkSenders.toPlayer(receiver, createMimicryPacket(subject));
    }

    private static SyncMimicryS2CPacket createMimicryPacket(ServerPlayer subject)
    {
        boolean active = PlayerMimicryData.isActive(subject);
        String characterId = "";
        if (active)
        {
            String resolved = PlayerMimicryData.getActiveCharacterId(subject);
            characterId = resolved == null ? "" : resolved;
        }
        return new SyncMimicryS2CPacket(subject.getUUID(), active, characterId);
    }
}
