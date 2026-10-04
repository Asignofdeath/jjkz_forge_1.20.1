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
import com.darkness.jujutsukaisenzone.network.packet.SyncModConfigS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncQuestProgressS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncQuestMissionBriefingS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncQuestTrackS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.TrackQuestC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncQuestBlackoutS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncQuestLetterboxS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncCgLookClampS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.MainQuest11CabinCmdCamS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncMusicPlayS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncMusicStopS2CPacket;
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
        channel.messageBuilder(SyncModConfigS2CPacket.class, 105, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncModConfigS2CPacket::encode)
                .decoder(SyncModConfigS2CPacket::decode)
                .consumerMainThread(SyncModConfigS2CPacket::handle)
                .add();
        channel.messageBuilder(SyncQuestTrackS2CPacket.class, 106, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncQuestTrackS2CPacket::encode)
                .decoder(SyncQuestTrackS2CPacket::decode)
                .consumerMainThread(SyncQuestTrackS2CPacket::handle)
                .add();
        channel.messageBuilder(SyncQuestProgressS2CPacket.class, 107, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncQuestProgressS2CPacket::encode)
                .decoder(SyncQuestProgressS2CPacket::decode)
                .consumerMainThread(SyncQuestProgressS2CPacket::handle)
                .add();
        channel.messageBuilder(TrackQuestC2SPacket.class, 108, NetworkDirection.PLAY_TO_SERVER)
                .encoder(TrackQuestC2SPacket::encode)
                .decoder(TrackQuestC2SPacket::decode)
                .consumerMainThread(TrackQuestC2SPacket::handle)
                .add();
        channel.messageBuilder(SyncQuestBlackoutS2CPacket.class, 109, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncQuestBlackoutS2CPacket::encode)
                .decoder(SyncQuestBlackoutS2CPacket::decode)
                .consumerMainThread(SyncQuestBlackoutS2CPacket::handle)
                .add();
        channel.messageBuilder(MainQuest11CabinCmdCamS2CPacket.class, 110, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(MainQuest11CabinCmdCamS2CPacket::encode)
                .decoder(MainQuest11CabinCmdCamS2CPacket::decode)
                .consumerMainThread(MainQuest11CabinCmdCamS2CPacket::handle)
                .add();
        channel.messageBuilder(SyncQuestLetterboxS2CPacket.class, 111, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncQuestLetterboxS2CPacket::encode)
                .decoder(SyncQuestLetterboxS2CPacket::decode)
                .consumerMainThread(SyncQuestLetterboxS2CPacket::handle)
                .add();
        channel.messageBuilder(SyncCgLookClampS2CPacket.class, 112, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncCgLookClampS2CPacket::encode)
                .decoder(SyncCgLookClampS2CPacket::decode)
                .consumerMainThread(SyncCgLookClampS2CPacket::handle)
                .add();
        channel.messageBuilder(SyncMusicPlayS2CPacket.class, 113, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncMusicPlayS2CPacket::encode)
                .decoder(SyncMusicPlayS2CPacket::decode)
                .consumerMainThread(SyncMusicPlayS2CPacket::handle)
                .add();
        channel.messageBuilder(SyncMusicStopS2CPacket.class, 114, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncMusicStopS2CPacket::encode)
                .decoder(SyncMusicStopS2CPacket::decode)
                .consumerMainThread(SyncMusicStopS2CPacket::handle)
                .add();
        channel.messageBuilder(SyncQuestMissionBriefingS2CPacket.class, 115, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncQuestMissionBriefingS2CPacket::encode)
                .decoder(SyncQuestMissionBriefingS2CPacket::decode)
                .consumerMainThread(SyncQuestMissionBriefingS2CPacket::handle)
                .add();
    }
}