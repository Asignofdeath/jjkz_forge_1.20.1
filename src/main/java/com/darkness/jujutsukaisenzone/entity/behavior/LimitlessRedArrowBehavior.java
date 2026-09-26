package com.darkness.jujutsukaisenzone.entity.behavior;

import com.darkness.jujutsukaisenzone.entity.SpecialEffectEntity;
import com.darkness.jujutsukaisenzone.entity.data.LimitlessRedConfig;
import com.darkness.jujutsukaisenzone.technique.config.SkillCombatConfigRegistry;
import com.darkness.jujutsukaisenzone.technique.TechniqueDamageSources;
import com.darkness.jujutsukaisenzone.technique.scaling.LimitlessRedSkillGrowth;
import com.darkness.jujutsukaisenzone.technique.skillset.SkillDamageType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 赫普通释放箭：匀速飞行，命中后爆炸 + 击飞 + Photon。
 */
public class LimitlessRedArrowBehavior extends EffectBehavior
{
    public static final String ID = "limitless_red_arrow";
    private static final String TAG_TEMP_RED_EXPLOSION_IMMUNE = "jujutsukaisenzone_red_explosion_immune";

    @Nullable
    private UUID ownerUuid;
    private boolean allowOwnerKnockback;
    private boolean exploded;
    private int postExplosionLifeTicks = -1;
    private float explosionPower = LimitlessRedConfig.ARROW_EXPLOSION_POWER;
    private Vec3 velocity = Vec3.ZERO;

    @Override
    public String id() { return ID; }

    public void configure(@Nullable LivingEntity owner, boolean allowOwnerKnockback, Vec3 velocity)
    {
        this.ownerUuid = owner == null ? null : owner.getUUID();
        this.allowOwnerKnockback = allowOwnerKnockback;
        this.exploded = false;
        this.explosionPower = LimitlessRedSkillGrowth.explosionPower(owner);
        this.velocity = velocity;
    }

    public float explosionPower()
    {
        return this.explosionPower;
    }

    @Override
    public void tick(SpecialEffectEntity entity)
    {
        if (this.exploded)
        {
            entity.setDeltaMovement(Vec3.ZERO);
            if (this.postExplosionLifeTicks-- <= 0)
            {
                entity.discard();
            }
            return;
        }

        entity.setNoGravity(true);
        entity.setDeltaMovement(this.velocity);

        // Check block collision
        Vec3 pos = entity.position();
        Vec3 nextPos = pos.add(this.velocity);
        if (entity.level().getBlockState(entity.blockPosition().offset(
                (int) Math.signum(this.velocity.x),
                (int) Math.signum(this.velocity.y),
                (int) Math.signum(this.velocity.z))).blocksMotion())
        {
            onHit(entity);
            return;
        }
        entity.setPos(nextPos);

        // Check entity collision
        AABB box = entity.getBoundingBox();
        for (LivingEntity target : entity.level().getEntitiesOfClass(LivingEntity.class, box))
        {
            if (target == entity) continue;
            if (this.ownerUuid != null && this.ownerUuid.equals(target.getUUID())) continue;
            if (!target.isAlive()) continue;
            onHit(entity);
            return;
        }
    }

    private void onHit(SpecialEffectEntity entity)
    {
        LivingEntity ownerLiving = resolveOwner(entity, this.ownerUuid);
        Vec3 ownerVelocityBeforeExplosion = ownerLiving == null ? null : ownerLiving.getDeltaMovement();
        this.exploded = true;
        this.postExplosionLifeTicks = LimitlessRedConfig.ARROW_POST_EXPLOSION_LIFE_TICKS;
        entity.setDeltaMovement(Vec3.ZERO);

        if (ownerLiving != null)
        {
            ownerLiving.getPersistentData().putBoolean(TAG_TEMP_RED_EXPLOSION_IMMUNE, true);
        }

        // 单次爆炸 ⇒ 显式放行暴击（术式默认不放行，见 TechniqueDamageSources#critAllowed）
        TechniqueDamageSources.runWithTypeCrit(SkillDamageType.TECHNIQUE, () ->
                entity.level().explode(entity, entity.getX(), entity.getY(), entity.getZ(),
                        this.explosionPower, false, ExplosionInteraction.TNT));

        if (ownerLiving != null)
        {
            ownerLiving.getPersistentData().remove(TAG_TEMP_RED_EXPLOSION_IMMUNE);
            if (ownerVelocityBeforeExplosion != null)
            {
                ownerLiving.setDeltaMovement(ownerVelocityBeforeExplosion);
                ownerLiving.hurtMarked = true;
            }
        }

        applyKnockback(entity);
        spawnExtraTntExplosionParticles(entity);

        CommandSourceStack source = entity.createCommandSourceStack().withPermission(2).withSuppressedOutput();
        if (entity.getServer() != null)
        {
            entity.getServer().getCommands().performPrefixedCommand(source, LimitlessRedConfig.PHOTON_EXPLOSION_COMMAND);
        }
    }

