package com.darkness.jujutsukaisenzone.player;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 「星星标记」的玩家级数据：存「目标 → 标记」的列表，外加「当前标记」的选择。
 * <p>
 * 与 {@link JudgmentTrialTargetData} 一致，写在 {@link Player#getPersistentData()} 上，
 * 因此死亡重生会清空（与项目其它玩家级战斗数据行为一致）。
 * <p>
 * 数量不设硬上限：范围由 {@code StarMarkRules.MARK_RANGE} 的距离剪除控制，超出的标记会自动解除。
 */
public final class PlayerStarMarkData
{
    private static final String TAG_ROOT = "jujutsukaisenzone_star_marks";
    private static final String TAG_SELECTED = "jujutsukaisenzone_star_mark_selected";
    private static final String KEY_KIND = "kind";
    private static final String KEY_DIMENSION = "dimension";
    private static final String KEY_POS = "pos";
    private static final String KEY_ENTITY = "entity";
    private static final String KEY_STAR = "star";

    private PlayerStarMarkData()
    {
    }

    // ======================== 当前标记 ========================

    /** 当前选中的标记；未设置时为首个（Imai）。 */
    public static StarMarkType getSelected(Player player)
    {
        return StarMarkType.cycle(0, player.getPersistentData().getInt(TAG_SELECTED));
    }

    public static void setSelected(Player player, StarMarkType type)
    {
        player.getPersistentData().putInt(TAG_SELECTED, StarMarkType.indexOf(type));
    }

    /** 切换当前标记，返回切换后的标记。 */
    public static StarMarkType cycleSelected(Player player, int delta)
    {
        StarMarkType next = StarMarkType.cycle(StarMarkType.indexOf(getSelected(player)), delta);
        setSelected(player, next);
        return next;
    }

    // ======================== 标记记录 ========================

    /** 该玩家身上的全部标记；无标记时返回空列表。 */
    public static List<StarMark> getAll(Player player)
    {
        return read(player);
    }

    /** 该玩家身上持有指定星的全部目标。 */
    public static List<StarMark> getByType(Player player, StarMarkType type)
    {
        List<StarMark> result = new ArrayList<>();
        for (StarMark mark : read(player))
        {
            if (mark.type() == type)
            {
                result.add(mark);
            }
        }
        return result;
    }

    /** 该玩家身上指定星的目标数量。 */
    public static int countOfType(Player player, StarMarkType type)
    {
        return getByType(player, type).size();
    }

    /** 查找某目标身上的标记；未被标记时返回 null。 */
    @Nullable
    public static StarMark findTarget(Player player, StarMark target)
    {
        for (StarMark mark : read(player))
        {
            if (mark.sameTargetAs(target))
            {
                return mark;
            }
        }
        return null;
    }

    /**
     * 打标：同一目标已有标记时原地覆盖（星换成新的那颗），否则追加。
     *
     * @return 是否覆盖了该目标原有的标记
     */
    public static boolean put(Player player, StarMark mark)
    {
        List<StarMark> marks = read(player);
        boolean replaced = false;
        for (int i = 0; i < marks.size(); i++)
        {
            if (marks.get(i).sameTargetAs(mark))
            {
                marks.set(i, mark);
                replaced = true;
                break;
            }
        }
        if (!replaced)
        {
            marks.add(mark);
        }
        save(player, marks);
        return replaced;
    }

    /** 按目标身份移除标记。 */
    public static void remove(Player player, StarMark target)
    {
        List<StarMark> marks = read(player);
        if (marks.removeIf(mark -> mark.sameTargetAs(target)))
        {
            save(player, marks);
        }
    }

    /** 用一份新的完整列表覆盖（批量失效清理、刷新实体坐标时使用，只写一次 NBT）。 */
    public static void replaceAll(Player player, List<StarMark> marks)
    {
        save(player, marks);
    }

    public static void clearAll(Player player)
    {
        save(player, Collections.emptyList());
    }

    // ======================== 读写 ========================

    private static List<StarMark> read(Player player)
    {
        ListTag list = player.getPersistentData().getList(TAG_ROOT, 10);
        if (list.isEmpty())
        {
            return new ArrayList<>();
        }
        List<StarMark> result = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++)
        {
            StarMark mark = parse(list.getCompound(i));
            if (mark != null)
            {
                result.add(mark);
            }
        }
        return result;
    }

    private static void save(Player player, List<StarMark> marks)
    {
        if (marks.isEmpty())
        {
            player.getPersistentData().remove(TAG_ROOT);
            return;
        }
        ListTag list = new ListTag();
        for (StarMark mark : marks)
        {
            list.add(toTag(mark));
        }
        player.getPersistentData().put(TAG_ROOT, list);
    }

    @Nullable
    private static StarMark parse(CompoundTag tag)
    {
        ResourceLocation dimensionId = ResourceLocation.tryParse(tag.getString(KEY_DIMENSION));
        if (dimensionId == null || !tag.contains(KEY_POS))
        {
            return null;
        }
        StarMark.Kind kind;
        try
        {
            kind = StarMark.Kind.valueOf(tag.getString(KEY_KIND));
        }
        catch (IllegalArgumentException e)
        {
            return null;
        }
        UUID entityId = parseUuid(tag.getString(KEY_ENTITY));
        if (kind != StarMark.Kind.BLOCK && entityId == null)
        {
            return null;
        }
        return new StarMark(
                kind,
                ResourceKey.create(Registries.DIMENSION, dimensionId),
                BlockPos.of(tag.getLong(KEY_POS)),
                entityId,
                StarMarkType.byId(tag.getString(KEY_STAR)));
    }

    private static CompoundTag toTag(StarMark mark)
    {
        CompoundTag tag = new CompoundTag();
        tag.putString(KEY_KIND, mark.kind().name());
        tag.putString(KEY_DIMENSION, mark.dimension().location().toString());
        tag.putLong(KEY_POS, mark.pos().asLong());
        tag.putString(KEY_ENTITY, mark.entityId() == null ? "" : mark.entityId().toString());
        tag.putString(KEY_STAR, mark.type().id());
        return tag;
    }

    @Nullable
    private static UUID parseUuid(String raw)
    {
        if (raw == null || raw.isEmpty())
        {
            return null;
        }
        try
        {
            return UUID.fromString(raw);
        }
        catch (IllegalArgumentException e)
        {
            return null;
        }
    }
}
