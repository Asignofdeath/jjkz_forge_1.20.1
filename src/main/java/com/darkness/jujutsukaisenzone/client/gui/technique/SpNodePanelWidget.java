package com.darkness.jujutsukaisenzone.client.gui.technique;

import com.darkness.jujutsukaisenzone.network.ModNetwork;
import com.darkness.jujutsukaisenzone.player.PlayerCursedGradeData;
import com.darkness.jujutsukaisenzone.player.PlayerSpData;
import com.darkness.jujutsukaisenzone.player.PlayerSpNodeData;
import com.darkness.jujutsukaisenzone.player.PlayerQualityPoolData;
import com.darkness.jujutsukaisenzone.technique.development.SpNodeInstance;
import com.darkness.jujutsukaisenzone.technique.development.SpNodeShelfRules;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.lowdraglib.utils.Size;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * SP 节点面板：经验驱动货架 · 点亮 / 日终刷新 / 付费刷新。
 */
@OnlyIn(Dist.CLIENT)
public final class SpNodePanelWidget extends Widget
{
    private static final int MARGIN = 8;
    private static final int TOP = 60;
    private static final int PADDING = 10;
    private static final int LINE_HEIGHT = 14;
    private static final int ROW_PAD = 3;
    private static final int ROW_GAP = 2;
    private static final int ROW_HEIGHT = LINE_HEIGHT + ROW_PAD * 2;
    private static final int BOTTOM_HINT = 40;
    private static final int MIN_OFFERS_VIEWPORT = ROW_HEIGHT * 3;
    private static final int SCROLLBAR_WIDTH = 5;
    private static final int SCROLLBAR_GAP = 3;

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
    private static final int COLOR_REFRESH = 0xFF3A4A5A;
    private static final int COLOR_REFRESH_HOVER = 0xFF4A6A7A;
    private static final int COLOR_BAR_TRACK = 0xFF202A38;
    private static final int COLOR_BAR_NORMAL = 0xFF3A6A9A;
    private static final int COLOR_SCROLL_THUMB = 0xFF5A7AAA;

    private static final String HELP_LANG_KEY = "gui.jujutsukaisenzone.sp_node.help";

    private final List<Hit> hits = new ArrayList<>();
    private final List<PanelHelpIcon.Hit> helpHits = new ArrayList<>();
    private int resetLeft;
    private int resetTop;
    private int resetRight;
    private int resetBottom;
    private int refreshLeft;
    private int refreshTop;
    private int refreshRight;
    private int refreshBottom;

    private int offersScrollPx;
    private int offersListLeft;
    private int offersListRight;
    private int offersListTop;
    private int offersListBottom;
    private int offersMaxScroll;
    private int offersScrollBarX;
    private int offersScrollBarTop;
    private int offersScrollBarHeight;
    private int offersThumbTop;
    private int offersThumbHeight;
    private boolean offersThumbDragging;
    private int offersThumbGrabOffset;

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
        int panelBottom = top + height - BOTTOM_HINT - PADDING;

        int available = PlayerSpData.getAvailable(player);
        int qualityOccupied = PlayerQualityPoolData.occupied(player);
        int qualityCap = PlayerQualityPoolData.cap(player);
        int qualityAllowance = PlayerQualityPoolData.allowance(player);
        int phys = PlayerSpNodeData.sumPhysicalBonus(player);
        int tech = PlayerSpNodeData.sumTechniqueBonus(player);
        int tier = PlayerSpNodeData.getTier(player);
        int xp = PlayerCursedGradeData.getExperience(player);
        boolean postSpecial = SpNodeShelfRules.isPostSpecial(xp);
        int refreshCost = SpNodeShelfRules.refreshCost(xp);
        int nextTierXp = (tier + 1) * SpNodeShelfRules.XP_PER_TIER;

