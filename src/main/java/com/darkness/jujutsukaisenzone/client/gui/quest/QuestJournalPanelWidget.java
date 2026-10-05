package com.darkness.jujutsukaisenzone.client.gui.quest;

import com.darkness.jujutsukaisenzone.network.NetworkSenders;
import com.darkness.jujutsukaisenzone.network.packet.TrackQuestC2SPacket;
import com.darkness.jujutsukaisenzone.player.PlayerQuestData;
import com.darkness.jujutsukaisenzone.quest.mainline.MainQuestDef;
import com.darkness.jujutsukaisenzone.quest.mainline.MainQuestDefs;
import com.darkness.jujutsukaisenzone.quest.mainline.MainQuestStatus;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.lowdraglib.utils.Size;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 任务全屏面板：列出主线并允许点击追踪 / 取消追踪。
 */
@OnlyIn(Dist.CLIENT)
public final class QuestJournalPanelWidget extends Widget
{
    private static final int MARGIN = 16;
    private static final int ROW_HEIGHT = 84;
    private static final int ROW_GAP = 6;
    private static final int COLOR_BACKGROUND = 0xE812161C;
    private static final int COLOR_BORDER = 0xFF3A5A8A;
    private static final int COLOR_TITLE = 0xFFE8C56B;
    private static final int COLOR_HINT = 0xFF808080;

    private final List<RowHit> rowHits = new ArrayList<>();

    public QuestJournalPanelWidget()
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
        rowHits.clear();

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        Font font = minecraft.font;
        int width = Math.max(getSize().width, minecraft.getWindow().getGuiScaledWidth());
        int height = Math.max(getSize().height, minecraft.getWindow().getGuiScaledHeight());

        graphics.fill(0, 0, width, height, COLOR_BACKGROUND);
        graphics.fill(0, 0, width, 1, COLOR_BORDER);
        graphics.fill(0, height - 1, width, height, COLOR_BORDER);

        graphics.drawString(font, Component.translatable("gui.jujutsukaisenzone.quest_journal"),
                MARGIN, 12, COLOR_TITLE, false);
        graphics.drawString(font, Component.translatable("gui.jujutsukaisenzone.quest_journal.hint"),
                MARGIN, 26, COLOR_HINT, false);

        if (player == null)
        {
            return;
        }

        int y = 44;
        int rowWidth = width - MARGIN * 2;
        // 主线只显示当前环（可追踪 / 追踪中）：未解锁不剧透，已完成让位给下一环
        for (MainQuestDef def : MainQuestDefs.all())
        {
            MainQuestStatus status = PlayerQuestData.status(player, def);
            if (status == MainQuestStatus.LOCKED || status == MainQuestStatus.COMPLETED)
            {
                continue;
            }
            boolean hover = mouseX >= MARGIN && mouseX <= MARGIN + rowWidth
                    && mouseY >= y && mouseY <= y + ROW_HEIGHT;
            int rowColor = hover && isClickable(status) ? 0xFF243044 : 0xFF1A1F28;
            graphics.fill(MARGIN, y, MARGIN + rowWidth, y + ROW_HEIGHT, rowColor);
            graphics.fill(MARGIN, y, MARGIN + 3, y + ROW_HEIGHT, statusColor(status));

            graphics.drawString(font, Component.translatable(def.titleKey()),
                    MARGIN + 12, y + 8, 0xFFFFFFFF, false);
            graphics.drawString(font, Component.translatable(def.objectiveKey()),
                    MARGIN + 12, y + 22, 0xFFB0B0B0, false);
            int descY = y + 36;
            for (FormattedCharSequence line : font.split(Component.translatable(def.descriptionKey()), rowWidth - 24))
            {
                if (descY + font.lineHeight > y + ROW_HEIGHT - 4)
                {
                    break;
                }
                graphics.drawString(font, line, MARGIN + 12, descY, 0xFF9AA3AD, false);
                descY += font.lineHeight;
            }
            graphics.drawString(font, Component.translatable(statusLang(status)),
                    MARGIN + rowWidth - 72, y + 8, statusColor(status), false);

            rowHits.add(new RowHit(def.id(), status, MARGIN, y, MARGIN + rowWidth, y + ROW_HEIGHT));
            y += ROW_HEIGHT + ROW_GAP;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if (button != 0)
        {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        for (RowHit hit : rowHits)
        {
            if (mouseX < hit.left || mouseX > hit.right || mouseY < hit.top || mouseY > hit.bottom)
            {
                continue;
            }
            if (hit.status == MainQuestStatus.AVAILABLE)
            {
                NetworkSenders.toServer(new TrackQuestC2SPacket(hit.questId, true));
                return true;
            }
            if (hit.status == MainQuestStatus.TRACKED)
            {
                NetworkSenders.toServer(new TrackQuestC2SPacket(hit.questId, false));
                return true;
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static boolean isClickable(MainQuestStatus status)
    {
        return status == MainQuestStatus.AVAILABLE || status == MainQuestStatus.TRACKED;
    }

    private static int statusColor(MainQuestStatus status)
    {
        return switch (status)
        {
            case TRACKED -> 0xFFE8C56B;
            case AVAILABLE -> 0xFF7FE3FF;
            case COMPLETED -> 0xFF6FBF73;
            case LOCKED -> 0xFF6E7883;
        };
    }

    private static String statusLang(MainQuestStatus status)
    {
        return switch (status)
        {
            case TRACKED -> "gui.jujutsukaisenzone.quest.status.tracked";
            case AVAILABLE -> "gui.jujutsukaisenzone.quest.status.available";
            case COMPLETED -> "gui.jujutsukaisenzone.quest.status.completed";
            case LOCKED -> "gui.jujutsukaisenzone.quest.status.locked";
        };
    }

    private record RowHit(String questId, MainQuestStatus status, int left, int top, int right, int bottom)
    {
    }
}
