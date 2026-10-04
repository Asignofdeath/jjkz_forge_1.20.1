package com.darkness.jujutsukaisenzone.locate;

import com.darkness.jujutsukaisenzone.worldgen.ModStructures;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.StructureTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.Structure;

import javax.annotation.Nullable;

/**
 * 可定位目标：单个结构、结构标签，和/或 jigsaw 模板片。
 */
public final class StructureLocateTarget
{
    /** 贴村独立连络所结构（非村庄 piece）。 */
    public static final StructureLocateTarget PLAINS_LIAISON_OFFICE = new StructureLocateTarget(
            "liaison_office",
            ModStructures.PLAINS_LIAISON_OFFICE,
            null,
            null,
            "连络所",
            PlayerLocateGuidance.ARRIVE_DISTANCE);

    /** 最近村庄（任意类型，{@code #minecraft:village}）。 */
    public static final StructureLocateTarget NEAREST_VILLAGE = new StructureLocateTarget(
            "village",
            null,
            StructureTags.VILLAGE,
            null,
            "村庄",
            32.0D);

    private final String id;
    @Nullable
    private final ResourceKey<Structure> hostStructure;
    @Nullable
    private final TagKey<Structure> structureTag;
    @Nullable
    private final ResourceLocation templatePiece;
    private final String displayName;
    private final double arriveDistance;

    public StructureLocateTarget(
            String id,
            @Nullable ResourceKey<Structure> hostStructure,
            @Nullable TagKey<Structure> structureTag,
            @Nullable ResourceLocation templatePiece,
            String displayName,
            double arriveDistance)
    {
        if (hostStructure == null && structureTag == null)
        {
            throw new IllegalArgumentException("StructureLocateTarget needs hostStructure or structureTag");
        }
        this.id = id;
        this.hostStructure = hostStructure;
        this.structureTag = structureTag;
        this.templatePiece = templatePiece;
        this.displayName = displayName;
        this.arriveDistance = arriveDistance;
    }

    /** 单结构目标（默认抵达距离）。 */
    public StructureLocateTarget(
            String id,
            ResourceKey<Structure> hostStructure,
            @Nullable ResourceLocation templatePiece,
            String displayName)
    {
        this(id, hostStructure, null, templatePiece, displayName, PlayerLocateGuidance.ARRIVE_DISTANCE);
    }

    public String id()
    {
        return this.id;
    }

    @Nullable
    public ResourceKey<Structure> hostStructure()
    {
        return this.hostStructure;
    }

    @Nullable
    public TagKey<Structure> structureTag()
    {
        return this.structureTag;
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

    public double arriveDistance()
    {
        return this.arriveDistance;
    }
}
