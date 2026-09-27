package com.darkness.jujutsukaisenzone.technique.se;

import java.util.Collection;

/**
 * SE 的适用范围：通用，或某个生得术式专用。
 * <p>
 * 专用 SE 的校验入参是<b>集合</b>（{@link #matches(Collection)}）。
 * 现在玩家只持有一个术式，调用方传单元素集合；
 * 将来「同时持有多个术式」上线时<b>只换数据源</b>，这里的判定逻辑不用动
 * —— 这就是把签名写成集合而不是 String 的唯一原因。
 */
public sealed interface SeScope
{
    /**
     * 本次判定所依据的「玩家持有的术式 id」。{@code ownedTechniqueIds} 允许为 null（按无术式处理）。
     */
    boolean matches(Collection<String> ownedTechniqueIds);

    /** 通用：不限术式，任何玩家都能装（前提是过得了特质与质量两道闸）。 */
    record Generic() implements SeScope
    {
        @Override
        public boolean matches(Collection<String> ownedTechniqueIds)
        {
            return true;
        }
    }

    /** 专用：只认一个术式 id，取值的合法范围见 {@code TechniqueSkillSetRegistry#getRegisteredTechniqueIds()}。 */
    record Technique(String techniqueId) implements SeScope
    {
        public Technique
        {
            if (techniqueId == null || techniqueId.isBlank())
            {
                throw new IllegalArgumentException("techniqueId（专用 SE 必须写明术式 id）");
            }
        }

        @Override
        public boolean matches(Collection<String> ownedTechniqueIds)
        {
            return ownedTechniqueIds != null && ownedTechniqueIds.contains(techniqueId);
        }
    }

    static SeScope generic()
    {
        return new Generic();
    }

    static SeScope technique(String techniqueId)
    {
        return new Technique(techniqueId);
    }
}
