package com.darkness.jujutsukaisenzone.technique.domain;

import com.darkness.jujutsukaisenzone.api.screen.ScreenShakeKind;
import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.compat.EpicFightCompat;
import com.darkness.jujutsukaisenzone.effect.TechniqueMeltdownEffect;
import com.darkness.jujutsukaisenzone.blockentity.JJKZSkyblockEntity;
import com.darkness.jujutsukaisenzone.entity.SpecialEffectEntity;
import com.darkness.jujutsukaisenzone.entity.combat.AiTechniqueStats;
import com.darkness.jujutsukaisenzone.network.ModNetwork;
import com.darkness.jujutsukaisenzone.player.PlayerBrainDamageData;
import com.darkness.jujutsukaisenzone.player.PlayerDomainModeData;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import com.darkness.jujutsukaisenzone.registry.ModEntities;
import com.darkness.jujutsukaisenzone.registry.ModSounds;
import com.darkness.jujutsukaisenzone.technique.scaling.LimitlessDomainSkillGrowth;
import com.darkness.jujutsukaisenzone.technique.se.LimitlessNearCasterExemptSeMechanic;
import com.darkness.jujutsukaisenzone.technique.se.SeMechanicRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.Advancement;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 无下限术式领域展开 - 无量空处。
 * <p>
 * 继承自 {@link AbstractTechniqueDomain}，实现无下限术式专属领域。
 * <p>
 * <b>核心契约：纯控制，零伤害</b>。无量空处是<b>完全的控制技能</b>——领域内只做
 * 搬点 / 朝南锁定 / 暂停 AI / 失明 / 乱码聊天 / 输入锁这一套控制，<b>绝不结算任何伤害</b>。
 * ⛔ 本类<b>不</b>得出现 {@code TechniqueDamageSources.deal} / {@code hurt} / 任何出伤调用；
 * 它也不经过 {@code TechniqueDamageZoneEvents} 的伤害乘区。想给无量空处加伤害 = 改玩法契约，先问。
 * <p>
 * <b>近身豁免</b>：领域<b>完成瞬间</b>处于施法者 {@link #EXCLUDE_CASTER_RADIUS} 格内的生物
 * 记入本次会话白名单，<b>整场领域</b>都不会被登记为控制目标（见 {@link #limitlessSessionMobWhitelist}）。
 * 例外：玩家装载「移除身旁豁免」SE（{@code LimitlessNearCasterExemptSeMechanic}）时豁免失效；
 * <b>AI 施法者永远保留豁免</b>（SE 系统只认玩家，见 {@code removesNearCasterExempt(DomainCaster)}）。
 * <p>
 * 特殊行为：
 * <ul>
 *   <li>按下领域展开键后等待 2 秒（40 tick）再展开领域结界</li>
 *   <li>按下领域展开键时在玩家位置播放领域展开音效</li>
 * </ul>
 */
public final class LimitlessDomainExpansion extends AbstractTechniqueDomain
{
    // ==================== 单例实例 ====================

    private static final LimitlessDomainExpansion INSTANCE = new LimitlessDomainExpansion();

    public static LimitlessDomainExpansion getInstance()
    {
        return INSTANCE;
    }

    // ==================== 配置参数 ====================

    /** 领域展开蓄力时间（tick），3 秒 = 60 tick */
    private static final int CAST_DELAY_TICKS = 60;
    /**
     * 开放式领域前摇时长（tick），4.5 秒 = 90 tick。
     * <p>
     * 开放式无壳、无构建，但不能与瞬发同为「无前摇秒开」；前摇期间<strong>不锁操作</strong>，只延后成型。
     */
    private static final int OPEN_DOMAIN_LEAD_IN_TICKS = 90;
    /** 白色滤镜总时长（tick），6 秒 = 120 tick（旧滤镜参数，保留供参考） */
    private static final int WHITE_OVERLAY_TOTAL_TICKS = 120;
    /**
     * Shader 白幕总时长：铺开约 2.1s；全白保持额外 1.7s（+34 tick）；开洞相对最初提速共 2 倍（66→17 tick）。
     * 合计 105 tick。与 {@code domain_white_curtain.fsh} 中 spreadEnd≈0.400、holdEnd≈0.838 对齐。
     */
    private static final int WHITE_CURTAIN_TOTAL_TICKS = WHITE_OVERLAY_TOTAL_TICKS + 34 - 49;
    /** 白色滤镜淡入时长（tick） */
    private static final int WHITE_OVERLAY_FADE_IN_TICKS = 12;
    /** 白色滤镜淡出时长（tick） */
    private static final int WHITE_OVERLAY_FADE_OUT_TICKS = 18;
    /** 白色滤镜峰值透明度（100%） */
    private static final float WHITE_OVERLAY_PEAK_ALPHA = 1.0F;
    /** 白色滤镜对周围玩家的影响半径（格） */
    private static final double WHITE_OVERLAY_RADIUS = 24.0D;
    /** 结界完成瞬间屏幕振动：持续客户端刻数与最大偏转角（度），同步范围同 {@link #WHITE_OVERLAY_RADIUS}。 */
    private static final int DOMAIN_COMPLETE_SCREEN_SHAKE_TICKS = 18;
    private static final float DOMAIN_COMPLETE_SCREEN_SHAKE_MAX_DEG = 2.5F;
    /** 结界完成 {@link #LOCK_PLAYER_DURATION_TICKS} 刻后全屏闪光参数（见 {@link ModNetwork#sendFullScreenWorldFlashAnimationSync}）。 */
    private static final int POST_DOMAIN_COMPLETION_FLASH_TOTAL_TICKS = 22;
    private static final int POST_DOMAIN_COMPLETION_FLASH_FADE_IN_TICKS = 3;
    private static final int POST_DOMAIN_COMPLETION_FLASH_FADE_OUT_TICKS = 14;
    private static final float POST_DOMAIN_COMPLETION_FLASH_PEAK_ALPHA = 1.0F;
    /** 球心环境音开始包同步半径（格），宜与 {@code sounds.json} 中 {@code attenuation_distance} 同量级。 */
    private static final double LIMITLESS_DOMAIN_AMBIENT_SOUND_SYNC_RADIUS = 64.0D;

    /** 领域名称 */
    private static final String DOMAIN_NAME = "无量空处";
    /** 无量空处内壁 / 地板天空盒层使用的纹理（对应 assets/.../textures/environment/stat_sky1）。 */
    private static final ResourceLocation LIMITLESS_DOMAIN_SKYBOX_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(JuJutsuKaisenZone.MODID, "textures/environment/stat_sky1");
    /** 结界完成 {@link #LOCK_PLAYER_DURATION_TICKS} 刻后切换为 stat_sky2（与演出锁定时长一致）。 */
    private static final ResourceLocation LIMITLESS_DOMAIN_SKYBOX_TEXTURE_AFTER_280 =
            ResourceLocation.fromNamespaceAndPath(JuJutsuKaisenZone.MODID, "textures/environment/stat_sky2");
    /** 无量空处完成后特效实体相对球心在 south 方向的距离（格） */
    private static final double LIGHT_LINE_SOUTH_DISTANCE = 22.0D;
    /** 结界完成 {@link #LOCK_PLAYER_DURATION_TICKS} 刻后球心水墨 Photon（绑定实体失效时强制清除粒子）。 */
    private static final String PHOTON_INKWASH_COMMAND =
            "photon fx photon:limitless_inkwash entity @s 0 0 0 0 0 0 1 1 1 0 true false none";
    /**
     * 瞬发水墨：瞬发领域球体更大（半径 32 vs 常规 24），整体放大到 1.35 倍。
     */
    private static final String PHOTON_INKWASH_COMMAND_INSTANT =
            "photon fx photon:limitless_inkwash entity @s 0 0 0 0 0 0 1.35 1.35 1.35 0 true false none";
    /**
     * 开放式水墨：开放式球体最大（半径 64），在此前按半径比取的 2.5 倍基础上再放大到 3.5 倍。
     */
    private static final String PHOTON_INKWASH_COMMAND_OPEN =
            "photon fx photon:limitless_inkwash entity @s 0 0 0 0 0 0 3.5 3.5 3.5 0 true false none";
    /** 常规无量空处 lightline（球心 south {@link #LIGHT_LINE_SOUTH_DISTANCE} 格，原始尺寸）。 */
    private static final String PHOTON_LIGHT_LINE_COMMAND =
            "photon fx photon:limitless_light_line entity @s";
    /** 开放式 lightline：球体更大，在此前 2 倍基础上再放大到 3 倍。 */
    private static final String PHOTON_LIGHT_LINE_COMMAND_OPEN =
            "photon fx photon:limitless_light_line entity @s 0 0 0 0 0 0 3 3 3 0 false false none";
    /** 开放式 lightline 锚点相对球心的 south 距离（格）；常规为 {@link #LIGHT_LINE_SOUTH_DISTANCE}（配半径 24）。 */
    private static final double OPEN_DOMAIN_LIGHT_LINE_SOUTH_DISTANCE = 44.0D;
    /** 开放式前摇内「扫描波」时点（自前摇开始流逝的 tick）。 */
    private static final int OPEN_DOMAIN_SCAN_WAVE_TICK = 20;
    /**
     * 扫描波总时长（tick）：前半程扩张、后半程收回。
     * <p>
     * 取 {@link #OPEN_DOMAIN_LEAD_IN_TICKS} - {@link #OPEN_DOMAIN_SCAN_WAVE_TICK}，波收完正好成型。
     */
    private static final int OPEN_DOMAIN_SCAN_WAVE_TOTAL_TICKS =
            OPEN_DOMAIN_LEAD_IN_TICKS - OPEN_DOMAIN_SCAN_WAVE_TICK;
    /**
     * 扫描波峰值半径（格）：取开放式领域半径（{@code OPEN_DOMAIN_RADIUS} = 64）的 2 倍。
     * <p>
     * 与领域半径一致时扫过范围观感偏小，故让它冲出领域边界一倍 —— 视觉上像结界扩张的气浪。
     */
    private static final float OPEN_DOMAIN_SCAN_WAVE_PEAK_RADIUS = 128.0F;
    /**
     * 开放式前摇内「电影上下黑条」时点（自前摇开始流逝的 tick）。
     * <p>
     * 演出次序：手势动画（t=0）→ 扫描波（{@link #OPEN_DOMAIN_SCAN_WAVE_TICK}）→ 黑条 → 白幕
     * （{@link #OPEN_DOMAIN_WHITE_CURTAIN_TICK}）→ 成型。
     */
    private static final int OPEN_DOMAIN_LETTERBOX_TICK = 28;
    /** 黑条演出总时长（tick）；需跨过白幕与成型时点，避免上下黑带中途消失。 */
    private static final int OPEN_DOMAIN_LETTERBOX_TOTAL_TICKS = 64;
    private static final int OPEN_DOMAIN_LETTERBOX_FADE_IN_TICKS = 10;
    private static final int OPEN_DOMAIN_LETTERBOX_FADE_OUT_TICKS = 20;
    /** 单侧黑条占屏幕高度的比例（上下各一条，故黑带合计约占 24%）。 */
    private static final float OPEN_DOMAIN_LETTERBOX_BAR_FRACTION = 0.12F;
    /** 开放式前摇内白幕开始铺开的时点（自前摇开始流逝的 tick）。 */
    private static final int OPEN_DOMAIN_WHITE_CURTAIN_TICK = 40;
    /** 完成演出时锁定施法者时长（tick） */
    private static final int LOCK_PLAYER_DURATION_TICKS = 280;
    /** Minecraft 世界坐标中 north 方向对应的固定 Yaw。 */
    private static final float NORTH_YAW = 180.0F;
    /** Minecraft 世界坐标中 south 方向对应的固定 Yaw。 */
    private static final float SOUTH_YAW = 0.0F;
    /** 传送锚点：球心 north 方向基础距离（格）。 */
    private static final double MOB_RELOCATE_NORTH_DISTANCE = 15.0D;
    /**
     * 近身豁免半径（格）：领域<b>完成瞬间</b>处于施法者该半径内的生物记入会话白名单，
     * 整场领域都不会被登记为控制目标（搬点 / 朝南 / 暂停 AI / 失明全部跳过）。
     * <p>
     * ⛔ 这是无量空处的<b>玩法契约</b>之一（另一条是「零伤害」），别当成「顺手加的保护」删掉。
     * 放宽 / 收紧它的唯一合法途径是那套「移除身旁豁免」SE（仅玩家）。
     */
    private static final double EXCLUDE_CASTER_RADIUS = 3.0D;
    /** 结界完成瞬间登记为领域锁定目标的生物数 {@literal >} 该值时触发「完美控制」反馈。 */
    private static final int PERFECT_CONTROL_LOCKED_TARGET_THRESHOLD = 5;
    /** 「完美控制」{@link ModSounds#SOUND_EFFECTS_IRON_SOUND} 相对默认播放音量（500%）。 */
    private static final float PERFECT_CONTROL_IRON_SOUND_VOLUME = 5.0F;
    /** 开放式：每 tick 在领域内地板随机刷出的白色尘屑数量。 */
    private static final int OPEN_DOMAIN_FLOOR_PARTICLES_PER_TICK = 28;
    /** 开放式地板白色尘屑缩放。 */
    private static final DustParticleOptions OPEN_DOMAIN_FLOOR_DUST =
            new DustParticleOptions(new Vector3f(1.0F, 1.0F, 1.0F), 1.15F);
    /** 生物分散摆放的最小间距（格）。 */
    private static final double MOB_SCATTER_SPACING = 1.75D;
    /** 领域关闭后延续锁定的最长时间（tick），90 秒。 */
    private static final int POST_DOMAIN_LOCK_MAX_TICKS = 90 * 20;
    /** 纯符号型乱码字符池。 */
    private static final char[] GARBLED_SYMBOL_CHARS = (
            "⍅⌇⍉⏚⋉⍟⎋⍜⍥⌿⍁⍂⍛⎅⌖⍢⍩⎇⍨⌾⍴⍶⋓⍫⍣⌌⌭⍊⍎⌁⌆⍼"
    ).toCharArray();
    /** 混合乱码（字母数字符号）字符池。 */
    private static final char[] GARBLED_MIXED_CHARS = (
            "ÃÇ¤ºªÆØßµñþæ¿¡€£¥§©®™†‡°•·¶⁄¡‹›ﬁﬂ‰‡·¶•‹›ﬁﬂ‡°·0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    ).toCharArray();
    /** 纯中文乱码（编码错乱风）字符池。 */
    private static final char[] GARBLED_CN_CHARS = (
            "鎻愬彇澶辫触锛岃绋嬪簭寮傚父鍐呭瓨涓嶈冻锛堬紵锛夊嵎璧锋潵"
    ).toCharArray();
    /** 程序崩溃风格固定片段。 */
    private static final String[] GARBLED_CRASH_LINES = new String[]{
            "0x7F8A3B2C: 0xDEADBEEF 0xCAFEBABE 0x0D15EA5E 0x8BADF00D",
            "▒▒▒▒▒▒▒▒▒▒ █▓▒░ █▓▒░ █▓▒░ [NULL][SIGSEGV]"
    };
    /** 乱码类型数量。 */
    private static final int GARBLED_STYLE_COUNT = 4;

    // ==================== 蓄力状态管理 ====================

    /** 正在蓄力中的玩家 UUID → 剩余 tick 数 */
    private final ConcurrentMap<UUID, Integer> castingDelayMap = new ConcurrentHashMap<>();
    /** 开放式前摇中的玩家 UUID → 剩余 tick 数（不锁操作，仅延后成型）。 */
    private final ConcurrentMap<UUID, Integer> openDomainLeadInMap = new ConcurrentHashMap<>();
    /** 完成演出后被锁定玩家 UUID → 锁定状态 */
    private final ConcurrentMap<UUID, PlayerLockState> playerLockMap = new ConcurrentHashMap<>();
    /** 领域内被锁定生物 UUID -> 锁定状态（直到该施法者领域关闭）。 */
    private final ConcurrentMap<UUID, MobLockState> mobLockMap = new ConcurrentHashMap<>();
    /**
     * 领域已关闭、但仍需延续锁定的目标（累计领域内锁定 tick ×3，上限 {@link #POST_DOMAIN_LOCK_MAX_TICKS}）。
     */
    private final ConcurrentMap<UUID, PostDomainMobLockState> postDomainMobLockMap = new ConcurrentHashMap<>();
    /**
     * 领域完成时处于施法者 {@link #EXCLUDE_CASTER_RADIUS} 格内、未被转移的非玩家生物：
     * 与施法者相同演出锁定 {@link #LOCK_PLAYER_DURATION_TICKS} tick（朝北、固定位姿、暂停 AI）。
     */
    private final ConcurrentMap<UUID, NearCasterMobLockState> nearCasterMobLockMap = new ConcurrentHashMap<>();
    /**
     * AI 实体施法者的演出姿态锁（施法者本人，不是目标）：UUID -> 锁定状态。
     * <p>
     * 玩家施法者的「朝北 + 定身」由 {@link #tickPlayerLockState(ServerPlayer)} 每 tick teleport 维持；
     * AI 实体没有 PlayerTick 通道，且自身 AI goal（{@code RandomLookAroundGoal} 等）会自由转头，
     * 若不加这一层锁，五条悟开领域时会朝向随机方向（常见即初始朝南）而非面朝被搬到北侧的受害者。
     * <p>
     * 时长：蓄力+构建不扣时；构建完成后再锁 {@link #LOCK_PLAYER_DURATION_TICKS}；锁定期间暂停 AI，到期恢复进入前的 {@code noAi} 状态。
     */
    private final ConcurrentMap<UUID, NearCasterMobLockState> aiCasterPoseLockMap = new ConcurrentHashMap<>();
    /**
     * 无量空处本次领域会话的施法目标白名单：领域<strong>完成瞬间</strong>在施法者 {@link #EXCLUDE_CASTER_RADIUS} 格内的生物 UUID。
     * 直至领域关闭前，这些实体永远不会被登记为领域锁定目标（与当前是否仍贴近施法者无关）。
     */
    private final ConcurrentMap<UUID, Set<UUID>> limitlessSessionMobWhitelist = new ConcurrentHashMap<>();
    /**
     * 施法者 UUID → 应将天空盒切换为 {@link #LIMITLESS_DOMAIN_SKYBOX_TEXTURE_AFTER_280} 的服务器 tick（含等于该 tick 时执行）。
     */
    private final ConcurrentMap<UUID, Integer> limitlessSkyboxSwapAtServerTick = new ConcurrentHashMap<>();
    /** 施法者 UUID → 允许再次按键解除 {@link #DOMAIN_NAME} 的最早服务器 tick（领域构建完成后再过 {@link #LOCK_PLAYER_DURATION_TICKS} 刻）。
     * 构建动画进行中另由 {@link BarrierInstance#isExpanding()} 拦截。
     */
    private final ConcurrentMap<UUID, Integer> limitlessDomainDismissUnlockServerTick = new ConcurrentHashMap<>();
    /**
     * 开放式：施法者 UUID →（目标 UUID → 域内停留 tick）。
     * 穿出或关域时结算一次后遗症。
     */
    private final ConcurrentMap<UUID, ConcurrentMap<UUID, Integer>> openDomainDwellByCaster = new ConcurrentHashMap<>();
    /** 开放式：施法者 UUID → 已结算过的目标 UUID（防重复）。 */
    private final ConcurrentMap<UUID, Set<UUID>> openDomainSettledByCaster = new ConcurrentHashMap<>();
    /** 用于每服务器 tick 只处理一次全服无量空处副作用（延续锁定、近身演出锁定等）。 */
    private static int lastPostDomainLockProcessedServerTick = -1;

    // ==================== 构造 ====================

    private LimitlessDomainExpansion()
    {
        setDomainName(DOMAIN_NAME);
    }

    // ==================== 实现抽象方法 ====================

    @Override
    protected void applyDomainSpecificEffect(ServerPlayer player, BarrierInstance instance)
    {
        /* 开放式：无壳、无搬点/朝南演出；仍对球内目标施加必中效果，并追踪穿出结算。 */
        if (instance.isOpenDomain())
        {
            beginOpenDomainSession(player, instance);
            return;
        }
        /* 瞬发：不搬生物、不上演出锁，只做天空盒与慢镜头。 */
        if (instance.isAutoCloseOnComplete() || instance.isSkyboxOnlyExpand())
        {
            return;
        }
        broadcastLimitlessDomainCompleteScreenShake(player, player.serverLevel());
        broadcastDomainWhiteCurtainElevate(player, player.serverLevel());
        ModNetwork.sendLimitlessDomainCmdCamSetupA(player, player.position());
        spawnLimitlessLightLineEntity(player, instance);
        populateLimitlessSessionMobWhitelist(player);
        int domainLockedTargets = relocateAndLockDomainMobs(player, instance);
        maybeAnnouncePerfectControl(player, instance, domainLockedTargets);
        lockSessionWhitelistedCasters(player);
        scheduleLimitlessSkyboxTextureSwapAfterCutscene(player);
        scheduleLimitlessDomainDismissUnlockAfterPerformance(player);
    }

    /**
     * 领域完成演出（施法者无关版）：AI 实体也走<b>完整</b>的真实无量空处演出。
     * <p>
     * 与玩家版的差别只有三处「玩家专属」：
     * <ul>
     *   <li><b>无 cmdcam 运镜</b>（AI 实体不占据任何客户端镜头）；</li>
     *   <li><b>无输入锁 / 无解除键</b>（AI 不会按键）；</li>
     *   <li>「完美控制」不授予成就、不弹聊天提示。</li>
     * </ul>
     * 其余全部保留：结界完成震屏、白幕提层、lightline 锚点、身旁白名单快照、搬点 + 朝南锁定、
     * 领域内乱码必中、280 刻后天空盒切换 + 水墨 + 全屏闪光 + 球心环境音。
     */
    @Override
    protected void applyDomainSpecificEffect(DomainCaster caster, BarrierInstance instance)
    {
        ServerPlayer player = caster.player();
        if (player != null)
        {
            applyDomainSpecificEffect(player, instance);
            return;
        }
        ServerLevel level = caster.level();
        if (level == null)
        {
            return;
        }
        /* 开放式 / 瞬发对 AI 不适用（AI 恒走常规），但保留判断以防未来扩展。 */
        if (instance.isOpenDomain())
        {
            beginOpenDomainSession(caster, instance);
            return;
        }
        if (instance.isAutoCloseOnComplete() || instance.isSkyboxOnlyExpand())
        {
            return;
        }
        broadcastLimitlessDomainCompleteScreenShakeAt(caster, level);
        broadcastDomainWhiteCurtainElevateAt(caster, level);
        /* 展开音效已在蓄力开场（{@link #cast(DomainCaster)}）播放，此处不重复。 */
        /* 无 cmdcam：AI 施法不占用玩家镜头。 */
        spawnLimitlessLightLineEntity(caster, instance);
        populateLimitlessSessionMobWhitelist(caster);
        int domainLockedTargets = relocateAndLockDomainMobs(caster, instance);
        maybeAnnouncePerfectControl(caster, instance, domainLockedTargets);
        lockSessionWhitelistedCasters(caster);
        scheduleLimitlessSkyboxTextureSwapAfterCutscene(caster);
    }

    @Override
    protected boolean hasLearnedTechnique(ServerPlayer player)
    {
        return PlayerTechniqueData.isLimitless(player);
    }

    /**
     * AI 实体施法者视为已习得「无量空处 · 领域展开」与「结界术」前置——
     * 前提是它已被赋予特级面板（{@code AiTechniqueStats}）与无量空处术式。
     * <p>
     * 有玩家时退回玩家存档判定，行为与既有一致。
     */
    @Override
    protected boolean hasLearnedDomainExpansion(DomainCaster caster)
    {
        return caster.player() != null ? super.hasLearnedDomainExpansion(caster) : AiTechniqueStats.has(caster.entity());
    }

    @Override
    protected boolean hasLearnedBarrierTechnique(DomainCaster caster)
    {
        return caster.player() != null ? super.hasLearnedBarrierTechnique(caster) : AiTechniqueStats.has(caster.entity());
    }

    /**
     * 瞬发展开过半：在球心补播水墨 Photon（常规走「完成演出后」那条，瞬发用放大 1.35 倍的命令），
     * 使瞬发也有水墨扩散；关域时随领域内特效实体一并清除。
     */
    @Override
    protected void onInstantDomainHalfProgress(ServerPlayer player, ServerLevel level, BarrierInstance instance)
    {
        spawnLimitlessInkwashAnchorAtSphereCenter(level, instance, PHOTON_INKWASH_COMMAND_INSTANT);
    }

    @Override
    public String getTechniqueType()
    {
        return "limitless_domain";
    }

    @Override
    protected ResourceLocation getDomainSkyboxTexture()
    {
        return LIMITLESS_DOMAIN_SKYBOX_TEXTURE;
    }

    @Override
    public ResourceLocation resolveInnateDomainSkyboxTexture(ServerPlayer player)
    {
        return LIMITLESS_DOMAIN_SKYBOX_TEXTURE_AFTER_280;
    }

    @Override
    protected double getMaintainCostReduction(ServerPlayer player)
    {
        return LimitlessDomainSkillGrowth.maintainCostReduction(player);
    }

    @Override
    protected void onBarrierClosed(ServerPlayer player, BarrierInstance instance)
    {
        ServerLevel level = player.getServer().getLevel(instance.getDimension());
        if (level == null)
        {
            return;
        }
        /*
         * 咒力耗尽 / 崩解 / 离开范围等非自愿解除，且在演出窗口内（展开动画未结束，或构建完成后未满 280 刻）：
         * 立刻解除施法者、白名单单位、领域内锁定目标的所有演出锁与客户端同步，并停止展开音效与蓄力白色滤镜。
         */
        if (isLimitlessForcedCloseDuringPerformance(player))
        {
            MinecraftServer server = level.getServer();
            if (server != null)
            {
                releaseAllLimitlessPerformanceLocks(player, server);
            }
            cancelLimitlessChargeClientPresentation(player);
        }
        broadcastLimitlessDomainAmbientSoundStop(level, player.getUUID());
        removeSpecialEffectEntitiesInsideBarrierSphere(level, instance);
        if (instance.isOpenDomain())
        {
            endOpenDomainSession(player, instance, true);
        }
        super.onBarrierClosed(player, instance);
    }

    /** 与 {@link #canDismissLimitlessDomainNow} 相反：仍处于「不可主动解除」的演出阶段时视为被迫关闭需清场。 */
    private boolean isLimitlessForcedCloseDuringPerformance(ServerPlayer player)
    {
        return !canDismissLimitlessDomainNow(player);
    }

    /**
     * 关闭回调（施法者无关版）。
     * <p>
     * AI 实体施法者走这里：**不能**只做「有玩家才转调」——否则 AI 关域时音效不停、领域内特效实体不清、
     * 白名单 / 搬点锁定不释放，会留下幽灵状态。因此 AI 分支自行完成全部世界侧清理。
     */
    @Override
    protected void onBarrierClosed(DomainCaster caster, BarrierInstance instance)
    {
        ServerPlayer player = caster.player();
        if (player != null)
        {
            onBarrierClosed(player, instance);
            return;
        }
        ServerLevel level = caster.level();
        if (level == null)
        {
            return;
        }
        MinecraftServer server = level.getServer();
        if (server != null)
        {
            releaseAllLimitlessPerformanceLocks(caster, server);
        }
        broadcastLimitlessDomainAmbientSoundStop(level, caster.uid());
        removeSpecialEffectEntitiesInsideBarrierSphere(level, instance);
        /* AI 走不到瞬发的 aftermath 分支，但这里保持与玩家版一致的清理链。 */
        super.onBarrierClosed(caster, instance);
    }

    /**
     * 解除无量空处本场演出的全部锁定：施法者镜头/移动/按键、白名单生物与玩家的 north 锁、领域转移目标的 south 锁。
     */
    private void releaseAllLimitlessPerformanceLocks(ServerPlayer caster, MinecraftServer server)
    {
        UUID casterUuid = caster.getUUID();
        ServerLevel serverLevel = caster.serverLevel();

        playerLockMap.remove(casterUuid);
        ModNetwork.sendPlayerInputLockSync(caster, false);

        Set<UUID> whitelist = limitlessSessionMobWhitelist.get(casterUuid);
        if (whitelist != null)
        {
            for (UUID targetId : whitelist)
            {
                if (playerLockMap.remove(targetId) != null)
                {
                    ServerPlayer sp = server.getPlayerList().getPlayer(targetId);
                    if (sp != null)
                    {
                        ModNetwork.sendPlayerInputLockSync(sp, false);
                    }
                }
                NearCasterMobLockState nearState = nearCasterMobLockMap.remove(targetId);
                if (nearState != null)
                {
                    releaseNearCasterMobLock(targetId, nearState, server);
                }
            }
        }

        List<UUID> mobToRemove = new ArrayList<>();
        for (Map.Entry<UUID, MobLockState> entry : mobLockMap.entrySet())
        {
            if (!entry.getValue().ownerUuid().equals(casterUuid))
            {
                continue;
            }
            releaseMobLock(entry.getKey(), entry.getValue(), serverLevel);
            mobToRemove.add(entry.getKey());
        }
        for (UUID id : mobToRemove)
        {
            mobLockMap.remove(id);
        }
    }

    /**
     * 解除全部演出锁定（施法者无关版）。AI 施法者没有自己的镜头 / 输入锁，但**同样**要释放：
     * 白名单单位的 north 锁、领域转移目标的 south 锁、以及区域内的玩家输入锁。
     */
    private void releaseAllLimitlessPerformanceLocks(DomainCaster caster, MinecraftServer server)
    {
        UUID casterUuid = caster.uid();
        ServerLevel serverLevel = caster.level();

        /* AI 施法者本人的姿态锁（朝北 + 定身）：领域关闭即解除，不必等演出计时自然到点。 */
        NearCasterMobLockState casterPose = aiCasterPoseLockMap.get(casterUuid);
        if (casterPose != null)
        {
            releaseAiCasterPoseLock(casterUuid, casterPose, server);
        }

        Set<UUID> whitelist = limitlessSessionMobWhitelist.get(casterUuid);
        if (whitelist != null)
        {
            for (UUID targetId : whitelist)
            {
                if (playerLockMap.remove(targetId) != null)
                {
                    ServerPlayer sp = server.getPlayerList().getPlayer(targetId);
                    if (sp != null)
                    {
                        ModNetwork.sendPlayerInputLockSync(sp, false);
                    }
                }
                NearCasterMobLockState nearState = nearCasterMobLockMap.remove(targetId);
                if (nearState != null)
                {
                    releaseNearCasterMobLock(targetId, nearState, server);
                }
            }
        }

        List<UUID> mobToRemove = new ArrayList<>();
        for (Map.Entry<UUID, MobLockState> entry : mobLockMap.entrySet())
        {
            if (!entry.getValue().ownerUuid().equals(casterUuid))
            {
                continue;
            }
            releaseMobLock(entry.getKey(), entry.getValue(), serverLevel);
            mobToRemove.add(entry.getKey());
        }
        for (UUID id : mobToRemove)
        {
            mobLockMap.remove(id);
        }
    }

    // ==================== 覆盖施放逻辑（延迟蓄力） ====================

    /**
     * 该施法者是否仍处于「已登记蓄力、尚未真正展开」的流程中。
     * <p>
     * 供 {@code AiDomainCasterRegistry.tickAll} 判断注销时机：无量空处先蓄力 60 刻才入
     * {@code ACTIVE_DOMAINS}，此期间 {@code hasActiveDomain} 恒 false，若据此注销会掐断蓄力。
     */
    public boolean isCastingPending(UUID casterUuid)
    {
        return casterUuid != null
                && (castingDelayMap.containsKey(casterUuid) || openDomainLeadInMap.containsKey(casterUuid));
    }

    @Override
    protected void configureDomainInstance(ServerPlayer player, BarrierInstance instance)
    {
        if (PlayerDomainModeData.isOpen(player))
        {
            instance.setOpenDomain(true);
            instance.setCustomSkyboxTexture(LIMITLESS_DOMAIN_SKYBOX_TEXTURE_AFTER_280);
            return;
        }
        if (!PlayerDomainModeData.isInstant(player))
        {
            return;
        }
        /* 瞬发：只展开天空盒，不放结界壳；铺完后保持完成态 2 秒再关。 */
        instance.setSkyboxOnlyExpand(true);
        instance.setAutoCloseOnComplete(true);
        /* 跳过演出，直接用演出后的第二套天空盒（stat_sky2）。 */
        instance.setCustomSkyboxTexture(LIMITLESS_DOMAIN_SKYBOX_TEXTURE_AFTER_280);
        /* 瞬发无蓄力阶段：展开瞬间补播无量空处领域手势动画。 */
        EpicFightCompat.playLimitlessDomainChargeAnimation(player);
    }

    @Override
    protected void configureDomainInstance(DomainCaster caster, BarrierInstance instance)
    {
        ServerPlayer player = caster.player();
        if (player != null)
        {
            configureDomainInstance(player, instance);
            return;
        }
        LivingEntity entity = caster.entity();
        if (AiDomainModeData.isOpen(entity))
        {
            instance.setOpenDomain(true);
            instance.setCustomSkyboxTexture(LIMITLESS_DOMAIN_SKYBOX_TEXTURE_AFTER_280);
            return;
        }
        if (!AiDomainModeData.isInstant(entity))
        {
            return;
        }
        instance.setSkyboxOnlyExpand(true);
        instance.setAutoCloseOnComplete(true);
        instance.setCustomSkyboxTexture(LIMITLESS_DOMAIN_SKYBOX_TEXTURE_AFTER_280);
        EpicFightCompat.playLimitlessDomainChargeAnimation(entity);
    }

    @Override
    protected void onInstantDomainHalfProgress(DomainCaster caster, ServerLevel level, BarrierInstance instance)
    {
        ServerPlayer player = caster.player();
        if (player != null)
        {
            onInstantDomainHalfProgress(player, level, instance);
            return;
        }
        spawnLimitlessInkwashAnchorAtSphereCenter(level, instance, PHOTON_INKWASH_COMMAND_INSTANT);
    }

    /**
     * 无量空处的施放逻辑：先进入蓄力状态，{@link #CAST_DELAY_TICKS} 后才真正展开领域。
     * <p>
     * <b>AI 实体施法者版</b>：与玩家版同一条蓄力前摇 —— 开场在施法者处播放
     * {@link ModSounds#LIMITLESS_DOMAIN_EXPANSION}，对周围玩家下发 shader 白幕
     * {@link #WHITE_CURTAIN_TOTAL_TICKS}，并登记 {@link #castingDelayMap} 蓄力倒计时，
     * 由 {@link #tickMaintain(DomainCaster)} AI 分支每 tick 驱动（{@link #tickCastingDelay(DomainCaster)}）。
     * <p>
     * 与玩家版的差别是玩家专属：解除键、取消文案。瞬发 / 开放式由 {@link AiDomainModeData} 选择，
     * 走与玩家同一套成型钩子。常规仍播 Epic Fight 蓄力手势
     * （{@link EpicFightCompat#playLimitlessDomainChargeAnimation(LivingEntity)}）。
     * 蓄力倒计时到点后照常走 {@code super.cast(caster)} 完成真实展开，因此 AI 也能被玩家看到完整的
     * 「白幕 → 蓄力 → 成型」过程。
     */
    @Override
    public void cast(DomainCaster caster)
    {
        ServerPlayer player = caster.player();
        if (player != null)
        {
            cast(player);
            return;
        }
        UUID uuid = caster.uid();
        ServerLevel level = caster.level();
        if (level == null)
        {
            return;
        }
        LivingEntity entity = caster.entity();
        if (AiDomainModeData.isInstant(entity))
        {
            if (castingDelayMap.containsKey(uuid))
            {
                castingDelayMap.remove(uuid);
                EpicFightCompat.stopLimitlessDomainChargeAnimation(entity);
            }
            if (openDomainLeadInMap.containsKey(uuid))
            {
                openDomainLeadInMap.remove(uuid);
                EpicFightCompat.stopLimitlessDomainChargeAnimation(entity);
            }
            if (DomainExpansionRouter.hasActiveDomain(uuid)
                    || DomainExpansionRouter.isPositionInAnyDomain(level, caster.blockPosition())
                    || !hasLearnedDomainExpansion(caster))
            {
                return;
            }
            Vec3 pos = caster.position();
            level.playSound(null, pos.x, pos.y, pos.z,
                    ModSounds.LIMITLESS_INSTANT_DOMAIN.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            super.cast(caster);
            if (entity != null && DomainExpansionRouter.hasActiveDomain(uuid))
            {
                AiDomainCasterRegistry.track(entity);
            }
            return;
        }
        if (AiDomainModeData.isOpen(entity))
        {
            if (openDomainLeadInMap.containsKey(uuid))
            {
                openDomainLeadInMap.remove(uuid);
                EpicFightCompat.stopLimitlessDomainChargeAnimation(entity);
                return;
            }
            if (castingDelayMap.containsKey(uuid))
            {
                castingDelayMap.remove(uuid);
                EpicFightCompat.stopLimitlessDomainChargeAnimation(entity);
            }
            if (DomainExpansionRouter.hasActiveDomain(uuid)
                    || DomainExpansionRouter.isPositionInAnyDomain(level, caster.blockPosition())
                    || !hasLearnedDomainExpansion(caster))
            {
                return;
            }
            openDomainLeadInMap.put(uuid, OPEN_DOMAIN_LEAD_IN_TICKS);
            Vec3 pos = caster.position();
            level.playSound(null, pos.x, pos.y, pos.z,
                    ModSounds.LIMITLESS_OPEN_DOMAIN.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            EpicFightCompat.playLimitlessDomainChargeAnimation(entity);
            if (entity != null)
            {
                AiDomainCasterRegistry.track(entity);
            }
            return;
        }
        /* 蓄力中再次施放 → 取消（与玩家版同口径，无音效/白幕残留清理由客户端超时兜底）。 */
        if (castingDelayMap.containsKey(uuid))
        {
            castingDelayMap.remove(uuid);
            return;
        }
        /* 前置校验：已在领域内 / 任何领域内 → 不进入蓄力（与玩家版提前检查一致）。 */
        if (DomainExpansionRouter.hasActiveDomain(uuid)
                || DomainExpansionRouter.isPositionInAnyDomain(level, caster.blockPosition())
                || !hasLearnedDomainExpansion(caster))
        {
            return;
        }
        /* 蓄力开场：施法者处播放展开音效 + 周围玩家 shader 白幕（与玩家版 t=0 完全同口径）。 */
        Vec3 pos = caster.position();
        level.playSound(null, pos.x, pos.y, pos.z,
                ModSounds.LIMITLESS_DOMAIN_EXPANSION.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        broadcastDomainWhiteCurtainAt(caster, level, WHITE_CURTAIN_TOTAL_TICKS);
        /* 与玩家同一条蓄力手势（LIMITLESS_DOMAIN_CHARGE）；无 LivingEntityPatch 时方法内跳过。 */
        EpicFightCompat.playLimitlessDomainChargeAnimation(caster.entity());
        /* AI 施法者演出锁：关 AI + 朝北定身；登记后由 tickMaintain(caster) AI 分支每 tick 维持。
         * 玩家版等价物是 playerLockMap + tickPlayerLockState，AI 无该通道故单列一张表。 */
        beginAiCasterPoseLock(caster, pos);
        castingDelayMap.put(uuid, CAST_DELAY_TICKS);
        /*
         * 立刻登记 AI 施法者，使 {@link AiDomainCasterRegistry#tickAll} 从本 tick 起驱动
         * {@link #tickMaintain(DomainCaster)} —— 蓄力倒计时（{@link #tickCastingDelay(DomainCaster)}）
         * 与压轴全挂在这条链上；等 60 刻后领域才入 ACTIVE_DOMAINS，若那时才 track 会先卡死在蓄力。
         */
        LivingEntity casterEntity = caster.entity();
        if (casterEntity != null)
        {
            AiDomainCasterRegistry.track(casterEntity);
        }
    }

    /**
     * 登记 AI 施法者演出锁：立即 {@code setNoAi(true)}，朝向锁到 {@link #NORTH_YAW}。
     * <p>
     * 蓄力与结界构建期间不扣剩余时长；构建完成后才开始 {@link #LOCK_PLAYER_DURATION_TICKS} 倒计时
     * （与玩家完成演出锁同窗口）。到期恢复进入前的 {@code noAi}。
     */
    private void beginAiCasterPoseLock(DomainCaster caster, Vec3 lockPos)
    {
        LivingEntity entity = caster.entity();
        if (entity == null)
        {
            return;
        }
        boolean originNoAi = false;
        boolean aiPausedByThisLock = false;
        if (entity instanceof Mob mob)
        {
            originNoAi = mob.isNoAi();
            if (!originNoAi)
            {
                mob.setNoAi(true);
                aiPausedByThisLock = true;
            }
        }
        aiCasterPoseLockMap.put(caster.uid(), new NearCasterMobLockState(
                lockPos, entity.getXRot(), LOCK_PLAYER_DURATION_TICKS, aiPausedByThisLock, originNoAi));
        applyCasterStyleLockToNonPlayerLiving(entity, lockPos, entity.getXRot());
    }

    /** 每 tick 维持 AI 施法者演出锁；到期后释放（恢复进入前的 {@code noAi} 状态）。 */
    private void tickAiCasterPoseLock(DomainCaster caster)
    {
        NearCasterMobLockState state = aiCasterPoseLockMap.get(caster.uid());
        if (state == null)
        {
            return;
        }
        LivingEntity entity = caster.entity();
        if (entity == null || !entity.isAlive())
        {
            releaseAiCasterPoseLock(caster.uid(), state, caster.server());
            return;
        }
        if (entity instanceof Mob mob && state.aiPausedByThisLock())
        {
            mob.setNoAi(true);
        }
        applyCasterStyleLockToNonPlayerLiving(entity, state.lockPos(), state.lockXRot());
        if (isAiCasterPerformanceWindup(caster.uid()))
        {
            return;
        }
        int remainingAfter = state.remainingTicks() - 1;
        if (remainingAfter <= 0)
        {
            releaseAiCasterPoseLock(caster.uid(), state, caster.server());
        }
        else
        {
            aiCasterPoseLockMap.put(caster.uid(), state.withRemainingTicks(remainingAfter));
        }
    }

    /** 蓄力尚未落地、或结界仍在构建：演出未完成，不扣姿态锁剩余时长。 */
    private boolean isAiCasterPerformanceWindup(UUID casterUuid)
    {
        if (casterUuid == null)
        {
            return false;
        }
        if (castingDelayMap.containsKey(casterUuid) || openDomainLeadInMap.containsKey(casterUuid))
        {
            return true;
        }
        java.util.Deque<BarrierInstance> queue = ACTIVE_DOMAINS.get(casterUuid);
        if (queue == null || queue.isEmpty())
        {
            return false;
        }
        for (BarrierInstance instance : queue)
        {
            if (instance != null && instance.isExpanding())
            {
                return true;
            }
        }
        return false;
    }

    /** 释放 AI 施法者姿态锁：恢复 AI（仅恢复本锁主动改动的对象）。 */
    private void releaseAiCasterPoseLock(UUID casterUuid, NearCasterMobLockState state, MinecraftServer server)
    {
        aiCasterPoseLockMap.remove(casterUuid);
        if (server == null)
        {
            return;
        }
        if (findLivingEntity(server, casterUuid) instanceof Mob mob && state.aiPausedByThisLock())
        {
            mob.setNoAi(state.originNoAi());
        }
    }

    /**
     * 无量空处的施放逻辑：先进入蓄力状态，2 秒后才真正展开领域。
     * <p>
     * 按下领域展开键时：
     * <ol>
     *   <li>在玩家位置播放领域展开音效</li>
     *   <li>进入蓄力状态，显示蓄力提示</li>
     *   <li>蓄力期间每 tick 由 {@link #tickCastingDelay(ServerPlayer)} 驱动倒计时</li>
     *   <li>蓄力期间再次按领域键可取消本次展开</li>
     *   <li>蓄力完成后调用 {@code super.cast(player)} 真正展开领域</li>
     * </ol>
     * 如果玩家已在自己的领域内，立即解除领域（不经过蓄力）。
     * <p>
     * 瞬发模式：跳过蓄力与领域演出，只播天空盒展开（不放结界壳方块）；
     * 构建完成后保持约 2 秒，期间对半径 32 内全体生物施加隐藏最高档缓慢与挖掘疲劳，再自动关闭。
     */
    @Override
    public void cast(ServerPlayer player)
    {
        if (!player.isAlive() || !isInBattleMode(player))
        {
            return;
        }

        /* 如果玩家已在自己领域内 → 直接解除，不需要蓄力 */
        if (isPlayerInsideOwnDomain(player))
        {
            if (!canDismissLimitlessDomainNow(player))
            {
                player.displayClientMessage(
                        Component.literal("「" + DOMAIN_NAME + "」暂无法解除").withStyle(ChatFormatting.RED),
                        true);
                return;
            }
            super.cast(player);
            return;
        }

        UUID uuid = player.getUUID();

        if (TechniqueMeltdownEffect.blockDomainExpansion(player))
        {
            return;
        }

        /* 瞬发：跳过蓄力与演出，直接走结界展开 */
        if (PlayerDomainModeData.isInstant(player))
        {
            if (castingDelayMap.containsKey(uuid))
            {
                castingDelayMap.remove(uuid);
                EpicFightCompat.stopLimitlessDomainChargeAnimation(player);
                cancelLimitlessChargeClientPresentation(player);
            }
            if (!canBeginLimitlessDomainCast(player))
            {
                return;
            }
            player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.LIMITLESS_INSTANT_DOMAIN.get(), SoundSource.PLAYERS,
                    1.0F, 1.0F);
            super.cast(player);
            return;
        }

        /*
         * 开放式：无壳、无构建，但不能与瞬发同为「无前摇秒开」。
         * 走一段不锁操作的前摇（仅延后成型），倒计时由 {@link #tickOpenDomainLeadIn} 驱动。
         */
        if (PlayerDomainModeData.isOpen(player))
        {
            /* 前摇中再次按下领域键 → 取消本次展开 */
            if (openDomainLeadInMap.containsKey(uuid))
            {
                openDomainLeadInMap.remove(uuid);
                EpicFightCompat.stopLimitlessDomainChargeAnimation(player);
                stopOpenDomainLeadInSound(player);
                player.displayClientMessage(
                        Component.literal("「" + DOMAIN_NAME + "」已取消")
                                .withStyle(ChatFormatting.YELLOW),
                        true);
                return;
            }
            if (castingDelayMap.containsKey(uuid))
            {
                castingDelayMap.remove(uuid);
                EpicFightCompat.stopLimitlessDomainChargeAnimation(player);
                cancelLimitlessChargeClientPresentation(player);
            }
            if (!canBeginLimitlessDomainCast(player))
            {
                return;
            }
            openDomainLeadInMap.put(uuid, OPEN_DOMAIN_LEAD_IN_TICKS);
            player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.LIMITLESS_OPEN_DOMAIN.get(), SoundSource.PLAYERS,
                    1.0F, 1.0F);
            EpicFightCompat.playLimitlessDomainChargeAnimation(player);
            return;
        }

        /* 蓄力中再次按下领域键 → 取消本次展开 */
        if (castingDelayMap.containsKey(uuid))
        {
            castingDelayMap.remove(uuid);
            EpicFightCompat.stopLimitlessDomainChargeAnimation(player);
            cancelLimitlessChargeClientPresentation(player);
            player.displayClientMessage(
                    Component.literal("「" + DOMAIN_NAME + "」已取消")
                            .withStyle(ChatFormatting.YELLOW),
                    true);
            return;
        }

        /* 前置条件校验（与父类 cast 一致，但提前检查以避免蓄力后失败） */
        if (DomainExpansionRouter.isPositionInAnyDomain(player.serverLevel(), player.blockPosition()))
        {
            player.displayClientMessage(Component.literal("无法展开领域，因为领域对抗还没做").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (!hasLearnedDomainExpansion(player))
        {
            player.displayClientMessage(Component.literal("尚未习得领域展开").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (!hasLearnedTechnique(player))
        {
            return;
        }

        /* 播放领域展开音效 */
        ServerLevel level = player.serverLevel();
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.LIMITLESS_DOMAIN_EXPANSION.get(), SoundSource.PLAYERS,
                1.0F, 1.0F);

        /*
         * 蓄力开场白幕：用 DomainWhiteCurtain shader 代替旧 WhiteOverlay 滤镜。
         * 影响范围同原先：施法者周围 WHITE_OVERLAY_RADIUS 格内玩家。
         */
        broadcastDomainWhiteCurtain(player, WHITE_CURTAIN_TOTAL_TICKS);

        /* 进入蓄力状态 */
        castingDelayMap.put(uuid, CAST_DELAY_TICKS);
        EpicFightCompat.playLimitlessDomainChargeAnimation(player);
        /*player.displayClientMessage(
                 Component.literal("「" + DOMAIN_NAME + "」展开蓄力中...")
                        .withStyle(ChatFormatting.LIGHT_PURPLE), true);*/
    }

    /**
     * 瞬发 / 开放式共用的施放前置校验（与父类 {@code cast} 同源，但提前检查以免白等一场前摇）。
     *
     * @return 校验通过返回 {@code true}；失败时已向玩家发送提示
     */
    private boolean canBeginLimitlessDomainCast(ServerPlayer player)
    {
        if (DomainExpansionRouter.isPositionInAnyDomain(player.serverLevel(), player.blockPosition()))
        {
            player.displayClientMessage(Component.literal("无法展开领域，因为领域对抗还没做").withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (!hasLearnedDomainExpansion(player))
        {
            player.displayClientMessage(Component.literal("尚未习得领域展开").withStyle(ChatFormatting.RED), true);
            return false;
        }
        return hasLearnedTechnique(player);
    }

    /**
     * 向施法者周围 {@link #WHITE_OVERLAY_RADIUS} 格内玩家同步白幕 shader。
     * <p>
     * 常规走蓄力开场（t=0），开放式走前摇中段（{@link #OPEN_DOMAIN_WHITE_CURTAIN_TICK}），两处共用。
     */
    private static void broadcastDomainWhiteCurtain(ServerPlayer caster, int durationTicks)
    {
        double radiusSqr = WHITE_OVERLAY_RADIUS * WHITE_OVERLAY_RADIUS;
        for (ServerPlayer nearby : caster.serverLevel().players())
        {
            if (nearby.distanceToSqr(caster) > radiusSqr)
            {
                continue;
            }
            ModNetwork.sendDomainWhiteCurtainSync(nearby, durationTicks);
        }
    }

    /** 白幕 shader（施法者无关版）：AI 实体蓄力开场同样对周围玩家下发。 */
    private static void broadcastDomainWhiteCurtainAt(DomainCaster caster, ServerLevel level, int durationTicks)
    {
        double radiusSqr = WHITE_OVERLAY_RADIUS * WHITE_OVERLAY_RADIUS;
        Vec3 center = caster.position();
        for (ServerPlayer nearby : level.players())
        {
            if (nearby.distanceToSqr(center.x, center.y, center.z) > radiusSqr)
            {
                continue;
            }
            ModNetwork.sendDomainWhiteCurtainSync(nearby, durationTicks);
        }
    }

    /**
     * 向施法者周围 {@link #WHITE_OVERLAY_RADIUS} 格内玩家同步「电影上下黑条」演出（开放式前摇
     * {@link #OPEN_DOMAIN_LETTERBOX_TICK} 时点）。
     * <p>
     * 纯客户端画面效果，<strong>不锁操作</strong>；范围口径与 {@link #broadcastDomainWhiteCurtain} 一致。
     * 手势图仅发给施术者本人（与黑条同进退、同淡入淡出）。
     */
    private static void broadcastCinematicLetterbox(ServerPlayer caster)
    {
        double radiusSqr = WHITE_OVERLAY_RADIUS * WHITE_OVERLAY_RADIUS;
        for (ServerPlayer nearby : caster.serverLevel().players())
        {
            if (nearby.distanceToSqr(caster) > radiusSqr)
            {
                continue;
            }
            ModNetwork.sendCinematicLetterboxSync(
                    nearby,
                    OPEN_DOMAIN_LETTERBOX_TOTAL_TICKS,
                    OPEN_DOMAIN_LETTERBOX_FADE_IN_TICKS,
                    OPEN_DOMAIN_LETTERBOX_FADE_OUT_TICKS,
                    OPEN_DOMAIN_LETTERBOX_BAR_FRACTION,
                    nearby.getUUID().equals(caster.getUUID()));
        }
    }

    /**
     * 向施法者周围 {@link #WHITE_OVERLAY_RADIUS} 格内玩家同步「扫描波」演出（开放式前摇
     * {@link #OPEN_DOMAIN_SCAN_WAVE_TICK} 时点）。
     * <p>
     * <strong>球心由客户端每帧跟踪施法者脚底</strong>（随包发送施法者 UUID）：前摇期间不锁操作、
     * 玩家可自由移动，只发一次快照的话人一走波心就留在原地，看起来「不是从脚下发出的」。
     * 这里的 {@code centerX/Y/Z} 只是兜底值 —— 施法者不在客户端实体列表时使用。
     * <p>
     * 取脚底而非身体中心：球心所在水平面就是客户端认定的「地面」，抬到身体中心会让环浮在膝盖高度。
     */
    private static void broadcastDomainScanWave(ServerPlayer caster)
    {
        double centerX = caster.getX();
        double centerY = caster.getY();
        double centerZ = caster.getZ();
        double radiusSqr = WHITE_OVERLAY_RADIUS * WHITE_OVERLAY_RADIUS;
        for (ServerPlayer nearby : caster.serverLevel().players())
        {
            if (nearby.distanceToSqr(caster) > radiusSqr)
            {
                continue;
            }
            ModNetwork.sendDomainScanWaveSync(
                    nearby,
                    caster.getUUID(),
                    centerX,
                    centerY,
                    centerZ,
                    OPEN_DOMAIN_SCAN_WAVE_TOTAL_TICKS,
                    OPEN_DOMAIN_SCAN_WAVE_PEAK_RADIUS);
        }
    }

    private static void broadcastDomainScanWaveAt(DomainCaster caster, ServerLevel level)
    {
        Vec3 pos = caster.position();
        double radiusSqr = WHITE_OVERLAY_RADIUS * WHITE_OVERLAY_RADIUS;
        for (ServerPlayer nearby : level.players())
        {
            if (nearby.distanceToSqr(pos.x, pos.y, pos.z) > radiusSqr)
            {
                continue;
            }
            ModNetwork.sendDomainScanWaveSync(
                    nearby,
                    caster.uid(),
                    pos.x,
                    pos.y,
                    pos.z,
                    OPEN_DOMAIN_SCAN_WAVE_TOTAL_TICKS,
                    OPEN_DOMAIN_SCAN_WAVE_PEAK_RADIUS);
        }
    }

    private static void broadcastCinematicLetterboxAt(DomainCaster caster, ServerLevel level)
    {
        Vec3 pos = caster.position();
        double radiusSqr = WHITE_OVERLAY_RADIUS * WHITE_OVERLAY_RADIUS;
        for (ServerPlayer nearby : level.players())
        {
            if (nearby.distanceToSqr(pos.x, pos.y, pos.z) > radiusSqr)
            {
                continue;
            }
            ModNetwork.sendCinematicLetterboxSync(
                    nearby,
                    OPEN_DOMAIN_LETTERBOX_TOTAL_TICKS,
                    OPEN_DOMAIN_LETTERBOX_FADE_IN_TICKS,
                    OPEN_DOMAIN_LETTERBOX_FADE_OUT_TICKS,
                    OPEN_DOMAIN_LETTERBOX_BAR_FRACTION,
                    false);
        }
    }

    /**
     * 开放式前摇取消 / 中断时：停止 {@link ModSounds#LIMITLESS_OPEN_DOMAIN}。
     */
    private static void stopOpenDomainLeadInSound(ServerPlayer caster)
    {
        ClientboundStopSoundPacket stop = new ClientboundStopSoundPacket(
                ModSounds.LIMITLESS_OPEN_DOMAIN.get().getLocation(),
                SoundSource.PLAYERS);
        for (ServerPlayer sp : caster.serverLevel().players())
        {
            sp.connection.send(stop);
        }
    }

    /**
     * 蓄力取消时：停止 {@link ModSounds#LIMITLESS_DOMAIN_EXPANSION} 在客户端的播放，并清除与开始蓄力时同范围玩家的白色滤镜。
     */
    private static void cancelLimitlessChargeClientPresentation(ServerPlayer caster)
    {
        EpicFightCompat.stopLimitlessDomainChargeAnimation(caster);
        ServerLevel level = caster.serverLevel();
        ClientboundStopSoundPacket stopExpansion = new ClientboundStopSoundPacket(
                ModSounds.LIMITLESS_DOMAIN_EXPANSION.get().getLocation(),
                SoundSource.PLAYERS);
        for (ServerPlayer sp : level.players())
        {
            sp.connection.send(stopExpansion);
        }
        double radiusSqr = WHITE_OVERLAY_RADIUS * WHITE_OVERLAY_RADIUS;
        for (ServerPlayer nearby : level.players())
        {
            if (nearby.distanceToSqr(caster) > radiusSqr)
            {
                continue;
            }
            ModNetwork.sendWhiteOverlaySync(nearby, false, 0.0F);
        }
    }

    // ==================== 蓄力 tick 驱动 ====================

    /**
     * 每tick驱动蓄力倒计时，蓄力完成后真正展开领域。
     * <p>
     * 应在 {@link #tickMaintain(ServerPlayer)} 中调用。
     *
     * @param player 玩家
     */
    public void tickCastingDelay(ServerPlayer player)
    {
        tickCastingDelay(DomainCaster.of(player));
    }

    /**
     * 每 tick 驱动蓄力倒计时（施法者无关版）：AI 实体施法者也走同一条前摇。
     * <p>
     * 与玩家版的差别只有「玩家专属」：不播 / 不停 Epic Fight 蓄力动画、不弹中断文案。
     *
     * @param caster 施法者（玩家或 AI 实体）
     */
    public void tickCastingDelay(DomainCaster caster)
    {
        UUID uuid = caster.uid();
        Integer remaining = castingDelayMap.get(uuid);
        if (remaining == null)
        {
            return;
        }

        /* 蓄力期间施法者死亡或脱离战斗模式 → 中断蓄力 */
        if (!caster.alive() || !caster.inBattleMode())
        {
            castingDelayMap.remove(uuid);
            ServerPlayer casterPlayer = caster.player();
            if (casterPlayer != null)
            {
                EpicFightCompat.stopLimitlessDomainChargeAnimation(casterPlayer);
                casterPlayer.displayClientMessage(
                        Component.literal("「无量空处」展开蓄力中断")
                                .withStyle(ChatFormatting.RED), true);
            }
            else if (caster.entity() != null)
            {
                EpicFightCompat.stopLimitlessDomainChargeAnimation(caster.entity());
                /* 蓄力中断（AI 未开成领域，不会走 onBarrierClosed 的释放链）→ 立即恢复其 AI 姿态。 */
                NearCasterMobLockState casterPose = aiCasterPoseLockMap.get(uuid);
                if (casterPose != null)
                {
                    releaseAiCasterPoseLock(uuid, casterPose, caster.server());
                }
            }
            return;
        }

        int newRemaining = remaining - 1;
        if (newRemaining <= 0)
        {
            castingDelayMap.remove(uuid);
            /*
             * 不在此处 stop Epic Fight 蓄力动画：剪辑可长于蓄力时长（例如 8s JSON），否则会在约 CAST_DELAY_TICKS 处被截断并显得前半段「极慢」。
             */
            /* 蓄力完成，调用父类 cast 真正展开领域 */
            super.cast(caster);
        }
        else
        {
            castingDelayMap.put(uuid, newRemaining);
        }
    }

    // ==================== 开放式前摇倒计时 ====================

    /**
     * 每 tick 驱动开放式前摇倒计时；到点后调用父类 {@code cast}，此时才真正成型。
     * <p>
     * 前摇期间不锁移动 / 输入 / 镜头，仅在玩家死亡或脱离战斗模式时中断。
     */
    private void tickOpenDomainLeadIn(ServerPlayer player)
    {
        UUID uuid = player.getUUID();
        Integer remaining = openDomainLeadInMap.get(uuid);
        if (remaining == null)
        {
            return;
        }

        /* 前摇期间死亡、脱离战斗模式或切走开放式 → 中断（与蓄力倒计时同口径） */
        if (!player.isAlive() || !isInBattleMode(player) || !PlayerDomainModeData.isOpen(player))
        {
            openDomainLeadInMap.remove(uuid);
            EpicFightCompat.stopLimitlessDomainChargeAnimation(player);
            stopOpenDomainLeadInSound(player);
            player.displayClientMessage(
                    Component.literal("「" + DOMAIN_NAME + "」展开中断")
                            .withStyle(ChatFormatting.RED), true);
            return;
        }

        int newRemaining = remaining - 1;
        if (newRemaining <= 0)
        {
            openDomainLeadInMap.remove(uuid);
            /*
             * 不在此处停 Epic Fight 动画：手势剪辑可长于前摇，提前截断会显得前半段「极慢」
             * （与 {@link #tickCastingDelay} 同一取舍）。
             */
            super.cast(player);
            /* 成型演出：cast 可能因咒力 / 量表 / 脑损不足而失败，故先确认真的建出了实例。 */
            BarrierInstance instance = currentOpenDomainInstance(player);
            if (instance != null)
            {
                playOpenDomainCompletionPresentation(player, instance);
            }
            return;
        }

        openDomainLeadInMap.put(uuid, newRemaining);
        /* 前摇内演出时点（自前摇开始流逝的 tick）。 */
        int elapsed = OPEN_DOMAIN_LEAD_IN_TICKS - newRemaining;
        if (elapsed == OPEN_DOMAIN_SCAN_WAVE_TICK)
        {
            playOpenDomainScanWave(player);
        }
        if (elapsed == OPEN_DOMAIN_LETTERBOX_TICK)
        {
            broadcastCinematicLetterbox(player);
        }
        if (elapsed == OPEN_DOMAIN_WHITE_CURTAIN_TICK)
        {
            broadcastDomainWhiteCurtain(player, WHITE_CURTAIN_TOTAL_TICKS);
        }
    }

    /**
     * AI 开放式前摇：与玩家同一倒计时与演出时点；中断条件为死亡或切走开放式（无战斗模式）。
     */
    private void tickOpenDomainLeadIn(DomainCaster caster)
    {
        UUID uuid = caster.uid();
        Integer remaining = openDomainLeadInMap.get(uuid);
        if (remaining == null)
        {
            return;
        }
        LivingEntity entity = caster.entity();
        ServerLevel level = caster.level();
        if (entity == null || !entity.isAlive() || level == null || !AiDomainModeData.isOpen(entity))
        {
            openDomainLeadInMap.remove(uuid);
            EpicFightCompat.stopLimitlessDomainChargeAnimation(entity);
            return;
        }

        int newRemaining = remaining - 1;
        if (newRemaining <= 0)
        {
            openDomainLeadInMap.remove(uuid);
            super.cast(caster);
            BarrierInstance instance = currentOpenDomainInstance(uuid);
            if (instance != null)
            {
                playOpenDomainCompletionPresentation(caster, instance);
            }
            return;
        }

        openDomainLeadInMap.put(uuid, newRemaining);
        int elapsed = OPEN_DOMAIN_LEAD_IN_TICKS - newRemaining;
        if (elapsed == OPEN_DOMAIN_SCAN_WAVE_TICK)
        {
            broadcastDomainScanWaveAt(caster, level);
        }
        if (elapsed == OPEN_DOMAIN_LETTERBOX_TICK)
        {
            broadcastCinematicLetterboxAt(caster, level);
        }
        if (elapsed == OPEN_DOMAIN_WHITE_CURTAIN_TICK)
        {
            broadcastDomainWhiteCurtainAt(caster, level, WHITE_CURTAIN_TOTAL_TICKS);
        }
    }

    /** 当前活跃的开放式领域实例；没有（或不是开放式）时返回 {@code null}。 */
    private BarrierInstance currentOpenDomainInstance(ServerPlayer player)
    {
        return currentOpenDomainInstance(player.getUUID());
    }

    private BarrierInstance currentOpenDomainInstance(UUID casterUuid)
    {
        java.util.Deque<BarrierInstance> queue = ACTIVE_DOMAINS.get(casterUuid);
        if (queue == null || queue.isEmpty())
        {
            return null;
        }
        BarrierInstance instance = queue.peekLast();
        return instance != null && instance.isOpenDomain() ? instance : null;
    }

    /**
     * 开放式成型瞬间的演出：白幕提层 + lightline（放大）+ 水墨（放大）。
     * <p>
     * 与常规不同，<strong>不锁施法者</strong>：只放锚点，不写 {@code playerLockMap}、不发输入锁。
     */
    private void playOpenDomainCompletionPresentation(ServerPlayer player, BarrierInstance instance)
    {
        playOpenDomainCompletionPresentation(DomainCaster.of(player), instance);
    }

    private void playOpenDomainCompletionPresentation(DomainCaster caster, BarrierInstance instance)
    {
        ServerLevel level = caster.level();
        if (level == null)
        {
            return;
        }
        broadcastDomainWhiteCurtainElevateAt(caster, level);
        spawnLightLineAnchor(caster, instance, OPEN_DOMAIN_LIGHT_LINE_SOUTH_DISTANCE, PHOTON_LIGHT_LINE_COMMAND_OPEN);
        spawnLimitlessInkwashAnchorAtSphereCenter(level, instance, PHOTON_INKWASH_COMMAND_OPEN);
    }

    /**
     * 开放式前摇内「扫描波」时点（{@link #OPEN_DOMAIN_SCAN_WAVE_TICK}）：
     * 以施法者为球心播一次「扩张 → 收回」的双线白环（客户端 {@code DomainScanWaveRenderer}）。
     * <p>
     * 纯画面效果，不锁操作；球体不做折射 / 补色，也不被地形遮挡 —— 详见 shader 注释。
     */
    private void playOpenDomainScanWave(ServerPlayer player)
    {
        broadcastDomainScanWave(player);
    }

    /**
     * 检查指定玩家是否正在施放中（蓄力中或开放式前摇中）。
     */
    public boolean isCasting(ServerPlayer player)
    {
        return castingDelayMap.containsKey(player.getUUID())
                || openDomainLeadInMap.containsKey(player.getUUID());
    }

    /**
     * 清除指定玩家的施放态（蓄力 / 开放式前摇）；用于玩家断线等情况。
     */
    public void clearCastingState(UUID uuid)
    {
        castingDelayMap.remove(uuid);
        openDomainLeadInMap.remove(uuid);
    }

    // ==================== 会话生命周期 ====================

    /**
     * 服务器停机兜底：除基类状态外，一并清掉本实现按 UUID 记录的施放态容器
     * （蓄力倒计时 / 开放式前摇倒计时 —— 二者都挂在进程级静态单例上，退出世界不会自行回收）。
     */
    @Override
    public void resetSessionState()
    {
        castingDelayMap.clear();
        openDomainLeadInMap.clear();
        aiCasterPoseLockMap.clear();
        super.resetSessionState();
    }

    /**
     * 玩家登出：先清施放态，再走基类的领域关闭。
     * <p>
     * 仅处于开放式前摇中（尚无领域实例、活跃表里没有条目）的玩家不会进入基类的关闭循环，
     * 故必须在进入基类逻辑前先把倒计时清掉，否则重登后首次按领域键会被残留条目当成「取消」。
     */
    @Override
    public void closeAllForPlayer(ServerPlayer player)
    {
        clearCastingState(player.getUUID());
        super.closeAllForPlayer(player);
    }

    // ==================== 覆盖 tickMaintain 以驱动蓄力 ====================

    @Override
    public void tickMaintain(ServerPlayer player)
    {
        tickMaintainPlayerSide(player);
    }

    /**
     * 玩家侧维持实体（{@link ServerPlayer} 与 {@link DomainCaster} 两条入口共用）。
     * <p>
     * ⛔ 抽出本方法是为了切断一条<b>虚调用环</b>：
     * {@code AbstractDomainExpansion.tickMaintain(ServerPlayer)} 会转发到
     * {@code tickMaintain(DomainCaster)}（基类第 772 行）；若子类的 {@code tickMaintain(DomainCaster)}
     * 玩家分支再回调 {@code tickMaintain(ServerPlayer)}，就会形成
     * 「DomainCaster 版 → ServerPlayer 版 → 基类 → DomainCaster 版」的无限递归。
     * 两条入口都直达本方法即可避免回环。
     */
    private void tickMaintainPlayerSide(ServerPlayer player)
    {
        tickPostDomainLocksOncePerServerTick(player);
        boolean instant = hasInstantDomainInstance(player);
        boolean open = hasOpenDomainInstance(player);
        /* 瞬发：整段演出/锁定都跳过。开放式：跳过搬点与施法者演出，但仍维持域内必中效果。 */
        if (!instant && !open)
        {
            tickPlayerLockState(player);
            captureNewDomainIntruders(player);
        }
        if (!instant)
        {
            tickLockedMobs(player);
        }
        /* 必中乱码视觉：常规 / 瞬发 / 开放式均有（后遗症阶段另由 tickPostDomainLocks 维持）。 */
        tickDomainSureHitGarbledVisuals(player);
        /* 驱动蓄力倒计时 */
        tickCastingDelay(player);
        /* 驱动开放式前摇倒计时 */
        tickOpenDomainLeadIn(player);
        /*
         * 正常的领域维持逻辑：必须调基类的 tickMaintain(DomainCaster)（纯实现），
         * ⛔ 不能写 super.tickMaintain(player) —— 基类那个重载会转发回本类的 tickMaintain(DomainCaster)，
         * 与本方法形成「ServerPlayer 版 → 基类 → DomainCaster 版 → 本方法」的无限递归。
         */
        super.tickMaintain(DomainCaster.of(player));
        if (!instant && !open)
        {
            tickLimitlessSkyboxTextureSwap(player);
        }
        clearOwnerMobLocksIfDomainClosed(player);
    }

    /** 当前活跃领域是否为开放式。 */
    private boolean hasOpenDomainInstance(ServerPlayer player)
    {
        java.util.Deque<BarrierInstance> queue = ACTIVE_DOMAINS.get(player.getUUID());
        if (queue == null || queue.isEmpty())
        {
            return false;
        }
        for (BarrierInstance instance : queue)
        {
            if (instance.isOpenDomain())
            {
                return true;
            }
        }
        return false;
    }

    /** 当前活跃领域是否为瞬发（天空盒-only / 构建完即关）。 */
    private boolean hasInstantDomainInstance(ServerPlayer player)
    {
        return hasInstantDomainInstance(player.getUUID());
    }

    /** 瞬发判定（UUID 版）：AI 实体施法者同样适用。 */
    private boolean hasInstantDomainInstance(UUID casterUuid)
    {
        java.util.Deque<BarrierInstance> queue = ACTIVE_DOMAINS.get(casterUuid);
        if (queue == null || queue.isEmpty())
        {
            return false;
        }
        for (BarrierInstance instance : queue)
        {
            if (instance.isAutoCloseOnComplete() || instance.isSkyboxOnlyExpand())
            {
                return true;
            }
        }
        return false;
    }

    // ==================== 覆盖 tickMaintain（施法者无关版）以驱动压轴 ====================

    /**
     * 领域维持（施法者无关版）：玩家与 AI 实体共用。
     * <p>
     * 基类 {@link AbstractTechniqueDomain#tickMaintain(DomainCaster)} 只跑通用的咒力消耗 / 崩解判定；
     * 无量空处的「压轴」全挂在施法者 UUID 的登记表上（{@code limitlessSkyboxSwapAtServerTick}），
     * 必须由本覆写驱动，否则 <b>AI 实体施法者永远等不到天空盒切换 / 水墨 / 白色闪屏 / 球心环境音</b>。
     * <p>
     * 玩家分支走 {@link #tickMaintainPlayerSide(ServerPlayer)}（蓄力 / 开放式前摇 / 玩家专属副作用全在里面）；
     * AI 分支驱动蓄力前摇 {@link #tickCastingDelay(DomainCaster)} + 通用维持 + 延迟压轴。
     * <p>
     * ⛔ 玩家分支<b>不得</b>写成 {@code tickMaintain(player)}：基类 {@code tickMaintain(ServerPlayer)}
     * 会转发回本方法（基类第 772 行），形成无限递归（StackOverflowError）。两条入口都必须直达
     * {@link #tickMaintainPlayerSide(ServerPlayer)} 这个非虚方法。
     */
    @Override
    public void tickMaintain(DomainCaster caster)
    {
        ServerPlayer player = caster.player();
        if (player != null)
        {
            /* ⛔ 不能回调 tickMaintain(ServerPlayer)：会与基类的转发形成虚调用环，见 tickMaintainPlayerSide。 */
            tickMaintainPlayerSide(player);
            return;
        }
        /* AI 实体施法者：无 cmdcam、无解除键；驱动开放式前摇 / 常规蓄力 + 通用维持 + 延迟压轴。 */
        tickOpenDomainLeadIn(caster);
        tickCastingDelay(caster);
        /* AI 施法者姿态锁（朝北 + 定身）：等价于玩家版的 tickPlayerLockState，必须在蓄力期与存续期都维持。 */
        tickAiCasterPoseLock(caster);
        super.tickMaintain(caster);
        if (!hasInstantDomainInstance(caster.uid()))
        {
            tickLimitlessSkyboxTextureSwap(caster);
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 检查玩家是否在自己的领域内（用于判断是否直接解除领域）。
     */
    private boolean isPlayerInsideOwnDomain(ServerPlayer player)
    {
        java.util.Deque<BarrierInstance> queue = ACTIVE_DOMAINS.get(player.getUUID());
        if (queue == null || queue.isEmpty())
        {
            return false;
        }
        for (BarrierInstance instance : queue)
        {
            if (isPlayerInsideBarrier(player, instance))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * 结界构建完成瞬间：对施法者 {@link #WHITE_OVERLAY_RADIUS} 格内玩家同步客户端屏幕振动。
     */
    private static void broadcastLimitlessDomainCompleteScreenShake(ServerPlayer caster, ServerLevel level)
    {
        broadcastLimitlessDomainCompleteScreenShakeAt(DomainCaster.of(caster), level);
    }

    /** 结屏震（施法者无关版）。 */
    private static void broadcastLimitlessDomainCompleteScreenShakeAt(DomainCaster caster, ServerLevel level)
    {
        double radiusSqr = WHITE_OVERLAY_RADIUS * WHITE_OVERLAY_RADIUS;
        Vec3 center = caster.position();
        for (ServerPlayer nearby : level.players())
        {
            if (nearby.distanceToSqr(center.x, center.y, center.z) > radiusSqr)
            {
                continue;
            }
            ModNetwork.sendScreenShake(nearby, ScreenShakeKind.DECAY, DOMAIN_COMPLETE_SCREEN_SHAKE_TICKS, DOMAIN_COMPLETE_SCREEN_SHAKE_MAX_DEG);
        }
    }

    /**
     * 结界/天空盒完成后：将范围内玩家正在播放的 shader 白幕提升到 AFTER_LEVEL，避免开洞被天空盒盖住。
     */
    private static void broadcastDomainWhiteCurtainElevate(ServerPlayer caster, ServerLevel level)
    {
        broadcastDomainWhiteCurtainElevateAt(DomainCaster.of(caster), level);
    }

    /** 白幕提层（施法者无关版）。 */
    private static void broadcastDomainWhiteCurtainElevateAt(DomainCaster caster, ServerLevel level)
    {
        double radiusSqr = WHITE_OVERLAY_RADIUS * WHITE_OVERLAY_RADIUS;
        Vec3 center = caster.position();
        for (ServerPlayer nearby : level.players())
        {
            if (nearby.distanceToSqr(center.x, center.y, center.z) > radiusSqr)
            {
                continue;
            }
            ModNetwork.sendDomainWhiteCurtainElevateSync(nearby);
        }
    }

    /*
     * 无量空处构建完成后的固定演出：
     * 1) 以世界绝对坐标系为基准，在球心 south 方向 22 格放置特效锚点；
     * 2) 锁定施法者视角到 north 并冻结移动 280 tick。
     * 业务意图：确保镜头构图和 Photon 朝向不受玩家当时朝向影响，演出可重复且稳定。
     */
    private void spawnLimitlessLightLineEntity(ServerPlayer player, BarrierInstance instance)
    {
        spawnLightLineAnchor(player, instance, LIGHT_LINE_SOUTH_DISTANCE, PHOTON_LIGHT_LINE_COMMAND);

        playerLockMap.put(player.getUUID(), new PlayerLockState(new Vec3(player.getX(), player.getY(), player.getZ()), player.getXRot(), LOCK_PLAYER_DURATION_TICKS));
        ModNetwork.sendPlayerInputLockSync(player, true);
    }

    /**
     * AI 实体版：放置 lightline 锚点，但<b>不</b>写 {@code playerLockMap}（那是玩家输入锁专用容器）。
     */
    private void spawnLimitlessLightLineEntity(DomainCaster caster, BarrierInstance instance)
    {
        spawnLightLineAnchor(caster, instance, LIGHT_LINE_SOUTH_DISTANCE, PHOTON_LIGHT_LINE_COMMAND);
    }

    /**
     * 在球心 south 方向 {@code southDistance} 格放置 lightline 锚点实体并执行 Photon 命令。
     * <p>
     * 不含镜头 / 输入锁：常规无量空处由 {@link #spawnLimitlessLightLineEntity} 追加锁定；
     * 开放式演出不锁施法者，直接调用本方法。
     */
    private static void spawnLightLineAnchor(ServerPlayer player, BarrierInstance instance, double southDistance, String photonCommand)
    {
        spawnLightLineAnchor(DomainCaster.of(player), instance, southDistance, photonCommand);
    }

    /** lightline 锚点（施法者无关版）：高度取施法者 Y，朝向固定 north。 */
    private static void spawnLightLineAnchor(DomainCaster caster, BarrierInstance instance, double southDistance, String photonCommand)
    {
        spawnLightLineAnchorAt(caster.level(), instance, caster.position().y, southDistance, photonCommand);
    }

    /** lightline 锚点底层实现：只依赖 level / center / 高度。 */
    private static void spawnLightLineAnchorAt(ServerLevel level, BarrierInstance instance,
            double spawnY, double southDistance, String photonCommand)
    {
        if (level == null)
        {
            return;
        }
        SpecialEffectEntity entity = ModEntities.SPECIAL_EFFECT_ENTITY.get().create(level);
        if (entity == null)
        {
            return;
        }

        BlockPos center = instance.getCenter();
        double spawnX = center.getX() + 0.5D;
        double spawnZ = center.getZ() + 0.5D + southDistance;

        entity.moveTo(spawnX, spawnY, spawnZ, NORTH_YAW, 0.0F);
        if (!level.addFreshEntity(entity))
        {
            return;
        }

        CommandSourceStack commandSource = entity.createCommandSourceStack()
                .withPermission(2)
                .withSuppressedOutput();
        level.getServer().getCommands().performPrefixedCommand(commandSource, photonCommand);
    }

    /**
     * 每 tick 维持“朝向 north + 禁止移动”的强制状态。
     * <p>
     * 重要副作用：玩家尝试移动或转头会被服务器在下一 tick 拉回固定位置与朝向。
     */
    private void tickPlayerLockState(ServerPlayer player)
    {
        UUID uuid = player.getUUID();
        PlayerLockState lockState = playerLockMap.get(uuid);
        if (lockState == null)
        {
            return;
        }

        if (!player.isAlive() || lockState.remainingTicks() <= 0)
        {
            playerLockMap.remove(uuid);
            ModNetwork.sendPlayerInputLockSync(player, false);
            return;
        }

        Vec3 lockPos = lockState.lockPos();
        float lockXRot = lockState.lockXRot();
        player.setDeltaMovement(Vec3.ZERO);
        player.hurtMarked = true;
        player.connection.teleport(lockPos.x, lockPos.y, lockPos.z, NORTH_YAW, lockXRot);

        playerLockMap.put(uuid, lockState.decreaseOneTick());
    }

    /**
     * 将领域内生物转移到球心 north 方向约 15 格区域，并锁定朝向 south 与移动能力。
     * <p>
     * 排除规则：SpecialEffectEntity、施法者、领域完成时快照的 {@link #limitlessSessionMobWhitelist} 内生物。
     * 领域存续期间，新进入球内且非排除对象的目标由 {@link #captureNewDomainIntruders(ServerPlayer)} 每 tick 补捕获。
     */
    /**
     * @return 本次登记为无量空处领域锁定（转移）目标的存活生物数量（结界完成瞬间快照）。
     */
    private int relocateAndLockDomainMobs(ServerPlayer caster, BarrierInstance instance)
    {
        return relocateAndLockDomainMobs(DomainCaster.of(caster), instance);
    }

    /** 搬点 + 朝南锁定（施法者无关版）。 */
    private int relocateAndLockDomainMobs(DomainCaster caster, BarrierInstance instance)
    {
        ServerLevel level = caster.level();
        if (level == null)
        {
            return 0;
        }
        BlockPos center = instance.getCenter();
        int radius = instance.getRadius();
        double radiusSqr = (double) radius * (double) radius;
        UUID casterUuid = caster.uid();

        AABB queryBox = new AABB(
                center.getX() - radius, center.getY() - radius, center.getZ() - radius,
                center.getX() + radius + 1.0D, center.getY() + radius + 1.0D, center.getZ() + radius + 1.0D
        );
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, queryBox, entity ->
        {
            if (entity == null || !entity.isAlive())
            {
                return false;
            }
            if (entity instanceof SpecialEffectEntity)
            {
                return false;
            }
            if (entity.getUUID().equals(casterUuid))
            {
                return false;
            }
            if (isExcludedFromBeingLimitlessDomainMobTarget(caster, casterUuid, entity))
            {
                return false;
            }
            return isInsideDomainSphere(entity.position(), center, radiusSqr);
        });
        if (candidates.isEmpty())
        {
            return 0;
        }

        Vec3 anchor = new Vec3(center.getX() + 0.5D, caster.position().y, center.getZ() + 0.5D - MOB_RELOCATE_NORTH_DISTANCE);
        List<Vec3> targetPositions = buildScatterPositions(anchor, candidates.size());
        for (int i = 0; i < candidates.size(); i++)
        {
            LivingEntity target = candidates.get(i);
            Vec3 targetPos = targetPositions.get(i);
            registerLivingEntityInDomainMobLock(caster, target, targetPos, level);
        }
        return candidates.size();
    }

    private static void maybeAnnouncePerfectControl(ServerPlayer caster, BarrierInstance instance, int domainLockedTargetCount)
    {
        maybeAnnouncePerfectControl(DomainCaster.of(caster), instance, domainLockedTargetCount);
    }

    /** 完美控制提示（施法者无关版）：AI 施法不给玩家弹聊天、不授予成就，只放音效。 */
    private static void maybeAnnouncePerfectControl(DomainCaster caster, BarrierInstance instance, int domainLockedTargetCount)
    {
        if (domainLockedTargetCount <= PERFECT_CONTROL_LOCKED_TARGET_THRESHOLD)
        {
            return;
        }
        ServerLevel level = caster.level();
        if (level == null)
        {
            return;
        }
        BlockPos center = instance.getCenter();
        double cx = center.getX() + 0.5D;
        double cy = center.getY() + 0.5D;
        double cz = center.getZ() + 0.5D;
        level.playSound(null, cx, cy, cz, ModSounds.LIMITLESS_DOMAIN_PERFECT_AMBIENT.get(), SoundSource.AMBIENT, 1.0F, 1.0F);
        Vec3 pos = caster.position();
        level.playSound(null, pos.x, pos.y, pos.z, ModSounds.SOUND_EFFECTS_IRON_SOUND.get(), SoundSource.PLAYERS, PERFECT_CONTROL_IRON_SOUND_VOLUME, 1.0F);
        ServerPlayer player = caster.player();
        if (player != null)
        {
            player.displayClientMessage(Component.literal("完美控制").withStyle(ChatFormatting.GOLD), false);
            grantAdvancement(player, "perfect_control");
        }
    }

    private static void grantAdvancement(ServerPlayer player, String name)
    {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(JuJutsuKaisenZone.MODID, name);
        Advancement advancement = player.server.getAdvancements().getAdvancement(id);
        if (advancement == null)
        {
            return;
        }
        var progress = player.getAdvancements();
        for (String criterion : advancement.getCriteria().keySet())
        {
            if (!progress.getOrStartProgress(advancement).isDone())
            {
                progress.award(advancement, criterion);
            }
        }
    }

    /**
     * 领域展开完成后，任何进入球内且非排除的生物都会被补登记为领域锁定目标。
     * <p>
     * 排除：施法者本人、{@link SpecialEffectEntity}、本次领域会话快照白名单 {@link #limitlessSessionMobWhitelist}。
     * 已被本施法者锁定的目标会跳过；已被其他施法者锁定的实体不抢夺。
     */
    private void captureNewDomainIntruders(ServerPlayer owner)
    {
        java.util.Deque<BarrierInstance> queue = ACTIVE_DOMAINS.get(owner.getUUID());
        if (queue == null || queue.isEmpty())
        {
            return;
        }
        BarrierInstance instance = queue.peekLast();
        if (instance == null)
        {
            return;
        }
        /* 瞬发领域不转移、不锁定生物位置。 */
        if (instance.isAutoCloseOnComplete() || instance.isSkyboxOnlyExpand())
        {
            return;
        }
        ServerLevel level = owner.serverLevel();
        if (!instance.getDimension().equals(level.dimension()))
        {
            return;
        }
        /* 展开动画在本 tick 的 super.tickMaintain 中才结束；此前白名单未写入，补捕获会误伤施法者身边单位。 */
        if (instance.isExpanding())
        {
            return;
        }
        BlockPos center = instance.getCenter();
        int radius = instance.getRadius();
        double radiusSqr = (double) radius * (double) radius;
        UUID ownerUuid = owner.getUUID();

        int nextScatterIndex = countMobLocksForOwner(ownerUuid);
        Vec3 anchor = new Vec3(center.getX() + 0.5D, owner.getY(), center.getZ() + 0.5D - MOB_RELOCATE_NORTH_DISTANCE);

        AABB queryBox = new AABB(
                center.getX() - radius, center.getY() - radius, center.getZ() - radius,
                center.getX() + radius + 1.0D, center.getY() + radius + 1.0D, center.getZ() + radius + 1.0D
        );
        List<LivingEntity> inside = level.getEntitiesOfClass(LivingEntity.class, queryBox, entity ->
        {
            if (entity == null || !entity.isAlive())
            {
                return false;
            }
            if (entity instanceof SpecialEffectEntity)
            {
                return false;
            }
            if (entity.getUUID().equals(ownerUuid))
            {
                return false;
            }
            if (isExcludedFromBeingLimitlessDomainMobTarget(owner, ownerUuid, entity))
            {
                return false;
            }
            return isInsideDomainSphere(entity.position(), center, radiusSqr);
        });

        for (LivingEntity target : inside)
        {
            if (mobLockMap.containsKey(target.getUUID()))
            {
                continue;
            }
            Vec3 targetPos = buildScatterPositions(anchor, nextScatterIndex + 1).get(nextScatterIndex);
            nextScatterIndex++;
            registerLivingEntityInDomainMobLock(owner, target, targetPos, level);
        }
    }

    private int countMobLocksForOwner(UUID ownerUuid)
    {
        int n = 0;
        for (MobLockState s : mobLockMap.values())
        {
            if (s.ownerUuid().equals(ownerUuid))
            {
                n++;
            }
        }
        return n;
    }

    /**
     * 领域完成瞬间：根据施法者当前位置，将 {@link #EXCLUDE_CASTER_RADIUS} 格内存活生物（除施法者、特效实体）记入本会话白名单。
     * <p>
     * 装载了「移除身旁豁免」SE 时跳过：身旁单位不再幸免。
     */
    private void populateLimitlessSessionMobWhitelist(ServerPlayer caster)
    {
        populateLimitlessSessionMobWhitelist(DomainCaster.of(caster));
    }

    /** 白名单快照（施法者无关版）。 */
    private void populateLimitlessSessionMobWhitelist(DomainCaster caster)
    {
        Set<UUID> set = limitlessSessionMobWhitelist.computeIfAbsent(caster.uid(), k -> ConcurrentHashMap.newKeySet());
        set.clear();
        if (removesNearCasterExempt(caster))
        {
            return;
        }
        ServerLevel level = caster.level();
        if (level == null)
        {
            return;
        }
        Vec3 pos = caster.position();
        double excludeRadiusSqr = EXCLUDE_CASTER_RADIUS * EXCLUDE_CASTER_RADIUS;
        AABB queryBox = new AABB(pos, pos).inflate(EXCLUDE_CASTER_RADIUS);
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, queryBox, entity ->
        {
            if (entity == null || !entity.isAlive())
            {
                return false;
            }
            if (entity instanceof SpecialEffectEntity)
            {
                return false;
            }
            if (entity.getUUID().equals(caster.uid()))
            {
                return false;
            }
            return entity.distanceToSqr(pos) <= excludeRadiusSqr;
        });
        for (LivingEntity entity : nearby)
        {
            set.add(entity.getUUID());
        }
    }

    private boolean isLimitlessSessionWhitelisted(UUID casterUuid, UUID entityUuid)
    {
        Set<UUID> set = limitlessSessionMobWhitelist.get(casterUuid);
        return set != null && set.contains(entityUuid);
    }

    /**
     * 是否不得登记为无量空处领域锁定（转移）目标：快照白名单内；或本会话尚未写入白名单键时按施法者身边距离临时保护。
     * <p>
     * 装载了「移除身旁豁免」SE 时恒为 false。
     */
    private boolean isExcludedFromBeingLimitlessDomainMobTarget(ServerPlayer caster, UUID casterUuid, LivingEntity entity)
    {
        if (removesNearCasterExempt(caster))
        {
            return false;
        }
        if (isLimitlessSessionWhitelisted(casterUuid, entity.getUUID()))
        {
            return true;
        }
        if (!limitlessSessionMobWhitelist.containsKey(casterUuid))
        {
            return entity.distanceToSqr(caster) <= EXCLUDE_CASTER_RADIUS * EXCLUDE_CASTER_RADIUS;
        }
        return false;
    }

    private boolean isExcludedFromBeingLimitlessDomainMobTarget(DomainCaster caster, UUID casterUuid, LivingEntity entity)
    {
        if (removesNearCasterExempt(caster))
        {
            return false;
        }
        if (isLimitlessSessionWhitelisted(casterUuid, entity.getUUID()))
        {
            return true;
        }
        if (!limitlessSessionMobWhitelist.containsKey(casterUuid))
        {
            Vec3 pos = caster.position();
            return entity.distanceToSqr(pos) <= EXCLUDE_CASTER_RADIUS * EXCLUDE_CASTER_RADIUS;
        }
        return false;
    }

    /** 是否因 SE 关掉身旁豁免（只认装载了对应窄口的施法者）。 */
    private static boolean removesNearCasterExempt(ServerPlayer caster)
    {
        return SeMechanicRegistry.findEquipped(caster, LimitlessNearCasterExemptSeMechanic.class)
                .map(LimitlessNearCasterExemptSeMechanic::removesNearCasterExempt)
                .orElse(false);
    }

    /** AI 实体走不到 SE 窄口（SE 系统只认玩家）⇒ 依然保留身旁豁免。 */
    private static boolean removesNearCasterExempt(DomainCaster caster)
    {
        ServerPlayer player = caster.player();
        return player != null && removesNearCasterExempt(player);
    }

    /**
     * 登记为无量空处领域锁定单位（构建完成后 280 刻内：南侧散布固定位；之后仅失明、暂停 AI、玩家输入锁等，不再传送与锁朝南）。
     */
    private void registerLivingEntityInDomainMobLock(ServerPlayer caster, LivingEntity target, Vec3 targetPos, ServerLevel level)
    {
        if (isExcludedFromBeingLimitlessDomainMobTarget(caster, caster.getUUID(), target))
        {
            return;
        }
        MinecraftServer server = level.getServer();
        int serverTick = server != null ? server.getTickCount() : 0;
        boolean enforceAnchorPose = !isLimitlessDomainMobAnchorPoseExpired(caster.getUUID(), serverTick);
        PostDomainMobLockState lingeringPost = postDomainMobLockMap.remove(target.getUUID());
        if (lingeringPost != null && server != null)
        {
            releasePostDomainMobLock(target.getUUID(), lingeringPost, server);
        }
        NearCasterMobLockState lingeringNear = nearCasterMobLockMap.remove(target.getUUID());
        if (lingeringNear != null && server != null)
        {
            releaseNearCasterMobLock(target.getUUID(), lingeringNear, server);
        }
        if (enforceAnchorPose)
        {
            lockEntityToSouth(target, targetPos);
        }
        boolean aiPausedByThisLock = false;
        boolean originNoAi = false;
        if (target instanceof Mob mob)
        {
            originNoAi = mob.isNoAi();
            if (!originNoAi)
            {
                mob.setNoAi(true);
                aiPausedByThisLock = true;
            }
        }
        if (target instanceof ServerPlayer targetPlayer)
        {
            ModNetwork.sendPlayerInputLockSync(targetPlayer, true);
        }
        applyLockBlindness(target);
        mobLockMap.put(target.getUUID(), new MobLockState(caster.getUUID(), targetPos, aiPausedByThisLock, originNoAi, 0));
    }

    /** 登记为无量空处领域锁定单位（施法者无关版）。AI 施法不会对玩家下发输入锁以外的东西——逻辑完全一致。 */
    private void registerLivingEntityInDomainMobLock(DomainCaster caster, LivingEntity target, Vec3 targetPos, ServerLevel level)
    {
        if (isExcludedFromBeingLimitlessDomainMobTarget(caster, caster.uid(), target))
        {
            return;
        }
        MinecraftServer server = level.getServer();
        int serverTick = server != null ? server.getTickCount() : 0;
        boolean enforceAnchorPose = !isLimitlessDomainMobAnchorPoseExpired(caster.uid(), serverTick);
        PostDomainMobLockState lingeringPost = postDomainMobLockMap.remove(target.getUUID());
        if (lingeringPost != null && server != null)
        {
            releasePostDomainMobLock(target.getUUID(), lingeringPost, server);
        }
        NearCasterMobLockState lingeringNear = nearCasterMobLockMap.remove(target.getUUID());
        if (lingeringNear != null && server != null)
        {
            releaseNearCasterMobLock(target.getUUID(), lingeringNear, server);
        }
        if (enforceAnchorPose)
        {
            lockEntityToSouth(target, targetPos);
        }
        boolean aiPausedByThisLock = false;
        boolean originNoAi = false;
        if (target instanceof Mob mob)
        {
            originNoAi = mob.isNoAi();
            if (!originNoAi)
            {
                mob.setNoAi(true);
                aiPausedByThisLock = true;
            }
        }
        if (target instanceof ServerPlayer targetPlayer)
        {
            ModNetwork.sendPlayerInputLockSync(targetPlayer, true);
        }
        applyLockBlindness(target);
        mobLockMap.put(target.getUUID(), new MobLockState(caster.uid(), targetPos, aiPausedByThisLock, originNoAi, 0));
    }

    private void lockSessionWhitelistedCasters(ServerPlayer caster)
    {
        lockSessionWhitelistedCasters(DomainCaster.of(caster));
    }

    /**
     * 对领域完成时记入白名单的生物施加与施法者相同的 {@link #LOCK_PLAYER_DURATION_TICKS} tick 演出锁定
     * （朝北、位置与俯仰角固定；非玩家暂停 AI）。白名单实体之后离开施法者身边也不会变为领域锁定目标。
     * <p>
     * 施法者无关版：AI 施法同样会锁住自己身旁的白名单单位；命中目标里的玩家依旧下发输入锁（玩家被 AI 领域卷进去要能感受到）。
     */
    private void lockSessionWhitelistedCasters(DomainCaster caster)
    {
        ServerLevel level = caster.level();
        if (level == null)
        {
            return;
        }
        MinecraftServer server = level.getServer();
        Set<UUID> whitelist = limitlessSessionMobWhitelist.get(caster.uid());
        if (whitelist == null || whitelist.isEmpty())
        {
            return;
        }
        for (UUID targetId : whitelist)
        {
            /* 施法者本人不在白名单内（白名单登记时就跳过施法者），此处再兜一层：
             * AI 实体是 LivingEntity，若不自查会被当成普通生物锁成朝北 ⇒ 「AI 五条悟开领域却面朝北」。 */
            if (targetId.equals(caster.uid()))
            {
                continue;
            }
            if (!(level.getEntity(targetId) instanceof LivingEntity target) || !target.isAlive())
            {
                continue;
            }
            Vec3 lockPos = new Vec3(target.getX(), target.getY(), target.getZ());
            float lockXRot = target.getXRot();
            PostDomainMobLockState lingeringPost = postDomainMobLockMap.remove(target.getUUID());
            if (lingeringPost != null && server != null)
            {
                releasePostDomainMobLock(target.getUUID(), lingeringPost, server);
            }
            NearCasterMobLockState lingeringNear = nearCasterMobLockMap.remove(target.getUUID());
            if (lingeringNear != null && server != null)
            {
                releaseNearCasterMobLock(target.getUUID(), lingeringNear, server);
            }
            if (target instanceof ServerPlayer sp)
            {
                playerLockMap.put(sp.getUUID(), new PlayerLockState(lockPos, lockXRot, LOCK_PLAYER_DURATION_TICKS));
                ModNetwork.sendPlayerInputLockSync(sp, true);
                continue;
            }
            boolean originNoAi = false;
            boolean aiPausedByThisLock = false;
            if (target instanceof Mob mob)
            {
                originNoAi = mob.isNoAi();
                if (!originNoAi)
                {
                    mob.setNoAi(true);
                    aiPausedByThisLock = true;
                }
            }
            nearCasterMobLockMap.put(target.getUUID(), new NearCasterMobLockState(
                    lockPos, lockXRot, LOCK_PLAYER_DURATION_TICKS, aiPausedByThisLock, originNoAi));
            applyCasterStyleLockToNonPlayerLiving(target, lockPos, lockXRot);
        }
    }

    /**
     * 领域（含天空盒）完成时登记：在 {@link #LOCK_PLAYER_DURATION_TICKS} 个服务器 tick 后将内壁 jjkz_skyblock 换为 stat_sky2。
     */
    private void scheduleLimitlessSkyboxTextureSwapAfterCutscene(ServerPlayer caster)
    {
        scheduleLimitlessSkyboxTextureSwapAfterCutscene(DomainCaster.of(caster));
    }

    /** 天空盒延迟换贴（施法者无关版）。 */
    private void scheduleLimitlessSkyboxTextureSwapAfterCutscene(DomainCaster caster)
    {
        MinecraftServer server = caster.server();
        if (server == null)
        {
            return;
        }
        limitlessSkyboxSwapAtServerTick.put(caster.uid(), server.getTickCount() + LOCK_PLAYER_DURATION_TICKS);
    }

    /**
     * 领域构建完成瞬间登记：再过 {@link #LOCK_PLAYER_DURATION_TICKS} 刻才允许施法者按键解除领域。
     */
    private void scheduleLimitlessDomainDismissUnlockAfterPerformance(ServerPlayer caster)
    {
        MinecraftServer server = caster.getServer();
        if (server == null)
        {
            return;
        }
        limitlessDomainDismissUnlockServerTick.put(caster.getUUID(), server.getTickCount() + LOCK_PLAYER_DURATION_TICKS);
    }

    /**
     * 构建动画进行中不可解除；构建完成后须再等 {@link #LOCK_PLAYER_DURATION_TICKS} 刻。
     */
    private boolean canDismissLimitlessDomainNow(ServerPlayer player)
    {
        MinecraftServer server = player.getServer();
        if (server == null)
        {
            return true;
        }
        UUID uuid = player.getUUID();
        java.util.Deque<BarrierInstance> queue = ACTIVE_DOMAINS.get(uuid);
        BarrierInstance instance = queue != null ? queue.peekLast() : null;
        if (instance != null && instance.isExpanding())
        {
            return false;
        }
        Integer unlockAt = limitlessDomainDismissUnlockServerTick.get(uuid);
        return unlockAt == null || server.getTickCount() >= unlockAt;
    }

    /**
     * 与 {@link #scheduleLimitlessDomainDismissUnlockAfterPerformance} 同一时刻起：
     * {@link #mobLockMap} 目标不再被强制传送到球心北侧散布位，也不再锁朝南朝向；失明、乱码聊天、AI/输入锁等不受影响。
     */
    private boolean isLimitlessDomainMobAnchorPoseExpired(UUID casterUuid, int serverTick)
    {
        Integer unlockAt = limitlessDomainDismissUnlockServerTick.get(casterUuid);
        return unlockAt != null && serverTick >= unlockAt;
    }

    /**
     * 到达预定 tick 时，将当前无量空处实例内所有天空盒方块与修补用纹理改为 stat_sky2，
     * 在球心放置 {@link SpecialEffectEntity} 执行 {@code photon:limitless_inkwash}，
     * 并对球心 {@link #WHITE_OVERLAY_RADIUS} 格内玩家播放一次全屏世界层闪光（方块与实体之上、HUD 之下）；
     * 对球心附近玩家开始循环播放 {@link ModSounds#LIMITLESS_DOMAIN_CENTER_AMBIENT}（20% 音量，领域关闭时停止）。
     */
    private void tickLimitlessSkyboxTextureSwap(ServerPlayer owner)
    {
        tickLimitlessSkyboxTextureSwap(DomainCaster.of(owner));
    }

    /**
     * <b>施法者无关版</b>：玩家与 AI 实体共用（AI 无此驱动则压轴永不触发）。
     */
    private void tickLimitlessSkyboxTextureSwap(DomainCaster caster)
    {
        UUID ownerUuid = caster.uid();
        Integer swapAt = limitlessSkyboxSwapAtServerTick.get(ownerUuid);
        if (swapAt == null)
        {
            return;
        }
        MinecraftServer server = caster.server();
        if (server == null || server.getTickCount() < swapAt)
        {
            return;
        }
        java.util.Deque<BarrierInstance> queue = ACTIVE_DOMAINS.get(ownerUuid);
        if (queue == null || queue.isEmpty())
        {
            limitlessSkyboxSwapAtServerTick.remove(ownerUuid);
            limitlessDomainDismissUnlockServerTick.remove(ownerUuid);
            return;
        }
        BarrierInstance instance = queue.peekLast();
        if (instance == null || instance.isExpanding())
        {
            return;
        }
        ServerLevel level = caster.level();
        if (level == null || !instance.getDimension().equals(level.dimension()))
        {
            return;
        }
        applyLimitlessSkyboxTextureToInstance(level, instance, LIMITLESS_DOMAIN_SKYBOX_TEXTURE_AFTER_280);
        spawnLimitlessInkwashAnchorAtSphereCenter(level, instance, PHOTON_INKWASH_COMMAND);
        sendPostDomainCompletionWhiteFlashNearSphereCenter(level, instance);
        broadcastLimitlessDomainAmbientSoundStart(level, instance, ownerUuid);
        limitlessSkyboxSwapAtServerTick.remove(ownerUuid);
    }

    /** 对领域球心附近玩家同步全屏闪光（{@link ModNetwork#sendFullScreenWorldFlashAnimationSync}）。 */
    private static void sendPostDomainCompletionWhiteFlashNearSphereCenter(ServerLevel level, BarrierInstance instance)
    {
        BlockPos c = instance.getCenter();
        double cx = c.getX() + 0.5D;
        double cy = c.getY() + 0.5D;
        double cz = c.getZ() + 0.5D;
        double radiusSqr = WHITE_OVERLAY_RADIUS * WHITE_OVERLAY_RADIUS;
        for (ServerPlayer nearby : level.players())
        {
            double dx = nearby.getX() - cx;
            double dy = nearby.getY() - cy;
            double dz = nearby.getZ() - cz;
            if (dx * dx + dy * dy + dz * dz > radiusSqr)
            {
                continue;
            }
            ModNetwork.sendFullScreenWorldFlashAnimationSync(
                    nearby,
                    POST_DOMAIN_COMPLETION_FLASH_TOTAL_TICKS,
                    POST_DOMAIN_COMPLETION_FLASH_FADE_IN_TICKS,
                    POST_DOMAIN_COMPLETION_FLASH_FADE_OUT_TICKS,
                    POST_DOMAIN_COMPLETION_FLASH_PEAK_ALPHA
            );
        }
    }

    private static void broadcastLimitlessDomainAmbientSoundStart(ServerLevel level, BarrierInstance instance, UUID casterUuid)
    {
        BlockPos c = instance.getCenter();
        double cx = c.getX() + 0.5D;
        double cy = c.getY() + 0.5D;
        double cz = c.getZ() + 0.5D;
        double radiusSqr = LIMITLESS_DOMAIN_AMBIENT_SOUND_SYNC_RADIUS * LIMITLESS_DOMAIN_AMBIENT_SOUND_SYNC_RADIUS;
        for (ServerPlayer nearby : level.players())
        {
            double dx = nearby.getX() - cx;
            double dy = nearby.getY() - cy;
            double dz = nearby.getZ() - cz;
            if (dx * dx + dy * dy + dz * dz > radiusSqr)
            {
                continue;
            }
            ModNetwork.sendLimitlessDomainAmbientSoundStart(nearby, casterUuid, cx, cy, cz);
        }
    }

    private static void broadcastLimitlessDomainAmbientSoundStop(ServerLevel level, UUID casterUuid)
    {
        for (ServerPlayer nearby : level.players())
        {
            ModNetwork.sendLimitlessDomainAmbientSoundStop(nearby, casterUuid);
        }
    }

    /** 球心锚点 + Photon 水墨扩散（与 {@link #LOCK_PLAYER_DURATION_TICKS} 后天空盒切换同刻）。 */
    private static void spawnLimitlessInkwashAnchorAtSphereCenter(ServerLevel level, BarrierInstance instance, String photonCommand)
    {
        SpecialEffectEntity entity = ModEntities.SPECIAL_EFFECT_ENTITY.get().create(level);
        if (entity == null)
        {
            return;
        }
        BlockPos center = instance.getCenter();
        double x = center.getX() + 0.5D;
        double y = center.getY() + 0.5D;
        double z = center.getZ() + 0.5D;
        entity.moveTo(x, y, z, SOUTH_YAW, 0.0F);
        if (!level.addFreshEntity(entity))
        {
            return;
        }
        CommandSourceStack commandSource = entity.createCommandSourceStack()
                .withPermission(2)
                .withSuppressedOutput();
        MinecraftServer server = level.getServer();
        if (server != null)
        {
            server.getCommands().performPrefixedCommand(commandSource, photonCommand);
        }
    }

    /**
     * 与 {@link #isPlayerInsideBarrier(ServerPlayer, BarrierInstance)} 相同的球内判定：移除领域内全部特效实体。
     */
    private static void removeSpecialEffectEntitiesInsideBarrierSphere(ServerLevel level, BarrierInstance instance)
    {
        BlockPos c = instance.getCenter();
        int radius = instance.getRadius();
        double cx = c.getX() + 0.5D;
        double cy = c.getY() + 0.5D;
        double cz = c.getZ() + 0.5D;
        double radiusSquared = (double) radius * (double) radius;
        AABB box = new AABB(cx - radius, cy - radius, cz - radius, cx + radius, cy + radius, cz + radius);
        for (SpecialEffectEntity entity : level.getEntitiesOfClass(SpecialEffectEntity.class, box))
        {
            if (!entity.isAlive())
            {
                continue;
            }
            double dx = entity.getX() - cx;
            double dy = entity.getY() - cy;
            double dz = entity.getZ() - cz;
            if (dx * dx + dy * dy + dz * dz < radiusSquared)
            {
                entity.discard();
            }
        }
    }

    private static void applyLimitlessSkyboxTextureToInstance(ServerLevel level, BarrierInstance instance, ResourceLocation texture)
    {
        for (BlockPos pos : instance.getSkyboxBlocks())
        {
            if (level.getBlockEntity(pos) instanceof JJKZSkyblockEntity be)
            {
                be.setTextureFolder(texture);
            }
        }
        instance.setCustomSkyboxTexture(texture);
    }

    /**
     * 每 tick 维持领域锁定目标：构建完成起 280 刻内强制南侧散布位与朝南姿态，之后不再传送与锁朝向，仍施加失明等直至领域关闭。
     * 开放式从不做搬点/朝南锁位，只维持失明、乱码、AI/输入锁。
     */
    private void tickLockedMobs(ServerPlayer owner)
    {
        ServerLevel level = owner.serverLevel();
        UUID ownerUuid = owner.getUUID();
        MinecraftServer server = level.getServer();
        int serverTick = server != null ? server.getTickCount() : 0;
        boolean openDomain = hasOpenDomainInstance(owner);
        boolean enforceAnchorPose = !openDomain && !isLimitlessDomainMobAnchorPoseExpired(ownerUuid, serverTick);
        List<UUID> toRemove = new ArrayList<>();
        for (Map.Entry<UUID, MobLockState> entry : mobLockMap.entrySet())
        {
            MobLockState lockState = entry.getValue();
            if (!lockState.ownerUuid().equals(ownerUuid))
            {
                continue;
            }
            if (!(level.getEntity(entry.getKey()) instanceof LivingEntity target) || !target.isAlive())
            {
                releaseMobLock(entry.getKey(), lockState, level);
                toRemove.add(entry.getKey());
                continue;
            }
            if (isExcludedFromBeingLimitlessDomainMobTarget(owner, ownerUuid, target))
            {
                releaseMobLock(entry.getKey(), lockState, level);
                toRemove.add(entry.getKey());
                continue;
            }
            if (enforceAnchorPose)
            {
                lockEntityToSouth(target, lockState.lockPos());
            }
            applyLockBlindness(target);
            if (target instanceof Mob mob && lockState.aiPausedByThisLock())
            {
                mob.setNoAi(true);
            }
            mobLockMap.put(entry.getKey(), lockState.withIncrementedDomainTicks());
        }
        for (UUID uuid : toRemove)
        {
            mobLockMap.remove(uuid);
        }
    }

    /**
     * 施法者领域关闭后，释放该施法者关联的全部被锁定生物。
     * 转入 {@link #postDomainMobLockMap} 时：若关闭时刻已过「构建完成 +280 刻」解锁点，则延续阶段不再传送/锁朝南，仅保留失明、乱码、AI/输入锁直至计时结束。
     */
    private void clearOwnerMobLocksIfDomainClosed(ServerPlayer owner)
    {
        java.util.Deque<BarrierInstance> queue = ACTIVE_DOMAINS.get(owner.getUUID());
        if (queue != null && !queue.isEmpty())
        {
            return;
        }
        limitlessSessionMobWhitelist.remove(owner.getUUID());
        limitlessSkyboxSwapAtServerTick.remove(owner.getUUID());
        MinecraftServer closeServer = owner.getServer();
        int serverTickAtClose = closeServer != null ? closeServer.getTickCount() : 0;
        Integer dismissUnlockAt = limitlessDomainDismissUnlockServerTick.remove(owner.getUUID());
        boolean anchorPoseExpiredAtClose = dismissUnlockAt != null && serverTickAtClose >= dismissUnlockAt;
        List<UUID> toRelease = new ArrayList<>();
        for (Map.Entry<UUID, MobLockState> entry : mobLockMap.entrySet())
        {
            if (!entry.getValue().ownerUuid().equals(owner.getUUID()))
            {
                continue;
            }
            MobLockState state = entry.getValue();
            /* 后遗症脑损：停留秒数（tick/20）；仅玩家；无提示。 */
            LivingEntity locked = findLivingEntity(owner.getServer(), entry.getKey());
            if (locked instanceof ServerPlayer victim)
            {
                PlayerBrainDamageData.addFromLimitlessDomainDwell(victim, state.domainLockElapsedTicks(), false);
            }
            int postTicks = Math.min(state.domainLockElapsedTicks() * 3, POST_DOMAIN_LOCK_MAX_TICKS);
            if (postTicks > 0)
            {
                boolean enforcePostAnchorPose = !anchorPoseExpiredAtClose;
                postDomainMobLockMap.put(entry.getKey(), new PostDomainMobLockState(
                        state.lockPos(),
                        postTicks,
                        state.aiPausedByThisLock(),
                        state.originNoAi(),
                        enforcePostAnchorPose
                ));
            }
            else
            {
                releaseMobLock(entry.getKey(), state, owner.serverLevel());
            }
            toRelease.add(entry.getKey());
        }
        for (UUID uuid : toRelease)
        {
            mobLockMap.remove(uuid);
        }
    }

    private void lockEntityToSouth(LivingEntity entity, Vec3 lockPos)
    {
        entity.setDeltaMovement(Vec3.ZERO);
        entity.hurtMarked = true;
        entity.teleportTo(lockPos.x, lockPos.y, lockPos.z);
        entity.setYRot(SOUTH_YAW);
        entity.setYBodyRot(SOUTH_YAW);
        entity.setYHeadRot(SOUTH_YAW);
        if (entity instanceof ServerPlayer serverPlayer)
        {
            serverPlayer.connection.teleport(lockPos.x, lockPos.y, lockPos.z, SOUTH_YAW, serverPlayer.getXRot());
        }
    }

    /**
     * 持续刷新短时失明，确保被锁定目标在锁定期内保持失明状态。
     */
    private void applyLockBlindness(LivingEntity entity)
    {
        entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 12, 0, false, false, false));
    }

    /**
     * 向被锁定玩家聊天框持续灌入乱码文本，强化“无量空处”精神压制感。
     */
    private void sendGarbledDomainChat(ServerPlayer player)
    {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int segmentCount = 2 + random.nextInt(3); // 2~4 段随机拼接
        StringBuilder builder = new StringBuilder(128);
        for (int i = 0; i < segmentCount; i++)
        {
            if (i > 0)
            {
                builder.append("  ");
            }
            int style = random.nextInt(GARBLED_STYLE_COUNT);
            switch (style)
            {
                case 0 -> appendRandomChars(builder, GARBLED_SYMBOL_CHARS, 10, 20, random);
                case 1 -> appendRandomChars(builder, GARBLED_MIXED_CHARS, 12, 24, random);
                case 2 -> builder.append(GARBLED_CRASH_LINES[random.nextInt(GARBLED_CRASH_LINES.length)]);
                default -> appendRandomChars(builder, GARBLED_CN_CHARS, 8, 18, random);
            }
        }
        player.sendSystemMessage(Component.literal(builder.toString()).withStyle(ChatFormatting.WHITE));
    }

    /**
     * 锁定目标的乱码表现：玩家额外灌聊天栏；全体在身周刷世界空间乱码碎片（附近玩家可见）。
     */
    private void applyDomainGarbledFx(LivingEntity target)
    {
        if (target instanceof ServerPlayer targetPlayer)
        {
            sendGarbledDomainChat(targetPlayer);
        }
        broadcastGarbledBurstAround(target);
    }

    private static final double GARBLED_BURST_SYNC_RADIUS = 48.0D;

    /**
     * 无量空处必中视觉：对当前活跃领域球内目标刷乱码（常规 / 瞬发 / 开放式）。
     * 常规展开动画未完成前不刷；开放式已结算穿出后遗症的目标不再刷。
     */
    private void tickDomainSureHitGarbledVisuals(ServerPlayer owner)
    {
        java.util.Deque<BarrierInstance> queue = ACTIVE_DOMAINS.get(owner.getUUID());
        if (queue == null || queue.isEmpty())
        {
            return;
        }
        BarrierInstance instance = queue.peekLast();
        if (instance == null)
        {
            return;
        }
        boolean instantOrOpen = instance.isOpenDomain()
                || instance.isAutoCloseOnComplete()
                || instance.isSkyboxOnlyExpand();
        if (instance.isExpanding() && !instantOrOpen)
        {
            return;
        }
        ServerLevel level = owner.serverLevel();
        if (!instance.getDimension().equals(level.dimension()))
        {
            return;
        }
        BlockPos center = instance.getCenter();
        int radius = instance.getRadius();
        double radiusSqr = (double) radius * (double) radius;
        UUID ownerUuid = owner.getUUID();
        Set<UUID> openSettled = instance.isOpenDomain()
                ? openDomainSettledByCaster.get(ownerUuid)
                : null;

        AABB queryBox = new AABB(
                center.getX() - radius, center.getY() - radius, center.getZ() - radius,
                center.getX() + radius + 1.0D, center.getY() + radius + 1.0D, center.getZ() + radius + 1.0D
        );
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, queryBox, LivingEntity::isAlive))
        {
            if (target.getUUID().equals(ownerUuid) || target instanceof SpecialEffectEntity)
            {
                continue;
            }
            if (isExcludedFromBeingLimitlessDomainMobTarget(owner, ownerUuid, target))
            {
                continue;
            }
            if (!isInsideDomainSphere(target.position(), center, radiusSqr))
            {
                continue;
            }
            if (openSettled != null && openSettled.contains(target.getUUID()))
            {
                continue;
            }
            applyDomainGarbledFx(target);
        }
    }

    private void broadcastGarbledBurstAround(LivingEntity target)
    {
        if (!(target.level() instanceof ServerLevel level))
        {
            return;
        }
        double x = target.getX();
        double y = target.getY() + target.getBbHeight() * 0.65D;
        double z = target.getZ();
        double radiusSqr = GARBLED_BURST_SYNC_RADIUS * GARBLED_BURST_SYNC_RADIUS;
        for (ServerPlayer nearby : level.players())
        {
            if (nearby.distanceToSqr(x, y, z) > radiusSqr)
            {
                continue;
            }
            ModNetwork.sendLimitlessDomainGarbledBurst(nearby, x, y, z);
        }
    }

    /**
     * 从指定字符池随机取样并拼接到消息中。
     */
    private void appendRandomChars(StringBuilder builder, char[] sourceChars, int minLen, int maxLen, ThreadLocalRandom random)
    {
        int len = minLen + random.nextInt(maxLen - minLen + 1);
        for (int i = 0; i < len; i++)
        {
            builder.append(sourceChars[random.nextInt(sourceChars.length)]);
        }
    }

    /**
     * 每服务器 tick 至多执行一次：延续锁定、近身演出锁定等（不依赖施法者是否在线）。
     */
    private void tickPostDomainLocksOncePerServerTick(ServerPlayer player)
    {
        MinecraftServer server = player.getServer();
        if (server == null)
        {
            return;
        }
        int tick = server.getTickCount();
        if (tick == lastPostDomainLockProcessedServerTick)
        {
            return;
        }
        lastPostDomainLockProcessedServerTick = tick;
        tickPostDomainLocks(server);
        tickNearCasterMobLocks(server);
    }

    private void tickPostDomainLocks(MinecraftServer server)
    {
        if (postDomainMobLockMap.isEmpty())
        {
            return;
        }
        List<UUID> toRemove = new ArrayList<>();
        for (Map.Entry<UUID, PostDomainMobLockState> entry : postDomainMobLockMap.entrySet())
        {
            UUID uuid = entry.getKey();
            PostDomainMobLockState state = entry.getValue();
            LivingEntity target = findLivingEntity(server, uuid);
            if (target == null || !target.isAlive())
            {
                releasePostDomainMobLock(uuid, state, server);
                toRemove.add(uuid);
                continue;
            }
            int remainingAfter = state.remainingTicks() - 1;
            if (state.enforceAnchorPose())
            {
                lockEntityToSouth(target, state.lockPos());
            }
            applyLockBlindness(target);
            if (target instanceof Mob mob && state.aiPausedByThisLock())
            {
                mob.setNoAi(true);
            }
            applyDomainGarbledFx(target);
            if (remainingAfter <= 0)
            {
                releasePostDomainMobLock(uuid, state, server);
                toRemove.add(uuid);
            }
            else
            {
                postDomainMobLockMap.put(uuid, state.withRemainingTicks(remainingAfter));
            }
        }
        for (UUID uuid : toRemove)
        {
            postDomainMobLockMap.remove(uuid);
        }
    }

    private static LivingEntity findLivingEntity(MinecraftServer server, UUID uuid)
    {
        for (ServerLevel level : server.getAllLevels())
        {
            Entity entity = level.getEntity(uuid);
            if (entity instanceof LivingEntity living)
            {
                return living;
            }
        }
        return null;
    }

    private void tickNearCasterMobLocks(MinecraftServer server)
    {
        if (nearCasterMobLockMap.isEmpty())
        {
            return;
        }
        List<UUID> toRemove = new ArrayList<>();
        for (Map.Entry<UUID, NearCasterMobLockState> entry : nearCasterMobLockMap.entrySet())
        {
            UUID uuid = entry.getKey();
            NearCasterMobLockState state = entry.getValue();
            LivingEntity target = findLivingEntity(server, uuid);
            if (target == null || !target.isAlive() || target instanceof ServerPlayer)
            {
                releaseNearCasterMobLock(uuid, state, server);
                toRemove.add(uuid);
                continue;
            }
            int remainingAfter = state.remainingTicks() - 1;
            applyCasterStyleLockToNonPlayerLiving(target, state.lockPos(), state.lockXRot());
            if (target instanceof Mob mob && state.aiPausedByThisLock())
            {
                mob.setNoAi(true);
            }
            if (remainingAfter <= 0)
            {
                releaseNearCasterMobLock(uuid, state, server);
                toRemove.add(uuid);
            }
            else
            {
                nearCasterMobLockMap.put(uuid, state.withRemainingTicks(remainingAfter));
            }
        }
        for (UUID uuid : toRemove)
        {
            nearCasterMobLockMap.remove(uuid);
        }
    }

    private void applyCasterStyleLockToNonPlayerLiving(LivingEntity entity, Vec3 lockPos, float lockXRot)
    {
        entity.setDeltaMovement(Vec3.ZERO);
        entity.hurtMarked = true;
        entity.teleportTo(lockPos.x, lockPos.y, lockPos.z);
        entity.setYRot(NORTH_YAW);
        entity.setYBodyRot(NORTH_YAW);
        entity.setYHeadRot(NORTH_YAW);
        entity.setXRot(lockXRot);
    }

    private void releaseNearCasterMobLock(UUID targetUuid, NearCasterMobLockState state, MinecraftServer server)
    {
        LivingEntity target = findLivingEntity(server, targetUuid);
        if (target instanceof Mob mob && state.aiPausedByThisLock())
        {
            mob.setNoAi(state.originNoAi());
        }
    }

    private void releasePostDomainMobLock(UUID targetUuid, PostDomainMobLockState state, MinecraftServer server)
    {
        LivingEntity target = findLivingEntity(server, targetUuid);
        if (target instanceof Mob mob && state.aiPausedByThisLock())
        {
            mob.setNoAi(state.originNoAi());
        }
        if (target instanceof ServerPlayer targetPlayer)
        {
            ModNetwork.sendPlayerInputLockSync(targetPlayer, false);
        }
        else
        {
            ServerPlayer onlinePlayer = server.getPlayerList().getPlayer(targetUuid);
            if (onlinePlayer != null)
            {
                ModNetwork.sendPlayerInputLockSync(onlinePlayer, false);
            }
        }
    }

    /**
     * 释放被锁定目标：恢复 AI（仅恢复本次锁定主动改动的对象）并解除玩家输入锁。
     */
    private void releaseMobLock(UUID targetUuid, MobLockState lockState, ServerLevel fallbackLevel)
    {
        if (fallbackLevel.getEntity(targetUuid) instanceof LivingEntity target)
        {
            if (target instanceof Mob mob && lockState.aiPausedByThisLock())
            {
                mob.setNoAi(lockState.originNoAi());
            }
            if (target instanceof ServerPlayer targetPlayer)
            {
                ModNetwork.sendPlayerInputLockSync(targetPlayer, false);
            }
        }
        else
        {
            ServerPlayer onlinePlayer = fallbackLevel.getServer().getPlayerList().getPlayer(targetUuid);
            if (onlinePlayer != null)
            {
                ModNetwork.sendPlayerInputLockSync(onlinePlayer, false);
            }
        }
    }

    @Override
    protected void tickOpenDomainMaintenance(ServerPlayer player, BarrierInstance instance)
    {
        tickOpenDomainPresence(player, instance);
        spawnOpenDomainFloorParticles(player, instance);
        // 持续同步球心/半径，使走进可视范围的玩家也能收到边界 spaceWarp
        broadcastOpenDomainCubemap(player, instance, true);
    }

    /**
     * 开放式：在半径内实际地表刷白色尘屑，强化无量空处地板氛围。
     */
    private void spawnOpenDomainFloorParticles(ServerPlayer caster, BarrierInstance instance)
    {
        ServerLevel level = caster.serverLevel();
        if (!level.dimension().equals(instance.getDimension()))
        {
            return;
        }
        BlockPos center = instance.getCenter();
        double radius = instance.getRadius();
        double radiusSqr = radius * radius;
        double cx = center.getX() + 0.5D;
        double cy = center.getY() + 0.5D;
        double cz = center.getZ() + 0.5D;
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < OPEN_DOMAIN_FLOOR_PARTICLES_PER_TICK; i++)
        {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double dist = Math.sqrt(random.nextDouble()) * radius;
            double x = cx + Math.cos(angle) * dist;
            double z = cz + Math.sin(angle) * dist;
            int blockX = (int) Math.floor(x);
            int blockZ = (int) Math.floor(z);
            int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockX, blockZ);
            double y = surfaceY + 0.08D;
            double dx = x - cx;
            double dy = y - cy;
            double dz = z - cz;
            if (dx * dx + dy * dy + dz * dz >= radiusSqr)
            {
                continue;
            }
            level.sendParticles(OPEN_DOMAIN_FLOOR_DUST, x, y, z, 1, 0.18D, 0.02D, 0.18D, 0.0D);
        }
    }

    private void beginOpenDomainSession(ServerPlayer caster, BarrierInstance instance)
    {
        UUID casterUuid = caster.getUUID();
        openDomainDwellByCaster.put(casterUuid, new ConcurrentHashMap<>());
        openDomainSettledByCaster.put(casterUuid, ConcurrentHashMap.newKeySet());
        broadcastOpenDomainCubemap(caster, instance, true);
        tickOpenDomainPresence(caster, instance);
    }

    /**
     * 开放式会话（施法者无关版）。AI 实体恒走常规领域，不存在开放式分支；
     * 玩家走 {@link #beginOpenDomainSession(ServerPlayer, BarrierInstance)}。
     */
    private void beginOpenDomainSession(DomainCaster caster, BarrierInstance instance)
    {
        ServerPlayer player = caster.player();
        if (player != null)
        {
            beginOpenDomainSession(player, instance);
        }
    }

    private void endOpenDomainSession(ServerPlayer caster, BarrierInstance instance, boolean settleRemaining)
    {
        UUID casterUuid = caster.getUUID();
        if (settleRemaining)
        {
            ConcurrentMap<UUID, Integer> dwellMap = openDomainDwellByCaster.get(casterUuid);
            if (dwellMap != null)
            {
                for (Map.Entry<UUID, Integer> entry : Set.copyOf(dwellMap.entrySet()))
                {
                    settleOpenDomainAftermathOnce(caster, entry.getKey(), entry.getValue());
                }
            }
        }
        openDomainDwellByCaster.remove(casterUuid);
        openDomainSettledByCaster.remove(casterUuid);
        broadcastOpenDomainCubemap(caster, instance, false);
    }

    private void tickOpenDomainPresence(ServerPlayer caster, BarrierInstance instance)
    {
        ServerLevel level = caster.serverLevel();
        if (!level.dimension().equals(instance.getDimension()))
        {
            return;
        }
        UUID casterUuid = caster.getUUID();
        ConcurrentMap<UUID, Integer> dwellMap =
                openDomainDwellByCaster.computeIfAbsent(casterUuid, key -> new ConcurrentHashMap<>());
        Set<UUID> settled = openDomainSettledByCaster.computeIfAbsent(casterUuid, key -> ConcurrentHashMap.newKeySet());

        BlockPos center = instance.getCenter();
        double radius = instance.getRadius();
        double radiusSqr = radius * radius;
        Vec3 centerVec = Vec3.atCenterOf(center);
        AABB box = new AABB(centerVec, centerVec).inflate(radius);
        Set<UUID> inside = ConcurrentHashMap.newKeySet();

        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive))
        {
            if (living.getUUID().equals(casterUuid))
            {
                continue;
            }
            if (living instanceof SpecialEffectEntity)
            {
                continue;
            }
            if (isExcludedFromBeingLimitlessDomainMobTarget(caster, casterUuid, living))
            {
                continue;
            }
            if (!isInsideDomainSphere(living.position(), center, radiusSqr))
            {
                continue;
            }
            UUID id = living.getUUID();
            inside.add(id);
            if (settled.contains(id))
            {
                continue;
            }
            dwellMap.merge(id, 1, Integer::sum);
            registerOpenDomainEffectLock(caster, living, level);
        }

        for (UUID id : Set.copyOf(dwellMap.keySet()))
        {
            if (inside.contains(id))
            {
                continue;
            }
            Integer dwell = dwellMap.remove(id);
            if (dwell != null)
            {
                settleOpenDomainAftermathOnce(caster, id, dwell);
            }
        }
    }

    /**
     * 开放式：在当前位置施加无量空处必中效果（失明 / 乱码 / AI·输入锁），不搬点、不朝南锁位。
     */
    private void registerOpenDomainEffectLock(ServerPlayer caster, LivingEntity target, ServerLevel level)
    {
        if (mobLockMap.containsKey(target.getUUID()))
        {
            return;
        }
        if (isExcludedFromBeingLimitlessDomainMobTarget(caster, caster.getUUID(), target))
        {
            return;
        }
        MinecraftServer server = level.getServer();
        PostDomainMobLockState lingeringPost = postDomainMobLockMap.remove(target.getUUID());
        if (lingeringPost != null && server != null)
        {
            releasePostDomainMobLock(target.getUUID(), lingeringPost, server);
        }
        NearCasterMobLockState lingeringNear = nearCasterMobLockMap.remove(target.getUUID());
        if (lingeringNear != null && server != null)
        {
            releaseNearCasterMobLock(target.getUUID(), lingeringNear, server);
        }
        boolean aiPausedByThisLock = false;
        boolean originNoAi = false;
        if (target instanceof Mob mob)
        {
            originNoAi = mob.isNoAi();
            if (!originNoAi)
            {
                mob.setNoAi(true);
                aiPausedByThisLock = true;
            }
        }
        if (target instanceof ServerPlayer targetPlayer)
        {
            ModNetwork.sendPlayerInputLockSync(targetPlayer, true);
        }
        applyLockBlindness(target);
        mobLockMap.put(target.getUUID(), new MobLockState(
                caster.getUUID(),
                target.position(),
                aiPausedByThisLock,
                originNoAi,
                0));
    }

    private void settleOpenDomainAftermathOnce(ServerPlayer caster, UUID targetId, int dwellTicks)
    {
        UUID casterUuid = caster.getUUID();
        Set<UUID> settled = openDomainSettledByCaster.computeIfAbsent(casterUuid, key -> ConcurrentHashMap.newKeySet());
        if (!settled.add(targetId))
        {
            return;
        }
        ConcurrentMap<UUID, Integer> dwellMap = openDomainDwellByCaster.get(casterUuid);
        if (dwellMap != null)
        {
            dwellMap.remove(targetId);
        }

        MobLockState priorLock = mobLockMap.remove(targetId);
        MinecraftServer server = caster.getServer();
        LivingEntity living = findLivingEntity(server, targetId);
        if (living instanceof ServerPlayer victim)
        {
            PlayerBrainDamageData.addFromLimitlessDomainDwell(victim, dwellTicks, false);
        }

        int postTicks = Math.min(dwellTicks * 3, POST_DOMAIN_LOCK_MAX_TICKS);
        if (postTicks <= 0 || living == null || !living.isAlive())
        {
            if (priorLock != null)
            {
                releaseMobLock(targetId, priorLock, caster.serverLevel());
            }
            return;
        }

        boolean aiPausedByUs = priorLock != null && priorLock.aiPausedByThisLock();
        boolean originNoAi = priorLock != null && priorLock.originNoAi();
        if (priorLock == null)
        {
            if (living instanceof Mob mob)
            {
                originNoAi = mob.isNoAi();
                if (!originNoAi)
                {
                    mob.setNoAi(true);
                    aiPausedByUs = true;
                }
            }
            if (living instanceof ServerPlayer targetPlayer)
            {
                ModNetwork.sendPlayerInputLockSync(targetPlayer, true);
            }
        }

        Vec3 lockPos = priorLock != null ? priorLock.lockPos() : living.position();
        postDomainMobLockMap.put(targetId, new PostDomainMobLockState(
                lockPos,
                postTicks,
                aiPausedByUs,
                originNoAi,
                false
        ));
    }

    private void broadcastOpenDomainCubemap(ServerPlayer caster, BarrierInstance instance, boolean active)
    {
        ServerLevel level = caster.serverLevel();
        BlockPos center = instance.getCenter();
        double cx = center.getX() + 0.5D;
        double cy = center.getY() + 0.5D;
        double cz = center.getZ() + 0.5D;
        int radius = instance.getRadius();
        ResourceLocation texture = instance.getCustomSkyboxTexture() != null
                ? instance.getCustomSkyboxTexture()
                : LIMITLESS_DOMAIN_SKYBOX_TEXTURE_AFTER_280;
        double syncRadius = Math.max(radius + 32.0D, 96.0D);
        double syncRadiusSqr = syncRadius * syncRadius;
        for (ServerPlayer nearby : level.players())
        {
            double dx = nearby.getX() - cx;
            double dy = nearby.getY() - cy;
            double dz = nearby.getZ() - cz;
            if (dx * dx + dy * dy + dz * dz > syncRadiusSqr)
            {
                continue;
            }
            ModNetwork.sendOpenDomainCubemapSync(nearby, active, cx, cy, cz, radius, texture);
        }
    }

    private boolean isInsideDomainSphere(Vec3 entityPos, BlockPos center, double radiusSqr)
    {
        double dx = entityPos.x - (center.getX() + 0.5D);
        double dy = entityPos.y - (center.getY() + 0.5D);
        double dz = entityPos.z - (center.getZ() + 0.5D);
        return dx * dx + dy * dy + dz * dz < radiusSqr;
    }

    /**
     * 生成“north 方向附近”的分散站位，避免多个生物叠在同一方块。
     */
    private List<Vec3> buildScatterPositions(Vec3 anchor, int count)
    {
        List<Vec3> positions = new ArrayList<>(count);
        for (int i = 0; i < count; i++)
        {
            if (i == 0)
            {
                positions.add(anchor);
                continue;
            }
            double angle = Math.toRadians(i * 137.508D);
            double distance = MOB_SCATTER_SPACING * Math.sqrt(i);
            double x = anchor.x + Math.cos(angle) * distance;
            double z = anchor.z + Math.sin(angle) * distance;
            positions.add(new Vec3(x, anchor.y, z));
        }
        return positions;
    }

    private record PlayerLockState(Vec3 lockPos, float lockXRot, int remainingTicks)
    {
        private PlayerLockState decreaseOneTick()
        {
            return new PlayerLockState(lockPos, lockXRot, remainingTicks - 1);
        }
    }

    private record MobLockState(UUID ownerUuid, Vec3 lockPos, boolean aiPausedByThisLock, boolean originNoAi, int domainLockElapsedTicks)
    {
        private MobLockState withIncrementedDomainTicks()
        {
            return new MobLockState(ownerUuid, lockPos, aiPausedByThisLock, originNoAi, domainLockElapsedTicks + 1);
        }
    }

    private record PostDomainMobLockState(Vec3 lockPos, int remainingTicks, boolean aiPausedByThisLock, boolean originNoAi, boolean enforceAnchorPose)
    {
        private PostDomainMobLockState withRemainingTicks(int ticks)
        {
            return new PostDomainMobLockState(lockPos, ticks, aiPausedByThisLock, originNoAi, enforceAnchorPose);
        }
    }

    private record NearCasterMobLockState(Vec3 lockPos, float lockXRot, int remainingTicks, boolean aiPausedByThisLock, boolean originNoAi)
    {
        private NearCasterMobLockState withRemainingTicks(int ticks)
        {
            return new NearCasterMobLockState(lockPos, lockXRot, ticks, aiPausedByThisLock, originNoAi);
        }
    }
}
