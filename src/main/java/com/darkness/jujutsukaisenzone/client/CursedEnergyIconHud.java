package com.darkness.jujutsukaisenzone.client;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.compat.EpicFightCompat;
import com.darkness.jujutsukaisenzone.player.PlayerCursedEnergyData;
import com.darkness.jujutsukaisenzone.player.PlayerFactionData;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 咒力图标条：战斗模式下替换原版饱食度位置；非战斗模式不显示。
 * <p>
 * 须在 {@link RenderGuiOverlayEvent.Pre} 中绘制：取消 Pre 后 Forge 不会派发 Post。
 */
public final class CursedEnergyIconHud
{
    public static final ResourceLocation CURSED_ICON_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            JuJutsuKaisenZone.MODID,
            "textures/gui/cursed_icon.png"
    );

    private static final int TEXTURE_SIZE = 48;
    private static final int ICON_SRC_SIZE = 16;
    private static final int ICON_DISPLAY_SIZE = 12;
    private static final int ICON_COUNT = 10;
    /** 略大于原版饱食度步进，以适配放大后的图标 */
    private static final int VANILLA_FOOD_ICON_STEP = 7;
    /** 原版 {@code ForgeGui.renderFood} 右侧锚点：{@code width / 2 + 91} */
    private static final int VANILLA_FOOD_ANCHOR_OFFSET = 91;
    /** 与 {@link #ICON_DISPLAY_SIZE} 一致，供 HUD 行高计算 */
    private static final int VANILLA_FOOD_ROW_HEIGHT = ICON_DISPLAY_SIZE;

    private static final int U_EMPTY = 0;
    private static final int U_FULL = 16;
    private static final int U_HALF = 32;

    private CursedEnergyIconHud()
    {
    }

    /** 与原版 {@code VanillaGuiOverlay.FOOD_LEVEL} 相同的生存 HUD 前置条件 */
    static boolean passesFoodOverlayGate(Minecraft minecraft)
    {
        Player player = minecraft.player;
        if (player == null || minecraft.options.hideGui)
        {
            return false;
        }
        if (!minecraft.gameMode.canHurtPlayer())
        {
            return false;
        }
        Entity vehicle = player.getVehicle();
        return vehicle == null || !vehicle.showVehicleHealth();
    }

    static boolean isBattleModeHudActive(Minecraft minecraft)
    {
        Player player = minecraft.player;
        if (player == null)
        {
            return false;
        }
        if (player.isSpectator() || player.isCreative())
        {
            return false;
        }
        return EpicFightCompat.isBattleMode(player);
    }

    static void draw(GuiGraphics graphics, int screenWidth, int screenHeight, int top, Player player)
    {
        String factionId = PlayerFactionData.getFactionId(player);
        if (PlayerFactionData.FACTION_CIVILIAN.equals(factionId))
        {
            return;
        }

        int rowV = rowVForFaction(factionId);
        if (rowV < 0)
        {
            return;
        }

        int current = PlayerCursedEnergyData.getCurrent(player);
        int max = PlayerCursedEnergyData.getMax(player);
        if (max <= 0)
        {
            return;
        }

        int virtualLevel = Mth.clamp(current * 20 / max, 0, 20);
        int anchorRight = screenWidth / 2 + VANILLA_FOOD_ANCHOR_OFFSET;

        RenderSystem.enableBlend();
        for (int i = 0; i < ICON_COUNT; i++)
        {
            int x = anchorRight - i * VANILLA_FOOD_ICON_STEP - ICON_DISPLAY_SIZE;
            // 右侧图标对应较高 idx，咒力减少时从右往左空
            int idx = (ICON_COUNT - 1 - i) * 2 + 1;

            blitIcon(graphics, x, top, U_EMPTY, rowV);

            if (idx < virtualLevel)
            {
                blitIcon(graphics, x, top, U_FULL, rowV);
            }
            else if (idx == virtualLevel && virtualLevel % 2 == 1)
            {
                blitIcon(graphics, x, top, U_HALF, rowV);
            }
        }
        RenderSystem.disableBlend();
    }

    private static void blitIcon(GuiGraphics graphics, int x, int y, int u, int v)
    {
        graphics.blit(
                CURSED_ICON_TEXTURE,
                x,
                y,
                ICON_DISPLAY_SIZE,
                ICON_DISPLAY_SIZE,
                (float) u,
                (float) v,
                ICON_SRC_SIZE,
                ICON_SRC_SIZE,
                TEXTURE_SIZE,
                TEXTURE_SIZE
        );
    }

    /** 咒术师 / 诅咒师 / 咒灵 对应纹理行 v=0/16/32 */
    private static int rowVForFaction(String factionId)
    {
        return switch (factionId)
        {
            case PlayerFactionData.FACTION_SORCERER -> 0;
            case PlayerFactionData.FACTION_CURSE_USER -> 16;
            case PlayerFactionData.FACTION_CURSE_SPIRIT -> 32;
            default -> -1;
        };
    }

    @Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class Renderer
    {
        private Renderer()
        {
        }

        @SubscribeEvent
        public static void onRenderGuiOverlayPre(RenderGuiOverlayEvent.Pre event)
        {
            if (!event.getOverlay().equals(VanillaGuiOverlay.FOOD_LEVEL.type()))
            {
                return;
            }

            Minecraft minecraft = Minecraft.getInstance();
            if (!passesFoodOverlayGate(minecraft) || !isBattleModeHudActive(minecraft))
            {
                return;
            }

            // 阻止原版 renderFood；取消后 Forge 不会调用 post()，故咒力条必须写在本 Pre 内
            event.setCanceled(true);

            if (!(minecraft.gui instanceof ForgeGui forgeGui))
            {
                return;
            }

            forgeGui.setupOverlayRenderState(true, false);

            int screenWidth = event.getWindow().getGuiScaledWidth();
            int screenHeight = event.getWindow().getGuiScaledHeight();
            int top = screenHeight - forgeGui.rightHeight;

            draw(event.getGuiGraphics(), screenWidth, screenHeight, top, minecraft.player);
            forgeGui.rightHeight += VANILLA_FOOD_ROW_HEIGHT;
        }
    }
}
