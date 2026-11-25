package org.fog.cache;

import java.util.HashMap;
import java.util.Map;

public class LfuPolicy implements CachePolicy {
    private final int capacity;
    private final Map<Integer, long[]> map = new HashMap<>();

    public LfuPolicy(int capacity) { this.capacity = capacity; }

    public boolean lookup(int id, long t) {
        long[] e = map.get(id);
        if (e == null) return false;
        e[0]++;
        e[1] = t;
        return true;
    }

    public void admit(int id, long t) {
        if (map.containsKey(id)) return;
        if (map.size() >= capacity) {
            int victim = -1;
            long bf = Long.MAX_VALUE, bt = Long.MAX_VALUE;
            for (Map.Entry<Integer, long[]> x : map.entrySet()) {
                long[] a = x.getValue();
                if (a[0] < bf || (a[0] == bf && a[1] < bt)) { bf = a[0]; bt = a[1]; victim = x.getKey(); }
            }
            map.remove(victim);
        }
        map.put(id, new long[]{1, t});
    }

    public int size() { return map.size(); }
    public String name() { return "LFU"; }
}
