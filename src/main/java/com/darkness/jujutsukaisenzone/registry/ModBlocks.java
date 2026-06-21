package com.darkness.jujutsukaisenzone.registry;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.block.BarrierBlock;
import com.darkness.jujutsukaisenzone.block.DomainBlock;
import com.darkness.jujutsukaisenzone.block.ShroudBlock;
import com.darkness.jujutsukaisenzone.block.JJKZSkyblock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks
{
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, JuJutsuKaisenZone.MODID);

    public static final RegistryObject<Block> BARRIER_BLOCK = BLOCKS.register(
            "barrier_block",
            () -> new BarrierBlock()
    );

    public static final RegistryObject<Block> SHROUD_BLOCK = BLOCKS.register(
            "shroud_block",
            ShroudBlock::new
    );

    public static final RegistryObject<Block> DOMAIN_BLOCK = BLOCKS.register(
            "domain_block",
            () -> new DomainBlock()
    );

    public static final RegistryObject<Block> JJKZ_SKYBLOCK = BLOCKS.register(
            "jjkz_skyblock",
            JJKZSkyblock::new
    );

    private ModBlocks()
    {
    }

    public static void register(IEventBus eventBus)
    {
        BLOCKS.register(eventBus);
    }
}
