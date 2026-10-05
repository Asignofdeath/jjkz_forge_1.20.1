package com.darkness.jujutsukaisenzone.quest.mainline;

import com.darkness.jujutsukaisenzone.locate.StructureLocateTarget;
import com.darkness.jujutsukaisenzone.locate.StructureLocator;
import com.darkness.jujutsukaisenzone.player.PlayerQuestData;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * 主线叙事村庄：首次定位后锁定，导航 / 清村 / 1.2 均只认这一座，不再随玩家位置改「最近村」。
 */
public final class NarrativeVillage
{
    private NarrativeVillage()
    {
    }

    @Nullable
    public static BlockPos pinnedPos(ServerPlayer player)
    {
        return PlayerQuestData.getNarrativeVillagePos(player);
    }

    public static void pin(ServerPlayer player, BlockPos pos)
    {
        if (pos == null)
        {
            return;
        }
        PlayerQuestData.setNarrativeVillagePos(player, pos.immutable());
    }

    public static void clear(ServerPlayer player)
    {
        PlayerQuestData.setNarrativeVillagePos(player, null);
    }

    /** 以锁定坐标为原点取结构包围盒；找不到则用锁定点附近后备盒。 */
    public static Optional<BoundingBox> resolveBounds(ServerPlayer player)
    {
        BlockPos pinned = pinnedPos(player);
        if (pinned == null)
        {
            return Optional.empty();
        }
        ServerLevel level = player.serverLevel();
        Optional<BoundingBox> bounds = StructureLocator.findNearestBounds(
                level,
                pinned,
                StructureLocateTarget.NEAREST_VILLAGE,
                StructureLocator.DEFAULT_RADIUS_CHUNKS);
        if (bounds.isPresent())
        {
            return bounds;
        }
        return Optional.of(fallbackBox(pinned));
    }

    public static BoundingBox fallbackBox(BlockPos center)
    {
        return new BoundingBox(
                center.getX() - 64, center.getY() - 16, center.getZ() - 64,
                center.getX() + 64, center.getY() + 48, center.getZ() + 64);
    }
}
