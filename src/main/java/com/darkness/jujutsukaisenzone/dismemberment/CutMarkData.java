package com.darkness.jujutsukaisenzone.dismemberment;

import com.darkness.jujutsukaisenzone.technique.skill.RatioBodyPart;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;

/**
 * 每肢体切痕层数的 NBT 存取。挂在目标 {@link LivingEntity#getPersistentData()} 下。
 */
final class CutMarkData
{
    private static final String ROOT = "jujutsukaisenzone_cut_marks";

    private CutMarkData()
    {
    }

    static int get(LivingEntity target, RatioBodyPart part)
    {
        if (target == null || part == null || part == RatioBodyPart.TORSO)
        {
            return 0;
        }
        CompoundTag root = rootOrNull(target);
        if (root == null)
        {
            return 0;
        }
        return Math.max(0, root.getInt(part.name()));
    }

    static void set(LivingEntity target, RatioBodyPart part, int stacks)
    {
        if (target == null || part == null || part == RatioBodyPart.TORSO)
        {
            return;
        }
        CompoundTag data = target.getPersistentData();
        CompoundTag root = data.contains(ROOT, CompoundTag.TAG_COMPOUND)
                ? data.getCompound(ROOT)
                : new CompoundTag();
        int value = Math.max(0, stacks);
        if (value <= 0)
        {
            root.remove(part.name());
        }
        else
        {
            root.putInt(part.name(), value);
        }
        if (root.isEmpty())
        {
            data.remove(ROOT);
        }
        else
        {
            data.put(ROOT, root);
        }
    }

    /** @return 是否仍有任一切痕 */
    static boolean decayAll(LivingEntity target, int amount)
    {
        if (target == null || amount <= 0)
        {
            return false;
        }
        CompoundTag root = rootOrNull(target);
        if (root == null || root.isEmpty())
        {
            return false;
        }
        boolean anyLeft = false;
        for (RatioBodyPart part : RatioBodyPart.values())
        {
            if (part == RatioBodyPart.TORSO)
            {
                continue;
            }
            if (!root.contains(part.name()))
            {
                continue;
            }
            int next = Math.max(0, root.getInt(part.name()) - amount);
            if (next <= 0)
            {
                root.remove(part.name());
            }
            else
            {
                root.putInt(part.name(), next);
                anyLeft = true;
            }
        }
        if (root.isEmpty())
        {
            target.getPersistentData().remove(ROOT);
            return false;
        }
        target.getPersistentData().put(ROOT, root);
        return anyLeft || !root.isEmpty();
    }

    static boolean hasAny(LivingEntity target)
    {
        CompoundTag root = rootOrNull(target);
        return root != null && !root.isEmpty();
    }

    private static CompoundTag rootOrNull(LivingEntity target)
    {
        CompoundTag data = target.getPersistentData();
        if (!data.contains(ROOT, CompoundTag.TAG_COMPOUND))
        {
            return null;
        }
        return data.getCompound(ROOT);
    }
}
