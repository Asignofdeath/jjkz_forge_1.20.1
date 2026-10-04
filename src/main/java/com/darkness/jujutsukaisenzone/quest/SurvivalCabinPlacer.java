package com.darkness.jujutsukaisenzone.quest;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.mojang.logging.LogUtils;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

/**
 * 运行时贴生存小屋：高度图用 {@link Heightmap.Types#MOTION_BLOCKING_NO_LEAVES}，
 * 穿过树干/树叶落到实心地面，脚垫取最低列以免埋进沙丘，模板空气写入以清内部，脚下补柱。
 */
public final class SurvivalCabinPlacer
{
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final ResourceLocation TEMPLATE_ID =
            ResourceLocation.fromNamespaceAndPath(JuJutsuKaisenZone.MODID, "jjkz_survival_cabin");

    private static final int MIN_DISTANCE = 16;
    private static final int MAX_DISTANCE = 48;
    private static final int STEP = 8;
    /** 复用已存小屋坐标时，在周围扫床脚的半径。 */
    private static final int BED_SCAN_RADIUS = 12;
    /** 床面高度（方块碰撞高 9/16）。 */
    private static final double BED_SURFACE_Y = 9.0D / 16.0D;
    /** 脚垫列高差超过此值则换点（原版村庄 beard 也避开陡坡）。 */
    private static final int MAX_RELIEF = 3;
    private static final int MAX_SUPPORT_DEPTH = 12;

    private SurvivalCabinPlacer()
    {
    }

    public record PlaceResult(BlockPos interior, Vec3 lookCenter, BoundingBox bounds)
    {
    }

    /** 床脚坐点：实体脚底位置 + 朝向（床 {@link BedBlock#FACING}）。 */
    public record BedSeat(Vec3 sitPos, float yaw)
    {
    }

    /** 工作台前站位：脚底在邻格中心，yaw 面朝工作台。 */
    public record WorkbenchStand(Vec3 standPos, float yaw)
    {
    }

