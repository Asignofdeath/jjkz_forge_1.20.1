package com.darkness.jujutsukaisenzone.client;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 王者荣耀式分段格条：替换原版心形血条，固定 91px 宽（原版一半），每格 10 HP（最多 20 格）。
 */
public final class CustomSegmentedHealthHud
{
    private static final ResourceLocation VANILLA_GUI_ICONS = ResourceLocation.fromNamespaceAndPath(
            "minecraft",
            "textures/gui/icons.png"
    );
    private static final int HEART_ICON_SRC_SIZE = 9;
    /** 略小于原版 9×9，作为血条左侧标识 */
    private static final int HEART_ICON_SIZE = 8;
    private static final int HEART_ICON_GAP = 2;
    /** 与 {@link net.minecraft.client.gui.Gui.HeartType#CONTAINER} 满心 u 一致 */
    private static final int HEART_U_CONTAINER = 16;
    /** 与 {@link net.minecraft.client.gui.Gui.HeartType#NORMAL} 满心 u 一致 */
    private static final int HEART_U_NORMAL = 52;
    private static final int HEART_U_POISONED = 88;
    private static final int HEART_U_WITHERED = 124;
    private static final int HEART_U_FROZEN = 178;

    private CustomSegmentedHealthHud()
    {
    }

    static boolean shouldRender(Minecraft minecraft)
    {
        Player player = minecraft.player;
        if (player == null || minecraft.options.hideGui)
        {
            return false;
        }
        return !player.isSpectator() && !player.isCreative();
    }

    static void draw(GuiGraphics graphics, int screenWidth, int screenHeight, Player player)
    {
        float health = player.getHealth();
        float maxHealth = player.getMaxHealth();
        float absorption = player.getAbsorptionAmount();
        if (maxHealth <= 0.0F)
        {
            return;
        }

        float damageBuffer = DamageBufferTracker.getDisplayHealth(player);

        int segments = Math.max(1, (int) Math.ceil(maxHealth / Renderer.BASE_SEGMENT_HP));
        float segmentHp = Renderer.BASE_SEGMENT_HP;
        if (segments > Renderer.MAX_SEGMENTS)
        {
            segments = Renderer.MAX_SEGMENTS;
            segmentHp = maxHealth / Renderer.MAX_SEGMENTS;
        }

        float cellWidth = Renderer.BAR_WIDTH / (float) segments;
        float filledHealthSegments = health / segmentHp;
        float filledShieldSegments = absorption / segmentHp;
        float healthRatio = health / maxHealth;
        int healthColor = healthColorForRatio(healthRatio);

        int barX = screenWidth / 2 - 91;
        int barY = screenHeight - Renderer.VANILLA_LEFT_HEIGHT_BASE;

        drawHeartIcon(graphics, barX, barY, player);

        // 削角边框 + 削角背景（drawBorder 内层已填充背景色）
        drawBorder(graphics, barX, barY);

        for (int i = 0; i < segments; i++)
        {
            int cellX = barX + Math.round(i * cellWidth);
            int cellW = Math.round((i + 1) * cellWidth) - Math.round(i * cellWidth);
            if (cellW <= 0)
            {
                continue;
            }

            float healthFill = Mth.clamp(filledHealthSegments - i, 0.0F, 1.0F);
            if (healthFill > 0.0F)
            {
                int fillW = Math.max(1, Math.round(cellW * healthFill));
                graphics.fill(cellX, barY, cellX + fillW, barY + Renderer.BAR_HEIGHT, healthColor);
            }

            float totalFill = Mth.clamp(filledHealthSegments + filledShieldSegments - i, 0.0F, 1.0F);
            float shieldFill = totalFill - healthFill;
            if (shieldFill > 0.0F)
            {
                int shieldStart = cellX + Math.round(cellW * healthFill);
                int shieldW = Math.max(1, Math.round(cellW * shieldFill));
                graphics.fill(shieldStart, barY, shieldStart + shieldW, barY + Renderer.BAR_HEIGHT, Renderer.COLOR_SHIELD);
            }
        }

        for (int i = 1; i < segments; i++)
        {
            int lineX = barX + Math.round(i * cellWidth);
            graphics.fill(lineX, barY, lineX + 1, barY + Renderer.BAR_HEIGHT, Renderer.COLOR_GRID);
        }

        drawDamageBufferBar(graphics, barX, barY, health, damageBuffer, maxHealth);
    }

    /** 血条左侧：原版 icons.png 心形（含容器底与状态对应贴图） */
    private static void drawHeartIcon(GuiGraphics graphics, int barX, int barY, Player player)
    {
        int iconX = barX - HEART_ICON_GAP - HEART_ICON_SIZE;
        int iconY = barY + (Renderer.BAR_HEIGHT - HEART_ICON_SIZE) / 2;
        int v = player.level().getLevelData().isHardcore() ? 45 : 0;
        int heartU = heartTextureUForPlayer(player);

        RenderSystem.enableBlend();
        blitVanillaHeart(graphics, iconX, iconY, v, HEART_U_CONTAINER);
        blitVanillaHeart(graphics, iconX, iconY, v, heartU);
    }

