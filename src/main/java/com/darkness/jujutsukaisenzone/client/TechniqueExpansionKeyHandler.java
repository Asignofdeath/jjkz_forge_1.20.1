package com.darkness.jujutsukaisenzone.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/**
 * 「术式的扩张」按键占位（默认 X）；逻辑尚未实现。
 */
public final class TechniqueExpansionKeyHandler
{
    public static final KeyMapping KEY_MAPPING = new KeyMapping(
            "key.jujutsukaisenzone.technique_expansion",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_X,
            "key.categories.jujutsukaisenzone"
    );

    private TechniqueExpansionKeyHandler()
    {
    }
}
