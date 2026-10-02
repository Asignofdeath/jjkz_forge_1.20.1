package com.darkness.jujutsukaisenzone.locate;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
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

        HolderSet<Structure> holderSet = HolderSet.direct(holder);
        Pair<BlockPos, Holder<Structure>> nearest = level.getChunkSource().getGenerator()
                .findNearestMapStructure(level, holderSet, origin, radiusChunks, false);
        if (nearest == null)
        {
            return Optional.empty();
        }

        BlockPos structurePos = nearest.getFirst();
        ResourceLocation pieceId = target.templatePiece();
        if (pieceId == null)
        {
            return Optional.of(structurePos);
        }

        Optional<BlockPos> piecePos = findPieceNear(level, structurePos, holder.value(), pieceId, radiusChunks);
        return piecePos.isPresent() ? piecePos : Optional.of(structurePos);
    }

    /**
     * 在宿主结构附近扫描已生成的 StructureStart，匹配模板片中心。
     * 若最近一座村没有该片，继续在半径内找其它村。
     */
    private static Optional<BlockPos> findPieceNear(
            ServerLevel level,
            BlockPos preferAround,
            Structure host,
            ResourceLocation pieceId,
            int radiusChunks)
    {
        Optional<BlockPos> atPrefer = findPieceInStructureAt(level, preferAround, host, pieceId);
        if (atPrefer.isPresent())
        {
            return atPrefer;
        }

        int originChunkX = preferAround.getX() >> 4;
        int originChunkZ = preferAround.getZ() >> 4;
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
                    double dist = piece.distSqr(preferAround);
                    if (dist < bestDist)
                    {
                        bestDist = dist;
                        best = piece;
                    }
                }
            }
            if (best != null && r >= 8)
            {
                // 已有结果且扫过一定范围，可提前结束以省开销
                break;
            }
        }
        return Optional.ofNullable(best);
    }

    private static Optional<BlockPos> findPieceInStructureAt(
            ServerLevel level,
            BlockPos pos,
            Structure host,
            ResourceLocation pieceId)
    {
        StructureStart start = level.structureManager().getStructureWithPieceAt(pos, host);
        if (start == null || !start.isValid())
        {
            SectionPos section = SectionPos.of(pos);
            ChunkAccess chunk = level.getChunk(section.x(), section.z(), ChunkStatus.STRUCTURE_STARTS, false);
            if (chunk != null)
            {
                start = chunk.getStartForStructure(host);
            }
        }
        if (start == null || !start.isValid())
        {
            return Optional.empty();
        }
        BlockPos piece = findPieceInStart(start, pieceId);
        return Optional.ofNullable(piece);
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
        return left.isPresent() && want.equals(left.get());
    }
}
