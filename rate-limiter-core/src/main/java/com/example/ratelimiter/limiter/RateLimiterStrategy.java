package com.example.ratelimiter.limiter;

import java.time.Duration;

/**
 * A pluggable rate-limiting algorithm. Each implementation owns its own
 * internal per-key state and decides whether a request for that key should
 * be allowed right now.
 *
 * Implementations: TokenBucketRateLimiter (Phase 1) and
 * SlidingWindowRateLimiter (Phase 2). RateLimitAspect depends only on this
 * interface, never on a concrete implementation, so adding a third
 * algorithm later never requires touching the aspect.
 */
public interface RateLimiterStrategy {

    /**
     * @param key    unique identifier for the caller+endpoint being limited
     * @param limit  max requests allowed within {@code window}
     * @param window the rolling/refill window duration
     */
    RateLimitResult tryConsume(String key, long limit, Duration window);
}
