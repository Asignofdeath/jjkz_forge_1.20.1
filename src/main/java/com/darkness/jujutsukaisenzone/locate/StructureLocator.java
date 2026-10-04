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
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
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
 * <p>
 * 未指定模板片时，会把 {@code findNearestMapStructure} 的粗坐标 refinement 到真实片中心或地表高度
 * （原版 locate 常把 Y 固定为 0，超平坦上会差几十格）。
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
        HolderSet<Structure> holderSet = resolveHolderSet(structures, target);
        if (holderSet == null)
        {
            return Optional.empty();
        }

        ResourceLocation pieceId = target.templatePiece();
        if (pieceId != null)
        {
            if (target.hostStructure() == null)
            {
                return Optional.empty();
            }
            Holder.Reference<Structure> holder = structures.getHolder(target.hostStructure()).orElse(null);
            if (holder == null)
            {
                return Optional.empty();
            }
            // 必须匹配到真实模板片；禁止回退到空村坐标
            return findNearestPieceFromOrigin(level, origin, holder.value(), pieceId, radiusChunks);
        }

        Pair<BlockPos, Holder<Structure>> nearest = level.getChunkSource().getGenerator()
                .findNearestMapStructure(level, holderSet, origin, radiusChunks, false);
        if (nearest == null)
        {
            return Optional.empty();
        }
        return Optional.of(refineStructurePos(level, nearest.getSecond().value(), nearest.getFirst(), null));
    }

    /**
     * 最近目标结构的真实包围盒（用于清村等）。找不到已生成 StructureStart 时为空。
     */
    public static Optional<BoundingBox> findNearestBounds(
            ServerLevel level,
            BlockPos origin,
            StructureLocateTarget target,
            int radiusChunks)
    {
        if (radiusChunks <= 0 || target.templatePiece() != null)
        {
            return Optional.empty();
        }
        Registry<Structure> structures = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        HolderSet<Structure> holderSet = resolveHolderSet(structures, target);
        if (holderSet == null)
        {
            return Optional.empty();
        }
        Pair<BlockPos, Holder<Structure>> nearest = level.getChunkSource().getGenerator()
                .findNearestMapStructure(level, holderSet, origin, radiusChunks, false);
        if (nearest == null)
        {
            return Optional.empty();
        }
        Structure structure = nearest.getSecond().value();
        BlockPos coarse = nearest.getFirst();
        ChunkAccess chunk = level.getChunk(
                coarse.getX() >> 4, coarse.getZ() >> 4, ChunkStatus.STRUCTURE_STARTS, true);
        if (chunk == null)
        {
            return Optional.empty();
        }
        StructureStart start = chunk.getStartForStructure(structure);
        if (start == null || !start.isValid() || start.getPieces().isEmpty())
        {
            return Optional.empty();
        }
        return Optional.of(start.getBoundingBox());
    }

    @Nullable
    private static HolderSet<Structure> resolveHolderSet(Registry<Structure> structures, StructureLocateTarget target)
    {
        if (target.structureTag() != null)
        {
            return structures.getTag(target.structureTag()).orElse(null);
        }
        if (target.hostStructure() == null)
        {
            return null;
        }
        Holder.Reference<Structure> holder = structures.getHolder(target.hostStructure()).orElse(null);
        return holder == null ? null : HolderSet.direct(holder);
    }

    /**
     * 把 locate 粗坐标校正为已生成片的包围盒中心；否则用该 XZ 的地表高度。
     */
    private static BlockPos refineStructurePos(
            ServerLevel level,
            Structure structure,
            BlockPos coarse,
            @Nullable ResourceLocation pieceId)
    {
        int chunkX = coarse.getX() >> 4;
        int chunkZ = coarse.getZ() >> 4;
        // 强制拉到 STRUCTURE_STARTS，拿到真实片
        ChunkAccess chunk = level.getChunk(chunkX, chunkZ, ChunkStatus.STRUCTURE_STARTS, true);
        if (chunk != null)
        {
            StructureStart start = chunk.getStartForStructure(structure);
            if (start != null && start.isValid())
            {
                BlockPos refined = pieceId != null
                        ? findPieceInStart(start, pieceId)
                        : findStartCenter(start);
                if (refined != null)
                {
                    return refined;
                }
            }
        }

        int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, coarse.getX(), coarse.getZ());
        return new BlockPos(coarse.getX(), surfaceY, coarse.getZ());
    }

    @Nullable
    private static BlockPos findStartCenter(StructureStart start)
    {
        if (start.getPieces().isEmpty())
        {
            return null;
        }
        BoundingBox box = start.getBoundingBox();
        // 用包围盒底部中心偏上一点，更接近入口/地面而不是屋顶
        return new BlockPos(box.getCenter().getX(), box.minY() + 1, box.getCenter().getZ());
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
            BoundingBox box = poolPiece.getBoundingBox();
            return new BlockPos(box.getCenter().getX(), box.minY() + 1, box.getCenter().getZ());
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
