package com.darkness.jujutsukaisenzone.client.gui.technique;

import com.darkness.jujutsukaisenzone.network.ModNetwork;
import com.darkness.jujutsukaisenzone.player.PlayerSpData;
import com.darkness.jujutsukaisenzone.player.PlayerSpNodeData;
import com.darkness.jujutsukaisenzone.player.PlayerQualityPoolData;
import com.darkness.jujutsukaisenzone.technique.development.SpNodeRegistry;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.lowdraglib.utils.Size;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * SP 节点面板：点亮可重置的基础战斗强度加点。
 */
@OnlyIn(Dist.CLIENT)
public final class SpNodePanelWidget extends Widget
{
    private static final int MARGIN = 8;
    private static final int TOP = 60;
    private static final int PADDING = 10;
    private static final int LINE_HEIGHT = 14;
    private static final int ROW_PAD = 3;
    private static final int BOTTOM_HINT = 22;

    private static final int COLOR_BACKGROUND = 0xCC12161C;
    private static final int COLOR_BORDER = 0xFF3A5A8A;
    private static final int COLOR_SECTION = 0xFF7FE3FF;
    private static final int COLOR_LABEL = 0xFF9AA4B0;
    private static final int COLOR_VALUE = 0xFFEDEDED;
    private static final int COLOR_HINT = 0xFF6E7883;
    private static final int COLOR_UNLOCKED = 0xFF4CAF70;
    private static final int COLOR_ROW_IDLE = 0xFF1A2230;
    private static final int COLOR_ROW_HOVER = 0xFF2A3A55;
    private static final int COLOR_RESET = 0xFF5A3A3A;
    private static final int COLOR_RESET_HOVER = 0xFF7A4A4A;

    private static final String HELP_LANG_KEY = "gui.jujutsukaisenzone.sp_node.help";

    private final List<Hit> hits = new ArrayList<>();
    private final List<PanelHelpIcon.Hit> helpHits = new ArrayList<>();
    private int resetLeft;
    private int resetTop;
    private int resetRight;
    private int resetBottom;

    public SpNodePanelWidget()
    {
        super(new Position(0, 0), new Size(1, 1));
        setClientSideWidget();
    }

    @Override
    public void onScreenSizeUpdate(int screenWidth, int screenHeight)
    {
        setSelfPosition(0, 0);
        setSize(new Size(screenWidth, screenHeight));
        super.onScreenSizeUpdate(screenWidth, screenHeight);
    }

