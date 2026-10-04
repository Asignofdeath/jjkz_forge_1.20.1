package com.darkness.jujutsukaisenzone.client;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.registry.ModSounds;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/**
 * COD 风格任务简报：左下角短暂开场。
 * <p>
 * 动画：标题左滑入 + 放大回弹；元数据逐行错开滑入；最后整体淡出。
 * <p>
 * 绘制路径对齐 {@code cg_director} 的 Media 字幕：{@link TickEvent.RenderTickEvent} {@code END}
 * + 自建 {@link GuiGraphics}。CMDCam 运镜时 {@code hideGui=true} 会跳过 {@code Gui.render}，
 * {@code RenderGuiEvent} 不会触发，故不能挂 HUD 事件。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class QuestMissionBriefingOverlay
{
    private static final int COLOR_WHITE = 0xFFFFFF;
    private static final float TITLE_SCALE = 1.65F;
    private static final float TITLE_SCALE_START = 1.20F;
    private static final int MARGIN_LEFT = 16;
    private static final int MARGIN_BOTTOM = 28;
    private static final float SLIDE_PX = 28.0F;

    private static final long TITLE_IN_MILLIS = 450L;
    private static final long[] META_DELAYS_MILLIS = {250L, 400L, 550L, 700L};
    private static final long META_IN_MILLIS = 320L;
    private static final long HOLD_MILLIS = 4300L;
    private static final long FADE_OUT_MILLIS = 600L;
    private static final long INTRO_END_MILLIS = META_DELAYS_MILLIS[META_DELAYS_MILLIS.length - 1] + META_IN_MILLIS;
    private static final long TOTAL_MILLIS = INTRO_END_MILLIS + HOLD_MILLIS + FADE_OUT_MILLIS;

    private static boolean active;
    /** 任务界面等还开着时先挂起，关界面后再起算动画。 */
    private static boolean waitingForHud;
    private static boolean soundPlayed;
    private static long startMillis;
    private static String titleKey = "";
    private static String orgKey = "";
    private static String locationKey = "";
    private static String timeText = "";
    private static String actorText = "";

    private QuestMissionBriefingOverlay()
    {
    }

    public static void show(String titleLangKey, String orgLangKey, String locationLangKey)
    {
        Minecraft minecraft = Minecraft.getInstance();
        titleKey = titleLangKey == null ? "" : titleLangKey;
        orgKey = orgLangKey == null ? "" : orgLangKey;
        locationKey = locationLangKey == null ? "" : locationLangKey;
        timeText = formatRealTime(minecraft);
        String name = minecraft.player != null
                ? minecraft.player.getGameProfile().getName()
                : "";
        actorText = name.isEmpty() ? "" : "「" + name + "」";
        boolean alreadyActive = active;
        active = true;
        waitingForHud = minecraft.screen != null;
        startMillis = waitingForHud ? 0L : System.currentTimeMillis();
        if (!alreadyActive)
        {
            soundPlayed = false;
        }
        if (!waitingForHud)
        {
            playBriefingSoundOnce();
        }
    }

    public static void clear()
    {
        active = false;
        waitingForHud = false;
        soundPlayed = false;
        titleKey = "";
        orgKey = "";
        locationKey = "";
        timeText = "";
        actorText = "";
    }

    public static boolean isActive()
    {
        return active;
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END || !active)
        {
            return;
        }
        if (QuestBlackoutOverlay.isActive())
        {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null)
        {
            return;
        }
        if (waitingForHud)
        {
            if (minecraft.screen != null)
            {
                return;
            }
            waitingForHud = false;
            startMillis = System.currentTimeMillis();
            playBriefingSoundOnce();
        }

        long elapsed = System.currentTimeMillis() - startMillis;
        if (elapsed >= TOTAL_MILLIS)
        {
            clear();
            return;
        }

        float exitAlpha = exitAlphaFor(elapsed);
        if (exitAlpha <= 0.01F)
        {
            return;
        }

        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();

        RenderSystem.backupProjectionMatrix();
        Matrix4f ortho = new Matrix4f().setOrtho(0.0F, (float) width, (float) height, 0.0F, 1000.0F, 3000.0F);
        RenderSystem.setProjectionMatrix(ortho, VertexSorting.ORTHOGRAPHIC_Z);

        PoseStack modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        modelView.setIdentity();
        modelView.translate(0.0D, 0.0D, 1000.0D - 3000.0D);
        RenderSystem.applyModelViewMatrix();

        GuiGraphics graphics = new GuiGraphics(minecraft, minecraft.renderBuffers().bufferSource());
        Font font = minecraft.font;

        String title = titleKey.isEmpty() ? "" : Component.translatable(titleKey).getString();
        String[] metaLines = {
                timeText,
                actorText,
                orgKey.isEmpty() ? "" : Component.translatable(orgKey).getString(),
                locationKey.isEmpty() ? "" : Component.translatable(locationKey).getString()
        };

        int lineH = font.lineHeight + 2;
        int titleH = Math.round(font.lineHeight * TITLE_SCALE) + 4;
        int blockH = titleH + lineH * metaLines.length;
        int baseY = height - MARGIN_BOTTOM - blockH;
        int baseX = MARGIN_LEFT;

        drawTitle(graphics, font, title, baseX, baseY, elapsed, exitAlpha);

        int metaY = baseY + titleH;
        for (int i = 0; i < metaLines.length; i++)
        {
            drawMetaLine(graphics, font, metaLines[i], baseX, metaY, elapsed, META_DELAYS_MILLIS[i], exitAlpha);
            metaY += lineH;
        }

        graphics.flush();

        modelView.popPose();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.restoreProjectionMatrix();
    }

    private static void drawTitle(
            GuiGraphics graphics, Font font, String text, int x, int y, long elapsed, float exitAlpha)
    {
        float t = progress(elapsed, 0L, TITLE_IN_MILLIS);
        if (t <= 0.0F)
        {
            return;
        }
        float ease = easeOutCubic(t);
        float back = easeOutBack(t);
        float alpha = ease * exitAlpha;
        float slide = (1.0F - ease) * -SLIDE_PX;
        float scale = TITLE_SCALE_START + (TITLE_SCALE - TITLE_SCALE_START) * back;
        float exitSlide = (1.0F - exitAlpha) * -10.0F;

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x + slide, y + exitSlide, 0.0F);
        pose.scale(scale, scale, 1.0F);
        graphics.drawString(font, text, 0, 0, withAlpha(COLOR_WHITE, alpha), true);
        pose.popPose();
    }

    private static void drawMetaLine(
            GuiGraphics graphics,
            Font font,
            String text,
            int x,
            int y,
            long elapsed,
            long delayMillis,
            float exitAlpha)
    {
        if (text == null || text.isEmpty())
        {
            return;
        }
        float t = progress(elapsed, delayMillis, META_IN_MILLIS);
        if (t <= 0.0F)
        {
            return;
        }
        float ease = easeOutCubic(t);
        float alpha = ease * exitAlpha;
        float slide = (1.0F - ease) * -SLIDE_PX;
        float exitSlide = (1.0F - exitAlpha) * -8.0F;

        graphics.drawString(
                font,
                text,
                Math.round(x + slide),
                Math.round(y + exitSlide),
                withAlpha(COLOR_WHITE, alpha),
                true);
    }

    private static void playBriefingSoundOnce()
    {
        if (soundPlayed)
        {
            return;
        }
        soundPlayed = true;
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getSoundManager().play(new SimpleSoundInstance(
                ModSounds.QUEST_TASK_BRIEF.get().getLocation(),
                SoundSource.MASTER,
                1.0F,
                1.0F,
                SoundInstance.createUnseededRandom(),
                false,
                0,
                SoundInstance.Attenuation.NONE,
                0.0D,
                0.0D,
                0.0D,
                true));
    }

    private static float exitAlphaFor(long elapsed)
    {
        long fadeStart = INTRO_END_MILLIS + HOLD_MILLIS;
        if (elapsed < fadeStart)
        {
            return 1.0F;
        }
        long fadeElapsed = elapsed - fadeStart;
        return 1.0F - Mth.clamp(fadeElapsed / (float) FADE_OUT_MILLIS, 0.0F, 1.0F);
    }

    private static float progress(long elapsed, long delayMillis, long durationMillis)
    {
        if (elapsed < delayMillis)
        {
            return 0.0F;
        }
        return Mth.clamp((elapsed - delayMillis) / (float) durationMillis, 0.0F, 1.0F);
    }

    private static float easeOutCubic(float t)
    {
        float u = 1.0F - t;
        return 1.0F - u * u * u;
    }

    private static float easeOutBack(float t)
    {
        float c1 = 1.70158F;
        float c3 = c1 + 1.0F;
        float u = t - 1.0F;
        return 1.0F + c3 * u * u * u + c1 * u * u;
    }

    private static int withAlpha(int rgb, float alpha)
    {
        int a = Mth.clamp(Math.round(alpha * 255.0F), 0, 255);
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    private static String formatRealTime(Minecraft minecraft)
    {
        LocalDateTime now = LocalDateTime.now();
        String lang = minecraft.getLanguageManager().getSelected();
        if (lang != null && (lang.startsWith("zh_") || lang.startsWith("ja_")))
        {
            return now.format(DateTimeFormatter.ofPattern("yyyy年M月d日HH:mm", Locale.ROOT));
        }
        return now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ROOT));
    }
}
