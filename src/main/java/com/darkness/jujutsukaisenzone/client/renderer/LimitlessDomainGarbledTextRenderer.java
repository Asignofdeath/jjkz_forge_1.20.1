package com.darkness.jujutsukaisenzone.client.renderer;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 无量空处：锁定目标周围漂浮的乱码文字碎片（billboard），
 * 玩家与生物共用；与聊天栏乱码同一字符池。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class LimitlessDomainGarbledTextRenderer
{
    private static final char[] SYMBOL_CHARS = (
            "⍅⌇⍉⏚⋉⍟⎋⍜⍥⌿⍁⍂⍛⎅⌖⍢⍩⎇⍨⌾⍴⍶⋓⍫⍣⌌⌭⍊⍎⌁⌆⍼"
    ).toCharArray();
    private static final char[] MIXED_CHARS = (
            "ÃÇ¤ºªÆØßµñþæ¿¡€£¥§©®™†‡°•·¶⁄¡‹›ﬁﬂ‰‡·¶•‹›ﬁﬂ‡°·0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    ).toCharArray();
    private static final char[] CN_CHARS = (
            "鎻愬彇澶辫触锛岃绋嬪簭寮傚父鍐呭瓨涓嶈冻锛堬紵锛夊嵎璧锋潵"
    ).toCharArray();
    private static final String[] CRASH_LINES = new String[]{
            "0xDEADBEEF",
            "0xCAFEBABE",
            "[NULL]",
            "[SIGSEGV]",
            "█▓▒░",
            "0x8BADF00D"
    };

    private static final int MAX_FRAGMENTS = 120;
    private static final int BURST_MIN = 3;
    private static final int BURST_MAX = 6;
    private static final int LIFE_MIN_TICKS = 22;
    private static final int LIFE_MAX_TICKS = 38;
    /** 白色乱码文字（无量空处必中视觉）。 */
    private static final int BASE_RGB = 0xFFFFFF;
    private static final float TEXT_SCALE = 0.028F;

    private static final List<Fragment> FRAGMENTS = new ArrayList<>(MAX_FRAGMENTS);

    private LimitlessDomainGarbledTextRenderer()
    {
    }

    /** 在指定世界坐标周围刷一批短乱码碎片。 */
    public static void spawnBurstAt(double x, double y, double z)
    {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null)
        {
            return;
        }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int count = BURST_MIN + random.nextInt(BURST_MAX - BURST_MIN + 1);
        for (int i = 0; i < count; i++)
        {
            if (FRAGMENTS.size() >= MAX_FRAGMENTS)
            {
                FRAGMENTS.remove(0);
            }
            double ox = (random.nextDouble() - 0.5D) * 1.8D;
            double oy = (random.nextDouble() - 0.25D) * 1.2D;
            double oz = (random.nextDouble() - 0.5D) * 1.8D;
            double vx = (random.nextDouble() - 0.5D) * 0.035D;
            double vy = 0.018D + random.nextDouble() * 0.028D;
            double vz = (random.nextDouble() - 0.5D) * 0.035D;
            int life = LIFE_MIN_TICKS + random.nextInt(LIFE_MAX_TICKS - LIFE_MIN_TICKS + 1);
            FRAGMENTS.add(new Fragment(
                    x + ox,
                    y + oy,
                    z + oz,
                    vx, vy, vz,
                    buildShortGarbled(random),
                    life,
                    life
            ));
        }
    }

    public static void clear()
    {
        FRAGMENTS.clear();
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event)
    {
        clear();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END || FRAGMENTS.isEmpty())
        {
            return;
        }
        Iterator<Fragment> it = FRAGMENTS.iterator();
        while (it.hasNext())
        {
            Fragment f = it.next();
            f.remainingTicks--;
            if (f.remainingTicks <= 0)
            {
                it.remove();
                continue;
            }
            f.x += f.vx;
            f.y += f.vy;
            f.z += f.vz;
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event)
    {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES)
        {
            return;
        }
        if (FRAGMENTS.isEmpty())
        {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null)
        {
            return;
        }

        Camera camera = event.getCamera();
        Vec3 camPos = camera.getPosition();
        Quaternionf camRot = camera.rotation();
        PoseStack poseStack = event.getPoseStack();
        Font font = minecraft.font;
        MultiBufferSource.BufferSource buffer = minecraft.renderBuffers().bufferSource();
        float partial = event.getPartialTick();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        for (Fragment f : FRAGMENTS)
        {
            float lifeFrac = (f.remainingTicks - partial) / (float) f.maxTicks;
            if (lifeFrac <= 0.0F)
            {
                continue;
            }
            float alpha = Mth.clamp(lifeFrac, 0.0F, 1.0F);
            if (lifeFrac > 0.85F)
            {
                alpha *= (1.0F - lifeFrac) / 0.15F;
            }
            int a = Mth.clamp((int) (alpha * 255.0F), 0, 255);
            if (a <= 0)
            {
                continue;
            }
            double ix = Mth.lerp(partial, f.x - f.vx, f.x);
            double iy = Mth.lerp(partial, f.y - f.vy, f.y);
            double iz = Mth.lerp(partial, f.z - f.vz, f.z);

            poseStack.pushPose();
            poseStack.translate(ix - camPos.x, iy - camPos.y, iz - camPos.z);
            poseStack.mulPose(camRot);
            poseStack.scale(-TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);

            int color = (a << 24) | BASE_RGB;
            float x = -font.width(f.text) * 0.5F;
            font.drawInBatch(
                    f.text,
                    x,
                    0.0F,
                    color,
                    false,
                    poseStack.last().pose(),
                    buffer,
                    Font.DisplayMode.SEE_THROUGH,
                    0,
                    0xF000F0
            );
            poseStack.popPose();
        }

        buffer.endBatch();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private static String buildShortGarbled(ThreadLocalRandom random)
    {
        int style = random.nextInt(4);
        return switch (style)
        {
            case 0 -> sampleChars(SYMBOL_CHARS, 6, 12, random);
            case 1 -> sampleChars(MIXED_CHARS, 7, 14, random);
            case 2 -> CRASH_LINES[random.nextInt(CRASH_LINES.length)];
            default -> sampleChars(CN_CHARS, 4, 9, random);
        };
    }

    private static String sampleChars(char[] pool, int minLen, int maxLen, ThreadLocalRandom random)
    {
        int len = minLen + random.nextInt(maxLen - minLen + 1);
        StringBuilder builder = new StringBuilder(len);
        for (int i = 0; i < len; i++)
        {
            builder.append(pool[random.nextInt(pool.length)]);
        }
        return builder.toString();
    }

    private static final class Fragment
    {
        private double x;
        private double y;
        private double z;
        private final double vx;
        private final double vy;
        private final double vz;
        private final String text;
        private int remainingTicks;
        private final int maxTicks;

        private Fragment(
                double x, double y, double z,
                double vx, double vy, double vz,
                String text, int remainingTicks, int maxTicks)
        {
            this.x = x;
            this.y = y;
            this.z = z;
            this.vx = vx;
            this.vy = vy;
            this.vz = vz;
            this.text = text;
            this.remainingTicks = remainingTicks;
            this.maxTicks = maxTicks;
        }
    }
}
