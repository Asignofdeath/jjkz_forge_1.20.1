package com.darkness.jujutsukaisenzone.quest.mainline.main_1_3;

import com.darkness.jujutsukaisenzone.cg.PlayerCgLock;
import com.darkness.jujutsukaisenzone.compat.EpicFightCompat;
import com.darkness.jujutsukaisenzone.entity.CutsceneActorEntity;
import com.darkness.jujutsukaisenzone.entity.GojoSatoruEntity;
import com.darkness.jujutsukaisenzone.entity.Grade3CurseSpiritAEntity;
import com.darkness.jujutsukaisenzone.network.ModNetwork;
import com.darkness.jujutsukaisenzone.network.NetworkSenders;
import com.darkness.jujutsukaisenzone.network.packet.MainQuest13CmdCamS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncCgLookClampS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncQuestCodCaptionsS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncQuestRedBlackoutS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncQuestWakeBlinkS2CPacket;
import com.darkness.jujutsukaisenzone.player.PlayerQuestData;
import com.darkness.jujutsukaisenzone.quest.mainline.MainQuestDefs;
import com.darkness.jujutsukaisenzone.technique.TechniqueAtkObjShackle;
import com.darkness.jujutsukaisenzone.technique.domain.AiDomainCasterRegistry;
import com.darkness.jujutsukaisenzone.technique.domain.DomainExpansionRouter;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

/**
 * 主线 1.3「特级术师的实力」演出总控。
 * <p>
 * <b>⚠️ 半成品</b>：本演出<b>不止这几个阶段</b>，终局之后还有后续剧情待接。
 * 终局是当前的<b>终点</b>——领域展开后停驻在此，<b>不做任何收尾</b>（不撤域、不删五条悟、不交还控制）。
 * <p>
 * 阶段划分（全程<b>不可跳过</b>：锁输入直到后续阶段接入）：
 * <ol>
 *   <li><b>阶段 0</b>：拾取上下文（玩家坐标 / 朝向），枚举并登记场上三级咒灵甲（可逆）；
 *       切战斗模式；COD 夹锥看向五条悟预定站位（同 1.1 {@link PlayerCgLock#beginLookAt}）。</li>
 *   <li><b>阶段 1</b>：立刻红屏 → 渐变黑屏（{@link SyncQuestRedBlackoutS2CPacket}）。</li>
 *   <li><b>阶段 2</b>：黑屏保持，铺陈「昏迷」节奏；结束后撤红黑遮罩。</li>
 *   <li><b>阶段 3</b>：五条悟 Geo 演员登场 + 倒地/背影镜与 4 秒醒来眨眼同一拍；
 *       睁眼后再 COD（句 1～3：神秘白发男子 / 玩家认出 / 五条悟）。</li>
 *   <li><b>阶段 4</b>：cmdcam 切正脸特写 → 慢放 {@code take_off_the_blindfold} 并定格 → 句 4～5。</li>
 *   <li><b>阶段 5</b>：句 6 开口，Geo 演员换成 {@link GojoSatoruEntity}，由它
 *       {@link AiDomainCasterRegistry#castLimitlessDomain} 走领域自带蓄力手势与完整真实演出
 *       （白幕、搬点朝南、必中、280 刻后天空盒切换 + 水墨 + 闪光 + 环境音）。
 *       <b>领域与五条悟留场</b>，其生命周期由 AI 自行维持（主动解除 / 咒力耗尽）。</li>
 * </ol>
 */
public final class MainQuest13Performance
{
    private static final Logger LOGGER = LogUtils.getLogger();

    // ==================== 阶段 1：红屏→黑屏 ====================
    /** 黑屏期铺陈昏迷节奏（不少于红屏渐变时长）。 */
    private static final int BLACKOUT_TICKS = 40;
    /** 1.2→1.3 红黑幕期间三级咒灵甲残响（与 {@link #BLACKOUT_TICKS} 同长）。 */
    private static final String BLACKOUT_CURSE_KEY = "quest.jujutsukaisenzone.main_1_2.blackout.1";
    private static final int BLACKOUT_CURSE_HOLD_TICKS = 80;

    /** 看向五条悟时的头部高度偏移（格），与 1.1 小屋对话对成人演员的口径一致。 */
    private static final double GOJO_LOOK_HEAD_Y = 1.4D;

    // ==================== 阶段 3：五条悟登场 + COD 对白 ====================
    /**
     * 登场对白（6 句）：神秘白发男子 → 玩家认出 → 此后署名五条悟。
     * <p>
     * 句间隔按 ogg 时长 + 10 刻空隙（换句会停上一句语音）。玩家句无配音，hold 40 刻。
     * 出现时刻：0 / 99 / 147 / 265 / 442 / 606。
     */
    private static final String[] GOJO_ENTER_DIALOG_KEYS = {
            "quest.jujutsukaisenzone.main_1_3.talk.1",
            "quest.jujutsukaisenzone.main_1_3.talk.2",
            "quest.jujutsukaisenzone.main_1_3.talk.3",
            "quest.jujutsukaisenzone.main_1_3.talk.4",
            "quest.jujutsukaisenzone.main_1_3.talk.5",
            "quest.jujutsukaisenzone.main_1_3.talk.6"
    };
    /** 相对上一句出现的间隔（首句 0）。 */
    private static final int[] GOJO_ENTER_DIALOG_DELAYS = {0, 99, 48, 118, 177, 164};
    private static final int[] GOJO_ENTER_DIALOG_HOLDS = {99, 48, 118, 177, 164, 80};

    /** 背影 + 挡在身前定格：覆盖句 1～3，切正脸时句 4 起（不含醒来眨眼的 80 刻）。 */
    private static final int GOJO_BACK_VIEW_TICKS = 265;
    /** 撤红黑后：多次眨眼再睁眼（4s）。 */
    private static final int WAKE_BLINK_TICKS = 80;

