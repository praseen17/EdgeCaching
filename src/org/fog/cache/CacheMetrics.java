package org.fog.cache;

import java.util.ArrayList;
import java.util.List;

public class CacheMetrics {
    public final String node;
    public long requests, hits, backhaulBytes;
    public double latencySum;
    private long winReq, winHit;
    public final List<Double> series = new ArrayList<>();
    private final long window;

    public CacheMetrics(String node, long window) { this.node = node; this.window = window; }

    public void record(boolean hit, double latencyMs, long bytes) {
        requests++;
        winReq++;
        if (hit) { hits++; winHit++; } else backhaulBytes += bytes;
        latencySum += latencyMs;
        if (winReq == window) { series.add(winHit / (double) winReq); winReq = 0; winHit = 0; }
    }

    public double hitRatio() { return requests == 0 ? 0 : hits / (double) requests; }
    public double avgLatency() { return requests == 0 ? 0 : latencySum / requests; }
}
