package com.darkness.jujutsukaisenzone.technique.skill;

import com.darkness.jujutsukaisenzone.registry.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 法槌各形态的投掷参数；近战参数见 {@code data/.../capabilities/weapons/sm_gavel_weapon.json}。
 */
public final class GavelFormStats
{
    public static final float STANDARD_PROJECTILE_DAMAGE = 6.0F;
    public static final float STANDARD_EXPLOSION_POWER = 1.5F;
    public static final float STANDARD_SHOOT_POWER = 3.0F;
    public static final int STANDARD_SHOOT_COOLDOWN_TICKS = 5;

    private static final float LONG_HANDLE_SHOOT_POWER = 3.9F;
    private static final int LONG_HANDLE_SHOOT_COOLDOWN_TICKS = 12;

    private static final float BIG_HAMMER_SHOOT_POWER = 2.1F;
    private static final int BIG_HAMMER_SHOOT_COOLDOWN_TICKS = 25;
    private static final float BIG_HAMMER_PROJECTILE_DAMAGE = 15.0F;
    private static final float BIG_HAMMER_EXPLOSION_POWER = 4.0F;

    private GavelFormStats()
    {
    }

    public static int resolveForm(ItemStack stack)
    {
        return JudgmentWeaponLogic.getGavelForm(stack);
    }

    public static float getShootPower(ItemStack stack)
    {
        return switch (resolveForm(stack))
        {
            case JudgmentWeaponLogic.GAVEL_FORM_LONG_HANDLE -> LONG_HANDLE_SHOOT_POWER;
            case JudgmentWeaponLogic.GAVEL_FORM_BIG_HAMMER -> BIG_HAMMER_SHOOT_POWER;
            default -> STANDARD_SHOOT_POWER;
        };
    }

    public static int getShootCooldownTicks(ItemStack stack)
    {
        return switch (resolveForm(stack))
        {
            case JudgmentWeaponLogic.GAVEL_FORM_LONG_HANDLE -> LONG_HANDLE_SHOOT_COOLDOWN_TICKS;
            case JudgmentWeaponLogic.GAVEL_FORM_BIG_HAMMER -> BIG_HAMMER_SHOOT_COOLDOWN_TICKS;
            default -> STANDARD_SHOOT_COOLDOWN_TICKS;
        };
    }

    public static float getProjectileDamage(ItemStack stack)
    {
        return switch (resolveForm(stack))
        {
            case JudgmentWeaponLogic.GAVEL_FORM_BIG_HAMMER -> BIG_HAMMER_PROJECTILE_DAMAGE;
            default -> STANDARD_PROJECTILE_DAMAGE;
        };
    }

    public static float getExplosionPower(ItemStack stack)
    {
        return switch (resolveForm(stack))
        {
            case JudgmentWeaponLogic.GAVEL_FORM_BIG_HAMMER -> BIG_HAMMER_EXPLOSION_POWER;
            default -> STANDARD_EXPLOSION_POWER;
        };
    }

    public static Level.ExplosionInteraction getExplosionInteraction(ItemStack stack)
    {
        if (stack.is(ModItems.SM_GAVEL_WEAPON.get())
                && resolveForm(stack) == JudgmentWeaponLogic.GAVEL_FORM_BIG_HAMMER)
        {
            return Level.ExplosionInteraction.BLOCK;
        }
        return Level.ExplosionInteraction.NONE;
    }
}
