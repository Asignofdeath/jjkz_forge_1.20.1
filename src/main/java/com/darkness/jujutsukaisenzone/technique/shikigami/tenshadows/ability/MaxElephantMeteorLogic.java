package com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability;

import com.darkness.jujutsukaisenzone.technique.TechniqueDamageSources;
import com.darkness.jujutsukaisenzone.technique.shikigami.ShikigamiAbility;
import com.darkness.jujutsukaisenzone.technique.shikigami.ShikigamiAbilitySource;
import com.darkness.jujutsukaisenzone.technique.skill.TenShadowsRitualLogic;
import com.darkness.jujutsukaisenzone.technique.skill.TenShadowsSummonLogic;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.Blocks;

/**
 * 满象 · 陨石坠（主动，Shift+Ctrl+5）：对准星实体砸落带伤陨石。
 */
public final class MaxElephantMeteorLogic
{
    public static final String TAG_METEOR_TICKS = "jujutsukaisenzone_elephant_meteor_ticks";
    public static final String TAG_METEOR_TARGET = "jujutsukaisenzone_elephant_meteor_target";
    public static final String TAG_METEOR_OWNER = "jujutsukaisenzone_elephant_meteor_owner";

    private static final float DAMAGE = 22.0F;
    private static final int FALL_TICKS = 18;
    private static final int DROP_HEIGHT = 16;

    private MaxElephantMeteorLogic()
    {
    }

    public static boolean cast(ServerPlayer owner, ShikigamiAbility ability, ShikigamiAbilitySource source)
    {
        if (source != ShikigamiAbilitySource.PLAYER)
        {
            return false;
        }
        Mob elephant = MaxElephantSupport.first(owner);
        if (elephant == null)
        {
            owner.displayClientMessage(
                    Component.literal("满象 · 陨石坠：场上没有满象").withStyle(ChatFormatting.GRAY),
                    true);
            return false;
        }
        if (elephant.getPersistentData().getInt(TAG_METEOR_TICKS) > 0)
        {
            owner.displayClientMessage(
                    Component.literal("满象 · 陨石坠：已在下落").withStyle(ChatFormatting.GRAY),
                    true);
            return false;
        }
        LivingEntity target = TenShadowsRitualLogic.resolveCrosshairLiving(owner, 40.0D);
        if (target == null || TenShadowsSummonLogic.isOwnedBy(target, owner.getUUID()))
        {
            owner.displayClientMessage(
                    Component.literal("满象 · 陨石坠：请准星对准实体").withStyle(ChatFormatting.GRAY),
                    true);
            return false;
        }

        ServerLevel level = owner.serverLevel();
        BlockPos spawn = BlockPos.containing(target.getX(), target.getY() + DROP_HEIGHT, target.getZ());
        FallingBlockEntity falling = FallingBlockEntity.fall(level, spawn, Blocks.MAGMA_BLOCK.defaultBlockState());
        falling.dropItem = false;
        falling.setHurtsEntities(3.0F, 40);

        elephant.getPersistentData().putInt(TAG_METEOR_TICKS, FALL_TICKS);
        elephant.getPersistentData().putUUID(TAG_METEOR_TARGET, target.getUUID());
        elephant.getPersistentData().putUUID(TAG_METEOR_OWNER, owner.getUUID());

        level.sendParticles(
                ParticleTypes.LAVA,
                target.getX(), target.getY() + DROP_HEIGHT * 0.4D, target.getZ(),
                24, 0.5D, 3.0D, 0.5D, 0.02D);
        owner.displayClientMessage(
                Component.literal("满象 · 陨石坠 → " + target.getName().getString())
                        .withStyle(ChatFormatting.RED),
                true);
        return true;
    }

    public static void tickElephant(Mob elephant)
    {
        if (elephant.level().isClientSide || !(elephant.level() instanceof ServerLevel level))
        {
            return;
        }
        if (!MaxElephantSupport.isMaxElephant(elephant))
        {
            return;
        }
        int remain = elephant.getPersistentData().getInt(TAG_METEOR_TICKS);
        if (remain <= 0)
        {
            return;
        }
        remain--;
        elephant.getPersistentData().putInt(TAG_METEOR_TICKS, remain);
        if (remain > 0)
        {
            return;
        }
        if (!elephant.getPersistentData().hasUUID(TAG_METEOR_TARGET)
                || !elephant.getPersistentData().hasUUID(TAG_METEOR_OWNER))
        {
            clear(elephant);
            return;
        }
        Entity targetEntity = level.getEntity(elephant.getPersistentData().getUUID(TAG_METEOR_TARGET));
        Entity ownerEntity = level.getEntity(elephant.getPersistentData().getUUID(TAG_METEOR_OWNER));
        clear(elephant);
        if (!(targetEntity instanceof LivingEntity target) || !target.isAlive())
        {
            return;
        }
        if (!(ownerEntity instanceof LivingEntity owner))
        {
            return;
        }
        TechniqueDamageSources.deal(target, owner, DAMAGE);
        level.sendParticles(
                ParticleTypes.EXPLOSION,
                target.getX(), target.getY() + 0.4D, target.getZ(),
                8, 0.5D, 0.35D, 0.5D, 0.02D);
        level.sendParticles(
                ParticleTypes.LAVA,
                target.getX(), target.getY() + 0.2D, target.getZ(),
                16, 0.6D, 0.2D, 0.6D, 0.05D);
    }

    private static void clear(Mob elephant)
    {
        elephant.getPersistentData().remove(TAG_METEOR_TICKS);
        elephant.getPersistentData().remove(TAG_METEOR_TARGET);
        elephant.getPersistentData().remove(TAG_METEOR_OWNER);
    }
}
