package com.darkness.jujutsukaisenzone.client.gui.commandwheel;

import com.darkness.jujutsukaisenzone.client.CommandWheelKeyHandler;
import com.darkness.jujutsukaisenzone.commandwheel.CommandWheelCatalog;
import com.darkness.jujutsukaisenzone.commandwheel.CommandWheelEntry;
import com.darkness.jujutsukaisenzone.commandwheel.CommandWheelIds;
import com.darkness.jujutsukaisenzone.commandwheel.CommandWheelPage;
import com.darkness.jujutsukaisenzone.network.CommandWheelNetwork;
import com.darkness.jujutsukaisenzone.entity.MahoragaEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import org.lwjgl.glfw.GLFW;
import yesman.epicfight.api.client.camera.EpicFightCameraAPI;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * 指令圆盘：不暂停游戏；默认通用页，R 循环切页；松手或左键确认当前扇区。
 */
public final class CommandWheelScreen extends Screen
{
    private static final int RADIUS = 90;
    private static final int INNER_DEADZONE = 26;
    private static final CommandWheelPage[] PAGES = CommandWheelPage.values();

    private List<CommandWheelEntry> entries = List.of();
    private int hoveredIndex = -1;
    /** 打开后短暂忽略松手，避免首 tick 误关。 */
    private int openTicks;
    private boolean mahoragaWheel;
    @Nullable
    private UUID mahoragaId;
    /** 非魔虚罗模式下的当前页；打开默认通用页。 */
    private CommandWheelPage page = CommandWheelPage.GENERAL;

    public CommandWheelScreen()
    {
        super(Component.translatable("command_wheel.jujutsukaisenzone.title"));
    }

