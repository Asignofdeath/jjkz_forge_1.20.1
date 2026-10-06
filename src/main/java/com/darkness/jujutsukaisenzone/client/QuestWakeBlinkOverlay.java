package com.darkness.jujutsukaisenzone.client;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.registry.ModSounds;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/**
 * 主线 1.3：昏迷黑屏之后、看到五条悟背影之前的「醒来眨眼」。
 * <p>
 * 上下两片眼皮，约 {@link #DURATION_MILLIS}。节奏是短眨、再眨、合住、最后慢慢睁开
 * （不是旧版「全黑 400ms + 三次匀速眨眼」）。
 * <p>
 * 眼皮覆盖整个屏幕（不留字幕带），合拢时全黑、不露画面。
 * <p>
 * 绘制顺序排在 {@link QuestCodCaptionOverlay} <b>之前</b>（{@code HIGHEST}），
 * 让字幕最后画、压在眼皮之上，对白不被黑幕吃掉。
 * <p>
 * 绘制路径照抄 {@link QuestCinematicLetterboxOverlay}：{@link TickEvent.RenderTickEvent} {@code END}
 * + 自建 {@link GuiGraphics}。本层与背影镜 CMDCam <b>同拍</b>启动，而运镜期间 {@code hideGui=true}
 * 会跳过 {@code Gui.render} ⇒ {@code RenderGuiEvent} 不触发，挂 HUD 事件必然整段丢失。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class QuestWakeBlinkOverlay
{
    public static final long DURATION_MILLIS = 4000L;
    private static final int LID_COLOR = 0xFF000000;

    private static boolean active;
    private static long startMillis;

    private QuestWakeBlinkOverlay()
    {
    }

    public static void play()
    {
        active = true;
        startMillis = System.currentTimeMillis();
        playLocal(ModSounds.QUEST_TINNITUS.get());
        playLocal(ModSounds.QUEST_NOTINVADE_OPEN.get());
    }

    private static void playLocal(SoundEvent sound)
    {
        if (sound == null)
        {
            return;
        }
        Minecraft.getInstance().getSoundManager().play(new SimpleSoundInstance(
                sound.getLocation(),
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

    public static void hide()
    {
        active = false;
    }

    public static boolean isActive()
    {
        return active;
    }

    /**
     * 画在 COD 字幕之前（{@code HIGHEST} 先执行），让字幕随后覆盖在黑幕之上。
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderTick(TickEvent.RenderTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END || !active)
        {
            return;
        }
        long elapsed = System.currentTimeMillis() - startMillis;
        if (elapsed >= DURATION_MILLIS)
        {
            active = false;
            return;
        }
        float close = lidClose(elapsed / (float) DURATION_MILLIS);
        if (close <= 0.01F)
        {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        int eyeBottom = height;
        int lid = Math.round(close * (eyeBottom * 0.5F));
        if (lid <= 0)
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
        graphics.fill(0, 0, width, lid, LID_COLOR);
        graphics.fill(0, eyeBottom - lid, width, eyeBottom, LID_COLOR);
        graphics.flush();

        modelView.popPose();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.restoreProjectionMatrix();
    }

    /**
     * 0 = 全睁，1 = 眼皮合拢（只盖 COD 带以上）。
     * t∈[0,1]：全闭 → 短眨 → 再眨 → 合住 → 慢睁。
     */
    private static float lidClose(float t)
    {
        float x = Mth.clamp(t, 0.0F, 1.0F);
        if (x < 0.10F)
        {
            return 1.0F;
        }
        if (x < 0.20F)
        {
            return blinkPulse((x - 0.10F) / 0.10F, 0.32F);
        }
        if (x < 0.35F)
        {
            return blinkPulse((x - 0.20F) / 0.15F, 0.48F);
        }
        if (x < 0.60F)
        {
            return 1.0F;
        }
        return 1.0F - smoothStep((x - 0.60F) / 0.40F);
    }

    /** 从全闭睁到 {@code open} 再合上。 */
    private static float blinkPulse(float u, float open)
    {
        float p = Mth.clamp(u, 0.0F, 1.0F);
        if (p < 0.45F)
        {
            return Mth.lerp(smoothStep(p / 0.45F), 1.0F, open);
        }
        return Mth.lerp(smoothStep((p - 0.45F) / 0.55F), open, 1.0F);
    }

    private static float smoothStep(float t)
    {
        float x = Mth.clamp(t, 0.0F, 1.0F);
        return x * x * (3.0F - 2.0F * x);
    }
}
