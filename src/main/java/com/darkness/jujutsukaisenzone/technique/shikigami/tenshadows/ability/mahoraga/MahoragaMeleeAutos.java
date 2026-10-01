package com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability.mahoraga;

import com.darkness.jujutsukaisenzone.technique.TechniqueDamageSources;
import com.darkness.jujutsukaisenzone.technique.shikigami.ShikigamiAbility;
import com.darkness.jujutsukaisenzone.technique.shikigami.ShikigamiAbilitySource;
import com.darkness.jujutsukaisenzone.technique.skill.TenShadowsSummonLogic;
import com.darkness.jujutsukaisenzone.technique.skill.tenshadows.TenShadowsShikigamiSkillRatios;
import com.darkness.jujutsukaisenzone.technique.skillset.SkillDamageType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** 魔虚罗自动近战组：砸地 / 击飞 / 投掷 / 拳击 / 挥剑 / 劈径。 */
public final class MahoragaMeleeAutos
{
    private MahoragaMeleeAutos()
    {
    }

    public static boolean castSlam(ServerPlayer owner, ShikigamiAbility ability, ShikigamiAbilitySource source)
    {
        Mob m = MahoragaSupport.first(owner);
        LivingEntity t = requireNear(owner, m, 5.0D);
        if (m == null || t == null)
        {
            return false;
        }
        ServerLevel level = owner.serverLevel();
        level.sendParticles(ParticleTypes.EXPLOSION, m.getX(), m.getY(), m.getZ(), 4, 1.2D, 0.1D, 1.2D, 0.02D);
        AABB box = m.getBoundingBox().inflate(4.0D, 1.0D, 4.0D);
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, box, e ->
                e.isAlive() && e != owner && e != m && !TenShadowsSummonLogic.isOwnedBy(e, owner.getUUID())))
        {
            TechniqueDamageSources.deal(living, m, TenShadowsShikigamiSkillRatios.MAHORAGA_SLAM,
                    SkillDamageType.MARTIAL);
            living.setDeltaMovement(living.getDeltaMovement().add(0.0D, 0.55D, 0.0D));
            living.hurtMarked = true;
        }
        return true;
    }

    public static boolean castLaunch(ServerPlayer owner, ShikigamiAbility ability, ShikigamiAbilitySource source)
    {
        Mob m = MahoragaSupport.first(owner);
        LivingEntity t = requireNear(owner, m, 4.0D);
        if (m == null || t == null)
        {
            return false;
        }
        TechniqueDamageSources.deal(t, m, TenShadowsShikigamiSkillRatios.MAHORAGA_LAUNCH,
                SkillDamageType.MARTIAL);
        t.setDeltaMovement(t.getDeltaMovement().add(0.0D, 1.15D, 0.0D));
        t.hurtMarked = true;
        return true;
    }

    public static boolean castThrow(ServerPlayer owner, ShikigamiAbility ability, ShikigamiAbilitySource source)
    {
        Mob m = MahoragaSupport.first(owner);
        LivingEntity t = requireNear(owner, m, 3.5D);
        if (m == null || t == null)
        {
            return false;
        }
        Vec3 dir = m.getLookAngle().normalize().scale(1.8D).add(0.0D, 0.55D, 0.0D);
        TechniqueDamageSources.deal(t, m, TenShadowsShikigamiSkillRatios.MAHORAGA_THROW,
                SkillDamageType.MARTIAL);
        t.setDeltaMovement(dir);
        t.hurtMarked = true;
        return true;
    }

    public static boolean castPunchCombo(ServerPlayer owner, ShikigamiAbility ability, ShikigamiAbilitySource source)
    {
        Mob m = MahoragaSupport.first(owner);
        LivingEntity t = requireNear(owner, m, 3.5D);
        if (m == null || t == null)
        {
            return false;
        }
        for (int i = 0; i < 3; i++)
        {
            TechniqueDamageSources.deal(t, m, TenShadowsShikigamiSkillRatios.MAHORAGA_PUNCH_HIT,
                    SkillDamageType.MARTIAL);
        }
        t.setDeltaMovement(t.getDeltaMovement().add(m.getLookAngle().scale(0.4D)));
        t.hurtMarked = true;
        return true;
    }

    public static boolean castSwordCombo(ServerPlayer owner, ShikigamiAbility ability, ShikigamiAbilitySource source)
    {
        Mob m = MahoragaSupport.first(owner);
        LivingEntity t = requireNear(owner, m, 4.5D);
        if (m == null || t == null)
        {
            return false;
        }
        ServerLevel level = owner.serverLevel();
        Vec3 look = m.getLookAngle();
        AABB box = m.getBoundingBox().expandTowards(look.scale(3.5D)).inflate(1.2D);
        boolean any = false;
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, box, e ->
                e.isAlive() && e != owner && e != m && !TenShadowsSummonLogic.isOwnedBy(e, owner.getUUID())))
        {
            MahoragaExtermination.applySwordHit(m, living, TenShadowsShikigamiSkillRatios.MAHORAGA_SWORD);
            any = true;
        }
        return any;
    }

    public static boolean castWheelSlash(ServerPlayer owner, ShikigamiAbility ability, ShikigamiAbilitySource source)
    {
        Mob m = MahoragaSupport.first(owner);
        LivingEntity t = requireNear(owner, m, 6.0D);
        if (m == null || t == null)
        {
            return false;
        }
        ServerLevel level = owner.serverLevel();
        Vec3 from = m.position().add(0.0D, 1.0D, 0.0D);
        Vec3 to = t.position().add(0.0D, t.getBbHeight() * 0.5D, 0.0D);
        Vec3 dir = to.subtract(from);
        double len = dir.length();
        if (len < 1.0E-3D)
        {
            return false;
        }
        dir = dir.normalize();
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, to.x, to.y, to.z, 6, 0.4D, 0.3D, 0.4D, 0.0D);
        AABB box = new AABB(from, to).inflate(1.0D);
        boolean any = false;
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, box, e ->
                e.isAlive() && e != owner && e != m && !TenShadowsSummonLogic.isOwnedBy(e, owner.getUUID())))
        {
            Vec3 rel = living.position().add(0.0D, living.getBbHeight() * 0.5D, 0.0D).subtract(from);
            if (rel.lengthSqr() > 49.0D)
            {
                continue;
            }
            if (rel.normalize().dot(dir) < 0.7D)
            {
                continue;
            }
            MahoragaExtermination.applySwordHit(m, living, TenShadowsShikigamiSkillRatios.MAHORAGA_WHEEL_SLASH);
            any = true;
        }
        return any;
    }

    private static LivingEntity requireNear(ServerPlayer owner, Mob mahoraga, double range)
    {
        if (mahoraga == null)
        {
            return null;
        }
        LivingEntity t = MahoragaSupport.resolveCombatTarget(owner, mahoraga);
        if (t == null || mahoraga.distanceToSqr(t) > range * range)
        {
            return null;
        }
        return t;
    }
}
