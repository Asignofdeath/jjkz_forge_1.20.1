package com.darkness.jujutsukaisenzone.entity.util;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 特效技能可复用的「作用区域」几何描述。
 * <p>
 * 统一「球体 / 胶囊（球沿轴扫过的柱）」两种形状的方块相交判定，供 {@link BlockErase} 遍历，
 * 消除各技能类里重复的球体 / 胶囊几何代码。方块判定一律委托 {@link GeometryUtils}，
 * 保证与既有技能手感逐字一致。
 */
public interface AreaShape
{
    /** 形状的包围盒（供方块遍历范围与实体候选查询使用）。 */
    AABB bounds();

    /** 方块单位立方体 {@code [bx,bx+1]×[by,by+1]×[bz,bz+1]} 是否与形状相交。 */
    boolean intersectsBlock(int bx, int by, int bz);

    /** 球体：(cx,cy,cz) 为球心，radius 为半径。 */
    static AreaShape sphere(Vec3 center, double radius)
    {
        return sphere(center.x, center.y, center.z, radius);
    }

    static AreaShape sphere(double cx, double cy, double cz, double radius)
    {
        return new Sphere(cx, cy, cz, radius);
    }

    /** 胶囊：轴 a→b、半径 radius（球沿轴连续扫过的区域）。 */
    static AreaShape capsule(Vec3 a, Vec3 b, double radius)
    {
        return new Capsule(a, b, radius);
    }

    /** 球体实现。 */
    final class Sphere implements AreaShape
    {
        private final double cx;
        private final double cy;
        private final double cz;
        private final double rSqr;

        Sphere(double cx, double cy, double cz, double radius)
        {
            this.cx = cx;
            this.cy = cy;
            this.cz = cz;
            this.rSqr = radius * radius;
        }

        @Override
        public AABB bounds()
        {
            double r = Math.sqrt(this.rSqr);
            return new AABB(this.cx - r, this.cy - r, this.cz - r, this.cx + r, this.cy + r, this.cz + r);
        }

        @Override
        public boolean intersectsBlock(int bx, int by, int bz)
        {
            return GeometryUtils.blockCubeIntersectsSphere(this.cx, this.cy, this.cz, this.rSqr, bx, by, bz);
        }
    }

    /**
     * 胶囊实现。
     * <p>
     * 方块判定：取方块中心在轴上的最近点（夹到线段内）作为球心，再做球-方块判定
     * ——即「球沿轴连续扫过」的胶囊语义，与解箭破方块的既有判据完全一致。
     */
    final class Capsule implements AreaShape
    {
        private final Vec3 a;
        private final Vec3 ab;
        private final double abLenSqr;
        private final double rSqr;

        Capsule(Vec3 a, Vec3 b, double radius)
        {
            this.a = a;
            this.ab = b.subtract(a);
            this.abLenSqr = Math.max(this.ab.lengthSqr(), 1.0E-8D);
            this.rSqr = radius * radius;
        }

        @Override
        public AABB bounds()
        {
            return new AABB(this.a, this.a.add(this.ab)).inflate(Math.sqrt(this.rSqr));
        }

        @Override
        public boolean intersectsBlock(int bx, int by, int bz)
        {
            double cx = bx + 0.5D;
            double cy = by + 0.5D;
            double cz = bz + 0.5D;
            double t = ((cx - this.a.x) * this.ab.x + (cy - this.a.y) * this.ab.y + (cz - this.a.z) * this.ab.z)
                    / this.abLenSqr;
            t = Mth.clamp(t, 0.0D, 1.0D);
            return GeometryUtils.blockCubeIntersectsSphere(
                    this.a.x + this.ab.x * t,
                    this.a.y + this.ab.y * t,
                    this.a.z + this.ab.z * t,
                    this.rSqr, bx, by, bz);
        }
    }
}
