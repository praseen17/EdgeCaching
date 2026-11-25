package org.fog.cache;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ZipfCatalog {
    private final int size;
    private final double[] cdf;
    private final double epochMs;
    private final List<int[]> perms = new ArrayList<>();

    public ZipfCatalog(int size, double alpha, double epochMs) {
        this.size = size;
        this.epochMs = epochMs;
        cdf = new double[size];
        double s = 0;
        for (int i = 0; i < size; i++) { s += 1 / Math.pow(i + 1, alpha); cdf[i] = s; }
        for (int i = 0; i < size; i++) cdf[i] /= s;
        int[] p = new int[size];
        for (int i = 0; i < size; i++) p[i] = i;
        perms.add(p);
    }

    public int size() { return size; }

    private int[] permutation(int epoch) {
        while (perms.size() <= epoch) {
            int e = perms.size();
            int[] p = perms.get(e - 1).clone();
            Random r = new Random(1000 + e);
            for (int k = 0; k < 30; k++) {
                int a = r.nextInt(Math.min(100, size)), b = r.nextInt(size);
                int tmp = p[a]; p[a] = p[b]; p[b] = tmp;
            }
            perms.add(p);
        }
        return perms.get(epoch);
    }

    public int sample(Random r, double nowMs) {
        int[] p = permutation((int) (nowMs / epochMs));
        double u = r.nextDouble();
        int lo = 0, hi = size - 1;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (cdf[mid] < u) lo = mid + 1; else hi = mid;
        }
        return p[lo];
    }
}