        String spHeader = tr("header.sp");
        graphics.drawString(font, spHeader, x, y, COLOR_LABEL, false);
        PanelHelpIcon.drawAfterLabel(graphics, font, spHeader, x, y, mouseX, mouseY,
                HELP_LANG_KEY, helpHits);
        int spValueX = x + PanelHelpIcon.labelWithHelpWidth(font, spHeader, true) + 6;
        graphics.drawString(font, Integer.toString(available), spValueX, y, COLOR_VALUE, false);
        y += LINE_HEIGHT;
        graphics.drawString(font, tr("header.quality", qualityOccupied, qualityCap, qualityAllowance),
                x, y, COLOR_LABEL, false);
        y += LINE_HEIGHT;
        graphics.drawString(font, tr("header.tier", tier, xp, nextTierXp), x, y, COLOR_LABEL, false);
        y += LINE_HEIGHT;
        graphics.drawString(font, tr("header.bonus", phys, tech), x, y, COLOR_SECTION, false);
        y += LINE_HEIGHT + 4;
        graphics.fill(x, y, x + Math.min(contentWidth, 220), y + 1, 0x667FE3FF);
        y += 6;

        List<SpNodeInstance> lit = PlayerSpNodeData.getLit(player);
        List<SpNodeInstance> offers = PlayerSpNodeData.getOffers(player);

        if (tier <= 0)
        {
            graphics.drawString(font, tr("locked"), x, y, COLOR_HINT, false);
            y += LINE_HEIGHT + 4;
        }

        // 已点亮：为「在售」视口至少留出 MIN_OFFERS_VIEWPORT（含标题行）。
        int offersHeaderH = LINE_HEIGHT;
        int litLimitBottom = panelBottom - offersHeaderH - MIN_OFFERS_VIEWPORT;
        if (!lit.isEmpty())
        {
            graphics.drawString(font, tr("section.lit"), x, y, COLOR_SECTION, false);
            y += LINE_HEIGHT;
            for (SpNodeInstance node : lit)
            {
                if (y + ROW_HEIGHT > litLimitBottom)
                {
                    graphics.drawString(font, "…", x, y, COLOR_HINT, false);
                    y = litLimitBottom;
                    break;
                }
                y = drawNodeRow(graphics, font, x, y, contentWidth, node, true, mouseX, mouseY, false,
                        Integer.MIN_VALUE, Integer.MAX_VALUE);
            }
            y += 4;
        }

        graphics.drawString(font, tr("section.offers"), x, y, COLOR_SECTION, false);
        y += LINE_HEIGHT;

        drawOffersList(graphics, font, offers, tier, x, y, contentWidth, panelBottom, mouseX, mouseY);

        String refreshLabel = tr("refresh", refreshCost);
        int refreshWidth = font.width(refreshLabel) + 16;
        String resetLabel = tr("reset");
        int resetWidth = font.width(resetLabel) + 16;

        resetRight = left + width - PADDING;
        resetLeft = resetRight - resetWidth;
        resetTop = top + height - BOTTOM_HINT + 2;
        resetBottom = resetTop + 16;

        refreshRight = resetLeft - 6;
        refreshLeft = refreshRight - refreshWidth;
        refreshTop = resetTop;
        refreshBottom = resetBottom;

        boolean refreshHover = mouseX >= refreshLeft && mouseX < refreshRight
                && mouseY >= refreshTop && mouseY < refreshBottom;
        boolean resetHover = mouseX >= resetLeft && mouseX < resetRight
                && mouseY >= resetTop && mouseY < resetBottom;
        graphics.fill(refreshLeft, refreshTop, refreshRight, refreshBottom,
                refreshHover ? COLOR_REFRESH_HOVER : COLOR_REFRESH);
        graphics.drawString(font, refreshLabel, refreshLeft + 8, refreshTop + 4, COLOR_VALUE, false);
        graphics.fill(resetLeft, resetTop, resetRight, resetBottom,
                resetHover ? COLOR_RESET_HOVER : COLOR_RESET);
        graphics.drawString(font, resetLabel, resetLeft + 8, resetTop + 4, COLOR_VALUE, false);

        String hint = postSpecial ? tr("hint.post_special") : tr("hint");
        graphics.drawString(font, hint, left + PADDING, resetTop + 20, COLOR_HINT, false);

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

