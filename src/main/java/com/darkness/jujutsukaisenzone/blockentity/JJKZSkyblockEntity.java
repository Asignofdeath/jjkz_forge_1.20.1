package com.darkness.jujutsukaisenzone.blockentity;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.registry.ModBlockEntities;
import com.noodlegamer76.shadered.client.util.skyblock.SkyblockPass;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * 自定义天空盒方块实体。
 * <p>
 * 存储天空盒纹理路径和渲染参数，支持通过 NBT 自定义纹理。
 * 默认纹理路径为 "jujutsukaisenzone:textures/environment/default"。
 */
public class JJKZSkyblockEntity extends BlockEntity
{
    /** 默认纹理文件夹路径（使用 Shadered 的星空纹理） */
    public static final ResourceLocation DEFAULT_TEXTURE =     new ResourceLocation(
            "shadered",
            "textures/environment/space"
    );

    /** NBT 键：纹理路径 */
    private static final String TAG_TEXTURE = "Texture";

    /** NBT 键：天空盒通道类型 */
    private static final String TAG_PASS = "Pass";

    /** 纹理文件夹的 ResourceLocation（不包含具体的文件名） */
    private ResourceLocation textureFolder = DEFAULT_TEXTURE;

    /** 天空盒渲染通道类型 */
    private SkyblockPass pass = SkyblockPass.NORMAL;

    public JJKZSkyblockEntity(BlockPos pPos, BlockState pBlockState)
    {
        super(ModBlockEntities.JJKZ_SKYBLOCK.get(), pPos, pBlockState);
    }

    /**
     * 获取当前纹理文件夹路径。
     *
     * @return 纹理文件夹的 ResourceLocation
     */
    public ResourceLocation getTextureFolder()
    {
        return textureFolder;
    }

    /**
     * 设置纹理文件夹路径并同步到客户端。
     *
     * @param textureFolder 纹理文件夹的 ResourceLocation
     */
    public void setTextureFolder(ResourceLocation textureFolder)
    {
        this.textureFolder = textureFolder;
        setChanged();
        if (level != null)
        {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /**
     * 获取当前天空盒通道类型。
     *
     * @return 天空盒通道类型
     */
    public SkyblockPass getPass()
    {
        return pass;
    }

    /**
     * 设置天空盒通道类型并同步到客户端。
     *
     * @param pass 天空盒通道类型
     */
    public void setPass(SkyblockPass pass)
    {
        this.pass = pass;
        setChanged();
        if (level != null)
        {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public void load(@NotNull CompoundTag tag)
    {
        super.load(tag);
        /* 从 NBT 读取纹理路径，若无效则使用默认值 */
        if (tag.contains(TAG_TEXTURE))
        {
            try
            {
                this.textureFolder = new ResourceLocation(tag.getString(TAG_TEXTURE));
            }
            catch (Exception e)
            {
                this.textureFolder = DEFAULT_TEXTURE;
            }
        }

        /* 从 NBT 读取通道类型，越界时使用 NORMAL */
        int passOrdinal = tag.getInt(TAG_PASS);
        if (passOrdinal >= 0 && passOrdinal < SkyblockPass.values().length)
        {
            this.pass = SkyblockPass.values()[passOrdinal];
        }
        else
        {
            this.pass = SkyblockPass.NORMAL;
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag)
    {
        super.saveAdditional(tag);
        tag.putString(TAG_TEXTURE, textureFolder.toString());
        tag.putInt(TAG_PASS, pass.ordinal());
    }

    @Override
    public @NotNull CompoundTag getUpdateTag()
    {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket()
    {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
