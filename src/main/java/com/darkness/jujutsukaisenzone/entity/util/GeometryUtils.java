package com.darkness.jujutsukaisenzone.entity.util;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

/**
 * 特效实体通用几何工具：球体-方块、球体-AABB 相交检测。
 * <p>
 * 供 {@link com.darkness.jujutsukaisenzone.entity.SpecialEffectEntity}
 * 和 {@link com.darkness.jujutsukaisenzone.entity.SpecialEffectArrowEntity} 共用。
 */
public final class GeometryUtils
{
    private GeometryUtils() {}

    /**
     * 判断以 (cx, cy, cz) 为球心、rSqr 为半径平方的球体是否与方块 (bx, by, bz) 的包围盒相交。
     */
    public static boolean blockCubeIntersectsSphere(double cx, double cy, double cz, double rSqr, int bx, int by, int bz)
    {
        double closestX = Mth.clamp(cx, bx, bx + 1.0D);
        double closestY = Mth.clamp(cy, by, by + 1.0D);
        double closestZ = Mth.clamp(cz, bz, bz + 1.0D);
        double dx = cx - closestX;
        double dy = cy - closestY;
        double dz = cz - closestZ;
        return dx * dx + dy * dy + dz * dz <= rSqr;
    }

    /**
     * 判断以 (cx, cy, cz) 为球心、rSqr 为半径平方的球体是否与 AABB 相交。
     */
    public static boolean aabbIntersectsSphere(double cx, double cy, double cz, double rSqr, AABB box)
    {
        double closestX = Mth.clamp(cx, box.minX, box.maxX);
        double closestY = Mth.clamp(cy, box.minY, box.maxY);
        double closestZ = Mth.clamp(cz, box.minZ, box.maxZ);
        double dx = cx - closestX;
        double dy = cy - closestY;
        double dz = cz - closestZ;
        return dx * dx + dy * dy + dz * dz <= rSqr;
    }
}
