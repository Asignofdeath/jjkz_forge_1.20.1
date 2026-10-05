package com.darkness.jujutsukaisenzone.client;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/**
 * CG / 电影巡镜用对白：上下黑幕（letterbox），台词画在下黑条里。
 * <p>
 * 普通对白 COD 字幕见 {@link QuestCodCaptionOverlay}。角色名染色见 {@link QuestDialogCaptionStyle}。
 * <p>
 * 激活期间强制 {@code hideGui=true}。黑幕/字幕走 {@link TickEvent.RenderTickEvent} {@code END} 自建 {@link GuiGraphics}。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class QuestCinematicLetterboxOverlay
{
    private static final int BAR_COLOR = 0xFF000000;
    private static final float BAR_FRACTION = 0.12F;
    private static final long FADE_IN_MILLIS = 10L * 50L;
    private static final long FADE_OUT_MILLIS = 20L * 50L;
    private static final int TYPEWRITER_CHARS_PER_TICK = 3;

    private static boolean active;
    private static boolean hiding;
    private static String captionKey = "";
    private static long fadeStartMillis;
    private static int revealedChars;
    private static boolean hideGuiCached;
    private static boolean forcingHideGui;

    private QuestCinematicLetterboxOverlay()
    {
    }

    public static void show(String key)
    {
        String next = key == null ? "" : key;
        if (!next.equals(captionKey))
        {
            captionKey = next;
            revealedChars = 0;
            QuestDialogVoice.playCaption(next);
        }
        if (!active)
        {
            active = true;
            hiding = false;
            fadeStartMillis = System.currentTimeMillis();
            beginForceHideGui();
        }
        else if (hiding)
        {
            hiding = false;
            fadeStartMillis = System.currentTimeMillis();
            beginForceHideGui();
        }
    }

    public static void hide()
    {
        if (!active)
        {
            endForceHideGui();
            return;
        }
        if (hiding)
        {
            return;
        }
        hiding = true;
        fadeStartMillis = System.currentTimeMillis();
        QuestDialogVoice.stop();
    }

    /** 立刻关掉黑幕并恢复 HUD。 */
    public static void clearImmediate()
    {
        active = false;
        hiding = false;
        captionKey = "";
        revealedChars = 0;
        QuestDialogVoice.stop();
        endForceHideGui();
    }

    public static boolean isActive()
    {
        return active;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END || !active)
        {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null)
        {
            minecraft.setScreen(null);
        }
        if (forcingHideGui)
        {
            minecraft.options.hideGui = true;
        }
        // 淡出收尾不依赖渲染，避免卡在 hideGui=true
        if (hiding)
        {
            resolveFade();
            if (!active)
            {
                return;
            }
        }
        if (hiding || captionKey.isBlank())
        {
            return;
        }
        String full = Component.translatable(captionKey).getString();
        if (revealedChars < full.length())
        {
            revealedChars = Math.min(full.length(), revealedChars + TYPEWRITER_CHARS_PER_TICK);
        }
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END || !active)
        {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null)
        {
            return;
        }
        if (forcingHideGui)
        {
            minecraft.options.hideGui = true;
        }

        float fade = resolveFade();
        if (fade <= 0.0F)
        {
            return;
        }

        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        Font font = minecraft.font;
        int wrapWidth = Math.max(16, width - 40);
        String full = captionKey.isBlank() ? "" : Component.translatable(captionKey).getString();
        Component fullStyled = QuestDialogCaptionStyle.style(full, full);
        String visiblePlain = full.isEmpty() ? "" : full.substring(0, Math.min(revealedChars, full.length()));
        Component visibleStyled = QuestDialogCaptionStyle.style(full, visiblePlain);

        List<FormattedCharSequence> layoutLines = full.isEmpty()
                ? List.of()
                : font.split(fullStyled, wrapWidth);
        List<FormattedCharSequence> lines = visiblePlain.isEmpty()
                ? List.of()
                : font.split(visibleStyled, wrapWidth);
        int textBlockH = layoutLines.isEmpty() ? 0 : layoutLines.size() * font.lineHeight;
        int barHeight = Math.round(Math.max(height * BAR_FRACTION, textBlockH + 8) * fade);
        barHeight = Math.min(barHeight, height / 3);
        if (barHeight <= 0)
        {
            return;
        }

        RenderSystem.backupProjectionMatrix();
        Matrix4f ortho = new Matrix4f().setOrtho(0.0F, (float) width, (float) height, 0.0F, 1000.0F, 3000.0F);
        RenderSystem.setProjectionMatrix(ortho, VertexSorting.ORTHOGRAPHIC_Z);

        PoseStack modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        modelView.setIdentity();
        modelView.translate(0.0D, 0.0D, 1000.0D - 3000.0D);
        RenderSystem.applyModelViewMatrix();

        GuiGraphics graphics = new GuiGraphics(minecraft, minecraft.renderBuffers().bufferSource());
        graphics.fill(0, 0, width, barHeight, BAR_COLOR);
        graphics.fill(0, height - barHeight, width, height, BAR_COLOR);
        if (!lines.isEmpty() && barHeight >= font.lineHeight + 2)
        {
            int alpha = Math.round(255.0F * fade);
            if (alpha > 0)
            {
                // alpha 由传入色提供；段内 RGB 来自 Style（角色名染色 / 正文白）
                int drawColor = (alpha << 24) | 0xFFFFFF;
                int y = height - (barHeight + textBlockH) / 2;
                for (FormattedCharSequence line : lines)
                {
                    int lineW = font.width(line);
                    graphics.drawString(font, line, (width - lineW) / 2, y, drawColor, false);
                    y += font.lineHeight;
                }
            }
        }
        graphics.flush();

        modelView.popPose();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.restoreProjectionMatrix();
    }

    private static void beginForceHideGui()
    {
        Minecraft minecraft = Minecraft.getInstance();
        if (!forcingHideGui)
        {
            // 不缓存 CMDCam 留下的 true，否则对话结束会「恢复」成一直隐藏
            hideGuiCached = false;
            forcingHideGui = true;
        }
        minecraft.options.hideGui = true;
    }

    private static void endForceHideGui()
    {
        if (!forcingHideGui)
        {
            // 即使标记丢了，对话结束也必须把 HUD 拉回来
            Minecraft.getInstance().options.hideGui = false;
            return;
        }
        Minecraft.getInstance().options.hideGui = false;
        forcingHideGui = false;
        hideGuiCached = false;
    }

    private static float resolveFade()
    {
        long elapsed = System.currentTimeMillis() - fadeStartMillis;
        if (hiding)
        {
            if (elapsed >= FADE_OUT_MILLIS)
            {
                active = false;
                hiding = false;
                captionKey = "";
                revealedChars = 0;
                endForceHideGui();
                return 0.0F;
            }
            return 1.0F - smoothStep((float) elapsed / (float) FADE_OUT_MILLIS);
        }
        if (elapsed >= FADE_IN_MILLIS)
        {
            return 1.0F;
        }
        return smoothStep((float) elapsed / (float) FADE_IN_MILLIS);
    }

    private static float smoothStep(float t)
    {
        float x = Mth.clamp(t, 0.0F, 1.0F);
        return x * x * (3.0F - 2.0F * x);
    }
}
