package com.darkness.jujutsukaisenzone.client.gui;

import com.darkness.jujutsukaisenzone.network.ModNetwork;
import com.darkness.jujutsukaisenzone.player.BarrierRulesData;
import com.darkness.jujutsukaisenzone.player.PlayerReverseTechniqueData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 结界规则配置界面
 * 按 Shift+B 打开
 */
public class BarrierRulesScreen extends Screen
{
    private Checkbox blockMobSpawnCheckbox;
    private Checkbox autoRepairBarrierCheckbox;
    private final boolean initialBlockMobSpawn;
    private final boolean initialAutoRepairBarrier;
    private final boolean showAutoRepairOption;

    public BarrierRulesScreen()
    {
        super(Component.literal("结界规则配置").withStyle(ChatFormatting.GOLD));
        Minecraft mc = Minecraft.getInstance();
        this.initialBlockMobSpawn = BarrierRulesData.isBlockMobSpawn(mc.player.getUUID());
        this.initialAutoRepairBarrier = BarrierRulesData.isAutoRepairBarrier(mc.player.getUUID());
        this.showAutoRepairOption = PlayerReverseTechniqueData.hasReverseTechnique(mc.player);
    }

    @Override
    protected void init()
    {
        super.init();

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int buttonWidth = 200;
        int buttonHeight = 20;
        int blockMobSpawnY = showAutoRepairOption ? centerY - 55 : centerY - 40;

        this.blockMobSpawnCheckbox = this.addRenderableWidget(
                new Checkbox(
                        centerX - buttonWidth / 2,
                        blockMobSpawnY,
                        buttonWidth,
                        buttonHeight,
                        Component.literal("阻止生物在结界内生成（立刻生效）").withStyle(ChatFormatting.WHITE),
                        initialBlockMobSpawn
                )
        );

        if (showAutoRepairOption)
        {
            this.autoRepairBarrierCheckbox = this.addRenderableWidget(
                    new Checkbox(
                            centerX - buttonWidth / 2,
                            centerY - 30,
                            buttonWidth,
                            buttonHeight,
                            Component.literal("自动修补结界（立即生效）").withStyle(ChatFormatting.WHITE),
                            initialAutoRepairBarrier
                    )
            );
        }

        this.addRenderableWidget(
                new StringWidget(
                        centerX - 100, centerY - 5, 200, 20,
                        Component.literal("有些规则是立即生效的").withStyle(ChatFormatting.GRAY),
                        this.font
                )
        );

        this.addRenderableWidget(
                Button.builder(Component.literal("保存").withStyle(ChatFormatting.GREEN), button -> {
                    saveSettings();
                    this.onClose();
                })
                        .bounds(centerX - buttonWidth / 2, centerY + 25, buttonWidth / 2 - 5, buttonHeight)
                        .build()
        );

        this.addRenderableWidget(
                Button.builder(Component.literal("取消").withStyle(ChatFormatting.RED), button -> {
                    this.onClose();
                })
                        .bounds(centerX + 5, centerY + 25, buttonWidth / 2 - 5, buttonHeight)
                        .build()
        );
    }

    private void saveSettings()
    {
        boolean newBlockMobSpawn = blockMobSpawnCheckbox.selected();
        boolean newAutoRepairBarrier = showAutoRepairOption
                && autoRepairBarrierCheckbox != null
                && autoRepairBarrierCheckbox.selected();
        if (newBlockMobSpawn != initialBlockMobSpawn || newAutoRepairBarrier != initialAutoRepairBarrier)
        {
            ModNetwork.sendBarrierRulesToServer(newBlockMobSpawn, newAutoRepairBarrier);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;

        graphics.drawString(this.font, this.title, centerX - this.font.width(this.title) / 2, 30, 0xFFFFFF);

        Component desc = Component.literal("配置结界的行为规则").withStyle(ChatFormatting.GRAY);
        graphics.drawString(this.font, desc, centerX - this.font.width(desc) / 2, 50, 0x888888);

        Component tip = Component.literal("按 ESC 或点击外部关闭").withStyle(ChatFormatting.DARK_GRAY);
        graphics.drawString(this.font, tip, centerX - this.font.width(tip) / 2, this.height - 30, 0x666666);
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