    /**
     * Geo 演员 {@code stand_up_and_protect} 起播后，隔多久切到
     * {@code stand_up_and_protect_and_look_back}（转头看一眼玩家、再复原，定格末帧）：3 秒。
     */
    private static final int GOJO_LOOK_BACK_DELAY_TICKS = 60;

    // ==================== 阶段 4：五条悟登场 ====================
    /** 五条悟站在玩家「正前方」的距离（格）。 */
    private static final double GOJO_FORWARD_DISTANCE = 2.2D;
    /** 五条悟相对玩家的横向偏移（0 = 正前）。 */
    private static final double GOJO_SIDE_OFFSET = 0.0D;
    /** 五条悟朝向偏移：0 = 与玩家同向（背对玩家，背影挡在身前）。 */
    private static final float GOJO_FACE_PLAYER = 0.0F;

    // ==================== 阶段 5：正脸特写 + 摘眼罩 ====================
    /**
     * 正脸段：慢放摘眼罩（1.75s×0.5＝70 刻）后定格末帧，覆盖句 4～5。
     * 结束时刻 = 句 6 开口，换 AI 正主开领域蓄力。
     */
    private static final int FACE_CLOSEUP_TICKS = 341;

    /** 冻结三级咒灵的搜索半径。 */
    private static final double FREEZE_RADIUS = 48.0D;

    // ==================== 阶段 4-7：芝诺墙（无下限） ====================
    /**
     * 演出芝诺墙的作用半径（格）：以「五条悟与玩家的中点」为心，把<b>两人一起</b>包住。
     * <p>
     * 硬钳位：圈内非白名单生物推到外沿；飞行物进圈即丢弃。白名单 = 本场玩家、五条悟。
     */
    private static final double ZENO_RANGE = 8.0D;
    /** 展开时把圈内非白名单生物弹出的水平冲量。 */
    private static final double ZENO_BURST = 0.85D;
    /**
     * 咒灵甲环绕「墙心」布置的半径（格）：在墙外沿（{@link #ZENO_RANGE}）之外留出这一段，
     * 让它们先正经冲一段、再一头撞进墙里，而不是凭空贴在墙上。
     */
    private static final double CURSE_ARRANGE_RADIUS = 12.0D;

    // ==================== 阶段 7：领域成型小灶（临时） ====================
    /**
     * 领域成型小灶的作用半径（格）：以五条悟为心，清掉这一段内的<b>非玩家</b>生物。
     * <p>
     * <b>⚠️ 临时小灶，纯为叙事稳定性服务</b>：五条悟开领域是「护住学生」的演出，玩家身旁若正好站着
     * 咒灵，会贴着玩家入镜、破坏画面。此处在领域成型的一刻把近身生物清掉，玩家自身<b>排斥</b>在外。
     * <p>
     * 语义与领域侧的 {@code EXCLUDE_CASTER_RADIUS} 一致（同为 3 格），但那是「豁免圈」，
     * 这里是「清场圈」——方向相反，别混。
     */
    private static final double DOMAIN_FORMED_PURGE_RADIUS = 3.0D;

    /**
     * 从进入阶段 7（= {@code castLimitlessDomain()} 发起）算起，等待多少刻再执行小灶。
     * <p>
     * 无量空处是「先蓄力 {@code CAST_DELAY_TICKS = 60} 刻 → 再逐批砌壳」：球壳半径 24，
     * 每 tick 砌 {@code BLOCKS_PER_BATCH = 256} 块，壳块数约 7.2k ⇒ 砌壳约 30 刻。
     * 取 100 刻（5 秒）留足余量，确保是「构建完成」而非「构建中」。
     */
    private static final int DOMAIN_FORMED_PURGE_DELAY_TICKS = 100;

    // ==================== 阶段 5：五条悟实体登场锁 ====================
    /**
     * 五条悟实体生成后，硬锁朝向 + 硬锁移动的时长（刻）：2 秒。
     * <p>
     * 从 {@link #buildDomain} 把 Geo 演员换成 {@link GojoSatoruEntity} 的那一刻起算，
     * 压住蓄力 / 结印期的乱动乱转；到点即放手，<b>不干预</b>领域后续自己的强制朝向。
     */
    private static final int GOJO_SWAP_LOCK_TICKS = 40;

    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    private MainQuest13Performance()
    {
    }

    public static boolean isRunning(ServerPlayer player)
    {
        return SESSIONS.containsKey(player.getUUID());
    }

    public static void startIfNeeded(ServerPlayer player)
    {
        if (!MainQuestDefs.MAIN_1_3_ID.equals(PlayerQuestData.getTrackedId(player)))
        {
            return;
        }
        if (PlayerQuestData.isCompleted(player, MainQuestDefs.MAIN_1_3_ID))
        {
            return;
        }
        if (PlayerQuestData.isMain13PerformanceDone(player))
        {
            return;
        }
        if (SESSIONS.containsKey(player.getUUID()))
        {
            return;
        }

        Session session = new Session();
        captureContext(player, session);
        SESSIONS.put(player.getUUID(), session);

        // 阶段 1：红屏 → 黑屏（立刻），并锁输入
        player.closeContainer();
        ModNetwork.sendPlayerInputLockSync(player, true);
        // Epic Fight 倒地只驱动战斗模式骨架；先切战斗模式，倒地与 cmdcam 同一拍再播（同 falltest）。
        EpicFightCompat.setBattleMode(player, true);
        beginCgLookAtGojo(player, session);
        NetworkSenders.toPlayer(player, SyncQuestCodCaptionsS2CPacket.clearImmediate());
        NetworkSenders.toPlayer(player, SyncQuestRedBlackoutS2CPacket.show());
        NetworkSenders.toPlayer(player, SyncQuestCodCaptionsS2CPacket.playUniform(
                new String[] {BLACKOUT_CURSE_KEY},
                0,
                BLACKOUT_CURSE_HOLD_TICKS));

        // 阶段 2 起：黑屏保持
        session.phase = Phase.BLACKOUT_HOLD;
        session.phaseStartTick = session.ticks;

        LOGGER.info("[Cutscene] Main 1.3 performance start for {} (battle mode + COD look + red->blackout).",
                player.getName().getString());
    }