    /** 与原版 {@code Gui.HeartType.forPlayer} 一致的心形 u 坐标 */
    private static int heartTextureUForPlayer(Player player)
    {
        if (player.hasEffect(MobEffects.POISON))
        {
            return HEART_U_POISONED;
        }
        if (player.hasEffect(MobEffects.WITHER))
        {
            return HEART_U_WITHERED;
        }
        if (player.isFullyFrozen())
        {
            return HEART_U_FROZEN;
        }
        return HEART_U_NORMAL;
    }

    private static void blitVanillaHeart(GuiGraphics graphics, int x, int y, int v, int u)
    {
        graphics.blit(
                VANILLA_GUI_ICONS,
                x,
                y,
                HEART_ICON_SIZE,
                HEART_ICON_SIZE,
                (float) u,
                (float) v,
                HEART_ICON_SRC_SIZE,
                HEART_ICON_SRC_SIZE,
                256,
                256
        );
    }

    /** 血条外缘削角细框：先画外边框色削角矩形，再叠内层背景色形成 1px 边框 */
    private static void drawBorder(GuiGraphics graphics, int barX, int barY)
    {
        int r = Renderer.CORNER_RADIUS;
        int outerX1 = barX - 1;
        int outerY1 = barY - 1;
        int outerX2 = barX + Renderer.BAR_WIDTH + 1;
        int outerY2 = barY + Renderer.BAR_HEIGHT + 1;

        // 外框（削角白色）
        fillRoundedRect(graphics, outerX1, outerY1, outerX2, outerY2, r, Renderer.COLOR_BORDER);
        // 内层挖空（削角背景色，r-1 形成 1px 边框线）
        fillRoundedRect(graphics, barX, barY, barX + Renderer.BAR_WIDTH, barY + Renderer.BAR_HEIGHT, r - 1, Renderer.COLOR_BG);
    }

    /** 削角矩形：逐行缩进切掉四角。r≤0 退化为普通 fill。 */
    private static void fillRoundedRect(GuiGraphics graphics, int x1, int y1, int x2, int y2, int r, int color)
    {
        if (r <= 0 || x2 <= x1 || y2 <= y1)
        {
            graphics.fill(x1, y1, x2, y2, color);
            return;
        }
        int w = x2 - x1;
        int h = y2 - y1;
        r = Math.min(r, Math.min(w / 2, h / 2));

        // 顶部 r 行：每行左侧缩进 (r-1-row)、右侧缩进 (r-1-row)
        for (int row = 0; row < r; row++)
        {
            int inset = r - 1 - row;
            graphics.fill(x1 + inset, y1 + row, x2 - inset, y1 + row + 1, color);
        }
        // 中间行：全宽
        graphics.fill(x1, y1 + r, x2, y2 - r, color);
        // 底部 r 行：对称缩进（row=0 对应最底行 y2-1）
        for (int row = 0; row < r; row++)
        {
            int inset = r - 1 - row;
            int y = y2 - 1 - row;
            graphics.fill(x1 + inset, y, x2 - inset, y + 1, color);
        }
    }

    /** 在当前血量右侧绘制黄色缓冲条（覆盖于格线之下，避免被分段线切碎） */
    private static void drawDamageBufferBar(
            GuiGraphics graphics,
            int barX,
            int barY,
            float health,
            float bufferHealth,
            float maxHealth
    )
    {
        if (bufferHealth <= health + 0.05F)
        {
            return;
        }

        float clampedBuffer = Mth.clamp(bufferHealth, health, maxHealth);
        int healthEndX = barX + Math.round(Renderer.BAR_WIDTH * (health / maxHealth));
        int bufferEndX = barX + Math.round(Renderer.BAR_WIDTH * (clampedBuffer / maxHealth));
        if (bufferEndX <= healthEndX)
        {
            bufferEndX = healthEndX + 1;
        }
        bufferEndX = Math.min(barX + Renderer.BAR_WIDTH, bufferEndX);

        int innerY = barY + 1;
        int innerH = Renderer.BAR_HEIGHT - 2;
        graphics.fill(healthEndX, innerY, bufferEndX, innerY + innerH, Renderer.COLOR_DAMAGE_BUFFER);
        // 左缘高亮，让缓冲条与当前血量分界更清晰
        graphics.fill(healthEndX, innerY, healthEndX + 1, innerY + innerH, Renderer.COLOR_DAMAGE_BUFFER_EDGE);
    }

