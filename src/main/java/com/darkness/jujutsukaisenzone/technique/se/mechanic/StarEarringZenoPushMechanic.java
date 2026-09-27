package com.darkness.jujutsukaisenzone.technique.se.mechanic;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.technique.se.ZenoWallSeMechanic;
import net.minecraft.network.chat.Component;

/**
 * 掉钻的星形耳环：跨序之星的芝诺墙换成施加力，可以把人推着走。
 */
public final class StarEarringZenoPushMechanic implements ZenoWallSeMechanic
{
    public static final String SE_ID = "star_zeno_push";

    /**
     * 每 tick 的外向冲量。原版同星吸引是 {@code ATTRACT_IMPULSE = 0.010}，这里给两倍——
     * 推得动、又不会像弹射（{@code BURST_IMPULSE_NEAR = 1.40}）那样把人掀飞。手感待调。
     */
    public static final double OUTWARD_IMPULSE = 0.020D;

    @Override
    public String seId()
    {
        return SE_ID;
    }

    @Override
    public double outwardImpulse()
    {
        return OUTWARD_IMPULSE;
    }

    @Override
    public boolean pushesOwner()
    {
        return false;
    }

    @Override
    public Component effectDescription()
    {
        return Component.translatable("gui." + JuJutsuKaisenZone.MODID + ".se.effect." + SE_ID);
    }
}
