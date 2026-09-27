package com.darkness.jujutsukaisenzone.client.gui.technique;

import com.darkness.jujutsukaisenzone.item.CursedObjectItem;
import com.darkness.jujutsukaisenzone.item.CursedObjectSeTrait;
import com.darkness.jujutsukaisenzone.network.ModNetwork;
import com.darkness.jujutsukaisenzone.player.CursedEnergyTrait;
import com.darkness.jujutsukaisenzone.player.PlayerCursedEnergyTraitData;
import com.darkness.jujutsukaisenzone.player.PlayerQualityPoolData;
import com.darkness.jujutsukaisenzone.player.PlayerSeLoadoutData;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import com.darkness.jujutsukaisenzone.player.SeEquippedEntry;
import com.darkness.jujutsukaisenzone.technique.se.PanelStat;
import com.darkness.jujutsukaisenzone.technique.se.SeEffectDef;
import com.darkness.jujutsukaisenzone.technique.se.SeEffectRegistry;
import com.darkness.jujutsukaisenzone.technique.se.SeMechanicRegistry;
import com.darkness.jujutsukaisenzone.technique.se.SePenalty;
import com.darkness.jujutsukaisenzone.technique.se.SeScope;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.lowdraglib.utils.Size;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * SE（术式侵入）面板。
 * <p>
 * 左栏：咒力特质 + 质量条 + 已装载列表。
 * 右栏：可侵入咒物目录；每项可从背包<strong>已探查</strong>实例中切换选件后再侵入。
 */
@OnlyIn(Dist.CLIENT)
public final class SeLoadoutPanelWidget extends Widget
{
    private static final int MARGIN = 8;
    private static final int TOP = 60;
    private static final int PADDING = 10;
    private static final int LINE_HEIGHT = 12;
    private static final int ROW_PAD = 3;
    private static final int COLUMN_GAP = 14;
    private static final int BAR_HEIGHT = 6;

    private static final int COLOR_BACKGROUND = 0xCC12161C;
    private static final int COLOR_BORDER = 0xFF3A5A8A;
    private static final int COLOR_SECTION = 0xFF7FE3FF;
    private static final int COLOR_LABEL = 0xFF9AA4B0;
    private static final int COLOR_VALUE = 0xFFEDEDED;
    private static final int COLOR_HINT = 0xFF6E7883;
    private static final int COLOR_ROW_IDLE = 0xFF1A2230;
    private static final int COLOR_ROW_HOVER = 0xFF2A3A55;
    private static final int COLOR_ACTIVE = 0xFF4CAF70;
    private static final int COLOR_MISSING = 0xFF8A6060;
    private static final int COLOR_BAR_TRACK = 0xFF202A38;
    private static final int COLOR_BAR_NORMAL = 0xFF3A6A9A;
    private static final int COLOR_BAR_ALLOWANCE = 0xFFC8A048;
    private static final int COLOR_BAR_OVERLOAD = 0xFFC04848;

    private static final String HELP_LANG_KEY = "gui.jujutsukaisenzone.se.help";

    private final List<Hit> hits = new ArrayList<>();
    private final List<PanelHelpIcon.Hit> helpHits = new ArrayList<>();
    /** seId → 当前选中的背包槽（已探查实例）。 */
    private final Map<String, Integer> selectedSlotBySeId = new HashMap<>();
    @Nullable
    private HoverTarget hoverTarget;

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
        helpHits.clear();
        hoverTarget = null;

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
        int columnWidth = Math.max(80, (width - COLUMN_GAP) / 2);
        int leftX = left + PADDING;
        int rightX = left + columnWidth + COLUMN_GAP + PADDING;
        int contentW = columnWidth - PADDING * 2;
        int bottom = top + height - PADDING;

        drawLeftColumn(graphics, font, player, leftX, contentW, top + PADDING, mouseX, mouseY);
        drawRightColumn(graphics, font, player, rightX, contentW, top + PADDING, bottom, mouseX, mouseY);

