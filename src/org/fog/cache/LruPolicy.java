package org.fog.cache;

import java.util.Iterator;
import java.util.LinkedHashMap;

public class LruPolicy implements CachePolicy {
    private final int capacity;
    private final LinkedHashMap<Integer, Boolean> map = new LinkedHashMap<>(16, 0.75f, true);

    public LruPolicy(int capacity) { this.capacity = capacity; }

    public boolean lookup(int id, long t) {
        if (!map.containsKey(id)) return false;
        map.get(id);
        return true;
    }

    public void admit(int id, long t) {
        if (map.containsKey(id)) return;
        if (map.size() >= capacity) {
            Iterator<Integer> it = map.keySet().iterator();
            it.next();
            it.remove();
        }
        map.put(id, true);
    }

    public int size() { return map.size(); }
    public String name() { return "LRU"; }
}
