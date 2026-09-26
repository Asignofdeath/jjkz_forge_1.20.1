package com.darkness.jujutsukaisenzone.technique.skill;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * 十划咒法光点所在的身体部位。锚点相对目标碰撞箱计算，供挂点与瞄准判定共用。
 */
public enum RatioBodyPart
{
    HEAD("头", 0.92D, 0.0D, 0.0D),
    TORSO("躯干", 0.55D, 0.0D, 0.0D),
    LEFT_ARM("左臂", 0.60D, 0.55D, 0.0D),
    RIGHT_ARM("右臂", 0.60D, -0.55D, 0.0D),
    LEFT_LEG("左腿", 0.18D, 0.25D, 0.0D),
    RIGHT_LEG("右腿", 0.18D, -0.25D, 0.0D);

    private final String displayName;
    /** 相对碰撞箱高度的比例（0 = 脚底，1 = 头顶）。 */
    private final double heightRatio;
    /** 相对碰撞箱宽度的横向偏移（正 = 目标本地左，经 yaw 旋转）。 */
    private final double lateralRatio;
    private final double depthRatio;

    RatioBodyPart(String displayName, double heightRatio, double lateralRatio, double depthRatio)
    {
        this.displayName = displayName;
        this.heightRatio = heightRatio;
        this.lateralRatio = lateralRatio;
        this.depthRatio = depthRatio;
    }

    public String displayName()
    {
        return displayName;
    }

    public static RatioBodyPart random(RandomSource random)
    {
        RatioBodyPart[] values = values();
        return values[random.nextInt(values.length)];
    }

    public static RatioBodyPart byName(String name)
    {
        if (name == null || name.isBlank())
        {
            return TORSO;
        }
        try
        {
            return RatioBodyPart.valueOf(name);
        }
        catch (IllegalArgumentException ignored)
        {
            return TORSO;
        }
    }

    /**
     * 光点世界坐标：高度取碰撞箱比例，横向按目标朝向旋转。
     */
    public Vec3 worldAnchor(LivingEntity target)
    {
        double width = target.getBbWidth();
        double height = target.getBbHeight();
        double localX = lateralRatio * width * 0.5D;
        double localZ = depthRatio * width * 0.5D;
        float yawRad = (float) Math.toRadians(-target.getYRot());
        double cos = Math.cos(yawRad);
        double sin = Math.sin(yawRad);
        double worldX = localX * cos - localZ * sin;
        double worldZ = localX * sin + localZ * cos;
        return new Vec3(
                target.getX() + worldX,
                target.getY() + height * heightRatio,
                target.getZ() + worldZ);
    }
}
