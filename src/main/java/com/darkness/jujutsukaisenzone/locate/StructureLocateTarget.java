package com.darkness.jujutsukaisenzone.locate;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;

import javax.annotation.Nullable;

/**
 * 可定位目标：结构类型和/或村庄 jigsaw 模板片。
 */
public final class StructureLocateTarget
{
    public static final StructureLocateTarget PLAINS_LIAISON_OFFICE = new StructureLocateTarget(
            "liaison_office",
            BuiltinStructures.VILLAGE_PLAINS,
            ResourceLocation.fromNamespaceAndPath(
                    "jujutsukaisenzone", "jujutsu_phenomena_council_liaison_office_v4"),
            "连络所");

    private final String id;
    private final ResourceKey<Structure> hostStructure;
    @Nullable
    private final ResourceLocation templatePiece;
    private final String displayName;

    public StructureLocateTarget(
            String id,
            ResourceKey<Structure> hostStructure,
            @Nullable ResourceLocation templatePiece,
            String displayName)
    {
        this.id = id;
        this.hostStructure = hostStructure;
        this.templatePiece = templatePiece;
        this.displayName = displayName;
    }

    public String id()
    {
        return this.id;
    }

    public ResourceKey<Structure> hostStructure()
    {
        return this.hostStructure;
    }

    @Nullable
    public ResourceLocation templatePiece()
    {
        return this.templatePiece;
    }

    public String displayName()
    {
        return this.displayName;
    }
}
