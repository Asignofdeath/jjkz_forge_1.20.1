package com.darkness.jujutsukaisenzone.client;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.compat.EpicFightCompat;
import com.darkness.jujutsukaisenzone.event.TechniqueSkillKeyEvents;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import com.darkness.jujutsukaisenzone.technique.development.SkillUnlockLogic;
import com.darkness.jujutsukaisenzone.technique.skillset.TechniqueSkill;
import com.darkness.jujutsukaisenzone.technique.skillset.TechniqueSkillSetRegistry;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 战斗模式 HUD：在物品栏左侧绘制当前组的 3 个技能图标（带背景框）。
 * <p>
 * 挂在 {@link VanillaGuiOverlay#PLAYER_LIST} overlay 的 Post 阶段渲染，
 * 位于快捷栏左侧，不影响现有 HUD 布局。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class SkillSlotHudOverlay
{
    private static final ResourceLocation FRAME_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            JuJutsuKaisenZone.MODID, "textures/gui/skill_display_frame.png");
    private static final int FRAME_SIZE = 24;
    private static final int SLOTS_PER_GROUP = 3;
    /** 技能图标在框内的尺寸（居中） */
    private static final int ICON_INNER_SIZE = 16;
    private static final int ICON_INNER_OFFSET = (FRAME_SIZE - ICON_INNER_SIZE) / 2;
    /** 框间距 */
    private static final int FRAME_GAP = 1;

    private SkillSlotHudOverlay()
    {
    }

    @SubscribeEvent
    public static void onRenderGuiOverlayPost(RenderGuiOverlayEvent.Post event)
    {
        if (!event.getOverlay().equals(VanillaGuiOverlay.HOTBAR.type()))
        {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui || mc.screen != null)
        {
            return;
        }
        if (!EpicFightCompat.isBattleMode(player))
        {
            return;
        }

        String techniqueId = PlayerTechniqueData.getInnateTechnique(player);
        if (techniqueId.isBlank() || PlayerTechniqueData.TECHNIQUE_NONE.equals(techniqueId))
        {
            return;
        }

        var setOpt = TechniqueSkillSetRegistry.get(techniqueId);
        if (setOpt.isEmpty()) return;

        int group = TechniqueSkillKeyEvents.getCurrentSkillSlotGroup();
        int screenWidth = event.getWindow().getGuiScaledWidth();
        int screenHeight = event.getWindow().getGuiScaledHeight();

        GuiGraphics graphics = event.getGuiGraphics();
        RenderSystem.enableBlend();

        // 在快捷栏左侧横向排列 3 个图标
        int hotbarLeft = screenWidth / 2 - 91;
        int startX = hotbarLeft - (FRAME_SIZE + FRAME_GAP) * SLOTS_PER_GROUP;
        int y = screenHeight - 22 + (22 - FRAME_SIZE) / 2; // 与22px高的快捷栏垂直居中

        for (int i = 0; i < SLOTS_PER_GROUP; i++)
        {
            int x = startX + i * (FRAME_SIZE + FRAME_GAP);
            int slot = group * SLOTS_PER_GROUP + i + 1;
            String skillId = ClientSkillBarCache.getSkillId(techniqueId, slot);

            // 画背景框
            graphics.blit(FRAME_TEXTURE, x, y, 0, 0, FRAME_SIZE, FRAME_SIZE, FRAME_SIZE, FRAME_SIZE);

            // 画技能图标
            if (!skillId.isEmpty())
            {
                TechniqueSkill skill = SkillUnlockLogic.findSkillDefinition(techniqueId, skillId).orElse(null);
                if (skill != null && SkillUnlockLogic.isSkillUnlocked(player, techniqueId, skillId, skill))
                {
                    ResourceLocation icon = iconResource(skill.iconPath());
                    graphics.blit(icon, x + ICON_INNER_OFFSET, y + ICON_INNER_OFFSET,
                            0, 0, ICON_INNER_SIZE, ICON_INNER_SIZE, ICON_INNER_SIZE, ICON_INNER_SIZE);
                }
            }
        }

        RenderSystem.disableBlend();
    }

    private static ResourceLocation iconResource(String path)
    {
        if (path.contains(":"))
        {
            return ResourceLocation.parse(path);
        }
        return ResourceLocation.fromNamespaceAndPath(JuJutsuKaisenZone.MODID, path);
    }
}
