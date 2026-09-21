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
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
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
 * 鼠标侧键 4/5 在两种模式下调节不同参数：
 * - 施法距离模式：调节施法距离档位（-25% / +25%）。
 * - 结界半径模式：调节结界半径档位（8 / 16 / 32 / 64）。
 * 鼠标中键在两种模式间切换。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class TechniqueSkillKeyEvents
{
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
    /** 0 = 施法距离模式，1 = 结界半径模式。 */
    private static int currentAdjustMode = 0;

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

        /* 鼠标侧键/中键的档位调整只需战斗模式，无需生得术式 */
        long window = minecraft.getWindow().getWindow();
        boolean mouse4Down = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_4) == GLFW.GLFW_PRESS;
        boolean mouse5Down = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_5) == GLFW.GLFW_PRESS;
        boolean mouseMiddleDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_3) == GLFW.GLFW_PRESS;

        if (mouseMiddleDown && !lastMouseMiddleDown)
        {
            currentAdjustMode = 1 - currentAdjustMode;
            String modeName = (currentAdjustMode == 0) ? "施法距离" : "结界半径";
            minecraft.player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("切换至：" + modeName + " 调节模式"), true);
        }
        lastMouseMiddleDown = mouseMiddleDown;

        if (mouse4Down && !lastMouseButton4Down)
        {
            if (currentAdjustMode == 0)
            {
                ModNetwork.sendAdjustCastRangeTierToServer(-1);
            }
            else
            {
                ModNetwork.sendAdjustBarrierRadiusTierToServer(-1);
            }
        }

        if (mouse5Down && !lastMouseButton5Down)
        {
            if (currentAdjustMode == 0)
            {
                ModNetwork.sendAdjustCastRangeTierToServer(1);
            }
            else
            {
                ModNetwork.sendAdjustBarrierRadiusTierToServer(1);
            }
        }

        lastMouseButton4Down = mouse4Down;
        lastMouseButton5Down = mouse5Down;

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
        MaxOutputChargeHud.end();
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
