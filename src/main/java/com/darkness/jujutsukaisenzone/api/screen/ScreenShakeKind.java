package com.darkness.jujutsukaisenzone.api.screen;

/**
 * 屏幕振动类型，供服务端 {@link com.darkness.jujutsukaisenzone.network.ModNetwork#sendScreenShake} 与
 * 客户端 {@link com.darkness.jujutsukaisenzone.client.ScreenShakeApi} 共用。
 */
public enum ScreenShakeKind
{
    /** 随机偏移、振幅随时间衰减（黑闪、无量空处完成等）。 */
    DECAY,
    /** 恒定振幅、墙钟截止（灶开爆炸等）。 */
    CONSTANT,
    /** 持续开关式振动（伏魔神龛领域攻击期间）。 */
    SUSTAINED,
    /** 低频 sin 波平滑晃动（ghost_face 等）。 */
    SMOOTH_WOBBLE,
    /** 单次冲击：固定方向甩出再收回，不连震。 */
    SINGLE
}
