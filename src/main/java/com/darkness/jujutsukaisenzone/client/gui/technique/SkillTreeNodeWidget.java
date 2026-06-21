package com.darkness.jujutsukaisenzone.client.gui.technique;

import com.lowdragmc.lowdraglib.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.lowdraglib.utils.Rect;
import com.lowdragmc.lowdraglib.utils.Size;
import com.darkness.jujutsukaisenzone.client.TechniqueSkillTooltipHelper;
import com.darkness.jujutsukaisenzone.network.ModNetwork;
import com.darkness.jujutsukaisenzone.technique.development.SkillUnlockLogic;
import com.darkness.jujutsukaisenzone.technique.development.SkillTreeNodeDef;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector4f;

import java.util.List;

/**
 * 术式开发树上的单个节点（技能或引用）。
 */
@OnlyIn(Dist.CLIENT)
public final class SkillTreeNodeWidget extends com.lowdragmc.lowdraglib.gui.widget.Widget
{
    public static final int WIDTH = 88;
    public static final int HEIGHT = 96;
    private static final int ICON_SIZE = 48;

    private final String techniqueId;
    private final SkillTreeNodeDef nodeDef;

    public SkillTreeNodeWidget(String techniqueId, SkillTreeNodeDef nodeDef)
    {
        super(new Position(0, 0), new Size(WIDTH, HEIGHT));
        this.techniqueId = techniqueId;
        this.nodeDef = nodeDef;
        setClientSideWidget();
    }

    public SkillTreeNodeDef nodeDef()
    {
        return nodeDef;
    }

    public float centerX()
    {
        return getSelfPosition().x + WIDTH * 0.5F;
    }

    public float centerY()
    {
        return getSelfPosition().y + HEIGHT * 0.5F;
    }

    @Override
    public void drawInBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks)
    {
        Player player = TechniqueSkillTreeViewHelper.localPlayer();
        if (player == null)
        {
            return;
        }

        TechniqueSkillTreeViewHelper.NodeVisualState state =
                TechniqueSkillTreeViewHelper.getVisualState(player, techniqueId, nodeDef);
        int borderColor = TechniqueSkillTreeViewHelper.getBorderColor(state);
        Position pos = getSelfPosition();
        Rect rect = Rect.ofRelative(pos.x, WIDTH, pos.y, HEIGHT);

        DrawerHelper.drawFilledFrameRoundBox(
                graphics,
                rect,
                1.5F,
                new Vector4f(8, 8, 8, 8),
                0xCC1A1A1A,
                borderColor
        );

        Component title = TechniqueSkillTreeViewHelper.getDisplayName(player, techniqueId, nodeDef);
        Font font = Minecraft.getInstance().font;
        int titleWidth = font.width(title);
        graphics.drawString(font, title, pos.x + (WIDTH - titleWidth) / 2, pos.y + 6, 0xFFFFFF, false);

        ResourceLocation icon = TechniqueSkillTreeViewHelper.getIconLocation(player, techniqueId, nodeDef);
        int iconX = pos.x + (WIDTH - ICON_SIZE) / 2;
        int iconY = pos.y + 22;
        if (icon != null)
        {
            graphics.blit(icon, iconX, iconY, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        }
        else
        {
            String shortLabel = title.getString();
            if (shortLabel.length() > 2)
            {
                shortLabel = shortLabel.substring(0, 2);
            }
            int labelWidth = font.width(shortLabel);
            graphics.drawString(font, shortLabel, iconX + (ICON_SIZE - labelWidth) / 2, iconY + 18, 0xCCCCCC, false);
        }

        String subtitle = TechniqueSkillTreeViewHelper.getSubtitle(player, techniqueId, nodeDef, state);
        int subtitleWidth = font.width(subtitle);
        graphics.drawString(font, subtitle, pos.x + (WIDTH - subtitleWidth) / 2, pos.y + HEIGHT - 14, 0xAAAAAA, false);

        super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public void updateScreen()
    {
        super.updateScreen();
        refreshHoverTooltips();
    }

    private void refreshHoverTooltips()
    {
        Player player = TechniqueSkillTreeViewHelper.localPlayer();
        if (player == null)
        {
            clearHoverTooltips();
            return;
        }

        TechniqueSkillTreeViewHelper.NodeVisualState state =
                TechniqueSkillTreeViewHelper.getVisualState(player, techniqueId, nodeDef);

        if (nodeDef.isReferenceNode())
        {
            Component title = TechniqueSkillTreeViewHelper.getDisplayName(player, techniqueId, nodeDef);
            String subtitle = TechniqueSkillTreeViewHelper.getSubtitle(player, techniqueId, nodeDef, state);
            setHoverTooltips(
                    title.copy().withStyle(ChatFormatting.GOLD),
                    Component.literal(subtitle).withStyle(ChatFormatting.GRAY));
            return;
        }

        if (!nodeDef.isSkillNode())
        {
            clearHoverTooltips();
            return;
        }

        if (state == TechniqueSkillTreeViewHelper.NodeVisualState.SKILL_LOCKED)
        {
            String subtitle = TechniqueSkillTreeViewHelper.getSubtitle(player, techniqueId, nodeDef, state);
            setHoverTooltips(
                    Component.literal("???").withStyle(ChatFormatting.DARK_GRAY),
                    Component.literal(subtitle).withStyle(ChatFormatting.GRAY));
            return;
        }

        SkillUnlockLogic.findSkillDefinition(techniqueId, nodeDef.skillId()).ifPresentOrElse(
                skill -> {
                    List<Component> tooltip = TechniqueSkillTooltipHelper.build(player, techniqueId, skill);
                    setHoverTooltips(tooltip.toArray(Component[]::new));
                },
                () -> clearHoverTooltips());
    }

    private void clearHoverTooltips()
    {
        setHoverTooltips(List.of());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if (button != 0 || !isMouseOverElement(mouseX, mouseY) || nodeDef.isReferenceNode())
        {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        Player player = TechniqueSkillTreeViewHelper.localPlayer();
        if (player == null)
        {
            return false;
        }

        TechniqueSkillTreeViewHelper.NodeVisualState state =
                TechniqueSkillTreeViewHelper.getVisualState(player, techniqueId, nodeDef);
        if (state != TechniqueSkillTreeViewHelper.NodeVisualState.SKILL_CAN_UNLOCK)
        {
            return false;
        }

        ModNetwork.sendUnlockSkillRequest(techniqueId, nodeDef.nodeId());
        return true;
    }
}