    /** 在原版爆炸粒子之外，再叠一层 TNT 同款 EXPLOSION / EXPLOSION_EMITTER。 */
    private static void spawnExtraTntExplosionParticles(SpecialEffectEntity entity)
    {
        if (!(entity.level() instanceof ServerLevel level))
        {
            return;
        }
        double x = entity.getX();
        double y = entity.getY();
        double z = entity.getZ();
        double spread = LimitlessRedConfig.EXTRA_TNT_EXPLOSION_BURST_SPREAD;

        for (int i = 0; i < LimitlessRedConfig.EXTRA_TNT_EXPLOSION_EMITTER_COUNT; i++)
        {
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        level.sendParticles(
                ParticleTypes.EXPLOSION,
                x,
                y,
                z,
                LimitlessRedConfig.EXTRA_TNT_EXPLOSION_BURST_COUNT,
                spread,
                spread,
                spread,
                0.0D);
    }

    private void applyKnockback(SpecialEffectEntity entity)
    {
        LivingEntity owner = resolveOwner(entity, this.ownerUuid);
        double knockbackRadius = LimitlessRedSkillGrowth.knockbackRadius(owner);
        double cx = entity.getX();
        double cy = entity.getY();
        double cz = entity.getZ();
        AABB area = new AABB(cx, cy, cz, cx, cy, cz).inflate(knockbackRadius);
        for (LivingEntity target : entity.level().getEntitiesOfClass(LivingEntity.class, area))
        {
            if (!target.isAlive() || (!this.allowOwnerKnockback && owner != null && target.getUUID().equals(owner.getUUID())))
                continue;
            Vec3 targetCenter = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
            Vec3 offset = targetCenter.subtract(cx, cy, cz);
            double dist = offset.length();
            if (dist < 1.0E-3D || dist > knockbackRadius) continue;
            Vec3 dir = offset.scale(1.0D / dist);
            double falloff = 1.0D - dist / knockbackRadius;
            double strength = LimitlessRedConfig.ARROW_KNOCKBACK_BASE * (falloff * falloff + 0.35D);
            Vec3 impulse = new Vec3(dir.x * strength, LimitlessRedConfig.ARROW_KNOCKBACK_UP * (falloff * falloff + 0.4D), dir.z * strength);
            target.setDeltaMovement(target.getDeltaMovement().add(impulse));
            target.hurtMarked = true;
        }
    }

    @Override
    public void writeToNbt(CompoundTag tag)
    {
        tag.putBoolean("Exploded", this.exploded);
        tag.putInt("PostExplosionLife", this.postExplosionLifeTicks);
        tag.putDouble("VelX", this.velocity.x);
        tag.putDouble("VelY", this.velocity.y);
        tag.putDouble("VelZ", this.velocity.z);
        tag.putBoolean("AllowOwnerKnockback", this.allowOwnerKnockback);
        tag.putFloat("ExplosionPower", this.explosionPower);
        if (this.ownerUuid != null) tag.putUUID("Owner", this.ownerUuid);
    }

    @Override
    public void readFromNbt(CompoundTag tag)
    {
        this.exploded = tag.getBoolean("Exploded");
        this.postExplosionLifeTicks = tag.contains("PostExplosionLife") ? tag.getInt("PostExplosionLife") : -1;
        this.velocity = new Vec3(tag.getDouble("VelX"), tag.getDouble("VelY"), tag.getDouble("VelZ"));
        this.allowOwnerKnockback = tag.getBoolean("AllowOwnerKnockback");
        this.explosionPower = tag.contains("ExplosionPower")
                ? tag.getFloat("ExplosionPower")
                : LimitlessRedConfig.ARROW_EXPLOSION_POWER;
        this.ownerUuid = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
    }
}
