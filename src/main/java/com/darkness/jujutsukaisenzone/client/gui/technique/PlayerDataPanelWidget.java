package com.darkness.jujutsukaisenzone.client.gui.technique;

import com.darkness.jujutsukaisenzone.player.CombatPowerStats;
import com.darkness.jujutsukaisenzone.player.PlayerBarrierTechniqueData;
import com.darkness.jujutsukaisenzone.player.PlayerCombatPowerData;
import com.darkness.jujutsukaisenzone.player.PlayerCursedEnergyData;
import com.darkness.jujutsukaisenzone.player.PlayerCursedEnergyTraitData;
import com.darkness.jujutsukaisenzone.player.PlayerCursedGradeData;
import com.darkness.jujutsukaisenzone.player.PlayerBrainDamageData;
import com.darkness.jujutsukaisenzone.player.PlayerDomainGaugeData;
import com.darkness.jujutsukaisenzone.player.PlayerDomainExpansionData;
import com.darkness.jujutsukaisenzone.player.PlayerMeltdownRepairData;
import com.darkness.jujutsukaisenzone.Config;
import com.darkness.jujutsukaisenzone.player.PlayerFactionData;
import com.darkness.jujutsukaisenzone.player.PlayerHealthData;
import com.darkness.jujutsukaisenzone.player.PlayerReverseTechniqueData;
import com.darkness.jujutsukaisenzone.player.PlayerSeanceTechniqueData;
import com.darkness.jujutsukaisenzone.player.PlayerSpData;
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
 * 取值全部走客户端已有的同步缓存（{@code persistentData} / 已同步属性），因此这里只读、不请求服务端。
 * <p>
 * 两页布局（底部子标签切换）：
 * <ul>
 *   <li>概览 —— 左：身份 / 资源（含术式情报）；右：战斗强度 / 防御</li>
 *   <li>术式状态 —— 左：反转 / 领域相关；右：结界 / 降灵 / 强化等通用技能</li>
 * </ul>
 * 不做滚动；溢出时底部画省略号。
 * <p>
 * 坐标说明：本控件固定在 (0,0) 且铺满全屏，因此 {@code drawInBackground} 里的坐标
 * 无论 LDLib 是否按自身位置做位移，落在屏幕上的结果都一致。
 * <p>
 * 非显而易见的属性旁带感叹号说明（悬停查看）。
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
    private static final int PAGE_BAR_HEIGHT = 18;
    private static final int PAGE_TAB_GAP = 4;
    private static final int PAGE_TAB_PAD_X = 8;
    private static final int PAGE_COUNT = 2;
    private static final int PAGE_OVERVIEW = 0;
    private static final int PAGE_STATUS = 1;

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
    private static final int COLOR_PAGE_ACTIVE = 0xFF3A5A8A;
    private static final int COLOR_PAGE_IDLE = 0xFF2A2A2A;

    private int page = PAGE_OVERVIEW;
    /** 绘制时写入，供 mouseClicked 命中测试。 */
    private final int[] pageTabLeft = new int[PAGE_COUNT];
    private final int[] pageTabRight = new int[PAGE_COUNT];
    private int pageTabTop;
    private int pageTabBottom;
    private final List<PanelHelpIcon.Hit> helpHits = new ArrayList<>();

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
        int bottom = top + height - BOTTOM_PADDING - PAGE_BAR_HEIGHT;

        Font font = minecraft.font;
        if (page == PAGE_STATUS)
        {
            drawColumn(graphics, font, leftX, startY, contentWidth, bottom, mouseX, mouseY,
                    buildStatusLeftColumn(player));
            drawColumn(graphics, font, rightX, startY, contentWidth, bottom, mouseX, mouseY,
                    buildStatusRightColumn(player));
        }
        else
        {
            drawColumn(graphics, font, leftX, startY, contentWidth, bottom, mouseX, mouseY,
                    buildOverviewLeftColumn(player));
            drawColumn(graphics, font, rightX, startY, contentWidth, bottom, mouseX, mouseY,
                    buildOverviewRightColumn(player));
        }

        drawPageBar(graphics, font, left, top, width, height);

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
        if (button == 0 && pageTabBottom > pageTabTop)
        {
            for (int i = 0; i < PAGE_COUNT; i++)
            {
                if (mouseX >= pageTabLeft[i] && mouseX < pageTabRight[i]
                        && mouseY >= pageTabTop && mouseY < pageTabBottom)
                {
                    page = i;
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    // ======== 数据装配 ========

    private static List<Line> buildOverviewLeftColumn(LocalPlayer player)
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
        var talent = com.darkness.jujutsukaisenzone.talent.InnateTalentData.get(player);
        if (talent != null)
        {
            var source = com.darkness.jujutsukaisenzone.talent.InnateTalentData.getSource(player);
            String labelKey = source == com.darkness.jujutsukaisenzone.talent.InnateTalentSource.ACQUIRED
                    ? "acquired_talent"
                    : "innate_talent";
            lines.add(Line.rowHelp(tr(labelKey), talent.displayComponent().getString(),
                    helpKey(talent.id())));
        }
        lines.add(Line.rowHelp(tr("cursed_energy_trait"),
                PlayerCursedEnergyTraitData.getTrait(player).displayName().getString(),
                helpKey("cursed_energy_trait")));

        lines.add(Line.section(tr("section.resource")));
        lines.add(Line.row(tr("cursed_energy"),
                PlayerCursedEnergyData.getCurrent(player) + " / " + PlayerCursedEnergyData.getMax(player)));
        lines.add(Line.row(tr("health"), String.format(Locale.ROOT, "%.1f / %.1f", player.getHealth(),
                PlayerHealthData.getMax(player))));
        lines.add(Line.rowHelp(tr("sp"), Integer.toString(PlayerSpData.getAvailable(player)),
                helpKey("sp")));
        int brain = PlayerBrainDamageData.getBrainDamage(player);
        int maxBrain = PlayerBrainDamageData.getBrainMax(player);
        lines.add(Line.rowHelp(tr("brain_damage"), brain + " / " + maxBrain
                        + (brain >= maxBrain ? "（" + tr("value.brain_full") + "）" : ""),
                helpKey("brain_damage")));
        String innateForIntel = PlayerTechniqueData.getInnateTechnique(player);
        int intel = innateForIntel == null || innateForIntel.isBlank()
                ? 0
                : PlayerTechniqueIntelData.getIntel(player, innateForIntel);
        lines.add(Line.rowHelp(tr("intel"), Integer.toString(intel), COLOR_TECHNIQUE, helpKey("intel")));

        return lines;
    }

    private static List<Line> buildOverviewRightColumn(LocalPlayer player)
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
        var physicalPenetration = player.getAttribute(ModAttributes.PHYSICAL_PENETRATION.get());
        var techniquePenetration = player.getAttribute(ModAttributes.TECHNIQUE_PENETRATION.get());
        lines.add(Line.rowHelp(tr("physical_penetration"),
                formatPercent(physicalPenetration == null ? 0.0D : physicalPenetration.getValue()),
                COLOR_PHYSICAL, helpKey("physical_penetration")));
        lines.add(Line.rowHelp(tr("technique_penetration"),
                formatPercent(techniquePenetration == null ? 0.0D : techniquePenetration.getValue()),
                COLOR_TECHNIQUE, helpKey("technique_penetration")));
        var critChance = player.getAttribute(ModAttributes.CRIT_CHANCE.get());
        lines.add(Line.rowHelp(tr("crit_chance"),
                formatPercent(critChance == null ? 5.0D : critChance.getValue()),
                COLOR_PHYSICAL, helpKey("crit_chance")));
        // 破韧效力：作用于怪物韧性条，不专属体术也不专属术式，故不染色。
        var breakPotency = player.getAttribute(ModAttributes.BREAK_POTENCY.get());
        lines.add(Line.rowHelp(tr("break_potency"),
                formatPercent(breakPotency == null ? 0.0D : breakPotency.getValue()),
                helpKey("break_potency")));

        // 与 /jjkz data 对齐：护甲 / 双抗（属性均 setSyncable）；穿甲 / 术穿在战斗强度分区。
        lines.add(Line.section(tr("section.defense")));
        lines.add(Line.row(tr("armor"), Integer.toString(player.getArmorValue())));
        var physicalResistance = player.getAttribute(ModAttributes.PHYSICAL_RESISTANCE.get());
        var techniqueResistance = player.getAttribute(ModAttributes.TECHNIQUE_RESISTANCE.get());
        lines.add(Line.rowHelp(tr("physical_resistance"),
                formatPercent(physicalResistance == null ? 0.0D : physicalResistance.getValue()),
                COLOR_PHYSICAL, helpKey("physical_resistance")));
        lines.add(Line.rowHelp(tr("technique_resistance"),
                formatPercent(techniqueResistance == null ? 0.0D : techniqueResistance.getValue()),
                COLOR_TECHNIQUE, helpKey("technique_resistance")));

        return lines;
    }

    /** 术式状态左栏：反转 + 领域相关。 */
    private static List<Line> buildStatusLeftColumn(LocalPlayer player)
    {
        List<Line> lines = new ArrayList<>();
        lines.add(Line.section(tr("section.status")));
        lines.add(Line.row(tr("reverse_technique"), learnedText(PlayerReverseTechniqueData.hasReverseTechnique(player))));
        var reverseEfficiency = player.getAttribute(ModAttributes.REVERSE_TECHNIQUE_EFFICIENCY.get());
        lines.add(Line.rowHelp(tr("reverse_technique_efficiency"),
                formatPercent(reverseEfficiency == null ? 100.0D : reverseEfficiency.getValue()),
                helpKey("reverse_technique_efficiency")));
        lines.add(Line.row(tr("domain_expansion"), learnedText(PlayerDomainExpansionData.hasLearned(player))));
        lines.add(Line.row(tr("meltdown_repair"), learnedText(PlayerMeltdownRepairData.hasLearned(player))));
        int gauge = PlayerDomainGaugeData.getGauge(player);
        int gaugeMax = PlayerDomainGaugeData.getMax(player);
        lines.add(Line.rowHelp(tr("domain_gauge"), gauge + " / " + gaugeMax, helpKey("domain_gauge")));
        var domainChargeEfficiency = player.getAttribute(ModAttributes.DOMAIN_GAUGE_CHARGE_EFFICIENCY.get());
        lines.add(Line.rowHelp(tr("domain_gauge_charge_efficiency"),
                formatPercent(domainChargeEfficiency == null ? 100.0D : domainChargeEfficiency.getValue()),
                helpKey("domain_gauge_charge_efficiency")));
        return lines;
    }

    /** 术式状态右栏：其余通用技能等级。 */
    private static List<Line> buildStatusRightColumn(LocalPlayer player)
    {
        List<Line> lines = new ArrayList<>();
        lines.add(Line.section(tr("section.common_skills")));
        lines.add(Line.row(tr("barrier_level"), levelName(PlayerBarrierTechniqueData.getLevel(player))));
        lines.add(Line.row(tr("seance_level"), levelName(PlayerSeanceTechniqueData.getLevel(player))));
        lines.add(Line.row(tr("strengthen_level"), levelName(PlayerStrengthenTechniqueData.getLevel(player))
                + (PlayerStrengthenTechniqueData.isActive(player) ? "，" + tr("value.active") : "")));
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

    private void drawPageBar(GuiGraphics graphics, Font font, int left, int top, int width, int height)
    {
        pageTabTop = top + height - PAGE_BAR_HEIGHT;
        pageTabBottom = top + height - 2;
        int y = pageTabTop + 2;
        int tabHeight = pageTabBottom - pageTabTop - 2;
        String[] labels = {tr("page.overview"), tr("page.status")};

        int x = left + PADDING;
        for (int i = 0; i < PAGE_COUNT; i++)
        {
            String label = labels[i];
            int tabWidth = font.width(label) + PAGE_TAB_PAD_X * 2;
            pageTabLeft[i] = x;
            pageTabRight[i] = x + tabWidth;
            boolean active = page == i;
            graphics.fill(x, y, x + tabWidth, y + tabHeight,
                    active ? COLOR_PAGE_ACTIVE : COLOR_PAGE_IDLE);
            graphics.drawString(font, label, x + PAGE_TAB_PAD_X, y + Math.max(0, (tabHeight - 8) / 2),
                    COLOR_VALUE, false);
            x += tabWidth + PAGE_TAB_GAP;
        }
    }

    /**
     * 画一栏内容：先量出该栏最长标签（含感叹号）宽度作为值列起点，再逐行绘制。
     */
    private int drawColumn(GuiGraphics graphics, Font font, int x, int y, int maxWidth, int bottom,
                           int mouseX, int mouseY, List<Line> lines)
    {
        int labelWidth = 0;
        for (Line line : lines)
        {
            if (!line.section() && line.value() != null)
            {
                labelWidth = Math.max(labelWidth,
                        PanelHelpIcon.labelWithHelpWidth(font, line.label(), line.helpKey() != null) + 4);
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
            if (line.helpKey() != null)
            {
                PanelHelpIcon.drawAfterLabel(graphics, font, line.label(), x, y, mouseX, mouseY,
                        line.helpKey(), helpHits);
            }
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

    private static String helpKey(String id)
    {
        return "gui.jujutsukaisenzone.player_data.help." + id;
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

    /**
     * 一行内容：{@code section} 为真时是分节标题（{@code value} 为 null）。
     * {@code helpKey} 非空时在标签旁画感叹号。
     */
    private record Line(String label, String value, int valueColor, boolean section, String helpKey)
    {
        static Line section(String title)
        {
            return new Line(title, null, COLOR_VALUE, true, null);
        }

        static Line row(String label, String value)
        {
            return new Line(label, value, COLOR_VALUE, false, null);
        }

        static Line row(String label, String value, int valueColor)
        {
            return new Line(label, value, valueColor, false, null);
        }

        static Line rowHelp(String label, String value, String helpKey)
        {
            return new Line(label, value, COLOR_VALUE, false, helpKey);
        }

        static Line rowHelp(String label, String value, int valueColor, String helpKey)
        {
            return new Line(label, value, valueColor, false, helpKey);
        }
    }
}