    public static void abort(ServerPlayer player)
    {
        Session session = SESSIONS.remove(player.getUUID());
        EpicFightCompat.stopPlayerFellToTheGroundAnimation(player);
        PlayerCgLock.end(player);
        NetworkSenders.toPlayer(player, SyncQuestRedBlackoutS2CPacket.clear());
        NetworkSenders.toPlayer(player, SyncCgLookClampS2CPacket.clear());
        NetworkSenders.toPlayer(player, SyncQuestCodCaptionsS2CPacket.clearImmediate());
        NetworkSenders.toPlayer(player, SyncQuestWakeBlinkS2CPacket.clear());
        ModNetwork.sendPlayerInputLockSync(player, false);
        player.setInvisible(false);
        MainQuest13Actors.despawnFor(player);
        if (session != null)
        {
            EpicFightCompat.setBattleMode(player, session.wasEpicFightMode);
            unfreezeCurses(session);
            session.gojoSwapLock = false;
            // 中断落在 2 秒窗口内时也要把 AI 放回去，别让实体停在 noAi。
            if (session.level != null && session.gojoEntityId != null
                    && session.level.getEntity(session.gojoEntityId) instanceof Mob gojo)
            {
                gojo.setNoAi(false);
            }
        }
    }

    public static void tick(ServerPlayer player)
    {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null)
        {
            return;
        }
        // 全程钉住玩家（玩家不移动；镜头由 CMDCam 接管）
        player.setDeltaMovement(Vec3.ZERO);
        player.xxa = 0.0F;
        player.zza = 0.0F;
        PlayerCgLock.tick(player);

