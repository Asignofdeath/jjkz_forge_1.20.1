package com.darkness.jujutsukaisenzone.network;

import com.darkness.jujutsukaisenzone.network.registration.PlayerPacketRegistrar;
import com.darkness.jujutsukaisenzone.network.registration.TechniquePacketRegistrar;
import com.darkness.jujutsukaisenzone.network.registration.SkillPacketRegistrar;
import com.darkness.jujutsukaisenzone.network.registration.DomainPacketRegistrar;
import com.darkness.jujutsukaisenzone.network.registration.VisualPacketRegistrar;
import net.minecraftforge.network.simple.SimpleChannel;

/** Registers the main channel's packets with fixed wire IDs. */
public final class NetworkRegistration
{
    private NetworkRegistration()
    {
    }

    public static void register(SimpleChannel channel)
    {
        PlayerPacketRegistrar.register(channel);
        TechniquePacketRegistrar.register(channel);
        SkillPacketRegistrar.register(channel);
        DomainPacketRegistrar.register(channel);
        VisualPacketRegistrar.register(channel);
    }
}
