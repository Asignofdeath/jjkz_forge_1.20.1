package com.darkness.jujutsukaisenzone.effect;

import com.darkness.jujutsukaisenzone.compat.EpicFightCompat;
import com.darkness.jujutsukaisenzone.registry.ModEffects;
import com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability.mahoraga.MahoragaAdaptation;
import com.darkness.jujutsukaisenzone.technique.skill.JudgmentSoundLogic;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import org.joml.Vector3f;

/**
 * 没收：玩家随机获得禁止领域 / 禁止咒力 / 没收装备之一；非玩家则随机获得原版 debuff。
 */
public final class ConfiscationEffect extends MobEffect
{
    public static final int VARIANT_DOMAIN = 0;
    public static final int VARIANT_CURSED_ENERGY = 1;
    public static final int VARIANT_EQUIPMENT = 2;

    public static final int DOMAIN_DURATION_TICKS = 20 * 60;
    public static final int CURSED_ENERGY_DURATION_TICKS = 20 * 60 * 3;
    private static final int MOB_DEBUFF_MIN_TICKS = 20 * 60;
    private static final int MOB_DEBUFF_MAX_TICKS = 20 * 60 * 3;
    private static final int PARTICLE_INTERVAL_TICKS = 5;
    private static final DustParticleOptions BLACK_DUST =
            new DustParticleOptions(new Vector3f(0.05F, 0.05F, 0.05F), 1.0F);
    private static final DustParticleOptions GRAY_DUST =
            new DustParticleOptions(new Vector3f(0.55F, 0.55F, 0.55F), 1.0F);

    private static final MobEffect[] MOB_DEBUFF_POOL = {
            MobEffects.MOVEMENT_SLOWDOWN,
            MobEffects.WEAKNESS,
            MobEffects.BLINDNESS,
            MobEffects.POISON,
            MobEffects.WITHER,
            MobEffects.DIG_SLOWDOWN
    };

    public ConfiscationEffect()
    {
        super(MobEffectCategory.HARMFUL, 0x2F2F2F);
    }

    public static void applyRandom(ServerPlayer caster, LivingEntity target)
    {
        if (MahoragaAdaptation.tryEngage(target, "judgment_confiscation"))
        {
            return;
        }
        if (target instanceof ServerPlayer playerTarget)
        {
            applyRandomPlayerConfiscation(caster, playerTarget);
        }
        else
        {
            applyRandomMobDebuff(target);
        }
        JudgmentSoundLogic.playConfiscation(caster, target);
    }

    public static boolean blocksDomainExpansion(ServerPlayer player)
    {
        MobEffectInstance instance = player.getEffect(ModEffects.CONFISCATION.get());
        return instance != null && instance.getAmplifier() == VARIANT_DOMAIN;
    }

    public static boolean blocksCursedEnergy(ServerPlayer player)
    {
        MobEffectInstance instance = player.getEffect(ModEffects.CONFISCATION.get());
        return instance != null && instance.getAmplifier() == VARIANT_CURSED_ENERGY;
    }

    public static void remove(LivingEntity target)
    {
        if (target == null)
        {
            return;
        }
        target.removeEffect(ModEffects.CONFISCATION.get());
    }

    public static boolean blockDomainExpansion(ServerPlayer player)
    {
        if (!blocksDomainExpansion(player))
        {
            return false;
        }
        player.displayClientMessage(
                Component.literal("领域已被没收，无法展开领域")
                        .withStyle(ChatFormatting.DARK_GRAY),
                true);
        return true;
    }

    public static boolean blockCursedEnergyUse(ServerPlayer player)
    {
        if (!blocksCursedEnergy(player))
        {
            return false;
        }
        player.displayClientMessage(
                Component.literal("咒力已被没收，无法使用咒力")
                        .withStyle(ChatFormatting.GRAY),
                true);
        return true;
    }

    public static void tickParticles(LivingEntity entity, MobEffectInstance instance)
    {
        if (entity.level().isClientSide || entity.tickCount % PARTICLE_INTERVAL_TICKS != 0)
        {
            return;
        }
        if (!(entity.level() instanceof ServerLevel level))
        {
            return;
        }

        int amplifier = instance.getAmplifier();
        double x = entity.getX();
        double y = entity.getY() + entity.getBbHeight() * 0.5D;
        double z = entity.getZ();
        if (amplifier == VARIANT_DOMAIN)
        {
            level.sendParticles(BLACK_DUST, x, y, z, 4, 0.35D, 0.45D, 0.35D, 0.0D);
            level.sendParticles(ParticleTypes.SQUID_INK, x, y, z, 1, 0.2D, 0.3D, 0.2D, 0.01D);
        }
        else if (amplifier == VARIANT_CURSED_ENERGY)
        {
            level.sendParticles(GRAY_DUST, x, y, z, 5, 0.35D, 0.45D, 0.35D, 0.0D);
            level.sendParticles(ParticleTypes.ASH, x, y, z, 2, 0.25D, 0.35D, 0.25D, 0.01D);
        }
    }