    /** 客户端每 tick 追踪扣血，供缓冲条使用 */
    private static final class DamageBufferTracker
    {
        private static float bufferHealth = -1.0F;
        private static float lastHealth = -1.0F;
        private static float lastMaxHealth = -1.0F;

        private DamageBufferTracker()
        {
        }

        static void tick(Player player)
        {
            float health = player.getHealth();
            float maxHealth = player.getMaxHealth();

            if (lastHealth < 0.0F || Math.abs(lastMaxHealth - maxHealth) > 0.001F)
            {
                resetTo(health, maxHealth);
                return;
            }

            if (health > lastHealth + 0.001F)
            {
                bufferHealth = health;
            }
            else if (health + 0.001F < lastHealth)
            {
                bufferHealth = Math.max(bufferHealth, lastHealth);
            }

            lastHealth = health;
            lastMaxHealth = maxHealth;

            if (bufferHealth > health)
            {
                bufferHealth = Math.max(health, bufferHealth - Renderer.DAMAGE_BUFFER_DECAY_PER_TICK);
            }
        }

        static float getDisplayHealth(Player player)
        {
            if (bufferHealth < 0.0F)
            {
                return player.getHealth();
            }
            return bufferHealth;
        }

        private static void resetTo(float health, float maxHealth)
        {
            bufferHealth = health;
            lastHealth = health;
            lastMaxHealth = maxHealth;
        }
    }

    private static int healthColorForRatio(float ratio)
    {
        if (ratio <= 0.2F)
        {
            return Renderer.COLOR_HP_LOW;
        }
        if (ratio <= 0.5F)
        {
            return Renderer.COLOR_HP_MID;
        }
        return Renderer.COLOR_HP_HIGH;
    }

    @Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class Renderer
    {
        static final int BAR_WIDTH = 91;
        static final int BAR_HEIGHT = 6;
        static final int CORNER_RADIUS = 2;
        static final float BASE_SEGMENT_HP = 10.0F;
        static final int MAX_SEGMENTS = 20;
        /** 原版 {@code ForgeGui.leftHeight} 初始值，第一行心形 Y = height - 39 */
        static final int VANILLA_LEFT_HEIGHT_BASE = 39;
        /** 单行心形占用高度，用于补偿 leftHeight 避免盔甲重叠 */
        static final int VANILLA_HEALTH_ROW_HEIGHT = 10;

        static final int COLOR_BG = 0x99000000;
        static final int COLOR_BORDER = 0xFFFFFFFF;
        static final int COLOR_GRID = 0x55FFFFFF;
        static final int COLOR_HP_HIGH = 0xFF22C55E;
        static final int COLOR_HP_MID = 0xFFF97316;
        static final int COLOR_HP_LOW = 0xFFEF4444;
        static final int COLOR_SHIELD = 0xCCFFFFFF;
        /** 扣血缓冲条：显示刚刚损失的血量，随时间缩短 */
        static final int COLOR_DAMAGE_BUFFER = 0xFFFFD700;
        static final int COLOR_DAMAGE_BUFFER_EDGE = 0xFFFFEE88;
        /** 缓冲条每游戏刻向当前血量收缩的 HP 量（20 tps 下约 24 HP/秒） */
        static final float DAMAGE_BUFFER_DECAY_PER_TICK = 1.2F;

        private Renderer()
        {
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event)
        {
            if (event.phase != TickEvent.Phase.END)
            {
                return;
            }
            Minecraft minecraft = Minecraft.getInstance();
            if (!shouldRender(minecraft))
            {
                return;
            }
            DamageBufferTracker.tick(minecraft.player);
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void onPlayerHealthOverlayPre(RenderGuiOverlayEvent.Pre event)
        {
            if (!event.getOverlay().id().equals(VanillaGuiOverlay.PLAYER_HEALTH.id()))
            {
                return;
            }

            Minecraft minecraft = Minecraft.getInstance();
            if (!shouldRender(minecraft))
            {
                return;
            }

            event.setCanceled(true);
            if (minecraft.gui instanceof ForgeGui forgeGui)
            {
                forgeGui.leftHeight += VANILLA_HEALTH_ROW_HEIGHT;
            }
        }

        @SubscribeEvent
        public static void onRenderGuiPost(RenderGuiEvent.Post event)
        {
            Minecraft minecraft = Minecraft.getInstance();
            if (!shouldRender(minecraft))
            {
                return;
            }
            if (!(minecraft.gui instanceof ForgeGui forgeGui))
            {
                return;
            }

            forgeGui.setupOverlayRenderState(true, false);
            draw(
                    event.getGuiGraphics(),
                    event.getWindow().getGuiScaledWidth(),
                    event.getWindow().getGuiScaledHeight(),
                    minecraft.player
            );
        }
    }
}
