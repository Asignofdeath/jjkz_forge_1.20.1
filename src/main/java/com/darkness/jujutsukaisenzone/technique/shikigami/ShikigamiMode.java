package com.darkness.jujutsukaisenzone.technique.shikigami;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Optional;

/**
 * 一种「式神模式」：何时占用数字键、槽位与能力表。
 * <p>
 * 十种影「影之式神」是首个实现；其它式神模式术式按同一约定填表。
 */
public interface ShikigamiMode
{
    /** 模式 id，用于 CD 键前缀等，如 {@code ten_shadows}。 */
    String modeId();

    /** 客户端 / 服务端：该玩家当前是否处于本模式（占用 1～0）。 */
    boolean isActive(Player player);

    /** 该模式下全部能力声明。 */
    List<ShikigamiAbility> abilities();

    /** 槽位索引 0～9 → 式神槽位 id；无效槽返回 empty。 */
    Optional<String> slotId(int shikigamiIndex);

    /** 场上是否有该槽存活式神（主动/自动默认都要求在场）。 */
    boolean hasAliveOnField(ServerPlayer owner, String shikigamiSlotId);

    default Optional<ShikigamiAbility> findPlayerAbility(String shikigamiSlotId, int modifierMask)
    {
        for (ShikigamiAbility ability : abilities())
        {
            if (!ability.shikigamiSlotId().equals(shikigamiSlotId))
            {
                continue;
            }
            if (!ability.castMode().allowsPlayer())
            {
                continue;
            }
            if (ability.modifierMask() == modifierMask)
            {
                return Optional.of(ability);
            }
        }
        return Optional.empty();
    }

    default List<ShikigamiAbility> autoAbilities(String shikigamiSlotId)
    {
        return abilities().stream()
                .filter(a -> a.shikigamiSlotId().equals(shikigamiSlotId))
                .filter(a -> a.castMode().allowsAuto())
                .sorted((a, b) -> Integer.compare(b.autoPriority(), a.autoPriority()))
                .toList();
    }
}
