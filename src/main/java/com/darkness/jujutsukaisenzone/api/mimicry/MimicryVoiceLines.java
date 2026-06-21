package com.darkness.jujutsukaisenzone.api.mimicry;

import com.darkness.jujutsukaisenzone.player.PlayerMimicryData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 降灵术拟态台词公共 API：按「触发点 + 拟态角色」注册台词池，在服务端玩法逻辑中调用 {@link #tryPlay} 即可。
 * <p>
 * 注册示例：
 * <pre>{@code
 * MimicryVoiceLines.register(
 *         MimicryVoiceLineEvents.SHRINE_KAI_SLASH,
 *         MimicryCharacterRegistry.CHARACTER_SUKUNA,
 *         0.30F,
 *         ModSounds.SUKUNA_KAI.get(),
 *         ModSounds.SUKUNA_HMPH.get());
 *
 * // 术式释放时
 * MimicryVoiceLines.tryPlay(player, MimicryVoiceLineEvents.SHRINE_KAI_SLASH);
 * }</pre>
 */
public final class MimicryVoiceLines
{
    private static final Map<MimicryVoiceLineEvent, Map<String, MimicryVoiceLineBinding>> BINDINGS = new HashMap<>();

    private MimicryVoiceLines()
    {
    }

    /**
     * 为指定触发点与拟态角色注册台词池。
     *
     * @throws IllegalStateException 同一 event + character 重复注册
     */
    public static void register(MimicryVoiceLineEvent event, String characterId, MimicryVoiceLineBinding binding)
    {
        Objects.requireNonNull(event, "event");
        Objects.requireNonNull(characterId, "characterId");
        Objects.requireNonNull(binding, "binding");
        if (characterId.isBlank())
        {
            throw new IllegalArgumentException("characterId must not be blank");
        }

        Map<String, MimicryVoiceLineBinding> byCharacter = BINDINGS.computeIfAbsent(event, key -> new HashMap<>());
        if (byCharacter.putIfAbsent(characterId, binding) != null)
        {
            throw new IllegalStateException(
                    "Duplicate mimicry voice binding for event " + event.id() + " and character " + characterId);
        }
    }

    /**
     * 便捷注册：默认音量 {@code 1.0}、音调 {@code 1.0}。
     */
    public static void register(MimicryVoiceLineEvent event, String characterId, float triggerChance, SoundEvent... sounds)
    {
        register(event, characterId, MimicryVoiceLineBinding.of(triggerChance, sounds));
    }

    /**
     * 查询已注册的台词配置（只读）。
     */
    public static Optional<MimicryVoiceLineBinding> getBinding(MimicryVoiceLineEvent event, String characterId)
    {
        Objects.requireNonNull(event, "event");
        if (characterId == null || characterId.isBlank())
        {
            return Optional.empty();
        }
        Map<String, MimicryVoiceLineBinding> byCharacter = BINDINGS.get(event);
        if (byCharacter == null)
        {
            return Optional.empty();
        }
        return Optional.ofNullable(byCharacter.get(characterId));
    }

    /**
     * 若玩家当前拟态角色在该 event 下已注册台词，则按配置概率随机播放一条。
     *
     * @return {@code true} 表示已播放台词；{@code false} 表示未触发（拟态未开、无配置、概率未中）
     */
    public static boolean tryPlay(ServerPlayer player, MimicryVoiceLineEvent event)
    {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(event, "event");

        if (!PlayerMimicryData.isActive(player))
        {
            return false;
        }

        String characterId = PlayerMimicryData.getActiveCharacterId(player);
        if (characterId == null)
        {
            return false;
        }

        MimicryVoiceLineBinding binding = BINDINGS.getOrDefault(event, Map.of()).get(characterId);
        if (binding == null)
        {
            return false;
        }

        if (binding.triggerChance() <= 0.0F)
        {
            return false;
        }
        if (binding.triggerChance() < 1.0F && player.getRandom().nextDouble() >= binding.triggerChance())
        {
            return false;
        }

        SoundEvent sound = binding.sounds().get(player.getRandom().nextInt(binding.sounds().size()));
        player.serverLevel().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                sound,
                SoundSource.PLAYERS,
                binding.volume(),
                binding.pitch());
        return true;
    }
}