        String hoverKey = PanelHelpIcon.findHoverKey(helpHits, mouseX, mouseY);
        if (hoverKey != null)
        {
            setHoverTooltips(PanelHelpIcon.tooltipLines(hoverKey));
        }
        else if (hoverTarget != null)
        {
            setHoverTooltips(buildSeTooltip(player, hoverTarget));
        }
        else
        {
            setHoverTooltips(List.of());
        }
    }

    private int drawLeftColumn(GuiGraphics graphics, Font font, LocalPlayer player,
                               int x, int contentW, int top, int mouseX, int mouseY)
    {
        int y = top;
        String traitSection = tr("section.trait");
        graphics.drawString(font, traitSection, x, y, COLOR_SECTION, false);
        PanelHelpIcon.drawAfterLabel(graphics, font, traitSection, x, y, mouseX, mouseY,
                HELP_LANG_KEY, helpHits);
        y += LINE_HEIGHT + 2;
        CursedEnergyTrait trait = PlayerCursedEnergyTraitData.getTrait(player);
        // ⚠️ 参数要传 Component 而不是 translationKey 字符串：String 参数会被当成字面量，
        // 界面上就会直接显示成 "gui.jujutsukaisenzone.trait.refined"。
        graphics.drawString(font, tr("trait.value", Component.translatable(trait.translationKey())),
                x, y, COLOR_VALUE, false);
        y += LINE_HEIGHT + 6;

        int cap = PlayerQualityPoolData.cap(player);
        int allowance = PlayerQualityPoolData.allowance();
        int occupied = PlayerQualityPoolData.occupied(player);
        int overload = PlayerQualityPoolData.overload(player);
        graphics.drawString(font, tr("quality.value",
                occupied, cap, allowance), x, y, COLOR_LABEL, false);
        if (overload > 0)
        {
            String warn = tr("quality.overload", overload);
            graphics.drawString(font, warn, x + contentW - font.width(warn), y, COLOR_BAR_OVERLOAD, false);
        }
        y += LINE_HEIGHT + 2;
        drawQualityBar(graphics, x, y, contentW, occupied, cap, allowance);
        y += BAR_HEIGHT + 6;

        graphics.drawString(font, tr("section.equipped"), x, y, COLOR_SECTION, false);
        y += LINE_HEIGHT + 2;

        List<SeEffectDef> equipped = PlayerSeLoadoutData.getAllEquipped(player);
        if (equipped.isEmpty())
        {
            graphics.drawString(font, tr("equipped.empty"), x, y, COLOR_HINT, false);
            y += LINE_HEIGHT + 4;
        }
        else
        {
            for (SeEffectDef def : equipped)
            {
                int rowHeight = LINE_HEIGHT * 2 + ROW_PAD * 2;
                boolean hover = mouseX >= x && mouseX < x + contentW && mouseY >= y && mouseY < y + rowHeight;
                graphics.fill(x, y, x + contentW, y + rowHeight, hover ? COLOR_ROW_HOVER : COLOR_ROW_IDLE);
                graphics.drawString(font, def.item().getDescription().getString(), x + 4, y + ROW_PAD,
                        COLOR_ACTIVE, false);
                var penaltyOpt = PlayerSeLoadoutData.getEquippedPenalty(player, def.seId());
                String penaltyLine = penaltyOpt
                        .map(p -> tr("penalty.line", def.quality(),
                                p.stat().displayName().getString(), formatPenaltySigned(p)))
                        .orElse(tr("penalty.line", def.quality(), "?", "0"));
                graphics.drawString(font, penaltyLine, x + 4, y + ROW_PAD + LINE_HEIGHT, COLOR_HINT, false);
                hits.add(new Hit(x, y, x + contentW, y + rowHeight, HitKind.UNEQUIP, def.seId(), -1));
                if (hover)
                {
                    hoverTarget = new HoverTarget(def.seId(), true);
                }
                y += rowHeight + 2;
            }
        }
        graphics.drawString(font, tr("hint.equipped"), x, y + 2, COLOR_HINT, false);
        return y;
    }

    private void drawQualityBar(GuiGraphics graphics, int x, int y, int width, int occupied, int cap, int allowance)
    {
        int hardLimit = cap + allowance;
        graphics.fill(x, y, x + width, y + BAR_HEIGHT, COLOR_BAR_TRACK);
        if (hardLimit <= 0 || width <= 0)
        {
            return;
        }
        // 左段 = 上限内，中段 = 宽限内，右端溢出 = 超载（红）。
        int normalEnd = x + Math.round((float) width * Math.min(occupied, cap) / hardLimit);
        if (occupied > 0)
        {
            graphics.fill(x, y, Math.max(x, normalEnd), y + BAR_HEIGHT, COLOR_BAR_NORMAL);
        }
        if (occupied > cap)
        {
            int allowanceEnd = x + Math.round(
                    (float) width * Math.min(occupied, hardLimit) / hardLimit);
            graphics.fill(Math.max(x, normalEnd), y, Math.max(Math.max(x, normalEnd), allowanceEnd),
                    y + BAR_HEIGHT, COLOR_BAR_ALLOWANCE);
        }
        if (occupied > hardLimit)
        {
            graphics.fill(x + width - 2, y, x + width, y + BAR_HEIGHT, COLOR_BAR_OVERLOAD);
        }
        // 硬顶刻度线
        graphics.fill(x + width - 1, y, x + width, y + BAR_HEIGHT, COLOR_BORDER);
    }

    private void drawRightColumn(GuiGraphics graphics, Font font, LocalPlayer player,
                                 int x, int contentW, int top, int bottom, int mouseX, int mouseY)
    {
        int y = top;
        graphics.drawString(font, tr("section.catalog"), x, y, COLOR_SECTION, false);
        y += LINE_HEIGHT + 4;

        Set<String> ownedTechniques = Set.of(PlayerTechniqueData.getInnateTechnique(player));
        for (SeEffectDef def : SeEffectRegistry.all())
        {
            int rowHeight = LINE_HEIGHT * 4 + ROW_PAD * 2;
            if (y + rowHeight > bottom)
            {
                graphics.drawString(font, "…", x, y, COLOR_HINT, false);
                break;
            }
            List<Integer> probedSlots = PlayerSeLoadoutData.listProbedSlots(player, def.item());
            int selectedSlot = resolveSelectedSlot(player, def, probedSlots);
            boolean active = PlayerSeLoadoutData.isSeEquipped(player, def.seId());
            boolean hasItem = hasItem(player, def.item());
            boolean hasProbed = !probedSlots.isEmpty();
            boolean slotOk = PlayerSeLoadoutData.isValidEquipSlot(player, def.item(), selectedSlot);
            boolean scopeOk = def.scope().matches(ownedTechniques);
            boolean narrowOk = !SeMechanicRegistry.hasNarrowInterfaceConflict(
                    PlayerSeLoadoutData.getEquippedIds(player), def.seId());
            boolean qualityOk = !PlayerQualityPoolData.wouldExceedHardLimit(player, def.quality());
            boolean loadable = !active && slotOk && scopeOk && narrowOk && qualityOk;

            boolean hover = mouseX >= x && mouseX < x + contentW && mouseY >= y && mouseY < y + rowHeight;
            graphics.fill(x, y, x + contentW, y + rowHeight, hover ? COLOR_ROW_HOVER : COLOR_ROW_IDLE);

            int nameColor = active ? COLOR_ACTIVE : (loadable ? COLOR_VALUE : COLOR_MISSING);
            String name = def.item().getDescription().getString();
            graphics.drawString(font, name, x + 4, y + ROW_PAD, nameColor, false);

            String status = statusText(active, narrowOk, slotOk, scopeOk, hasItem, hasProbed, qualityOk);
            graphics.drawString(font, status, x + contentW - font.width(status) - 4, y + ROW_PAD,
                    loadable ? COLOR_ACTIVE : COLOR_HINT, false);

            String effect = SeMechanicRegistry.find(def.seId())
                    .map(m -> m.effectDescription().getString())
                    .orElseGet(() -> tr("effect.fallback", def.seId()));
            graphics.drawString(font, effect, x + 4, y + ROW_PAD + LINE_HEIGHT, COLOR_HINT, false);

            String requirement = tr("requirement", traitText(),
                    scopeText(def), def.quality());
            graphics.drawString(font, requirement, x + 4, y + ROW_PAD + LINE_HEIGHT * 2, COLOR_HINT, false);

            int selY = y + ROW_PAD + LINE_HEIGHT * 3;
            int btnW = 14;
            String selLabel = formatSelectionLabel(player, selectedSlot, probedSlots.size());
            graphics.drawString(font, "<", x + 4, selY, COLOR_VALUE, false);
            graphics.drawString(font, selLabel, x + 4 + btnW, selY, COLOR_HINT, false);
            graphics.drawString(font, ">", x + contentW - 4 - font.width(">"), selY, COLOR_VALUE, false);

            hits.add(new Hit(x + 4, selY, x + 4 + btnW, selY + LINE_HEIGHT,
                    HitKind.CYCLE_PREV, def.seId(), -1));
            hits.add(new Hit(x + contentW - 4 - btnW, selY, x + contentW - 4, selY + LINE_HEIGHT,
                    HitKind.CYCLE_NEXT, def.seId(), -1));
            if (loadable)
            {
                hits.add(new Hit(x, y, x + contentW, selY, HitKind.EQUIP, def.seId(), selectedSlot));
            }
            if (hover)
            {
                hoverTarget = new HoverTarget(def.seId(), false);
            }
            y += rowHeight + 2;
        }
    }

    private int resolveSelectedSlot(LocalPlayer player, SeEffectDef def, List<Integer> probedSlots)
    {
        if (probedSlots.isEmpty())
        {
            selectedSlotBySeId.remove(def.seId());
            return -1;
        }
        Integer current = selectedSlotBySeId.get(def.seId());
        if (current != null && probedSlots.contains(current))
        {
            return current;
        }
        int compat = PlayerSeLoadoutData.findCompatibleSlot(player, def.item());
        int pick = compat >= 0 ? compat : probedSlots.get(0);
        selectedSlotBySeId.put(def.seId(), pick);
        return pick;
    }

    private void cycleSelection(LocalPlayer player, String seId, int delta)
    {
        SeEffectRegistry.find(seId).ifPresent(def ->
        {
            List<Integer> probed = PlayerSeLoadoutData.listProbedSlots(player, def.item());
            if (probed.isEmpty())
            {
                return;
            }
            int current = resolveSelectedSlot(player, def, probed);
            int idx = Math.max(0, probed.indexOf(current));
            int next = Math.floorMod(idx + delta, probed.size());
            selectedSlotBySeId.put(seId, probed.get(next));
        });
    }

    private String formatSelectionLabel(LocalPlayer player, int slot, int total)
    {
        if (slot < 0 || total <= 0)
        {
            return tr("select.none");
        }
        ItemStack stack = player.getInventory().getItem(slot);
        CursedEnergyTrait trait = CursedObjectSeTrait.getTrait(stack);
        SePenalty penalty = CursedObjectSeTrait.getPenalty(stack);
        String traitName = trait == null ? "?" : trait.displayName().getString();
        String penaltyText = penalty == null
                ? "?"
                : penalty.stat().displayName().getString() + formatPenaltySigned(penalty);
        int index = 1;
        List<Integer> all = PlayerSeLoadoutData.listProbedSlots(player, stack.getItem());
        int pos = all.indexOf(slot);
        if (pos >= 0)
        {
            index = pos + 1;
        }
        return tr("select.entry", index, total, traitName, penaltyText);
    }

    private List<Component> buildSeTooltip(LocalPlayer player, HoverTarget target)
    {
        Optional<SeEffectDef> defOpt = SeEffectRegistry.find(target.seId());
        if (defOpt.isEmpty())
        {
            return List.of();
        }
        SeEffectDef def = defOpt.get();
        List<Component> lines = new ArrayList<>();

        lines.add(def.item().getDescription().copy().withStyle(ChatFormatting.GOLD));
        if (def.item() instanceof CursedObjectItem cursed)
        {
            lines.add(cursed.grade().tooltipLine());
        }

        lines.add(Component.empty());
        lines.add(Component.translatable("gui.jujutsukaisenzone.se.tooltip.effect")
                .withStyle(ChatFormatting.GRAY)
                .append(SeMechanicRegistry.find(def.seId())
                        .map(m -> m.effectDescription().copy().withStyle(ChatFormatting.AQUA))
                        .orElseGet(() -> Component.literal(def.seId()).withStyle(ChatFormatting.AQUA))));

        lines.add(Component.translatable("gui.jujutsukaisenzone.se.tooltip.quality", def.quality())
                .withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("gui.jujutsukaisenzone.se.tooltip.scope", scopeText(def))
                .withStyle(ChatFormatting.GRAY));

        if (target.equipped())
        {
            SeEquippedEntry entry = PlayerSeLoadoutData.getEquippedMap(player).get(def.seId());
            if (entry != null)
            {
                lines.add(Component.empty());
                lines.add(Component.translatable("gui.jujutsukaisenzone.se.tooltip.instance")
                        .withStyle(ChatFormatting.YELLOW));
                lines.add(traitLine(CursedEnergyTrait.byId(entry.traitId)));
                lines.add(penaltyLine(entry.penalty));
            }
            lines.add(Component.empty());
            lines.add(Component.translatable("gui.jujutsukaisenzone.se.tooltip.click_unequip")
                    .withStyle(ChatFormatting.DARK_GRAY));
            return lines;
        }

        Set<String> ownedTechniques = Set.of(PlayerTechniqueData.getInnateTechnique(player));
        List<Integer> probedSlots = PlayerSeLoadoutData.listProbedSlots(player, def.item());
        int selectedSlot = resolveSelectedSlot(player, def, probedSlots);
        boolean active = PlayerSeLoadoutData.isSeEquipped(player, def.seId());
        boolean hasItem = hasItem(player, def.item());
        boolean hasProbed = !probedSlots.isEmpty();
        // 与行内一致：看当前选中槽，不是「背包里有没有任意吻合件」。
        boolean slotOk = PlayerSeLoadoutData.isValidEquipSlot(player, def.item(), selectedSlot);
        boolean scopeOk = def.scope().matches(ownedTechniques);
        boolean narrowOk = !SeMechanicRegistry.hasNarrowInterfaceConflict(
                PlayerSeLoadoutData.getEquippedIds(player), def.seId());
        boolean qualityOk = !PlayerQualityPoolData.wouldExceedHardLimit(player, def.quality());
        boolean loadable = !active && slotOk && scopeOk && narrowOk && qualityOk;

        lines.add(Component.empty());
        lines.add(Component.translatable("gui.jujutsukaisenzone.se.tooltip.status",
                        statusText(active, narrowOk, slotOk, scopeOk, hasItem, hasProbed, qualityOk))
                .withStyle(loadable ? ChatFormatting.GREEN : ChatFormatting.RED));

        if (selectedSlot >= 0)
        {
            ItemStack preview = player.getInventory().getItem(selectedSlot);
            int index = Math.max(0, probedSlots.indexOf(selectedSlot)) + 1;
            lines.add(Component.empty());
            lines.add(Component.translatable(
                            "gui.jujutsukaisenzone.se.tooltip.selected",
                            index,
                            probedSlots.size())
                    .withStyle(ChatFormatting.YELLOW));
            if (!slotOk && CursedObjectSeTrait.isProbed(preview))
            {
                lines.add(Component.translatable("gui.jujutsukaisenzone.se.tooltip.selected_mismatch")
                        .withStyle(ChatFormatting.RED));
            }
            CursedEnergyTrait stackTrait = CursedObjectSeTrait.getTrait(preview);
            SePenalty stackPenalty = CursedObjectSeTrait.getPenalty(preview);
            if (stackTrait != null)
            {
                lines.add(traitLine(stackTrait));
            }
            if (stackPenalty != null)
            {
                lines.add(penaltyLine(stackPenalty));
            }
        }
        else if (!hasItem)
        {
            lines.add(Component.empty());
            lines.add(Component.translatable("gui.jujutsukaisenzone.se.tooltip.no_item")
                    .withStyle(ChatFormatting.DARK_GRAY));
            lines.add(Component.translatable("gui.jujutsukaisenzone.se.tooltip.cost_rolls_on_item")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        else if (!hasProbed)
        {
            lines.add(Component.empty());
            lines.add(Component.translatable("gui.jujutsukaisenzone.se.tooltip.no_probed")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }

        if (loadable)
        {
            lines.add(Component.empty());
            lines.add(Component.translatable("gui.jujutsukaisenzone.se.tooltip.click_equip")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        return lines;
    }

    private static Component traitLine(CursedEnergyTrait trait)
    {
        return Component.translatable("gui.jujutsukaisenzone.se.tooltip.trait", trait.displayName())
                .withStyle(ChatFormatting.AQUA);
    }

    private static Component penaltyLine(SePenalty penalty)
    {
        MutableComponent signed = Component.literal(formatPenaltySigned(penalty));
        return Component.translatable(
                        "gui.jujutsukaisenzone.se.tooltip.penalty",
                        penalty.stat().displayName(),
                        signed)
                .withStyle(ChatFormatting.RED);
    }

    /**
     * 领域量表满条是 +amount（更难攒满），其余落点用 −amount。
     */
    private static String formatPenaltySigned(SePenalty penalty)
    {
        String amount = formatAmount(penalty.amount());
        if (penalty.stat().applyKind() == PanelStat.ApplyKind.DOMAIN_GAUGE_MAX_ADD)
        {
            return "+" + amount;
        }
        return "−" + amount;
    }

    /** 与 {@code PlayerSeLoadoutData#tryEquip} 的校验链同序，第一条不满足的就是界面要显示的原因。 */
    private String statusText(boolean active, boolean narrowOk, boolean slotOk, boolean scopeOk,
                              boolean hasItem, boolean hasProbed, boolean qualityOk)
    {
        if (active)
        {
            return tr("status.equipped");
        }
        if (!hasItem)
        {
            return tr("status.missing");
        }
        if (!hasProbed)
        {
            return tr("status.not_probed");
        }
        if (!slotOk)
        {
            return tr("status.trait_mismatch");
        }
        if (!scopeOk)
        {
            return tr("status.technique_mismatch");
        }
        if (!narrowOk)
        {
            return tr("status.narrow_conflict");
        }
        if (!qualityOk)
        {
            return tr("status.quality_exceeded");
        }
        return tr("status.ready");
    }

    private String traitText()
    {
        return tr("trait.instance");
    }

    private String scopeText(SeEffectDef def)
    {
        if (def.scope() instanceof SeScope.Technique technique)
        {
            return tr("scope.technique", PlayerTechniqueData.getDisplayName(technique.techniqueId()));
        }
        return tr("scope.generic");
    }

    private static String formatAmount(double amount)
    {
        if (Math.abs(amount - Math.rint(amount)) < 1.0E-6)
        {
            return Long.toString(Math.round(amount));
        }
        return String.format(Locale.ROOT, "%.1f", amount);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if (button == 0)
        {
            LocalPlayer player = Minecraft.getInstance().player;
            for (Hit hit : hits)
            {
                if (mouseX >= hit.left && mouseX < hit.right && mouseY >= hit.top && mouseY < hit.bottom)
                {
                    switch (hit.kind)
                    {
                        case UNEQUIP -> ModNetwork.sendUnequipSeRequest(hit.seId);
                        case EQUIP -> ModNetwork.sendEquipSeRequest(hit.seId, hit.inventorySlot);
                        case CYCLE_PREV ->
                        {
                            if (player != null)
                            {
                                cycleSelection(player, hit.seId, -1);
                            }
                        }
                        case CYCLE_NEXT ->
                        {
                            if (player != null)
                            {
                                cycleSelection(player, hit.seId, 1);
                            }
                        }
                    }
                    return true;
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
        EQUIP,
        UNEQUIP,
        CYCLE_PREV,
        CYCLE_NEXT
    }

    private record Hit(int left, int top, int right, int bottom, HitKind kind, String seId, int inventorySlot)
    {
    }

    private record HoverTarget(String seId, boolean equipped)
    {
    }
}
