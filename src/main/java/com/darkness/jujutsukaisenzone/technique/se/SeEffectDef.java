package com.darkness.jujutsukaisenzone.technique.se;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/**
 * SE（术式侵入）装载元数据：咒物、触发形态、互斥、质量、术式 scope、目录标签。
 * <p>
 * 效果逻辑在 {@link SeMechanic}（见 {@link SeMechanicRegistry}），不在本 record 里塞烟管字段。
 * <p>
 * 装载闸：咒物实例特质须吻合、术式 scope、质量。
 * <b>代价与特质均在咒物产出时写入实例</b>，不在本定义里写死。
 * <p>
 * 互斥有两层：本类的 {@code mutexGroup} 是设计师<b>额外</b>声明的冲突；
 * 另外同一「窄口」的 SE 由装载层<b>自动互斥</b>（见 {@code SeMechanicRegistry#hasNarrowInterfaceConflict}），
 * 不用也不能在这里写。
 * <p>
 * {@code catalogTags} 只服务 UI 筛选：须含且仅含一个 scope 轴标签（与 {@link SeScope} 一致），
 * 以及至少一个 mode 轴标签（{@link SeCatalogTag#MODIFY} / {@link SeCatalogTag#ADDITIVE} /
 * {@link SeCatalogTag#REMOVE}）。
 */
public record SeEffectDef(
        String seId,
        Supplier<? extends Item> cursedObject,
        SeTrigger trigger,
        String mutexGroup,
        int quality,
        SeScope scope,
        Set<SeCatalogTag> catalogTags)
{
    public SeEffectDef
    {
        if (seId == null || seId.isBlank())
        {
            throw new IllegalArgumentException("seId");
        }
        if (cursedObject == null)
        {
            throw new IllegalArgumentException("cursedObject");
        }
        if (trigger == null)
        {
            throw new IllegalArgumentException("trigger");
        }
        mutexGroup = mutexGroup == null ? "" : mutexGroup;
        if (quality < 1)
        {
            throw new IllegalArgumentException("quality 必须 >= 1（0 质量的 SE 等于免费强度）：" + seId);
        }
        if (scope == null)
        {
            throw new IllegalArgumentException("scope：" + seId);
        }
        if (catalogTags == null || catalogTags.isEmpty())
        {
            throw new IllegalArgumentException("catalogTags 不能为空：" + seId);
        }
        catalogTags = Collections.unmodifiableSet(EnumSet.copyOf(catalogTags));
    }

    public static SeEffectDef of(
            String seId,
            RegistryObject<? extends Item> item,
            SeTrigger trigger,
            String mutexGroup,
            int quality,
            SeScope scope,
            SeCatalogTag... catalogTags)
    {
        Objects.requireNonNull(catalogTags, "catalogTags");
        if (catalogTags.length == 0)
        {
            throw new IllegalArgumentException("catalogTags 不能为空：" + seId);
        }
        return new SeEffectDef(seId, item, trigger, mutexGroup, quality, scope, EnumSet.of(
                catalogTags[0],
                java.util.Arrays.copyOfRange(catalogTags, 1, catalogTags.length)));
    }

    public Item item()
    {
        return cursedObject.get();
    }

    public boolean matchesCatalogFilter(Set<SeCatalogTag> selected)
    {
        return SeCatalogTag.matchesFilter(catalogTags, selected);
    }
}
