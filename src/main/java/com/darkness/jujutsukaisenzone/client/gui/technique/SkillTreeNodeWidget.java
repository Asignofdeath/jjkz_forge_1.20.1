package com.darkness.jujutsukaisenzone.client.gui.technique;

import com.lowdragmc.lowdraglib.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.lowdraglib.utils.Rect;
import com.lowdragmc.lowdraglib.utils.Size;
import com.darkness.jujutsukaisenzone.client.TechniqueSkillTooltipHelper;
import com.darkness.jujutsukaisenzone.network.ModNetwork;
import com.darkness.jujutsukaisenzone.technique.development.SkillUnlockLogic;
import com.darkness.jujutsukaisenzone.technique.development.SkillTreeNodeDef;
import com.darkness.jujutsukaisenzone.technique.skillset.TechniqueSpecialMove;
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

    /** 点选高亮：选中节点用金色，直连节点用青色，均为「本体外一圈辉光 + 描边换色」。 */
    private static final int HIGHLIGHT_PADDING = 3;
    private static final int SELECTED_BORDER_COLOR = 0xFFFFD24A;
    private static final int SELECTED_GLOW_COLOR = 0x66FFD24A;
    private static final int CONNECTED_BORDER_COLOR = 0xFF7FE3FF;
    private static final int CONNECTED_GLOW_COLOR = 0x447FE3FF;

    private final String techniqueId;
    private final SkillTreeNodeDef nodeDef;
    private SkillTreeHighlight highlight = SkillTreeHighlight.NONE;
    private java.util.function.Consumer<String> onNodeClicked;

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

    /** 由画布注入当前点选高亮。 */
    public void setHighlight(SkillTreeHighlight highlight)
    {
        this.highlight = highlight == null ? SkillTreeHighlight.NONE : highlight;
    }

    /** 由画布注入点选回调；点击本节点时上报自己的 nodeId。 */
    public void setOnNodeClicked(java.util.function.Consumer<String> onNodeClicked)
    {
        this.onNodeClicked = onNodeClicked;
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
        boolean selected = highlight.isSelected(nodeDef.nodeId());
        boolean connected = highlight.isConnected(nodeDef.nodeId());
        Position pos = getSelfPosition();

        // 辉光先画：外扩一圈的圆角框，随后节点本体覆盖内圈，只留下外缘的辉光。
        if (selected || connected)
        {
            DrawerHelper.drawFilledFrameRoundBox(
                    graphics,
                    Rect.ofRelative(pos.x - HIGHLIGHT_PADDING, WIDTH + HIGHLIGHT_PADDING * 2,
                            pos.y - HIGHLIGHT_PADDING, HEIGHT + HIGHLIGHT_PADDING * 2),
                    2.0F,
                    new Vector4f(8, 8, 8, 8),
                    selected ? SELECTED_GLOW_COLOR : CONNECTED_GLOW_COLOR,
                    selected ? SELECTED_BORDER_COLOR : CONNECTED_BORDER_COLOR
            );
        }

        int borderColor = selected ? SELECTED_BORDER_COLOR
                : connected ? CONNECTED_BORDER_COLOR
                : TechniqueSkillTreeViewHelper.getBorderColor(state, nodeDef);
        float borderWidth = selected ? 3.0F : connected ? 2.4F : 1.5F;

        DrawerHelper.drawFilledFrameRoundBox(
                graphics,
                Rect.ofRelative(pos.x, WIDTH, pos.y, HEIGHT),
                borderWidth,
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

        if (nodeDef.isInfoNode())
        {
            Component title = TechniqueSkillTreeViewHelper.getDisplayName(player, techniqueId, nodeDef);
            Component body = Component.translatable(nodeDef.infoDescriptionKey()).withStyle(ChatFormatting.GRAY);
            setHoverTooltips(
                    title.copy().withStyle(ChatFormatting.AQUA),
                    body);
            return;
        }

        if (nodeDef.isSpecialMoveNode())
        {
            buildSpecialMoveTooltip(player, state);
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

    /**
     * 特别招式节点的悬浮提示：标注「特别招式」与触发方式，并说明数值 / 解锁继承自原技能。
     */
    private void buildSpecialMoveTooltip(Player player, TechniqueSkillTreeViewHelper.NodeVisualState state)
    {
        Component title = TechniqueSkillTreeViewHelper.getDisplayName(player, techniqueId, nodeDef);
        TechniqueSpecialMove move = nodeDef.specialMove();
        boolean unlocked = state == TechniqueSkillTreeViewHelper.NodeVisualState.SKILL_UNLOCKED;

        if (move == null || !unlocked)
        {
            setHoverTooltips(
                    title.copy().withStyle(ChatFormatting.DARK_GRAY),
                    Component.literal(TechniqueSkillTreeViewHelper.getSubtitle(player, techniqueId, nodeDef, state))
                            .withStyle(ChatFormatting.GRAY));
            return;
        }

        setHoverTooltips(
                title.copy().withStyle(ChatFormatting.AQUA),
                Component.translatable("gui.jujutsukaisenzone.skill_tree.special_move_tag")
                        .withStyle(ChatFormatting.LIGHT_PURPLE),
                Component.translatable("gui.jujutsukaisenzone.skill_tree.special_move_trigger", move.modifierLabel())
                        .withStyle(ChatFormatting.GRAY),
                Component.translatable("gui.jujutsukaisenzone.skill_tree.special_move_inherit",
                                TechniqueSkillTreeViewHelper.baseSkillDisplayName(techniqueId, move.baseSkillId()))
                        .withStyle(ChatFormatting.DARK_GRAY));
    }

    private void clearHoverTooltips()
    {
        setHoverTooltips(List.of());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if (button != 0 || !isMouseOverElement(mouseX, mouseY))
        {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        // 技能 / 特别招式 / 引用 / 说明节点一律响应点选高亮（再次点击同一节点 = 取消）。
        if (onNodeClicked != null)
        {
            onNodeClicked.accept(nodeDef.nodeId());
        }

        // 特别招式随原技能解锁、引用与说明节点没有解锁入口：点选后即结束。
        if (nodeDef.isReferenceNode() || nodeDef.isInfoNode() || nodeDef.isSpecialMoveNode())
        {
            return true;
        }

        Player player = TechniqueSkillTreeViewHelper.localPlayer();
        if (player == null)
        {
            return true;
        }

        TechniqueSkillTreeViewHelper.NodeVisualState state =
                TechniqueSkillTreeViewHelper.getVisualState(player, techniqueId, nodeDef);
        if (state == TechniqueSkillTreeViewHelper.NodeVisualState.SKILL_CAN_UNLOCK)
        {
            ModNetwork.sendUnlockSkillRequest(techniqueId, nodeDef.nodeId());
        }
        return true;
    }
}
