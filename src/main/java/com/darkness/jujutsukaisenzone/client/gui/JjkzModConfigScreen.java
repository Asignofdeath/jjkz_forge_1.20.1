package com.darkness.jujutsukaisenzone.client.gui;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.client.compat.ForgeConfigScreensIntegration;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;

/** 模组列表「配置」按钮：优先使用 Config Menus for Forge，否则打开配置文件。 */
public final class JjkzModConfigScreen extends Screen
{
    private static final int BODY_COLOR = 0xA0_A0A0A0;

    private final Screen parent;

    private JjkzModConfigScreen(Screen parent)
    {
        super(Component.literal("JuJutsuKaisen Zone Config"));
        this.parent = parent;
    }

    public static Screen create(Screen parent)
    {
        Screen configMenusScreen = ForgeConfigScreensIntegration.tryCreate(parent);
        if (configMenusScreen != null)
        {
            return configMenusScreen;
        }
        return new JjkzModConfigScreen(parent);
    }

    @Override
    protected void init()
    {
        int centerX = this.width / 2;
        int y = this.height / 2 + 20;

        addRenderableWidget(Button.builder(Component.literal("打开配置文件"), button -> openConfigFile())
                .bounds(centerX - 100, y, 200, 20)
                .build());

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(centerX - 100, y + 28, 200, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 40, 0xFFFFFF);

        int textY = 64;
        for (Component line : getHelpLines())
        {
            graphics.drawCenteredString(font, line, width / 2, textY, BODY_COLOR);
            textY += 12;
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private static Component[] getHelpLines()
    {
        return new Component[] {
                Component.literal("配置文件：config/jujutsukaisenzone-common.toml"),
                Component.literal("含黑闪、击杀经验、技能战斗数值等全部可调项"),
                Component.literal("修改后需重启世界或服务器生效"),
                Component.literal("安装 Config Menus for Forge 可在游戏内直接编辑")
        };
    }

    private static void openConfigFile()
    {
        Path configFile = FMLPaths.CONFIGDIR.get().resolve(JuJutsuKaisenZone.MODID + "-common.toml");
        Util.getPlatform().openFile(configFile.toFile());
    }

    @Override
    public void onClose()
    {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
