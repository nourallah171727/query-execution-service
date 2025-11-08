package com.example.demo.cache;

public interface QueryResultCache {
    String get(long queryId);
    void put(long queryId, String json);
    boolean contains(long queryId);
}
