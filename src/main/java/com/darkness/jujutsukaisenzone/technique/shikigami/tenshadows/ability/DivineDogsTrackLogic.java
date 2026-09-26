package com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability;

import com.darkness.jujutsukaisenzone.technique.shikigami.ShikigamiAbility;
import com.darkness.jujutsukaisenzone.technique.shikigami.ShikigamiAbilitySource;
import com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.TenShadowsFieldQuery;
import com.darkness.jujutsukaisenzone.technique.skill.TenShadowsShikigami;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.List;

/**
 * 玉犬 · 追踪（主动，Shift+1）：占位——令场上玉犬朝向并锁定附近最近敌对生物。
 */
public final class DivineDogsTrackLogic
{
    private static final double SEARCH_RANGE = 32.0D;

    private DivineDogsTrackLogic()
    {
    }

    public static boolean cast(ServerPlayer owner, ShikigamiAbility ability, ShikigamiAbilitySource source)
    {
        List<Mob> dogs = TenShadowsFieldQuery.aliveMobs(owner, TenShadowsShikigami.DIVINE_DOGS);
        if (dogs.isEmpty())
        {
            return false;
        }

        LivingEntity target = findNearestHostile(owner);
        if (target == null)
        {
            owner.displayClientMessage(
                    Component.literal("玉犬 · 追踪：附近没有目标").withStyle(ChatFormatting.GRAY),
                    true);
            return false;
        }

        for (Mob dog : dogs)
        {
            dog.setTarget(target);
            dog.getNavigation().moveTo(target, 1.35D);
        }
        owner.displayClientMessage(
                Component.literal("玉犬 · 追踪 → " + target.getName().getString())
                        .withStyle(ChatFormatting.DARK_PURPLE),
                true);
        return true;
    }

    private static LivingEntity findNearestHostile(ServerPlayer owner)
    {
        AABB box = owner.getBoundingBox().inflate(SEARCH_RANGE);
        return owner.serverLevel().getEntitiesOfClass(
                        LivingEntity.class,
                        box,
                        living -> living.isAlive()
                                && living != owner
                                && (living instanceof Monster || living instanceof ServerPlayer)
                                && owner.canAttack(living))
                .stream()
                .min(Comparator.comparingDouble(owner::distanceToSqr))
                .orElse(null);
    }
}
