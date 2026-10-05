package com.darkness.jujutsukaisenzone.quest.mainline.main_1_1;

import com.darkness.jujutsukaisenzone.locate.PlayerLocateGuidance;
import com.darkness.jujutsukaisenzone.locate.StructureLocateTarget;
import com.darkness.jujutsukaisenzone.player.PlayerQuestData;
import com.darkness.jujutsukaisenzone.quest.mainline.MainQuestDefs;
import com.darkness.jujutsukaisenzone.quest.mainline.NarrativeVillage;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

/**
 * 主线 1.1：清空<strong>已锁定叙事村</strong>内生物（叙事空村）。
 * 导航期间定期再清，避免区块加载后又刷出村民/动物。
 */
public final class MainQuest11VillageClear
{
    private MainQuest11VillageClear()
    {
    }

    public static void tick(ServerPlayer player)
    {
        if (!MainQuestDefs.MAIN_1_1_ID.equals(PlayerQuestData.getTrackedId(player)))
        {
            return;
        }
        if (PlayerQuestData.isCompleted(player, MainQuestDefs.MAIN_1_1_ID))
        {
            return;
        }
        if (MainQuest11Intro.isRunning(player))
        {
            return;
        }
        PlayerLocateGuidance.Session session = PlayerLocateGuidance.get(player);
        if (session == null || !StructureLocateTarget.NEAREST_VILLAGE.id().equals(session.targetId()))
        {
            return;
        }
        clearNarrativeVillage(player);
    }

    public static void clearNarrativeVillage(ServerPlayer player)
    {
        ServerLevel level = player.serverLevel();
        Optional<BoundingBox> bounds = NarrativeVillage.resolveBounds(player);
        if (bounds.isEmpty())
        {
            return;
        }
        clearLivingInBox(level, bounds.get());
    }

    /** @deprecated 用 {@link #clearNarrativeVillage} */
    @Deprecated
    public static void clearNearestVillage(ServerPlayer player)
    {
        clearNarrativeVillage(player);
    }

    private static void clearLivingInBox(ServerLevel level, BoundingBox box)
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
        AABB aabb = new AABB(
                box.minX(),
                box.minY(),
                box.minZ(),
                box.maxX() + 1.0D,
                box.maxY() + 1.0D,
                box.maxZ() + 1.0D);
        List<LivingEntity> living = level.getEntitiesOfClass(
                LivingEntity.class, aabb, entity -> !(entity instanceof Player));
        for (LivingEntity entity : living)
        {
            entity.discard();
        }
    }
}
