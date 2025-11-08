package com.example.demo.cache;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryQueryResultCache implements QueryResultCache {
    private final ConcurrentHashMap<Long, String> map = new ConcurrentHashMap<>();

    @Override public String get(long queryId) { return map.get(queryId); }
    @Override public void put(long queryId, String json) { map.put(queryId, json); }
    @Override public boolean contains(long queryId) { return map.containsKey(queryId); }
}