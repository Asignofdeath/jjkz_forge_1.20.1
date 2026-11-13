package com.darkness.jujutsukaisenzone.heaven.threat;

import com.darkness.jujutsukaisenzone.gameplay.CursedSpiritGrade;
import com.darkness.jujutsukaisenzone.technique.se.SeMechanic;
import com.darkness.jujutsukaisenzone.technique.se.SeMechanicRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 咒灵个体天道对策 SE 槽：有序队列（早→晚），长度受等级 {@link HeavenGradeOdds#slotCap} 限制；
 * {@link #offer} 入队，溢出丢掉队首。是否生效另看维度压力是否解锁。
 */
public final class CurseSpiritHeavenSe
{
    private static final String TAG_ROLLED = "jjkz_heaven_se_rolled";
    /** 有序 ListTag：下标 0 = 最早入队。 */
    private static final String TAG_ELIGIBLE = "jjkz_heaven_eligible_se";

    private CurseSpiritHeavenSe()
    {
    }

    public static boolean isRolled(LivingEntity entity)
    {
        return entity != null && entity.getPersistentData().getBoolean(TAG_ROLLED);
    }

    /** 当前队列（早→晚）；只读视图。 */
    public static List<String> eligibleIds(LivingEntity entity)
    {
        return Collections.unmodifiableList(readQueue(entity));
    }

    public static boolean isEligible(LivingEntity entity, String seId)
    {
        if (seId == null || seId.isBlank())
        {
            return false;
        }
        return readQueue(entity).contains(seId);
    }

    public static int slotCap(LivingEntity entity, CursedSpiritGrade grade)
    {
        return HeavenGradeOdds.slotCap(grade);
    }

    /**
     * 入队一条 SE。已在队列中则忽略；满槽时丢掉最早的一条再追加。
     *
     * @return 是否实际写入（含因重复而跳过为 false）
     */
    public static boolean offer(LivingEntity entity, String seId, CursedSpiritGrade grade)
    {
        if (entity == null || entity.level().isClientSide || seId == null || seId.isBlank())
        {
            return false;
        }
        int cap = HeavenGradeOdds.slotCap(grade);
        if (cap <= 0)
        {
            return false;
        }
        if (SeMechanicRegistry.find(seId).isEmpty())
        {
            return false;
        }

        List<String> queue = readQueue(entity);
        if (queue.contains(seId))
        {
            return false;
        }
        if (SeMechanicRegistry.hasNarrowInterfaceConflict(queue, seId))
        {
            return false;
        }

        queue.add(seId);
        while (queue.size() > cap)
        {
            queue.remove(0);
        }
        writeQueue(entity, queue);
        return true;
    }

    /** 按当前等级裁掉超出槽位的队首（存档兼容）。 */
    public static void enforceCap(LivingEntity entity, CursedSpiritGrade grade)
    {
        if (entity == null || entity.level().isClientSide)
        {
            return;
        }
        int cap = HeavenGradeOdds.slotCap(grade);
        List<String> queue = readQueue(entity);
        if (cap <= 0)
        {
            if (!queue.isEmpty())
            {
                writeQueue(entity, List.of());
            }
            return;
        }
        if (queue.size() <= cap)
        {
            return;
        }
        while (queue.size() > cap)
        {
            queue.remove(0);
        }
        writeQueue(entity, queue);
    }

    /**
     * 尚未掷过则按等级抽资格入队；已掷过则只强制槽位上限。
     */
    public static void ensureRolled(LivingEntity entity, CursedSpiritGrade grade)
    {
        if (entity == null || entity.level().isClientSide)
        {
            return;
        }
        if (isRolled(entity))
        {
            enforceCap(entity, grade);
            return;
        }
        rollAndStore(entity, grade, entity.getRandom());
    }

    public static void rollAndStore(LivingEntity entity, CursedSpiritGrade grade, RandomSource random)
    {
        if (entity == null || random == null)
        {
            return;
        }
        CompoundTag data = entity.getPersistentData();
        data.putBoolean(TAG_ROLLED, true);
        writeQueue(entity, List.of());

        float chance = HeavenGradeOdds.chanceAtLeastOne(grade);
        int cap = HeavenGradeOdds.slotCap(grade);
        if (chance <= 0.0F || cap <= 0 || random.nextFloat() >= chance)
        {
            return;
        }

        List<HeavenCounterDef> pool = new ArrayList<>(HeavenCounterRegistry.allList());
        if (pool.isEmpty())
        {
            return;
        }

        int want = 1 + random.nextInt(cap);
        for (int n = 0; n < want && !pool.isEmpty(); n++)
        {
            HeavenCounterDef chosen = pickWeighted(pool, random);
            if (chosen == null)
            {
                break;
            }
            pool.remove(chosen);
            SeMechanic mechanic = SeMechanicRegistry.find(chosen.seId()).orElse(null);
            if (mechanic == null)
            {
                continue;
            }
            offer(entity, chosen.seId(), grade);
        }
    }

    private static List<String> readQueue(LivingEntity entity)
    {
        if (entity == null)
        {
            return new ArrayList<>();
        }
        CompoundTag data = entity.getPersistentData();
        if (!data.contains(TAG_ELIGIBLE, Tag.TAG_LIST))
        {
            return new ArrayList<>();
        }
        ListTag list = data.getList(TAG_ELIGIBLE, Tag.TAG_STRING);
        List<String> ids = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++)
        {
            String id = list.getString(i);
            if (id != null && !id.isBlank())
            {
                ids.add(id);
            }
        }
        return ids;
    }

    private static void writeQueue(LivingEntity entity, List<String> queue)
    {
        CompoundTag data = entity.getPersistentData();
        if (queue == null || queue.isEmpty())
        {
            data.remove(TAG_ELIGIBLE);
            return;
        }
        ListTag list = new ListTag();
        for (String seId : queue)
        {
            if (seId != null && !seId.isBlank())
            {
                list.add(StringTag.valueOf(seId));
            }
        }
        if (list.isEmpty())
        {
            data.remove(TAG_ELIGIBLE);
        }
        else
        {
            data.put(TAG_ELIGIBLE, list);
        }
    }

    private static HeavenCounterDef pickWeighted(List<HeavenCounterDef> pool, RandomSource random)
    {
        int total = 0;
        for (HeavenCounterDef def : pool)
        {
            total += def.weight();
        }
        if (total <= 0)
        {
            return null;
        }
        int roll = random.nextInt(total);
        int acc = 0;
        for (HeavenCounterDef def : pool)
        {
            acc += def.weight();
            if (roll < acc)
            {
                return def;
            }
        }
        return pool.get(pool.size() - 1);
    }
}
