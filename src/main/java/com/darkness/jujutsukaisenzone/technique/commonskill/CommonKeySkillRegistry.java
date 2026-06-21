package com.darkness.jujutsukaisenzone.technique.commonskill;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 通用独立按键技能注册表。
 * <p>
 * 实现简述：负责统一注册按键映射，并在客户端 tick 中遍历分发按住触发逻辑。
 * 业务意图：避免把独立技能输入逻辑分散在主类，新增技能时只需注册实现。
 */
public final class CommonKeySkillRegistry
{
    private static final Logger LOGGER = LoggerFactory.getLogger(CommonKeySkillRegistry.class);
    private static final Map<String, ICommonKeySkill> REGISTRY = new LinkedHashMap<>();
    private static final Map<String, Boolean> LAST_KEY_DOWN = new LinkedHashMap<>();

    private CommonKeySkillRegistry()
    {
    }

    public static void register(ICommonKeySkill skill)
    {
        ICommonKeySkill previous = REGISTRY.put(skill.skillId(), skill);
        LAST_KEY_DOWN.putIfAbsent(skill.skillId(), false);
        if (previous != null)
        {
            LOGGER.warn("通用按键技能重复注册，已覆盖：{}", skill.skillId());
        }
    }

    public static void registerKeyMappings(RegisterKeyMappingsEvent event)
    {
        for (ICommonKeySkill skill : REGISTRY.values())
        {
            event.register(skill.keyMapping());
        }
    }

    /**
     * 在客户端 END 阶段分发技能输入。
     * <p>
     * 重要假设：仅在无 GUI 打开时处理按键，防止输入与界面交互冲突。
     */
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END)
        {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.screen != null)
        {
            resetKeyStates();
            return;
        }

        for (ICommonKeySkill skill : REGISTRY.values())
        {
            boolean isDown = skill.keyMapping().isDown();
            boolean wasDown = LAST_KEY_DOWN.getOrDefault(skill.skillId(), false);

            if (!isDown)
            {
                if (wasDown)
                {
                    skill.onKeyReleaseClientTick(minecraft, player);
                }
                LAST_KEY_DOWN.put(skill.skillId(), false);
                continue;
            }
            if (!skill.canTriggerClient(player))
            {
                LAST_KEY_DOWN.put(skill.skillId(), isDown);
                continue;
            }

            if (skill.triggerOnPress())
            {
                if (!wasDown)
                {
                    skill.onKeyPressClientTick(minecraft, player);
                }
            }
            else
            {
                skill.onKeyHoldClientTick(minecraft, player);
            }

            LAST_KEY_DOWN.put(skill.skillId(), isDown);
        }
    }

    private static void resetKeyStates()
    {
        for (String skillId : LAST_KEY_DOWN.keySet())
        {
            LAST_KEY_DOWN.put(skillId, false);
        }
        EmotionalReleaseCommonKeySkill.INSTANCE.resetBeginSent();
    }
}
