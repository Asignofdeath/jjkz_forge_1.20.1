package com.darkness.jujutsukaisenzone.player;

import com.darkness.jujutsukaisenzone.quest.mainline.MainQuestDef;
import com.darkness.jujutsukaisenzone.quest.mainline.MainQuestDefs;
import com.darkness.jujutsukaisenzone.quest.mainline.MainQuestStatus;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * 主线任务进度读写（服务端 {@link JjkzPlayerDataStorage}；客户端 persistentData 镜像）。
 */
public final class PlayerQuestData
{
    private static final String CLIENT_TAG_TRACKED = "jujutsukaisenzone_tracked_main_quest";
    private static final String CLIENT_TAG_COMPLETED = "jujutsukaisenzone_completed_main_quests";

    private PlayerQuestData()
    {
    }

    public static boolean isBootstrapped(ServerPlayer player)
    {
        return data(player).mainQuestBootstrapped;
    }

    public static void setBootstrapped(ServerPlayer player, boolean value)
    {
        data(player).mainQuestBootstrapped = value;
        dirty(player);
    }

    public static boolean isCompleted(Player player, String questId)
    {
        if (player instanceof ServerPlayer serverPlayer)
        {
            return data(serverPlayer).completedMainQuests.contains(questId);
        }
        return readClientCompleted(player).contains(questId);
    }

    public static Set<String> completedIds(Player player)
    {
        if (player instanceof ServerPlayer serverPlayer)
        {
            return Collections.unmodifiableSet(data(serverPlayer).completedMainQuests);
        }
        return Collections.unmodifiableSet(readClientCompleted(player));
    }

    public static void markCompleted(ServerPlayer player, String questId)
    {
        if (questId == null || questId.isBlank())
        {
            return;
        }
        data(player).completedMainQuests.add(questId);
        dirty(player);
    }

    public static void clearCompleted(ServerPlayer player, String questId)
    {
        data(player).completedMainQuests.remove(questId);
        dirty(player);
    }

    public static void clearAllProgress(ServerPlayer player)
    {
        JjkzPlayerDataStorage.PlayerDataEntry entry = data(player);
        entry.completedMainQuests.clear();
        entry.trackedMainQuest = "";
        entry.mainQuestBootstrapped = false;
        entry.main11IntroDone = false;
        entry.main11TalkDone = false;
        entry.main11Kitted = false;
        entry.main11CabinPos = null;
        entry.narrativeVillagePos = null;
        entry.main12AmbushStarted = false;
        entry.main12AmbushOrigin = null;
        entry.main12DecorPlaced = false;
        entry.main12HealthLocked = false;
        dirty(player);
    }

    @Nullable
    public static String getTrackedId(Player player)
    {
        if (player instanceof ServerPlayer serverPlayer)
        {
            String id = data(serverPlayer).trackedMainQuest;
            return id == null || id.isBlank() ? null : id;
        }
        String id = player.getPersistentData().getString(CLIENT_TAG_TRACKED);
        return id.isBlank() ? null : id;
    }

    public static void setTrackedId(ServerPlayer player, @Nullable String questId)
    {
        data(player).trackedMainQuest = questId == null ? "" : questId;
        dirty(player);
    }

    public static boolean isMain11IntroDone(ServerPlayer player)
    {
        return data(player).main11IntroDone;
    }

    public static void setMain11IntroDone(ServerPlayer player, boolean value)
    {
        data(player).main11IntroDone = value;
        dirty(player);
    }

    public static boolean isMain11TalkDone(ServerPlayer player)
    {
        return data(player).main11TalkDone;
    }

    public static void setMain11TalkDone(ServerPlayer player, boolean value)
    {
        data(player).main11TalkDone = value;
        dirty(player);
    }

    public static boolean isMain11Kitted(ServerPlayer player)
    {
        return data(player).main11Kitted;
    }

    public static void setMain11Kitted(ServerPlayer player, boolean value)
    {
        data(player).main11Kitted = value;
        dirty(player);
    }

    @Nullable
    public static BlockPos getMain11CabinPos(ServerPlayer player)
    {
        return data(player).main11CabinPos;
    }

    public static void setMain11CabinPos(ServerPlayer player, @Nullable BlockPos pos)
    {
        data(player).main11CabinPos = pos == null ? null : pos.immutable();
        dirty(player);
    }

