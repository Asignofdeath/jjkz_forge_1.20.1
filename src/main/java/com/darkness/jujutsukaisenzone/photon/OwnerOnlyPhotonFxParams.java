package com.darkness.jujutsukaisenzone.photon;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

/** Photon 实体挂载参数（与 {@code photon fx entity} 命令字段一致）。 */
public record OwnerOnlyPhotonFxParams(
        Vec3 offset,
        Vec3 rotation,
        Vec3 scale,
        int delay,
        boolean forcedDeath,
        boolean allowMulti,
        int autoRotateOrdinal)
{
    /** {@code AutoRotate.NONE} */
    public static final int AUTO_ROTATE_NONE = 0;
    /** {@code AutoRotate.LOOK} */
    public static final int AUTO_ROTATE_LOOK = 2;

    public static final OwnerOnlyPhotonFxParams KAI_SLASH = new OwnerOnlyPhotonFxParams(
            Vec3.ZERO, Vec3.ZERO, new Vec3(1.0D, 1.0D, 1.0D), 0, false, false, AUTO_ROTATE_LOOK);

    public static final OwnerOnlyPhotonFxParams HACHI = new OwnerOnlyPhotonFxParams(
            Vec3.ZERO, Vec3.ZERO, new Vec3(0.5D, 0.5D, 0.5D), 0, false, false, AUTO_ROTATE_NONE);

    public static final OwnerOnlyPhotonFxParams DOMAIN_DEFAULT = new OwnerOnlyPhotonFxParams(
            Vec3.ZERO, Vec3.ZERO, new Vec3(1.0D, 1.0D, 1.0D), 0, false, false, AUTO_ROTATE_NONE);

    /** 与 {@code photon fx photon:spotlight entity ... true false none} 一致。 */
    public static final OwnerOnlyPhotonFxParams SPOTLIGHT = new OwnerOnlyPhotonFxParams(
            Vec3.ZERO, Vec3.ZERO, new Vec3(1.0D, 1.0D, 1.0D), 0, true, false, AUTO_ROTATE_NONE);

    public void encode(FriendlyByteBuf buffer)
    {
        buffer.writeDouble(this.offset.x);
        buffer.writeDouble(this.offset.y);
        buffer.writeDouble(this.offset.z);
        buffer.writeDouble(this.rotation.x);
        buffer.writeDouble(this.rotation.y);
        buffer.writeDouble(this.rotation.z);
        buffer.writeDouble(this.scale.x);
        buffer.writeDouble(this.scale.y);
        buffer.writeDouble(this.scale.z);
        buffer.writeVarInt(this.delay);
        buffer.writeBoolean(this.forcedDeath);
        buffer.writeBoolean(this.allowMulti);
        buffer.writeVarInt(this.autoRotateOrdinal);
    }

    public static OwnerOnlyPhotonFxParams decode(FriendlyByteBuf buffer)
    {
        return new OwnerOnlyPhotonFxParams(
                new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()),
                new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()),
                new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()),
                buffer.readVarInt(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readVarInt());
    }
}
