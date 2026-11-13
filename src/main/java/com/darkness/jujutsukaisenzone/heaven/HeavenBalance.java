package com.darkness.jujutsukaisenzone.heaven;

import com.darkness.jujutsukaisenzone.gameplay.CursedSpiritGrade;
import com.darkness.jujutsukaisenzone.heaven.adapter.HeavenAdapterRegistry;
import com.darkness.jujutsukaisenzone.heaven.scale.ExternalMobPowerAdapter;
import com.darkness.jujutsukaisenzone.heaven.threat.CurseCounterSeAdapter;
import com.darkness.jujutsukaisenzone.heaven.threat.CurseSpiritHeavenSe;
import com.darkness.jujutsukaisenzone.heaven.threat.HeavenBalanceData;
import com.darkness.jujutsukaisenzone.heaven.threat.HeavenCounterRegistry;
import com.darkness.jujutsukaisenzone.heaven.threat.HeavenThreatIds;
import com.darkness.jujutsukaisenzone.technique.se.SeMechanic;
import com.darkness.jujutsukaisenzone.technique.se.SeMechanicRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * 天道平衡门面：适配器 bootstrap、对策 SE 查询、威胁加压。
 */
public final class HeavenBalance
{
    private HeavenBalance()
    {
    }

    /**
     * 须在 {@link SeMechanicRegistry#bootstrap()} 与 {@link com.darkness.jujutsukaisenzone.technique.se.SeEffectRegistry#validate()}
     * 之后调用。
     */
    public static void bootstrap()
    {
        HeavenAdapterRegistry.bootstrap();
        HeavenCounterRegistry.bootstrap();
        HeavenAdapterRegistry.register(ExternalMobPowerAdapter.INSTANCE);
        HeavenAdapterRegistry.register(CurseCounterSeAdapter.INSTANCE);
        HeavenCounterRegistry.validate();
    }

    /**
     * 已生效的对策 mechanic：玩家走装载；咒灵走「资格 ∩ 维度解锁」；其它 empty。
     * 玩家永远不走天道资格。
     */
    public static <T extends SeMechanic> Optional<T> findActive(LivingEntity entity, Class<T> type)
    {
        if (entity == null || type == null)
        {
            return Optional.empty();
        }
        if (entity instanceof Player player)
        {
            return SeMechanicRegistry.findEquipped(player, type);
        }
        if (!CurseSpiritEntities.isCurseSpirit(entity))
        {
            return Optional.empty();
        }
        CursedSpiritGrade grade = CurseSpiritEntities.gradeOrNull(entity);
        if (grade != null)
        {
            CurseSpiritHeavenSe.ensureRolled(entity, grade);
        }
        Level level = entity.level();
        if (!(level instanceof ServerLevel serverLevel))
        {
            return Optional.empty();
        }
        for (String seId : CurseSpiritHeavenSe.eligibleIds(entity))
        {
            if (!HeavenCounterRegistry.isUnlocked(serverLevel, seId))
            {
                continue;
            }
            SeMechanic mechanic = SeMechanicRegistry.find(seId).orElse(null);
            if (type.isInstance(mechanic))
            {
                return Optional.of(type.cast(mechanic));
            }
        }
        return Optional.empty();
    }

    /** 对咒灵断肢成功后调用：本维 dismemberment 压力上升。 */
    public static void onCurseSpiritSevered(LivingEntity target)
    {
        if (!CurseSpiritEntities.isCurseSpirit(target))
        {
            return;
        }
        if (!(target.level() instanceof ServerLevel serverLevel))
        {
            return;
        }
        HeavenBalanceData.get(serverLevel).addPressure(
                serverLevel,
                HeavenThreatIds.DISMEMBERMENT,
                HeavenBalanceData.PRESSURE_PER_SEVER);
    }

    public static int pressure(ServerLevel level, String threatId)
    {
        if (level == null)
        {
            return 0;
        }
        return HeavenBalanceData.get(level).getPressure(level, threatId);
    }
}
