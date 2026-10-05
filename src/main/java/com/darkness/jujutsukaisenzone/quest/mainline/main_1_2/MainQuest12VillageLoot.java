package com.darkness.jujutsukaisenzone.quest.mainline.main_1_2;

import com.darkness.jujutsukaisenzone.registry.ModItems;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraftforge.registries.RegistryObject;

/**
 * 主线 1.2：在目标村箱子/桶里塞少量普通咒物，供探索拾取。
 */
public final class MainQuest12VillageLoot
{
    private static final int MIN_CONTAINERS = 2;
    private static final int MAX_CONTAINERS = 4;

    @SuppressWarnings("unchecked")
    private static final RegistryObject<Item>[] COMMON_POOL = new RegistryObject[] {
            ModItems.CURSED_OBJECT_RUSTY_POCKET_WATCH,
            ModItems.CURSED_OBJECT_BROKEN_GETA,
            ModItems.CURSED_OBJECT_TORN_FAN,
            ModItems.CURSED_OBJECT_OLD_PIPE,
            ModItems.CURSED_OBJECT_CRACKED_ABACUS,
            ModItems.CURSED_OBJECT_WARPED_MIRROR,
            ModItems.CURSED_OBJECT_DRIED_FLOWERS,
            ModItems.CURSED_OBJECT_BLOODIED_BANDAGE,
            ModItems.CURSED_OBJECT_SINGLE_BENTO,
            ModItems.CURSED_OBJECT_TORN_STUDENT_ID
    };

    private MainQuest12VillageLoot()
    {
    }

    public static void place(ServerLevel level, BoundingBox box)
    {
        forceChunks(level, box);
        List<Container> containers = collectContainers(level, box);
        if (containers.isEmpty())
        {
            return;
        }
        RandomSource random = level.getRandom();
        Collections.shuffle(containers, new java.util.Random(random.nextLong()));
        int target = Mth.clamp(
                MIN_CONTAINERS + random.nextInt(MAX_CONTAINERS - MIN_CONTAINERS + 1),
                1,
                containers.size());
        for (int i = 0; i < target; i++)
        {
            tryInsert(containers.get(i), pickCommon(random));
        }
    }

    private static void forceChunks(ServerLevel level, BoundingBox box)
    {
        int minCx = box.minX() >> 4;
        int maxCx = box.maxX() >> 4;
        int minCz = box.minZ() >> 4;
        int maxCz = box.maxZ() >> 4;
        for (int cx = minCx; cx <= maxCx; cx++)
        {
            for (int cz = minCz; cz <= maxCz; cz++)
            {
                level.getChunk(cx, cz, ChunkStatus.FULL, true);
            }
        }
    }

    private static List<Container> collectContainers(ServerLevel level, BoundingBox box)
    {
        List<Container> found = new ArrayList<>();
        int minCx = box.minX() >> 4;
        int maxCx = box.maxX() >> 4;
        int minCz = box.minZ() >> 4;
        int maxCz = box.maxZ() >> 4;
        for (int cx = minCx; cx <= maxCx; cx++)
        {
            for (int cz = minCz; cz <= maxCz; cz++)
            {
                LevelChunk chunk = level.getChunk(cx, cz);
                for (BlockEntity be : chunk.getBlockEntities().values())
                {
                    if (!(be instanceof ChestBlockEntity) && !(be instanceof BarrelBlockEntity))
                    {
                        continue;
                    }
                    BlockPos pos = be.getBlockPos();
                    if (!box.isInside(pos))
                    {
                        continue;
                    }
                    if (be instanceof Container container)
                    {
                        found.add(container);
                    }
                }
            }
        }
        return found;
    }

    private static ItemStack pickCommon(RandomSource random)
    {
        RegistryObject<Item> holder = COMMON_POOL[random.nextInt(COMMON_POOL.length)];
        return new ItemStack(holder.get());
    }

    private static void tryInsert(Container container, ItemStack stack)
    {
        for (int slot = 0; slot < container.getContainerSize(); slot++)
        {
            if (container.getItem(slot).isEmpty())
            {
                container.setItem(slot, stack);
                container.setChanged();
                return;
            }
        }
    }
}
