package com.darkness.jujutsukaisenzone.client.gui.technique;

import com.darkness.jujutsukaisenzone.player.CombatPowerStats;
import com.darkness.jujutsukaisenzone.player.PlayerBarrierTechniqueData;
import com.darkness.jujutsukaisenzone.player.PlayerCombatPowerData;
import com.darkness.jujutsukaisenzone.player.PlayerCursedEnergyData;
import com.darkness.jujutsukaisenzone.player.PlayerCursedGradeData;
import com.darkness.jujutsukaisenzone.player.PlayerDomainExpansionData;
import com.darkness.jujutsukaisenzone.player.PlayerFactionData;
import com.darkness.jujutsukaisenzone.player.PlayerHealthData;
import com.darkness.jujutsukaisenzone.player.PlayerReverseTechniqueData;
import com.darkness.jujutsukaisenzone.player.PlayerSeanceTechniqueData;
import com.darkness.jujutsukaisenzone.player.PlayerStrengthenTechniqueData;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueIntelData;
import com.darkness.jujutsukaisenzone.registry.ModAttributes;
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
import java.util.Locale;

/**
 * 「玩家数据」标签页面板：把 {@code /jjkz data} 与 {@code /jjkz stats} 的输出搬进 GUI。
 * <p>
 * 取值全部走客户端已有的同步缓存（{@code persistentData} / 已同步属性），因此这里只读、不请求服务端：
 * 咒力 / 咒术经验 / 阵营 / 生得术式 / 反转术式 / 领域展开 / 结界·降灵·强化术等级 /
 * 生命上限 / 天与咒缚 / 战斗强度面板 / 护甲与体术·术式抗性，均由既有同步刷新。
 * <p>
 * ⚠️ 唯一没有搬进来的是<strong>罪行记录</strong>：它只存在服务端存档里、没有任何同步包，
 * 要显示得另加一对请求 / 响应包。
 * <p>
 * 布局固定为两栏（左：身份 / 资源 / 术式状态；右：战斗强度 / 防御 / 术式情报），不做滚动——
 * 当前行数在最小分辨率下也放得下；真的溢出时会在底部画省略号。
 * 防御行（护甲 / 体抗 / 术抗）与 {@code /jjkz data} 对齐；抗性属性已 {@code setSyncable}，客户端直接读。
 * <p>
 * 坐标说明：本控件固定在 (0,0) 且铺满全屏，因此 {@code drawInBackground} 里的坐标
 * 无论 LDLib 是否按自身位置做位移，落在屏幕上的结果都一致。
 */
@OnlyIn(Dist.CLIENT)
public final class PlayerDataPanelWidget extends Widget
{
    private static final int MARGIN = 8;
    private static final int TOP = 60;
    private static final int PADDING = 10;
    private static final int COLUMN_GAP = 14;
    private static final int LINE_HEIGHT = 11;
    private static final int SECTION_TOP_GAP = 5;
    private static final int BOTTOM_PADDING = 6;

    private static final int COLOR_BACKGROUND = 0xCC12161C;
    private static final int COLOR_BORDER = 0xFF3A5A8A;
    private static final int COLOR_SECTION = 0xFF7FE3FF;
    private static final int COLOR_SECTION_UNDERLINE = 0x667FE3FF;
    private static final int COLOR_LABEL = 0xFF9AA4B0;
    private static final int COLOR_VALUE = 0xFFEDEDED;
    private static final int COLOR_SUB_VALUE = 0xFFB9C2CC;
    private static final int COLOR_PHYSICAL = 0xFFFFB37A;
    private static final int COLOR_TECHNIQUE = 0xFF8FD4FF;
    private static final int COLOR_HINT = 0xFF6E7883;

