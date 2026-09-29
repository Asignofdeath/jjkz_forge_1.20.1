package com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability;

import com.darkness.jujutsukaisenzone.technique.shikigami.ShikigamiAbility;
import com.darkness.jujutsukaisenzone.technique.shikigami.ShikigamiAbilitySource;
import com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.TenShadowsFieldQuery;
import com.darkness.jujutsukaisenzone.technique.skill.TenShadowsShikigami;
import com.darkness.jujutsukaisenzone.technique.skill.TenShadowsSummonLogic;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/**
 * 大蛇 · 突袭（BOTH，Shift+4）：钻地后从目标脚下破土冲出并控制。
 */
public final class GreatSerpentAmbushLogic
{
    public static final String TAG_AMBUSH_TICKS = "jujutsukaisenzone_serpent_ambush_ticks";
    public static final String TAG_AMBUSH_TARGET = "jujutsukaisenzone_serpent_ambush_target";
    /** 突袭结算瞬间允许造成伤害（平时封锁普攻）。 */
    public static final String TAG_ALLOW_STRIKE = "jujutsukaisenzone_serpent_allow_strike";

    private static final double AUTO_RANGE = 14.0D;
    private static final int BURROW_TICKS = 14;

    private GreatSerpentAmbushLogic()
    {
    }

    public static boolean cast(ServerPlayer owner, ShikigamiAbility ability, ShikigamiAbilitySource source)
    {
        List<Mob> serpents = TenShadowsFieldQuery.aliveMobs(owner, TenShadowsShikigami.GREAT_SERPENT);
        if (serpents.isEmpty())
        {
            return false;
        }
        Mob serpent = serpents.get(0);
        if (isAmbushing(serpent))
        {
            return false;
        }

        LivingEntity focus = resolveFocus(owner, serpent, source);
        if (focus == null)
        {
            if (source == ShikigamiAbilitySource.PLAYER)
            {
                owner.displayClientMessage(
                        Component.literal("大蛇 · 突袭：没有目标").withStyle(ChatFormatting.GRAY),
                        true);
            }
            return false;
        }

        ServerLevel level = owner.serverLevel();
        burrowStartFx(level, serpent.position());
        serpent.setInvisible(true);
        serpent.setNoGravity(true);
        serpent.noPhysics = true;
        serpent.getNavigation().stop();
        serpent.setDeltaMovement(Vec3.ZERO);
        serpent.setPos(serpent.getX(), serpent.getY() - 1.2D, serpent.getZ());
        serpent.getPersistentData().putUUID(TAG_AMBUSH_TARGET, focus.getUUID());
        serpent.getPersistentData().putInt(TAG_AMBUSH_TICKS, BURROW_TICKS);

        if (source == ShikigamiAbilitySource.PLAYER)
        {
            owner.displayClientMessage(
                    Component.literal("大蛇 · 突袭 → " + focus.getName().getString())
                            .withStyle(ChatFormatting.DARK_GREEN),
                    true);
        }
        return true;
    }

    public static boolean isGreatSerpent(Mob mob)
    {
        return mob != null
                && TenShadowsSummonLogic.isFriendlyOwned(mob)
                && mob.getPersistentData().getInt(TenShadowsSummonLogic.TAG_INDEX)
                == TenShadowsShikigami.GREAT_SERPENT.index();
    }

    public static boolean isAmbushing(Mob mob)
    {
        return mob != null && mob.getPersistentData().getInt(TAG_AMBUSH_TICKS) > 0;
    }

    public static boolean isStrikeAllowed(Mob mob)
    {
        return mob != null && mob.getPersistentData().getBoolean(TAG_ALLOW_STRIKE);
    }

    private static LivingEntity resolveFocus(ServerPlayer owner, Mob serpent, ShikigamiAbilitySource source)
    {
        LivingEntity target = serpent.getTarget();
        if (target != null && target.isAlive() && !TenShadowsSummonLogic.isOwnedBy(target, owner.getUUID()))
        {
            if (source == ShikigamiAbilitySource.AUTO
                    && serpent.distanceToSqr(target) > AUTO_RANGE * AUTO_RANGE)
            {
                return null;
            }
            return target;
        }
        if (source == ShikigamiAbilitySource.AUTO)
        {
            return null;
        }
        return findNearestHostile(owner, serpent.position(), 28.0D);
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

    private static void burrowStartFx(ServerLevel level, Vec3 pos)
    {
        level.sendParticles(
                new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()),
                pos.x, pos.y + 0.1D, pos.z,
                28, 0.45D, 0.15D, 0.45D, 0.08D);
        level.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y + 0.2D, pos.z, 8, 0.3D, 0.1D, 0.3D, 0.02D);
    }
}