    @Nullable
    public static PlaceResult placeNear(ServerLevel level, BlockPos origin)
    {
        Optional<StructureTemplate> loaded = level.getStructureManager().get(TEMPLATE_ID);
        if (loaded.isEmpty())
        {
            LOGGER.warn("Missing structure template {}", TEMPLATE_ID);
            return null;
        }
        StructureTemplate template = loaded.get();
        Vec3i size = template.getSize();
        BlockPos placeOrigin = findSurfaceOrigin(level, origin, size);
        if (placeOrigin == null)
        {
            return null;
        }
        BlockState[][] fillUnder = sampleFillBlocks(level, placeOrigin, size);
        // 只忽略结构方块：模板里的 air 必须写入，否则沙/叶会留在屋里（STRUCTURE_AND_AIR 的坑）。
        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setRotation(Rotation.NONE)
                .setMirror(Mirror.NONE)
                .setIgnoreEntities(false)
                .addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);
        RandomSource random = level.getRandom();
        template.placeInWorld(level, placeOrigin, placeOrigin, settings, random, Block.UPDATE_ALL);
        BoundingBox box = template.getBoundingBox(settings, placeOrigin);
        supportUnderside(level, box, placeOrigin.getY(), fillUnder);
        BlockPos center = box.getCenter();
        Vec3 lookCenter = new Vec3(center.getX() + 0.5D, center.getY() + 0.5D, center.getZ() + 0.5D);
        return new PlaceResult(interiorPos(level, box), lookCenter, box);
    }

    /** 在小屋 AABB 内找第一张床的脚部。 */
    @Nullable
    public static BedSeat findBedSeat(ServerLevel level, BoundingBox box)
    {
        for (int x = box.minX(); x <= box.maxX(); x++)
        {
            for (int y = box.minY(); y <= box.maxY(); y++)
            {
                for (int z = box.minZ(); z <= box.maxZ(); z++)
                {
                    BedSeat seat = bedSeatAt(level, new BlockPos(x, y, z));
                    if (seat != null)
                    {
                        return seat;
                    }
                }
            }
        }
        return null;
    }

    /** 以室内点为中心扫半径内床脚（已放置过小屋、只有 interior 坐标时用）。 */
    @Nullable
    public static BedSeat findBedSeatNear(ServerLevel level, BlockPos near)
    {
        return findBedSeat(level, scanBoxAround(near));
    }

    /** 小屋 AABB 内找工作台，站在朝向室内的空邻格，面朝台面。 */
    @Nullable
    public static WorkbenchStand findWorkbenchStand(ServerLevel level, BoundingBox box, @Nullable BlockPos preferNear)
    {
        BlockPos table = null;
        for (int x = box.minX(); x <= box.maxX(); x++)
        {
            for (int y = box.minY(); y <= box.maxY(); y++)
            {
                for (int z = box.minZ(); z <= box.maxZ(); z++)
                {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (level.getBlockState(pos).is(Blocks.CRAFTING_TABLE))
                    {
                        table = pos;
                        break;
                    }
                }
                if (table != null)
                {
                    break;
                }
            }
            if (table != null)
            {
                break;
            }
        }
        if (table == null)
        {
            return null;
        }
        BlockPos bestStand = null;
        Direction bestFace = null;
        int bestDist = Integer.MAX_VALUE;
        for (Direction dir : Direction.Plane.HORIZONTAL)
        {
            BlockPos stand = table.relative(dir);
            if (!canStandAt(level, stand))
            {
                continue;
            }
            int dist = preferNear == null
                    ? 0
                    : Math.abs(stand.getX() - preferNear.getX()) + Math.abs(stand.getZ() - preferNear.getZ());
            if (bestStand == null || dist < bestDist)
            {
                bestStand = stand;
                bestFace = dir.getOpposite();
                bestDist = dist;
            }
        }
        if (bestStand == null || bestFace == null)
        {
            return null;
        }
        Vec3 standPos = new Vec3(bestStand.getX() + 0.5D, bestStand.getY(), bestStand.getZ() + 0.5D);
        return new WorkbenchStand(standPos, bestFace.toYRot());
    }

    @Nullable
    public static WorkbenchStand findWorkbenchStandNear(ServerLevel level, BlockPos near)
    {
        return findWorkbenchStand(level, scanBoxAround(near), near);
    }

    private static BoundingBox scanBoxAround(BlockPos near)
    {
        return new BoundingBox(
                near.getX() - BED_SCAN_RADIUS,
                near.getY() - 4,
                near.getZ() - BED_SCAN_RADIUS,
                near.getX() + BED_SCAN_RADIUS,
                near.getY() + 8,
                near.getZ() + BED_SCAN_RADIUS);
    }

    private static boolean canStandAt(ServerLevel level, BlockPos stand)
    {
        BlockState here = level.getBlockState(stand);
        BlockState below = level.getBlockState(stand.below());
        return here.isAir() && !below.isAir();
    }

    @Nullable
    private static BedSeat bedSeatAt(ServerLevel level, BlockPos pos)
    {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BedBlock) || state.getValue(BedBlock.PART) != BedPart.FOOT)
        {
            return null;
        }
        Direction facing = state.getValue(BedBlock.FACING);
        Vec3 sitPos = new Vec3(pos.getX() + 0.5D, pos.getY() + BED_SURFACE_Y, pos.getZ() + 0.5D);
        return new BedSeat(sitPos, facing.toYRot());
    }

    @Nullable
    private static BlockPos findSurfaceOrigin(ServerLevel level, BlockPos playerPos, Vec3i size)
    {
        int px = playerPos.getX();
        int pz = playerPos.getZ();
        BlockPos fallback = null;
        int fallbackRelief = Integer.MAX_VALUE;
        for (int dist = MIN_DISTANCE; dist <= MAX_DISTANCE; dist += STEP)
        {
            for (int dx = -dist; dx <= dist; dx += STEP)
            {
                for (int dz = -dist; dz <= dist; dz += STEP)
                {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != dist)
                    {
                        continue;
                    }
                    int relief = footprintRelief(level, px + dx, pz + dz, size);
                    if (relief < 0)
                    {
                        continue;
                    }
                    int originY = minGroundY(level, px + dx, pz + dz, size) + 1;
                    BlockPos candidate = new BlockPos(px + dx, originY, pz + dz);
                    if (relief <= MAX_RELIEF)
                    {
                        return candidate;
                    }
                    if (relief < fallbackRelief)
                    {
                        fallbackRelief = relief;
                        fallback = candidate;
                    }
                }
            }
        }
        return fallback;
    }

    /** 脚垫最高实心块 Y 减最低；水面/无地面返回 -1。 */
    private static int footprintRelief(ServerLevel level, int originX, int originZ, Vec3i size)
    {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int x = originX; x < originX + size.getX(); x++)
        {
            for (int z = originZ; z < originZ + size.getZ(); z++)
            {
                int groundY = solidGroundY(level, x, z);
                if (groundY < level.getMinBuildHeight())
                {
                    return -1;
                }
                if (isWaterColumn(level, x, groundY, z))
                {
                    return -1;
                }
                min = Math.min(min, groundY);
                max = Math.max(max, groundY);
            }
        }
        return max - min;
    }

    private static int minGroundY(ServerLevel level, int originX, int originZ, Vec3i size)
    {
        int min = Integer.MAX_VALUE;
        for (int x = originX; x < originX + size.getX(); x++)
        {
            for (int z = originZ; z < originZ + size.getZ(); z++)
            {
                min = Math.min(min, solidGroundY(level, x, z));
            }
        }
        return min;
    }

    /**
     * 无叶运动遮挡高度图，再向下穿过树干/叶/可替换物，落到可站实心块。
     * 返回该实心块的 Y（不是上面的空气）。
     */
    private static int solidGroundY(ServerLevel level, int x, int z)
    {
        int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(x, top, z);
        int minY = level.getMinBuildHeight() + 1;
        while (cursor.getY() >= minY && !isStableGround(level.getBlockState(cursor)))
        {
            cursor.move(Direction.DOWN);
        }
        return cursor.getY();
    }

    private static boolean isStableGround(BlockState state)
    {
        if (state.isAir() || !state.getFluidState().isEmpty())
        {
            return false;
        }
        if (state.getBlock() instanceof LeavesBlock
                || state.getBlock() instanceof BushBlock
                || state.getBlock() instanceof SnowLayerBlock)
        {
            return false;
        }
        if (state.is(BlockTags.LEAVES)
                || state.is(BlockTags.LOGS)
                || state.is(BlockTags.SAPLINGS)
                || state.canBeReplaced())
        {
            return false;
        }
        return state.blocksMotion();
    }

    private static boolean isWaterColumn(ServerLevel level, int x, int groundY, int z)
    {
        BlockPos ground = new BlockPos(x, groundY, z);
        return level.getFluidState(ground).is(FluidTags.WATER)
                || level.getFluidState(ground.above()).is(FluidTags.WATER);
    }

    private static BlockState[][] sampleFillBlocks(ServerLevel level, BlockPos origin, Vec3i size)
    {
        BlockState[][] fill = new BlockState[size.getX()][size.getZ()];
        for (int dx = 0; dx < size.getX(); dx++)
        {
            for (int dz = 0; dz < size.getZ(); dz++)
            {
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                int gy = solidGroundY(level, x, z);
                BlockState sample = level.getBlockState(new BlockPos(x, gy, z));
                if (!isStableGround(sample))
                {
                    sample = Blocks.DIRT.defaultBlockState();
                }
                fill[dx][dz] = sample;
            }
        }
        return fill;
    }

    /** 脚下补柱：斜坡低侧悬空时用放置前采到的地表方块填到地面（beard 的运行时简化）。 */
    private static void supportUnderside(ServerLevel level, BoundingBox box, int floorY, BlockState[][] fillUnder)
    {
        int originX = box.minX();
        int originZ = box.minZ();
        for (int x = box.minX(); x <= box.maxX(); x++)
        {
            for (int z = box.minZ(); z <= box.maxZ(); z++)
            {
                BlockPos floor = new BlockPos(x, floorY, z);
                if (level.getBlockState(floor).isAir())
                {
                    continue;
                }
                int dx = x - originX;
                int dz = z - originZ;
                BlockState fill = Blocks.DIRT.defaultBlockState();
                if (dx >= 0 && dz >= 0 && dx < fillUnder.length && dz < fillUnder[dx].length
                        && fillUnder[dx][dz] != null)
                {
                    fill = fillUnder[dx][dz];
                }
                for (int y = floorY - 1; y >= floorY - MAX_SUPPORT_DEPTH; y--)
                {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState existing = level.getBlockState(pos);
                    if (isStableGround(existing))
                    {
                        break;
                    }
                    level.setBlock(pos, fill, Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    private static BlockPos interiorPos(ServerLevel level, BoundingBox box)
    {
        BlockPos fallback = new BlockPos(box.getCenter().getX(), box.minY() + 1, box.getCenter().getZ());
        BlockPos best = null;
        int bestDist = Integer.MAX_VALUE;
        int cx = box.getCenter().getX();
        int cz = box.getCenter().getZ();
        for (int x = box.minX(); x <= box.maxX(); x++)
        {
            for (int z = box.minZ(); z <= box.maxZ(); z++)
            {
                for (int y = box.minY(); y <= box.maxY() - 1; y++)
                {
                    BlockPos feet = new BlockPos(x, y, z);
                    if (!level.getBlockState(feet).isAir() || !level.getBlockState(feet.above()).isAir())
                    {
                        continue;
                    }
                    if (level.getBlockState(feet.below()).isAir())
                    {
                        continue;
                    }
                    int dist = Math.abs(x - cx) + Math.abs(z - cz);
                    if (dist < bestDist)
                    {
                        bestDist = dist;
                        best = feet;
                    }
                    break;
                }
            }
        }
        return best != null ? best : fallback;
    }
}
