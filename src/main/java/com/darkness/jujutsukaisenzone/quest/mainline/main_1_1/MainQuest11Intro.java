package com.darkness.jujutsukaisenzone.quest.mainline.main_1_1;

import com.darkness.jujutsukaisenzone.cg.PlayerCgLock;
import com.darkness.jujutsukaisenzone.locate.PlayerLocateGuidance;
import com.darkness.jujutsukaisenzone.network.ModNetwork;
import com.darkness.jujutsukaisenzone.network.NetworkSenders;
import com.darkness.jujutsukaisenzone.music.ModMusic;
import com.darkness.jujutsukaisenzone.music.MusicPriority;
import com.darkness.jujutsukaisenzone.network.packet.MainQuest11CabinCmdCamS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncQuestBlackoutS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncQuestLetterboxS2CPacket;
import com.darkness.jujutsukaisenzone.network.packet.SyncQuestMissionBriefingS2CPacket;
import com.darkness.jujutsukaisenzone.player.PlayerQuestData;
import com.darkness.jujutsukaisenzone.quest.mainline.MainQuestDefs;
import com.darkness.jujutsukaisenzone.quest.mainline.MainQuestLogic;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;

/**
 * 主线 1.1 第一次追踪顺序：
 * <ol>
 *   <li>全黑开场旁白（{@link com.darkness.jujutsukaisenzone.client.QuestBlackoutOverlay}）放小屋、传送、发装；黑屏期布好三组 cmdcam</li>
 *   <li>揭幕后逐组播放：小屋巡镜 → 小石子焦点 → KiRo 焦点</li>
 *   <li>电影黑幕对话 + Geo 角色轨（无新运镜）</li>
 * </ol>
 */
public final class MainQuest11Intro
{
    public static final int LINE_TICKS = 45;
    public static final int GAP_TICKS = 5;
    /** 开场全黑居中旁白。 */
    public static final String[] BLACKOUT_KEYS = {
            "quest.jujutsukaisenzone.main_1_1.blackout.1",
            "quest.jujutsukaisenzone.main_1_1.blackout.2",
            "quest.jujutsukaisenzone.main_1_1.blackout.3"
    };

    private static final int BLACKOUT_TICKS =
            BLACKOUT_KEYS.length * LINE_TICKS + (BLACKOUT_KEYS.length - 1) * GAP_TICKS;
    /** 组1 6s + 组2/3 各 3s（组间 load 吃进上一组时长，不另加空档）。 */
    private static final int CAM_LOCK_TICKS = 120 + 60 + 60;
    private static final String CAM_START_MUSIC_ID = "leisurely_and_carefree";
    /** 第一组镜头起播时的剧情曲音量（相对音乐滑条）。 */
    private static final float CAM_START_MUSIC_VOLUME = 0.3F;
    private static final int CAM_START_MUSIC_DURATION_MS = 90_000;
    /** 揭幕起播淡入；400ms 听不出。 */
    private static final int CAM_START_MUSIC_FADE_IN_MS = 2_000;
    /** 一分半到点后的淡出。 */
    private static final int CAM_START_MUSIC_FADE_OUT_MS = 2_000;
    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> CAM_UNLOCK_TICKS = new ConcurrentHashMap<>();

    private MainQuest11Intro()
    {
    }

    public static boolean isRunning(ServerPlayer player)
    {
        UUID id = player.getUUID();
        return SESSIONS.containsKey(id) || CAM_UNLOCK_TICKS.containsKey(id) || MainQuest11CabinTalk.isRunning(player);
    }

    public static void startIfNeeded(ServerPlayer player)
    {
        if (isRunning(player) || PlayerQuestData.isMain11TalkDone(player))
        {
            return;
        }
        // 开场未完：清掉误开的村庄引导
        PlayerLocateGuidance.clearSilent(player);
        // 黑屏已过、对话未完（含中途下线）：只补对话
        if (PlayerQuestData.isMain11IntroDone(player))
        {
            MainQuest11CabinTalk.start(player);
            return;
        }
        Session session = new Session();
        session.shownCaption = 0;
        SESSIONS.put(player.getUUID(), session);
        player.closeContainer();
        ModNetwork.sendPlayerInputLockSync(player, true);
        showBlackoutLine(player, 0);
        holdInPlace(player);
    }

    public static void abort(ServerPlayer player)
    {
        UUID id = player.getUUID();
        boolean intro = SESSIONS.remove(id) != null;
        boolean cam = CAM_UNLOCK_TICKS.remove(id) != null;
        boolean talk = MainQuest11CabinTalk.isRunning(player);
        MainQuest11CabinTalk.abort(player);
        PlayerCgLock.end(player);
        if (!intro && !cam && !talk)
        {
            return;
        }
        NetworkSenders.toPlayer(player, SyncQuestBlackoutS2CPacket.clear());
        NetworkSenders.toPlayer(player, SyncQuestLetterboxS2CPacket.clear());
        NetworkSenders.toPlayer(player, SyncQuestMissionBriefingS2CPacket.clear());
        ModMusic.stopIf(player, MusicPriority.STORY, 0);
        ModNetwork.sendPlayerInputLockSync(player, false);
    }

