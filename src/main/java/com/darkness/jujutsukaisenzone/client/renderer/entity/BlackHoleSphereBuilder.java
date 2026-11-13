package com.darkness.jujutsukaisenzone.client.renderer.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 黑洞_A —— 正二十面体细分球网格生成器（事件视界用）。
 */
public final class BlackHoleSphereBuilder
{
    private static float[] cachedVertices;
    private static int[] cachedIndices;
    private static int cachedSubdivisions = -1;

    private BlackHoleSphereBuilder()
    {
    }

    public static float[] getVertices(int subdivisions)
    {
        if (cachedSubdivisions != subdivisions)
        {
            buildIcosphere(subdivisions);
        }
        return cachedVertices;
    }

    public static int[] getIndices(int subdivisions)
    {
        if (cachedSubdivisions != subdivisions)
        {
            buildIcosphere(subdivisions);
        }
        return cachedIndices;
    }

    private static void buildIcosphere(int subdivisions)
    {
        float t = (float) ((1.0 + Math.sqrt(5.0)) / 2.0);
        List<float[]> vertices = new ArrayList<>();
        vertices.add(normalize(-1.0F, t, 0.0F));
        vertices.add(normalize(1.0F, t, 0.0F));
        vertices.add(normalize(-1.0F, -t, 0.0F));
        vertices.add(normalize(1.0F, -t, 0.0F));
        vertices.add(normalize(0.0F, -1.0F, t));
        vertices.add(normalize(0.0F, 1.0F, t));
        vertices.add(normalize(0.0F, -1.0F, -t));
        vertices.add(normalize(0.0F, 1.0F, -t));
        vertices.add(normalize(t, 0.0F, -1.0F));
        vertices.add(normalize(t, 0.0F, 1.0F));
        vertices.add(normalize(-t, 0.0F, -1.0F));
        vertices.add(normalize(-t, 0.0F, 1.0F));

        List<int[]> faces = new ArrayList<>();
        faces.add(new int[] {0, 11, 5});
        faces.add(new int[] {0, 5, 1});
        faces.add(new int[] {0, 1, 7});
        faces.add(new int[] {0, 7, 10});
        faces.add(new int[] {0, 10, 11});
        faces.add(new int[] {1, 5, 9});
        faces.add(new int[] {5, 11, 4});
        faces.add(new int[] {11, 10, 2});
        faces.add(new int[] {10, 7, 6});
        faces.add(new int[] {7, 1, 8});
        faces.add(new int[] {3, 9, 4});
        faces.add(new int[] {3, 4, 2});
        faces.add(new int[] {3, 2, 6});
        faces.add(new int[] {3, 6, 8});
        faces.add(new int[] {3, 8, 9});
        faces.add(new int[] {4, 9, 5});
        faces.add(new int[] {2, 4, 11});
        faces.add(new int[] {6, 2, 10});
        faces.add(new int[] {8, 6, 7});
        faces.add(new int[] {9, 8, 1});

        Map<Long, Integer> midpointCache = new HashMap<>();
        for (int i = 0; i < subdivisions; i++)
        {
            List<int[]> newFaces = new ArrayList<>();
            midpointCache.clear();
            for (int[] tri : faces)
            {
                int a = getMidpoint(tri[0], tri[1], vertices, midpointCache);
                int b = getMidpoint(tri[1], tri[2], vertices, midpointCache);
                int c = getMidpoint(tri[2], tri[0], vertices, midpointCache);
                newFaces.add(new int[] {tri[0], a, c});
                newFaces.add(new int[] {tri[1], b, a});
                newFaces.add(new int[] {tri[2], c, b});
                newFaces.add(new int[] {a, b, c});
            }
            faces = newFaces;
        }

        cachedVertices = new float[vertices.size() * 3];
        for (int i = 0; i < vertices.size(); i++)
        {
            float[] v = vertices.get(i);
            cachedVertices[i * 3] = v[0];
            cachedVertices[i * 3 + 1] = v[1];
            cachedVertices[i * 3 + 2] = v[2];
        }
        cachedIndices = new int[faces.size() * 3];
        for (int i = 0; i < faces.size(); i++)
        {
            int[] f = faces.get(i);
            cachedIndices[i * 3] = f[0];
            cachedIndices[i * 3 + 1] = f[1];
            cachedIndices[i * 3 + 2] = f[2];
        }
        cachedSubdivisions = subdivisions;
    }

    private static int getMidpoint(int i1, int i2, List<float[]> vertices, Map<Long, Integer> cache)
    {
        long key = (long) Math.min(i1, i2) << 32 | (long) Math.max(i1, i2);
        Integer cached = cache.get(key);
        if (cached != null)
        {
            return cached;
        }
        float[] v1 = vertices.get(i1);
        float[] v2 = vertices.get(i2);
        float[] mid = normalize((v1[0] + v2[0]) * 0.5F, (v1[1] + v2[1]) * 0.5F, (v1[2] + v2[2]) * 0.5F);
        int index = vertices.size();
        vertices.add(mid);
        cache.put(key, index);
        return index;
    }

    private static float[] normalize(float x, float y, float z)
    {
        float len = (float) Math.sqrt(x * x + y * y + z * z);
        return new float[] {x / len, y / len, z / len};
    }
}
