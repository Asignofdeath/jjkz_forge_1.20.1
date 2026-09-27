package com.darkness.jujutsukaisenzone.technique.se;

import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import com.darkness.jujutsukaisenzone.registry.ModItems;
import com.darkness.jujutsukaisenzone.technique.se.mechanic.BrainChargeDullMechanic;
import com.darkness.jujutsukaisenzone.technique.se.mechanic.DamagedLeatherWatchMechanic;
import com.darkness.jujutsukaisenzone.technique.se.mechanic.FilledTimesheetMechanic;
import com.darkness.jujutsukaisenzone.technique.se.mechanic.MartialCritIgniteMechanic;
import com.darkness.jujutsukaisenzone.technique.se.mechanic.MartialDomainSiphonMechanic;
import com.darkness.jujutsukaisenzone.technique.se.mechanic.MeltdownSoftResetMechanic;
import com.darkness.jujutsukaisenzone.technique.se.mechanic.ShrineFinishCutMechanic;
import com.darkness.jujutsukaisenzone.technique.se.mechanic.StarEarringZenoPushMechanic;
import com.darkness.jujutsukaisenzone.technique.se.mechanic.StrongestProofMechanic;
import com.darkness.jujutsukaisenzone.technique.skillset.TechniqueSkillSetRegistry;
import net.minecraft.world.item.Item;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * SE 术式侵入装载元数据注册表。
 * <p>
 * 特质与代价均不在定义上声明：咒物产出时实例随机滚，装载时按实例结算。
 * 效果见 {@link SeMechanicRegistry}。
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
        // 旧烟管：体术暴击点燃（增量）
        register(SeEffectDef.of(
                MartialCritIgniteMechanic.SE_ID,
                ModItems.CURSED_OBJECT_OLD_PIPE,
                SeTrigger.ON_MARTIAL_CRIT,
                "fire",
                1,
                SeScope.generic(),
                SeCatalogTag.GENERIC, SeCatalogTag.ADDITIVE));
        // 裂纹算盘：体术命中灌领域量表（增量：多一条灌充来源）
        register(SeEffectDef.of(
                MartialDomainSiphonMechanic.SE_ID,
                ModItems.CURSED_OBJECT_CRACKED_ABACUS,
                SeTrigger.PASSIVE,
                "domain",
                2,
                SeScope.generic(),
                SeCatalogTag.GENERIC, SeCatalogTag.ADDITIVE));
        // 半融的冰袋：修熔断更省（修改兑换比）
        register(SeEffectDef.of(
                MeltdownSoftResetMechanic.SE_ID,
                ModItems.CURSED_OBJECT_MELTED_ICE_PACK,
                SeTrigger.PASSIVE,
                "",
                2,
                SeScope.generic(),
                SeCatalogTag.GENERIC, SeCatalogTag.MODIFY));
        // 填料脱落的耳塞：中度脑损充能惩罚减半（修改曲线）
        register(SeEffectDef.of(
                BrainChargeDullMechanic.SE_ID,
                ModItems.CURSED_OBJECT_LOOSE_EARPLUGS,
                SeTrigger.PASSIVE,
                "",
                2,
                SeScope.generic(),
                SeCatalogTag.GENERIC, SeCatalogTag.MODIFY));
        // 缺口的俎板：御厨子专用「收刀」（专用 + 增量）
        register(SeEffectDef.of(
                ShrineFinishCutMechanic.SE_ID,
                ModItems.CURSED_OBJECT_CHIPPED_CUTTING_BOARD,
                SeTrigger.PASSIVE,
                "signature",
                2,
                SeScope.technique(PlayerTechniqueData.TECHNIQUE_SHRINE),
                SeCatalogTag.SPECIALIZED, SeCatalogTag.ADDITIVE));
        // 掉钻的星形耳环：爱与幽会专用「推着走」（专用 + 修改芝诺墙）
        register(SeEffectDef.of(
                StarEarringZenoPushMechanic.SE_ID,
                ModItems.CURSED_OBJECT_STAR_EARRING,
                SeTrigger.PASSIVE,
                "",
                2,
                SeScope.technique(PlayerTechniqueData.TECHNIQUE_LOVE_RENDEZVOUS),
                SeCatalogTag.SPECIALIZED, SeCatalogTag.MODIFY));
        // 最强的证明：无下限专用「移除身旁豁免」（专用 + 移除）
        register(SeEffectDef.of(
                StrongestProofMechanic.SE_ID,
                ModItems.CURSED_OBJECT_PROOF_OF_THE_STRONGEST,
                SeTrigger.PASSIVE,
                "",
                2,
                SeScope.technique(PlayerTechniqueData.TECHNIQUE_LIMITLESS),
                SeCatalogTag.SPECIALIZED, SeCatalogTag.REMOVE));
        // 填满的工时单：通用「工作效率」触发（通用 + 增量）
        register(SeEffectDef.of(
                FilledTimesheetMechanic.SE_ID,
                ModItems.CURSED_OBJECT_FILLED_TIMESHEET,
                SeTrigger.PASSIVE,
                "",
                2,
                SeScope.generic(),
                SeCatalogTag.GENERIC, SeCatalogTag.ADDITIVE));
        // 损坏的皮质手表：通用「暴击叠层抬强度」（通用 + 增量）
        register(SeEffectDef.of(
                DamagedLeatherWatchMechanic.SE_ID,
                ModItems.CURSED_OBJECT_DAMAGED_LEATHER_WATCH,
                SeTrigger.PASSIVE,
                "",
                2,
                SeScope.generic(),
                SeCatalogTag.GENERIC, SeCatalogTag.ADDITIVE));
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

    /**
     * 启动自检：对照真实存在的术式 id 检查每条 SE 的声明，并与 mechanic 成对。
     * <p>
     * ⚠️ 必须在<b>全部术式技能集注册完成之后</b>、且 {@link SeMechanicRegistry#bootstrap()} 之后调用。
     */
    public static void validate()
    {
        if (BY_ID.isEmpty())
        {
            throw new IllegalStateException("SE 注册表是空的：SeEffectRegistry.bootstrap() 没有生效");
        }
        Set<String> knownTechniqueIds = new LinkedHashSet<>(TechniqueSkillSetRegistry.getRegisteredTechniqueIds());
        if (knownTechniqueIds.isEmpty())
        {
            throw new IllegalStateException("SE 自检时机不对：此刻术式表为空，"
                    + "本方法必须在全部术式技能集注册完成之后调用");
        }
        for (SeEffectDef def : BY_ID.values())
        {
            if (def.scope() instanceof SeScope.Technique technique
                    && !knownTechniqueIds.contains(technique.techniqueId()))
            {
                throw new IllegalStateException("专用 SE「" + def.seId() + "」指向未知术式 id："
                        + technique.techniqueId()
                        + "——拼错了，或术式改名后这里没跟着改（后果是这个 SE 永远装载不上）");
            }
            validateCatalogTags(def);
        }
        SeMechanicRegistry.validatePairedWithEffects();
    }

    private static void validateCatalogTags(SeEffectDef def)
    {
        Set<SeCatalogTag> tags = def.catalogTags();
        boolean generic = tags.contains(SeCatalogTag.GENERIC);
        boolean specialized = tags.contains(SeCatalogTag.SPECIALIZED);
        if (generic == specialized)
        {
            throw new IllegalStateException("SE「" + def.seId() + "」目录标签须且仅含一个 scope 轴"
                    + "（GENERIC 或 SPECIALIZED），当前：" + tags);
        }
        boolean genericScope = def.scope() instanceof SeScope.Generic;
        if (generic != genericScope)
        {
            throw new IllegalStateException("SE「" + def.seId() + "」目录标签与 SeScope 不一致："
                    + "scope=" + def.scope().getClass().getSimpleName() + " tags=" + tags);
        }
        boolean modify = tags.contains(SeCatalogTag.MODIFY);
        boolean additive = tags.contains(SeCatalogTag.ADDITIVE);
        boolean remove = tags.contains(SeCatalogTag.REMOVE);
        if (!modify && !additive && !remove)
        {
            throw new IllegalStateException("SE「" + def.seId() + "」目录标签至少要有一个 mode 轴"
                    + "（MODIFY / ADDITIVE / REMOVE），当前：" + tags);
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
