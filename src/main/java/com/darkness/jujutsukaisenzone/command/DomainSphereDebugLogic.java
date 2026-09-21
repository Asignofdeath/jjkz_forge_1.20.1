package com.darkness.jujutsukaisenzone.command;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.technique.DomainExpansionServerLogic;
import com.darkness.jujutsukaisenzone.technique.domain.AbstractDomainExpansion;
import com.darkness.jujutsukaisenzone.technique.domain.InnateDomainExperienceManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 调试命令：在玩家位置放置领域球并指定天空盒纹理。
 */
public final class DomainSphereDebugLogic
{
    private static final ConcurrentMap<UUID, AbstractDomainExpansion.InnateArenaHandle> ACTIVE_BY_PLAYER =
            new ConcurrentHashMap<>();

    private DomainSphereDebugLogic()
    {
    }

    public static boolean place(ServerPlayer player, String skyboxArg, int radius)
    {
        if (radius < 4 || radius > 64)
        {
            player.sendSystemMessage(Component.literal("半径需在 4～64 之间。"));
            return false;
        }

        ResourceLocation skyboxTexture = parseSkyboxTexture(skyboxArg);
        if (skyboxTexture == null)
        {
            player.sendSystemMessage(Component.literal("天空盒路径无效：" + skyboxArg));
            return false;
        }

        close(player);

        ServerLevel level = player.serverLevel();
        BlockPos center = player.blockPosition();
        int playerFootY = player.getBlockY() - 1;
        AbstractDomainExpansion.InnateArenaHandle handle = DomainExpansionServerLogic.getInstance()
                .buildInnateDomainArena(player, level, center, radius, playerFootY, skyboxTexture);
        if (handle == null)
        {
            player.sendSystemMessage(Component.literal("领域球放置失败。"));
            return false;
        }

        ACTIVE_BY_PLAYER.put(player.getUUID(), handle);
        player.sendSystemMessage(Component.literal(
                "已放置调试领域球：中心 "
                        + center.toShortString()
                        + "，半径 "
                        + radius
                        + "，天空盒 "
                        + skyboxTexture));
        return true;
    }

    public static boolean close(ServerPlayer player)
    {
        AbstractDomainExpansion.InnateArenaHandle handle = ACTIVE_BY_PLAYER.remove(player.getUUID());
        if (handle == null)
        {
            player.sendSystemMessage(Component.literal("当前没有由调试命令放置的领域球。"));
            return false;
        }

        DomainExpansionServerLogic.getInstance().dismantleInnateDomainArena(player, handle);
        player.sendSystemMessage(Component.literal("已移除调试领域球。"));
        return true;
    }

    public static int defaultRadius()
    {
        return InnateDomainExperienceManager.DOMAIN_RADIUS;
    }

    static ResourceLocation parseSkyboxTexture(String raw)
    {
        if (raw == null || raw.isBlank())
        {
            return null;
        }

        String trimmed = raw.trim().replace('\\', '/');
        if (trimmed.startsWith("assets/"))
        {
            trimmed = trimmed.substring("assets/".length());
        }

        if (trimmed.contains(":"))
        {
            return ResourceLocation.tryParse(trimmed);
        }

        int slash = trimmed.indexOf('/');
        if (slash > 0)
        {
            String namespace = trimmed.substring(0, slash);
            String path = trimmed.substring(slash + 1);
            if (namespace.equals(JuJutsuKaisenZone.MODID))
            {
                return ResourceLocation.fromNamespaceAndPath(namespace, path);
            }
        }

        String path = trimmed.startsWith("textures/") ? trimmed : "textures/environment/" + trimmed;
        return ResourceLocation.fromNamespaceAndPath(JuJutsuKaisenZone.MODID, path);
    }
}