    public PlayerDataPanelWidget()
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
            // onScreenSizeUpdate 尚未触发时的兜底（例如 open() 当帧就被绘制）。
            screenWidth = minecraft.getWindow().getGuiScaledWidth();
            screenHeight = minecraft.getWindow().getGuiScaledHeight();
        }

        int left = MARGIN;
        int top = TOP;
        int width = Math.max(100, screenWidth - MARGIN * 2);
        int height = Math.max(40, screenHeight - TOP - MARGIN);
        drawFrame(graphics, left, top, width, height);

        int columnWidth = Math.max(80, (width - COLUMN_GAP) / 2);
        int leftX = left + PADDING;
        int rightX = left + columnWidth + COLUMN_GAP + PADDING;
        int contentWidth = columnWidth - PADDING * 2;
        int startY = top + PADDING;
        int bottom = top + height - BOTTOM_PADDING;

        Font font = minecraft.font;
        drawColumn(graphics, font, leftX, startY, contentWidth, bottom, buildLeftColumn(player));
        drawColumn(graphics, font, rightX, startY, contentWidth, bottom, buildRightColumn(player));
    }

    // ======== 数据装配 ========

    private static List<Line> buildLeftColumn(LocalPlayer player)
    {
        List<Line> lines = new ArrayList<>();

        lines.add(Line.section(tr("section.identity")));
        String factionId = PlayerFactionData.getFactionId(player);
        lines.add(Line.row(tr("faction"), factionId == null || factionId.isEmpty()
                ? tr("value.none")
                : PlayerFactionData.getDisplayName(factionId) + " (" + factionId + ")"));
        String innateTechnique = PlayerTechniqueData.getInnateTechnique(player);
        lines.add(Line.row(tr("innate_technique"),
                innateTechnique == null || innateTechnique.isBlank()
                        || PlayerTechniqueData.TECHNIQUE_NONE.equals(innateTechnique)
                        ? tr("value.none")
                        : innateTechnique));
        int rank = PlayerCursedGradeData.getRank(player);
        lines.add(Line.row(tr("cursed_grade"), PlayerCursedGradeData.getRankDisplayName(rank)));
        lines.add(Line.row(tr("cursed_experience"), Integer.toString(PlayerCursedGradeData.getExperience(player))));
        lines.add(Line.row(tr("heavenly_restriction"), yesNo(PlayerCursedEnergyData.isHeavenlyRestricted(player))));

        lines.add(Line.section(tr("section.resource")));
        lines.add(Line.row(tr("cursed_energy"),
                PlayerCursedEnergyData.getCurrent(player) + " / " + PlayerCursedEnergyData.getMax(player)));
        lines.add(Line.row(tr("health"), String.format(Locale.ROOT, "%.1f / %.1f", player.getHealth(),
                PlayerHealthData.getMax(player))));

        lines.add(Line.section(tr("section.status")));
        lines.add(Line.row(tr("reverse_technique"), learnedText(PlayerReverseTechniqueData.hasReverseTechnique(player))));
        lines.add(Line.row(tr("domain_expansion"), learnedText(PlayerDomainExpansionData.hasLearned(player))));
        lines.add(Line.row(tr("barrier_level"), levelName(PlayerBarrierTechniqueData.getLevel(player))));
        lines.add(Line.row(tr("seance_level"), levelName(PlayerSeanceTechniqueData.getLevel(player))));
        lines.add(Line.row(tr("strengthen_level"), levelName(PlayerStrengthenTechniqueData.getLevel(player))
                + (PlayerStrengthenTechniqueData.isActive(player) ? "，" + tr("value.active") : "")));

        return lines;
    }

    private static List<Line> buildRightColumn(LocalPlayer player)
    {
        List<Line> lines = new ArrayList<>();
        lines.add(Line.section(tr("section.power")));

        CombatPowerStats stats = PlayerCombatPowerData.resolve(player);
        if (!stats.resolved())
        {
            lines.add(Line.row(tr("physical_power"), tr("value.unsynced"), COLOR_HINT));
            lines.add(Line.row(tr("technique_power"), tr("value.unsynced"), COLOR_HINT));
        }
        else
        {
            lines.add(Line.row(tr("physical_power"), Integer.toString(stats.physicalPower()), COLOR_PHYSICAL));
            for (CombatPowerStats.Contribution contribution : stats.physicalBreakdown())
            {
                lines.add(Line.row("  · " + contribution.label(),
                        Integer.toString(contribution.amount()), COLOR_SUB_VALUE));
            }
            lines.add(Line.row(tr("technique_power"), Integer.toString(stats.techniquePower()), COLOR_TECHNIQUE));
            for (CombatPowerStats.Contribution contribution : stats.techniqueBreakdown())
            {
                lines.add(Line.row("  · " + contribution.label(),
                        Integer.toString(contribution.amount()), COLOR_SUB_VALUE));
            }
        }

        // 与 /jjkz data 对齐：护甲 / 双抗 / 穿甲 / 术穿（属性均 setSyncable）。
        lines.add(Line.section(tr("section.defense")));
        lines.add(Line.row(tr("armor"), Integer.toString(player.getArmorValue())));
        var physicalResistance = player.getAttribute(ModAttributes.PHYSICAL_RESISTANCE.get());
        var techniqueResistance = player.getAttribute(ModAttributes.TECHNIQUE_RESISTANCE.get());
        var physicalPenetration = player.getAttribute(ModAttributes.PHYSICAL_PENETRATION.get());
        var techniquePenetration = player.getAttribute(ModAttributes.TECHNIQUE_PENETRATION.get());
        lines.add(Line.row(tr("physical_resistance"),
                formatPercent(physicalResistance == null ? 0.0D : physicalResistance.getValue()),
                COLOR_PHYSICAL));
        lines.add(Line.row(tr("technique_resistance"),
                formatPercent(techniqueResistance == null ? 0.0D : techniqueResistance.getValue()),
                COLOR_TECHNIQUE));
        lines.add(Line.row(tr("physical_penetration"),
                formatPercent(physicalPenetration == null ? 0.0D : physicalPenetration.getValue()),
                COLOR_PHYSICAL));
        lines.add(Line.row(tr("technique_penetration"),
                formatPercent(techniquePenetration == null ? 0.0D : techniquePenetration.getValue()),
                COLOR_TECHNIQUE));

        lines.add(Line.section(tr("section.intel")));
        String innateTechnique = PlayerTechniqueData.getInnateTechnique(player);
        int intel = innateTechnique == null || innateTechnique.isBlank()
                ? 0
                : PlayerTechniqueIntelData.getIntel(player, innateTechnique);
        lines.add(Line.row(tr("intel"), Integer.toString(intel), COLOR_TECHNIQUE));

        return lines;
    }

    /** 抗性属性单位是百分比点数（25 = 25%），展示成一位小数加 %。 */
    private static String formatPercent(double points)
    {
        return String.format(Locale.ROOT, "%.1f%%", points);
    }

    // ======== 绘制 ========

    private static void drawFrame(GuiGraphics graphics, int left, int top, int width, int height)
    {
        graphics.fill(left, top, left + width, top + height, COLOR_BACKGROUND);
        graphics.fill(left, top, left + width, top + 1, COLOR_BORDER);
        graphics.fill(left, top + height - 1, left + width, top + height, COLOR_BORDER);
        graphics.fill(left, top, left + 1, top + height, COLOR_BORDER);
        graphics.fill(left + width - 1, top, left + width, top + height, COLOR_BORDER);
    }

    /**
     * 画一栏内容：先量出该栏最长标签宽度作为值列起点，再逐行绘制。
     *
     * @return 结束时的 y（未使用，便于后续扩展）
     */
    private static int drawColumn(GuiGraphics graphics, Font font, int x, int y, int maxWidth, int bottom,
                                  List<Line> lines)
    {
        int labelWidth = 0;
        for (Line line : lines)
        {
            if (!line.section() && line.value() != null)
            {
                labelWidth = Math.max(labelWidth, font.width(line.label()) + 4);
            }
        }
        // 值列最多占一半宽，避免长标签把值挤出面板。
        labelWidth = Math.min(labelWidth, Math.max(0, maxWidth / 2));

        int startY = y;
        for (Line line : lines)
        {
            if (y + LINE_HEIGHT > bottom)
            {
                graphics.drawString(font, "…", x, y, COLOR_HINT, false);
                return y;
            }
            if (line.section())
            {
                if (y > startY)
                {
                    y += SECTION_TOP_GAP;
                }
                graphics.drawString(font, line.label(), x, y, COLOR_SECTION, false);
                graphics.fill(x, y + 9, x + font.width(line.label()), y + 10, COLOR_SECTION_UNDERLINE);
                y += LINE_HEIGHT;
                continue;
            }
            graphics.drawString(font, line.label(), x, y, COLOR_LABEL, false);
            if (line.value() != null)
            {
                int valueX = x + labelWidth;
                graphics.drawString(font, ellipsize(font, line.value(), x + maxWidth - valueX),
                        valueX, y, line.valueColor(), false);
            }
            y += LINE_HEIGHT;
        }
        return y;
    }

    private static String ellipsize(Font font, String text, int maxWidth)
    {
        if (maxWidth <= 0 || font.width(text) <= maxWidth)
        {
            return text;
        }
        String result = text;
        while (result.length() > 1 && font.width(result + "…") > maxWidth)
        {
            result = result.substring(0, result.length() - 1);
        }
        return result + "…";
    }

    // ======== 文案 ========

    private static String tr(String path)
    {
        return Component.translatable("gui.jujutsukaisenzone.player_data." + path).getString();
    }

    private static String yesNo(boolean value)
    {
        return tr(value ? "value.yes" : "value.no");
    }

    private static String learnedText(boolean learned)
    {
        return tr(learned ? "value.learned" : "value.not_learned");
    }

    /** 结界 / 降灵 / 强化术三个等级枚举同名（UNLEARNED / BEGINNER / MASTERED / COMPLETE），共用一套文案。 */
    private static String levelName(Enum<?> level)
    {
        return tr("level." + level.name().toLowerCase(Locale.ROOT));
    }

    /** 一行内容：{@code section} 为真时是分节标题（{@code value} 为 null）。 */
    private record Line(String label, String value, int valueColor, boolean section)
    {
        static Line section(String title)
        {
            return new Line(title, null, COLOR_VALUE, true);
        }

        static Line row(String label, String value)
        {
            return new Line(label, value, COLOR_VALUE, false);
        }

        static Line row(String label, String value, int valueColor)
        {
            return new Line(label, value, valueColor, false);
        }
    }
}
