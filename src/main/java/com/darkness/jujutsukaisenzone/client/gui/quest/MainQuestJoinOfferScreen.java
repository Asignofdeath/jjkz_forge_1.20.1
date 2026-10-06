package com.darkness.jujutsukaisenzone.client.gui.quest;

import com.darkness.jujutsukaisenzone.client.QuestJournalKeyHandler;
import com.darkness.jujutsukaisenzone.gameplay.ModRules;
import com.darkness.jujutsukaisenzone.network.NetworkSenders;
import com.darkness.jujutsukaisenzone.network.packet.JoinOfferSetModRuleC2SPacket;
import com.darkness.jujutsukaisenzone.network.packet.MainQuestJoinOfferC2SPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * 第一次进世界：询问是否进入主线，并展示全服 ModRules。
 */
@OnlyIn(Dist.CLIENT)
public final class MainQuestJoinOfferScreen extends Screen
{
    private static final int COLOR_BACKGROUND = 0xE812161C;
    private static final int COLOR_BORDER = 0xFF3A5A8A;
    private static final int COLOR_TITLE = 0xFFE8C56B;
    private static final int COLOR_BODY = 0xFFECECEC;
    private static final int COLOR_HINT = 0xFF808080;

    private boolean admin;
    private boolean aerialEnabled;
    private boolean skillNoCooldown;
    private byte hungerOrdinal;
    private boolean stylizedEnabled;
    private boolean answered;
    private final List<SideHint> sideHints = new ArrayList<>();
    private int lockedHintY = -1;

    public MainQuestJoinOfferScreen(
            boolean admin,
            boolean aerialEnabled,
            boolean skillNoCooldown,
            byte hungerOrdinal,
            boolean stylizedEnabled)
    {
        super(Component.translatable("gui.jujutsukaisenzone.main_quest_offer.title"));
        this.admin = admin;
        this.aerialEnabled = aerialEnabled;
        this.skillNoCooldown = skillNoCooldown;
        this.hungerOrdinal = hungerOrdinal;
        this.stylizedEnabled = stylizedEnabled;
    }

    public void applySnapshot(
            boolean admin,
            boolean aerialEnabled,
            boolean skillNoCooldown,
            byte hungerOrdinal,
            boolean stylizedEnabled)
    {
        this.admin = admin;
        this.aerialEnabled = aerialEnabled;
        this.skillNoCooldown = skillNoCooldown;
        this.hungerOrdinal = hungerOrdinal;
        this.stylizedEnabled = stylizedEnabled;
        this.rebuildWidgets();
    }

