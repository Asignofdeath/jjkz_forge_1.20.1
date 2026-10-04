package com.darkness.jujutsukaisenzone.worldgen;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * 数据包结构的 ResourceKey（locate / 配置引用）。
 */
public final class ModStructures
{
    public static final ResourceKey<Structure> PLAINS_LIAISON_OFFICE = key("plains_liaison_office");

    private ModStructures()
    {
    }

    private static ResourceKey<Structure> key(String path)
    {
        return ResourceKey.create(
                Registries.STRUCTURE,
                ResourceLocation.fromNamespaceAndPath(JuJutsuKaisenZone.MODID, path));
    }
}
