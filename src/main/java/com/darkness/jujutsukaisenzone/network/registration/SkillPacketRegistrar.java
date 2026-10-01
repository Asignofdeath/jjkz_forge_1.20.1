package com.darkness.jujutsukaisenzone.network.registration;

import com.darkness.jujutsukaisenzone.network.packet.TechniqueSkillUseC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncSkillBarConfigC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncSkillBarConfigS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncSkillBarConfigRequestC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncSpS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncSpNodesS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncSeLoadoutS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncSkillLevelsS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.UpgradeSkillC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.UpgradeSkillS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.UnlockSkillC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.UnlockSkillS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.UnlockSpNodeC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.ResetSpNodesC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.RefreshSpOffersC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.EquipSeC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.UnequipSeC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncSkillUnlocksS2CPacket;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.simple.SimpleChannel;

public final class SkillPacketRegistrar
{
    private SkillPacketRegistrar()
    {
    }

    public static void register(SimpleChannel channel)
    {
        channel.messageBuilder(TechniqueSkillUseC2SPacket.class, 7, NetworkDirection.PLAY_TO_SERVER)
                .encoder(TechniqueSkillUseC2SPacket::encode)
                .decoder(TechniqueSkillUseC2SPacket::decode)
                .consumerMainThread(TechniqueSkillUseC2SPacket::handle)
                .add();
        channel.messageBuilder(SyncSkillBarConfigC2SPacket.class, 51, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SyncSkillBarConfigC2SPacket::encode)
                .decoder(SyncSkillBarConfigC2SPacket::decode)
                .consumerMainThread(SyncSkillBarConfigC2SPacket::handle)
                .add();
        channel.messageBuilder(SyncSkillBarConfigS2CPacket.class, 52, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncSkillBarConfigS2CPacket::encode)
                .decoder(SyncSkillBarConfigS2CPacket::decode)
                .consumerMainThread(SyncSkillBarConfigS2CPacket::handle)
                .add();
        channel.messageBuilder(SyncSkillBarConfigRequestC2SPacket.class, 53, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SyncSkillBarConfigRequestC2SPacket::encode)
                .decoder(SyncSkillBarConfigRequestC2SPacket::decode)
                .consumerMainThread(SyncSkillBarConfigRequestC2SPacket::handle)
                .add();
        channel.messageBuilder(SyncSpS2CPacket.class, 63, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncSpS2CPacket::encode)
                .decoder(SyncSpS2CPacket::decode)
                .consumerMainThread(SyncSpS2CPacket::handle)
                .add();
        channel.messageBuilder(SyncSpNodesS2CPacket.class, 64, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncSpNodesS2CPacket::encode)
                .decoder(SyncSpNodesS2CPacket::decode)
                .consumerMainThread(SyncSpNodesS2CPacket::handle)
                .add();
        channel.messageBuilder(SyncSeLoadoutS2CPacket.class, 65, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncSeLoadoutS2CPacket::encode)
                .decoder(SyncSeLoadoutS2CPacket::decode)
                .consumerMainThread(SyncSeLoadoutS2CPacket::handle)
                .add();
        channel.messageBuilder(SyncSkillLevelsS2CPacket.class, 68, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncSkillLevelsS2CPacket::encode)
                .decoder(SyncSkillLevelsS2CPacket::decode)
                .consumerMainThread(SyncSkillLevelsS2CPacket::handle)
                .add();
        channel.messageBuilder(UpgradeSkillC2SPacket.class, 69, NetworkDirection.PLAY_TO_SERVER)
                .encoder(UpgradeSkillC2SPacket::encode)
                .decoder(UpgradeSkillC2SPacket::decode)
                .consumerMainThread(UpgradeSkillC2SPacket::handle)
                .add();
        channel.messageBuilder(UpgradeSkillS2CPacket.class, 70, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(UpgradeSkillS2CPacket::encode)
                .decoder(UpgradeSkillS2CPacket::decode)
                .consumerMainThread(UpgradeSkillS2CPacket::handle)
                .add();
        channel.messageBuilder(UnlockSkillC2SPacket.class, 71, NetworkDirection.PLAY_TO_SERVER)
                .encoder(UnlockSkillC2SPacket::encode)
                .decoder(UnlockSkillC2SPacket::decode)
                .consumerMainThread(UnlockSkillC2SPacket::handle)
                .add();
        channel.messageBuilder(UnlockSkillS2CPacket.class, 72, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(UnlockSkillS2CPacket::encode)
                .decoder(UnlockSkillS2CPacket::decode)
                .consumerMainThread(UnlockSkillS2CPacket::handle)
                .add();
        channel.messageBuilder(UnlockSpNodeC2SPacket.class, 73, NetworkDirection.PLAY_TO_SERVER)
                .encoder(UnlockSpNodeC2SPacket::encode)
                .decoder(UnlockSpNodeC2SPacket::decode)
                .consumerMainThread(UnlockSpNodeC2SPacket::handle)
                .add();
        channel.messageBuilder(ResetSpNodesC2SPacket.class, 74, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ResetSpNodesC2SPacket::encode)
                .decoder(ResetSpNodesC2SPacket::decode)
                .consumerMainThread(ResetSpNodesC2SPacket::handle)
                .add();
        channel.messageBuilder(RefreshSpOffersC2SPacket.class, 75, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RefreshSpOffersC2SPacket::encode)
                .decoder(RefreshSpOffersC2SPacket::decode)
                .consumerMainThread(RefreshSpOffersC2SPacket::handle)
                .add();
        channel.messageBuilder(EquipSeC2SPacket.class, 76, NetworkDirection.PLAY_TO_SERVER)
                .encoder(EquipSeC2SPacket::encode)
                .decoder(EquipSeC2SPacket::decode)
                .consumerMainThread(EquipSeC2SPacket::handle)
                .add();
        channel.messageBuilder(UnequipSeC2SPacket.class, 77, NetworkDirection.PLAY_TO_SERVER)
                .encoder(UnequipSeC2SPacket::encode)
                .decoder(UnequipSeC2SPacket::decode)
                .consumerMainThread(UnequipSeC2SPacket::handle)
                .add();
        channel.messageBuilder(SyncSkillUnlocksS2CPacket.class, 78, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncSkillUnlocksS2CPacket::encode)
                .decoder(SyncSkillUnlocksS2CPacket::decode)
                .consumerMainThread(SyncSkillUnlocksS2CPacket::handle)
                .add();
    }
}
