package com.darkness.jujutsukaisenzone.client;

import com.darkness.jujutsukaisenzone.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

/**
 * 获得处刑人之剑时：图腾同款物品冲屏演出（{@link net.minecraft.client.renderer.GameRenderer#displayItemActivation}）。
 */
public final class ExecutionersSwordActivationClientEffects
{
    private static final int TOTEM_PARTICLE_COUNT = 40;

    private ExecutionersSwordActivationClientEffects()
    {
    }

    public static void play()
    {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null)
        {
            return;
        }

        ItemStack stack = new ItemStack(ModItems.EXECUTIONERS_SWORD.get());
        mc.gameRenderer.displayItemActivation(stack);
        spawnTotemParticles(player);
        player.playSound(SoundEvents.TOTEM_USE, 1.0F, 1.0F);
    }

    private static void spawnTotemParticles(LocalPlayer player)
    {
        RandomSource random = player.getRandom();
        for (int i = 0; i < TOTEM_PARTICLE_COUNT; i++)
        {
            double vx = random.nextGaussian() * 0.02D;
            double vy = random.nextGaussian() * 0.02D;
            double vz = random.nextGaussian() * 0.02D;
            player.level().addParticle(
                    ParticleTypes.TOTEM_OF_UNDYING,
                    player.getRandomX(1.0D),
                    player.getRandomY() + 1.0D,
                    player.getRandomZ(1.0D),
                    vx,
                    vy + 0.05D,
                    vz);
        }
    }
}
