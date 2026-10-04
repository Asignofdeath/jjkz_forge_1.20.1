package com.darkness.jujutsukaisenzone.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.structure.BuiltinStructureSets;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

/**
 * 贴村独立 jigsaw：每个村庄种子区块确定性指定唯一连络所区块（有且仅有 1 次尝试）。
 * 不算村庄一部分；贴地靠 heightmap + terrain_adaptation。
 */
public final class NearVillageJigsawStructure extends Structure
{
    /** 与村庄 salt 分离，用于从村种子推导连络所区块。 */
    private static final int DESIGNATION_SALT = 714829301;

    public static final Codec<NearVillageJigsawStructure> CODEC = ExtraCodecs.validate(
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    settingsCodec(instance),
                    StructureTemplatePool.CODEC.fieldOf("start_pool")
                            .forGetter(s -> s.startPool),
                    ResourceLocation.CODEC.optionalFieldOf("start_jigsaw_name")
                            .forGetter(s -> s.startJigsawName),
                    Codec.intRange(0, 7).fieldOf("size")
                            .forGetter(s -> s.maxDepth),
                    HeightProvider.CODEC.fieldOf("start_height")
                            .forGetter(s -> s.startHeight),
                    Codec.BOOL.fieldOf("use_expansion_hack")
                            .forGetter(s -> s.useExpansionHack),
                    Heightmap.Types.CODEC.optionalFieldOf("project_start_to_heightmap")
                            .forGetter(s -> s.projectStartToHeightmap),
                    Codec.intRange(1, 128).fieldOf("max_distance_from_center")
                            .forGetter(s -> s.maxDistanceFromCenter),
                    Codec.intRange(1, 32).fieldOf("min_village_chunk_distance")
                            .forGetter(s -> s.minVillageChunkDistance),
                    Codec.intRange(1, 32).fieldOf("max_village_chunk_distance")
                            .forGetter(s -> s.maxVillageChunkDistance)
            ).apply(instance, NearVillageJigsawStructure::new)),
            NearVillageJigsawStructure::verify
    ).codec();

    private final Holder<StructureTemplatePool> startPool;
    private final Optional<ResourceLocation> startJigsawName;
    private final int maxDepth;
    private final HeightProvider startHeight;
    private final boolean useExpansionHack;
    private final Optional<Heightmap.Types> projectStartToHeightmap;
    private final int maxDistanceFromCenter;
    private final int minVillageChunkDistance;
    private final int maxVillageChunkDistance;

    private static DataResult<NearVillageJigsawStructure> verify(NearVillageJigsawStructure structure)
    {
        if (structure.minVillageChunkDistance > structure.maxVillageChunkDistance)
        {
            return DataResult.error(() -> "min_village_chunk_distance > max_village_chunk_distance");
        }
        int beard = structure.terrainAdaptation() == TerrainAdjustment.NONE ? 0 : 12;
        if (structure.maxDistanceFromCenter + beard > 128)
        {
            return DataResult.error(() -> "Structure size including terrain adaptation must not exceed 128");
        }
        return DataResult.success(structure);
    }

    public NearVillageJigsawStructure(
            StructureSettings settings,
            Holder<StructureTemplatePool> startPool,
            Optional<ResourceLocation> startJigsawName,
            int maxDepth,
            HeightProvider startHeight,
            boolean useExpansionHack,
            Optional<Heightmap.Types> projectStartToHeightmap,
            int maxDistanceFromCenter,
            int minVillageChunkDistance,
            int maxVillageChunkDistance)
    {
        super(settings);
        this.startPool = startPool;
        this.startJigsawName = startJigsawName;
        this.maxDepth = maxDepth;
        this.startHeight = startHeight;
        this.useExpansionHack = useExpansionHack;
        this.projectStartToHeightmap = projectStartToHeightmap;
        this.maxDistanceFromCenter = maxDistanceFromCenter;
        this.minVillageChunkDistance = minVillageChunkDistance;
        this.maxVillageChunkDistance = maxVillageChunkDistance;
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context)
    {
        if (!isExclusiveDesignatedChunk(context))
        {
            return Optional.empty();
        }

        ChunkPos chunkPos = context.chunkPos();
        int y = this.startHeight.sample(
                context.random(),
                new WorldGenerationContext(context.chunkGenerator(), context.heightAccessor()));
        BlockPos blockPos = new BlockPos(chunkPos.getMinBlockX(), y, chunkPos.getMinBlockZ());
        // size 必须 >=1：原版 JigsawPlacement 在 maxDepth==0 时不会把起始片加入 builder
        int depth = Math.max(1, this.maxDepth);
        return JigsawPlacement.addPieces(
                context,
                this.startPool,
                this.startJigsawName,
                depth,
                blockPos,
                this.useExpansionHack,
                this.projectStartToHeightmap,
                this.maxDistanceFromCenter);
    }

    /**
     * 仅当本区块是某个村庄种子「唯一指定」的连络所区块时返回 true → 每村至多 1。
     * 结构集 spacing=1 保证该指定区块总会走到这里 → 每村至少尝试 1 次。
     */
    private boolean isExclusiveDesignatedChunk(GenerationContext context)
    {
        RandomSpreadStructurePlacement villagePlacement = villagePlacement(context);
        if (villagePlacement == null)
        {
            return false;
        }

        long seed = context.seed();
        ChunkPos here = context.chunkPos();
        int min = this.minVillageChunkDistance;
        int max = this.maxVillageChunkDistance;

        for (int dx = -max; dx <= max; dx++)
        {
            for (int dz = -max; dz <= max; dz++)
            {
                int chebyshev = Math.max(Math.abs(dx), Math.abs(dz));
                if (chebyshev < min || chebyshev > max)
                {
                    continue;
                }
                int vx = here.x - dx;
                int vz = here.z - dz;
                if (!isVillageSeedChunk(villagePlacement, seed, vx, vz))
                {
                    continue;
                }
                ChunkPos designated = designateLiaisonChunk(seed, vx, vz, min, max);
                if (designated.x == here.x && designated.z == here.z)
                {
                    return true;
                }
            }
        }
        return false;
    }

    private static RandomSpreadStructurePlacement villagePlacement(GenerationContext context)
    {
        Registry<StructureSet> sets = context.registryAccess().registryOrThrow(Registries.STRUCTURE_SET);
        StructureSet villageSet = sets.get(BuiltinStructureSets.VILLAGES);
        if (villageSet == null)
        {
            return null;
        }
        StructurePlacement placement = villageSet.placement();
        return placement instanceof RandomSpreadStructurePlacement randomSpread ? randomSpread : null;
    }

    private static boolean isVillageSeedChunk(
            RandomSpreadStructurePlacement villagePlacement, long seed, int chunkX, int chunkZ)
    {
        ChunkPos potential = villagePlacement.getPotentialStructureChunk(seed, chunkX, chunkZ);
        return potential.x == chunkX && potential.z == chunkZ;
    }

    /**
     * 由世界种子 + 村种子区块确定性选出环上唯一连络所区块。
     */
    static ChunkPos designateLiaisonChunk(long seed, int villageChunkX, int villageChunkZ, int minDist, int maxDist)
    {
        WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(0L));
        random.setLargeFeatureWithSalt(seed, villageChunkX, villageChunkZ, DESIGNATION_SALT);
        int dist = minDist + random.nextInt(maxDist - minDist + 1);
        int index = random.nextInt(8 * dist);
        int[] offset = chebyshevRingOffset(dist, index);
        return new ChunkPos(villageChunkX + offset[0], villageChunkZ + offset[1]);
    }

    /** 切比雪夫距离 = dist 的环上第 index 个偏移（共 8*dist 个）。 */
    static int[] chebyshevRingOffset(int dist, int index)
    {
        int i = Math.floorMod(index, 8 * dist);
        int topCount = 2 * dist + 1;
        if (i < topCount)
        {
            return new int[] { -dist + i, -dist };
        }
        i -= topCount;
        int rightCount = 2 * dist - 1;
        if (i < rightCount)
        {
            return new int[] { dist, -dist + 1 + i };
        }
        i -= rightCount;
        int bottomCount = 2 * dist + 1;
        if (i < bottomCount)
        {
            return new int[] { dist - i, dist };
        }
        i -= bottomCount;
        return new int[] { -dist, dist - 1 - i };
    }

    @Override
    public StructureType<?> type()
    {
        return ModStructureTypes.NEAR_VILLAGE_JIGSAW.get();
    }
}
