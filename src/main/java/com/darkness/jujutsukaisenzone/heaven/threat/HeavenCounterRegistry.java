package com.darkness.jujutsukaisenzone.heaven.threat;

import com.darkness.jujutsukaisenzone.technique.se.SeMechanicRegistry;
import com.darkness.jujutsukaisenzone.technique.se.mechanic.CauterizedCottonMechanic;
import com.darkness.jujutsukaisenzone.technique.se.mechanic.CleanGauzeRollMechanic;
import com.darkness.jujutsukaisenzone.technique.se.mechanic.LeadWaistGuardMechanic;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 天道对策 SE 目录。只登记可进咒灵资格池的 seId；效果仍在 {@link SeMechanicRegistry}。
 */
public final class HeavenCounterRegistry
{
    private static final Map<String, HeavenCounterDef> BY_SE_ID = new LinkedHashMap<>();

    private HeavenCounterRegistry()
    {
    }

    public static void bootstrap()
    {
        BY_SE_ID.clear();
        // 断肢对策（与玩家三条 SE 同 id / 同 mechanic）
        register(HeavenCounterDef.of(
                CleanGauzeRollMechanic.SE_ID,
                HeavenThreatIds.DISMEMBERMENT,
                20));
        register(HeavenCounterDef.of(
                LeadWaistGuardMechanic.SE_ID,
                HeavenThreatIds.DISMEMBERMENT,
                45));
        register(HeavenCounterDef.of(
                CauterizedCottonMechanic.SE_ID,
                HeavenThreatIds.DISMEMBERMENT,
                70));
    }

    /** 扩展入口：新对策在 bootstrap 或其后登记一行即可。 */
    public static void register(HeavenCounterDef def)
    {
        if (def == null)
        {
            throw new IllegalArgumentException("HeavenCounterDef");
        }
        if (BY_SE_ID.put(def.seId(), def) != null)
        {
            throw new IllegalStateException("重复注册天道对策：" + def.seId());
        }
    }

    /** 须在 {@link SeMechanicRegistry#bootstrap()} 之后调用。 */
    public static void validate()
    {
        for (HeavenCounterDef def : BY_SE_ID.values())
        {
            if (SeMechanicRegistry.find(def.seId()).isEmpty())
            {
                throw new IllegalStateException("天道对策「" + def.seId() + "」没有对应 SeMechanic");
            }
        }
    }

    public static Optional<HeavenCounterDef> find(String seId)
    {
        if (seId == null || seId.isBlank())
        {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_SE_ID.get(seId));
    }

    public static Collection<HeavenCounterDef> all()
    {
        return Collections.unmodifiableCollection(BY_SE_ID.values());
    }

    public static List<HeavenCounterDef> allList()
    {
        return List.copyOf(BY_SE_ID.values());
    }

    public static boolean isUnlocked(ServerLevel level, String seId)
    {
        HeavenCounterDef def = BY_SE_ID.get(seId);
        if (def == null || level == null)
        {
            return false;
        }
        int pressure = HeavenBalanceData.get(level).getPressure(level, def.threatId());
        return pressure >= def.unlockPressure();
    }

    public static List<HeavenCounterDef> unlockedIn(ServerLevel level, String threatId)
    {
        if (level == null || threatId == null)
        {
            return List.of();
        }
        int pressure = HeavenBalanceData.get(level).getPressure(level, threatId);
        List<HeavenCounterDef> out = new ArrayList<>();
        for (HeavenCounterDef def : BY_SE_ID.values())
        {
            if (threatId.equals(def.threatId()) && pressure >= def.unlockPressure())
            {
                out.add(def);
            }
        }
        return Collections.unmodifiableList(out);
    }
}
