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
 * <b>持有上限 {@link #MAX_MARKS}</b>（杀生樱式）：超出上限时顶掉「最久没被打标」的那一条，
 * 而不是拒绝打标——所以新打的永远进得来，被淘汰的是你放着不管的那几颗。
 * 距离剪除（{@code StarMarkRules.MARK_RANGE}）仍然保留，两者是不同性质的兜底：
 * 距离管「跑太远」，上限管「同时铺太多」。
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
    /** 星位等级（1 ~ {@link StarMark#MAX_STACKS}）；旧存档没有这个键 → 视作 1 级。 */
    private static final String KEY_STACKS = "stacks";
    /** 最后一次打标 / 叠加的游戏刻；旧存档没有这个键 → 读作 0（等价于「很久以前打的」）。 */
    private static final String KEY_MARKED_AT = "marked";

    /**
     * 单个施术者能同时持有的标记条数上限（杀生樱式）。
     * <p>
     * 超出时顶掉「最久没被打标」的那条，因此新打的永远进得来，被淘汰的是你放着不管的那几颗。
     * 取 32：够铺满一整片战场的布置，逐条遍历 / 分组的开销也仍然可以忽略。
     * <p>
     * 注意它已大于 {@code StarMarkRules.MAX_SIMULATED}（16）：超出的那部分标记依旧存在、
     * 依旧可被定向清除 / 参与引爆判定，只是单 tick 的吸引 / 斥力力学按距离截断
     * （取离施术者最近的 16 条）后不再结算。
     */
    public static final int MAX_MARKS = 32;

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
     * 打标：同一目标已有标记时原地覆盖，否则追加。
     * <p>
     * 覆盖是「换星」与「叠加」共用的落地方式——两者都先由打标入口算出新的 {@link StarMark}
     * （换星走重新构造、叠加走 {@link StarMark#withStacks(int)}），这里只负责按目标身份替换，
     * 不区分二者。
     * <p>
     * 追加时若超出 {@link #MAX_MARKS}，按「最久没被打标」逐条顶掉；刚打上的这一条永远不会被顶掉
     * （它的打标时刻最新），所以是「打标一定成功，代价是丢掉一颗旧的」。
     *
     * @return 是否覆盖了该目标原有的标记，以及因上限被顶掉的标记（按顶掉顺序）
     */
    public static PutOutcome put(Player player, StarMark mark)
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
        List<StarMark> evicted = List.of();
        if (!replaced)
        {
            marks.add(mark);
            evicted = evictOverflow(marks, mark);
        }
        save(player, marks);
        return new PutOutcome(replaced, evicted);
    }

    /**
     * {@link #put} 的结果。
     *
     * @param replaced 是否覆盖了该目标原有的标记
     * @param evicted  因持有上限被顶掉的标记（按顶掉顺序）；未超出上限时为空
     */
    public record PutOutcome(boolean replaced, List<StarMark> evicted)
    {
    }

    /**
     * 杀生樱式持有上限：把超出 {@link #MAX_MARKS} 的部分按「最久没被打标」逐条顶掉。
     * <p>
     * 在传入的列表上<b>原地删除</b>，因此调用方拿到的是已经修剪好的列表。
     * 上限是本轮才引入的，老存档可能残留更多条目——{@code StarMarkSkillLogic#resolveAll}
     * 每次解析时也调用本方法，所以旧存档在第一次 tick 就会被修到上限内。
     *
     * @param exclude 绝不顶掉的标记（刚打上的那条）；没有则传 {@code null}
     * @return 被顶掉的标记，按顶掉顺序
     */
    public static List<StarMark> evictOverflow(List<StarMark> marks, @Nullable StarMark exclude)
    {
        if (marks.size() <= MAX_MARKS)
        {
            return List.of();
        }
        List<StarMark> evicted = new ArrayList<>();
        while (marks.size() > MAX_MARKS)
        {
            evicted.add(marks.remove(oldestIndex(marks, exclude)));
        }
        return evicted;
    }

    /**
     * 找「最久没被打标」那条的下标。
     * <p>
     * {@code exclude} 是刚打上的那条，绝不选它。调用点保证列表里至少有一条非 exclude 的候选
     * （{@code size > MAX_MARKS >= 1}），因此返回值恒为合法下标。
     */
    private static int oldestIndex(List<StarMark> marks, @Nullable StarMark exclude)
    {
        int victim = -1;
        for (int i = 0; i < marks.size(); i++)
        {
            if (marks.get(i).sameTargetAs(exclude))
            {
                continue;
            }
            if (victim < 0 || marks.get(i).lastMarkedAt() < marks.get(victim).lastMarkedAt())
            {
                victim = i;
            }
        }
        return victim;
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

    /**
     * 定向清除：移除该玩家身上所有持指定星的目标（「当前标记」这一颗星的全部标记）。
     *
     * @return 被移除的标记数量
     */
    public static int removeByType(Player player, StarMarkType type)
    {
        List<StarMark> marks = read(player);
        int before = marks.size();
        marks.removeIf(mark -> mark.type() == type);
        int removed = before - marks.size();
        if (removed > 0)
        {
            save(player, marks);
        }
        return removed;
    }

    /** 用一份新的完整列表覆盖（批量失效清理、刷新实体坐标时使用，只写一次 NBT）。 */
    public static void replaceAll(Player player, List<StarMark> marks)
    {
        save(player, marks);
    }

    /**
     * 清除该玩家持有的全部标记。
     * <p>
     * 标记是「每个施术者各自持有」的，因此这里只影响调用者自己记下的标记，
     * 别人打在同一目标上的标记不受影响。
     *
     * @return 被清除的标记数量
     */
    public static int clearAll(Player player)
    {
        int removed = read(player).size();
        if (removed > 0)
        {
            save(player, Collections.emptyList());
        }
        return removed;
    }

    /** 该玩家当前持有的标记总数。 */
    public static int count(Player player)
    {
        return read(player).size();
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
        StarMarkType type = StarMarkType.byId(tag.getString(KEY_STAR));
        /*
         * 旧存档里可能残留 unlock 键（通行证曾挂在标记上），这里不再读取，静默忽略即可。
         * 星位等级也是后加的键：缺失即 1 级，等价于「这条标记还没叠过」，因此同样不需要迁移。
         */
        int stacks = tag.contains(KEY_STACKS) ? tag.getInt(KEY_STACKS) : 1;
        /* 打标时刻同样是后加的键：缺失读作 0，等价于「很久以前打的」，会被立刻结算到 1 级 */
        long lastMarkedAt = tag.getLong(KEY_MARKED_AT);
        return new StarMark(
                kind,
                ResourceKey.create(Registries.DIMENSION, dimensionId),
                BlockPos.of(tag.getLong(KEY_POS)),
                entityId,
                type,
                stacks,
                lastMarkedAt);
    }

    private static CompoundTag toTag(StarMark mark)
    {
        CompoundTag tag = new CompoundTag();
        tag.putString(KEY_KIND, mark.kind().name());
        tag.putString(KEY_DIMENSION, mark.dimension().location().toString());
        tag.putLong(KEY_POS, mark.pos().asLong());
        tag.putString(KEY_ENTITY, mark.entityId() == null ? "" : mark.entityId().toString());
        tag.putString(KEY_STAR, mark.type().id());
        tag.putInt(KEY_STACKS, mark.stacks());
        tag.putLong(KEY_MARKED_AT, mark.lastMarkedAt());
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