        session.ticks++;
        // 芝诺墙：挡住圈内非白名单生物与飞行物（白名单 = 本场玩家、五条悟）。
        tickZenoWall(player, session);
        // 五条悟实体登场锁：生成后 2 秒内暂停 AI + 硬锁朝向 + 硬锁移动（详见 tickGojoSwapLock）。
        tickGojoSwapLock(session);
        // 阶段 3：Geo 演员 stand_up_and_protect 起播 3 秒后，切 look_back 段。
        tickGojoActorLookBack(player, session);
        switch (session.phase)
        {
            case BLACKOUT_HOLD -> tickBlackoutHold(player, session);
            case WAKE_BLINK -> tickWakeBlink(player, session);
            case GOJO_ENTER -> tickGojoEnter(player, session);
            case FACE_CLOSEUP -> tickFaceCloseup(player, session);
            case SWAP_MODEL -> tickSwapModel(player, session);
            case DOMAIN -> tickDomain(player, session);
        }
    }

    // ==================== 阶段推进 ====================

    /** 阶段 2：黑屏保持一小段；到点撤红黑遮罩，五条悟登场 + 背影镜。 */
    private static void tickBlackoutHold(ServerPlayer player, Session session)
    {
        if (session.ticks - session.phaseStartTick < BLACKOUT_TICKS)
        {
            return;
        }
        spawnGojo(player, session);
        if (!session.running)
        {
            return;
        }
        // 从阶段 3 起由 CMDCam 接管运镜：只钉站位、撤下 COD 视角夹锥（同 pinOnly 语义）。
        PlayerCgLock.pinOnly(player, session.originPos);
        CutsceneActorEntity gojo = MainQuest13Actors.actor(player, MainQuest13Actors.ROLE_GOJO);
        double px = session.originPos.x;
        double pz = session.originPos.z;
        double gy = gojo != null ? gojo.getY() : session.originPos.y;
        double gx = gojo != null ? gojo.getX() : px;
        double gz = gojo != null ? gojo.getZ() : pz;
        // 与 /jjkz falltest 同一拍：倒地 Instantly + 背影 cmdcam。
        EpicFightCompat.playPlayerFellToTheGroundAnimation(player);
        NetworkSenders.toPlayer(player, MainQuest13CmdCamS2CPacket.backView(
                px, session.originPos.y, pz, gx, gz));
        session.gojoFeetY = gy;
        // 先眨眼（从全闭接上红黑幕），再撤红黑，避免闪一帧实景。对白等睁眼后再播。
        NetworkSenders.toPlayer(player, SyncQuestCodCaptionsS2CPacket.fadeAll());
        NetworkSenders.toPlayer(player, SyncQuestWakeBlinkS2CPacket.play());
        NetworkSenders.toPlayer(player, SyncQuestRedBlackoutS2CPacket.clear());
        advance(session, Phase.WAKE_BLINK);
        LOGGER.info("[Cutscene] Main 1.3 phase 3: Gojo enters (wake blink + back view).");
    }

    /** 醒来眨眼 4 秒后再出 COD 对白。 */
    private static void tickWakeBlink(ServerPlayer player, Session session)
    {
        if (session.ticks - session.phaseStartTick < WAKE_BLINK_TICKS)
        {
            return;
        }
        playGojoEnterDialog(player);
        advance(session, Phase.GOJO_ENTER);
        LOGGER.info("[Cutscene] Main 1.3: wake blink done, COD dialog starts.");
    }

    /** 五条悟登场时启播 COD 弹幕对白队列。 */
    private static void playGojoEnterDialog(ServerPlayer player)
    {
        NetworkSenders.toPlayer(player, SyncQuestCodCaptionsS2CPacket.play(
                GOJO_ENTER_DIALOG_KEYS, GOJO_ENTER_DIALOG_DELAYS, GOJO_ENTER_DIALOG_HOLDS));
    }

    /** 阶段 3：背影镜头保持一段时间，随后切正脸特写。 */
    private static void tickGojoEnter(ServerPlayer player, Session session)
    {
        if (session.ticks - session.phaseStartTick < GOJO_BACK_VIEW_TICKS)
        {
            return;
        }
        // 阶段 4：正脸特写镜头
        CutsceneActorEntity gojo = MainQuest13Actors.actor(player, MainQuest13Actors.ROLE_GOJO);
        double gx = gojo != null ? gojo.getX() : session.originPos.x;
        double gz = gojo != null ? gojo.getZ() : session.originPos.z;
        float gYaw = gojo != null ? gojo.getYRot() : session.originYaw + GOJO_FACE_PLAYER;
        NetworkSenders.toPlayer(player, MainQuest13CmdCamS2CPacket.faceCloseup(gx, session.gojoFeetY, gz, gYaw));
        advance(session, Phase.FACE_CLOSEUP);
        LOGGER.info("[Cutscene] Main 1.3 phase 4: face close-up.");
    }

    /** 阶段 4 前半：正脸特写镜头播出，同时播摘眼罩动画。 */
    private static void tickFaceCloseup(ServerPlayer player, Session session)
    {
        if (!session.blindfoldStarted)
        {
            session.blindfoldStarted = true;
            CutsceneActorEntity gojo = MainQuest13Actors.actor(player, MainQuest13Actors.ROLE_GOJO);
            if (gojo != null)
            {
                gojo.setClip(CutsceneActorEntity.CLIP_GOJO_TAKE_OFF_THE_BLINDFOLD, false);
            }
            session.phaseStartTick = session.ticks;
            return;
        }
        if (session.ticks - session.phaseStartTick < FACE_CLOSEUP_TICKS)
        {
            return;
        }
        // 句 6 开口：换 AI 正主，领域自带蓄力手势。
        boolean built = buildDomain(player, session);
        if (built)
        {
            LOGGER.info("[Cutscene] Main 1.3 phase 5: real Unlimited Void domain cast started with talk 6.");
        }
        else
        {
            LOGGER.warn("[Cutscene] Main 1.3: domain build failed.");
        }
        advance(session, Phase.DOMAIN);
    }

    /** 兜底：正脸段未开域时由此立刻开域。 */
    private static void tickSwapModel(ServerPlayer player, Session session)
    {
        boolean built = buildDomain(player, session);
        if (built)
        {
            LOGGER.info("[Cutscene] Main 1.3 phase 5: real Unlimited Void domain built.");
        }
        else
        {
            LOGGER.warn("[Cutscene] Main 1.3: domain build failed.");
        }
        advance(session, Phase.DOMAIN);
    }

    /**
     * 阶段 5：无量空处已展开，演出停驻在此，等待后续阶段。
     * <p>
     * ⛔ <b>这里不做任何收尾</b>：不撤域、不删五条悟、不交还控制。1.3 并非只有这些阶段，
     * 后续剧情阶段会接在此处。领域与五条悟由 AI 自行维持其生命周期
     * （施法者主动解除 / 咒力耗尽，见领域实现），不归演出脚本管。
     */
    private static void tickDomain(ServerPlayer player, Session session)
    {
        // 停驻：无操作，等待后续阶段接入。
        // 唯一的例外 = 领域成型小灶（临时）：见 tickDomainFormedPurge。
        tickDomainFormedPurge(session);
    }

    /**
     * <b>领域成型小灶（临时，仅服务叙事稳定性）</b>：阶段 7 领域构建完成后，<b>一次性</b>清掉
     * 五条悟 {@link #DOMAIN_FORMED_PURGE_RADIUS} 格内的<b>非玩家</b>生物。
     * <p>
     * 判据 = 「领域确实已活跃」({@link DomainExpansionRouter#hasActiveDomain(UUID)}，进入蓄力时为 false，
     * 60 刻后真正展开才为 true) <b>且</b>距阶段 7 起始已过 {@link #DOMAIN_FORMED_PURGE_DELAY_TICKS} 刻
     * （留出砌壳动画时间）。两者同时满足才执行，执行后置 {@code domainPurgeDone}，永不再跑。
     * <p>
     * <b>玩家一律排斥</b>（不杀、不动）：小灶只针对咒灵等杂兵。五条悟自身也排除。
     */
    private static void tickDomainFormedPurge(Session session)
    {
        if (session.domainPurgeDone || session.level == null)
        {
            return;
        }
        UUID gojoId = session.gojoEntityId;
        if (gojoId == null || !DomainExpansionRouter.hasActiveDomain(gojoId))
        {
            return;
        }
        if (session.ticks - session.phaseStartTick < DOMAIN_FORMED_PURGE_DELAY_TICKS)
        {
            return;
        }

        Entity raw = session.level.getEntity(gojoId);
        if (!(raw instanceof LivingEntity gojo))
        {
            return;
        }

        session.domainPurgeDone = true;
        AABB box = gojo.getBoundingBox().inflate(DOMAIN_FORMED_PURGE_RADIUS);
        List<LivingEntity> nearby = session.level.getEntitiesOfClass(LivingEntity.class, box);
        int purged = 0;
        for (LivingEntity target : nearby)
        {
            if (target == gojo || target instanceof Player)
            {
                // 「排斥玩家」= 玩家不在清场对象内。
                continue;
            }
            target.kill();
            purged++;
        }
        if (purged > 0)
        {
            LOGGER.info("[Cutscene] Main 1.3 phase 7: domain-formed purge removed {} mob(s) within {} blocks.",
                    purged, DOMAIN_FORMED_PURGE_RADIUS);
        }
    }

    /**
     * 五条悟实体「登场锁」：从 {@link #buildDomain} 生成 {@link GojoSatoruEntity} 那一刻起，
     * 连续 {@link #GOJO_SWAP_LOCK_TICKS} 刻（2 秒）<b>暂停 AI</b>，并硬锁<b>朝向</b> + 硬锁<b>移动</b>，
     * 压住蓄力 / 结印期的乱动乱转。
     * <p>
     * 锁朝向 = Geo 演员消失时的朝向（{@code buildDomain} 里的 {@code yaw}），不是面向玩家。
     * 每 tick 复写位置 / 朝向并标记同步，抵抗同 tick 内其它系统的位姿改写；到点恢复 AI 并置
     * {@code gojoSwapLock=false} 放手，<b>不干预</b>领域后续自己的强制朝向。
     */
    private static void tickGojoSwapLock(Session session)
    {
        if (!session.gojoSwapLock || session.level == null
                || session.gojoEntityId == null || session.gojoLockPos == null)
        {
            return;
        }
        Entity raw = session.level.getEntity(session.gojoEntityId);
        Mob gojo = raw instanceof Mob mob ? mob : null;
        if (session.ticks - session.gojoSwapLockStart >= GOJO_SWAP_LOCK_TICKS)
        {
            session.gojoSwapLock = false;
            if (gojo != null)
            {
                gojo.setNoAi(false);
            }
            return;
        }
        if (gojo == null)
        {
            return;
        }
        // 暂停 AI：与位姿锁同一个 2 秒窗口。
        gojo.setNoAi(true);
        // 锁移动：速度清零 + 每 tick 拉回锚点，并标记同步。
        gojo.setDeltaMovement(Vec3.ZERO);
        gojo.hasImpulse = true;
        gojo.hurtMarked = true;
        gojo.teleportTo(session.gojoLockPos.x, session.gojoLockPos.y, session.gojoLockPos.z);
        // 锁朝向：身体 / 头部 / 俯仰一并钉住。
        gojo.setYRot(session.gojoLockYaw);
        gojo.setYBodyRot(session.gojoLockYaw);
        gojo.setYHeadRot(session.gojoLockYaw);
        gojo.setXRot(session.gojoLockPitch);
    }

    /**
     * 阶段 3 细化：Geo 演员的 {@code stand_up_and_protect} <b>起播 3 秒后</b>，切到
     * {@code stand_up_and_protect_and_look_back} —— 转头看一眼玩家、再复原，一次性、定格末帧。
     * <p>
     * 只在演员存在期间生效：切正脸时 {@code tickFaceCloseup} 会把 clip 换成摘眼罩、
     * 领域阶段演员已被实体替换；切完即置 {@code gojoLookBackPending=false}，不会重复触发。
     */
    private static void tickGojoActorLookBack(ServerPlayer player, Session session)
    {
        if (!session.gojoLookBackPending)
        {
            return;
        }
        if (session.ticks - session.gojoActorSpawnTick < GOJO_LOOK_BACK_DELAY_TICKS)
        {
            return;
        }
        session.gojoLookBackPending = false;
        CutsceneActorEntity gojo = MainQuest13Actors.actor(player, MainQuest13Actors.ROLE_GOJO);
        if (gojo == null)
        {
            return;
        }
        gojo.setClip(CutsceneActorEntity.CLIP_GOJO_STAND_UP_AND_PROTECT_AND_LOOK_BACK, false);
        LOGGER.info("[Cutscene] Main 1.3: Gojo actor switched to look-back clip (3s after stand-up).");
    }

    private static void advance(Session session, Phase next)
    {
        session.phase = next;
        session.phaseStartTick = session.ticks;
    }

    // ==================== 阶段 0：上下文 ====================

    /** 阶段 0：记录坐标 / 朝向 / 维度，登记场上三级咒灵甲（不冻结，仅记原状态以便还原）。 */
    private static void captureContext(ServerPlayer player, Session session)
    {
        session.originPos = player.position();
        session.originYaw = player.getYRot();
        session.level = player.serverLevel();
        session.wasEpicFightMode = EpicFightCompat.isBattleMode(player);
        rememberCurses(player, session);
    }

    /**
     * 登记附近三级咒灵甲的原状（位置 / 目标 / 是否无 AI），供演出结束后还原。
     * <p>
     * <b>不冻结 AI</b>——本轮改造后咒灵要主动围攻（阶段 4 由
     * {@link #arrangeCursesAroundWall} 搬到墙外环上并令其仇恨玩家，被芝诺墙挡住）。
     */
    private static void rememberCurses(ServerPlayer player, Session session)
    {
        ServerLevel level = player.serverLevel();
        AABB box = player.getBoundingBox().inflate(FREEZE_RADIUS);
        List<Grade3CurseSpiritAEntity> curses = level.getEntitiesOfClass(Grade3CurseSpiritAEntity.class, box);
        for (Grade3CurseSpiritAEntity curse : curses)
        {
            FrozenCurse frozen = new FrozenCurse(
                    curse.getUUID(),
                    curse.isNoAi(),
                    curse.getTarget() == null ? null : curse.getTarget().getUUID(),
                    curse.getDeltaMovement(),
                    curse.position());
            session.frozenCurses.add(frozen);
        }
        LOGGER.info("[Cutscene] Main 1.3 registered {} grade-3 curses.", session.frozenCurses.size());
    }

    /** 还原被登记的三级咒灵甲（演出结束 / abort）：复位位置、AI 状态与原目标。 */
    private static void unrestoreCurses(Session session)
    {
        if (session == null || session.frozenCurses.isEmpty() || session.level == null)
        {
            return;
        }
        for (FrozenCurse frozen : session.frozenCurses)
        {
            Entity entity = session.level.getEntity(frozen.entityId());
            if (entity instanceof Mob mob)
            {
                mob.setNoAi(frozen.noAi());
                mob.setTarget(null);
                if (frozen.targetId() != null)
                {
                    Entity target = session.level.getEntity(frozen.targetId());
                    if (target instanceof LivingEntity living)
                    {
                        mob.setTarget(living);
                    }
                }
                mob.setDeltaMovement(frozen.deltaMovement());
                if (frozen.originPos() != null)
                {
                    mob.teleportTo(frozen.originPos().x, frozen.originPos().y, frozen.originPos().z);
                }
            }
        }
        session.frozenCurses.clear();
        session.wallCenter = null;
    }

    /**
     * 阶段 4：把场上被登记的三级咒灵甲全部按角度均分，搬到「墙心」外侧的
     * {@link #CURSE_ARRANGE_RADIUS} 环上，令其仇恨玩家 —— 它们会从<b>四面八方</b>扑向倒地的玩家，
     * 却统统被以墙心为心、半径 {@link #ZENO_RANGE} 的芝诺墙（无下限，见 {@link #tickZenoWall}）挡在圈外。
     * <p>
     * <b>为什么是整圈而不是正前方</b>：墙心 = 五条悟与玩家的中点，墙把<b>两人一起</b>包住 ⇒ 无论咒灵
     * 从哪个方向来都会被拦。因此这里<b>不需要</b>按朝向挑参战者、不需要限制数量、也不需要冻结落选者：
     * 全都放进来、全都朝玩家扑，全都被墙挡住。
     * <p>
     * 演出结束随 {@link #unrestoreCurses} 一起复位（位置 / AI / 原目标）。
     *
     * @param center 墙心（五条悟与玩家的中点，水平面）
     */
    private static void arrangeCursesAroundWall(Session session, Vec3 center)
    {
        if (session.level == null || session.frozenCurses.isEmpty())
        {
            return;
        }
        List<FrozenCurse> alive = new ArrayList<>();
        for (FrozenCurse frozen : session.frozenCurses)
        {
            Entity entity = session.level.getEntity(frozen.entityId());
            if (entity instanceof Grade3CurseSpiritAEntity curse && curse.isAlive() && !curse.isRemoved())
            {
                alive.add(frozen);
            }
        }
        int count = alive.size();
        if (count == 0)
        {
            return;
        }
        // 从「五条悟在墙心的方向」起算，让咒灵不至于正好站在五条悟与玩家连线上（纯观感）。
        double baseAngle = Math.atan2(center.x - session.arrangeAnchorX, center.z - session.arrangeAnchorZ);
        for (int i = 0; i < count; i++)
        {
            FrozenCurse frozen = alive.get(i);
            Entity entity = session.level.getEntity(frozen.entityId());
            if (!(entity instanceof Grade3CurseSpiritAEntity curse))
            {
                continue;
            }
            double angle = baseAngle + (2.0D * Math.PI * i) / count;
            double dirX = -Math.sin(angle);
            double dirZ = Math.cos(angle);
            double x = center.x + dirX * CURSE_ARRANGE_RADIUS;
            double z = center.z + dirZ * CURSE_ARRANGE_RADIUS;
            curse.teleportTo(x, center.y, z);
            curse.setDeltaMovement(Vec3.ZERO);
            curse.setNoAi(false);
            curse.setTarget(nearestPlayerForCurse(session, curse, x, z));
            curse.getLookControl().setLookAt(center.x, center.y + 1.0D, center.z);
        }
        LOGGER.info("[Cutscene] Main 1.3 arranged {} curses on the zeno wall ring (center={},{},{}).",
                count, center.x, center.y, center.z);
    }

    /** 取该咒灵附近最近的在线玩家作为仇恨目标（无则留空，交给其自带 AI）。 */
    @Nullable
    private static Player nearestPlayerForCurse(Session session, Grade3CurseSpiritAEntity curse, double x, double z)
    {
        if (session.level == null)
        {
            return null;
        }
        Player best = null;
        double bestSqr = Double.MAX_VALUE;
        for (Player candidate : session.level.players())
        {
            if (!candidate.isAlive() || candidate.isSpectator())
            {
                continue;
            }
            double d = candidate.distanceToSqr(x, candidate.getY(), z);
            if (d < bestSqr)
            {
                bestSqr = d;
                best = candidate;
            }
        }
        return best;
    }

    /** 还原被登记的三级咒灵甲（演出结束 / abort）。 */
    private static void unfreezeCurses(Session session)
    {
        unrestoreCurses(session);
    }

    // ==================== 阶段 4-7：芝诺墙（无下限） ====================

    /**
     * 每 tick 硬钳位：圈内非白名单生物推到 {@link #ZENO_RANGE} 外沿，飞行物丢弃。
     * 白名单（本场玩家、五条悟演员 / 正主）不受影响。
     */
    private static void tickZenoWall(ServerPlayer player, Session session)
    {
        enforceZenoWall(player, session, false);
    }

    /** 墙展开时：圈内非白名单生物弹到外沿并给外向冲量，飞行物丢弃。 */
    private static void burstZenoWall(ServerPlayer player, Session session)
    {
        enforceZenoWall(player, session, true);
    }

    private static void enforceZenoWall(ServerPlayer player, Session session, boolean burst)
    {
        if (session.wallCenter == null || session.level == null)
        {
            return;
        }
        Vec3 center = session.wallCenter;
        AABB box = new AABB(center.x, center.y, center.z, center.x, center.y, center.z)
                .inflate(ZENO_RANGE + 2.0D, 8.0D, ZENO_RANGE + 2.0D);
        List<Entity> entities = session.level.getEntitiesOfClass(Entity.class, box);
        for (Entity entity : entities)
        {
            if (!entity.isAlive() || entity.isRemoved())
            {
                continue;
            }
            if (isZenoWhitelist(player, session, entity))
            {
                continue;
            }
            boolean projectile = entity instanceof Projectile;
            boolean living = entity instanceof LivingEntity;
            if (!projectile && !living)
            {
                continue;
            }
            double dx = entity.getX() - center.x;
            double dz = entity.getZ() - center.z;
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance >= ZENO_RANGE)
            {
                continue;
            }
            if (projectile)
            {
                entity.discard();
                continue;
            }
            double outX;
            double outZ;
            if (distance < 1.0E-6D)
            {
                double yawRad = Math.toRadians(player.getYRot());
                outX = -Math.sin(yawRad);
                outZ = Math.cos(yawRad);
            }
            else
            {
                outX = dx / distance;
                outZ = dz / distance;
            }
            entity.teleportTo(center.x + outX * ZENO_RANGE, entity.getY(), center.z + outZ * ZENO_RANGE);
            if (burst)
            {
                entity.setDeltaMovement(outX * ZENO_BURST, 0.2D, outZ * ZENO_BURST);
            }
            else
            {
                entity.setDeltaMovement(Vec3.ZERO);
            }
            entity.hasImpulse = true;
            entity.hurtMarked = true;
        }
    }

    /** 白名单：本场 1.3 玩家、五条悟（Geo 演员或 AI 正主）。 */
    private static boolean isZenoWhitelist(ServerPlayer player, Session session, Entity entity)
    {
        if (entity == player || player.getUUID().equals(entity.getUUID()))
        {
            return true;
        }
        if (entity instanceof GojoSatoruEntity)
        {
            return true;
        }
        if (session.gojoEntityId != null && session.gojoEntityId.equals(entity.getUUID()))
        {
            return true;
        }
        CutsceneActorEntity gojo = MainQuest13Actors.actor(player, MainQuest13Actors.ROLE_GOJO);
        return gojo != null && entity == gojo;
    }

    // ==================== 开场 COD 看向五条悟 ====================

    /**
     * 开场 COD 夹锥：钉站位并看向五条悟预定站位的头部（与 {@link #spawnGojo} 同公式）。
     * 半角沿用 1.1 {@link PlayerCgLock#DEFAULT_YAW_HALF} / {@link PlayerCgLock#DEFAULT_PITCH_HALF}。
     */
    private static void beginCgLookAtGojo(ServerPlayer player, Session session)
    {
        Vec3 stand = session.originPos != null ? session.originPos : player.position();
        PlayerCgLock.beginLookAt(
                player,
                stand,
                predictedGojoLookTarget(session, stand),
                PlayerCgLock.DEFAULT_YAW_HALF,
                PlayerCgLock.DEFAULT_PITCH_HALF);
    }

    /** 五条悟预定站位的头部坐标（尚未生成演员时也能对准）。 */
    private static Vec3 predictedGojoLookTarget(Session session, Vec3 stand)
    {
        float yaw = session.originYaw;
        double yawRad = Math.toRadians(yaw);
        double fwdX = -Math.sin(yawRad);
        double fwdZ = Math.cos(yawRad);
        double rightX = -fwdZ;
        double rightZ = fwdX;
        double gx = stand.x + fwdX * GOJO_FORWARD_DISTANCE + rightX * GOJO_SIDE_OFFSET;
        double gz = stand.z + fwdZ * GOJO_FORWARD_DISTANCE + rightZ * GOJO_SIDE_OFFSET;
        return new Vec3(gx, stand.y + GOJO_LOOK_HEAD_Y, gz);
    }

    // ==================== 阶段 4：五条悟登场 ====================

    /** 阶段 4：在玩家正前方生成五条悟（背对玩家，挡在身前）。 */
    private static void spawnGojo(ServerPlayer player, Session session)
    {
        Vec3 pos = session.originPos != null ? session.originPos : player.position();
        float yaw = session.originYaw;
        // 玩家水平前向（MC yaw：0=+Z，90=-X）
        double yawRad = Math.toRadians(yaw);
        double fwdX = -Math.sin(yawRad);
        double fwdZ = Math.cos(yawRad);
        double rightX = -fwdZ;
        double rightZ = fwdX;
        double gx = pos.x + fwdX * GOJO_FORWARD_DISTANCE + rightX * GOJO_SIDE_OFFSET;
        double gz = pos.z + fwdZ * GOJO_FORWARD_DISTANCE + rightZ * GOJO_SIDE_OFFSET;
        // 五条悟背对玩家（朝向前方）：朝向 = 玩家朝向，玩家看到的是他的背影。
        float faceYaw = yaw + GOJO_FACE_PLAYER;

        CutsceneActorEntity gojo = MainQuest13Actors.spawnGojo(player, gx, pos.y, gz, faceYaw);
        if (gojo == null)
        {
            LOGGER.warn("[Cutscene] Main 1.3: Gojo actor spawn failed; aborting performance.");
            session.running = false;
            abort(player);
            return;
        }
        session.running = true;
        session.gojoFeetY = gojo.getY();
        session.gojoYaw = faceYaw;
        // 阶段 3 细致：stand_up_and_protect 起播 3 秒后，转头看一眼玩家再复原（见 tickGojoActorLookBack）。
        session.gojoActorSpawnTick = session.ticks;
        session.gojoLookBackPending = true;
        // 芝诺墙心 = 五条悟与玩家连线的中点 ⇒ 墙把两人一起包住、全向阻挡。
        session.arrangeAnchorX = gx;
        session.arrangeAnchorZ = gz;
        session.wallCenter = new Vec3((gx + pos.x) / 2.0D, pos.y, (gz + pos.z) / 2.0D);
        // 三级咒灵甲搬到墙外环上、环绕整圈、仇恨玩家（被芝诺墙挡在圈外）。
        arrangeCursesAroundWall(session, session.wallCenter);
        burstZenoWall(player, session);
    }

    // ==================== 阶段 7：真实领域展开 ====================

    /**
     * 阶段 7：五条悟展开<b>真正的常规无量空处</b>。
     * <p>
     * 关键点：Geo 演员（{@link CutsceneActorEntity}）无 AI、不能施法。这里在演员所在的同一位置与朝向
     * <b>临时代替</b>为 {@link GojoSatoruEntity}（有 AI、带特级面板、能开真实领域），由它经
     * {@link AiDomainCasterRegistry#castLimitlessDomain} 走完整真实演出（蓄力完成后球壳自背后向前包，
     * 白幕提层、lightline、搬点朝南、必中、280 刻后天空盒切换 + 水墨 + 全屏闪光 + 环境音）。
     * <p>
     * 交接对齐位置 / 朝向。正主贴图 {@code gojosatoru_teacher_noeyemask} + Epic Fight {@code Meshes.BIPED}，
     * 蓄力手势走领域自己的 {@code epicfight_animation_wuliang}。
     *
     * @return 是否成功进入真实领域展开
     */
    private static boolean buildDomain(ServerPlayer player, Session session)
    {
        if (session.level == null)
        {
            return false;
        }
        CutsceneActorEntity actor = MainQuest13Actors.actor(player, MainQuest13Actors.ROLE_GOJO);
        Vec3 anchor = actor != null ? actor.position() : session.originPos;
        if (anchor == null)
        {
            return false;
        }
        float yaw = actor != null ? actor.getYRot() : session.gojoYaw;

        // 演员退场，实体接管（同位置 / 同朝向 / 同外观资源）。
        if (actor != null)
        {
            actor.discard();
        }

        GojoSatoruEntity gojo = GojoSatoruEntity.spawnAt(session.level, anchor, yaw);
        if (gojo == null)
        {
            LOGGER.warn("[Cutscene] Main 1.3: Gojo AI entity spawn failed.");
            return false;
        }
        TechniqueAtkObjShackle.replace(gojo, TechniqueAtkObjShackle.MODE_BL, Set.of(player.getUUID()));
        gojo.playDomainGesture();
        gojo.setDeltaMovement(Vec3.ZERO);
        session.gojoEntityId = gojo.getUUID();

        // 实体一生成就锁死朝向 + 移动 2 秒：朝向 = Geo 演员消失时的朝向（yaw），
        // 压住蓄力 / 结印期的乱动乱转；到点由 tickGojoSwapLock 自动放手。
        session.gojoLockPos = anchor;
        session.gojoLockYaw = yaw;
        session.gojoLockPitch = gojo.getXRot();
        session.gojoSwapLockStart = session.ticks;
        session.gojoSwapLock = true;

        boolean cast = gojo.castLimitlessDomain();
        if (!cast)
        {
            LOGGER.warn("[Cutscene] Main 1.3: AI Gojo failed to cast real Unlimited Void.");
            return false;
        }
        return true;
    }

    private enum Phase
    {
        /** 阶段 2：黑屏保持，铺陈昏迷节奏。 */
        BLACKOUT_HOLD,
        /** 撤红黑后：眨眼醒来（4s），再进背影对白。 */
        WAKE_BLINK,
        /** 阶段 3：五条悟登场 + 背影镜头。 */
        GOJO_ENTER,
        /** 阶段 4 前半：正脸特写 + 摘眼罩。 */
        FACE_CLOSEUP,
        /** 兜底开域（正常路径正脸结束已进 DOMAIN）。 */
        SWAP_MODEL,
        /** 阶段 5：真实领域展开（已展开，停驻等待后续阶段；⛔ 此处不做任何收尾）。 */
        DOMAIN
    }

    private static final class Session
    {
        Phase phase = Phase.BLACKOUT_HOLD;
        int ticks;
        int phaseStartTick;
        /** 布景失败时置 false，阻止继续推进。 */
        boolean running = true;
        boolean blindfoldStarted;
        @Nullable Vec3 originPos;
        float originYaw;
        double gojoFeetY;
        float gojoYaw;
        /** 阶段 3：Geo 演员登场（{@code stand_up_and_protect} 起播）的时刻。 */
        int gojoActorSpawnTick;
        /** 阶段 3：{@code stand_up_and_protect_and_look_back} 段是否尚未切换（一次性）。 */
        boolean gojoLookBackPending;
        /** 阶段 4 起芝诺墙的墙心 = 五条悟与玩家的中点，把两人一起包住。 */
        @Nullable Vec3 wallCenter;
        /** 咒灵环布置的起算参考点（五条悟水平坐标），用于让咒灵避开「五条悟—玩家」连线。 */
        double arrangeAnchorX;
        double arrangeAnchorZ;
        final List<FrozenCurse> frozenCurses = new ArrayList<>();
        /** 还原咒灵 AI 用。 */
        @Nullable ServerLevel level;
        /** 阶段 7 登场的 AI 五条悟实体 UUID（领域施法者）。 */
        @Nullable UUID gojoEntityId;
        /** 五条悟实体「登场锁」是否激活（生成后 {@link #GOJO_SWAP_LOCK_TICKS} 刻内为 true）。 */
        boolean gojoSwapLock;
        /** 登场锁起算 tick（= 实体生成时的 {@code ticks}）。 */
        int gojoSwapLockStart;
        /** 登场锁锚点（生成位置）。 */
        @Nullable Vec3 gojoLockPos;
        /** 登场锁朝向 = Geo 演员消失时的朝向。 */
        float gojoLockYaw;
        /** 登场锁俯仰（生成时实体自身的 XRot）。 */
        float gojoLockPitch;
        /** 阶段 7 领域成型小灶是否已执行（一次性，防重复清场）。 */
        boolean domainPurgeDone;
        /** 开演前是否已在 Epic Fight 战斗模式，中止时还原。 */
        boolean wasEpicFightMode;
    }

    private record FrozenCurse(
            UUID entityId,
            boolean noAi,
            @Nullable UUID targetId,
            Vec3 deltaMovement,
            @Nullable Vec3 originPos)
    {
    }
}
