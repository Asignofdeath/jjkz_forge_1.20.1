package com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability;

import com.darkness.jujutsukaisenzone.technique.shikigami.ShikigamiAbility;
import com.darkness.jujutsukaisenzone.technique.shikigami.ShikigamiAbilitySource;
import com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.TenShadowsFieldQuery;
import com.darkness.jujutsukaisenzone.technique.skill.TenShadowsShikigami;
import com.darkness.jujutsukaisenzone.technique.skill.TenShadowsSummonLogic;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 脱兔 · 爆发（主动，Shift+6）：额外刷出一批脱兔并入名册。
 */
public final class RabbitEscapeBurstLogic
{
    /** 常规召出 8，爆发再 +12。 */
    public static final int BURST_EXTRA = 12;

    private RabbitEscapeBurstLogic()
    {
    }

    public static boolean cast(ServerPlayer owner, ShikigamiAbility ability, ShikigamiAbilitySource source)
    {
        if (source != ShikigamiAbilitySource.PLAYER)
        {
            return false;
        }
        List<Mob> existing = TenShadowsFieldQuery.aliveMobs(owner, TenShadowsShikigami.RABBIT_ESCAPE);
        Vec3 center;
        if (!existing.isEmpty())
        {
            center = existing.get(0).position();
        }
        else
        {
            /* 未召出时也允许爆发：在身前起巢 */
            Vec3 look = owner.getLookAngle();
            center = owner.position().add(look.x * 1.5D, 0.0D, look.z * 1.5D);
        }

        int spawned = TenShadowsSummonLogic.appendSummons(
                owner, TenShadowsShikigami.RABBIT_ESCAPE, BURST_EXTRA, center);
        if (spawned <= 0)
        {
            owner.displayClientMessage(
                    Component.literal("脱兔 · 爆发失败").withStyle(ChatFormatting.RED),
                    true);
            return false;
        }
        owner.displayClientMessage(
                Component.literal("脱兔 · 爆发 +" + spawned).withStyle(ChatFormatting.LIGHT_PURPLE),
                true);
        return true;
    }
}
