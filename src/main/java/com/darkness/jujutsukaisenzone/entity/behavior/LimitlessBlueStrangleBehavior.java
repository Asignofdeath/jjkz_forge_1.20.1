package com.darkness.jujutsukaisenzone.entity.behavior;

import com.darkness.jujutsukaisenzone.entity.SpecialEffectEntity;
import com.darkness.jujutsukaisenzone.entity.data.LimitlessBlueStrangleConfig;
import com.darkness.jujutsukaisenzone.technique.TechniqueDamageSources;
import com.darkness.jujutsukaisenzone.technique.scaling.LimitlessBlueStrangleSkillGrowth;
import com.darkness.jujutsukaisenzone.technique.skill.ShrineCutMarkLogic;
import com.darkness.jujutsukaisenzone.technique.skillset.LimitlessTechniqueSkillSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 苍之绞杀：对单体目标持续周期伤害，每次命中随机一肢叠大量切痕 → 断肢。
 * <p>
 * 特效临时复用坚守者（监守者）声波：每次命中在目标位置放
 * {@link ParticleTypes#SONIC_BOOM} 粒子 + {@code WARDEN_SONIC_BOOM} 音效。
 */
public class LimitlessBlueStrangleBehavior extends EffectBehavior
{
    public static final String ID = "limitless_blue_strangle";

    @Nullable
    private UUID ownerUuid;
    @Nullable
    private UUID targetUuid;
    private int remainingLifeTicks = -1;
    private int damageTicks;

    @Override
    public String id() { return ID; }

    public void configure(@Nullable LivingEntity owner, LivingEntity target)
    {
        this.ownerUuid = owner == null ? null : owner.getUUID();
        this.targetUuid = target.getUUID();
        this.remainingLifeTicks = LimitlessBlueStrangleConfig.DURATION_TICKS;
        this.damageTicks = 0;
    }

    @Override
    public void tick(SpecialEffectEntity entity)
    {
        if (!(entity.level() instanceof ServerLevel level)) return;

        LivingEntity target = resolveTarget(level);
        if (target == null || !target.isAlive())
        {
            entity.discard();
            return;
        }

        this.damageTicks++;
        if (this.damageTicks >= LimitlessBlueStrangleConfig.DAMAGE_INTERVAL)
        {
            this.damageTicks = 0;
            dealDamage(entity, level, target);
        }

        this.remainingLifeTicks--;
        if (this.remainingLifeTicks <= 0)
        {
            entity.discard();
        }
    }

    @Nullable
    private LivingEntity resolveTarget(ServerLevel level)
    {
        if (this.targetUuid == null) return null;
        return level.getEntity(this.targetUuid) instanceof LivingEntity living ? living : null;
    }

    private void dealDamage(SpecialEffectEntity entity, ServerLevel level, LivingEntity target)
    {
        LivingEntity owner = resolveOwner(entity, this.ownerUuid);
        // 放行暴击：DAMAGE_INTERVAL(=15) tick 一次、目标锁定单体，间隔 > 原版无敌帧(10)，
        // 每次都真实结算；掷骰频率与单发招式同一量级。
        float damage = LimitlessBlueStrangleSkillGrowth.damage(owner);
        if (TechniqueDamageSources.dealCrit(target, owner, damage, LimitlessTechniqueSkillSet.SKILL_BLUE_STRANGLE))
        {
            ShrineCutMarkLogic.applyRandomLimb(owner, target, LimitlessBlueStrangleConfig.CUT_MARKS_BASE, damage);
            playSonicBoomFx(level, target);
        }
    }

    /** 临时特效：坚守者声波（粒子 + 音效）。 */
    private void playSonicBoomFx(ServerLevel level, LivingEntity target)
    {
        Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        level.sendParticles(ParticleTypes.SONIC_BOOM, center.x, center.y, center.z, 1, 0, 0, 0, 0);
        level.playSound(null, target.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    @Override
    public void writeToNbt(CompoundTag tag)
    {
        tag.putInt("RemainingLife", this.remainingLifeTicks);
        tag.putInt("DamageTicks", this.damageTicks);
        if (this.ownerUuid != null) tag.putUUID("Owner", this.ownerUuid);
        if (this.targetUuid != null) tag.putUUID("Target", this.targetUuid);
    }

    @Override
    public void readFromNbt(CompoundTag tag)
    {
        this.remainingLifeTicks = tag.contains("RemainingLife") ? tag.getInt("RemainingLife") : -1;
        this.damageTicks = tag.contains("DamageTicks") ? tag.getInt("DamageTicks") : 0;
        this.ownerUuid = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        this.targetUuid = tag.hasUUID("Target") ? tag.getUUID("Target") : null;
    }
}
