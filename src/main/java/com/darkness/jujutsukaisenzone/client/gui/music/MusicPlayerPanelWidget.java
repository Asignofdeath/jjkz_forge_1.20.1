package com.darkness.jujutsukaisenzone.client.gui.music;

import com.darkness.jujutsukaisenzone.client.music.JjkzMusicDirector;
import com.darkness.jujutsukaisenzone.music.ModMusicTrack;
import com.darkness.jujutsukaisenzone.music.ModMusicTracks;
import com.darkness.jujutsukaisenzone.music.MusicPriority;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.lowdraglib.utils.Size;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 音乐鉴赏列表：点歌仅 APPRECIATION；被剧情/战斗占用时不可开播。
 */
@OnlyIn(Dist.CLIENT)
public final class MusicPlayerPanelWidget extends Widget
{
    private static final int MARGIN = 16;
    private static final int ROW_HEIGHT = 44;
    private static final int ROW_GAP = 4;
    private static final int COLOR_BACKGROUND = 0xE812161C;
    private static final int COLOR_BORDER = 0xFF3A5A8A;
    private static final int COLOR_TITLE = 0xFFE8C56B;
    private static final int COLOR_HINT = 0xFF808080;

    private final List<RowHit> rowHits = new ArrayList<>();

    public MusicPlayerPanelWidget()
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
        Font font = minecraft.font;
        int width = Math.max(getSize().width, minecraft.getWindow().getGuiScaledWidth());
        int height = Math.max(getSize().height, minecraft.getWindow().getGuiScaledHeight());

        graphics.fill(0, 0, width, height, COLOR_BACKGROUND);
        graphics.fill(0, 0, width, 1, COLOR_BORDER);
        graphics.fill(0, height - 1, width, height, COLOR_BORDER);

        graphics.drawString(font, Component.translatable("gui.jujutsukaisenzone.music_player"),
                MARGIN, 12, COLOR_TITLE, false);
        graphics.drawString(font, Component.translatable("gui.jujutsukaisenzone.music_player.hint"),
                MARGIN, 26, COLOR_HINT, false);

        if (ModMusicTracks.isEmpty())
        {
            graphics.drawString(font, Component.translatable("gui.jujutsukaisenzone.music_player.empty"),
                    MARGIN, 48, COLOR_HINT, false);
            return;
        }

        boolean canPlay = JjkzMusicDirector.canStartAppreciation();
        if (!canPlay)
        {
            graphics.drawString(font, Component.translatable("gui.jujutsukaisenzone.music_player.busy"),
                    MARGIN, 40, 0xFFE08A8A, false);
        }

        int y = canPlay ? 44 : 56;
        int rowWidth = width - MARGIN * 2;
        for (ModMusicTrack track : ModMusicTracks.all())
        {
            boolean playing = JjkzMusicDirector.isPlayingAppreciation(track.id());
            boolean hover = mouseX >= MARGIN && mouseX <= MARGIN + rowWidth
                    && mouseY >= y && mouseY <= y + ROW_HEIGHT;
            int rowColor = hover && canPlay ? 0xFF243044 : 0xFF1A1F28;
            if (playing)
            {
                rowColor = 0xFF2A3A28;
            }
            graphics.fill(MARGIN, y, MARGIN + rowWidth, y + ROW_HEIGHT, rowColor);
            graphics.fill(MARGIN, y, MARGIN + 3, y + ROW_HEIGHT, playing ? 0xFF6FBF73 : 0xFF3A5A8A);

            graphics.drawString(font, track.title(), MARGIN + 12, y + 8, 0xFFFFFFFF, false);
            graphics.drawString(font, track.source(), MARGIN + 12, y + 22, 0xFF9AA3AD, false);

            Component status = playing
                    ? Component.translatable("gui.jujutsukaisenzone.music_player.playing")
                    : (canPlay
                            ? Component.translatable("gui.jujutsukaisenzone.music_player.play")
                            : Component.translatable("gui.jujutsukaisenzone.music_player.locked"));
            int statusColor = playing ? 0xFF6FBF73 : (canPlay ? 0xFF7FE3FF : 0xFF6E7883);
            graphics.drawString(font, status, MARGIN + rowWidth - 56, y + 14, statusColor, false);

            rowHits.add(new RowHit(track.id(), MARGIN, y, MARGIN + rowWidth, y + ROW_HEIGHT));
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
            if (JjkzMusicDirector.isPlayingAppreciation(hit.trackId))
            {
                JjkzMusicDirector.stopAppreciationNow();
                return true;
            }
            if (!JjkzMusicDirector.canStartAppreciation())
            {
                return true;
            }
            JjkzMusicDirector.play(hit.trackId, MusicPriority.APPRECIATION, false, 200);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private record RowHit(String trackId, int left, int top, int right, int bottom)
    {
    }
}
