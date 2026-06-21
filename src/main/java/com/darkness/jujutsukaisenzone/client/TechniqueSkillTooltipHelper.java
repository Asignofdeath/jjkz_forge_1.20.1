package com.darkness.jujutsukaisenzone.client;

import com.darkness.jujutsukaisenzone.technique.description.TechniqueSkillDescriptionKeys;
import com.darkness.jujutsukaisenzone.technique.description.TechniqueSkillDescriptionView;
import com.darkness.jujutsukaisenzone.technique.description.TechniqueSkillDescriptions;
import com.darkness.jujutsukaisenzone.technique.skillset.TechniqueSkill;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 术式技能悬停描述框：将 {@link TechniqueSkillDescriptionView} 格式化为多行 tooltip。
 * <p>
 * 阶段 3/4 的快捷栏配置界面与术式开发界面应统一调用此类。
 */
@OnlyIn(Dist.CLIENT)
public final class TechniqueSkillTooltipHelper
{
    private TechniqueSkillTooltipHelper()
    {
    }

    public static List<Component> build(Player player, String techniqueId, TechniqueSkill skill)
    {
        return build(TechniqueSkillDescriptions.resolve(player, techniqueId, skill));
    }

    public static List<Component> build(TechniqueSkillDescriptionView view)
    {
        if (view == null || view.displayName().getString().isEmpty())
        {
            return Collections.emptyList();
        }

        List<Component> lines = new ArrayList<>(4);
        lines.add(view.displayName().copy().withStyle(ChatFormatting.GOLD));
        lines.add(Component.translatable(
                        TechniqueSkillDescriptionKeys.COST,
                        view.cursedEnergyCost())
                .withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable(
                        TechniqueSkillDescriptionKeys.COOLDOWN,
                        view.cooldownSeconds())
                .withStyle(ChatFormatting.GRAY));
        if (!view.description().getString().isBlank())
        {
            lines.add(Component.empty());
            lines.add(view.description().copy().withStyle(ChatFormatting.DARK_GRAY));
        }
        return Collections.unmodifiableList(lines);
    }
}
