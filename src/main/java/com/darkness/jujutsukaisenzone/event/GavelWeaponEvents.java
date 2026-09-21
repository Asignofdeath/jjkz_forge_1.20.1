package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.compat.EpicFightCompat;
import com.darkness.jujutsukaisenzone.effect.SinfulOneEffect;
import com.darkness.jujutsukaisenzone.entity.GavelProjectileEntity;
import com.darkness.jujutsukaisenzone.registry.ModItems;
import com.darkness.jujutsukaisenzone.technique.skill.GavelProjectileLogic;
import com.darkness.jujutsukaisenzone.technique.skill.JudgmentWeaponLogic;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yesman.epicfight.world.damagesource.EpicFightDamageSource;

/**
 * 法槌：右键发射；对【罪恶者】目标增伤。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GavelWeaponEvents
{
    private GavelWeaponEvents()
    {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttachItemCapabilities(AttachCapabilitiesEvent<ItemStack> event)
    {
        JudgmentWeaponLogic.ensureGavelFormTag(event.getObject());
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event)
    {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ServerPlayer player))
        {
            return;
        }
        if (!EpicFightCompat.isBattleMode(player))
        {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (!stack.is(ModItems.SM_GAVEL_WEAPON.get()))
        {
            return;
        }
        InteractionHand hand = event.getHand();
        if (!GavelProjectileLogic.tryShoot(player, hand))
        {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event)
    {
        if (event.getEntity().level().isClientSide())
        {
            return;
        }
        LivingEntity target = event.getEntity();
        if (!SinfulOneEffect.isActive(target) || !isGavelWeaponDamage(event.getSource()))
        {
            return;
        }
        event.setAmount(JudgmentWeaponLogic.boostDamageAgainstSinfulOne(target, event.getAmount()));
    }

    private static boolean isGavelWeaponDamage(DamageSource source)
    {
        if (source.getDirectEntity() instanceof GavelProjectileEntity)
        {
            return true;
        }
        if (!(source.getEntity() instanceof ServerPlayer player))
        {
            return false;
        }
        if (!JudgmentWeaponLogic.isWieldingGavel(player))
        {
            return false;
        }
        if (source instanceof EpicFightDamageSource)
        {
            return true;
        }
        return source.getDirectEntity() == player;
    }
}
