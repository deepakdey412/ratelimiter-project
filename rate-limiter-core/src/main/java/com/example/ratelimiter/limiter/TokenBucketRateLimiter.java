package com.example.ratelimiter.limiter;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Owns one TokenBucket per rate-limit key (typically "clientIp::method") and
 * routes each request to the right bucket.
 *
 * Why ConcurrentHashMap + computeIfAbsent():
 *   - ConcurrentHashMap gives thread-safe, non-blocking reads for keys that
 *     already exist (the common case once traffic warms up).
 *   - computeIfAbsent() makes "create the bucket if this is a brand-new key"
 *     atomic: if two threads race to be the first caller for the same new
 *     key, only one TokenBucket is actually constructed and stored; both
 *     threads get back the same instance. Without this, two racing threads
 *     could each create their own bucket, silently doubling a client's
 *     effective limit.
 *
 * SCOPE / KNOWN LIMITATION (still true as of Phase 2):
 *   This map lives in a single JVM's heap. If you scale this service to
 *   multiple instances behind a load balancer, each instance keeps its own
 *   independent counters, so the *effective* global limit for a client
 *   becomes (configured limit x number of instances). This is fine for a
 *   single instance or for coarse, best-effort protection, but it is NOT
 *   correct multi-instance rate limiting.
 *   A documented future extension (Phase 3+) is to swap this class's
 *   internals for a Redis-backed counter (e.g. Redis + Lua script for
 *   atomic check-and-decrement, or the redis-rate-limiter pattern) so all
 *   instances share one source of truth. Not implemented in this phase.
 *
 * Deliberately a plain POJO with no @Component here: rate-limiter-core has
 * zero Spring dependency by design. It's the autoconfigure module's
 * RateLimiterAutoConfiguration that constructs this as a Spring @Bean.
 */
public class TokenBucketRateLimiter implements RateLimiterStrategy {

    private final ConcurrentMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    /**
     * @param key    unique identifier for the caller+endpoint being limited
     * @param limit  bucket capacity (max requests per window)
     * @param window refill window duration
     */
    @Override
    public RateLimitResult tryConsume(String key, long limit, Duration window) {
        TokenBucket bucket = buckets.computeIfAbsent(key, k -> new TokenBucket(limit, window.toNanos()));
        return bucket.tryConsume();
    }
}
