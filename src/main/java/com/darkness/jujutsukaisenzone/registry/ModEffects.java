package com.darkness.jujutsukaisenzone.registry;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.effect.RushIntoTheZoneClimaxEffect;
import com.darkness.jujutsukaisenzone.effect.TechniqueMeltdownEffect;
import com.darkness.jujutsukaisenzone.effect.ZoneEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEffects
{
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, JuJutsuKaisenZone.MODID);

    public static final RegistryObject<MobEffect> ZONE = EFFECTS.register("zone", ZoneEffect::new);
    public static final RegistryObject<MobEffect> TECHNIQUE_MELTDOWN =
            EFFECTS.register("technique_meltdown", TechniqueMeltdownEffect::new);
    public static final RegistryObject<MobEffect> RUSH_INTO_THE_ZONE_CLIMAX =
            EFFECTS.register("rush_into_the_zone_climax", RushIntoTheZoneClimaxEffect::new);

    private ModEffects()
    {
    }

    public static void register(IEventBus bus)
    {
        EFFECTS.register(bus);
    }
}
