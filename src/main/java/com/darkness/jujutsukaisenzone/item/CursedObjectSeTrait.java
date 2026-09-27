package com.darkness.jujutsukaisenzone.item;

import com.darkness.jujutsukaisenzone.player.CursedEnergyTrait;
import com.darkness.jujutsukaisenzone.technique.se.PanelStat;
import com.darkness.jujutsukaisenzone.technique.se.SePenalty;
import com.darkness.jujutsukaisenzone.technique.se.SePenaltyAmountTable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 咒物实例上的 SE 随机字段：特质 + 代价。
 * <p>
 * 仅在「咒力探查」完成后写入；未探查可堆叠，已探查带唯一实例 id、不可再叠。
 */
public final class CursedObjectSeTrait
{
    public static final String TAG = "jujutsukaisenzone_ce_trait";
    public static final String TAG_PENALTY_STAT = "jujutsukaisenzone_se_penalty_stat";
    public static final String TAG_PENALTY_AMOUNT = "jujutsukaisenzone_se_penalty_amount";
    /** 已探查实例的唯一 id，保证即使特质+代价碰巧相同也不能堆叠。 */
    public static final String TAG_INSTANCE_ID = "jujutsukaisenzone_se_instance_id";

    private CursedObjectSeTrait()
    {
    }

    /**
     * 已完成咒力探查：特质与代价已写入。
     * 实例 id 可懒补（旧档缺 id 时在装载/取堆叠上限时补）。
     */
    public static boolean isProbed(ItemStack stack)
    {
        return hasTrait(stack) && hasPenalty(stack);
    }

    public static boolean hasTrait(ItemStack stack)
    {
        if (stack == null || stack.isEmpty())
        {
            return false;
        }
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(TAG);
    }

    public static boolean hasPenalty(ItemStack stack)
    {
        if (stack == null || stack.isEmpty())
        {
            return false;
        }
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(TAG_PENALTY_STAT) && tag.contains(TAG_PENALTY_AMOUNT);
    }

    public static boolean hasInstanceId(ItemStack stack)
    {
        if (stack == null || stack.isEmpty())
        {
            return false;
        }
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(TAG_INSTANCE_ID);
    }

    /**
     * 读取实例特质；未写入时返回 {@code null}（不要用 {@link CursedEnergyTrait#byId} 回落成「常」，
     * 否则「未探查」和「滚到常」分不清）。
     */
    @Nullable
    public static CursedEnergyTrait getTrait(ItemStack stack)
    {
        if (!hasTrait(stack))
        {
            return null;
        }
        return CursedEnergyTrait.find(stack.getTag().getString(TAG));
    }

    @Nullable
    public static SePenalty getPenalty(ItemStack stack)
    {
        if (!hasPenalty(stack))
        {
            return null;
        }
        CompoundTag tag = stack.getTag();
        PanelStat stat = PanelStat.find(tag.getString(TAG_PENALTY_STAT));
        if (stat == null)
        {
            return null;
        }
        return new SePenalty(stat, tag.getDouble(TAG_PENALTY_AMOUNT));
    }

    public static void setTrait(ItemStack stack, CursedEnergyTrait trait)
    {
        if (stack == null || stack.isEmpty() || trait == null)
        {
            return;
        }
        stack.getOrCreateTag().putString(TAG, trait.id());
    }

    public static void setPenalty(ItemStack stack, SePenalty penalty)
    {
        if (stack == null || stack.isEmpty() || penalty == null)
        {
            return;
        }
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(TAG_PENALTY_STAT, penalty.stat().id());
        tag.putDouble(TAG_PENALTY_AMOUNT, penalty.amount());
    }

    public static void ensureInstanceId(ItemStack stack)
    {
        if (stack == null || stack.isEmpty() || hasInstanceId(stack))
        {
            return;
        }
        stack.getOrCreateTag().putString(TAG_INSTANCE_ID, UUID.randomUUID().toString());
    }

    /** 四特质均匀随机。 */
    public static CursedEnergyTrait roll(RandomSource random)
    {
        CursedEnergyTrait[] all = CursedEnergyTrait.values();
        return all[random.nextInt(all.length)];
    }

    /**
     * 咒力探查：写入特质 + 代价 + 实例 id。已探查则不再覆盖。
     *
     * @return 是否新完成探查
     */
    public static boolean probe(ItemStack stack, CursedObjectGrade grade, RandomSource random)
    {
        if (stack == null || stack.isEmpty() || isProbed(stack))
        {
            return false;
        }
        if (!hasTrait(stack))
        {
            setTrait(stack, roll(random));
        }
        if (!hasPenalty(stack))
        {
            setPenalty(stack, SePenaltyAmountTable.roll(grade, random));
        }
        ensureInstanceId(stack);
        return isProbed(stack);
    }

    public static ItemStack withTraitAndPenalty(ItemStack stack, CursedEnergyTrait trait, SePenalty penalty)
    {
        ItemStack copy = stack.copy();
        setTrait(copy, trait);
        if (penalty != null)
        {
            setPenalty(copy, penalty);
        }
        ensureInstanceId(copy);
        return copy;
    }

    /** 调试 / 创造用：直接生成已探查实例。 */
    public static ItemStack createProbed(net.minecraft.world.item.Item item, RandomSource random)
    {
        ItemStack stack = new ItemStack(item);
        CursedObjectGrade grade = item instanceof CursedObjectItem cursed
                ? cursed.grade()
                : CursedObjectGrade.COMMON;
        probe(stack, grade, random);
        return stack;
    }

    /** @deprecated 用 {@link #createProbed}；旧名保留以免漏改。 */
    public static ItemStack createWithRandomRoll(net.minecraft.world.item.Item item, RandomSource random)
    {
        return createProbed(item, random);
    }

    public static ItemStack createWithRandomTrait(net.minecraft.world.item.Item item, RandomSource random)
    {
        return createProbed(item, random);
    }
}