    private static void applyRandomPlayerConfiscation(ServerPlayer caster, ServerPlayer target)
    {
        RandomSource random = target.getRandom();
        int variant = random.nextInt(3);
        if (variant == VARIANT_EQUIPMENT)
        {
            confiscateEquipment(caster, target);
            target.displayClientMessage(
                    Component.literal("装备与武器已被没收")
                            .withStyle(ChatFormatting.DARK_RED),
                    true);
            return;
        }

        int duration = variant == VARIANT_DOMAIN
                ? DOMAIN_DURATION_TICKS
                : CURSED_ENERGY_DURATION_TICKS;
        target.addEffect(new MobEffectInstance(
                ModEffects.CONFISCATION.get(),
                duration,
                variant,
                false,
                false,
                true));
        target.displayClientMessage(
                Component.literal(variant == VARIANT_DOMAIN
                        ? "领域已被没收（1 分钟）"
                        : "咒力已被没收（3 分钟）")
                        .withStyle(ChatFormatting.DARK_GRAY),
                true);
    }

    private static void applyRandomMobDebuff(LivingEntity target)
    {
        RandomSource random = target.getRandom();
        MobEffect debuff = MOB_DEBUFF_POOL[random.nextInt(MOB_DEBUFF_POOL.length)];
        int duration = MOB_DEBUFF_MIN_TICKS + random.nextInt(MOB_DEBUFF_MAX_TICKS - MOB_DEBUFF_MIN_TICKS + 1);
        int amplifier = debuff == MobEffects.WITHER || debuff == MobEffects.POISON ? 0 : 1;
        target.addEffect(new MobEffectInstance(debuff, duration, amplifier, false, false, true));
    }

    private static void confiscateEquipment(ServerPlayer caster, ServerPlayer target)
    {
        for (EquipmentSlot slot : EquipmentSlot.values())
        {
            if (slot.getType() != EquipmentSlot.Type.ARMOR)
            {
                continue;
            }
            transferEquipmentSlot(caster, target, slot);
        }

        WeaponPick weaponPick = pickRandomWeapon(target);
        if (weaponPick != null)
        {
            clearWeaponPick(target, weaponPick);
            giveToCaster(caster, weaponPick.stack());
        }
    }

    private static void transferEquipmentSlot(ServerPlayer caster, ServerPlayer target, EquipmentSlot slot)
    {
        ItemStack stack = target.getItemBySlot(slot);
        if (stack.isEmpty())
        {
            return;
        }
        giveToCaster(caster, stack.copy());
        target.setItemSlot(slot, ItemStack.EMPTY);
    }

    private static WeaponPick pickRandomWeapon(ServerPlayer target)
    {
        RandomSource random = target.getRandom();
        java.util.List<WeaponPick> candidates = new java.util.ArrayList<>();
        addWeaponCandidate(candidates, target, WeaponPick.hand(EquipmentSlot.MAINHAND, target.getMainHandItem()));
        addWeaponCandidate(candidates, target, WeaponPick.hand(EquipmentSlot.OFFHAND, target.getOffhandItem()));

        Inventory inventory = target.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++)
        {
            ItemStack stack = inventory.getItem(slot);
            if (isConfiscatableWeapon(stack))
            {
                candidates.add(WeaponPick.inventory(slot, stack));
            }
        }

        if (candidates.isEmpty())
        {
            return null;
        }
        return candidates.get(random.nextInt(candidates.size()));
    }

    private static void addWeaponCandidate(java.util.List<WeaponPick> candidates, ServerPlayer target, WeaponPick pick)
    {
        if (pick == null || pick.stack().isEmpty() || !isConfiscatableWeapon(pick.stack()))
        {
            return;
        }
        for (WeaponPick existing : candidates)
        {
            if (ItemStack.isSameItemSameTags(existing.stack(), pick.stack())
                    && existing.stack().getCount() == pick.stack().getCount())
            {
                return;
            }
        }
        candidates.add(pick);
    }

    private static boolean isConfiscatableWeapon(ItemStack stack)
    {
        if (stack.isEmpty())
        {
            return false;
        }
        return stack.is(ItemTags.SWORDS)
                || stack.is(ItemTags.AXES)
                || stack.getItem() instanceof BowItem
                || stack.getItem() instanceof CrossbowItem
                || stack.getItem() instanceof TridentItem
                || EpicFightCompat.isEpicFightWeapon(stack);
    }

    private static void clearWeaponPick(ServerPlayer target, WeaponPick pick)
    {
        switch (pick.kind())
        {
            case HAND -> target.setItemSlot(pick.equipmentSlot(), ItemStack.EMPTY);
            case INVENTORY -> target.getInventory().setItem(pick.inventorySlot(), ItemStack.EMPTY);
        }
        target.inventoryMenu.broadcastChanges();
    }

    private static void giveToCaster(ServerPlayer caster, ItemStack stack)
    {
        if (stack.isEmpty())
        {
            return;
        }
        if (!caster.getInventory().add(stack))
        {
            caster.drop(stack, false);
        }
        caster.inventoryMenu.broadcastChanges();
    }

    private record WeaponPick(Kind kind, EquipmentSlot equipmentSlot, int inventorySlot, ItemStack stack)
    {
        private enum Kind
        {
            HAND,
            INVENTORY
        }

        private static WeaponPick hand(EquipmentSlot slot, ItemStack stack)
        {
            return new WeaponPick(Kind.HAND, slot, -1, stack);
        }

        private static WeaponPick inventory(int slot, ItemStack stack)
        {
            return new WeaponPick(Kind.INVENTORY, EquipmentSlot.MAINHAND, slot, stack);
        }
    }
}
