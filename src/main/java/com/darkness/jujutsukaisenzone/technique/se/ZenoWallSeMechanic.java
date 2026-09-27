package com.darkness.jujutsukaisenzone.technique.se;

/**
 * 芝诺墙改写窄口：把跨序之星的「位移域硬约束」换成「速度域施加力」。
 * <p>
 * 装载后 {@code StarMarkRules} 的跨序斥力不再走 {@code zenoWall}，改为沿同一条外向向量施加冲量。
 * 换域的后果是刻意的（论证见 {@code StarMarkRules#zenoWall} 的类内注释）：位移域是
 * 「无论多用力都只能指数逼近、永不抵达」，速度域是「比谁力气大，持续输出能净推进」。
 * ⇒ 「禁行」星域的「无法靠近」退化为「很难靠近」，跨序之星可以被硬挤进去并推着走。
 * 这正是本窄口存在的理由，不是 bug。
 * <p>
 * ⚠️ <b>同一窄口至多装载一条</b>：两条 SE 都改写芝诺墙会互相打架，由装载闸
 * （{@link SeMechanicRegistry#hasNarrowInterfaceConflict}）自动互斥——
 * 所以「以后又有一个 SE 要改芝诺墙」不需要维护冲突列表，实现同一个窄口即可。
 * 将来若要支持「改衰减指数 + 改作用距离」这种可共存的组合，就把本窄口拆细成两个。
 */
public interface ZenoWallSeMechanic extends SeMechanic
{
    /** 每 tick 沿外向施加的冲量（格/tick）。持续量级，参照 {@code ATTRACT_IMPULSE = 0.010}。 */
    double outwardImpulse();

    /**
     * 施术者本人是否也吃这股力。
     * {@code false} ⇒ 施术者不受斥力，可自由逼近并把对方推走（「推着别人走」的本体）；
     * {@code true} ⇒ 双方都被推开，与芝诺墙的双向约束同构（换了域但没换玩法）。
     */
    boolean pushesOwner();
}
