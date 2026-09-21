package com.darkness.jujutsukaisenzone.effect;

import com.darkness.jujutsukaisenzone.entity.util.LimitlessRedBlockLaunch;
import com.darkness.jujutsukaisenzone.player.PlayerEndCrystalAttackerData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.Explosion;

/**
 * 判定爆炸是否属于玩家引爆 TNT 或末地水晶。
 */
public final class PublicSafetyExplosionRules
{
    public static final String CRIME_DESCRIPTION = "引爆TNT或末地水晶";

    private PublicSafetyExplosionRules()
    {
    }

    public static ServerPlayer resolvePlayerCauser(Explosion explosion, ServerLevel level)
    {
        if (LimitlessRedBlockLaunch.isRedExplosion(explosion))
        {
            return null;
        }

        Entity directSource = explosion.getDirectSourceEntity();
        if (directSource instanceof PrimedTnt primedTnt && primedTnt.getOwner() instanceof ServerPlayer owner)
        {
            return owner;
        }
        if (directSource instanceof EndCrystal crystal)
        {
            return PlayerEndCrystalAttackerData.takeAttacker(crystal);
        }
        return null;
    }
}
