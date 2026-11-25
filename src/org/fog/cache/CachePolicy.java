package org.fog.cache;

public interface CachePolicy {
    boolean lookup(int id, long t);
    void admit(int id, long t);
    int size();
    String name();
}
