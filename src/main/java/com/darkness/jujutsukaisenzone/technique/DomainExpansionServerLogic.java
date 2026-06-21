package com.darkness.jujutsukaisenzone.technique;

import com.darkness.jujutsukaisenzone.technique.domain.AbstractDomainExpansion;
import net.minecraft.server.level.ServerPlayer;

/**
 * 基础领域展开 - 玩家未习得术式时的领域展开。
 * <p>
 * 继承自 {@link AbstractDomainExpansion}，实现基础领域的逻辑。
 * 基础领域无特殊效果，仅提供领域展开的基本功能。
 * <p>
 * 当玩家有术式时，应使用术式专属领域（继承自 {@link com.darkness.jujutsukaisenzone.technique.domain.AbstractTechniqueDomain}）。
 */
public final class DomainExpansionServerLogic extends AbstractDomainExpansion
{
    // ==================== 单例实例 ====================

    private static final DomainExpansionServerLogic INSTANCE = new DomainExpansionServerLogic();

    public static DomainExpansionServerLogic getInstance()
    {
        return INSTANCE;
    }

    /** 静态委托方法，供网络包等处调用。 */
    public static void staticRepairBarrierGaps(ServerPlayer player) { INSTANCE.repairBarrierGaps(player); }

    // ==================== 实现抽象方法 ====================

    @Override
    protected void applyDomainSpecificEffect(ServerPlayer player, BarrierInstance instance)
    {
        // 基础领域无特殊效果
    }

    @Override
    public String getTechniqueType()
    {
        return "domain_expansion";
    }
}
