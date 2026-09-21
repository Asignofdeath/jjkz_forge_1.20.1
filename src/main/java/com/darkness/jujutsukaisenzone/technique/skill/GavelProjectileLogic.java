package com.darkness.jujutsukaisenzone.technique.skill;

import com.darkness.jujutsukaisenzone.entity.GavelProjectileEntity;
import com.darkness.jujutsukaisenzone.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class GavelProjectileLogic
{
    private static final float SHOOT_INACCURACY = 0.0F;

    private GavelProjectileLogic()
    {
    }

    public static boolean tryShoot(ServerPlayer player, InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(ModItems.SM_GAVEL_WEAPON.get()))
        {
            return false;
        }
        if (player.getCooldowns().isOnCooldown(ModItems.SM_GAVEL_WEAPON.get()))
        {
            return false;
        }

        Level level = player.level();
        GavelProjectileEntity projectile = new GavelProjectileEntity(level, player);
        projectile.setItem(stack.copy());
        projectile.shootFromRotation(
                player,
                player.getXRot(),
                player.getYRot(),
                0.0F,
                GavelFormStats.getShootPower(stack),
                SHOOT_INACCURACY);
        if (!level.addFreshEntity(projectile))
        {
            return false;
        }

        level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.TRIDENT_THROW,
                SoundSource.PLAYERS,
                1.0F,
                1.1F);

        if (!player.getAbilities().instabuild)
        {
            stack.shrink(1);
        }
        player.awardStat(Stats.ITEM_USED.get(ModItems.SM_GAVEL_WEAPON.get()));
        player.getCooldowns().addCooldown(ModItems.SM_GAVEL_WEAPON.get(), GavelFormStats.getShootCooldownTicks(stack));
        return true;
    }
}