    /** 主线叙事村庄锁定坐标（首次 locate 后写入，不再随「最近村」漂移）。 */
    @Nullable
    public static BlockPos getNarrativeVillagePos(ServerPlayer player)
    {
        return data(player).narrativeVillagePos;
    }

    public static void setNarrativeVillagePos(ServerPlayer player, @Nullable BlockPos pos)
    {
        data(player).narrativeVillagePos = pos == null ? null : pos.immutable();
        dirty(player);
    }

    public static boolean isMain12AmbushStarted(ServerPlayer player)
    {
        return data(player).main12AmbushStarted;
    }

    public static void setMain12AmbushStarted(ServerPlayer player, boolean value)
    {
        data(player).main12AmbushStarted = value;
        dirty(player);
    }

    @Nullable
    public static BlockPos getMain12AmbushOrigin(ServerPlayer player)
    {
        return data(player).main12AmbushOrigin;
    }

    public static void setMain12AmbushOrigin(ServerPlayer player, @Nullable BlockPos pos)
    {
        data(player).main12AmbushOrigin = pos == null ? null : pos.immutable();
        dirty(player);
    }

    public static void clearMain12Ambush(ServerPlayer player)
    {
        JjkzPlayerDataStorage.PlayerDataEntry entry = data(player);
        entry.main12AmbushStarted = false;
        entry.main12AmbushOrigin = null;
        dirty(player);
    }

    public static boolean isMain12DecorPlaced(ServerPlayer player)
    {
        return data(player).main12DecorPlaced;
    }

    public static void setMain12DecorPlaced(ServerPlayer player, boolean value)
    {
        data(player).main12DecorPlaced = value;
        dirty(player);
    }

    /** 1.2 伏击残血锁血：完成后仍保持，留给 1.3（重置进度时清掉）。 */
    public static boolean isMain12HealthLocked(ServerPlayer player)
    {
        return data(player).main12HealthLocked;
    }

    public static void setMain12HealthLocked(ServerPlayer player, boolean value)
    {
        data(player).main12HealthLocked = value;
        dirty(player);
    }

    public static void applyClientSnapshot(Player player, @Nullable String trackedId, Collection<String> completed)
    {
        CompoundTag tag = player.getPersistentData();
        tag.putString(CLIENT_TAG_TRACKED, trackedId == null ? "" : trackedId);
        ListTag list = new ListTag();
        for (String questId : completed)
        {
            if (questId != null && !questId.isBlank())
            {
                list.add(StringTag.valueOf(questId));
            }
        }
        tag.put(CLIENT_TAG_COMPLETED, list);
    }

    public static boolean isPrerequisiteMet(Player player, MainQuestDef def)
    {
        String prereq = def.prerequisiteId();
        if (prereq == null || prereq.isBlank())
        {
            return true;
        }
        return isCompleted(player, prereq);
    }

    public static MainQuestStatus status(Player player, MainQuestDef def)
    {
        if (isCompleted(player, def.id()))
        {
            return MainQuestStatus.COMPLETED;
        }
        if (!isPrerequisiteMet(player, def))
        {
            return MainQuestStatus.LOCKED;
        }
        String tracked = getTrackedId(player);
        if (def.id().equals(tracked))
        {
            return MainQuestStatus.TRACKED;
        }
        return MainQuestStatus.AVAILABLE;
    }

    public static MainQuestStatus status(Player player, String questId)
    {
        MainQuestDef def = MainQuestDefs.byId(questId);
        if (def == null)
        {
            return MainQuestStatus.LOCKED;
        }
        return status(player, def);
    }

    private static Set<String> readClientCompleted(Player player)
    {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        ListTag list = player.getPersistentData().getList(CLIENT_TAG_COMPLETED, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++)
        {
            String questId = list.getString(i);
            if (questId != null && !questId.isBlank())
            {
                ids.add(questId);
            }
        }
        return ids;
    }

    private static JjkzPlayerDataStorage.PlayerDataEntry data(ServerPlayer player)
    {
        return JjkzPlayerDataStorage.get(player).getOrCreate(player);
    }

    private static void dirty(ServerPlayer player)
    {
        JjkzPlayerDataStorage.get(player).setDirty();
    }
}
