package com.darkness.jujutsukaisenzone.technique.domain;

import net.minecraft.server.level.ServerPlayer;

/**
 * 术式领域展开抽象基类 - 所有术式专属领域的公共实现。
 * <p>
 * 继承自 {@link AbstractDomainExpansion}，为术式专属领域提供通用逻辑。
 * <p>
 * 与 {@link com.darkness.jujutsukaisenzone.technique.DomainExpansionServerLogic}（基础领域）平级，
 * 两者都继承自 {@link AbstractDomainExpansion}。
 * <p>
 * 类层次：
 * <pre>
 *   AbstractDomainExpansion
 *       ├── DomainExpansionServerLogic（基础领域，无术式时）
 *       └── AbstractTechniqueDomain（本类）
 *               ├── XxxDomainExpansion（具体术式领域）
 *               └── ...
 * </pre>
 * <p>
 * 子类需要实现：
 * <ul>
 *   <li>{@link #getTechniqueType()} - 返回术式类型标识</li>
 *   <li>{@link #applyDomainSpecificEffect} - 术式专属效果</li>
 *   <li>{@link #hasLearnedTechnique(ServerPlayer)} - 检查玩家是否习得对应术式</li>
 * </ul>
 */
public abstract class AbstractTechniqueDomain extends AbstractDomainExpansion
{
    // ==================== 子类必须实现的抽象方法 ====================

    /**
     * 检查玩家是否习得此术式领域对应的术式。
     * <p>
     * 术式领域的前置条件除了习得领域展开和结界术外，
     * 还需要习得对应的术式。
     *
     * @param player 玩家
     * @return 是否习得对应术式
     */
    protected abstract boolean hasLearnedTechnique(ServerPlayer player);

    // ==================== 覆盖父类方法 ====================

    /**
     * 术式领域的施放条件。
     * <p>
     * 在基础领域条件之上，额外要求玩家已习得对应术式。
     *
     * @param player 玩家
     * @return 是否可以施放
     */
    @Override
    public boolean canCast(ServerPlayer player)
    {
        return super.canCast(player) && hasLearnedTechnique(player);
    }
}
