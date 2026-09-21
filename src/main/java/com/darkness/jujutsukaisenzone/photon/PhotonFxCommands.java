package com.darkness.jujutsukaisenzone.photon;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

public final class PhotonFxCommands
{
    public static final String SPOTLIGHT_MOUNT =
            "photon fx photon:spotlight entity @s 0 0 0 0 0 0 1 1 1 0 true false none";
    public static final String SPOTLIGHT_REMOVE =
            "photon fx remove entity @s false photon:spotlight";

    private PhotonFxCommands()
    {
    }

    public static void perform(Entity entity, String command)
    {
        if (!(entity.level() instanceof ServerLevel level) || level.getServer() == null)
        {
            return;
        }
        CommandSourceStack commandSource = entity.createCommandSourceStack()
                .withPermission(2)
                .withSuppressedOutput();
        level.getServer().getCommands().performPrefixedCommand(commandSource, command);
    }
}
