package com.bikestore.api.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * In-memory sliding-window rate limiter for incoming IPN requests, keyed by client IP address.
 *
 * <p>Allows up to {@code webhook.ipn.rate-limit.max-requests} requests per IP within a rolling
 * {@code webhook.ipn.rate-limit.window-seconds} window. A background task runs every 5 minutes to
 * evict entries for IPs that have no recent activity, preventing unbounded memory growth.
 */
@Component
@Slf4j
public class WebhookRateLimiter {

    private final int maxRequests;
    private final long windowMillis;

    private final ConcurrentHashMap<String, ConcurrentLinkedDeque<Long>> requestLog =
            new ConcurrentHashMap<>();

    public WebhookRateLimiter(
            @Value("${webhook.ipn.rate-limit.max-requests:30}") int maxRequests,
            @Value("${webhook.ipn.rate-limit.window-seconds:60}") long windowSeconds) {
        this.maxRequests = maxRequests;
        this.windowMillis = windowSeconds * 1000L;
    }

    /**
     * Returns {@code true} if the request from {@code ip} is within the allowed rate, and records
     * the hit. Returns {@code false} if the limit has been exceeded.
     */
    public boolean isAllowed(String ip) {
        long now = System.currentTimeMillis();
        long windowStart = now - windowMillis;

        ConcurrentLinkedDeque<Long> timestamps =
                requestLog.computeIfAbsent(ip, k -> new ConcurrentLinkedDeque<>());

        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
                timestamps.pollFirst();
            }
            if (timestamps.size() < maxRequests) {
                timestamps.addLast(now);
                return true;
            }
        }
        return false;
    }

    /** Removes entries for IPs with no activity within the current window to free memory. */
    @Scheduled(fixedRate = 300_000)
    public void evictStaleEntries() {
        long windowStart = System.currentTimeMillis() - windowMillis;
        requestLog.entrySet().removeIf(entry -> {
            ConcurrentLinkedDeque<Long> q = entry.getValue();
            synchronized (q) {
                while (!q.isEmpty() && q.peekFirst() < windowStart) {
                    q.pollFirst();
                }
                return q.isEmpty();
            }
        });
        log.debug("Rate limiter cache eviction complete. Active IP entries: {}", requestLog.size());
    }
}
