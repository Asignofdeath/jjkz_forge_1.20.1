package com.darkness.jujutsukaisenzone.blackhole;

/**
 * 黑洞_A 的物理常量与配色工具（纯视觉演示所需的最小子集）。
 * 公式来源：Schwarzschild (1916)、Darwin (1959)、Shakura & Sunyaev (1973)。
 */
public final class BlackHolePhysics
{
    /** 临界撞影参数 b_crit = (3√3/2) Rs ≈ 2.598 Rs（阴影表观半径） */
    public static final double SHADOW_FACTOR = 3.0 * Math.sqrt(3.0) / 2.0;

    private BlackHolePhysics()
    {
    }

    /** 光子球半径 = 1.5 Rs */
    public static double photonSphereRadius(double schwarzschildRadius)
    {
        return 1.5 * schwarzschildRadius;
    }

    /** 阴影（表观事件视界）半径 = b_crit * Rs */
    public static double shadowRadius(double schwarzschildRadius)
    {
        return SHADOW_FACTOR * schwarzschildRadius;
    }

    /** 最内稳定圆轨道 ISCO = 3 Rs（吸积盘内缘） */
    public static double iscoRadius(double schwarzschildRadius)
    {
        return 3.0 * schwarzschildRadius;
    }

    /** 引力红shift：z = 1/sqrt(1 - Rs/r) - 1；r <= Rs 时返回正无穷 */
    public static double gravitationalRedshift(double schwarzschildRadius, double radius)
    {
        if (radius <= schwarzschildRadius)
        {
            return Double.MAX_VALUE;
        }
        return 1.0 / Math.sqrt(1.0 - schwarzschildRadius / radius) - 1.0;
    }

    /**
     * 普朗克黑体辐射色温 → RGB（Tanner Helland 近似公式）。
     * 返回 [r, g, b]，各分量 0~1。
     */
    public static float[] blackBodyColor(double kelvin)
    {
        double temp = Math.max(1000.0, Math.min(40000.0, kelvin)) / 100.0;
        float r;
        float g;
        float b;
        double x;

        if (temp <= 66.0)
        {
            r = 1.0F;
        }
        else
        {
            x = temp - 60.0;
            r = (float) Math.max(0.0, Math.min(1.0, 329.698727446 * Math.pow(x, -0.1332047592) / 255.0));
        }

        if (temp <= 66.0)
        {
            x = temp;
            g = (float) Math.max(0.0, Math.min(1.0, (99.4708025861 * Math.log(x) - 161.1195681661) / 255.0));
        }
        else
        {
            x = temp - 60.0;
            g = (float) Math.max(0.0, Math.min(1.0, 288.1221695283 * Math.pow(x, -0.0755148492) / 255.0));
        }

        if (temp >= 66.0)
        {
            b = 1.0F;
        }
        else if (temp <= 19.0)
        {
            b = 0.0F;
        }
        else
        {
            x = temp - 10.0;
            b = (float) Math.max(0.0, Math.min(1.0, (138.5177312231 * Math.log(x) - 305.0447927307) / 255.0));
        }

        return new float[] {r, g, b};
    }
}
