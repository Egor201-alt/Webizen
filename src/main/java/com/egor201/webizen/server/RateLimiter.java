package com.egor201.webizen.server;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class RateLimiter {

    private final int maxRequests;
    private final long windowMs;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public RateLimiter(int maxRequests, long windowMs) {
        this.maxRequests = maxRequests;
        this.windowMs    = windowMs;
    }

    public boolean allow(String ip) {
        long now = System.currentTimeMillis();
        Window window = windows.compute(ip, (k, w) -> {
            if (w == null || now - w.startMs.get() >= windowMs) {
                Window fresh = new Window();
                fresh.startMs.set(now);
                fresh.count.set(0);
                return fresh;
            }
            return w;
        });
        return window.count.incrementAndGet() <= maxRequests;
    }

    private static class Window {
        final AtomicLong startMs = new AtomicLong(0);
        final AtomicInteger count = new AtomicInteger(0);
    }
}
