package com.darkness.jujutsukaisenzone.entity.combat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * AI 术师实体的<b>独立数值来源</b>：体术强度 / 术式强度 / 咒力池 / 咒术等级。
 * <p>
 * 与玩家分开写死一份，<b>不</b>读 {@code PlayerCombatPowerData}、<b>不</b>写任何玩家存档，
 * 也不参与 SE 术式侵入 / 领域量表 / 脑损等玩家专属系统。
 * <p>
 * <b>为什么不用 {@link LivingCombatStats}</b>：那一份是「强度面板」的通用载体，用于十影式神这类
 * 一次性召唤物，只存 physical/technique 两项。AI 术师还需要咒力池（领域展开与维持要花钱）
 * 和咒术等级（展示用），所以单独建一份。
 * <p>
 * 数据存在实体 persistent data 里。⚠️ {@code getPersistentData()} <b>不</b>随实体自动序列化，
 * 宿主实体需在 {@code addAdditionalSaveData} / {@code readAdditionalSaveData} 里显式搬运
 * {@link #TAG_ROOT}（范式见 {@code GojoSatoruEntity}）。{@link #set(LivingEntity, int, int, int, int)}
 * 同时把面板同步进 {@link LivingCombatStats}，这样 {@code SkillDamageResolver} 无需改动就能给它算伤害。
 */
public final class AiTechniqueStats
{
    public static final String TAG_ROOT = "jujutsukaisenzone_ai_technique_stats";
    private static final String TAG_PHYSICAL = "physical_power";
    private static final String TAG_TECHNIQUE = "technique_power";
    private static final String TAG_ENERGY_MAX = "cursed_energy_max";
    private static final String TAG_ENERGY_CURRENT = "cursed_energy_current";
    private static final String TAG_RANK = "cursed_grade_rank";

    /** 特级（与 {@code PlayerCursedGradeData.RANK_SPECIAL} 对齐，仅作展示 / 记录用）。 */
    public static final int RANK_SPECIAL = 5;

    /**
     * 五条悟（AI）默认面板：<b>「现代最强」独一档</b>，远超玩家可达到的天花板。
     * <p>
     * 参照系：无面板兜底基线 {@code combatPowerTechniqueBaseline = 20}。玩家特级基础面板 =
     * {@code 10 + 10×5 = 60}，再靠术式情报 / SP / 修炼往上堆。五条悟直接给到：
     * <ul>
     *   <li><b>术式强度 1600</b>；</li>
     *   <li><b>体术强度 2400</b>；</li>
     *   <li><b>咒力上限 4500</b> —— 单次完整领域演出约耗 400（展开 200 + 维持 ≈ 196），
     *       余量足以连开数次并支撑后续追加的术式。</li>
     * </ul>
     */
    public static final int GOJO_PHYSICAL_POWER = 2400;
    public static final int GOJO_TECHNIQUE_POWER = 1600;
    public static final int GOJO_CURSED_ENERGY_MAX = 4500;

    private AiTechniqueStats()
    {
    }

    /** 写入一份 AI 术师面板，并同步 {@link LivingCombatStats}。 */
    public static void set(LivingEntity entity, int physicalPower, int techniquePower, int cursedEnergyMax, int rank)
    {
        if (entity == null)
        {
            return;
        }
        CompoundTag root = new CompoundTag();
        root.putInt(TAG_PHYSICAL, Math.max(0, physicalPower));
        root.putInt(TAG_TECHNIQUE, Math.max(0, techniquePower));
        root.putInt(TAG_ENERGY_MAX, Math.max(0, cursedEnergyMax));
        root.putInt(TAG_ENERGY_CURRENT, Math.max(0, cursedEnergyMax));
        root.putInt(TAG_RANK, rank);
        entity.getPersistentData().put(TAG_ROOT, root);
        LivingCombatStats.set(entity, Math.max(0, physicalPower), Math.max(0, techniquePower));
    }

    /** 五条悟（AI）审计用预设。 */
    public static void applyGojoPreset(LivingEntity entity)
    {
        set(entity, GOJO_PHYSICAL_POWER, GOJO_TECHNIQUE_POWER, GOJO_CURSED_ENERGY_MAX, RANK_SPECIAL);
        com.darkness.jujutsukaisenzone.talent.InnateTalentData.set(
                entity,
                com.darkness.jujutsukaisenzone.talent.InnateTalent.SIX_EYES,
                com.darkness.jujutsukaisenzone.talent.InnateTalentSource.INNATE);
    }

    public static boolean has(LivingEntity entity)
    {
        return entity != null && entity.getPersistentData().contains(TAG_ROOT);
    }

    public static int physicalPower(LivingEntity entity)
    {
        return read(entity, TAG_PHYSICAL, -1);
    }

    public static int techniquePower(LivingEntity entity)
    {
        return read(entity, TAG_TECHNIQUE, -1);
    }

    public static int rank(LivingEntity entity)
    {
        return read(entity, TAG_RANK, 0);
    }

    public static int cursedEnergyMax(LivingEntity entity)
    {
        return read(entity, TAG_ENERGY_MAX, 0);
    }

    public static int cursedEnergyCurrent(LivingEntity entity)
    {
        return read(entity, TAG_ENERGY_CURRENT, 0);
    }

    /** 扣咒力；不足返回 false（不扣）。 */
    public static boolean tryConsume(LivingEntity entity, int amount)
    {
        if (entity == null)
        {
            return false;
        }
        if (amount <= 0)
        {
            return true;
        }
        int billed = com.darkness.jujutsukaisenzone.talent.InnateTalentData.billCursedEnergy(entity, amount);
        if (billed <= 0)
        {
            return true;
        }
        CompoundTag root = entity.getPersistentData().getCompound(TAG_ROOT);
        if (root.isEmpty())
        {
            return false;
        }
        int current = root.getInt(TAG_ENERGY_CURRENT);
        if (current < billed)
        {
            return false;
        }
        root.putInt(TAG_ENERGY_CURRENT, current - billed);
        entity.getPersistentData().put(TAG_ROOT, root);
        return true;
    }

    /** 回满咒力（用于调试 / 重跑演出）。 */
    public static void refillCursedEnergy(LivingEntity entity)
    {
        if (entity == null)
        {
            return;
        }
        CompoundTag root = entity.getPersistentData().getCompound(TAG_ROOT);
        if (root.isEmpty())
        {
            return;
        }
        root.putInt(TAG_ENERGY_CURRENT, root.getInt(TAG_ENERGY_MAX));
        entity.getPersistentData().put(TAG_ROOT, root);
    }

    @Nullable
    private static CompoundTag rootOrNull(LivingEntity entity)
    {
        if (entity == null || !entity.getPersistentData().contains(TAG_ROOT))
        {
            return null;
        }
        return entity.getPersistentData().getCompound(TAG_ROOT);
    }

    private static int read(LivingEntity entity, String key, int fallback)
    {
        CompoundTag root = rootOrNull(entity);
        return root == null ? fallback : root.getInt(key);
    }
}
