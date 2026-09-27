package com.darkness.jujutsukaisenzone.player;

import com.darkness.jujutsukaisenzone.technique.se.PanelStat;
import com.darkness.jujutsukaisenzone.technique.se.SeEffectDef;
import com.darkness.jujutsukaisenzone.technique.se.SeEffectRegistry;
import com.darkness.jujutsukaisenzone.technique.se.SePenalty;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

/**
 * SE 代价在 <b>Attribute 层</b>的施加者（含原版最大生命）。
 * <p>
 * 唯一入口 {@link #recompute(ServerPlayer)}：先把本模组写过的 SE 修饰符<b>全部清掉</b>，
 * 再按当前装载实例代价重新施加。幂等。
 * <p>
 * 面板强度行走 {@code PlayerCombatPowerData}；咒力上限 / 领域量表满条 / 脑损上限
 * 走各自 Data 类对 {@link PlayerSeLoadoutData#sumPenaltyAmount} 的读取。
 */
public final class PlayerSePenaltyApplier
{
    private static final String UUID_SALT = "jjkz_se_penalty:";

    private PlayerSePenaltyApplier()
    {
    }

    /** 按当前装载重算全部 Attribute 层 SE 代价。幂等。 */
    public static void recompute(ServerPlayer player)
    {
        clearAll(player);
        for (Map.Entry<String, SeEquippedEntry> e : PlayerSeLoadoutData.getEquippedMap(player).entrySet())
        {
            SePenalty penalty = e.getValue().penalty;
            PanelStat.ApplyKind kind = penalty.stat().applyKind();
            if (kind != PanelStat.ApplyKind.ATTRIBUTE_SUBTRACT)
            {
                continue;
            }
            AttributeInstance instance = resolveAttribute(player, penalty.stat());
            if (instance == null)
            {
                continue;
            }
            instance.addTransientModifier(new AttributeModifier(
                    uuidFor(e.getKey()),
                    "jjkz_se_penalty_" + e.getKey(),
                    penalty.attributeDelta(),
                    AttributeModifier.Operation.ADDITION));
        }
    }

    private static AttributeInstance resolveAttribute(ServerPlayer player, PanelStat stat)
    {
        if (stat.usesVanillaMaxHealth())
        {
            return player.getAttribute(Attributes.MAX_HEALTH);
        }
        var attribute = stat.attribute();
        if (attribute == null)
        {
            return null;
        }
        return player.getAttribute(attribute.get());
    }

    /**
     * 清掉全部可能存在的 SE 代价修饰符。
     * 遍历注册表全部 SE id（而非仅当前装载），以免卸下后残留。
     */
    private static void clearAll(ServerPlayer player)
    {
        for (SeEffectDef def : SeEffectRegistry.all())
        {
            UUID uuid = uuidFor(def.seId());
            AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
            if (maxHealth != null)
            {
                maxHealth.removeModifier(uuid);
            }
            for (PanelStat stat : PanelStat.values())
            {
                var attribute = stat.attribute();
                if (attribute == null)
                {
                    continue;
                }
                AttributeInstance instance = player.getAttribute(attribute.get());
                if (instance != null)
                {
                    instance.removeModifier(uuid);
                }
            }
        }
    }

    private static UUID uuidFor(String seId)
    {
        return UUID.nameUUIDFromBytes((UUID_SALT + seId).getBytes(StandardCharsets.UTF_8));
    }
}
