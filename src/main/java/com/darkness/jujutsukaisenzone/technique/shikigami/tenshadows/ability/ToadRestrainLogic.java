package com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability;

import com.darkness.jujutsukaisenzone.technique.TechniqueDamageSources;
import com.darkness.jujutsukaisenzone.technique.shikigami.ShikigamiAbility;
import com.darkness.jujutsukaisenzone.technique.shikigami.ShikigamiAbilitySource;
import com.darkness.jujutsukaisenzone.technique.skill.TenShadowsSummonLogic;
import com.darkness.jujutsukaisenzone.technique.skill.tenshadows.TenShadowsShikigamiSkillRatios;
import com.darkness.jujutsukaisenzone.technique.skillset.SkillDamageType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * 蝦蟇 · 牵制（自动）：吐舌控制敌人（粒子舌线 + 强减速 + 轻拉）。
 */
public final class ToadRestrainLogic
{
    private static final double MIN_RANGE = 2.5D;
    private static final double MAX_RANGE = 12.0D;
    private static final int CONTROL_TICKS = 20 * 3;

    private ToadRestrainLogic()
    {
    }

    public static boolean cast(ServerPlayer owner, ShikigamiAbility ability, ShikigamiAbilitySource source)
    {
        Mob toad = ToadSupport.firstToad(owner);
        if (toad == null)
        {
            return false;
        }
        LivingEntity target = toad.getTarget();
        if (target == null || !target.isAlive() || TenShadowsSummonLogic.isOwnedBy(target, owner.getUUID()))
        {
            return false;
        }
        double distSq = toad.distanceToSqr(target);
        if (distSq < MIN_RANGE * MIN_RANGE || distSq > MAX_RANGE * MAX_RANGE)
        {
            return false;
        }

        ServerLevel level = owner.serverLevel();
        tongueParticles(level, toad.position().add(0.0D, 0.4D, 0.0D), target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D));

        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, CONTROL_TICKS, 4, false, true, true));
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, CONTROL_TICKS, 0, false, true, true));
        TechniqueDamageSources.deal(target, toad, TenShadowsShikigamiSkillRatios.TOAD_RESTRAIN,
                SkillDamageType.TECHNIQUE);

        Vec3 pull = toad.position().subtract(target.position());
        if (pull.lengthSqr() > 1.0E-4D)
        {
            target.setDeltaMovement(target.getDeltaMovement().add(pull.normalize().scale(0.55D)));
            target.hurtMarked = true;
        }
        return true;
    }

    private static void tongueParticles(ServerLevel level, Vec3 from, Vec3 to)
    {
        Vec3 delta = to.subtract(from);
        double len = delta.length();
        if (len < 1.0E-3D)
        {
            return;
        }
        Vec3 step = delta.scale(1.0D / Math.max(8, (int) (len * 4.0D)));
        Vec3 pos = from;
        int count = Math.max(8, (int) (len * 4.0D));
        for (int i = 0; i < count; i++)
        {
            level.sendParticles(ParticleTypes.ITEM_SLIME, pos.x, pos.y, pos.z, 1, 0.02D, 0.02D, 0.02D, 0.0D);
            pos = pos.add(step);
        }
    }
}
