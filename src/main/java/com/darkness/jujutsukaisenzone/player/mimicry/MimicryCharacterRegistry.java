package com.darkness.jujutsukaisenzone.player.mimicry;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Optional;

/**
 * 术式与拟态角色外观的静态映射表。
 * <p>
 * 术式 ID 与角色 ID 是不同维度：术式决定「当前能拟态成谁」，角色 ID 决定贴图与模型类型。
 */
public final class MimicryCharacterRegistry
{
    public static final String CHARACTER_GOJO = "gojo";
    public static final String CHARACTER_SUKUNA = "sukuna";

    /**
     * 角色外观配置：标准 Minecraft 玩家皮肤 UV（支持 64×64 及等比高清），供原版与 Epic Fight Biped 网格共用。
     *
     * @param skinTexture 贴图路径（相对 mod 命名空间）
     * @param slim        true 时使用 Alex 窄臂模型（Epic Fight {@code Meshes.ALEX}）
     */
    public record MimicryProfile(ResourceLocation skinTexture, boolean slim)
    {
    }

    private static final Map<String, MimicryProfile> BY_CHARACTER_ID = Map.of(
            CHARACTER_GOJO, profile("textures/entity/mimicry/gojo.png", false),
            CHARACTER_SUKUNA, profile("textures/entity/mimicry/sukuna.png", false)
    );

    private static final Map<String, String> TECHNIQUE_TO_CHARACTER = Map.of(
            PlayerTechniqueData.TECHNIQUE_LIMITLESS, CHARACTER_GOJO,
            PlayerTechniqueData.TECHNIQUE_SHRINE, CHARACTER_SUKUNA
    );

    private MimicryCharacterRegistry()
    {
    }

    /**
     * 根据术式 ID 解析可拟态的角色外观；无映射或术式为 none 时返回 empty。
     */
    public static Optional<MimicryProfile> resolveFromTechnique(String techniqueId)
    {
        String characterId = resolveCharacterId(techniqueId);
        if (characterId == null)
        {
            return Optional.empty();
        }
        return getProfile(characterId);
    }

    /**
     * 根据术式 ID 解析角色 ID；无映射时返回 null。
     */
    public static String resolveCharacterId(String techniqueId)
    {
        if (techniqueId == null || techniqueId.isBlank()
                || PlayerTechniqueData.TECHNIQUE_NONE.equals(techniqueId))
        {
            return null;
        }
        return TECHNIQUE_TO_CHARACTER.get(techniqueId);
    }

    public static Optional<MimicryProfile> getProfile(String characterId)
    {
        if (characterId == null || characterId.isBlank())
        {
            return Optional.empty();
        }
        MimicryProfile profile = BY_CHARACTER_ID.get(characterId);
        return Optional.ofNullable(profile);
    }

    private static MimicryProfile profile(String path, boolean slim)
    {
        return new MimicryProfile(
                ResourceLocation.fromNamespaceAndPath(JuJutsuKaisenZone.MODID, path),
                slim
        );
    }
}