    @Override
    protected void init()
    {
        super.init();
        this.sideHints.clear();
        this.lockedHintY = -1;
        int buttonWidth = 220;
        int buttonX = Math.max(12, this.width / 2 - 170);
        int hintX = buttonX + buttonWidth + 8;
        int hintWidth = Math.max(80, this.width - hintX - 12);
        int y = 96;
        y = addBooleanRule(
                buttonX, y, buttonWidth, hintX, hintWidth,
                "gui.jujutsukaisenzone.main_quest_offer.rule.aerial",
                "gui.jujutsukaisenzone.main_quest_offer.rule.aerial.desc",
                this.aerialEnabled,
                JoinOfferSetModRuleC2SPacket.RULE_AERIAL);
        y = addBooleanRule(
                buttonX, y, buttonWidth, hintX, hintWidth,
                "gui.jujutsukaisenzone.main_quest_offer.rule.skill_no_cooldown",
                null,
                this.skillNoCooldown,
                JoinOfferSetModRuleC2SPacket.RULE_SKILL_NO_COOLDOWN);
        CycleButton<ModRules.HungerConsumption.Mode> hunger = CycleButton.builder(MainQuestJoinOfferScreen::hungerLabel)
                .withValues(java.util.List.of(ModRules.HungerConsumption.Mode.values()))
                .withInitialValue(hungerMode())
                .create(buttonX, y, buttonWidth, 20,
                        Component.translatable("gui.jujutsukaisenzone.main_quest_offer.rule.hunger"),
                        (button, mode) ->
                        {
                            if (this.admin)
                            {
                                NetworkSenders.toServer(new JoinOfferSetModRuleC2SPacket(
                                        JoinOfferSetModRuleC2SPacket.RULE_HUNGER, mode.ordinal()));
                            }
                        });
        hunger.active = this.admin;
        this.addRenderableWidget(hunger);
        y += 24;
        y = addBooleanRule(
                buttonX, y, buttonWidth, hintX, hintWidth,
                "gui.jujutsukaisenzone.main_quest_offer.rule.stylized",
                "gui.jujutsukaisenzone.main_quest_offer.rule.stylized.desc",
                this.stylizedEnabled,
                JoinOfferSetModRuleC2SPacket.RULE_STYLIZED);
        if (!this.admin)
        {
            this.lockedHintY = y;
            y += 28;
        }
        else
        {
            y += 8;
        }
        int acceptWidth = 120;
        int gap = 16;
        int acceptX = this.width / 2 - acceptWidth - gap / 2;
        int closeX = this.width / 2 + gap / 2;
        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.jujutsukaisenzone.main_quest_offer.accept"),
                        button -> answer(true))
                .bounds(acceptX, y, acceptWidth, 20)
                .build());
        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.jujutsukaisenzone.main_quest_offer.close"),
                        button -> answer(false))
                .bounds(closeX, y, acceptWidth, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        graphics.fill(0, 0, this.width, this.height, COLOR_BACKGROUND);
        graphics.fill(0, 0, this.width, 1, COLOR_BORDER);
        graphics.fill(0, this.height - 1, this.width, this.height, COLOR_BORDER);

        int centerX = this.width / 2;
        graphics.drawCenteredString(this.font, this.title, centerX, 16, COLOR_TITLE);
        int wrapWidth = Math.min(400, this.width - 48);
        int y = 34;
        y = drawWrapped(graphics, Component.translatable("gui.jujutsukaisenzone.main_quest_offer.ask"),
                centerX, y, wrapWidth, COLOR_BODY);
        y += 6;
        Component keyName = QuestJournalKeyHandler.KEY_MAPPING.getTranslatedKeyMessage();
        drawWrapped(graphics, Component.translatable("gui.jujutsukaisenzone.main_quest_offer.journal_hint", keyName),
                centerX, y, wrapWidth, COLOR_HINT);

        for (SideHint hint : this.sideHints)
        {
            drawWrappedLeft(graphics, hint.text(), hint.x(), hint.y(), hint.width(), COLOR_HINT);
        }
        if (this.lockedHintY >= 0)
        {
            drawWrapped(graphics, Component.translatable("gui.jujutsukaisenzone.main_quest_offer.modrules_locked"),
                    centerX, this.lockedHintY, wrapWidth, COLOR_HINT);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose()
    {
        answer(false);
    }

    @Override
    public boolean shouldCloseOnEsc()
    {
        return true;
    }

    private int addBooleanRule(
            int x,
            int y,
            int width,
            int hintX,
            int hintWidth,
            String nameKey,
            String descKey,
            boolean value,
            byte rule)
    {
        CycleButton<Boolean> button = CycleButton.booleanBuilder(
                        Component.translatable("gui.jujutsukaisenzone.main_quest_offer.rule.on"),
                        Component.translatable("gui.jujutsukaisenzone.main_quest_offer.rule.off"))
                .withInitialValue(value)
                .create(x, y, width, 20, Component.translatable(nameKey),
                        (clicked, enabled) ->
                        {
                            if (this.admin)
                            {
                                NetworkSenders.toServer(new JoinOfferSetModRuleC2SPacket(rule, enabled ? 1 : 0));
                            }
                        });
        button.active = this.admin;
        this.addRenderableWidget(button);
        if (descKey != null)
        {
            this.sideHints.add(new SideHint(hintX, y + 2, hintWidth, Component.translatable(descKey)));
        }
        return y + 24;
    }

    private ModRules.HungerConsumption.Mode hungerMode()
    {
        ModRules.HungerConsumption.Mode[] modes = ModRules.HungerConsumption.Mode.values();
        int index = this.hungerOrdinal & 0xFF;
        if (index >= modes.length)
        {
            return ModRules.HungerConsumption.Mode.LOW;
        }
        return modes[index];
    }

    private static Component hungerLabel(ModRules.HungerConsumption.Mode mode)
    {
        return Component.literal(mode.zhLabel());
    }

    private void answer(boolean accept)
    {
        if (this.answered)
        {
            return;
        }
        this.answered = true;
        NetworkSenders.toServer(new MainQuestJoinOfferC2SPacket(accept));
        super.onClose();
    }

    private int drawWrapped(GuiGraphics graphics, Component text, int centerX, int y, int wrapWidth, int color)
    {
        List<FormattedCharSequence> lines = this.font.split(text, wrapWidth);
        for (FormattedCharSequence line : lines)
        {
            int lineX = centerX - this.font.width(line) / 2;
            graphics.drawString(this.font, line, lineX, y, color, false);
            y += this.font.lineHeight + 2;
        }
        return y;
    }

    private void drawWrappedLeft(GuiGraphics graphics, Component text, int x, int y, int wrapWidth, int color)
    {
        List<FormattedCharSequence> lines = this.font.split(text, wrapWidth);
        for (FormattedCharSequence line : lines)
        {
            graphics.drawString(this.font, line, x, y, color, false);
            y += this.font.lineHeight + 1;
        }
    }

    private record SideHint(int x, int y, int width, Component text)
    {
    }
}
