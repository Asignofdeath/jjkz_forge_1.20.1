package com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability;

import com.darkness.jujutsukaisenzone.technique.shikigami.ShikigamiAbility;
import com.darkness.jujutsukaisenzone.technique.shikigami.ShikigamiAbilitySource;
import com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.TenShadowsFieldQuery;
import com.darkness.jujutsukaisenzone.technique.skill.TenShadowsShikigami;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 鵺 · 俯冲（自动）：自高处冲向仇恨目标并撞击。
 */
public final class NueDiveLogic
{
    private static final double MIN_HEIGHT_ABOVE = 3.5D;
    private static final double MAX_RANGE = 16.0D;
    private static final double MIN_RANGE = 4.0D;
    private static final float DAMAGE = 10.0F;

    private NueDiveLogic()
    {
    }

    public static boolean cast(ServerPlayer owner, ShikigamiAbility ability, ShikigamiAbilitySource source)
    {
        List<Mob> nues = TenShadowsFieldQuery.aliveMobs(owner, TenShadowsShikigami.NUE);
        if (nues.isEmpty())
        {
            return false;
        }
        Mob nue = nues.get(0);
        if (owner.getVehicle() == nue)
        {
            return false;
        }
        LivingEntity target = nue.getTarget();
        if (target == null || !target.isAlive())
        {
            return false;
        }
        double distSq = nue.distanceToSqr(target);
        if (distSq < MIN_RANGE * MIN_RANGE || distSq > MAX_RANGE * MAX_RANGE)
        {
            return false;
        }
        double heightAbove = nue.getY() - target.getY();
        if (heightAbove < MIN_HEIGHT_ABOVE)
        {
            return false;
        }

        Vec3 delta = target.position().add(0.0D, 0.2D, 0.0D).subtract(nue.position());
        if (delta.lengthSqr() < 1.0E-4D)
        {
            return false;
        }
        Vec3 dive = delta.normalize().scale(1.55D);
        nue.setDeltaMovement(dive.x, Math.min(dive.y, -0.35D), dive.z);
        nue.hurtMarked = true;

        ServerLevel level = owner.serverLevel();
        level.sendParticles(
                ParticleTypes.CLOUD,
                nue.getX(), nue.getY(), nue.getZ(),
                10, 0.3D, 0.2D, 0.3D, 0.02D);

        if (nue.distanceToSqr(target) <= 9.0D)
        {
            target.hurt(nue.damageSources().mobAttack(nue), DAMAGE);
        }
        else
        {
            /* 冲刺途中贴近时补一刀：短时标记，由 tick 结算 */
            nue.getPersistentData().putUUID(NueDiveImpactTicker.TAG_DIVE_TARGET, target.getUUID());
            nue.getPersistentData().putInt(NueDiveImpactTicker.TAG_DIVE_TICKS, 12);
        }
        return true;
    }
}
