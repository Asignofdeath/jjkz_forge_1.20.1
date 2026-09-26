package com.darkness.jujutsukaisenzone.client.gui.technique;

import com.darkness.jujutsukaisenzone.network.ModNetwork;
import com.darkness.jujutsukaisenzone.player.PlayerSeLoadoutData;
import com.darkness.jujutsukaisenzone.technique.se.SeEffectDef;
import com.darkness.jujutsukaisenzone.technique.se.SeEffectRegistry;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.lowdraglib.utils.Size;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * SE（Status Effect / 术式侵入）面板：2 槽热装咒物效果。
 */
@OnlyIn(Dist.CLIENT)
public final class SeLoadoutPanelWidget extends Widget
{
    private static final int MARGIN = 8;
    private static final int TOP = 60;
    private static final int PADDING = 10;
    private static final int LINE_HEIGHT = 12;
    private static final int SLOT_HEIGHT = 28;
    private static final int ROW_PAD = 3;
    private static final int COLUMN_GAP = 14;

    private static final int COLOR_BACKGROUND = 0xCC12161C;
    private static final int COLOR_BORDER = 0xFF3A5A8A;
    private static final int COLOR_SECTION = 0xFF7FE3FF;
    private static final int COLOR_LABEL = 0xFF9AA4B0;
    private static final int COLOR_VALUE = 0xFFEDEDED;
    private static final int COLOR_HINT = 0xFF6E7883;
    private static final int COLOR_SELECTED = 0xFF3A5A8A;
    private static final int COLOR_ROW_IDLE = 0xFF1A2230;
    private static final int COLOR_ROW_HOVER = 0xFF2A3A55;
    private static final int COLOR_ACTIVE = 0xFF4CAF70;
    private static final int COLOR_MISSING = 0xFF8A6060;

    private int selectedSlot;
    private final List<Hit> hits = new ArrayList<>();

    public SeLoadoutPanelWidget()
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

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null)
        {
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
        int columnWidth = Math.max(80, (width - COLUMN_GAP) / 2);
        int leftX = left + PADDING;
        int rightX = left + columnWidth + COLUMN_GAP + PADDING;
        int contentW = columnWidth - PADDING * 2;
        int y = top + PADDING;
        int bottom = top + height - PADDING;

        graphics.drawString(font, tr("section.slots"), leftX, y, COLOR_SECTION, false);
        y += LINE_HEIGHT + 4;

        List<String> slots = PlayerSeLoadoutData.getSlots(player);
        for (int i = 0; i < PlayerSeLoadoutData.SLOT_COUNT; i++)
        {
            int slotTop = y;
            int slotBottom = y + SLOT_HEIGHT;
            boolean selected = selectedSlot == i;
            boolean hover = mouseX >= leftX && mouseX < leftX + contentW
                    && mouseY >= slotTop && mouseY < slotBottom;
            int bg = selected ? COLOR_SELECTED : (hover ? COLOR_ROW_HOVER : COLOR_ROW_IDLE);
            graphics.fill(leftX, slotTop, leftX + contentW, slotBottom, bg);

            graphics.drawString(font, tr("slot", i + 1), leftX + 4, slotTop + 4, COLOR_LABEL, false);

            String seId = i < slots.size() ? slots.get(i) : "";
            if (seId == null || seId.isBlank())
            {
                graphics.drawString(font, tr("empty"), leftX + 4, slotTop + 14, COLOR_HINT, false);
            }
            else
            {
                SeEffectDef def = SeEffectRegistry.find(seId).orElse(null);
                String name = def == null ? seId : def.item().getDescription().getString();
                graphics.drawString(font, name, leftX + 4, slotTop + 14, COLOR_ACTIVE, false);
            }
            hits.add(new Hit(leftX, slotTop, leftX + contentW, slotBottom, HitKind.SLOT, i, ""));
            y = slotBottom + 4;
        }

        graphics.drawString(font, tr("hint.slot"), leftX, y, COLOR_HINT, false);

        int listY = top + PADDING;
        graphics.drawString(font, tr("section.catalog"), rightX, listY, COLOR_SECTION, false);
        listY += LINE_HEIGHT + 4;

        for (SeEffectDef def : SeEffectRegistry.all())
        {
            if (listY + LINE_HEIGHT * 2 + ROW_PAD * 2 > bottom)
            {
                graphics.drawString(font, "…", rightX, listY, COLOR_HINT, false);
                break;
            }
            boolean active = PlayerSeLoadoutData.isSeEquipped(player, def.seId());
            boolean hasItem = hasItem(player, def.item());
            int rowTop = listY;
            int rowBottom = listY + LINE_HEIGHT * 2 + ROW_PAD * 2;
            boolean hover = mouseX >= rightX && mouseX < rightX + contentW
                    && mouseY >= rowTop && mouseY < rowBottom;
            graphics.fill(rightX, rowTop, rightX + contentW, rowBottom,
                    hover ? COLOR_ROW_HOVER : COLOR_ROW_IDLE);

            String itemName = def.item().getDescription().getString();
            int nameColor = active ? COLOR_ACTIVE : (hasItem ? COLOR_VALUE : COLOR_MISSING);
            graphics.drawString(font, itemName, rightX + 4, rowTop + ROW_PAD, nameColor, false);

            String detail = tr("effect." + def.seId(),
                    Math.round(def.procChance() * 100),
                    def.igniteSeconds());
            graphics.drawString(font, detail, rightX + 4, rowTop + ROW_PAD + LINE_HEIGHT, COLOR_HINT, false);

            String status = active ? tr("status.equipped")
                    : (hasItem ? tr("status.ready") : tr("status.missing"));
            int statusX = rightX + contentW - font.width(status) - 4;
            graphics.drawString(font, status, statusX, rowTop + ROW_PAD, nameColor, false);

            if (!active && hasItem)
            {
                hits.add(new Hit(rightX, rowTop, rightX + contentW, rowBottom,
                        HitKind.APPLY, selectedSlot, def.seId()));
            }
            listY = rowBottom + 2;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if (button == 0)
        {
            for (Hit hit : hits)
            {
                if (mouseX >= hit.left && mouseX < hit.right && mouseY >= hit.top && mouseY < hit.bottom)
                {
                    if (hit.kind == HitKind.SLOT)
                    {
                        LocalPlayer player = Minecraft.getInstance().player;
                        if (player != null)
                        {
                            List<String> slots = PlayerSeLoadoutData.getSlots(player);
                            String current = hit.slotIndex < slots.size() ? slots.get(hit.slotIndex) : "";
                            if (current != null && !current.isBlank())
                            {
                                ModNetwork.sendUnequipSeRequest(hit.slotIndex);
                            }
                            selectedSlot = hit.slotIndex;
                        }
                        return true;
                    }
                    if (hit.kind == HitKind.APPLY)
                    {
                        ModNetwork.sendEquipSeRequest(hit.slotIndex, hit.seId);
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static boolean hasItem(LocalPlayer player, Item item)
    {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++)
        {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(item))
            {
                return true;
            }
        }
        return false;
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
        return Component.translatable("gui.jujutsukaisenzone.se." + path, args).getString();
    }

    private enum HitKind
    {
        SLOT,
        APPLY
    }

    private record Hit(int left, int top, int right, int bottom, HitKind kind, int slotIndex, String seId)
    {
    }
}
