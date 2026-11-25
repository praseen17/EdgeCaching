package org.fog.cache;

import java.util.List;
import java.util.Random;

public class EdgeCacheTests {
    static int passed = 0, failed = 0;

    static void check(String name, boolean ok) {
        if (ok) passed++; else failed++;
        System.out.println((ok ? "[PASS] " : "[FAIL] ") + name);
    }

    public static void main(String[] args) throws Exception {
        System.out.println("UNIT TESTS");
        System.out.println("------------------------------------------------------------");
        int cap = 5;

        CachePolicy lru = new LruPolicy(cap);
        for (int i = 0; i < cap; i++) lru.admit(i, i);
        check("T1 LRU: cached item is a hit", lru.lookup(0, 10));
        lru.admit(99, 11);
        check("T2 LRU: least recently used item is evicted", !lru.lookup(1, 12));
        check("T3 LRU: recently used item survives", lru.lookup(0, 13));
        check("T4 LRU: size never exceeds capacity", lru.size() <= cap);

        CachePolicy lfu = new LfuPolicy(cap);
        lfu.admit(7, 0);
        for (int i = 1; i <= 10; i++) lfu.lookup(7, i);
        for (int i = 100; i < 120; i++) lfu.admit(i, 20 + i);
        check("T5 LFU: frequently used item survives eviction", lfu.lookup(7, 500));
        check("T6 LFU: size never exceeds capacity", lfu.size() <= cap);

        MlPolicy ml = new MlPolicy(cap, 1000);
        Random r = new Random(1);
        boolean capOk = true;
        for (int t = 1; t <= 5000; t++) {
            int id = r.nextDouble() < 0.5 ? r.nextInt(5) : r.nextInt(1000);
            if (!ml.lookup(id, t)) ml.admit(id, t);
            if (ml.size() > cap) capOk = false;
        }
        check("T7 ML: size never exceeds capacity", capOk);
        check("T8 ML: score is a valid probability", ml.score(0, 5001) >= 0 && ml.score(0, 5001) <= 1);

        ZipfCatalog c1 = new ZipfCatalog(1000, 0.9, 1000), c2 = new ZipfCatalog(1000, 0.9, 1000);
        Random a = new Random(3), b = new Random(3);
        boolean same = true;
        for (int i = 0; i < 1000; i++) if (c1.sample(a, i * 10.0) != c2.sample(b, i * 10.0)) same = false;
        check("T9 Workload: same seed gives identical request stream", same);

        Random z = new Random(5);
        int top = 0;
        ZipfCatalog c3 = new ZipfCatalog(1000, 0.9, 1e9);
        for (int i = 0; i < 20000; i++) if (c3.sample(z, 0) < 10) top++;
        check("T10 Workload: skewed popularity (top 1% gets > 15% of requests)", top > 3000);

        System.out.println();
        System.out.println("INTEGRATION TEST (iFogSim2 run, 60s simulated)");
        System.out.println("------------------------------------------------------------");
        EdgeCacheMain.SIM_TIME_MS = 60000;
        List<CacheMetrics> res = EdgeCacheMain.run();
        CacheMetrics mLru = res.get(0), mLfu = res.get(1), mMl = res.get(2);
        check("T11 Simulation: all edge nodes served requests", mLru.requests > 0 && mLfu.requests > 0 && mMl.requests > 0);
        check("T12 Simulation: every node saw the same workload", Math.abs(mLru.requests - mMl.requests) < 50);
        check("T13 Simulation: hits never exceed requests", mLru.hits <= mLru.requests && mMl.hits <= mMl.requests);
        check("T14 Simulation: hits are faster than misses (avg latency falls as hit ratio rises)", mMl.hitRatio() > mLru.hitRatio() == mMl.avgLatency() < mLru.avgLatency());
        check("T15 Result: ML hit ratio beats LRU", mMl.hitRatio() > mLru.hitRatio());
        check("T16 Result: ML hit ratio beats LFU", mMl.hitRatio() > mLfu.hitRatio());
        check("T17 Result: ML reduces backhaul traffic vs LRU", mMl.backhaulBytes < mLru.backhaulBytes);

        System.out.println("------------------------------------------------------------");
        System.out.println(passed + " passed, " + failed + " failed");
        System.exit(failed == 0 ? 0 : 1);
    }
}
