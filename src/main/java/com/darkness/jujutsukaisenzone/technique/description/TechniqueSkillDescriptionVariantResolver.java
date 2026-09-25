package com.darkness.jujutsukaisenzone.technique.description;

import com.darkness.jujutsukaisenzone.technique.skillset.TechniqueSkill;
import net.minecraft.world.entity.player.Player;

/**
 * 按玩家上下文切换技能描述的语言键与参数（见 {@link TechniqueSkillDescriptionVariant}）。
 * <p>
 * 返回 {@code null} 表示用默认键（{@link TechniqueSkillDescriptionKeys#description(String)}）。
 * <p>
 * <b>约定：返回的非空变体，其 {@code descriptionKey} 必须在 lang 里存在。</b>键缺失时
 * {@code translatable} 会把键名原样显示，所以每加一个后缀都要在 {@code zh_cn} 与
 * {@code en_us} 两侧一起补，不能只补一边。
 */
public interface TechniqueSkillDescriptionVariantResolver
{
    TechniqueSkillDescriptionVariant resolve(Player player, String techniqueId, TechniqueSkill skill);
}
