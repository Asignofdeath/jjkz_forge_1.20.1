package com.darkness.jujutsukaisenzone.network.registration;

import com.darkness.jujutsukaisenzone.network.packet.SyncReverseTechniqueS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncTechniqueS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncMaxOutputChargeInterruptS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.ReverseTechniqueTickC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.TechniqueSpecialMoveC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.AdjustCastRangeTierC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.TechniqueMaxOutputC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.TechniqueHoldReleaseC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncTenShadowsModeS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.TenShadowsShikigamiC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.ShikigamiAbilityC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncTenShadowsContractS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncTenShadowsFalseDeathS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.ToggleMimicryC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncMimicryS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncSeanceTechniqueLearnedS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncStrengthenTechniqueLearnedS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.EmotionalReleaseC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncEmotionalReleaseStateS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncTechniqueIntelS2CPacket;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.simple.SimpleChannel;

public final class TechniquePacketRegistrar
{
    private TechniquePacketRegistrar()
    {
    }

    public static void register(SimpleChannel channel)
    {
        channel.messageBuilder(SyncReverseTechniqueS2CPacket.class, 3, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncReverseTechniqueS2CPacket::encode)
                .decoder(SyncReverseTechniqueS2CPacket::decode)
                .consumerMainThread(SyncReverseTechniqueS2CPacket::handle)
                .add();
        channel.messageBuilder(SyncTechniqueS2CPacket.class, 4, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncTechniqueS2CPacket::encode)
                .decoder(SyncTechniqueS2CPacket::decode)
                .consumerMainThread(SyncTechniqueS2CPacket::handle)
                .add();
        channel.messageBuilder(SyncMaxOutputChargeInterruptS2CPacket.class, 5, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncMaxOutputChargeInterruptS2CPacket::encode)
                .decoder(SyncMaxOutputChargeInterruptS2CPacket::decode)
                .consumerMainThread(SyncMaxOutputChargeInterruptS2CPacket::handle)
                .add();
        channel.messageBuilder(ReverseTechniqueTickC2SPacket.class, 6, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ReverseTechniqueTickC2SPacket::encode)
                .decoder(ReverseTechniqueTickC2SPacket::decode)
                .consumerMainThread(ReverseTechniqueTickC2SPacket::handle)
                .add();
        channel.messageBuilder(TechniqueSpecialMoveC2SPacket.class, 8, NetworkDirection.PLAY_TO_SERVER)
                .encoder(TechniqueSpecialMoveC2SPacket::encode)
                .decoder(TechniqueSpecialMoveC2SPacket::decode)
                .consumerMainThread(TechniqueSpecialMoveC2SPacket::handle)
                .add();
        channel.messageBuilder(AdjustCastRangeTierC2SPacket.class, 9, NetworkDirection.PLAY_TO_SERVER)
                .encoder(AdjustCastRangeTierC2SPacket::encode)
                .decoder(AdjustCastRangeTierC2SPacket::decode)
                .consumerMainThread(AdjustCastRangeTierC2SPacket::handle)
                .add();
        channel.messageBuilder(TechniqueMaxOutputC2SPacket.class, 11, NetworkDirection.PLAY_TO_SERVER)
                .encoder(TechniqueMaxOutputC2SPacket::encode)
                .decoder(TechniqueMaxOutputC2SPacket::decode)
                .consumerMainThread(TechniqueMaxOutputC2SPacket::handle)
                .add();
        channel.messageBuilder(TechniqueHoldReleaseC2SPacket.class, 12, NetworkDirection.PLAY_TO_SERVER)
                .encoder(TechniqueHoldReleaseC2SPacket::encode)
                .decoder(TechniqueHoldReleaseC2SPacket::decode)
                .consumerMainThread(TechniqueHoldReleaseC2SPacket::handle)
                .add();
        channel.messageBuilder(SyncTenShadowsModeS2CPacket.class, 20, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncTenShadowsModeS2CPacket::encode)
                .decoder(SyncTenShadowsModeS2CPacket::decode)
                .consumerMainThread(SyncTenShadowsModeS2CPacket::handle)
                .add();
        channel.messageBuilder(TenShadowsShikigamiC2SPacket.class, 21, NetworkDirection.PLAY_TO_SERVER)
                .encoder(TenShadowsShikigamiC2SPacket::encode)
                .decoder(TenShadowsShikigamiC2SPacket::decode)
                .consumerMainThread(TenShadowsShikigamiC2SPacket::handle)
                .add();
        channel.messageBuilder(ShikigamiAbilityC2SPacket.class, 22, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ShikigamiAbilityC2SPacket::encode)
                .decoder(ShikigamiAbilityC2SPacket::decode)
                .consumerMainThread(ShikigamiAbilityC2SPacket::handle)
                .add();
        channel.messageBuilder(SyncTenShadowsContractS2CPacket.class, 23, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncTenShadowsContractS2CPacket::encode)
                .decoder(SyncTenShadowsContractS2CPacket::decode)
                .consumerMainThread(SyncTenShadowsContractS2CPacket::handle)
                .add();
        channel.messageBuilder(SyncTenShadowsFalseDeathS2CPacket.class, 24, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncTenShadowsFalseDeathS2CPacket::encode)
                .decoder(SyncTenShadowsFalseDeathS2CPacket::decode)
                .consumerMainThread(SyncTenShadowsFalseDeathS2CPacket::handle)
                .add();
        channel.messageBuilder(ToggleMimicryC2SPacket.class, 54, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ToggleMimicryC2SPacket::encode)
                .decoder(ToggleMimicryC2SPacket::decode)
                .consumerMainThread(ToggleMimicryC2SPacket::handle)
                .add();
        channel.messageBuilder(SyncMimicryS2CPacket.class, 55, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncMimicryS2CPacket::encode)
                .decoder(SyncMimicryS2CPacket::decode)
                .consumerMainThread(SyncMimicryS2CPacket::handle)
                .add();
        channel.messageBuilder(SyncSeanceTechniqueLearnedS2CPacket.class, 56, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncSeanceTechniqueLearnedS2CPacket::encode)
                .decoder(SyncSeanceTechniqueLearnedS2CPacket::decode)
                .consumerMainThread(SyncSeanceTechniqueLearnedS2CPacket::handle)
                .add();
        channel.messageBuilder(SyncStrengthenTechniqueLearnedS2CPacket.class, 57, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncStrengthenTechniqueLearnedS2CPacket::encode)
                .decoder(SyncStrengthenTechniqueLearnedS2CPacket::decode)
                .consumerMainThread(SyncStrengthenTechniqueLearnedS2CPacket::handle)
                .add();
        channel.messageBuilder(EmotionalReleaseC2SPacket.class, 58, NetworkDirection.PLAY_TO_SERVER)
                .encoder(EmotionalReleaseC2SPacket::encode)
                .decoder(EmotionalReleaseC2SPacket::decode)
                .consumerMainThread(EmotionalReleaseC2SPacket::handle)
                .add();
        channel.messageBuilder(SyncEmotionalReleaseStateS2CPacket.class, 59, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncEmotionalReleaseStateS2CPacket::encode)
                .decoder(SyncEmotionalReleaseStateS2CPacket::decode)
                .consumerMainThread(SyncEmotionalReleaseStateS2CPacket::handle)
                .add();
        channel.messageBuilder(SyncTechniqueIntelS2CPacket.class, 62, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncTechniqueIntelS2CPacket::encode)
                .decoder(SyncTechniqueIntelS2CPacket::decode)
                .consumerMainThread(SyncTechniqueIntelS2CPacket::handle)
                .add();
    }
}
