package com.darkness.jujutsukaisenzone.client.compat;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.Nullable;

/** 可选联动 Config Menus for Forge（{@code forgeconfigscreens}），无硬依赖。 */
public final class ForgeConfigScreensIntegration
{
    private ForgeConfigScreensIntegration()
    {
    }

    @Nullable
    public static Screen tryCreate(Screen parent)
    {
        if (!ModList.get().isLoaded("forgeconfigscreens"))
        {
            return null;
        }

        try
        {
            Class<?> screensClass = Class.forName("fuzs.forgeconfigscreens.ForgeConfigScreens");
            return (Screen) screensClass
                    .getMethod("createConfigScreen", Screen.class, String.class)
                    .invoke(null, parent, JuJutsuKaisenZone.MODID);
        }
        catch (ReflectiveOperationException ignored)
        {
            return null;
        }
    }
}
