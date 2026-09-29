package com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability.mahoraga;

import com.darkness.jujutsukaisenzone.technique.NarrativeSkillFamilies;
import com.darkness.jujutsukaisenzone.technique.TechniqueDamageSources;
import com.darkness.jujutsukaisenzone.technique.skillset.LimitlessTechniqueSkillSet;
import com.darkness.jujutsukaisenzone.technique.skillset.RatioTechniqueSkillSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.FallingBlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * 魔虚罗适应：按叙事族叠层；满层后免疫该族伤害与机制。
 * <p>
 * 满层数 = 族表 {@link NarrativeSkillFamilies#narrativePower}（叙事强度）。
 * 每次成功叠层后，该族进入 {@link #STACK_COOLDOWN_TICKS}（15 秒）内置 CD。
 * <p>
 * 机制汇入口请调 {@link #tryEngage}：已满返回 true（调用方应跳过效果）；未满返回 false（效果照常，可能叠层）。
 */
public final class MahoragaAdaptation
{
    public static final String TAG_ADAPT = "jujutsukaisenzone_mahoraga_adapt";
    public static final String KEY_STACKS = "stacks";
    public static final String KEY_MAX = "max";
    public static final String KEY_CD_UNTIL = "cdUntil";

    /** 每次叠层后的内置冷却（15 秒）。 */
    public static final int STACK_COOLDOWN_TICKS = 20 * 15;

    private MahoragaAdaptation()
    {
    }

    /**
     * 伤害侧：若已满适应则免疫（取消伤害）；否则可能叠一层后仍受伤。
     *
     * @return true 表示本次伤害应被完全免疫
     */
    public static boolean tryAdaptAndMaybeImmune(Mob mahoraga, DamageSource source, float amount)
    {
        if (!MahoragaSupport.isMahoraga(mahoraga) || amount <= 0.0F)
        {
            return false;
        }
        return tryEngage(mahoraga, adaptKey(source));
    }

    /**
     * 机制 / 伤害共用：对目标施加某叙事族效果前调用。
     *
     * @param target            可能是魔虚罗；非魔虚罗恒返回 false
     * @param memberOrFamilyId  skillId / 族 id / 粗键
     * @return true = 已完全适应，调用方应<strong>跳过</strong>该效果；false = 未免疫（可能刚叠层）
     */
    public static boolean tryEngage(@Nullable LivingEntity target, @Nullable String memberOrFamilyId)
    {
        if (!(target instanceof Mob mob) || !MahoragaSupport.isMahoraga(mob))
        {
            return false;
        }
        String family = NarrativeSkillFamilies.resolveFamilyId(memberOrFamilyId);
        if (family == null || family.isBlank())
        {
            return false;
        }

        CompoundTag root = mob.getPersistentData().getCompound(TAG_ADAPT);
        CompoundTag entry = entryOf(root, family);
        int max = lockedMax(entry, family);
        int stacks = entry.getInt(KEY_STACKS);
        if (stacks >= max)
        {
            return true;
        }

        long now = mob.level().getGameTime();
        if (now < entry.getLong(KEY_CD_UNTIL))
        {
            return false;
        }

        entry.putInt(KEY_STACKS, stacks + 1);
        entry.putInt(KEY_MAX, max);
        entry.putLong(KEY_CD_UNTIL, now + STACK_COOLDOWN_TICKS);
        root.put(family, entry);
        mob.getPersistentData().put(TAG_ADAPT, root);
        return false;
    }

    /** 仅查询：是否已对该叙事族完全适应（不叠层、不刷新 CD）。 */
    public static boolean isImmune(@Nullable LivingEntity target, @Nullable String memberOrFamilyId)
    {
        if (!(target instanceof Mob mob) || !MahoragaSupport.isMahoraga(mob))
        {
            return false;
        }
        String family = NarrativeSkillFamilies.resolveFamilyId(memberOrFamilyId);
        if (family == null || family.isBlank())
        {
            return false;
        }
        CompoundTag root = mob.getPersistentData().getCompound(TAG_ADAPT);
        CompoundTag entry = entryOf(root, family);
        return entry.getInt(KEY_STACKS) >= lockedMax(entry, family);
    }

    public static int getStacks(Mob mahoraga, String key)
    {
        String family = NarrativeSkillFamilies.resolveFamilyId(key);
        if (family == null)
        {
            return 0;
        }
        return entryOf(mahoraga.getPersistentData().getCompound(TAG_ADAPT), family).getInt(KEY_STACKS);
    }

    public static int getMaxStacks(Mob mahoraga, String key)
    {
        String family = NarrativeSkillFamilies.resolveFamilyId(key);
        if (family == null)
        {
            return NarrativeSkillFamilies.DEFAULT_NARRATIVE_POWER;
        }
        return lockedMax(entryOf(mahoraga.getPersistentData().getCompound(TAG_ADAPT), family), family);
    }

    private static CompoundTag entryOf(CompoundTag root, String family)
    {
        if (root.contains(family, Tag.TAG_COMPOUND))
        {
            return root.getCompound(family);
        }
        // 兼容旧存档：扁平 int 层数
        CompoundTag entry = new CompoundTag();
        if (root.contains(family, Tag.TAG_INT))
        {
            entry.putInt(KEY_STACKS, root.getInt(family));
        }
        return entry;
    }

    private static int lockedMax(CompoundTag entry, String family)
    {
        if (entry.contains(KEY_MAX))
        {
            return Math.max(1, entry.getInt(KEY_MAX));
        }
        return Math.max(1, NarrativeSkillFamilies.narrativePower(family));
    }

    private static String adaptKey(DamageSource source)
    {
        String family = TechniqueDamageSources.currentNarrativeFamilyId();
        if (family != null && !family.isBlank())
        {
            return family;
        }
        if (source.getDirectEntity() instanceof FallingBlockEntity falling)
        {
            if (falling.getPersistentData().getBoolean(TechniqueDamageSources.TAG_RED_DEBRIS))
            {
                return LimitlessTechniqueSkillSet.SKILL_RED;
            }
            if (falling.getPersistentData().getBoolean(TechniqueDamageSources.TAG_RATIO_DEBRIS))
            {
                return RatioTechniqueSkillSet.SKILL_COLLAPSE;
            }
        }
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE))
        {
            return "fire";
        }
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION))
        {
            return "explosion";
        }
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE))
        {
            return "projectile";
        }
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_FALL))
        {
            return "fall";
        }
        if (source.is(net.minecraft.tags.DamageTypeTags.WITCH_RESISTANT_TO))
        {
            return "magic";
        }
        LivingEntity attacker = source.getEntity() instanceof LivingEntity living ? living : null;
        if (attacker != null)
        {
            return "melee";
        }
        return "generic";
    }
}
