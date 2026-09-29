package com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability;

import com.darkness.jujutsukaisenzone.technique.shikigami.ShikigamiAbility;
import com.darkness.jujutsukaisenzone.technique.shikigami.ShikigamiAbilitySource;
import com.darkness.jujutsukaisenzone.technique.skill.TenShadowsRitualLogic;
import com.darkness.jujutsukaisenzone.technique.skill.TenShadowsSummonLogic;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 贯牛 · 蓄力冲锋（BOTH，Shift+8）：高速冲向目标，路径敌人击飞。
 */
public final class PiercingOxChargeLogic
{
    public static final String TAG_CHARGE_TICKS = "jujutsukaisenzone_ox_charge_ticks";
    public static final String TAG_CHARGE_TARGET = "jujutsukaisenzone_ox_charge_target";
    public static final String TAG_CHARGE_HIT = "jujutsukaisenzone_ox_charge_hit";

    private static final double MIN_RANGE = 4.0D;
    private static final double MAX_RANGE = 22.0D;
    private static final int CHARGE_TICKS = 18;
    private static final double SPEED = 1.55D;
    private static final float DAMAGE = 12.0F;
    private static final double HIT_INFLATE = 1.15D;

    private PiercingOxChargeLogic()
    {
    }

    public static boolean cast(ServerPlayer owner, ShikigamiAbility ability, ShikigamiAbilitySource source)
    {
        Mob ox = PiercingOxSupport.first(owner);
        if (ox == null)
        {
            return false;
        }
        if (isCharging(ox))
        {
            return false;
        }

        LivingEntity focus = resolveFocus(owner, ox, source);
        if (focus == null)
        {
            if (source == ShikigamiAbilitySource.PLAYER)
            {
                owner.displayClientMessage(
                        Component.literal("贯牛 · 冲锋：没有目标").withStyle(ChatFormatting.GRAY),
                        true);
            }
            return false;
        }
        double distSq = ox.distanceToSqr(focus);
        if (distSq < MIN_RANGE * MIN_RANGE || distSq > MAX_RANGE * MAX_RANGE)
        {
            if (source == ShikigamiAbilitySource.PLAYER)
            {
                owner.displayClientMessage(
                        Component.literal("贯牛 · 冲锋：距离不合适").withStyle(ChatFormatting.GRAY),
                        true);
            }
            return false;
        }

        ox.getNavigation().stop();
        ox.setTarget(focus);
        ox.getPersistentData().putInt(TAG_CHARGE_TICKS, CHARGE_TICKS);
        ox.getPersistentData().putUUID(TAG_CHARGE_TARGET, focus.getUUID());
        ox.getPersistentData().put(TAG_CHARGE_HIT, new ListTag());

        if (source == ShikigamiAbilitySource.PLAYER)
        {
            owner.displayClientMessage(
                    Component.literal("贯牛 · 蓄力冲锋 → " + focus.getName().getString())
                            .withStyle(ChatFormatting.GOLD),
                    true);
        }
        return true;
    }

    public static boolean isCharging(Mob ox)
    {
        return ox != null && ox.getPersistentData().getInt(TAG_CHARGE_TICKS) > 0;
    }

