package com.darkness.jujutsukaisenzone.effect;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * 判定箱子是否位于地牢、废弃矿井或村庄结构中。
 */
public final class ProtectedChestStructureRules
{
    private static final String MINESHAFT_STRUCTURE = "mineshaft";
    private static final String MONSTER_ROOM_STRUCTURE = "monster_room";

    private ProtectedChestStructureRules()
    {
    }

    public static boolean isLootableChestBlock(BlockState state)
    {
        return state.getBlock() instanceof ChestBlock;
    }

    public static boolean isProtectedStructureChest(ServerLevel level, BlockPos pos)
    {
        for (Structure structure : level.structureManager().getAllStructuresAt(pos).keySet())
        {
            ResourceLocation id = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getKey(structure);
            if (id == null)
            {
                continue;
            }
            String path = id.getPath();
            if (MINESHAFT_STRUCTURE.equals(path) || MONSTER_ROOM_STRUCTURE.equals(path))
            {
                return true;
            }
            if (level.registryAccess().registryOrThrow(Registries.STRUCTURE).getResourceKey(structure)
                    .flatMap(level.registryAccess().registryOrThrow(Registries.STRUCTURE)::getHolder)
                    .map(holder -> holder.is(StructureTags.VILLAGE))
                    .orElse(false))
            {
                return true;
            }
        }
        return false;
    }
}
