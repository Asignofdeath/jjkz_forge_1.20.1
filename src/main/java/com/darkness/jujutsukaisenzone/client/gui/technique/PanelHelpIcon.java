package com.darkness.jujutsukaisenzone.client.gui.technique;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * 面板感叹号说明：画在标签旁，悬停显示玩家向短文案。
 */
@OnlyIn(Dist.CLIENT)
public final class PanelHelpIcon
{
    public static final int SIZE = 8;
    private static final int GAP = 3;
    private static final int COLOR_BG = 0xFF2A3240;
    private static final int COLOR_BG_HOVER = 0xFF3A5A8A;
    private static final int COLOR_MARK = 0xFFE8C96A;
    private static final int COLOR_MARK_HOVER = 0xFFFFF0B0;

    private PanelHelpIcon()
    {
    }

    /**
     * 在标签右侧画 {@code !}，并把命中盒写入 {@code hitsOut}。
     *
     * @return 图标右缘 x（便于后续布局）
     */
    public static int drawAfterLabel(
            GuiGraphics graphics,
            Font font,
            String label,
            int labelX,
            int labelY,
            int mouseX,
            int mouseY,
            String helpLangKey,
            List<Hit> hitsOut)
    {
        int ix = labelX + font.width(label) + GAP;
        int iy = labelY;
        boolean hover = mouseX >= ix && mouseX < ix + SIZE && mouseY >= iy && mouseY < iy + SIZE;
        graphics.fill(ix, iy, ix + SIZE, iy + SIZE, hover ? COLOR_BG_HOVER : COLOR_BG);
        graphics.drawString(font, "!", ix + 2, iy, hover ? COLOR_MARK_HOVER : COLOR_MARK, false);
        hitsOut.add(new Hit(ix, iy, ix + SIZE, iy + SIZE, helpLangKey));
        return ix + SIZE;
    }

    /** 标签 + 可选感叹号占用的总宽度（含间距）。 */
    public static int labelWithHelpWidth(Font font, String label, boolean hasHelp)
    {
        int width = font.width(label);
        return hasHelp ? width + GAP + SIZE : width;
    }

    public static String findHoverKey(List<Hit> hits, double mouseX, double mouseY)
    {
        for (Hit hit : hits)
        {
            if (mouseX >= hit.left && mouseX < hit.right && mouseY >= hit.top && mouseY < hit.bottom)
            {
                return hit.langKey;
            }
        }
        return null;
    }

    /** 把 lang 文案按 {@code \\n} 拆成多行灰字 Component。 */
    public static List<Component> tooltipLines(String langKey)
    {
        String text = Component.translatable(langKey).getString();
        List<Component> lines = new ArrayList<>();
        for (String part : text.split("\n", -1))
        {
            if (part.isEmpty())
            {
                continue;
            }
            lines.add(Component.literal(part).withStyle(ChatFormatting.GRAY));
        }
        if (lines.isEmpty())
        {
            lines.add(Component.translatable(langKey).withStyle(ChatFormatting.GRAY));
        }
        return lines;
    }

    public record Hit(int left, int top, int right, int bottom, String langKey)
    {
    }
}
