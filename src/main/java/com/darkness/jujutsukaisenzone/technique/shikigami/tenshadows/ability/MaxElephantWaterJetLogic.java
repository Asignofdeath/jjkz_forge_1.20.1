package com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability;

import com.darkness.jujutsukaisenzone.technique.TechniqueDamageSources;
import com.darkness.jujutsukaisenzone.technique.shikigami.ShikigamiAbility;
import com.darkness.jujutsukaisenzone.technique.shikigami.ShikigamiAbilitySource;
import com.darkness.jujutsukaisenzone.technique.skill.TenShadowsSummonLogic;
import com.darkness.jujutsukaisenzone.technique.skill.tenshadows.TenShadowsShikigamiSkillRatios;
import com.darkness.jujutsukaisenzone.technique.skillset.SkillDamageType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * 满象 · 水柱（BOTH，Shift+5）：象鼻喷水，术式伤害。
 */
public final class MaxElephantWaterJetLogic
{
    private static final double AUTO_RANGE = 14.0D;

    private MaxElephantWaterJetLogic()
    {
    }

    public static boolean cast(ServerPlayer owner, ShikigamiAbility ability, ShikigamiAbilitySource source)
    {
        Mob elephant = MaxElephantSupport.first(owner);
        if (elephant == null)
        {
            return false;
        }
        LivingEntity focus = resolveFocus(owner, elephant, source);
        if (focus == null)
        {
            if (source == ShikigamiAbilitySource.PLAYER)
            {
                owner.displayClientMessage(
                        Component.literal("满象 · 水柱：没有目标").withStyle(ChatFormatting.GRAY),
                        true);
            }
            return false;
        }

        ServerLevel level = owner.serverLevel();
        sprayParticles(level, elephant.position().add(0.0D, 1.6D, 0.0D), focus.position().add(0.0D, focus.getBbHeight() * 0.5D, 0.0D));
        TechniqueDamageSources.deal(focus, elephant, TenShadowsShikigamiSkillRatios.MAX_ELEPHANT_WATER_JET,
                SkillDamageType.TECHNIQUE);
        if (source == ShikigamiAbilitySource.PLAYER)
        {
            owner.displayClientMessage(
                    Component.literal("满象 · 水柱").withStyle(ChatFormatting.AQUA),
                    true);
        }
        return true;
    }

    private static LivingEntity resolveFocus(ServerPlayer owner, Mob elephant, ShikigamiAbilitySource source)
    {
        LivingEntity target = elephant.getTarget();
        if (target != null && target.isAlive() && !TenShadowsSummonLogic.isOwnedBy(target, owner.getUUID()))
        {
            if (source == ShikigamiAbilitySource.AUTO
                    && elephant.distanceToSqr(target) > AUTO_RANGE * AUTO_RANGE)
            {
                return null;
            }
            return target;
        }
        if (source == ShikigamiAbilitySource.AUTO)
        {
            return null;
        }
        return TenShadowsSummonLogic.isOwnedBy(elephant, owner.getUUID())
                ? com.darkness.jujutsukaisenzone.technique.skill.TenShadowsRitualLogic.resolveCrosshairLiving(owner, 28.0D)
                : null;
    }

    private static void sprayParticles(ServerLevel level, Vec3 from, Vec3 to)
    {
        Vec3 delta = to.subtract(from);
        double len = delta.length();
        if (len < 1.0E-3D)
        {
            return;
        }
        int count = Math.max(12, (int) (len * 5.0D));
        Vec3 step = delta.scale(1.0D / count);
        Vec3 pos = from;
        for (int i = 0; i < count; i++)
        {
            level.sendParticles(ParticleTypes.SPLASH, pos.x, pos.y, pos.z, 2, 0.05D, 0.05D, 0.05D, 0.0D);
            level.sendParticles(ParticleTypes.FALLING_WATER, pos.x, pos.y, pos.z, 1, 0.02D, 0.02D, 0.02D, 0.0D);
            pos = pos.add(step);
        }
    }
}
