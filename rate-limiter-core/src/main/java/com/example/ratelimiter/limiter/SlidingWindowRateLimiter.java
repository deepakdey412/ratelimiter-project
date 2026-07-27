package com.example.ratelimiter.limiter;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Owns one SlidingWindowCounter per rate-limit key. Structurally identical
 * to TokenBucketRateLimiter (same ConcurrentHashMap + computeIfAbsent
 * pattern, same in-memory/single-JVM scope and limitations - see that
 * class's Javadoc for the full explanation, which applies here too).
 *
 * Deliberately a plain POJO with no @Component here: rate-limiter-core has
 * zero Spring dependency by design. It's the autoconfigure module's
 * RateLimiterAutoConfiguration that constructs this as a Spring @Bean - see
 * that class for why explicit @Bean registration is used instead of
 * component-scanning-based discovery.
 */
public class SlidingWindowRateLimiter implements RateLimiterStrategy {

    private final ConcurrentMap<String, SlidingWindowCounter> counters = new ConcurrentHashMap<>();

    @Override
    public RateLimitResult tryConsume(String key, long limit, Duration window) {
        SlidingWindowCounter counter =
                counters.computeIfAbsent(key, k -> new SlidingWindowCounter(limit, window.toNanos()));
        return counter.tryConsume();
    }
}
