package com.darkness.jujutsukaisenzone.api.mimicry;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/**
 * 拟态台词触发点标识。不同玩法（术式释放、领域、受击等）使用不同 event，便于按场景注册台词池。
 */
public final class MimicryVoiceLineEvent
{
    private final ResourceLocation id;

    private MimicryVoiceLineEvent(ResourceLocation id)
    {
        this.id = Objects.requireNonNull(id, "id");
    }

    public static MimicryVoiceLineEvent of(ResourceLocation id)
    {
        return new MimicryVoiceLineEvent(id);
    }

    public static MimicryVoiceLineEvent of(String namespace, String path)
    {
        return of(ResourceLocation.fromNamespaceAndPath(namespace, path));
    }

    public ResourceLocation id()
    {
        return this.id;
    }

    @Override
    public boolean equals(Object other)
    {
        return other instanceof MimicryVoiceLineEvent event && this.id.equals(event.id);
    }

    @Override
    public int hashCode()
    {
        return this.id.hashCode();
    }

    @Override
    public String toString()
    {
        return "MimicryVoiceLineEvent[" + this.id + "]";
    }
}
