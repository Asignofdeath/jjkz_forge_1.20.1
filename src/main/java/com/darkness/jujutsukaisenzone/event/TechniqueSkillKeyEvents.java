package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.client.ClientSkillBarCache;
import com.darkness.jujutsukaisenzone.client.MaxOutputChargeHud;
import com.darkness.jujutsukaisenzone.compat.EpicFightCompat;
import com.darkness.jujutsukaisenzone.network.ModNetwork;
import com.darkness.jujutsukaisenzone.network.packet.TechniqueHoldReleaseC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.TechniqueMaxOutputC2SPacket;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import com.darkness.jujutsukaisenzone.technique.skillset.IInnateTechniqueSkillSet;
import com.darkness.jujutsukaisenzone.technique.skillset.LimitlessTechniqueSkillSet;
import com.darkness.jujutsukaisenzone.technique.skillset.SpecialMoveInput;
import com.darkness.jujutsukaisenzone.technique.skillset.TechniqueSkill;
import com.darkness.jujutsukaisenzone.technique.skillset.TechniqueSkillSetRegistry;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.Arrays;
import java.util.Optional;

/**
 * 战斗模式下术式招式槽输入：9 个槽位分 3 组，每组 3 个。
 * <p>
 * Tab+1/2/3 切换当前组；无 Tab 时 1/2/3 对应当前组内槽位（映射为服务端槽位 1~9）。
 * <p>
 * 无修饰键：支持最大输出的槽位短按松手为普通释放；按住超过阈值后进入蓄力（显示蓄力条），
 * 蓄满松手为最大输出，蓄力期间未满松手则取消。不支持最大输出的槽位按下即释放。
 * <p>
 * 调节模式由 {@link #KEY_ADJUST_MODE}（默认鼠标中键）循环切换，档位由 {@link #KEY_ADJUST_TIER_DOWN}
 * （默认鼠标侧键 4）/ {@link #KEY_ADJUST_TIER_UP}（默认鼠标侧键 5）按 ±1 调节，三者均可在控制设置中改键：
 * - 施法距离模式：调节施法距离档位（-25% / +25%）。
 * - 结界半径模式：调节结界半径档位（8 / 16 / 32 / 64）。
 * - 星星标记模式：切换「星星标记」当前标记（仅生得术式为星间飞行时可用）。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class TechniqueSkillKeyEvents
{
    /** 循环切换调节模式（施法距离 / 结界半径 / 星星标记）。默认中键（GLFW 的 MOUSE_BUTTON_3 即中键）。 */
    public static final KeyMapping KEY_ADJUST_MODE = new KeyMapping(
            "key.jujutsukaisenzone.adjust_mode",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.MOUSE,
            GLFW.GLFW_MOUSE_BUTTON_3,
            "key.categories.jujutsukaisenzone"
    );

    /** 当前调节模式的档位 −1（星星标记模式下为上一个标记）。 */
    public static final KeyMapping KEY_ADJUST_TIER_DOWN = new KeyMapping(
            "key.jujutsukaisenzone.adjust_tier_down",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.MOUSE,
            GLFW.GLFW_MOUSE_BUTTON_4,
            "key.categories.jujutsukaisenzone"
    );

    /** 当前调节模式的档位 +1（星星标记模式下为下一个标记）。 */
    public static final KeyMapping KEY_ADJUST_TIER_UP = new KeyMapping(
            "key.jujutsukaisenzone.adjust_tier_up",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.MOUSE,
            GLFW.GLFW_MOUSE_BUTTON_5,
            "key.categories.jujutsukaisenzone"
    );

    /** 每组槽位数，共 {@link #SLOT_GROUP_COUNT} 组。 */
    private static final int SLOTS_PER_GROUP = 3;
    private static final int SLOT_GROUP_COUNT = 3;

    /** 当前组索引 0~2，对应 Tab+1 / Tab+2 / Tab+3。 */
    private static int currentSkillSlotGroup;

    private static final int[] HOTKEY_KEYS = {
            GLFW.GLFW_KEY_1,
            GLFW.GLFW_KEY_2,
            GLFW.GLFW_KEY_3
    };

    private static final boolean[] LAST_HOTKEY_STATES = new boolean[HOTKEY_KEYS.length];
    /** Tab+1/2/3 切组时，避免按住数字键连发切组。 */
    private static final boolean[] LAST_GROUP_SELECT_KEY_STATES = new boolean[HOTKEY_KEYS.length];

    /** 按下时 {@link net.minecraft.world.entity.Entity#tickCount}，-1 表示未跟踪。 */
    private static final int[] SLOT_HOLD_ORIGIN_TICK = new int[HOTKEY_KEYS.length];
    /** 进入蓄力态时的 tick（-1 表示尚未进入蓄力，仍为短按判定区间）。 */
    private static final int[] SLOT_CHARGE_BEGIN_TICK = new int[HOTKEY_KEYS.length];
    /** 该槽蓄力所需 tick（进入蓄力态后计）。 */
    private static final int[] SLOT_CHARGE_REQUIRED_TICKS = new int[HOTKEY_KEYS.length];
    /** 按住超过该刻数才进入蓄力（约 0.6 秒），此前松手视为普通释放。 */
    private static final int MAX_OUTPUT_HOLD_THRESHOLD_TICKS = 12;
    /** 该槽是否为长按蓄力、松手释放类技能（如「灶」开）。 */
    private static final boolean[] SLOT_HOLD_RELEASE_MODE = new boolean[HOTKEY_KEYS.length];
    /** 长按蓄力已满并由服务端自动完成（如超远程虚式茈），松手时不再发送 release/cancel。 */
    private static final boolean[] SLOT_HOLD_RELEASE_COMPLETED = new boolean[HOTKEY_KEYS.length];

    private static boolean lastMouseButton4Down;
    private static boolean lastMouseButton5Down;
    private static boolean lastMouseMiddleDown;

    /** 调节模式：施法距离档位。所有玩家可用。 */
    private static final int ADJUST_MODE_CAST_RANGE = 0;
    /** 调节模式：结界半径档位。所有玩家可用。 */
    private static final int ADJUST_MODE_BARRIER_RADIUS = 1;
    /** 调节模式：切换「星星标记」当前标记。仅生得术式为星间飞行时可用。 */
    private static final int ADJUST_MODE_STAR_MARK = 2;
    /** 通用可用模式数（施法距离 + 结界半径）。 */
    private static final int ADJUST_MODE_COUNT_BASE = 2;
    /** 星间飞行额外提供「星星标记」模式。 */
    private static final int ADJUST_MODE_COUNT_LOVE_RENDEZVOUS = ADJUST_MODE_STAR_MARK + 1;

    /** 当前调节模式，取值见 {@code ADJUST_MODE_*} 常量。 */
    private static int currentAdjustMode = ADJUST_MODE_CAST_RANGE;

    /** 打断后须先松键再蓄力，避免按住期间立刻重新进入蓄力。 */
    private static final boolean[] SLOT_IGNORE_HOLD_UNTIL_RELEASE = new boolean[HOTKEY_KEYS.length];

    static
    {
        Arrays.fill(SLOT_HOLD_ORIGIN_TICK, -1);
        Arrays.fill(SLOT_CHARGE_BEGIN_TICK, -1);
    }

    private TechniqueSkillKeyEvents()
    {
    }

    /** 当前组 0~2（对外展示为第 1~3 组）。 */
    public static int getCurrentSkillSlotGroup()
    {
        return currentSkillSlotGroup;
    }

    /** 热键索引 0~2 映射到服务端槽位 1~9。 */
    public static int hotkeyIndexToServerSlot(int hotkeyIndex)
    {
        return currentSkillSlotGroup * SLOTS_PER_GROUP + hotkeyIndex + 1;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END)
        {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null
                || minecraft.screen != null
                || !EpicFightCompat.isBattleMode(minecraft.player))
        {
            resetChargeInputState();
            lastMouseButton4Down = false;
            lastMouseButton5Down = false;
            lastMouseMiddleDown = false;
            return;
        }

        /* 调节模式的按键调整只需战斗模式，无需生得术式；按键可在控制设置中改绑 */
        long window = minecraft.getWindow().getWindow();
        boolean mouse4Down = KEY_ADJUST_TIER_DOWN.isDown();
        boolean mouse5Down = KEY_ADJUST_TIER_UP.isDown();
        boolean mouseMiddleDown = KEY_ADJUST_MODE.isDown();

        /* 「星星标记」模式只服务于星间飞行；术式变更后自动退回施法距离模式 */
        int adjustModeCount = resolveAdjustModeCount(minecraft.player);
        if (currentAdjustMode >= adjustModeCount)
        {
            currentAdjustMode = ADJUST_MODE_CAST_RANGE;
        }

        if (mouseMiddleDown && !lastMouseMiddleDown)
        {
            currentAdjustMode = (currentAdjustMode + 1) % adjustModeCount;
            minecraft.player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal(
                            "切换至：" + adjustModeName(currentAdjustMode) + " 调节模式"), true);
        }
        lastMouseMiddleDown = mouseMiddleDown;

        if (mouse4Down && !lastMouseButton4Down)
        {
            applyAdjustModeDelta(currentAdjustMode, -1);
        }

        if (mouse5Down && !lastMouseButton5Down)
        {
            applyAdjustModeDelta(currentAdjustMode, 1);
        }

        lastMouseButton4Down = mouse4Down;
        lastMouseButton5Down = mouse5Down;

        /* 快速双击 Shift：清除星星标记（单独清除 / 全部清除由服务端按准星自行判定） */
        handleStarMarkDoubleTapClear(minecraft, window);

        Optional<IInnateTechniqueSkillSet> setOpt = TechniqueSkillSetRegistry.get(
                PlayerTechniqueData.getInnateTechnique(minecraft.player));
        boolean acceptHotbar = setOpt.map(set -> set.acceptsClientHotbarInput(minecraft.player)).orElse(false);
        boolean acceptSpecial = setOpt.map(set -> set.acceptsClientSpecialMoveInput(minecraft.player)).orElse(false);
        if (!acceptHotbar && !acceptSpecial)
        {
            resetChargeInputState();
            return;
        }

        boolean tabDown = glfwKeyPressed(window, GLFW.GLFW_KEY_TAB);
        handleSkillSlotGroupSwitch(minecraft, window, tabDown);

        int modifierMask = buildModifierMask(window);
        int playerTick = minecraft.player.tickCount;

        if (modifierMask != 0 || tabDown)
        {
            for (int i = 0; i < HOTKEY_KEYS.length; i++)
            {
                if (SLOT_HOLD_ORIGIN_TICK[i] >= 0)
                {
                    abortSlotHold(i, true);
                }
            }
        }

        for (int i = 0; i < HOTKEY_KEYS.length; i++)
        {
            boolean isDown = GLFW.glfwGetKey(window, HOTKEY_KEYS[i]) == GLFW.GLFW_PRESS;
            boolean wasDown = LAST_HOTKEY_STATES[i];
            int slot = hotkeyIndexToServerSlot(i);

            if (!isDown)
            {
                SLOT_IGNORE_HOLD_UNTIL_RELEASE[i] = false;
            }

            if (tabDown)
            {
                LAST_HOTKEY_STATES[i] = isDown;
                continue;
            }

            if (isDown && !wasDown)
            {
                /* 任何技能键都会打断「双击 Shift」的序列，避免 Shift+技能键 被误判成双击 */
                lastShiftPressTick = -1;
            }

            if (modifierMask != 0 && acceptSpecial && isDown && !wasDown)
            {
                ModNetwork.sendTechniqueSpecialMoveToServer(modifierMask, slot);
            }
            else if (modifierMask == 0 && acceptHotbar && !SLOT_IGNORE_HOLD_UNTIL_RELEASE[i])
            {
                // 通过客户端缓存查快捷栏位映射的 skillId，再按 skillId 判断是否支持最大输出。
                // 玩家可在 SkillBarConfigScreen 中任意配置各栏位对应的技能。
                String techniqueId = PlayerTechniqueData.getInnateTechnique(minecraft.player);
                String skillId = resolveConfiguredSkillId(techniqueId, slot, setOpt);
                int holdReleaseChargeTicks = skillId.isEmpty()
                        ? 0
                        : setOpt.map(s -> s.getHoldReleaseChargeTicksBySkill(minecraft.player, skillId)).orElse(0);
                boolean requiresHoldRelease = holdReleaseChargeTicks > 0;
                int maxOutputChargeTicks = resolveMaxOutputChargeTicks(minecraft.player, setOpt, skillId);
                boolean supportsMaxOutput = !requiresHoldRelease && maxOutputChargeTicks > 0;

                if (!wasDown && isDown)
                {
                    if (requiresHoldRelease)
                    {
                        SLOT_HOLD_ORIGIN_TICK[i] = playerTick;
                        SLOT_CHARGE_BEGIN_TICK[i] = playerTick;
                        SLOT_CHARGE_REQUIRED_TICKS[i] = holdReleaseChargeTicks;
                        SLOT_HOLD_RELEASE_MODE[i] = true;
                        ModNetwork.sendTechniqueHoldReleaseToServer(TechniqueHoldReleaseC2SPacket.ACTION_BEGIN, slot);
                        MaxOutputChargeHud.begin(slot, holdReleaseChargeTicks, resolveSkillChargeLabel(setOpt, skillId), playerTick);
                    }
                    else if (supportsMaxOutput)
                    {
                        SLOT_HOLD_ORIGIN_TICK[i] = playerTick;
                        SLOT_CHARGE_BEGIN_TICK[i] = -1;
                        SLOT_CHARGE_REQUIRED_TICKS[i] = maxOutputChargeTicks;
                        SLOT_HOLD_RELEASE_MODE[i] = false;
                    }
                    else
                    {
                        ModNetwork.sendTechniqueSkillUseToServer(slot);
                    }
                }

                if (isDown && SLOT_HOLD_ORIGIN_TICK[i] >= 0 && SLOT_CHARGE_BEGIN_TICK[i] < 0 && supportsMaxOutput)
                {
                    int heldTicks = playerTick - SLOT_HOLD_ORIGIN_TICK[i];
                    if (heldTicks >= MAX_OUTPUT_HOLD_THRESHOLD_TICKS)
                    {
                        enterSlotCharge(i, slot, playerTick);
                    }
                }

                if (isDown && SLOT_HOLD_RELEASE_MODE[i] && SLOT_HOLD_ORIGIN_TICK[i] >= 0
                        && !SLOT_HOLD_RELEASE_COMPLETED[i]
                        && holdReleaseAutoCompletesWithoutKeyRelease(skillId))
                {
                    int heldTicks = playerTick - SLOT_HOLD_ORIGIN_TICK[i];
                    if (heldTicks >= SLOT_CHARGE_REQUIRED_TICKS[i])
                    {
                        SLOT_HOLD_RELEASE_COMPLETED[i] = true;
                        MaxOutputChargeHud.end();
                    }
                }

                if (wasDown && !isDown && SLOT_HOLD_ORIGIN_TICK[i] >= 0)
                {
                    if (SLOT_HOLD_RELEASE_MODE[i])
                    {
                        if (!SLOT_HOLD_RELEASE_COMPLETED[i])
                        {
                            int heldTicks = playerTick - SLOT_HOLD_ORIGIN_TICK[i];
                            if (heldTicks >= SLOT_CHARGE_REQUIRED_TICKS[i])
                            {
                                ModNetwork.sendTechniqueHoldReleaseToServer(TechniqueHoldReleaseC2SPacket.ACTION_RELEASE, slot);
                            }
                            else
                            {
                                ModNetwork.sendTechniqueHoldReleaseToServer(TechniqueHoldReleaseC2SPacket.ACTION_CANCEL, slot);
                            }
                        }
                        MaxOutputChargeHud.end();
                    }
                    else if (SLOT_CHARGE_BEGIN_TICK[i] < 0)
                    {
                        ModNetwork.sendTechniqueSkillUseToServer(slot);
                        MaxOutputChargeHud.end();
                    }
                    else
                    {
                        int chargeHeldTicks = playerTick - SLOT_CHARGE_BEGIN_TICK[i];
                        if (chargeHeldTicks >= SLOT_CHARGE_REQUIRED_TICKS[i])
                        {
                            ModNetwork.sendTechniqueMaxOutputToServer(TechniqueMaxOutputC2SPacket.ACTION_CAST, slot);
                        }
                        else
                        {
                            ModNetwork.sendTechniqueMaxOutputToServer(TechniqueMaxOutputC2SPacket.ACTION_CHARGE_CANCEL, slot);
                        }
                        MaxOutputChargeHud.end();
                    }
                    clearSlotHold(i);
                }
            }

            LAST_HOTKEY_STATES[i] = isDown;
        }
    }

    /** Tab+1/2/3：切换到第 1/2/3 组（组内3个快捷栏位对应按键 1/2/3，实际技能由玩家配置决定）。 */
    private static void handleSkillSlotGroupSwitch(Minecraft minecraft, long window, boolean tabDown)
    {
        if (!tabDown)
        {
            Arrays.fill(LAST_GROUP_SELECT_KEY_STATES, false);
            return;
        }

        for (int i = 0; i < HOTKEY_KEYS.length; i++)
        {
            boolean isDown = GLFW.glfwGetKey(window, HOTKEY_KEYS[i]) == GLFW.GLFW_PRESS;
            if (isDown && !LAST_GROUP_SELECT_KEY_STATES[i] && i < SLOT_GROUP_COUNT)
            {
                currentSkillSlotGroup = i;
                minecraft.player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("招式组 " + (i + 1)),
                        true);
            }
            LAST_GROUP_SELECT_KEY_STATES[i] = isDown;
        }
    }

    @SubscribeEvent
    public static void onClientLivingHurt(LivingHurtEvent event)
    {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || event.getEntity() != minecraft.player || event.getAmount() <= 0.0F)
        {
            return;
        }
        if (MaxOutputChargeHud.isVisible() || hasAnySlotHoldTracked())
        {
            forceInterruptLocalCharge();
        }
    }

    public static void forceInterruptLocalCharge()
    {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getWindow() != null)
        {
            long window = minecraft.getWindow().getWindow();
            for (int i = 0; i < HOTKEY_KEYS.length; i++)
            {
                if (GLFW.glfwGetKey(window, HOTKEY_KEYS[i]) == GLFW.GLFW_PRESS)
                {
                    SLOT_IGNORE_HOLD_UNTIL_RELEASE[i] = true;
                }
            }
        }
        Arrays.fill(SLOT_HOLD_ORIGIN_TICK, -1);
        Arrays.fill(SLOT_CHARGE_BEGIN_TICK, -1);
        Arrays.fill(SLOT_CHARGE_REQUIRED_TICKS, 0);
        Arrays.fill(SLOT_HOLD_RELEASE_MODE, false);
        Arrays.fill(SLOT_HOLD_RELEASE_COMPLETED, false);
        MaxOutputChargeHud.end();
    }

    /** 当前可用调节模式数：星间飞行额外开启「切换星星标记」模式。 */
    private static int resolveAdjustModeCount(Player player)
    {
        return PlayerTechniqueData.isLoveRendezvous(player)
                ? ADJUST_MODE_COUNT_LOVE_RENDEZVOUS
                : ADJUST_MODE_COUNT_BASE;
    }

    private static String adjustModeName(int mode)
    {
        return switch (mode)
        {
            case ADJUST_MODE_BARRIER_RADIUS -> "结界半径";
            case ADJUST_MODE_STAR_MARK -> "星星标记";
            default -> "施法距离";
        };
    }

    /** 按当前调节模式把侧键 ±1 派发到对应参数。 */
    private static void applyAdjustModeDelta(int mode, int delta)
    {
        switch (mode)
        {
            case ADJUST_MODE_BARRIER_RADIUS -> ModNetwork.sendAdjustBarrierRadiusTierToServer(delta);
            case ADJUST_MODE_STAR_MARK -> ModNetwork.sendSelectStarMarkToServer(delta);
            default -> ModNetwork.sendAdjustCastRangeTierToServer(delta);
        }
    }

    private static boolean hasAnySlotHoldTracked()
    {
        for (int i = 0; i < SLOT_HOLD_ORIGIN_TICK.length; i++)
        {
            if (SLOT_HOLD_ORIGIN_TICK[i] >= 0 || SLOT_CHARGE_BEGIN_TICK[i] >= 0)
            {
                return true;
            }
        }
        return false;
    }

    private static void enterSlotCharge(int hotkeyIndex, int slot, int beginTick)
    {
        SLOT_CHARGE_BEGIN_TICK[hotkeyIndex] = beginTick;
        ModNetwork.sendTechniqueMaxOutputToServer(TechniqueMaxOutputC2SPacket.ACTION_CHARGE_BEGIN, slot);
        MaxOutputChargeHud.begin(slot, SLOT_CHARGE_REQUIRED_TICKS[hotkeyIndex], "最大输出 · " + slot, beginTick);
    }

    private static int resolveMaxOutputChargeTicks(
            Player player,
            Optional<IInnateTechniqueSkillSet> setOpt,
            String skillId)
    {
        if (skillId.isEmpty())
        {
            return 0;
        }
        int ticks = setOpt.map(s -> s.getMaxOutputChargeTicksBySkill(player, skillId)).orElse(0);
        if (ticks > 0)
        {
            return ticks;
        }
        return setOpt.flatMap(s -> s.getConfigurableSkills().stream()
                        .filter(skill -> skill.skillId().equals(skillId))
                        .map(TechniqueSkill::maxOutputChargeTicks)
                        .filter(chargeTicks -> chargeTicks > 0)
                        .findFirst())
                .orElse(0);
    }

    private static void abortSlotHold(int hotkeyIndex, boolean sendCancelIfCharging)
    {
        if (sendCancelIfCharging)
        {
            if (SLOT_HOLD_RELEASE_MODE[hotkeyIndex] && SLOT_HOLD_ORIGIN_TICK[hotkeyIndex] >= 0
                    && !SLOT_HOLD_RELEASE_COMPLETED[hotkeyIndex])
            {
                ModNetwork.sendTechniqueHoldReleaseToServer(
                        TechniqueHoldReleaseC2SPacket.ACTION_CANCEL,
                        hotkeyIndexToServerSlot(hotkeyIndex));
            }
            else if (SLOT_CHARGE_BEGIN_TICK[hotkeyIndex] >= 0)
            {
                ModNetwork.sendTechniqueMaxOutputToServer(
                        TechniqueMaxOutputC2SPacket.ACTION_CHARGE_CANCEL, hotkeyIndexToServerSlot(hotkeyIndex));
            }
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getWindow() != null
                && GLFW.glfwGetKey(minecraft.getWindow().getWindow(), HOTKEY_KEYS[hotkeyIndex]) == GLFW.GLFW_PRESS)
        {
            SLOT_IGNORE_HOLD_UNTIL_RELEASE[hotkeyIndex] = true;
        }
        clearSlotHold(hotkeyIndex);
        if (MaxOutputChargeHud.getChargingSlot() == hotkeyIndexToServerSlot(hotkeyIndex))
        {
            MaxOutputChargeHud.end();
        }
    }

    private static void clearSlotHold(int hotkeyIndex)
    {
        SLOT_HOLD_ORIGIN_TICK[hotkeyIndex] = -1;
        SLOT_CHARGE_BEGIN_TICK[hotkeyIndex] = -1;
        SLOT_CHARGE_REQUIRED_TICKS[hotkeyIndex] = 0;
        SLOT_HOLD_RELEASE_MODE[hotkeyIndex] = false;
        SLOT_HOLD_RELEASE_COMPLETED[hotkeyIndex] = false;
    }

    /**
     * 优先读客户端快捷栏缓存；生产环境登录时包可能晚到，回退到技能默认槽位映射，
     * 避免蓄力逻辑与进度条因缓存未就绪而失效。
     */
    private static String resolveConfiguredSkillId(
            String techniqueId,
            int slot,
            Optional<IInnateTechniqueSkillSet> setOpt)
    {
        String skillId = ClientSkillBarCache.getSkillId(techniqueId, slot);
        if (!skillId.isEmpty())
        {
            return skillId;
        }
        return setOpt.flatMap(set -> set.getConfigurableSkills().stream()
                        .filter(skill -> skill.defaultSlot() == slot)
                        .map(TechniqueSkill::skillId)
                        .findFirst())
                .orElse("");
    }

    private static String resolveSkillChargeLabel(Optional<IInnateTechniqueSkillSet> setOpt, String skillId)
    {
        return setOpt.flatMap(set -> set.getConfigurableSkills().stream()
                        .filter(skill -> skill.skillId().equals(skillId))
                        .findFirst()
                        .map(skill -> skill.displayName().getString()))
                .filter(name -> !name.isBlank())
                .orElse(skillId);
    }

    /** 仅超远程虚式茈等「按满即服务端自动完成、无需松手发 release」的长按技能。 */
    private static boolean holdReleaseAutoCompletesWithoutKeyRelease(String skillId)
    {
        return LimitlessTechniqueSkillSet.SKILL_REMOTE_PURPLE.equals(skillId);
    }

    private static void resetChargeInputState()
    {
        Arrays.fill(LAST_HOTKEY_STATES, false);
        Arrays.fill(LAST_GROUP_SELECT_KEY_STATES, false);
        Arrays.fill(SLOT_HOLD_ORIGIN_TICK, -1);
        Arrays.fill(SLOT_CHARGE_BEGIN_TICK, -1);
        Arrays.fill(SLOT_CHARGE_REQUIRED_TICKS, 0);
        Arrays.fill(SLOT_HOLD_RELEASE_MODE, false);
        Arrays.fill(SLOT_HOLD_RELEASE_COMPLETED, false);
        Arrays.fill(SLOT_IGNORE_HOLD_UNTIL_RELEASE, false);
        currentSkillSlotGroup = 0;
        lastShiftDown = false;
        lastShiftPressTick = -1;
        MaxOutputChargeHud.end();
    }

    /** 双击 Shift 的判定窗口（约 400ms）；两次按下之间松开、且中间没有按任何技能键才算数。 */
    private static final int STAR_MARK_CLEAR_DOUBLE_TAP_TICKS = 8;

    private static boolean lastShiftDown;
    /** 上一次 Shift 按下的 tick；-1 表示序列已中断（没有待配对的上一次按下）。 */
    private static int lastShiftPressTick = -1;

    /**
     * 快速双击 Shift 清除「星星标记」。
     * <p>
     * 服务端只在按技能键那一刻收到修饰键掩码，拿不到 Shift 的原始按键状态，所以双击只能在客户端判定；
     * 判定通过后只发一个无字段包，清除走「单独清除」还是「全部清除」由服务端按准星自行决定。
     * 仅在战斗模式且生得术式为星间飞行时生效——Shift 同时是原版潜行键，全局生效会污染潜行体验。
     */
    private static void handleStarMarkDoubleTapClear(Minecraft minecraft, long window)
    {
        boolean shiftDown = glfwKeyPressed(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                || glfwKeyPressed(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
        if (!shiftDown)
        {
            lastShiftDown = false;
            return;
        }
        if (lastShiftDown)
        {
            return;
        }
        lastShiftDown = true;

        if (minecraft.player == null || !PlayerTechniqueData.isLoveRendezvous(minecraft.player))
        {
            lastShiftPressTick = -1;
            return;
        }

        int now = minecraft.player.tickCount;
        if (lastShiftPressTick >= 0 && now - lastShiftPressTick <= STAR_MARK_CLEAR_DOUBLE_TAP_TICKS)
        {
            lastShiftPressTick = -1;
            ModNetwork.sendStarMarkClearToServer();
            return;
        }
        lastShiftPressTick = now;
    }

    /** Shift/Ctrl/Alt（左右键任一按下即置位），与 {@link SpecialMoveInput} 位定义一致。 */
    private static int buildModifierMask(long window)
    {
        int m = 0;
        if (glfwKeyPressed(window, GLFW.GLFW_KEY_LEFT_SHIFT) || glfwKeyPressed(window, GLFW.GLFW_KEY_RIGHT_SHIFT))
        {
            m |= SpecialMoveInput.MOD_SHIFT;
        }
        if (glfwKeyPressed(window, GLFW.GLFW_KEY_LEFT_CONTROL) || glfwKeyPressed(window, GLFW.GLFW_KEY_RIGHT_CONTROL))
        {
            m |= SpecialMoveInput.MOD_CTRL;
        }
        if (glfwKeyPressed(window, GLFW.GLFW_KEY_LEFT_ALT) || glfwKeyPressed(window, GLFW.GLFW_KEY_RIGHT_ALT))
        {
            m |= SpecialMoveInput.MOD_ALT;
        }
        return m;
    }

    private static boolean glfwKeyPressed(long window, int key)
    {
        return GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
    }
}