    public static void tickOx(Mob ox)
    {
        if (ox.level().isClientSide || !(ox.level() instanceof ServerLevel level))
        {
            return;
        }
        if (!PiercingOxSupport.isPiercingOx(ox))
        {
            return;
        }
        int remain = ox.getPersistentData().getInt(TAG_CHARGE_TICKS);
        if (remain <= 0)
        {
            return;
        }
        remain--;
        ox.getPersistentData().putInt(TAG_CHARGE_TICKS, remain);

        LivingEntity focus = null;
        if (ox.getPersistentData().hasUUID(TAG_CHARGE_TARGET))
        {
            var entity = level.getEntity(ox.getPersistentData().getUUID(TAG_CHARGE_TARGET));
            if (entity instanceof LivingEntity living && living.isAlive())
            {
                focus = living;
            }
        }

        Vec3 dir;
        if (focus != null)
        {
            dir = focus.position().add(0.0D, 0.2D, 0.0D).subtract(ox.position());
        }
        else
        {
            dir = ox.getLookAngle();
        }
        if (dir.lengthSqr() < 1.0E-4D)
        {
            clear(ox);
            return;
        }
        dir = dir.normalize();
        Vec3 motion = dir.scale(SPEED);
        ox.setDeltaMovement(motion.x, Math.max(motion.y, -0.1D), motion.z);
        ox.hurtMarked = true;
        ox.setYRot((float) (Math.atan2(-dir.x, dir.z) * (180.0D / Math.PI)));

        level.sendParticles(
                ParticleTypes.CLOUD,
                ox.getX(), ox.getY() + 0.3D, ox.getZ(),
                4, 0.25D, 0.1D, 0.25D, 0.02D);

        sweepHits(ox, level, ownerOf(ox));

        if (remain <= 0 || (focus != null && ox.distanceToSqr(focus) < 2.25D))
        {
            clear(ox);
        }
    }

    private static void sweepHits(Mob ox, ServerLevel level, UUID ownerId)
    {
        Set<UUID> already = readHitSet(ox);
        AABB box = ox.getBoundingBox().inflate(HIT_INFLATE, 0.6D, HIT_INFLATE);
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, box, e ->
                e.isAlive()
                        && e != ox
                        && (ownerId == null || !e.getUUID().equals(ownerId))
                        && !TenShadowsSummonLogic.isOwnedBy(e, ownerId)
                        && !already.contains(e.getUUID())))
        {
            living.hurt(ox.damageSources().mobAttack(ox), DAMAGE);
            Vec3 knock = living.position().subtract(ox.position());
            if (knock.lengthSqr() < 1.0E-4D)
            {
                knock = ox.getLookAngle();
            }
            living.setDeltaMovement(knock.normalize().scale(0.7D).add(0.0D, 0.85D, 0.0D));
            living.hurtMarked = true;
            already.add(living.getUUID());
            level.sendParticles(
                    ParticleTypes.CRIT,
                    living.getX(), living.getY() + 1.0D, living.getZ(),
                    6, 0.2D, 0.3D, 0.2D, 0.1D);
        }
        writeHitSet(ox, already);
    }

    private static LivingEntity resolveFocus(ServerPlayer owner, Mob ox, ShikigamiAbilitySource source)
    {
        LivingEntity target = ox.getTarget();
        if (target != null && target.isAlive() && !TenShadowsSummonLogic.isOwnedBy(target, owner.getUUID()))
        {
            return target;
        }
        if (source == ShikigamiAbilitySource.AUTO)
        {
            return null;
        }
        LivingEntity cross = TenShadowsRitualLogic.resolveCrosshairLiving(owner, 32.0D);
        if (cross != null && !TenShadowsSummonLogic.isOwnedBy(cross, owner.getUUID()))
        {
            return cross;
        }
        return null;
    }

    private static UUID ownerOf(Mob ox)
    {
        return TenShadowsSummonLogic.getOwnerId(ox);
    }

    private static Set<UUID> readHitSet(Mob ox)
    {
        Set<UUID> set = new HashSet<>();
        ListTag list = ox.getPersistentData().getList(TAG_CHARGE_HIT, Tag.TAG_INT_ARRAY);
        for (int i = 0; i < list.size(); i++)
        {
            set.add(NbtUtils.loadUUID(list.get(i)));
        }
        return set;
    }

    private static void writeHitSet(Mob ox, Set<UUID> set)
    {
        ListTag list = new ListTag();
        for (UUID id : set)
        {
            list.add(NbtUtils.createUUID(id));
        }
        ox.getPersistentData().put(TAG_CHARGE_HIT, list);
    }

    private static void clear(Mob ox)
    {
        ox.getPersistentData().remove(TAG_CHARGE_TICKS);
        ox.getPersistentData().remove(TAG_CHARGE_TARGET);
        ox.getPersistentData().remove(TAG_CHARGE_HIT);
    }
}
