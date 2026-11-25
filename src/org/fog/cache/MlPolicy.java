package org.fog.cache;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class MlPolicy implements CachePolicy {
    private static final int HORIZON = 300;
    private static final double LEARNING_RATE = 0.05;
    private final int capacity;
    private final double[] w = new double[4];
    private final int[] count;
    private final long[] last;
    private final double[] gap;
    private final double[][] snap;
    private final long[] snapT;
    private final List<Integer> seen = new ArrayList<>();
    private final ArrayDeque<Integer> queue = new ArrayDeque<>();
    private final Set<Integer> cache = new HashSet<>();
    private final Random rnd = new Random(7);

    public MlPolicy(int capacity, int catalogSize) {
        this.capacity = capacity;
        count = new int[catalogSize];
        last = new long[catalogSize];
        gap = new double[catalogSize];
        snap = new double[catalogSize][];
        snapT = new long[catalogSize];
    }

    private double[] features(int id, long t) {
        return new double[]{1, Math.log1p(count[id]) / 5, Math.log1p(t - last[id]) / 8, Math.log1p(gap[id]) / 8};
    }

    private double probability(double[] x) {
        double z = 0;
        for (int i = 0; i < 4; i++) z += w[i] * x[i];
        return 1 / (1 + Math.exp(-z));
    }

    private void train(double[] x, double label) {
        double err = label - probability(x);
        for (int i = 0; i < 4; i++) w[i] += LEARNING_RATE * err * x[i];
    }

    public double score(int id, long t) { return probability(features(id, t)); }

    public boolean lookup(int id, long t) {
        boolean hit = cache.contains(id);
        if (count[id] == 0) { seen.add(id); gap[id] = 1000; }
        else gap[id] = 0.5 * gap[id] + 0.5 * (t - last[id]);
        count[id]++;
        last[id] = t;
        if (snap[id] != null && t - snapT[id] <= HORIZON) { train(snap[id], 1); snap[id] = null; }
        while (!queue.isEmpty() && snapT[queue.peek()] + HORIZON < t) {
            int k = queue.poll();
            if (snap[k] != null) { train(snap[k], 0); snap[k] = null; }
        }
        int c = seen.get(rnd.nextInt(seen.size()));
        if (snap[c] == null) { snap[c] = features(c, t); snapT[c] = t; queue.add(c); }
        return hit;
    }

    public void admit(int id, long t) {
        if (cache.contains(id)) return;
        if (cache.size() < capacity) { cache.add(id); return; }
        int victim = -1;
        double best = 2;
        for (int x : cache) {
            double s = score(x, t);
            if (s < best) { best = s; victim = x; }
        }
        if (score(id, t) > best) { cache.remove(victim); cache.add(id); }
    }

    public int size() { return cache.size(); }
    public String name() { return "ML-Predict"; }
}
