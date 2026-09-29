package com.darkness.jujutsukaisenzone.player;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 一条「星星标记」记录：被标记的目标 + 打在上面的那颗星 + 该星的<b>星位等级</b>。
 * <p>
 * 「目标唯一」：方块按维度 + 坐标判定，实体按维度 + uuid 判定（见 {@link #sameTargetAs(StarMark)}）。
 * 同一个目标身上至多一个标记；而同一颗星可以打给任意多个不同目标。
 * <p>
 * <b>叠加与换星</b>：对同一目标重复打<b>同一颗星</b>即为叠加，{@link #stacks()} 加 1；
 * 打<b>另一颗星</b>则是更换，星位等级回到 1（换星走「重新构造一条标记」，因此
 * {@link #lastMarkedAt()} 也一并刷新）。叠满 {@link #MAX_STACKS} 后再次叠加会引爆，见
 * {@code StarMarkDetonation}——引爆会消耗掉全部层数，该目标回到「未被标记」，想再炸一次就必须重新叠满。
 * <p>
 * <b>星位等级会衰减</b>：距 {@link #lastMarkedAt()} 每过 {@code StarMarkRules.STACK_DECAY_TICKS} 掉 1 级，
 * 最低停在 1 级——衰减只吃掉「叠上去的部分」，标记本身不会因为时间而消失。因此「叠满 5 级」是
 * 一个有时限的成就，而不是一劳永逸的状态。
 * <p>
 * <b>跨序「经过」的进度不在本记录里</b>：通行证属于星域实例，存在玩家数据上，
 * 每次展开星域即重置，见 {@code StarMarkRules}。因此标记本身只描述「谁被打了哪颗星、叠了几级」。
 * <p>
 * <b>喷漆附着</b>：{@link #face()} / {@link #u()} / {@link #v()} 描述贴花贴在哪一面、面上哪一点。
 * 方块面是世界朝向；实体面是相对实体 yaw=0 的局部朝向（随实体转身一起转）。
 *
 * @param pos          {@link Kind#BLOCK} 为被标记的方块坐标；{@link Kind#ENTITY}
 *                     为实体最后一次被解析到的坐标（用于区分「区块未加载」与「实体已消失」）
 * @param stacks       星位等级，恒在 1 ~ {@link #MAX_STACKS} 之间（由 compact constructor 夹取，
 *                     所以脏存档不会越界；「已叠满」这个状态由 {@code == MAX_STACKS} 表达）
 * @param lastMarkedAt 最后一次被施术者打标 / 叠加的那一游戏刻。两个用途：星位等级的衰减基准，
 *                     以及超出持有上限时「顶掉最旧」的排序依据（见 {@code PlayerStarMarkData#MAX_MARKS}）。
 *                     旧存档没有这个键 → 读作 0（等价于「很久以前打的」），不需要迁移。
 * @param face         贴花所在面（{@link Direction#get3DDataValue()}）；旧存档缺失时按 {@link #defaultFace(Kind)}
 * @param u            面上横向局部坐标，0~1
 * @param v            面上纵向局部坐标，0~1
 */
public record StarMark(Kind kind, ResourceKey<Level> dimension, BlockPos pos,
                       @Nullable UUID entityId, StarMarkType type, int stacks, long lastMarkedAt,
                       Direction face, float u, float v)
{
    /** 星位等级上限：叠满之后再叠一次即引爆。 */
    public static final int MAX_STACKS = 5;

    public StarMark
    {
        stacks = Math.max(1, Math.min(MAX_STACKS, stacks));
        face = face == null ? defaultFace(kind) : face;
        u = clamp01(u);
        v = clamp01(v);
    }

    public enum Kind
    {
        /** 方块 */
        BLOCK,
        /** 实体（生物 / 玩家等；不含掉落物） */
        ENTITY
    }

    /** 旧存档 / 未指定附着点时的默认面。 */
    public static Direction defaultFace(Kind kind)
    {
        return kind == Kind.BLOCK ? Direction.UP : Direction.SOUTH;
    }

    private static float clamp01(float value)
    {
        if (value < 0.0F)
        {
            return 0.0F;
        }
        if (value > 1.0F)
        {
            return 1.0F;
        }
        return value;
    }

    /** 目标类型的中文名，用于提示。 */
    public String kindName()
    {
        return switch (kind)
        {
            case BLOCK -> "方块";
            case ENTITY -> "实体";
        };
    }

    /** 是否为「会动」的目标：方块是静态锚点，不参与力学移动。 */
    public boolean mobile()
    {
        return kind != Kind.BLOCK;
    }

    /** 是否应绘制喷漆贴花。 */
    public boolean hasDecal()
    {
        return true;
    }

    /** 目标身份是否相同：方块看维度 + 坐标，实体看维度 + uuid。 */
    public boolean sameTargetAs(@Nullable StarMark other)
    {
        if (other == null || kind != other.kind || !dimension.equals(other.dimension))
        {
            return false;
        }
        if (kind == Kind.BLOCK)
        {
            return pos.equals(other.pos);
        }
        return entityId != null && entityId.equals(other.entityId);
    }

    /**
     * 视觉指纹：忽略实体坐标刷新，用于判断是否需要把贴花同步给客户端。
     */
    public String visualKey()
    {
        String target = kind == Kind.BLOCK
                ? pos.asLong() + ""
                : (entityId == null ? "?" : entityId.toString());
        return kind.name() + '|' + dimension.location() + '|' + target + '|'
                + type.id() + '|' + stacks + '|' + face.get3DDataValue()
                + '|' + Float.floatToIntBits(u) + '|' + Float.floatToIntBits(v);
    }

    /** 刷新实体的最后已知坐标（不改变目标身份、星位等级、打标时刻与附着点）。 */
    public StarMark withPos(BlockPos newPos)
    {
        return new StarMark(kind, dimension, newPos, entityId, type, stacks, lastMarkedAt, face, u, v);
    }

    /**
     * 覆盖该目标身上的星：换星即星位等级归 1（新的一颗星从头叠起）。
     * <p>
     * 打标时刻与附着点<b>原样保留</b>——这个变换只改「星」这一个轴。玩家换星实际走的是
     * {@code StarMarkSkillLogic#markOfEntity} 重新构造一条标记，那条路径会带上新的时刻与命中面。
     */
    public StarMark withType(StarMarkType newType)
    {
        return new StarMark(kind, dimension, pos, entityId, newType, 1, lastMarkedAt, face, u, v);
    }

    /**
     * 改为指定的星位等级（叠加 +1、衰减回落、引爆后清零时使用）。
     * <p>
     * 不会越过 {@link #MAX_STACKS}——「叠满」这一档只表示「可以引爆了」，
     * 引爆本身不做成第 6 级，是否引爆由调用方在叠加之前判断。
     * 打标时刻原样保留：衰减结算与叠加各自负责自己那一段（见 {@code StarMarkRules#decayedStacks}）。
     */
    public StarMark withStacks(int newStacks)
    {
        return new StarMark(kind, dimension, pos, entityId, type, newStacks, lastMarkedAt, face, u, v);
    }

    /**
     * 把星位等级直接钉到满级，并<b>把打标时刻挪到当下</b>。
     * <p>
     * 与 {@link #withStacks(int)} 的唯一区别就在时刻上，而这正是它存在的原因：
     * {@code StarMarkRules#decayedStacks} 只认「距 {@link #lastMarkedAt()} 过了多久」，
     * 只改等级、不刷新时刻的话，满级会在同一个 tick 被衰减结算按回原样，一秒都站不住。
     * <p>
     * 供「爱与幽会」领域的引爆传播使用：域内炸开时，半径内的标记被直接拉满
     * （见 {@code StarMarkDetonation#spreadMark}），使被波及的目标当场成为下一个可燃点。
     */
    public StarMark maxed(long now)
    {
        return new StarMark(kind, dimension, pos, entityId, type, MAX_STACKS, now, face, u, v);
    }

    /** 目标描述，用于动作栏提示。 */
    public String targetLabel()
    {
        return switch (kind)
        {
            case BLOCK -> "方块 (" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")";
            case ENTITY -> "实体";
        };
    }
}
