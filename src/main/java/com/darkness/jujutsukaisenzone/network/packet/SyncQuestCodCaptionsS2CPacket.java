package com.darkness.jujutsukaisenzone.network.packet;

import com.darkness.jujutsukaisenzone.client.QuestCodCaptionOverlay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 服务端 → 客户端：普通对白 COD 字幕队列（有限 2 槽，客户端按时序播）。
 * CG 电影黑幕仍走 {@link SyncQuestLetterboxS2CPacket}。
 */
public class SyncQuestCodCaptionsS2CPacket
{
    private final Mode mode;
    private final String[] keys;
    private final int[] delayFromPrevTicks;
    private final int[] holdTicks;

    public enum Mode
    {
        PLAY,
        FADE_ALL,
        CLEAR_IMMEDIATE
    }

    private SyncQuestCodCaptionsS2CPacket(Mode mode, String[] keys, int[] delayFromPrevTicks, int[] holdTicks)
    {
        this.mode = mode;
        this.keys = keys == null ? new String[0] : keys;
        this.delayFromPrevTicks = delayFromPrevTicks == null ? new int[0] : delayFromPrevTicks;
        this.holdTicks = holdTicks == null ? new int[0] : holdTicks;
    }

    public static SyncQuestCodCaptionsS2CPacket play(String[] keys, int[] delayFromPrevTicks, int[] holdTicks)
    {
        return new SyncQuestCodCaptionsS2CPacket(Mode.PLAY, keys, delayFromPrevTicks, holdTicks);
    }

    /** 均匀间隔队列：首句 delay=0，其后每句间隔 {@code gapTicks}。 */
    public static SyncQuestCodCaptionsS2CPacket playUniform(String[] keys, int gapTicks, int holdTicks)
    {
        if (keys == null || keys.length == 0)
        {
            return fadeAll();
        }
        int[] delays = new int[keys.length];
        int[] holds = new int[keys.length];
        for (int i = 0; i < keys.length; i++)
        {
            delays[i] = i == 0 ? 0 : Math.max(0, gapTicks);
            holds[i] = holdTicks;
        }
        return play(keys, delays, holds);
    }

    public static SyncQuestCodCaptionsS2CPacket fadeAll()
    {
        return new SyncQuestCodCaptionsS2CPacket(Mode.FADE_ALL, null, null, null);
    }

    public static SyncQuestCodCaptionsS2CPacket clearImmediate()
    {
        return new SyncQuestCodCaptionsS2CPacket(Mode.CLEAR_IMMEDIATE, null, null, null);
    }

    public static void encode(SyncQuestCodCaptionsS2CPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeEnum(packet.mode);
        buffer.writeVarInt(packet.keys.length);
        for (String key : packet.keys)
        {
            buffer.writeUtf(key == null ? "" : key);
        }
        buffer.writeVarInt(packet.delayFromPrevTicks.length);
        for (int delay : packet.delayFromPrevTicks)
        {
            buffer.writeVarInt(delay);
        }
        buffer.writeVarInt(packet.holdTicks.length);
        for (int hold : packet.holdTicks)
        {
            buffer.writeVarInt(hold);
        }
    }

    public static SyncQuestCodCaptionsS2CPacket decode(FriendlyByteBuf buffer)
    {
        Mode mode = buffer.readEnum(Mode.class);
        int keyCount = buffer.readVarInt();
        String[] keys = new String[keyCount];
        for (int i = 0; i < keyCount; i++)
        {
            keys[i] = buffer.readUtf();
        }
        int delayCount = buffer.readVarInt();
        int[] delays = new int[delayCount];
        for (int i = 0; i < delayCount; i++)
        {
            delays[i] = buffer.readVarInt();
        }
        int holdCount = buffer.readVarInt();
        int[] holds = new int[holdCount];
        for (int i = 0; i < holdCount; i++)
        {
            holds[i] = buffer.readVarInt();
        }
        return new SyncQuestCodCaptionsS2CPacket(mode, keys, delays, holds);
    }

    public static void handle(SyncQuestCodCaptionsS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            switch (packet.mode)
            {
                case PLAY -> QuestCodCaptionOverlay.playQueue(
                        packet.keys, packet.delayFromPrevTicks, packet.holdTicks);
                case FADE_ALL -> QuestCodCaptionOverlay.fadeAll();
                case CLEAR_IMMEDIATE -> QuestCodCaptionOverlay.clearImmediate();
            }
        });
        context.setPacketHandled(true);
    }
}
