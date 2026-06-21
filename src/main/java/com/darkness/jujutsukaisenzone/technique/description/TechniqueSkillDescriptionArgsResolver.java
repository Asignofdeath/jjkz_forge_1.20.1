package com.darkness.jujutsukaisenzone.technique.description;

import com.darkness.jujutsukaisenzone.technique.skillset.TechniqueSkill;
import net.minecraft.world.entity.player.Player;

/**
 * 为某技能的描述语言键提供 {@link net.minecraft.network.chat.Component#translatable} 参数。
 */
public interface TechniqueSkillDescriptionArgsResolver
{
    Object[] resolve(Player player, String techniqueId, TechniqueSkill skill);
}
