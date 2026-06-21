package com.darkness.jujutsukaisenzone.player;

import com.darkness.jujutsukaisenzone.technique.development.SkillUnlockLogic;
import com.darkness.jujutsukaisenzone.technique.skillset.TechniqueSkill;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 玩家技能快捷栏配置：每个术式独立保存 9 个栏位（1~9）到 skillId 的映射。
 * <p>
 * 存储位置：玩家 {@code persistentData}，key 格式为 {@code jujutsukaisenzone_skillbar_{techniqueId}_slot_{1..9}}。
 * <p>
 * 首次加载时自动用 {@link TechniqueSkill#defaultSlot()} 填充默认值。
 */
public final class PlayerSkillBarConfig
{
    private static final String KEY_PREFIX = "jujutsukaisenzone_skillbar_";
    private static final String SLOT_KEY_SUFFIX = "_slot_";

    private PlayerSkillBarConfig()
    {
    }

    /**
     * 获取指定术式、指定栏位（1~9）配置的技能 ID；未配置时返回空串。
     */
    public static String getSkillId(Player player, String techniqueId, int slot)
    {
        if (techniqueId == null || techniqueId.isBlank())
        {
            return "";
        }
        String key = slotKey(techniqueId, slot);
        CompoundTag data = player.getPersistentData();
        return data.contains(key, CompoundTag.TAG_STRING) ? data.getString(key) : "";
    }

    /**
     * 设置指定术式、指定栏位配置的技能 ID。
     */
    public static void setSkillId(Player player, String techniqueId, int slot, String skillId)
    {
        if (techniqueId == null || techniqueId.isBlank() || slot < 1 || slot > 9)
        {
            return;
        }
        player.getPersistentData().putString(slotKey(techniqueId, slot), skillId == null ? "" : skillId);
    }

    /**
     * 读取整个术式的 9 栏位配置，返回快照 Map。
     */
    public static Map<Integer, String> getAllSlots(Player player, String techniqueId)
    {
        Map<Integer, String> result = new LinkedHashMap<>();
        for (int i = 1; i <= 9; i++)
        {
            result.put(i, getSkillId(player, techniqueId, i));
        }
        return result;
    }

    /**
     * 用已解锁技能的默认槽位填充空栏位（仅填充尚未配置的栏位）。
     */
    public static void fillDefaults(Player player, String techniqueId, List<TechniqueSkill> skills)
    {
        Map<Integer, String> filled = new LinkedHashMap<>();
        for (TechniqueSkill skill : skills)
        {
            if (!SkillUnlockLogic.isSkillUnlocked(player, techniqueId, skill.skillId(), skill))
            {
                continue;
            }
            int slot = skill.defaultSlot();
            if (slot >= 1 && slot <= 9)
            {
                filled.put(slot, skill.skillId());
            }
        }

        CompoundTag tag = player.getPersistentData();
        for (int i = 1; i <= 9; i++)
        {
            String key = slotKey(techniqueId, i);
            if (!tag.contains(key, CompoundTag.TAG_STRING) || tag.getString(key).isEmpty())
            {
                String defaultSkill = filled.getOrDefault(i, "");
                if (!defaultSkill.isEmpty())
                {
                    tag.putString(key, defaultSkill);
                }
            }
        }
    }

    /** 清除快捷栏中尚未解锁的技能配置。 */
    public static void sanitizeSlots(Player player, String techniqueId)
    {
        if (techniqueId == null || techniqueId.isBlank())
        {
            return;
        }
        for (int i = 1; i <= 9; i++)
        {
            String current = getSkillId(player, techniqueId, i);
            String sanitized = SkillUnlockLogic.sanitizeSkillBarSlot(player, techniqueId, current);
            if (!current.equals(sanitized))
            {
                setSkillId(player, techniqueId, i, sanitized);
            }
        }
    }

    /**
     * 清空某个术式的所有栏位配置。
     */
    public static void clear(Player player, String techniqueId)
    {
        for (int i = 1; i <= 9; i++)
        {
            player.getPersistentData().remove(slotKey(techniqueId, i));
        }
    }

    /**
     * 从原始玩家复制所有技能快捷栏配置到新玩家（PlayerEvent.Clone 时调用，防止死亡后配置清空）。
     */
    public static void copyAllFrom(Player newPlayer, Player originalPlayer)
    {
        CompoundTag oldData = originalPlayer.getPersistentData();
        CompoundTag newData = newPlayer.getPersistentData();
        for (String key : oldData.getAllKeys())
        {
            if (key.startsWith(KEY_PREFIX))
            {
                newData.put(key, oldData.get(key));
            }
        }
    }

    private static String slotKey(String techniqueId, int slot)
    {
        return KEY_PREFIX + techniqueId + SLOT_KEY_SUFFIX + slot;
    }
}
