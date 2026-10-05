package com.darkness.jujutsukaisenzone.client;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/**
 * COD 风格普通对白：底中有限队列（最大 2），淡入+上浮出现，淡出消失。
 * <p>
 * 句间间隔 ≥ {@link #SIMULTANEOUS_GAP_TICKS}：下一条把上一条顶上去后淡出；
 * 间隔更短：两条同时占满两槽（多人抢话）。
 * <p>
 * 绘制路径对齐任务简报：{@link TickEvent.RenderTickEvent} {@code END} + 自建 {@link GuiGraphics}。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class QuestCodCaptionOverlay
{
    public static final int MAX_SLOTS = 2;
    /** 推荐：约 0.6s；小于此间隔的两句同时出现。 */
    public static final int SIMULTANEOUS_GAP_TICKS = 12;
    public static final int DEFAULT_HOLD_TICKS = 70;

    private static final long FADE_IN_MILLIS = 220L;
    private static final long FADE_OUT_MILLIS = 320L;
    private static final float FLOAT_IN_PX = 12.0F;
    /** 抬过原版血条 / 护甲条（约在底边上方 39–59）。 */
    private static final int MARGIN_BOTTOM = 72;
    private static final int LINE_GAP = 4;

    private static final List<PendingLine> PENDING = new ArrayList<>();
    private static final List<Slot> SLOTS = new ArrayList<>();
    private static long lastPresentMillis = Long.MIN_VALUE / 4;

    private QuestCodCaptionOverlay()
    {
    }

    /**
     * 播一整条队列。{@code delayFromPrevTicks[i]} 为相对上一句出现的间隔（首句通常 0）。
     * {@code holdTicks[i]} 未被顶替时的停留；null 则用 {@link #DEFAULT_HOLD_TICKS}。
     */
    public static void playQueue(String[] keys, int[] delayFromPrevTicks, int[] holdTicks)
    {
        if (keys == null || keys.length == 0)
        {
            return;
        }
        clearImmediate();
        long now = System.currentTimeMillis();
        long appearAt = now;
        for (int i = 0; i < keys.length; i++)
        {
            String key = keys[i] == null ? "" : keys[i];
            if (key.isBlank())
            {
                continue;
            }
            int delay = delayFromPrevTicks != null && i < delayFromPrevTicks.length
                    ? Math.max(0, delayFromPrevTicks[i])
                    : (i == 0 ? 0 : DEFAULT_HOLD_TICKS);
            if (i > 0)
            {
                appearAt += delay * 50L;
            }
            int hold = holdTicks != null && i < holdTicks.length && holdTicks[i] > 0
                    ? holdTicks[i]
                    : DEFAULT_HOLD_TICKS;
            PENDING.add(new PendingLine(appearAt, key, hold));
        }
    }

    /** 淡出清空当前槽与未播队列。 */
    public static void fadeAll()
    {
        PENDING.clear();
        long now = System.currentTimeMillis();
        for (Slot slot : SLOTS)
        {
            if (!slot.isFading())
            {
                slot.beginFade(now);
            }
        }
    }

    public static void clearImmediate()
    {
        PENDING.clear();
        SLOTS.clear();
        lastPresentMillis = Long.MIN_VALUE / 4;
        QuestDialogVoice.stop();
    }

    public static boolean isActive()
    {
        return !PENDING.isEmpty() || !SLOTS.isEmpty();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END)
        {
            return;
        }
        if (PENDING.isEmpty() && SLOTS.isEmpty())
        {
            return;
        }
        long now = System.currentTimeMillis();
        flushPending(now);
        tickSlots(now);
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END || SLOTS.isEmpty())
        {
            return;
        }
        if (QuestBlackoutOverlay.isActive() || QuestCinematicLetterboxOverlay.isActive())
        {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null)
        {
            return;
        }

        long now = System.currentTimeMillis();
        flushPending(now);
        tickSlots(now);
        if (SLOTS.isEmpty())
        {
            return;
        }

        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        Font font = minecraft.font;
        int lineStep = font.lineHeight + LINE_GAP;
        int bottomY = height - MARGIN_BOTTOM - font.lineHeight;

        RenderSystem.backupProjectionMatrix();
        Matrix4f ortho = new Matrix4f().setOrtho(0.0F, (float) width, (float) height, 0.0F, 1000.0F, 3000.0F);
        RenderSystem.setProjectionMatrix(ortho, VertexSorting.ORTHOGRAPHIC_Z);

        PoseStack modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        modelView.setIdentity();
        modelView.translate(0.0D, 0.0D, 1000.0D - 3000.0D);
        RenderSystem.applyModelViewMatrix();

        GuiGraphics graphics = new GuiGraphics(minecraft, minecraft.renderBuffers().bufferSource());

        // 最多两行：皆从底槽起算，被顶上去的槽用 promoteOffset 上移一行
        int count = Math.min(MAX_SLOTS, SLOTS.size());
        int start = SLOTS.size() - count;
        for (int i = start; i < SLOTS.size(); i++)
        {
            Slot slot = SLOTS.get(i);
            float alpha = slot.alpha(now);
            if (alpha <= 0.01F)
            {
                continue;
            }
            float floatIn = (1.0F - smoothStep(slot.fadeInProgress(now))) * FLOAT_IN_PX;
            float promote = slot.promoteOffset(now, lineStep);
            int y = Math.round(bottomY + floatIn - promote);
            Component text = QuestDialogCaptionStyle.styleFull(
                    Component.translatable(slot.key).getString());
            int textW = font.width(text);
            int x = (width - textW) / 2;
            int color = withAlpha(0xFFFFFF, alpha);
            graphics.drawString(font, text, x, y, color, true);
        }

        graphics.flush();

        modelView.popPose();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.restoreProjectionMatrix();
    }

    private static void flushPending(long now)
    {
        if (PENDING.isEmpty())
        {
            return;
        }
        Iterator<PendingLine> it = PENDING.iterator();
        while (it.hasNext())
        {
            PendingLine pending = it.next();
            if (pending.appearAtMillis > now)
            {
                break;
            }
            it.remove();
            present(pending.key, pending.holdTicks, now);
        }
    }

    private static void present(String key, int holdTicks, long now)
    {
        long gap = lastPresentMillis <= Long.MIN_VALUE / 8
                ? Long.MAX_VALUE
                : now - lastPresentMillis;
        boolean simultaneous = gap < SIMULTANEOUS_GAP_TICKS * 50L;
        lastPresentMillis = now;

        QuestDialogVoice.playCaption(key);

        if (SLOTS.isEmpty())
        {
            SLOTS.add(new Slot(key, now, holdTicks));
            return;
        }

        pruneDead(now);

        if (simultaneous && SLOTS.size() == 1 && !SLOTS.get(0).isFading())
        {
            Slot older = SLOTS.get(0);
            older.markPromoted(now);
            SLOTS.add(new Slot(key, now, holdTicks));
            return;
        }

        // 接句：上一条顶上去后淡出；已满两槽时丢掉最旧顶行
        while (SLOTS.size() >= MAX_SLOTS)
        {
            SLOTS.remove(0);
        }
        if (!SLOTS.isEmpty())
        {
            Slot prev = SLOTS.get(SLOTS.size() - 1);
            if (!prev.isFading())
            {
                prev.markPromoted(now);
                prev.beginFade(now);
            }
        }
        SLOTS.add(new Slot(key, now, holdTicks));
    }

    private static void tickSlots(long now)
    {
        for (Slot slot : SLOTS)
        {
            if (!slot.isFading() && now >= slot.expireAtMillis)
            {
                slot.beginFade(now);
            }
        }
        pruneDead(now);
    }

    private static void pruneDead(long now)
    {
        SLOTS.removeIf(slot -> slot.isFading() && now - slot.fadeOutStartMillis >= FADE_OUT_MILLIS);
    }

    private static int withAlpha(int rgb, float alpha)
    {
        int a = Mth.clamp(Math.round(alpha * 255.0F), 0, 255);
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    private static final class PendingLine
    {
        final long appearAtMillis;
        final String key;
        final int holdTicks;

        PendingLine(long appearAtMillis, String key, int holdTicks)
        {
            this.appearAtMillis = appearAtMillis;
            this.key = key;
            this.holdTicks = holdTicks;
        }
    }

    private static final class Slot
    {
        final String key;
        final long bornMillis;
        final long expireAtMillis;
        long fadeOutStartMillis = -1L;
        long promoteStartMillis = -1L;
        boolean promoted;

        Slot(String key, long bornMillis, int holdTicks)
        {
            this.key = key;
            this.bornMillis = bornMillis;
            this.expireAtMillis = bornMillis + Math.max(1, holdTicks) * 50L;
        }

        boolean isFading()
        {
            return fadeOutStartMillis >= 0L;
        }

        void beginFade(long now)
        {
            if (fadeOutStartMillis < 0L)
            {
                fadeOutStartMillis = now;
            }
        }

        void markPromoted(long now)
        {
            if (!promoted)
            {
                promoted = true;
                promoteStartMillis = now;
            }
        }

        float fadeInProgress(long now)
        {
            return Mth.clamp((now - bornMillis) / (float) FADE_IN_MILLIS, 0.0F, 1.0F);
        }

        float alpha(long now)
        {
            float in = smoothStep(fadeInProgress(now));
            if (!isFading())
            {
                return in;
            }
            float outT = Mth.clamp((now - fadeOutStartMillis) / (float) FADE_OUT_MILLIS, 0.0F, 1.0F);
            return in * (1.0F - smoothStep(outT));
        }

        /** 顶上去的额外上移（像素），接句时从 0 → lineStep。 */
        float promoteOffset(long now, int lineStep)
        {
            if (!promoted || promoteStartMillis < 0L)
            {
                return 0.0F;
            }
            float t = smoothStep(Mth.clamp((now - promoteStartMillis) / (float) FADE_IN_MILLIS, 0.0F, 1.0F));
            return lineStep * t;
        }
    }

    private static float smoothStep(float t)
    {
        float x = Mth.clamp(t, 0.0F, 1.0F);
        return x * x * (3.0F - 2.0F * x);
    }
}
