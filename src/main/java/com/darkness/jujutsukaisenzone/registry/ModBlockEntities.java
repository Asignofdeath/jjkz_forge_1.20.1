package com.darkness.jujutsukaisenzone.registry;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.block.JJKZSkyblock;
import com.darkness.jujutsukaisenzone.blockentity.JJKZSkyblockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities
{
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, JuJutsuKaisenZone.MODID);

    public static final RegistryObject<BlockEntityType<JJKZSkyblockEntity>> JJKZ_SKYBLOCK =
            BLOCK_ENTITIES.register(
                    "jjkz_skyblock",
                    () -> BlockEntityType.Builder.of(
                                    JJKZSkyblockEntity::new,
                                    ModBlocks.JJKZ_SKYBLOCK.get()
                            ).build(null)
            );

    private ModBlockEntities()
    {
    }

    public static void register(IEventBus eventBus)
    {
        BLOCK_ENTITIES.register(eventBus);
    }
}