    private void drawOffersList(
            GuiGraphics graphics,
            Font font,
            List<SpNodeInstance> offers,
            int tier,
            int x,
            int y,
            int contentWidth,
            int bottom,
            int mouseX,
            int mouseY)
    {
        int entryCount = offers.size();
        int contentHeight = entryCount <= 0
                ? LINE_HEIGHT
                : entryCount * ROW_HEIGHT + Math.max(0, entryCount - 1) * ROW_GAP;
        int viewportH = Math.max(0, bottom - y);
        boolean needScroll = contentHeight > viewportH;
        int listW = needScroll ? Math.max(40, contentWidth - SCROLLBAR_WIDTH - SCROLLBAR_GAP) : contentWidth;
        offersMaxScroll = Math.max(0, contentHeight - viewportH);
        offersScrollPx = Mth.clamp(offersScrollPx, 0, offersMaxScroll);

        offersListLeft = x;
        offersListRight = x + contentWidth;
        offersListTop = y;
        offersListBottom = bottom;
        offersScrollBarX = x + contentWidth - SCROLLBAR_WIDTH;
        offersScrollBarTop = y;
        offersScrollBarHeight = viewportH;
        updateOffersThumbGeometry();

        graphics.enableScissor(x, offersListTop, x + listW, offersListBottom);
        if (entryCount == 0)
        {
            graphics.drawString(font, tr(tier <= 0 ? "locked" : "offers.empty"),
                    x + 4, offersListTop + 2, COLOR_HINT, false);
        }
        else
        {
            int rowY = offersListTop - offersScrollPx;
            for (SpNodeInstance node : offers)
            {
                int rowBottom = rowY + ROW_HEIGHT;
                boolean visible = rowBottom > offersListTop && rowY < offersListBottom;
                if (visible)
                {
                    drawNodeRow(graphics, font, x, rowY, listW, node, false, mouseX, mouseY, true,
                            offersListTop, offersListBottom);
                }
                rowY = rowBottom + ROW_GAP;
            }
        }
        graphics.disableScissor();

        if (needScroll)
        {
            drawOffersScrollbar(graphics, mouseX, mouseY);
        }
        else
        {
            offersThumbHeight = 0;
        }
    }

    private int drawNodeRow(
            GuiGraphics graphics,
            Font font,
            int x,
            int y,
            int contentWidth,
            SpNodeInstance node,
            boolean lit,
            int mouseX,
            int mouseY,
            boolean clickable,
            int clipTop,
            int clipBottom)
    {
        int rowTop = y;
        int rowBottom = y + ROW_HEIGHT;
        int hitTop = Math.max(rowTop, clipTop);
        int hitBottom = Math.min(rowBottom, clipBottom);
        boolean hover = mouseX >= x && mouseX < x + contentWidth
                && mouseY >= hitTop && mouseY < hitBottom
                && hitBottom > hitTop;
        graphics.fill(x, rowTop, x + contentWidth, rowBottom, hover ? COLOR_ROW_HOVER : COLOR_ROW_IDLE);

        String name = nodeName(node);
        int nameColor = lit ? COLOR_UNLOCKED : COLOR_VALUE;
        graphics.drawString(font, name, x + 4, rowTop + ROW_PAD + 2, nameColor, false);

        String detail = nodeDetail(node);
        int detailX = x + contentWidth - font.width(detail) - 4;
        graphics.drawString(font, detail, detailX, rowTop + ROW_PAD + 2,
                lit ? COLOR_UNLOCKED : COLOR_HINT, false);

        if (clickable && !lit && hitBottom > hitTop)
        {
            hits.add(new Hit(x, hitTop, x + contentWidth, hitBottom, node.instanceId()));
        }
        return rowBottom + ROW_GAP;
    }

    private void updateOffersThumbGeometry()
    {
        if (offersMaxScroll <= 0 || offersScrollBarHeight <= 0)
        {
            offersThumbTop = offersScrollBarTop;
            offersThumbHeight = 0;
            return;
        }
        int track = offersScrollBarHeight;
        offersThumbHeight = Math.max(16, track * track / (track + offersMaxScroll));
        int travel = Math.max(1, track - offersThumbHeight);
        offersThumbTop = offersScrollBarTop
                + (int) Math.round((double) offersScrollPx / offersMaxScroll * travel);
    }

