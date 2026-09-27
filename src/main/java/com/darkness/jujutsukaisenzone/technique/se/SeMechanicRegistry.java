package com.darkness.jujutsukaisenzone.technique.se;

import com.darkness.jujutsukaisenzone.player.PlayerSeLoadoutData;
import com.darkness.jujutsukaisenzone.technique.se.mechanic.MartialCritIgniteMechanic;
import com.darkness.jujutsukaisenzone.technique.se.mechanic.MartialDomainSiphonMechanic;
import com.darkness.jujutsukaisenzone.technique.se.mechanic.ShrineFinishCutMechanic;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * SE 效果处理器注册表与按玩家装载的查询入口。
 * <p>
 * 业务点只认窄口接口（如 {@link DomainGaugeSeMechanic}），不散落 {@code seId.equals(...)}。
 */
public final class SeMechanicRegistry
{
    private static final Map<String, SeMechanic> BY_ID = new LinkedHashMap<>();

    /**
     * 全部「窄口」接口：一条 SE 至多实现其中一个（{@link #validatePairedWithEffects()} 强制），
     * 且<b>同一窄口至多装载一条</b>（{@link #hasNarrowInterfaceConflict} 在装载闸上强制）。
     */
    private static final List<Class<? extends SeMechanic>> NARROW_INTERFACES = List.of(
            MartialCritSeMechanic.class,
            DomainGaugeSeMechanic.class,
            ShrineFinishCutSeMechanic.class);

    private SeMechanicRegistry()
    {
    }

    public static void bootstrap()
    {
        BY_ID.clear();
        register(new MartialCritIgniteMechanic());
        register(new MartialDomainSiphonMechanic());
        register(new ShrineFinishCutMechanic());
    }

    private static void register(SeMechanic mechanic)
    {
        if (mechanic == null || mechanic.seId() == null || mechanic.seId().isBlank())
        {
            throw new IllegalArgumentException("SeMechanic 缺少 seId");
        }
        if (BY_ID.put(mechanic.seId(), mechanic) != null)
        {
            throw new IllegalStateException("重复注册 SE mechanic：" + mechanic.seId());
        }
    }

    public static Optional<SeMechanic> find(String seId)
    {
        if (seId == null || seId.isBlank())
        {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_ID.get(seId));
    }

    /**
     * 该玩家已装载的、实现了 {@code type} 的第一条 mechanic（注册序）。
     * <p>
     * <b>契约</b>：同一窄口（{@link #NARROW_INTERFACES} 之一）至多装载一条，由
     * {@link PlayerSeLoadoutData#tryEquip} 的互斥校验在装载时拦住，
     * 因此「取注册序第一条」不存在歧义，不会随装载顺序漂移。
     * 跨窄口同接口叠装时同样取先注册者；调用方若要叠算请用 {@link #allEquipped}。
     */
    public static <T extends SeMechanic> Optional<T> findEquipped(Player player, Class<T> type)
    {
        if (player == null || type == null)
        {
            return Optional.empty();
        }
        for (String seId : PlayerSeLoadoutData.getEquippedIds(player))
        {
            SeMechanic mechanic = BY_ID.get(seId);
            if (type.isInstance(mechanic))
            {
                return Optional.of(type.cast(mechanic));
            }
        }
        return Optional.empty();
    }

    /** 该玩家已装载的全部 {@code type} 实现（注册序）。 */
    public static <T extends SeMechanic> List<T> allEquipped(Player player, Class<T> type)
    {
        if (player == null || type == null)
        {
            return List.of();
        }
        List<T> result = new ArrayList<>();
        for (String seId : PlayerSeLoadoutData.getEquippedIds(player))
        {
            SeMechanic mechanic = BY_ID.get(seId);
            if (type.isInstance(mechanic))
            {
                result.add(type.cast(mechanic));
            }
        }
        return Collections.unmodifiableList(result);
    }

    /** 两条 mechanic 是否共用同一个窄口（决定装载互斥）。 */
    public static boolean shareNarrowInterface(@Nullable SeMechanic a, @Nullable SeMechanic b)
    {
        if (a == null || b == null || a == b)
        {
            return false;
        }
        for (Class<? extends SeMechanic> narrow : NARROW_INTERFACES)
        {
            if (narrow.isInstance(a) && narrow.isInstance(b))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * {@code candidateId} 与已装载集合是否撞窄口。
     * <p>
     * 撞了就必须拦：{@link #findEquipped} 只认先注册者，装上第二条等于白装（死重）。
     * 客户端也调它，用来把装不上的条目提前置灰——判据必须与服务端一致。
     */
    public static boolean hasNarrowInterfaceConflict(Collection<String> equippedIds, String candidateId)
    {
        SeMechanic candidate = find(candidateId).orElse(null);
        if (candidate == null || equippedIds == null)
        {
            return false;
        }
        for (String equippedId : equippedIds)
        {
            if (shareNarrowInterface(find(equippedId).orElse(null), candidate))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * 自检：每条 {@link SeEffectDef} 必须有对应 mechanic，每个 mechanic 必须有对应 def；
     * 且 mechanic 的窄口形态与 def 上声明的 {@link SeTrigger} 必须一致。
     * 在 {@link SeEffectRegistry#validate()} 末尾调用。
     */
    public static void validatePairedWithEffects()
    {
        if (BY_ID.isEmpty())
        {
            throw new IllegalStateException("SeMechanicRegistry 是空的：bootstrap() 没有生效");
        }
        for (SeEffectDef def : SeEffectRegistry.all())
        {
            if (!BY_ID.containsKey(def.seId()))
            {
                throw new IllegalStateException("SE「" + def.seId() + "」没有注册 SeMechanic"
                        + "——装载元数据有了，效果处理器缺失");
            }
        }
        for (String seId : BY_ID.keySet())
        {
            if (SeEffectRegistry.find(seId).isEmpty())
            {
                throw new IllegalStateException("SeMechanic「" + seId + "」没有对应 SeEffectDef"
                        + "——效果处理器悬空");
            }
        }

        for (Map.Entry<String, SeMechanic> entry : BY_ID.entrySet())
        {
            SeMechanic mechanic = entry.getValue();
            SeEffectDef def = SeEffectRegistry.find(entry.getKey()).orElse(null);
            if (def == null)
            {
                continue;
            }
            int narrowCount = 0;
            for (Class<? extends SeMechanic> narrow : NARROW_INTERFACES)
            {
                if (narrow.isInstance(mechanic))
                {
                    narrowCount++;
                }
            }
            if (narrowCount > 1)
            {
                throw new IllegalStateException("SeMechanic「" + entry.getKey() + "」同时实现了多个窄口"
                        + "——装载互斥与 findEquipped 的「取第一条」都会失去意义，请拆成两条 SE");
            }
            boolean declaredConditional = def.trigger() == SeTrigger.ON_MARTIAL_CRIT;
            boolean implementedConditional = mechanic instanceof MartialCritSeMechanic;
            if (declaredConditional != implementedConditional)
            {
                throw new IllegalStateException("SE「" + def.seId() + "」trigger=" + def.trigger()
                        + " 与 mechanic 形态不一致："
                        + (declaredConditional
                                ? "声明为条件触发，但 mechanic 没实现 MartialCritSeMechanic，事件链永远回调不到它"
                                : "实现了 MartialCritSeMechanic，但 trigger 不是 ON_MARTIAL_CRIT")
                        + "——两处必须同时改");
            }
        }
    }
}
