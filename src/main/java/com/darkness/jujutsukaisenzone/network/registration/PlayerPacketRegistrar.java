package com.darkness.jujutsukaisenzone.network.registration;

import com.darkness.jujutsukaisenzone.network.packet.SyncCursedEnergyS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncFactionS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncCursedGradeS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncCursedEnergyTraitS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.AerialMultiJumpC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.SelectStarMarkC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.StarMarkClearC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncStarMarksS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncCombatPowerS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncLocateGuidanceS2CPacket;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.simple.SimpleChannel;

public final class PlayerPacketRegistrar
{
    private PlayerPacketRegistrar()
    {
    }

    public static void register(SimpleChannel channel)
    {
        channel.messageBuilder(SyncCursedEnergyS2CPacket.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncCursedEnergyS2CPacket::encode)
                .decoder(SyncCursedEnergyS2CPacket::decode)
                .consumerMainThread(SyncCursedEnergyS2CPacket::handle)
                .add();
        channel.messageBuilder(SyncFactionS2CPacket.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncFactionS2CPacket::encode)
                .decoder(SyncFactionS2CPacket::decode)
                .consumerMainThread(SyncFactionS2CPacket::handle)
                .add();
        channel.messageBuilder(SyncCursedGradeS2CPacket.class, 2, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncCursedGradeS2CPacket::encode)
                .decoder(SyncCursedGradeS2CPacket::decode)
                .consumerMainThread(SyncCursedGradeS2CPacket::handle)
                .add();
        channel.messageBuilder(SyncCursedEnergyTraitS2CPacket.class, 66, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncCursedEnergyTraitS2CPacket::encode)
                .decoder(SyncCursedEnergyTraitS2CPacket::decode)
                .consumerMainThread(SyncCursedEnergyTraitS2CPacket::handle)
                .add();
        channel.messageBuilder(AerialMultiJumpC2SPacket.class, 90, NetworkDirection.PLAY_TO_SERVER)
                .encoder(AerialMultiJumpC2SPacket::encode)
                .decoder(AerialMultiJumpC2SPacket::decode)
                .consumerMainThread(AerialMultiJumpC2SPacket::handle)
                .add();
        channel.messageBuilder(SelectStarMarkC2SPacket.class, 91, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SelectStarMarkC2SPacket::encode)
                .decoder(SelectStarMarkC2SPacket::decode)
                .consumerMainThread(SelectStarMarkC2SPacket::handle)
                .add();
        channel.messageBuilder(StarMarkClearC2SPacket.class, 92, NetworkDirection.PLAY_TO_SERVER)
                .encoder(StarMarkClearC2SPacket::encode)
                .decoder(StarMarkClearC2SPacket::decode)
                .consumerMainThread(StarMarkClearC2SPacket::handle)
                .add();
        channel.messageBuilder(SyncStarMarksS2CPacket.class, 93, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncStarMarksS2CPacket::encode)
                .decoder(SyncStarMarksS2CPacket::decode)
                .consumerMainThread(SyncStarMarksS2CPacket::handle)
                .add();
        channel.messageBuilder(SyncCombatPowerS2CPacket.class, 94, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncCombatPowerS2CPacket::encode)
                .decoder(SyncCombatPowerS2CPacket::decode)
                .consumerMainThread(SyncCombatPowerS2CPacket::handle)
                .add();
        channel.messageBuilder(SyncLocateGuidanceS2CPacket.class, 104, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncLocateGuidanceS2CPacket::encode)
                .decoder(SyncLocateGuidanceS2CPacket::decode)
                .consumerMainThread(SyncLocateGuidanceS2CPacket::handle)
                .add();
    }
}