    public static void tick(ServerPlayer player)
    {
        Session session = SESSIONS.get(player.getUUID());
        if (session != null)
        {
            holdInPlace(player);
            if (!session.worldReady)
            {
                prepareWorld(player, session);
            }
            if (session.worldReady && !session.camSetupSent)
            {
                sendCabinCamSetup(player, session);
                session.camSetupSent = true;
            }

            int captionIndex = captionIndexForTick(session.ticks, BLACKOUT_KEYS.length);
            if (captionIndex != session.shownCaption)
            {
                session.shownCaption = captionIndex;
                if (captionIndex >= 0)
                {
                    showBlackoutLine(player, captionIndex);
                }
                else
                {
                    NetworkSenders.toPlayer(player, new SyncQuestBlackoutS2CPacket(true, ""));
                }
            }

            session.ticks++;
            if (session.ticks >= BLACKOUT_TICKS && session.worldReady)
            {
                finishBlackout(player, session);
            }
            return;
        }

        Integer remaining = CAM_UNLOCK_TICKS.get(player.getUUID());
        if (remaining != null)
        {
            PlayerCgLock.tick(player);
            if (remaining <= 1)
            {
                CAM_UNLOCK_TICKS.remove(player.getUUID());
                MainQuest11CabinTalk.start(player);
                return;
            }
            CAM_UNLOCK_TICKS.put(player.getUUID(), remaining - 1);
            return;
        }

        if (MainQuest11CabinTalk.isRunning(player))
        {
            MainQuest11CabinTalk.tick(player);
            if (!MainQuest11CabinTalk.isRunning(player))
            {
                PlayerCgLock.end(player);
                PlayerQuestData.setMain11TalkDone(player, true);
                MainQuestLogic.ensureLocateForTracked(player, true);
            }
        }
    }

    private static void showBlackoutLine(ServerPlayer player, int index)
    {
        NetworkSenders.toPlayer(player, new SyncQuestBlackoutS2CPacket(true, BLACKOUT_KEYS[index]));
    }

    private static int captionIndexForTick(int ticks, int lineCount)
    {
        int cycle = LINE_TICKS + GAP_TICKS;
        int index = ticks / cycle;
        int into = ticks % cycle;
        if (index >= lineCount)
        {
            return lineCount - 1;
        }
        if (into >= LINE_TICKS)
        {
            return -1;
        }
        return index;
    }

    private static void prepareWorld(ServerPlayer player, Session session)
    {
        ServerLevel level = player.serverLevel();
        BlockPos stored = PlayerQuestData.getMain11CabinPos(player);
        if (stored != null)
        {
            session.teleportPos = stored;
            session.lookCenter = Vec3.atCenterOf(stored);
        }
        else
        {
            SurvivalCabinPlacer.PlaceResult placed = SurvivalCabinPlacer.placeNear(level, player.blockPosition());
            if (placed != null)
            {
                PlayerQuestData.setMain11CabinPos(player, placed.interior());
                session.teleportPos = placed.interior();
                session.lookCenter = placed.lookCenter();
                session.cabinBounds = placed.bounds();
            }
        }
        session.worldReady = true;
        if (session.teleportPos == null)
        {
            session.teleportPos = player.blockPosition();
        }
    }

    private static void finishBlackout(ServerPlayer player, Session session)
    {
        SESSIONS.remove(player.getUUID());
        BlockPos dest = session.teleportPos != null ? session.teleportPos : player.blockPosition();
        player.teleportTo(dest.getX() + 0.5D, dest.getY(), dest.getZ() + 0.5D);
        grantStarterKitIfNeeded(player, true);
        PlayerQuestData.setMain11IntroDone(player, true);
        player.inventoryMenu.broadcastChanges();
        // 第一组巡镜起播前：小屋演员已进世界；同时播 COD 风格任务简报
        MainQuest11CabinActors.spawnForCamStart(player, session.cabinBounds, session.teleportPos);
        NetworkSenders.toPlayer(player, MainQuest11CabinCmdCamS2CPacket.startPlayback());
        ModMusic.play(
                player,
                CAM_START_MUSIC_ID,
                MusicPriority.STORY,
                false,
                CAM_START_MUSIC_FADE_IN_MS,
                CAM_START_MUSIC_VOLUME,
                CAM_START_MUSIC_DURATION_MS,
                CAM_START_MUSIC_FADE_OUT_MS);
        NetworkSenders.toPlayer(player, SyncQuestBlackoutS2CPacket.clear());
        MainQuestLogic.sendMissionBriefing(player, MainQuestDefs.MAIN_1_1);
        PlayerCgLock.pinOnly(player, player.position());
        CAM_UNLOCK_TICKS.put(player.getUUID(), CAM_LOCK_TICKS);
    }

