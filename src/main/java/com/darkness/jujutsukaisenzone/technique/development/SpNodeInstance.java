package com.darkness.jujutsukaisenzone.technique.development;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

/**
 * 玩家货架上的一条 SP 节点实例（在售或已点亮）。
 */
public final class SpNodeInstance
{
    private final String instanceId;
    private final SpNodeFamily family;
    /** 1 = 小，2 = 中。 */
    private final int size;
    private final int spCost;
    private final int physicalBonus;
    private final int techniqueBonus;
    /** 异向 Attribute 加成（百分点）；强度节点为 0。 */
    private final int attributeBonus;
    private final int createdTier;

    public SpNodeInstance(
            String instanceId,
            SpNodeFamily family,
            int size,
            int spCost,
            int physicalBonus,
            int techniqueBonus,
            int attributeBonus,
            int createdTier)
    {
        if (instanceId == null || instanceId.isBlank())
        {
            throw new IllegalArgumentException("instanceId");
        }
        if (family == null)
        {
            throw new IllegalArgumentException("family");
        }
        this.instanceId = instanceId;
        this.family = family;
        this.size = size <= 1 ? 1 : 2;
        this.spCost = Math.max(1, spCost);
        this.physicalBonus = Math.max(0, physicalBonus);
        this.techniqueBonus = Math.max(0, techniqueBonus);
        this.attributeBonus = Math.max(0, attributeBonus);
        this.createdTier = Math.max(0, createdTier);
    }

    public static SpNodeInstance create(
            SpNodeFamily family,
            int size,
            int spCost,
            int physicalBonus,
            int techniqueBonus,
            int attributeBonus,
            int createdTier)
    {
        return new SpNodeInstance(
                UUID.randomUUID().toString(),
                family,
                size,
                spCost,
                physicalBonus,
                techniqueBonus,
                attributeBonus,
                createdTier);
    }

    public String instanceId()
    {
        return instanceId;
    }

    public SpNodeFamily family()
    {
        return family;
    }

    public int size()
    {
        return size;
    }

    public int spCost()
    {
        return spCost;
    }

    public int physicalBonus()
    {
        return physicalBonus;
    }

    public int techniqueBonus()
    {
        return techniqueBonus;
    }

    public int attributeBonus()
    {
        return attributeBonus;
    }

    public int createdTier()
    {
        return createdTier;
    }

    public CompoundTag save()
    {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", instanceId);
        tag.putString("family", family.id());
        tag.putInt("size", size);
        tag.putInt("spCost", spCost);
        tag.putInt("phys", physicalBonus);
        tag.putInt("tech", techniqueBonus);
        tag.putInt("attr", attributeBonus);
        tag.putInt("tier", createdTier);
        return tag;
    }

    public static SpNodeInstance load(CompoundTag tag)
    {
        SpNodeFamily family = SpNodeFamily.byId(tag.getString("family"));
        if (family == null)
        {
            return null;
        }
        String id = tag.getString("id");
        if (id == null || id.isBlank())
        {
            return null;
        }
        return new SpNodeInstance(
                id,
                family,
                tag.getInt("size"),
                tag.getInt("spCost"),
                tag.getInt("phys"),
                tag.getInt("tech"),
                tag.getInt("attr"),
                tag.getInt("tier"));
    }

    public void write(FriendlyByteBuf buffer)
    {
        buffer.writeUtf(instanceId);
        buffer.writeUtf(family.id());
        buffer.writeVarInt(size);
        buffer.writeVarInt(spCost);
        buffer.writeVarInt(physicalBonus);
        buffer.writeVarInt(techniqueBonus);
        buffer.writeVarInt(attributeBonus);
        buffer.writeVarInt(createdTier);
    }

    public static SpNodeInstance read(FriendlyByteBuf buffer)
    {
        String id = buffer.readUtf();
        SpNodeFamily family = SpNodeFamily.byId(buffer.readUtf());
        int size = buffer.readVarInt();
        int spCost = buffer.readVarInt();
        int phys = buffer.readVarInt();
        int tech = buffer.readVarInt();
        int attr = buffer.readVarInt();
        int tier = buffer.readVarInt();
        if (family == null || id.isBlank())
        {
            return null;
        }
        return new SpNodeInstance(id, family, size, spCost, phys, tech, attr, tier);
    }
}
