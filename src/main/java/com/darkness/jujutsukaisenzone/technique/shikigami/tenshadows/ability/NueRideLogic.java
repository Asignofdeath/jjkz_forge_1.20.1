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

import java.util.List;

/**
 * 鵺 · 骑乘（主动，Ctrl+2）：抓住爪下飞行。再按一次或潜行下车。
 * <p>
 * 不上式神公用 CD（{@code cooldownTicks = 0}），避免挡雷击 / 俯冲。
 */
public final class NueRideLogic
{
    public static final String TAG_RIDE = "jujutsukaisenzone_nue_ride";

    private NueRideLogic()
    {
    }

    public static boolean cast(ServerPlayer owner, ShikigamiAbility ability, ShikigamiAbilitySource source)
    {
        if (source != ShikigamiAbilitySource.PLAYER)
        {
            return false;
        }
        List<Mob> nues = TenShadowsFieldQuery.aliveMobs(owner, TenShadowsShikigami.NUE);
        if (nues.isEmpty())
        {
            owner.displayClientMessage(
                    Component.literal("鵺 · 骑乘：场上没有鵺").withStyle(ChatFormatting.GRAY),
                    true);
            return false;
        }
        Mob nue = nues.get(0);

        if (owner.getVehicle() == nue)
        {
            owner.stopRiding();
            nue.getPersistentData().remove(TAG_RIDE);
            owner.displayClientMessage(
                    Component.literal("鵺 · 着陆").withStyle(ChatFormatting.GRAY),
                    true);
            return true;
        }
        if (owner.isPassenger())
        {
            owner.displayClientMessage(
                    Component.literal("鵺 · 骑乘：请先下车").withStyle(ChatFormatting.GRAY),
                    true);
            return false;
        }

        /* force：幻翼默认不接受乘客 */
        if (!owner.startRiding(nue, true))
        {
            owner.displayClientMessage(
                    Component.literal("鵺 · 骑乘失败").withStyle(ChatFormatting.RED),
                    true);
            return false;
        }
        nue.getPersistentData().putBoolean(TAG_RIDE, true);
        nue.setTarget(null);
        owner.displayClientMessage(
                Component.literal("鵺 · 骑乘（潜行或再按 Ctrl+2 下车）").withStyle(ChatFormatting.AQUA),
                true);
        return true;
    }

    public static boolean isNueRideVehicle(ServerPlayer owner, Mob mob)
    {
        return owner != null
                && mob != null
                && owner.getVehicle() == mob
                && TenShadowsSummonLogic.isOwnedBy(mob, owner.getUUID())
                && mob.getPersistentData().getInt(TenShadowsSummonLogic.TAG_INDEX)
                == TenShadowsShikigami.NUE.index();
    }
}
