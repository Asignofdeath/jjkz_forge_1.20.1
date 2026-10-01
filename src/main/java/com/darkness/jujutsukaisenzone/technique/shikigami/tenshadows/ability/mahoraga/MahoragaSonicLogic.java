package com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability.mahoraga;

import com.darkness.jujutsukaisenzone.technique.TechniqueDamageSources;
import com.darkness.jujutsukaisenzone.technique.shikigami.ShikigamiAbility;
import com.darkness.jujutsukaisenzone.technique.skillset.SkillDamageType;
import com.darkness.jujutsukaisenzone.technique.shikigami.ShikigamiAbilitySource;
import com.darkness.jujutsukaisenzone.technique.skill.TenShadowsSummonLogic;
import com.darkness.jujutsukaisenzone.technique.skill.tenshadows.TenShadowsShikigamiSkillRatios;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 魔虚罗 · 声波攻击（BOTH，Shift+0）。
 * <p>
 * 自动释放极少触发（原著中很少用）；手动不受影响。失败不写入公用 CD。
 */
public final class MahoragaSonicLogic
{
    private static final double RANGE = 12.0D;
    /** 自动索敌时实际出手概率。 */
    private static final float AUTO_CAST_CHANCE = 0.08F;

    private MahoragaSonicLogic()
    {
    }

    public static boolean cast(ServerPlayer owner, ShikigamiAbility ability, ShikigamiAbilitySource source)
    {
        Mob mahoraga = MahoragaSupport.first(owner);
        if (mahoraga == null)
        {
            return false;
        }
        if (mahoraga instanceof com.darkness.jujutsukaisenzone.entity.MahoragaEntity entity && entity.isRideMode())
        {
            return false;
        }
        LivingEntity focus = MahoragaSupport.resolveCombatTarget(owner, mahoraga);
        if (focus == null && source == ShikigamiAbilitySource.AUTO)
        {
            return false;
        }
        if (source == ShikigamiAbilitySource.AUTO
                && mahoraga.getRandom().nextFloat() >= AUTO_CAST_CHANCE)
        {
            return false;
        }
        Vec3 origin = mahoraga.position().add(0.0D, 1.2D, 0.0D);
        Vec3 look = focus != null
                ? focus.position().add(0.0D, focus.getBbHeight() * 0.5D, 0.0D).subtract(origin).normalize()
                : mahoraga.getLookAngle();

        ServerLevel level = owner.serverLevel();
        level.playSound(null, mahoraga.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 1.4F, 1.0F);
        level.sendParticles(ParticleTypes.SONIC_BOOM, origin.x, origin.y, origin.z, 1, 0, 0, 0, 0);

        AABB box = mahoraga.getBoundingBox().inflate(RANGE);
        int hit = 0;
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, box, e ->
                e.isAlive() && e != owner && e != mahoraga
                        && !TenShadowsSummonLogic.isOwnedBy(e, owner.getUUID())))
        {
            Vec3 to = living.position().add(0.0D, living.getBbHeight() * 0.5D, 0.0D).subtract(origin);
            double dist = to.length();
            if (dist > RANGE || dist < 1.0E-3D)
            {
                continue;
            }
            double dot = to.normalize().dot(look);
            if (dot < 0.55D)
            {
                continue;
            }
            TechniqueDamageSources.deal(living, mahoraga, TenShadowsShikigamiSkillRatios.MAHORAGA_SONIC,
                    SkillDamageType.TECHNIQUE);
            living.setDeltaMovement(to.normalize().scale(1.35D).add(0.0D, 0.35D, 0.0D));
            living.hurtMarked = true;
            hit++;
        }
        if (hit <= 0 && source == ShikigamiAbilitySource.AUTO)
        {
            return false;
        }
        if (source == ShikigamiAbilitySource.PLAYER)
        {
            owner.displayClientMessage(
                    Component.literal("魔虚罗 · 声波").withStyle(ChatFormatting.DARK_AQUA),
                    true);
        }
        return true;
    }
}
