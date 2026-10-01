package com.darkness.jujutsukaisenzone.client.gui.technique;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.technique.development.SkillUnlockLogic;
import com.darkness.jujutsukaisenzone.technique.development.SkillUpgradeStatCatalog;
import com.darkness.jujutsukaisenzone.technique.skillset.TechniqueSkill;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * 技能升级成功后的居中弹窗：淡入缩放 → 停留 → 淡出，列出本级提升的数值。
 * <p>
 * 有 Screen 时走 {@link ScreenEvent.Render.Post}（在界面与 tip 之后）；无 Screen 时走
 * {@link RenderGuiEvent.Post}。绘制时关闭深度测试，否则会被 LDLib 技能描述 tip（更高 Z）挡住。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class SkillUpgradeResultPopup
{
    private static final long FADE_IN_MS = 220L;
    private static final long HOLD_MS = 2600L;
    private static final long FADE_OUT_MS = 320L;
    private static final long TOTAL_MS = FADE_IN_MS + HOLD_MS + FADE_OUT_MS;

    private static final int PANEL_RGB = 0x12121A;
    private static final int BORDER_RGB = 0xC9A227;
    private static final int TITLE_RGB = 0xF5E6A8;
    private static final int LEVEL_RGB = 0xB8B8C8;
    private static final int LABEL_RGB = 0xD0D0D8;
    private static final int FROM_RGB = 0x888898;
    private static final int ARROW_RGB = 0xA0A0B0;
    private static final int TO_RGB = 0x6DDC8C;

    private static ActivePopup active;

    private SkillUpgradeResultPopup()
    {
    }

    public static void show(String techniqueId, String skillId, int oldLevel, int newLevel)
    {
        Component skillName = SkillUnlockLogic.findSkillDefinition(techniqueId, skillId)
                .map(TechniqueSkill::displayName)
                .orElse(Component.literal(skillId == null ? "?" : skillId));
        List<SkillUpgradeStatCatalog.Line> lines = new ArrayList<>(
                SkillUpgradeStatCatalog.linesFor(skillId, oldLevel, newLevel));
        active = new ActivePopup(skillName, oldLevel, newLevel, lines, System.currentTimeMillis());
    }

    /** 盖在当前 Screen（含术式开发 ModularUI）之上。 */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onScreenRenderPost(ScreenEvent.Render.Post event)
    {
        renderPopup(event.getGuiGraphics(), event.getScreen().width, event.getScreen().height);
    }

    /** 无 Screen 时的兜底（HUD 层）。有 Screen 时不画，避免被界面盖住又重复绘制。 */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onRenderGui(RenderGuiEvent.Post event)
    {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null)
        {
            return;
        }
        renderPopup(
                event.getGuiGraphics(),
                event.getWindow().getGuiScaledWidth(),
                event.getWindow().getGuiScaledHeight());
    }

    private static void renderPopup(GuiGraphics graphics, int screenW, int screenH)
    {
        ActivePopup popup = active;
        if (popup == null)
        {
            return;
        }
        long elapsed = System.currentTimeMillis() - popup.startedAtMs;
        if (elapsed >= TOTAL_MS)
        {
            active = null;
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null)
        {
            return;
        }

        float alpha = resolveAlpha(elapsed);
        float scale = resolveScale(elapsed);
        if (alpha <= 0.01F)
        {
            return;
        }

        Font font = minecraft.font;
        int lineH = 12;
        int paddingX = 14;
        int paddingY = 12;
        int titleGap = 4;
        int levelGap = 10;
        int contentW = font.width(popup.title());
        contentW = Math.max(contentW, font.width(popup.levelLine()));
        for (SkillUpgradeStatCatalog.Line line : popup.lines)
        {
            contentW = Math.max(contentW, measureStatLine(font, line));
        }
        int panelW = Math.max(160, contentW + paddingX * 2);
        int panelH = paddingY * 2
                + font.lineHeight
                + titleGap
                + font.lineHeight
                + (popup.lines.isEmpty() ? 0 : levelGap + popup.lines.size() * lineH);

        int centerX = screenW / 2;
        int centerY = Math.max(panelH / 2 + 24, screenH / 5 + panelH / 2);
        float slide = (1.0F - easeOutCubic(Math.min(1.0F, elapsed / (float) FADE_IN_MS))) * 10.0F;

        graphics.pose().pushPose();
        // LDLib 技能描述 tip 在 ~Z600 且仍开深度测试；关深度才能保证本弹窗盖在 tip 之上。
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        graphics.pose().translate(centerX, centerY + slide, 1000.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.pose().translate(-centerX, -centerY, 0.0F);

        int left = centerX - panelW / 2;
        int top = centerY - panelH / 2;
        int right = left + panelW;
        int bottom = top + panelH;

        int panelAlpha = Math.round(210 * alpha);
        int borderAlpha = Math.round(230 * alpha);
        graphics.fill(left - 1, top - 1, right + 1, bottom + 1, colorWithAlpha(BORDER_RGB, borderAlpha));
        graphics.fill(left, top, right, bottom, colorWithAlpha(PANEL_RGB, panelAlpha));
        graphics.fill(left, top, right, top + 2, colorWithAlpha(BORDER_RGB, borderAlpha));

        int y = top + paddingY;
        drawCentered(graphics, font, popup.title(), centerX, y, colorWithAlpha(TITLE_RGB, Math.round(255 * alpha)));
        y += font.lineHeight + titleGap;
        drawCentered(graphics, font, popup.levelLine(), centerX, y, colorWithAlpha(LEVEL_RGB, Math.round(230 * alpha)));
        y += font.lineHeight;

        if (!popup.lines.isEmpty())
        {
            y += levelGap - 2;
            for (SkillUpgradeStatCatalog.Line line : popup.lines)
            {
                drawStatLine(graphics, font, line, left + paddingX, right - paddingX, y, alpha);
                y += lineH;
            }
        }

        graphics.pose().popPose();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static int measureStatLine(Font font, SkillUpgradeStatCatalog.Line line)
    {
        return font.width(line.label())
                + font.width("  ")
                + font.width(line.from())
                + font.width(" → ")
                + font.width(line.to());
    }

    private static void drawStatLine(
            GuiGraphics graphics,
            Font font,
            SkillUpgradeStatCatalog.Line line,
            int left,
            int right,
            int y,
            float alpha)
    {
        int labelColor = colorWithAlpha(LABEL_RGB, Math.round(240 * alpha));
        int fromColor = colorWithAlpha(FROM_RGB, Math.round(220 * alpha));
        int arrowColor = colorWithAlpha(ARROW_RGB, Math.round(220 * alpha));
        int toColor = colorWithAlpha(TO_RGB, Math.round(255 * alpha));

        String arrow = " → ";
        int valuesW = font.width(line.from()) + font.width(arrow) + font.width(line.to());
        int labelX = left;
        int valuesX = right - valuesW;
        if (valuesX < labelX + font.width(line.label()) + 8)
        {
            valuesX = labelX + font.width(line.label()) + 8;
        }

        graphics.drawString(font, line.label(), labelX, y, labelColor, false);
        int x = valuesX;
        graphics.drawString(font, line.from(), x, y, fromColor, false);
        x += font.width(line.from());
        graphics.drawString(font, arrow, x, y, arrowColor, false);
        x += font.width(arrow);
        graphics.drawString(font, line.to(), x, y, toColor, false);
    }

    private static void drawCentered(GuiGraphics graphics, Font font, Component text, int centerX, int y, int color)
    {
        graphics.drawString(font, text, centerX - font.width(text) / 2, y, color, false);
    }

    private static float resolveAlpha(long elapsed)
    {
        if (elapsed < FADE_IN_MS)
        {
            return easeOutCubic(elapsed / (float) FADE_IN_MS);
        }
        if (elapsed < FADE_IN_MS + HOLD_MS)
        {
            return 1.0F;
        }
        float t = (elapsed - FADE_IN_MS - HOLD_MS) / (float) FADE_OUT_MS;
        return 1.0F - easeInCubic(Mth.clamp(t, 0.0F, 1.0F));
    }

    private static float resolveScale(long elapsed)
    {
        if (elapsed < FADE_IN_MS)
        {
            float t = easeOutCubic(elapsed / (float) FADE_IN_MS);
            return 0.88F + 0.12F * t;
        }
        if (elapsed < FADE_IN_MS + HOLD_MS)
        {
            return 1.0F;
        }
        float t = (elapsed - FADE_IN_MS - HOLD_MS) / (float) FADE_OUT_MS;
        return 1.0F - 0.04F * easeInCubic(Mth.clamp(t, 0.0F, 1.0F));
    }

    private static float easeOutCubic(float t)
    {
        float u = 1.0F - t;
        return 1.0F - u * u * u;
    }

    private static float easeInCubic(float t)
    {
        return t * t * t;
    }

    private static int colorWithAlpha(int rgb, int alpha)
    {
        return (Mth.clamp(alpha, 0, 255) << 24) | (rgb & 0xFFFFFF);
    }

    private record ActivePopup(
            Component skillName,
            int oldLevel,
            int newLevel,
            List<SkillUpgradeStatCatalog.Line> lines,
            long startedAtMs)
    {
        private Component title()
        {
            return Component.translatable("gui.jujutsukaisenzone.skill_upgrade.popup.title", skillName);
        }

        private Component levelLine()
        {
            return Component.translatable(
                    "gui.jujutsukaisenzone.skill_upgrade.popup.level",
                    Integer.toString(oldLevel),
                    Integer.toString(newLevel));
        }
    }
}
