package com.darkness.jujutsukaisenzone.photon;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/**
 * 实体锚点上的 Photon 特效挂载 —— <b>世界可见</b>通道的唯一入口。
 * <p>
 * 全项目只有两条挂载通道，别在调用点各自手搓：
 * <ul>
 *   <li><b>世界可见</b>（本类）：服务端执行 {@code photon} 命令挂到任意实体锚点，所有玩家可见
 *       （御厨子 / 无下限 / 黑闪 / 灶 等特效实体）。</li>
 *   <li><b>仅施术者可见</b>：{@code OwnerOnlyShrineSlashFx}（owner-only 数据包，只同步给施术者）。</li>
 * </ul>
 * ⛔ 别再自己拼 {@code createCommandSourceStack().withPermission(2).withSuppressedOutput()} +
 * {@code performPrefixedCommand} —— 那正是本类要收敛的重复，直接调 {@link #perform(net.minecraft.world.entity.Entity, String)}。
 */
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
