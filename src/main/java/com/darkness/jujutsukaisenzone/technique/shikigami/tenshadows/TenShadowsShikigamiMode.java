package com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows;

import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import com.darkness.jujutsukaisenzone.player.PlayerTenShadowsModeData;
import com.darkness.jujutsukaisenzone.player.PlayerTenShadowsRosterData;
import com.darkness.jujutsukaisenzone.technique.shikigami.ShikigamiAbility;
import com.darkness.jujutsukaisenzone.technique.shikigami.ShikigamiMode;
import com.darkness.jujutsukaisenzone.technique.shikigami.ShikigamiModeRegistry;
import com.darkness.jujutsukaisenzone.technique.skill.TenShadowsShikigami;
import com.darkness.jujutsukaisenzone.technique.skill.TenShadowsSummonLogic;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 十种影法术「影之式神」模式：占用 1～0，能力表见 {@link TenShadowsAbilities}。
 */
public final class TenShadowsShikigamiMode implements ShikigamiMode
{
    public static final String MODE_ID = "ten_shadows";
    public static final TenShadowsShikigamiMode INSTANCE = new TenShadowsShikigamiMode();

    static
    {
        ShikigamiModeRegistry.register(INSTANCE);
    }

    private TenShadowsShikigamiMode()
    {
    }

    /** 触发类加载以完成注册。 */
    public static void bootstrap()
    {
        // no-op：访问本类即可
    }

    @Override
    public String modeId()
    {
        return MODE_ID;
    }

    @Override
    public boolean isActive(Player player)
    {
        return PlayerTechniqueData.isTenShadows(player) && PlayerTenShadowsModeData.isShikigamiMode(player);
    }

    @Override
    public List<ShikigamiAbility> abilities()
    {
        return TenShadowsAbilities.ALL;
    }

    @Override
    public Optional<String> slotId(int shikigamiIndex)
    {
        TenShadowsShikigami shikigami = TenShadowsShikigami.byIndex(shikigamiIndex);
        return shikigami == null ? Optional.empty() : Optional.of(shikigami.id());
    }

    @Override
    public boolean hasAliveOnField(ServerPlayer owner, String shikigamiSlotId)
    {
        TenShadowsShikigami shikigami = TenShadowsShikigami.byId(shikigamiSlotId);
        if (shikigami == null)
        {
            return false;
        }
        ServerLevel level = owner.serverLevel();
        for (UUID id : PlayerTenShadowsRosterData.get(owner, shikigami.index()))
        {
            Entity entity = level.getEntity(id);
            if (entity != null && entity.isAlive() && TenShadowsSummonLogic.isOwnedBy(entity, owner.getUUID()))
            {
                return true;
            }
        }
        return false;
    }
}