    private void drawOffersScrollbar(GuiGraphics graphics, int mouseX, int mouseY)
    {
        int x1 = offersScrollBarX;
        int y1 = offersScrollBarTop;
        int x2 = x1 + SCROLLBAR_WIDTH;
        int y2 = y1 + offersScrollBarHeight;
        graphics.fill(x1, y1, x2, y2, COLOR_BAR_TRACK);
        boolean thumbHover = mouseX >= x1 && mouseX < x2
                && mouseY >= offersThumbTop && mouseY < offersThumbTop + offersThumbHeight;
        int thumbColor = (thumbHover || offersThumbDragging) ? COLOR_SCROLL_THUMB : COLOR_BAR_NORMAL;
        graphics.fill(x1, offersThumbTop, x2, offersThumbTop + offersThumbHeight, thumbColor);
    }

    private boolean isOverOffersList(double mouseX, double mouseY)
    {
        return mouseX >= offersListLeft && mouseX < offersListRight
                && mouseY >= offersListTop && mouseY < offersListBottom;
    }

    private boolean isOverOffersScrollbar(double mouseX, double mouseY)
    {
        return offersMaxScroll > 0
                && mouseX >= offersScrollBarX && mouseX < offersScrollBarX + SCROLLBAR_WIDTH
                && mouseY >= offersScrollBarTop
                && mouseY < offersScrollBarTop + offersScrollBarHeight;
    }

    private void setOffersScrollFromThumbCenter(double mouseY)
    {
        if (offersMaxScroll <= 0)
        {
            offersScrollPx = 0;
            return;
        }
        int travel = Math.max(1, offersScrollBarHeight - offersThumbHeight);
        double rel = mouseY - offersScrollBarTop - offersThumbHeight / 2.0D;
        offersScrollPx = Mth.clamp((int) Math.round(rel / travel * offersMaxScroll), 0, offersMaxScroll);
        updateOffersThumbGeometry();
    }

    private void setOffersScrollFromDrag(double mouseY)
    {
        if (offersMaxScroll <= 0)
        {
            return;
        }
        int travel = Math.max(1, offersScrollBarHeight - offersThumbHeight);
        double rel = mouseY - offersScrollBarTop - offersThumbGrabOffset;
        offersScrollPx = Mth.clamp((int) Math.round(rel / travel * offersMaxScroll), 0, offersMaxScroll);
        updateOffersThumbGeometry();
    }

    private static String nodeName(SpNodeInstance node)
    {
        String sizeKey = node.size() >= 2 ? "medium" : "small";
        return Component.translatable(
                "gui.jujutsukaisenzone.sp_node.family." + node.family().id() + "." + sizeKey).getString();
    }

    private static String nodeDetail(SpNodeInstance node)
    {
        if (node.family().isPowerRow())
        {
            return tr("node.detail.power", node.spCost(), node.physicalBonus(), node.techniqueBonus());
        }
        return tr("node.detail.attr", node.spCost(), node.attributeBonus());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if (button == 0)
        {
            if (isOverOffersScrollbar(mouseX, mouseY))
            {
                if (mouseY >= offersThumbTop && mouseY < offersThumbTop + offersThumbHeight)
                {
                    offersThumbDragging = true;
                    offersThumbGrabOffset = (int) mouseY - offersThumbTop;
                }
                else
                {
                    setOffersScrollFromThumbCenter(mouseY);
                }
                return true;
            }
            if (mouseX >= resetLeft && mouseX < resetRight && mouseY >= resetTop && mouseY < resetBottom)
            {
                ModNetwork.sendResetSpNodesRequest();
                return true;
            }
            if (mouseX >= refreshLeft && mouseX < refreshRight && mouseY >= refreshTop && mouseY < refreshBottom)
            {
                ModNetwork.sendRefreshSpOffersRequest();
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

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY)
    {
        if (offersThumbDragging && button == 0)
        {
            setOffersScrollFromDrag(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button)
    {
        if (button == 0 && offersThumbDragging)
        {
            offersThumbDragging = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseWheelMove(double mouseX, double mouseY, double wheelDelta)
    {
        if (offersMaxScroll > 0 && isOverOffersList(mouseX, mouseY))
        {
            int step = Math.max(ROW_HEIGHT, LINE_HEIGHT * 2);
            offersScrollPx = Mth.clamp(
                    offersScrollPx - (int) Math.signum(wheelDelta) * step,
                    0,
                    offersMaxScroll);
            updateOffersThumbGeometry();
            return true;
        }
        return super.mouseWheelMove(mouseX, mouseY, wheelDelta);
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