    private static void sendCabinCamSetup(ServerPlayer player, Session session)
    {
        BlockPos dest = session.teleportPos != null ? session.teleportPos : player.blockPosition();
        Vec3 playerPos = new Vec3(dest.getX() + 0.5D, dest.getY(), dest.getZ() + 0.5D);
        Vec3 lookCenter = session.lookCenter != null
                ? session.lookCenter
                : new Vec3(playerPos.x, playerPos.y + 1.0D, playerPos.z);
        ServerLevel level = player.serverLevel();
        Vec3 xiaoshiziLook = MainQuest11CabinActors.xiaoshiziLookPos(level, session.cabinBounds, session.teleportPos);
        Vec3 kiroLook = MainQuest11CabinActors.kiroLookPos(level, session.cabinBounds, session.teleportPos);
        if (xiaoshiziLook == null)
        {
            xiaoshiziLook = lookCenter;
        }
        if (kiroLook == null)
        {
            kiroLook = lookCenter;
        }
        NetworkSenders.toPlayer(player, MainQuest11CabinCmdCamS2CPacket.setup(
                playerPos, player.getYRot(), lookCenter, xiaoshiziLook, kiroLook));
    }

    private static void holdInPlace(ServerPlayer player)
    {
        player.setDeltaMovement(Vec3.ZERO);
        player.hurtMarked = true;
        player.connection.teleport(
                player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot());
    }

    public static void grantStarterKitIfNeeded(ServerPlayer player)
    {
        grantStarterKitIfNeeded(player, false);
    }

    public static void grantStarterKitIfNeeded(ServerPlayer player, boolean force)
    {
        if (hasAnyDiamondArmor(player))
        {
            PlayerQuestData.setMain11Kitted(player, true);
            return;
        }
        if (!force && !PlayerQuestData.isMain11Kitted(player) && !PlayerQuestData.isMain11IntroDone(player))
        {
            return;
        }
        giveStarterKit(player);
        PlayerQuestData.setMain11Kitted(player, true);
        player.inventoryMenu.broadcastChanges();
    }

    private static boolean hasAnyDiamondArmor(ServerPlayer player)
    {
        if (isDiamondArmor(player.getItemBySlot(EquipmentSlot.HEAD))
                || isDiamondArmor(player.getItemBySlot(EquipmentSlot.CHEST))
                || isDiamondArmor(player.getItemBySlot(EquipmentSlot.LEGS))
                || isDiamondArmor(player.getItemBySlot(EquipmentSlot.FEET)))
        {
            return true;
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++)
        {
            if (isDiamondArmor(player.getInventory().getItem(i)))
            {
                return true;
            }
        }
        return false;
    }

    private static boolean isDiamondArmor(ItemStack stack)
    {
        return !stack.isEmpty()
                && (stack.is(Items.DIAMOND_HELMET)
                || stack.is(Items.DIAMOND_CHESTPLATE)
                || stack.is(Items.DIAMOND_LEGGINGS)
                || stack.is(Items.DIAMOND_BOOTS));
    }
    private static void giveStarterKit(ServerPlayer player)
    {
        equip(player, EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
        equip(player, EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
        equip(player, EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
        equip(player, EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
        give(player, new ItemStack(Items.DIAMOND_SWORD));
        give(player, new ItemStack(Items.BOW));
        give(player, new ItemStack(Items.ARROW, 64));
        give(player, new ItemStack(Items.COOKED_BEEF, 16));
        give(player, new ItemStack(Items.WATER_BUCKET));
        give(player, new ItemStack(Items.TORCH, 32));
        give(player, new ItemStack(Items.EMERALD, 16));
    }

    private static void equip(ServerPlayer player, EquipmentSlot slot, ItemStack stack)
    {
        ItemStack old = player.getItemBySlot(slot);
        player.setItemSlot(slot, stack);
        if (!old.isEmpty())
        {
            give(player, old);
        }
    }

    private static void give(ServerPlayer player, ItemStack stack)
    {
        player.getInventory().placeItemBackInInventory(stack);
    }

    private static final class Session
    {
        int ticks;
        int shownCaption = -1;
        boolean worldReady;
        boolean camSetupSent;
        @Nullable BlockPos teleportPos;
        @Nullable Vec3 lookCenter;
        @Nullable BoundingBox cabinBounds;
    }
}
