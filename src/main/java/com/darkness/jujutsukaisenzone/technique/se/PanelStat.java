package com.darkness.jujutsukaisenzone.technique.se;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.registry.ModAttributes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * SE 代价落点——13 条。
 * <p>
 * 产出时<b>先均匀抽 {@link SePenaltyAmountGroup}（三档各 1/3），再在组内均匀抽一条</b>，
 * 最后按该组的区间掷 amount（见 {@link SePenaltyAmountTable}）。
 * <p>
 * ⚠️ 不要在 13 条上一把均匀抽：三条组的条数是 2 / 7 / 4，
 * 直接均匀会把「削体术 / 术式强度」压到约 15%、把「削百分比类」抬到约 54%。
 * <p>
 * {@link #applyKind()} 决定结算通道：
 * <ul>
 *   <li>{@link ApplyKind#POWER_ROW}：面板强度负贡献</li>
 *   <li>{@link ApplyKind#ATTRIBUTE_SUBTRACT}：Attribute ADDITION 负修饰（含最大生命）</li>
 *   <li>{@link ApplyKind#CURSED_ENERGY_MAX_SUBTRACT}：有效咒力上限 −amount</li>
 *   <li>{@link ApplyKind#DOMAIN_GAUGE_MAX_ADD}：领域量表满条 +amount（更难攒满）</li>
 *   <li>{@link ApplyKind#BRAIN_DAMAGE_MAX_SUBTRACT}：脑损上限 −amount（更早封领域）</li>
 * </ul>
 */
public enum PanelStat
{
    PHYSICAL_POWER(null, SePenaltyAmountGroup.POWER, ApplyKind.POWER_ROW),
    TECHNIQUE_POWER(null, SePenaltyAmountGroup.POWER, ApplyKind.POWER_ROW),
    PHYSICAL_RESISTANCE(ModAttributes.PHYSICAL_RESISTANCE, SePenaltyAmountGroup.PERCENT, ApplyKind.ATTRIBUTE_SUBTRACT),
    TECHNIQUE_RESISTANCE(ModAttributes.TECHNIQUE_RESISTANCE, SePenaltyAmountGroup.PERCENT, ApplyKind.ATTRIBUTE_SUBTRACT),
    PHYSICAL_PENETRATION(ModAttributes.PHYSICAL_PENETRATION, SePenaltyAmountGroup.PERCENT, ApplyKind.ATTRIBUTE_SUBTRACT),
    TECHNIQUE_PENETRATION(ModAttributes.TECHNIQUE_PENETRATION, SePenaltyAmountGroup.PERCENT, ApplyKind.ATTRIBUTE_SUBTRACT),
    REVERSE_TECHNIQUE_EFFICIENCY(ModAttributes.REVERSE_TECHNIQUE_EFFICIENCY, SePenaltyAmountGroup.PERCENT, ApplyKind.ATTRIBUTE_SUBTRACT),
    DOMAIN_GAUGE_CHARGE_EFFICIENCY(ModAttributes.DOMAIN_GAUGE_CHARGE_EFFICIENCY, SePenaltyAmountGroup.PERCENT, ApplyKind.ATTRIBUTE_SUBTRACT),
    CRIT_CHANCE(ModAttributes.CRIT_CHANCE, SePenaltyAmountGroup.PERCENT, ApplyKind.ATTRIBUTE_SUBTRACT),
    CURSED_ENERGY_MAX(null, SePenaltyAmountGroup.RESOURCE, ApplyKind.CURSED_ENERGY_MAX_SUBTRACT),
    MAX_HEALTH(null, SePenaltyAmountGroup.RESOURCE, ApplyKind.ATTRIBUTE_SUBTRACT),
    DOMAIN_GAUGE_MAX(null, SePenaltyAmountGroup.RESOURCE, ApplyKind.DOMAIN_GAUGE_MAX_ADD),
    BRAIN_DAMAGE_MAX(null, SePenaltyAmountGroup.RESOURCE, ApplyKind.BRAIN_DAMAGE_MAX_SUBTRACT);

    private static final String LANG_PREFIX = "gui." + JuJutsuKaisenZone.MODID + ".panel_stat.";

    /**
     * 按数值分组索引落点。新增落点只要挂对 group，抽取分布自动跟上，不用改
     * {@link SePenaltyAmountTable#roll}。
     */
    private static final Map<SePenaltyAmountGroup, List<PanelStat>> BY_AMOUNT_GROUP =
            Arrays.stream(values()).collect(Collectors.groupingBy(
                    PanelStat::amountGroup, LinkedHashMap::new, Collectors.toList()));

    @Nullable
    private final RegistryObject<Attribute> attribute;
    private final SePenaltyAmountGroup amountGroup;
    private final ApplyKind applyKind;

    PanelStat(
            @Nullable RegistryObject<Attribute> attribute,
            SePenaltyAmountGroup amountGroup,
            ApplyKind applyKind)
    {
        this.attribute = attribute;
        this.amountGroup = amountGroup;
        this.applyKind = applyKind;
    }

    public enum ApplyKind
    {
        POWER_ROW,
        ATTRIBUTE_SUBTRACT,
        CURSED_ENERGY_MAX_SUBTRACT,
        DOMAIN_GAUGE_MAX_ADD,
        BRAIN_DAMAGE_MAX_SUBTRACT
    }

    public boolean isPowerRow()
    {
        return applyKind == ApplyKind.POWER_ROW;
    }

    public SePenaltyAmountGroup amountGroup()
    {
        return amountGroup;
    }

    public ApplyKind applyKind()
    {
        return applyKind;
    }

    /**
     * Attribute 层落点。
     * {@link #MAX_HEALTH} 用原版 {@link Attributes#MAX_HEALTH}（不经 RegistryObject）。
     */
    @Nullable
    public RegistryObject<Attribute> attribute()
    {
        return attribute;
    }

    public boolean usesVanillaMaxHealth()
    {
        return this == MAX_HEALTH;
    }

    public String id()
    {
        return name().toLowerCase(Locale.ROOT);
    }

    @Nullable
    public static PanelStat find(String id)
    {
        if (id == null || id.isBlank())
        {
            return null;
        }
        String normalized = id.trim().toLowerCase(Locale.ROOT);
        for (PanelStat stat : values())
        {
            if (stat.id().equals(normalized))
            {
                return stat;
            }
        }
        return null;
    }

    /** 该数值分组下的全部落点（枚举声明序）。组不存在或为空时返回空表。 */
    public static List<PanelStat> byAmountGroup(SePenaltyAmountGroup group)
    {
        if (group == null)
        {
            return List.of();
        }
        return Collections.unmodifiableList(BY_AMOUNT_GROUP.getOrDefault(group, List.of()));
    }

    public Component displayName()
    {
        return Component.translatable(LANG_PREFIX + id());
    }
}
