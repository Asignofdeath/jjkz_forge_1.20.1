package com.darkness.jujutsukaisenzone.player;

import com.darkness.jujutsukaisenzone.technique.development.SpNodeFamily;
import com.darkness.jujutsukaisenzone.technique.development.SpNodeInstance;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * 已点亮 SP 异向节点的 Attribute 施加者。幂等：先清再加。
 */
public final class PlayerSpNodeAttributeApplier
{
    private static final String UUID_SALT = "jjkz_sp_node:";
    /** 上次施加过的 instanceId，避免 reset 后残留。 */
    private static final Map<ServerPlayer, Set<String>> APPLIED = new WeakHashMap<>();

    private PlayerSpNodeAttributeApplier()
    {
    }

    public static void recompute(ServerPlayer player)
    {
        clearApplied(player);
        Set<String> now = new HashSet<>();
        for (SpNodeInstance node : PlayerSpNodeData.getLit(player))
        {
            if (!node.family().isOffAxis() || node.attributeBonus() <= 0)
            {
                continue;
            }
            var attribute = node.family().attribute();
            if (attribute == null)
            {
                continue;
            }
            AttributeInstance instance = player.getAttribute(attribute.get());
            if (instance == null)
            {
                continue;
            }
            instance.addTransientModifier(new AttributeModifier(
                    uuidFor(node.instanceId()),
                    "jjkz_sp_node_" + node.family().id(),
                    node.attributeBonus(),
                    AttributeModifier.Operation.ADDITION));
            now.add(node.instanceId());
        }
        APPLIED.put(player, now);
    }

    /** 点亮前/重置前：按给定实例列表移除修饰符。 */
    public static void clearInstances(ServerPlayer player, Iterable<SpNodeInstance> nodes)
    {
        for (SpNodeInstance node : nodes)
        {
            removeOne(player, node);
        }
        APPLIED.remove(player);
    }

    private static void clearApplied(ServerPlayer player)
    {
        Set<String> previous = APPLIED.get(player);
        if (previous != null)
        {
            for (String id : previous)
            {
                removeById(player, id);
            }
        }
        // 兜底：当前 lit 上也可能挂着未登记的修饰符
        for (SpNodeInstance node : PlayerSpNodeData.getLit(player))
        {
            removeOne(player, node);
        }
        APPLIED.remove(player);
    }

    private static void removeOne(ServerPlayer player, SpNodeInstance node)
    {
        if (node == null || !node.family().isOffAxis())
        {
            return;
        }
        removeById(player, node.instanceId());
    }

    private static void removeById(ServerPlayer player, String instanceId)
    {
        UUID uuid = uuidFor(instanceId);
        for (SpNodeFamily family : SpNodeFamily.values())
        {
            if (!family.isOffAxis() || family.attribute() == null)
            {
                continue;
            }
            AttributeInstance instance = player.getAttribute(family.attribute().get());
            if (instance != null)
            {
                instance.removeModifier(uuid);
            }
        }
    }

    private static UUID uuidFor(String instanceId)
    {
        return UUID.nameUUIDFromBytes((UUID_SALT + instanceId).getBytes(StandardCharsets.UTF_8));
    }
}
