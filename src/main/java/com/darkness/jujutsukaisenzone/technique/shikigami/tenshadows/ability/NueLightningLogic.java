package com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability;

import com.darkness.jujutsukaisenzone.registry.ModEffects;
import com.darkness.jujutsukaisenzone.technique.shikigami.ShikigamiAbility;
import com.darkness.jujutsukaisenzone.technique.shikigami.ShikigamiAbilitySource;
import com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.TenShadowsFieldQuery;
import com.darkness.jujutsukaisenzone.technique.skill.TenShadowsShikigami;
import com.darkness.jujutsukaisenzone.technique.skill.TenShadowsSummonLogic;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/**
 * 鵺 · 雷击（BOTH）：范围 AOE，附加感电与易伤。占位——视觉闪电 + 范围伤。
 */
public final class NueLightningLogic
{
    private static final double AUTO_RANGE = 12.0D;
    private static final double AOE_RADIUS = 4.5D;
    private static final float DAMAGE = 8.0F;
    private static final int DEBUFF_TICKS = 20 * 5;

    private NueLightningLogic()
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
        if (isOwnerRiding(owner, nue))
        {
            /* 骑乘中不放雷击，避免误伤与抢 CD */
            return false;
        }

        LivingEntity focus = resolveFocus(owner, nue, source);
        if (focus == null)
        {
            if (source == ShikigamiAbilitySource.PLAYER)
            {
                owner.displayClientMessage(
                        Component.literal("鵺 · 雷击：没有目标").withStyle(ChatFormatting.GRAY),
                        true);
            }
            return false;
        }

        Vec3 center = focus.position();
        ServerLevel level = owner.serverLevel();
        strikeVisual(level, center);
        level.sendParticles(
                ParticleTypes.ELECTRIC_SPARK,
                center.x, center.y + 0.5D, center.z,
                24, 1.2D, 0.6D, 1.2D, 0.05D);

        AABB box = new AABB(center, center).inflate(AOE_RADIUS);
        int hit = 0;
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, box, e ->
                e.isAlive() && e != owner && !TenShadowsSummonLogic.isOwnedBy(e, owner.getUUID())))
        {
            living.hurt(owner.damageSources().magic(), DAMAGE);
            living.addEffect(new MobEffectInstance(ModEffects.ELECTRIFIED.get(), DEBUFF_TICKS, 0, false, true, true));
            living.addEffect(new MobEffectInstance(ModEffects.VULNERABILITY.get(), DEBUFF_TICKS, 0, false, true, true));
            hit++;
        }
        if (hit <= 0)
        {
            return false;
        }
        if (source == ShikigamiAbilitySource.PLAYER)
        {
            owner.displayClientMessage(
                    Component.literal("鵺 · 雷击 ×" + hit).withStyle(ChatFormatting.AQUA),
                    true);
        }
        return true;
    }

    private static LivingEntity resolveFocus(ServerPlayer owner, Mob nue, ShikigamiAbilitySource source)
    {
        if (source == ShikigamiAbilitySource.AUTO)
        {
            LivingEntity target = nue.getTarget();
            if (target != null && target.isAlive() && nue.distanceToSqr(target) <= AUTO_RANGE * AUTO_RANGE)
            {
                return target;
            }
            return null;
        }
        LivingEntity target = nue.getTarget();
        if (target != null && target.isAlive())
        {
            return target;
        }
        return findNearestHostile(owner, nue.position(), 28.0D);
    }

    private static LivingEntity findNearestHostile(ServerPlayer owner, Vec3 origin, double range)
    {
        AABB box = new AABB(origin, origin).inflate(range);
        return owner.serverLevel().getEntitiesOfClass(
                        LivingEntity.class,
                        box,
                        living -> living.isAlive()
                                && living != owner
                                && (living instanceof Monster || living instanceof ServerPlayer)
                                && owner.canAttack(living)
                                && !TenShadowsSummonLogic.isOwnedBy(living, owner.getUUID()))
                .stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(origin)))
                .orElse(null);
    }

    private static void strikeVisual(ServerLevel level, Vec3 pos)
    {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt == null)
        {
            return;
        }
        bolt.moveTo(pos.x, pos.y, pos.z);
        bolt.setVisualOnly(true);
        level.addFreshEntity(bolt);
    }

    private static boolean isOwnerRiding(ServerPlayer owner, Mob nue)
    {
        return owner.getVehicle() == nue;
    }
}