    @Override
    protected void init()
    {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && CommandWheelCatalog.isAimingAtMahoraga(player, Minecraft.getInstance().hitResult)
                && Minecraft.getInstance().hitResult instanceof EntityHitResult hit
                && hit.getEntity() instanceof MahoragaEntity mahoraga)
        {
            mahoragaWheel = true;
            mahoragaId = mahoraga.getUUID();
        }
        page = CommandWheelPage.GENERAL;
        refreshEntries();
        hoveredIndex = -1;
        openTicks = 0;
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers)
    {
        if (CommandWheelKeyHandler.KEY_MAPPING.matches(keyCode, scanCode))
        {
            return true;
        }
        if (!mahoragaWheel && keyCode == GLFW.GLFW_KEY_R)
        {
            cyclePage();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void tick()
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null)
        {
            onClose();
            return;
        }
        openTicks++;
        /* 必须用 GLFW 物理键：KeyMapping#isDown 在 IN_GAME 上下文于 GUI 打开后恒为 false */
        if (openTicks >= 2 && !CommandWheelKeyHandler.isBoundKeyPhysicallyDown())
        {
            confirmAndClose();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        refreshEntries();
        int cx = width / 2;
        int cy = height / 2;
        updateHover(mouseX, mouseY, cx, cy);

        graphics.fill(cx - RADIUS - 8, cy - RADIUS - 8, cx + RADIUS + 8, cy + RADIUS + 8, 0x66000000);
        graphics.fill(cx - INNER_DEADZONE, cy - INNER_DEADZONE, cx + INNER_DEADZONE, cy + INNER_DEADZONE, 0x88000000);

        LocalPlayer player = Minecraft.getInstance().player;
        int n = entries.size();
        if (n == 0)
        {
            graphics.drawCenteredString(font, titleForPage(), cx, cy - RADIUS - 16, 0xFFDDDDDD);
            graphics.drawCenteredString(
                    font,
                    Component.translatable(mahoragaWheel
                            ? "command_wheel.jujutsukaisenzone.empty"
                            : page == CommandWheelPage.TECHNIQUE
                                    ? "command_wheel.jujutsukaisenzone.empty_technique"
                                    : "command_wheel.jujutsukaisenzone.empty"),
                    cx,
                    cy - 4,
                    0xFFAAAAAA);
            if (!mahoragaWheel)
            {
                graphics.drawCenteredString(
                        font,
                        Component.translatable("command_wheel.jujutsukaisenzone.hint_cycle"),
                        cx,
                        cy + RADIUS + 6,
                        0xFF777777);
            }
            super.render(graphics, mouseX, mouseY, partialTick);
            return;
        }

        double slice = (Math.PI * 2.0D) / n;
        for (int i = 0; i < n; i++)
        {
            CommandWheelEntry entry = entries.get(i);
            boolean enabled = player != null && entry.isEnabled(player);
            boolean active = player != null && entry.isActive(player);
            boolean hover = i == hoveredIndex && enabled;
            double mid = -Math.PI / 2.0D + i * slice + slice * 0.5D;
            int lx = cx + (int) (Math.cos(mid) * (RADIUS * 0.68D));
            int ly = cy + (int) (Math.sin(mid) * (RADIUS * 0.68D));
            Component label = entry.labelFor(player);
            int bw = font.width(label) + 10;
            int bg = !enabled ? 0x66444444 : hover ? 0xCCB8860B : active ? 0xAA5B2C8A : 0x9922222A;
            graphics.fill(lx - bw / 2, ly - 7, lx + bw / 2, ly + 9, bg);
            int textColor = enabled ? (hover ? 0xFFFFFFAA : 0xFFECECEC) : 0xFF777777;
            graphics.drawCenteredString(font, label, lx, ly - 3, textColor);
        }

        graphics.drawCenteredString(font, titleForPage(), cx, cy - RADIUS - 16, 0xFFDDDDDD);
        if (hoveredIndex >= 0 && hoveredIndex < n)
        {
            graphics.drawCenteredString(font, entries.get(hoveredIndex).labelFor(player), cx, cy - 4, 0xFFFFFFFF);
        }
        else
        {
            graphics.drawCenteredString(
                    font,
                    Component.translatable("command_wheel.jujutsukaisenzone.hint"),
                    cx,
                    cy - 4,
                    0xFF888888);
        }
        if (!mahoragaWheel)
        {
            graphics.drawCenteredString(
                    font,
                    Component.translatable("command_wheel.jujutsukaisenzone.hint_cycle"),
                    cx,
                    cy + RADIUS + 6,
                    0xFF777777);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if (button == 0)
        {
            confirmAndClose();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void cyclePage()
    {
        int index = page.ordinal();
        page = PAGES[(index + 1) % PAGES.length];
        hoveredIndex = -1;
        refreshEntries();
    }

    private Component titleForPage()
    {
        if (mahoragaWheel)
        {
            return Component.translatable("command_wheel.jujutsukaisenzone.mahoraga_title");
        }
        return Component.translatable(page == CommandWheelPage.GENERAL
                ? "command_wheel.jujutsukaisenzone.title_general"
                : "command_wheel.jujutsukaisenzone.title_technique");
    }

    private void refreshEntries()
    {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null)
        {
            entries = List.of();
            return;
        }
        if (mahoragaWheel)
        {
            entries = CommandWheelCatalog.entriesFor(player, true);
            return;
        }
        entries = CommandWheelCatalog.entriesFor(player, page);
    }

    private void confirmAndClose()
    {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && hoveredIndex >= 0 && hoveredIndex < entries.size())
        {
            CommandWheelEntry entry = entries.get(hoveredIndex);
            if (entry.isEnabled(player))
            {
                UUID targetUuid = null;
                if (CommandWheelIds.TEN_SHADOWS_ATTACK_ORDER.equals(entry.id()))
                {
                    targetUuid = resolveEpicFightLockTargetUuid();
                }
                else if (CommandWheelIds.MAHORAGA_RIDE.equals(entry.id()))
                {
                    targetUuid = mahoragaId;
                }
                CommandWheelNetwork.sendActionToServer(entry.id(), targetUuid);
            }
        }
        onClose();
    }

    @Nullable
    private static UUID resolveEpicFightLockTargetUuid()
    {
        EpicFightCameraAPI cameraApi = EpicFightCameraAPI.getInstance();
        if (!cameraApi.isLockingOnTarget())
        {
            return null;
        }
        LivingEntity target = cameraApi.getFocusingEntity();
        if (target == null || !target.isAlive())
        {
            return null;
        }
        return target.getUUID();
    }

    private void updateHover(int mouseX, int mouseY, int cx, int cy)
    {
        double dx = mouseX - cx;
        double dy = mouseY - cy;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < INNER_DEADZONE || entries.isEmpty())
        {
            hoveredIndex = -1;
            return;
        }
        double angle = Math.atan2(dy, dx);
        double normalized = angle + Math.PI / 2.0D;
        if (normalized < 0)
        {
            normalized += Math.PI * 2.0D;
        }
        int n = entries.size();
        hoveredIndex = Mth.clamp((int) (normalized / (Math.PI * 2.0D / n)), 0, n - 1);
    }
}
