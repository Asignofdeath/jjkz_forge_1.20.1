package com.darkness.jujutsukaisenzone.entity.data;

import com.darkness.jujutsukaisenzone.entity.util.SpecialEffectChunkForce;

/**
 * 无下限术式「「稍微乱来一下」· 超远程虚式茈」特效参数。
 */
public final class LimitlessRemoteMurasakiConfig
{
    private LimitlessRemoteMurasakiConfig() {}

    /** 起手动画 {@code remote_murasaki_start} 等待时长（tick）。 */
    public static final int START_DELAY_TICKS = 20 * 5;
    /** 长按蓄力满后松手才进入发射阶段（与起手演出时长一致；满后服务端自动发射，无需松手）。 */
    public static final int HOLD_RELEASE_CHARGE_TICKS = START_DELAY_TICKS;
    /** 起手第 1 秒：左侧苍球 Photon。 */
    public static final int START_BLUE_SPAWN_TICK = 20;
    /** 起手第 1 秒：蓝色世界层滤镜 0.4 秒。 */
    public static final int START_BLUE_OVERLAY_TOTAL_TICKS = 8;
    public static final int START_BLUE_OVERLAY_FADE_IN_TICKS = 2;
    public static final int START_BLUE_OVERLAY_FADE_OUT_TICKS = 6;
    public static final float START_BLUE_OVERLAY_PEAK_ALPHA = 0.45F;
    public static final int START_BLUE_OVERLAY_COLOR_R = 40;
    public static final int START_BLUE_OVERLAY_COLOR_G = 120;
    public static final int START_BLUE_OVERLAY_COLOR_B = 255;
    /** 起手第 3 秒：右侧赫球 Photon。 */
    public static final int START_RED_SPAWN_TICK = 20 * 3;
    /** 起手第 3 秒：红色世界层滤镜 0.4 秒。 */
    public static final int START_RED_OVERLAY_TOTAL_TICKS = 8;
    public static final int START_RED_OVERLAY_FADE_IN_TICKS = 2;
    public static final int START_RED_OVERLAY_FADE_OUT_TICKS = 6;
    public static final float START_RED_OVERLAY_PEAK_ALPHA = 0.45F;
    public static final int START_RED_OVERLAY_COLOR_R = 255;
    public static final int START_RED_OVERLAY_COLOR_G = 40;
    public static final int START_RED_OVERLAY_COLOR_B = 40;
    /** 起手第 4 秒：双侧开始向中线合拢。 */
    public static final int START_CONVERGE_TICK = 20 * 4;
    /** 起手第 5 秒：紫色滤镜（先全屏 0.3 秒，再无缝衔接世界层 0.8 秒）。 */
    public static final int START_PURPLE_OVERLAY_TICK = 20 * 5;
    /** 全屏紫色滤镜时长：0.3 秒；淡出为 0，峰值无缝交给世界层。 */
    public static final int START_PURPLE_FULLSCREEN_TOTAL_TICKS = 6;
    public static final int START_PURPLE_FULLSCREEN_FADE_IN_TICKS = 2;
    public static final int START_PURPLE_FULLSCREEN_FADE_OUT_TICKS = 0;
    public static final float START_PURPLE_FULLSCREEN_PEAK_ALPHA = 0.55F;
    /** 全屏结束后衔接的世界层紫色滤镜：0.8 秒。 */
    public static final int START_PURPLE_WORLD_LAYER_TOTAL_TICKS = 16;
    /** 无缝衔接：世界层不做淡入，直接以峰值透明度开始。 */
    public static final int START_PURPLE_WORLD_LAYER_FADE_IN_TICKS = 0;
    public static final int START_PURPLE_WORLD_LAYER_FADE_OUT_TICKS = 12;
    /** 与全屏峰值一致，保证衔接时不跳变。 */
    public static final float START_PURPLE_WORLD_LAYER_PEAK_ALPHA = 0.55F;
    /** 起手第 5 秒：施法者屏幕短震时长（tick）。 */
    public static final int START_SCREEN_SHAKE_TICKS = 10;
    /** 起手第 5 秒：施法者屏幕短震最大偏转（度）。 */
    public static final float START_SCREEN_SHAKE_MAX_DEG = 3.0F;
    /** 飞行阶段：屏幕晃动影响半径（格）。 */
    public static final double FLIGHT_SCREEN_SHAKE_RADIUS = 32.0D;
    /** 飞行阶段：向范围内玩家刷新 {@link com.darkness.jujutsukaisenzone.api.screen.ScreenShakeKind#SMOOTH_WOBBLE} 的间隔（tick）。 */
    public static final int FLIGHT_SCREEN_SHAKE_INTERVAL_TICKS = 20;
    /** 飞行阶段：单次平滑晃动时长（tick），较长以保持低频率。 */
    public static final int FLIGHT_SCREEN_SHAKE_DURATION_TICKS = 60;
    /** 飞行阶段：平滑晃动最大偏转（度）。 */
    public static final float FLIGHT_SCREEN_SHAKE_MAX_AMPLITUDE = 8.0F;
    /** 起手演出：玩家视线前方中心距离（格）。 */
    public static final double START_CENTER_FORWARD = 3.0D;
    /** 起手演出：苍/赫球相对中线的横向偏移（格）。 */
    public static final double START_SIDE_OFFSET = 2.5D;
    /** 起手苍球 Photon 命令（资源 id 为 {@code remote_murasaki_bule}，绑定实体失效时强制清除粒子）。 */
    public static final String PHOTON_START_BLUE_COMMAND =
            "photon fx photon:remote_murasaki_bule entity @s 0 0 0 0 0 0 0.6 0.6 0.6 0 true false none";
    /** 起手赫球 Photon 命令（绑定实体失效时强制清除粒子）。 */
    public static final String PHOTON_START_RED_COMMAND =
            "photon fx photon:remote_murasaki_red entity @s 0 0 0 0 0 0 0.6 0.6 0.6 0 true false none";
    /** 起手合拢阶段每 tick 最大位移（格）。 */
    public static final double START_CONVERGE_SPEED_PER_TICK =
            START_SIDE_OFFSET / (START_DELAY_TICKS - START_CONVERGE_TICK);
    /** 发射动画 {@code remote_murasaki_fire} 等待时长（tick）。 */
    public static final int FIRE_DELAY_TICKS = 20 * 2;
    /** 发射前锚点相对脚底的高度比例。 */
    public static final double FIRE_ANCHOR_HEIGHT_RATIO = 0.45D;
    /** 发射前锚点位于玩家面前的距离（格）。 */
    public static final double FIRE_ANCHOR_FORWARD_DISTANCE = 5.0D;
    /** 蓄力与飞行共用同一特效实体的 Photon 命令。 */
    public static final String PHOTON_COMMAND = "photon fx photon:remote_murasaki_purple entity @s";
    /** 虚式茈箭飞行速度。 */
    public static final float ARROW_SPEED = 3.0F;
    /** 发射瞬间沿视线方向的前向偏移。 */
    public static final double ARROW_SPAWN_FORWARD = 1.2D;
    /** 发射瞬间 Y 偏移。 */
    public static final double ARROW_SPAWN_Y_OFFSET = 0.0D;
    /** 箭体球体判定半径：普通茈 {@link LimitlessPurpleConfig#ARROW_BLOCK_ERASE_RADIUS} 的 5 倍。 */
    public static final double ARROW_BLOCK_ERASE_RADIUS = 7.0D;
    /** 箭体球体每 tick 伤害。 */
    public static final float ARROW_SPHERE_DAMAGE_PER_TICK = 400.0F;
    /**
     * 【配置项·默认值】最大飞行距离（区块数）：64 区块。
     * <p>
     * 运行时真值由 {@code SkillCombatConfigRegistry#resolveRemoteMurasakiMaxFlightDistance} 给出，
     * ⛔ 业务代码不要直接引用本常量。
     */
    public static final int MAX_FLIGHT_CHUNK_COUNT = 64;
    /** 【配置项·默认值】最大飞行距离每升一级咒术等级的增量（区块数）。 */
    public static final double MAX_FLIGHT_CHUNK_COUNT_PER_RANK = 0.0D;
    public static final double MAX_FLIGHT_DISTANCE = SpecialEffectChunkForce.blocksFromChunks(MAX_FLIGHT_CHUNK_COUNT);
    /** 临时区块强制加载参数。 */
    public static final SpecialEffectChunkForce.Options CHUNK_FORCE_OPTIONS = SpecialEffectChunkForce.Options.defaults();
}
