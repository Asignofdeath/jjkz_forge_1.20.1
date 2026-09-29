package com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability;

import com.darkness.jujutsukaisenzone.technique.skill.TenShadowsShikigami;
import com.darkness.jujutsukaisenzone.technique.skill.TenShadowsSummonLogic;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.UUID;

/** 鵺俯冲撞击的短时窗口结算。 */
public final class NueDiveImpactTicker
{
    public static final String TAG_DIVE_TARGET = "jujutsukaisenzone_nue_dive_target";
    public static final String TAG_DIVE_TICKS = "jujutsukaisenzone_nue_dive_ticks";
    private static final float DAMAGE = 10.0F;
    private static final double HIT_RANGE_SQ = 3.5D * 3.5D;

    private NueDiveImpactTicker()
    {
    }

    public static void tickMob(Mob mob)
    {
        if (mob.level().isClientSide || !(mob.level() instanceof ServerLevel level))
        {
            return;
        }
        if (!TenShadowsSummonLogic.isFriendlyOwned(mob))
        {
            return;
        }
        if (mob.getPersistentData().getInt(TenShadowsSummonLogic.TAG_INDEX) != TenShadowsShikigami.NUE.index())
        {
            return;
        }
        int remain = mob.getPersistentData().getInt(TAG_DIVE_TICKS);
        if (remain <= 0 || !mob.getPersistentData().hasUUID(TAG_DIVE_TARGET))
        {
            return;
        }
        remain--;
        if (remain <= 0)
        {
            clear(mob);
            return;
        }
        mob.getPersistentData().putInt(TAG_DIVE_TICKS, remain);
        UUID targetId = mob.getPersistentData().getUUID(TAG_DIVE_TARGET);
        Entity entity = level.getEntity(targetId);
        if (!(entity instanceof LivingEntity target) || !target.isAlive())
        {
            clear(mob);
            return;
        }
        if (mob.distanceToSqr(target) <= HIT_RANGE_SQ)
        {
            target.hurt(mob.damageSources().mobAttack(mob), DAMAGE);
            clear(mob);
        }
    }

    private static void clear(Mob mob)
    {
        mob.getPersistentData().remove(TAG_DIVE_TARGET);
        mob.getPersistentData().remove(TAG_DIVE_TICKS);
    }
}
