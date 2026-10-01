package com.darkness.jujutsukaisenzone.technique.skill;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import com.darkness.jujutsukaisenzone.technique.TechniqueDamageSources;
import com.darkness.jujutsukaisenzone.technique.development.SkillUpgradeLogic;
import com.darkness.jujutsukaisenzone.technique.skillset.RatioTechniqueSkillSet;
import com.darkness.jujutsukaisenzone.technique.skillset.SkillDamageType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 冲击：一段前冲击飞并僵直；窗口内再按二段跳到目标面前（不出伤，由玩家自选后续攻击）。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RatioImpactLogic
{
    public static final float DAMAGE = 0.30F;
    public static final double RANGE = 4.5D;
    public static final double HALF_WIDTH = 1.1D;
    /** 一段命中后可接二段的时限。 */
    public static final int FOLLOWUP_WINDOW_TICKS = 24;
    /** 目标僵直时长。 */
    public static final int STUN_TICKS = 40;
    public static final double LAUNCH_HORIZONTAL = 0.55D;
    public static final double LAUNCH_VERTICAL = 0.62D;
    public static final double STAND_OFF = 1.55D;

    private static final String TAG_STUN = "jujutsukaisenzone_ratio_impact_stun";
    private static final String TAG_CASTER = "caster";
    private static final String TAG_EXPIRE = "expire";

    private static final Map<UUID, FollowUp> FOLLOW_UPS = new ConcurrentHashMap<>();

    private RatioImpactLogic()
    {
    }

    public static boolean hasPendingFollowUp(ServerPlayer player)
    {
        FollowUp followUp = FOLLOW_UPS.get(player.getUUID());
        if (followUp == null)
        {
            return false;
        }
        if (player.level().getGameTime() > followUp.expireGameTime)
        {
            FOLLOW_UPS.remove(player.getUUID());
            return false;
        }
        return true;
    }

    /**
     * 二段：贴脸位移。成功处理（含失败提示）返回 true，调用方勿再走技能经济。
     */
    public static boolean tryFollowUp(ServerPlayer player)
    {
        FollowUp followUp = FOLLOW_UPS.get(player.getUUID());
        if (followUp == null)
        {
            return false;
        }
        long now = player.level().getGameTime();
        if (now > followUp.expireGameTime)
        {
            FOLLOW_UPS.remove(player.getUUID());
            return false;
        }
        FOLLOW_UPS.remove(player.getUUID());

        if (!(player.level() instanceof ServerLevel level))
        {
            return true;
        }
        LivingEntity target = findLiving(level, followUp.targetUuid);
        if (target == null || !target.isAlive() || !isStunnedBy(player, target, now))
        {
            player.displayClientMessage(
                    Component.literal("追击目标已脱离僵直").withStyle(ChatFormatting.YELLOW),
                    true);
            return true;
        }

        snapInFrontOf(player, target);
        level.playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDER_PEARL_THROW,
                SoundSource.PLAYERS,
                0.55F,
                1.35F);
        level.sendParticles(
                ParticleTypes.CLOUD,
                player.getX(), player.getY() + 0.2D, player.getZ(),
                10,
                0.2D, 0.1D, 0.2D,
                0.02D);
        player.displayClientMessage(
                Component.literal("冲击·追击").withStyle(ChatFormatting.GOLD),
                true);
        return true;
    }

    /**
     * 一段：扇形内最近目标，体术伤害 + 击飞僵直，开启二段窗口。
     */
    public static boolean tryStageOne(ServerPlayer player)
    {
        if (!(player.level() instanceof ServerLevel level))
        {
            return false;
        }

        LivingEntity target = findFrontTarget(player);
        if (target == null)
        {
            player.displayClientMessage(
                    Component.literal("冲击未命中").withStyle(ChatFormatting.YELLOW),
                    true);
            return false;
        }

        Vec3 look = player.getLookAngle().normalize();
        Vec3 flat = new Vec3(look.x, 0.0D, look.z);
        if (flat.lengthSqr() < 1.0E-6D)
        {
            flat = new Vec3(0.0D, 0.0D, 1.0D);
        }
        else
        {
            flat = flat.normalize();
        }

        // 轻微前冲，方便手感
        Vec3 dash = flat.scale(0.85D).add(0.0D, 0.12D, 0.0D);
        player.setDeltaMovement(player.getDeltaMovement().add(dash));
        player.hurtMarked = true;

        TechniqueDamageSources.deal(target, player,
                SkillUpgradeLogic.scaleRatio(
                        player,
                        PlayerTechniqueData.TECHNIQUE_RATIO,
                        RatioTechniqueSkillSet.SKILL_IMPACT,
                        DAMAGE),
                SkillDamageType.MARTIAL,
                RatioTechniqueSkillSet.SKILL_IMPACT);

        Vec3 launch = flat.scale(LAUNCH_HORIZONTAL).add(0.0D, LAUNCH_VERTICAL, 0.0D);
        target.setDeltaMovement(target.getDeltaMovement().add(launch));
        target.hurtMarked = true;
        applyStun(player, target);

        long now = level.getGameTime();
        FOLLOW_UPS.put(player.getUUID(), new FollowUp(target.getUUID(), now + FOLLOWUP_WINDOW_TICKS));

        level.playSound(
                null,
                target.getX(), target.getY(), target.getZ(),
                SoundEvents.PLAYER_ATTACK_KNOCKBACK,
                SoundSource.PLAYERS,
                0.9F,
                0.9F);
        level.sendParticles(
                ParticleTypes.CRIT,
                target.getX(), target.getY() + target.getBbHeight() * 0.6D, target.getZ(),
                14,
                0.3D, 0.35D, 0.3D,
                0.08D);
        player.displayClientMessage(
                Component.literal("冲击！再按可追到面前")
                        .withStyle(ChatFormatting.GOLD),
                true);
        return true;
    }

    public static boolean isStunnedBy(ServerPlayer caster, LivingEntity target, long now)
    {
        CompoundTag stun = target.getPersistentData().getCompound(TAG_STUN);
        if (stun.isEmpty() || !stun.hasUUID(TAG_CASTER))
        {
            return false;
        }
        if (!stun.getUUID(TAG_CASTER).equals(caster.getUUID()))
        {
            return false;
        }
        return stun.getLong(TAG_EXPIRE) >= now;
    }

    private static void applyStun(ServerPlayer caster, LivingEntity target)
    {
        long expire = caster.level().getGameTime() + STUN_TICKS;
        CompoundTag stun = new CompoundTag();
        stun.putUUID(TAG_CASTER, caster.getUUID());
        stun.putLong(TAG_EXPIRE, expire);
        target.getPersistentData().put(TAG_STUN, stun);

        target.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN, STUN_TICKS, 9, false, false, true));
        target.addEffect(new MobEffectInstance(
                MobEffects.WEAKNESS, STUN_TICKS, 0, false, false, true));
        // 极短浮空，配合击飞
        target.addEffect(new MobEffectInstance(
                MobEffects.LEVITATION, 8, 0, false, false, true));
    }

    private static void snapInFrontOf(ServerPlayer player, LivingEntity target)
    {
        Vec3 away = player.position().subtract(target.position());
        Vec3 flat = new Vec3(away.x, 0.0D, away.z);
        if (flat.lengthSqr() < 1.0E-4D)
        {
            Vec3 look = target.getLookAngle();
            flat = new Vec3(-look.x, 0.0D, -look.z);
            if (flat.lengthSqr() < 1.0E-4D)
            {
                flat = new Vec3(0.0D, 0.0D, 1.0D);
            }
        }
        flat = flat.normalize();
        Vec3 dest = target.position().add(flat.scale(STAND_OFF));
        double y = target.getY();
        player.teleportTo(dest.x, y, dest.z);
        player.setDeltaMovement(Vec3.ZERO);
        player.hurtMarked = true;

        double dx = target.getX() - player.getX();
        double dz = target.getZ() - player.getZ();
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0D);
        player.setYRot(yaw);
        player.setYHeadRot(yaw);
        player.setYBodyRot(yaw);
    }

    private static LivingEntity findFrontTarget(ServerPlayer player)
    {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        AABB box = player.getBoundingBox()
                .expandTowards(look.scale(RANGE))
                .inflate(HALF_WIDTH, 0.75D, HALF_WIDTH);

        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity living : player.level().getEntitiesOfClass(
                LivingEntity.class,
                box,
                entity -> entity.isAlive()
                        && !entity.isRemoved()
                        && entity.isAttackable()
                        && !entity.getUUID().equals(player.getUUID())
                        && !entity.isSpectator()
                        && entity.isPickable()))
        {
            Vec3 to = living.getEyePosition().subtract(eye);
            double dist = to.length();
            if (dist < 0.01D || dist > RANGE + living.getBbWidth())
            {
                continue;
            }
            if (to.normalize().dot(look) < 0.4D)
            {
                continue;
            }
            if (dist < bestDist)
            {
                bestDist = dist;
                best = living;
            }
        }
        return best;
    }

    private static LivingEntity findLiving(ServerLevel level, UUID uuid)
    {
        if (level.getEntity(uuid) instanceof LivingEntity living)
        {
            return living;
        }
        return null;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide)
        {
            return;
        }
        if (!(event.player instanceof ServerPlayer player))
        {
            return;
        }
        FollowUp followUp = FOLLOW_UPS.get(player.getUUID());
        if (followUp == null)
        {
            return;
        }
        if (!player.isAlive() || !PlayerTechniqueData.isRatio(player)
                || player.level().getGameTime() > followUp.expireGameTime)
        {
            FOLLOW_UPS.remove(player.getUUID());
        }
    }

    /** 僵直期间压住水平速度，避免被击飞后立刻跑开。 */
    @SubscribeEvent
    public static void onEntityTick(net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent event)
    {
        LivingEntity living = event.getEntity();
        if (living.level().isClientSide)
        {
            return;
        }
        CompoundTag stun = living.getPersistentData().getCompound(TAG_STUN);
        if (stun.isEmpty())
        {
            return;
        }
        long now = living.level().getGameTime();
        if (stun.getLong(TAG_EXPIRE) < now)
        {
            living.getPersistentData().remove(TAG_STUN);
            return;
        }
        Vec3 motion = living.getDeltaMovement();
        // 保留一点上升/下落，锁死水平跑动
        living.setDeltaMovement(motion.x * 0.15D, motion.y, motion.z * 0.15D);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event)
    {
        Player player = event.getEntity();
        FOLLOW_UPS.remove(player.getUUID());
    }

    public static void clear(ServerPlayer player)
    {
        FOLLOW_UPS.remove(player.getUUID());
    }

    private static final class FollowUp
    {
        private final UUID targetUuid;
        private final long expireGameTime;

        private FollowUp(UUID targetUuid, long expireGameTime)
        {
            this.targetUuid = targetUuid;
            this.expireGameTime = expireGameTime;
        }
    }
}
