package com.darkness.jujutsukaisenzone.worldgen;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModStructureTypes
{
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, JuJutsuKaisenZone.MODID);

    public static final RegistryObject<StructureType<NearVillageJigsawStructure>> NEAR_VILLAGE_JIGSAW =
            STRUCTURE_TYPES.register("near_village_jigsaw", () -> () -> NearVillageJigsawStructure.CODEC);

    private ModStructureTypes()
    {
    }

    public static void register(IEventBus eventBus)
    {
        STRUCTURE_TYPES.register(eventBus);
    }
}
