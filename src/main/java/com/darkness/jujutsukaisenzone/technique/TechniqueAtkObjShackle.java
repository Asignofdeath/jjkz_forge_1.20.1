package com.darkness.jujutsukaisenzone.technique;

import com.darkness.jujutsukaisenzone.player.JjkzPlayerDataStorage;
import com.darkness.jujutsukaisenzone.technique.commonskill.CommonTechniqueSkillRegistry;
import com.darkness.jujutsukaisenzone.technique.skillset.IInnateTechniqueSkillSet;
import com.darkness.jujutsukaisenzone.technique.skillset.SkillDamageType;
import com.darkness.jujutsukaisenzone.technique.skillset.TechniqueSkill;
import com.darkness.jujutsukaisenzone.technique.skillset.TechniqueSkillSetRegistry;
import com.darkness.jujutsukaisenzone.technique.skillset.TechniqueSpecialMove;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 术式伤害对象限定（白名单 / 黑名单）。
 * 仅作用于术式技能表中的术式伤害；限定生效时该次乘区最终伤害 × {@link #DAMAGE_FACTOR}。
 */
public final class TechniqueAtkObjShackle
{
    public static final String MODE_WL = "wl";
    public static final String MODE_BL = "bl";
    public static final float DAMAGE_FACTOR = 0.8F;

    public static final String TAG_MODE = "jujutsukaisenzone_technique_atk_obj_mode";
    public static final String TAG_IDS = "jujutsukaisenzone_technique_atk_obj_ids";

    private TechniqueAtkObjShackle()
    {
    }

    public static boolean isActive(@Nullable LivingEntity caster)
    {
        if (caster == null)
        {
            return false;
        }
        String mode = getMode(caster);
        return MODE_WL.equals(mode) || MODE_BL.equals(mode);
    }

    public static String getMode(@Nullable LivingEntity caster)
    {
        if (caster instanceof ServerPlayer player)
        {
            String mode = JjkzPlayerDataStorage.get(player).getOrCreate(player).techniqueAtkObjMode;
            return mode == null ? "" : mode;
        }
        if (caster == null)
        {
            return "";
        }
        return caster.getPersistentData().getString(TAG_MODE);
    }

    public static Set<UUID> getIds(@Nullable LivingEntity caster)
    {
        if (caster instanceof ServerPlayer player)
        {
            return new LinkedHashSet<>(JjkzPlayerDataStorage.get(player).getOrCreate(player).techniqueAtkObjIds);
        }
        LinkedHashSet<UUID> ids = new LinkedHashSet<>();
        if (caster == null)
        {
            return ids;
        }
        ListTag list = caster.getPersistentData().getList(TAG_IDS, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++)
        {
            try
            {
                ids.add(UUID.fromString(list.getString(i)));
            }
            catch (IllegalArgumentException ignored)
            {
            }
        }
        return ids;
    }

    public static void replace(LivingEntity caster, String mode, Collection<UUID> ids)
    {
        if (caster == null || caster.level().isClientSide)
        {
            return;
        }
        String normalized = MODE_WL.equals(mode) ? MODE_WL : MODE_BL;
        LinkedHashSet<UUID> copy = new LinkedHashSet<>();
        if (ids != null)
        {
            for (UUID id : ids)
            {
                if (id != null)
                {
                    copy.add(id);
                }
            }
        }
        write(caster, normalized, copy);
    }

    public static void clear(LivingEntity caster)
    {
        if (caster == null || caster.level().isClientSide)
        {
            return;
        }
        write(caster, "", new LinkedHashSet<>());
    }

    /**
     * 术式技能表中的术式伤害、且目标不在允许集合内时，跳过结算。
     */
    public static boolean shouldSkipHit(
            @Nullable LivingEntity caster,
            @Nullable LivingEntity target,
            @Nullable SkillDamageType damageType,
            @Nullable String skillId)
    {
        if (!isTechniqueSkillTableHit(damageType, skillId) || !isActive(caster) || target == null)
        {
            return false;
        }
        boolean listed = getIds(caster).contains(target.getUUID());
        String mode = getMode(caster);
        if (MODE_WL.equals(mode))
        {
            return !listed;
        }
        return listed;
    }

    public static boolean shouldReduceDamage(
            @Nullable LivingEntity caster,
            @Nullable SkillDamageType damageType,
            @Nullable String skillId)
    {
        return isTechniqueSkillTableHit(damageType, skillId) && isActive(caster);
    }

    public static boolean isTechniqueSkillTableHit(
            @Nullable SkillDamageType damageType,
            @Nullable String skillId)
    {
        if (damageType != SkillDamageType.TECHNIQUE || skillId == null || skillId.isBlank())
        {
            return false;
        }
        if (isTechniqueSkillTableId(skillId))
        {
            return true;
        }
        String family = NarrativeSkillFamilies.resolveFamilyId(skillId);
        return family != null && !family.equals(skillId) && isTechniqueSkillTableId(family);
    }

    private static boolean isTechniqueSkillTableId(String skillId)
    {
        if (CommonTechniqueSkillRegistry.isRegistered(skillId))
        {
            return true;
        }
        for (String techniqueId : TechniqueSkillSetRegistry.getRegisteredTechniqueIds())
        {
            IInnateTechniqueSkillSet set = TechniqueSkillSetRegistry.get(techniqueId).orElse(null);
            if (set == null)
            {
                continue;
            }
            for (TechniqueSkill skill : set.getConfigurableSkills())
            {
                if (skillId.equals(skill.skillId()))
                {
                    return true;
                }
                for (TechniqueSpecialMove move : set.getSpecialMoves(skill.skillId()))
                {
                    if (skillId.equals(move.variantId()) || skillId.equals(move.baseSkillId()))
                    {
                        return true;
                    }
                }
            }
            for (TechniqueSkill skill : set.getMechanicSkills())
            {
                if (skillId.equals(skill.skillId()))
                {
                    return true;
                }
            }
        }
        return false;
    }

    private static void write(LivingEntity caster, String mode, LinkedHashSet<UUID> ids)
    {
        if (caster instanceof ServerPlayer player)
        {
            JjkzPlayerDataStorage storage = JjkzPlayerDataStorage.get(player);
            JjkzPlayerDataStorage.PlayerDataEntry entry = storage.getOrCreate(player);
            entry.techniqueAtkObjMode = mode == null ? "" : mode;
            entry.techniqueAtkObjIds.clear();
            entry.techniqueAtkObjIds.addAll(ids);
            storage.setDirty();
            return;
        }
        CompoundTag tag = caster.getPersistentData();
        if (mode == null || mode.isEmpty())
        {
            tag.remove(TAG_MODE);
            tag.remove(TAG_IDS);
            return;
        }
        tag.putString(TAG_MODE, mode);
        ListTag list = new ListTag();
        for (UUID id : ids)
        {
            list.add(StringTag.valueOf(id.toString()));
        }
        tag.put(TAG_IDS, list);
    }
}
