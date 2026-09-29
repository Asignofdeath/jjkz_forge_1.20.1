package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.client.StarMarkDecalClient;
import com.darkness.jujutsukaisenzone.player.StarMark;
import com.darkness.jujutsukaisenzone.player.StarMarkType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/** 服务端 → 附近客户端：某施术者当前持有的星星标记列表（供喷漆贴花渲染）。 */
public final class SyncStarMarksS2CPacket
{
    private final UUID ownerId;
    private final List<StarMark> marks;

    public SyncStarMarksS2CPacket(UUID ownerId, List<StarMark> marks)
    {
        this.ownerId = ownerId;
        this.marks = marks == null ? List.of() : List.copyOf(marks);
    }

    public static void encode(SyncStarMarksS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeUUID(packet.ownerId);
        buffer.writeVarInt(packet.marks.size());
        for (StarMark mark : packet.marks)
        {
            buffer.writeEnum(mark.kind());
            buffer.writeResourceLocation(mark.dimension().location());
            buffer.writeBlockPos(mark.pos());
            buffer.writeBoolean(mark.entityId() != null);
            if (mark.entityId() != null)
            {
                buffer.writeUUID(mark.entityId());
            }
            buffer.writeUtf(mark.type().id());
            buffer.writeVarInt(mark.stacks());
            buffer.writeVarLong(mark.lastMarkedAt());
            buffer.writeByte(mark.face().get3DDataValue());
            buffer.writeFloat(mark.u());
            buffer.writeFloat(mark.v());
        }
    }

    public static SyncStarMarksS2CPacket decode(FriendlyByteBuf buffer)
    {
        UUID ownerId = buffer.readUUID();
        int count = buffer.readVarInt();
        List<StarMark> marks = new ArrayList<>(count);
        for (int i = 0; i < count; i++)
        {
            StarMark.Kind kind = buffer.readEnum(StarMark.Kind.class);
            ResourceLocation dimId = buffer.readResourceLocation();
            ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, dimId);
            BlockPos pos = buffer.readBlockPos();
            UUID entityId = buffer.readBoolean() ? buffer.readUUID() : null;
            StarMarkType type = StarMarkType.byId(buffer.readUtf());
            int stacks = buffer.readVarInt();
            long markedAt = buffer.readVarLong();
            Direction face = Direction.from3DDataValue(buffer.readByte());
            float u = buffer.readFloat();
            float v = buffer.readFloat();
            marks.add(new StarMark(kind, dimension, pos, entityId, type, stacks, markedAt, face, u, v));
        }
        return new SyncStarMarksS2CPacket(ownerId, marks);
    }

    @OnlyIn(Dist.CLIENT)
    public static void handle(SyncStarMarksS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> StarMarkDecalClient.accept(packet.ownerId, packet.marks));
        context.setPacketHandled(true);
    }
}