    @Override
    public void drawInBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks)
    {
        super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
        hits.clear();
        helpHits.clear();

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null)
        {
            setHoverTooltips(List.of());
            return;
        }

        int screenWidth = getSize().width;
        int screenHeight = getSize().height;
        if (screenWidth <= 2 || screenHeight <= 2)
        {
            screenWidth = minecraft.getWindow().getGuiScaledWidth();
            screenHeight = minecraft.getWindow().getGuiScaledHeight();
        }

        int left = MARGIN;
        int top = TOP;
        int width = Math.max(100, screenWidth - MARGIN * 2);
        int height = Math.max(40, screenHeight - TOP - MARGIN);
        drawFrame(graphics, left, top, width, height);

        Font font = minecraft.font;
        int x = left + PADDING;
        int y = top + PADDING;
        int contentWidth = width - PADDING * 2;
        int bottom = top + height - BOTTOM_HINT - PADDING;

        int available = PlayerSpData.getAvailable(player);
        // 质量是权重和（Σ spCost），并且与 SE 共用同一个池 —— 显示占用 / 上限（+宽限）。
        int qualityOccupied = PlayerQualityPoolData.occupied(player);
        int qualityCap = PlayerQualityPoolData.cap(player);
        int qualityAllowance = PlayerQualityPoolData.allowance();
        int phys = PlayerSpNodeData.sumPhysicalBonus(player);
        int tech = PlayerSpNodeData.sumTechniqueBonus(player);

        String spHeader = tr("header.sp");
        graphics.drawString(font, spHeader, x, y, COLOR_LABEL, false);
        PanelHelpIcon.drawAfterLabel(graphics, font, spHeader, x, y, mouseX, mouseY,
                HELP_LANG_KEY, helpHits);
        int spValueX = x + PanelHelpIcon.labelWithHelpWidth(font, spHeader, true) + 6;
        graphics.drawString(font, Integer.toString(available), spValueX, y, COLOR_VALUE, false);
        y += LINE_HEIGHT;
        String qualityHeader = tr("header.quality", qualityOccupied, qualityCap, qualityAllowance);
        graphics.drawString(font, qualityHeader, x, y, COLOR_LABEL, false);
        y += LINE_HEIGHT;
        graphics.drawString(font, tr("header.bonus", phys, tech), x, y, COLOR_SECTION, false);
        y += LINE_HEIGHT + 4;
        graphics.fill(x, y, x + Math.min(contentWidth, 220), y + 1, 0x667FE3FF);
        y += 6;

        for (SpNodeRegistry.SpNodeDef def : SpNodeRegistry.all())
        {
            if (y + LINE_HEIGHT + ROW_PAD * 2 > bottom)
            {
                graphics.drawString(font, "…", x, y, COLOR_HINT, false);
                break;
            }
            boolean lit = PlayerSpNodeData.isUnlocked(player, def.nodeId());
            int rowTop = y;
            int rowBottom = y + LINE_HEIGHT + ROW_PAD * 2;
            boolean hover = mouseX >= x && mouseX < x + contentWidth && mouseY >= rowTop && mouseY < rowBottom;
            graphics.fill(x, rowTop, x + contentWidth, rowBottom, hover ? COLOR_ROW_HOVER : COLOR_ROW_IDLE);

            String name = tr("node." + def.nodeId());
            int nameColor = lit ? COLOR_UNLOCKED : COLOR_VALUE;
            graphics.drawString(font, name, x + 4, rowTop + ROW_PAD + 2, nameColor, false);

            String detail = tr("node.detail", def.spCost(), def.physicalBonus(), def.techniqueBonus());
            int detailX = x + contentWidth - font.width(detail) - 4;
            graphics.drawString(font, detail, detailX, rowTop + ROW_PAD + 2,
                    lit ? COLOR_UNLOCKED : COLOR_HINT, false);

            if (!lit)
            {
                hits.add(new Hit(x, rowTop, x + contentWidth, rowBottom, def.nodeId()));
            }
            y = rowBottom + 2;
        }

        String resetLabel = tr("reset");
        int resetWidth = font.width(resetLabel) + 16;
        resetLeft = left + width - PADDING - resetWidth;
        resetTop = top + height - BOTTOM_HINT;
        resetRight = left + width - PADDING;
        resetBottom = resetTop + 16;
        boolean resetHover = mouseX >= resetLeft && mouseX < resetRight
                && mouseY >= resetTop && mouseY < resetBottom;
        graphics.fill(resetLeft, resetTop, resetRight, resetBottom,
                resetHover ? COLOR_RESET_HOVER : COLOR_RESET);
        graphics.drawString(font, resetLabel, resetLeft + 8, resetTop + 4, COLOR_VALUE, false);
        graphics.drawString(font, tr("hint"), left + PADDING, resetTop + 4, COLOR_HINT, false);

        String hoverKey = PanelHelpIcon.findHoverKey(helpHits, mouseX, mouseY);
        if (hoverKey != null)
        {
            setHoverTooltips(PanelHelpIcon.tooltipLines(hoverKey));
        }
        else
        {
            setHoverTooltips(List.of());
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if (button == 0)
        {
            if (mouseX >= resetLeft && mouseX < resetRight && mouseY >= resetTop && mouseY < resetBottom)
            {
                ModNetwork.sendResetSpNodesRequest();
                return true;
            }
            for (Hit hit : hits)
            {
                if (mouseX >= hit.left && mouseX < hit.right && mouseY >= hit.top && mouseY < hit.bottom)
                {
                    ModNetwork.sendUnlockSpNodeRequest(hit.id);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static void drawFrame(GuiGraphics graphics, int left, int top, int width, int height)
    {
        graphics.fill(left, top, left + width, top + height, COLOR_BACKGROUND);
        graphics.fill(left, top, left + width, top + 1, COLOR_BORDER);
        graphics.fill(left, top + height - 1, left + width, top + height, COLOR_BORDER);
        graphics.fill(left, top, left + 1, top + height, COLOR_BORDER);
        graphics.fill(left + width - 1, top, left + width, top + height, COLOR_BORDER);
    }

    private static String tr(String path, Object... args)
    {
        return Component.translatable("gui.jujutsukaisenzone.sp_node." + path, args).getString();
    }

    private record Hit(int left, int top, int right, int bottom, String id)
    {
    }
}
