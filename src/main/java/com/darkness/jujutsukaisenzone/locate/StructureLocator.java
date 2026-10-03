package com.darkness.jujutsukaisenzone.locate;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import javax.annotation.Nullable;

/**
 * 通用结构/模板片查找。
 * <p>
 * 若目标指定了模板片，则<strong>必须</strong>在已生成的 StructureStart 中匹配到该片才算成功，
 * 绝不回退到「仅有宿主结构（如村庄）中心」的坐标。
 */
public final class StructureLocator
{
    /** 默认搜索半径（区块）。 */
    public static final int DEFAULT_RADIUS_CHUNKS = 100;

    /** SinglePoolElement.template — Either&lt;ResourceLocation, StructureTemplate&gt; */
    private static final String FIELD_TEMPLATE = "f_210411_";

    private StructureLocator()
    {
    }

    public static Optional<BlockPos> findNearest(ServerLevel level, BlockPos origin, StructureLocateTarget target)
    {
        return findNearest(level, origin, target, DEFAULT_RADIUS_CHUNKS);
    }

    public static Optional<BlockPos> findNearest(
            ServerLevel level,
            BlockPos origin,
            StructureLocateTarget target,
            int radiusChunks)
    {
        if (radiusChunks <= 0)
        {
            return Optional.empty();
        }

        Registry<Structure> structures = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        Holder.Reference<Structure> holder = structures.getHolder(target.hostStructure()).orElse(null);
        if (holder == null)
        {
            return Optional.empty();
        }

        ResourceLocation pieceId = target.templatePiece();
        if (pieceId != null)
        {
            // 必须匹配到真实模板片；禁止回退到空村坐标
            return findNearestPieceFromOrigin(level, origin, holder.value(), pieceId, radiusChunks);
        }

        HolderSet<Structure> holderSet = HolderSet.direct(holder);
        Pair<BlockPos, Holder<Structure>> nearest = level.getChunkSource().getGenerator()
                .findNearestMapStructure(level, holderSet, origin, radiusChunks, false);
        if (nearest == null)
        {
            return Optional.empty();
        }
        return Optional.of(nearest.getFirst());
    }

    /**
     * 以原点为中心扫描已生成区块，只返回含指定模板片的最近坐标。
     * 不含该片的宿主结构一律忽略。
     */
    private static Optional<BlockPos> findNearestPieceFromOrigin(
            ServerLevel level,
            BlockPos origin,
            Structure host,
            ResourceLocation pieceId,
            int radiusChunks)
    {
        int originChunkX = origin.getX() >> 4;
        int originChunkZ = origin.getZ() >> 4;
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;

        int scan = Math.min(radiusChunks, 48);
        for (int r = 0; r <= scan; r++)
        {
            for (int dz = -r; dz <= r; dz++)
            {
                for (int dx = -r; dx <= r; dx++)
                {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r)
                    {
                        continue;
                    }
                    ChunkAccess chunk = level.getChunk(originChunkX + dx, originChunkZ + dz, ChunkStatus.STRUCTURE_STARTS, false);
                    if (chunk == null)
                    {
                        continue;
                    }
                    StructureStart start = chunk.getStartForStructure(host);
                    if (start == null || !start.isValid())
                    {
                        continue;
                    }
                    BlockPos piece = findPieceInStart(start, pieceId);
                    if (piece == null)
                    {
                        continue;
                    }
                    double dist = piece.distSqr(origin);
                    if (dist < bestDist)
                    {
                        bestDist = dist;
                        best = piece;
                    }
                }
            }
            if (best != null && r >= 8)
            {
                break;
            }
        }
        return Optional.ofNullable(best);
    }

    @Nullable
    private static BlockPos findPieceInStart(StructureStart start, ResourceLocation pieceId)
    {
        for (StructurePiece piece : start.getPieces())
        {
            if (!(piece instanceof PoolElementStructurePiece poolPiece))
            {
                continue;
            }
            if (!matchesTemplate(poolPiece.getElement(), pieceId))
            {
                continue;
            }
            return poolPiece.getBoundingBox().getCenter();
        }
        return null;
    }

    private static boolean matchesTemplate(StructurePoolElement element, ResourceLocation want)
    {
        if (!(element instanceof SinglePoolElement single))
        {
            return false;
        }
        Either<ResourceLocation, StructureTemplate> either =
                ObfuscationReflectionHelper.getPrivateValue(SinglePoolElement.class, single, FIELD_TEMPLATE);
        if (either == null)
        {
            return element.toString().contains(want.getPath());
        }
        Optional<ResourceLocation> left = either.left();
        if (left.isEmpty())
        {
            return false;
        }
        ResourceLocation got = left.get();
        return want.equals(got) || want.getPath().equals(got.getPath());
    }
}
