package com.darkness.jujutsukaisenzone.technique.se;

import com.darkness.jujutsukaisenzone.registry.ModItems;
import net.minecraft.world.item.Item;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * SE 术式侵入注册表。
 */
public final class SeEffectRegistry
{
    private static final Map<String, SeEffectDef> BY_ID = new LinkedHashMap<>();
    private static final Map<Item, SeEffectDef> BY_ITEM = new LinkedHashMap<>();

    private SeEffectRegistry()
    {
    }

    public static void bootstrap()
    {
        BY_ID.clear();
        BY_ITEM.clear();
        // 样板：旧烟管 → 体术暴击后点燃（读 HitCritState，不再自掷）
        register(SeEffectDef.of(
                "martial_crit_ignite",
                ModItems.CURSED_OBJECT_OLD_PIPE,
                SeTrigger.ON_MARTIAL_CRIT,
                "fire",
                1.0F,
                4));
    }

    private static void register(SeEffectDef def)
    {
        BY_ID.put(def.seId(), def);
    }

    /** 物品注册完成后把 item→def 索引建好（bootstrap 时 RegistryObject 可能尚未 ready）。 */
    public static void rebuildItemIndex()
    {
        BY_ITEM.clear();
        for (SeEffectDef def : BY_ID.values())
        {
            BY_ITEM.put(def.item(), def);
        }
    }

    public static Optional<SeEffectDef> find(String seId)
    {
        if (seId == null || seId.isBlank())
        {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_ID.get(seId));
    }

    public static Optional<SeEffectDef> findByItem(Item item)
    {
        if (item == null)
        {
            return Optional.empty();
        }
        if (BY_ITEM.isEmpty() && !BY_ID.isEmpty())
        {
            rebuildItemIndex();
        }
        return Optional.ofNullable(BY_ITEM.get(item));
    }

    public static Collection<SeEffectDef> all()
    {
        return Collections.unmodifiableCollection(BY_ID.values());
    }
}
