package com.example.ratelimiter.limiter;

import com.example.ratelimiter.annotation.RateLimitStrategy;

import java.util.EnumMap;
import java.util.Map;

/**
 * Tiny, explicit lookup from RateLimitStrategy enum value to its concrete
 * RateLimiterStrategy implementation.
 *
 * Deliberately hand-wired rather than using classpath scanning, reflection,
 * or Spring's "inject a Map<String, Bean> of every implementation" trick.
 * Both concrete strategies are ordinary constructor parameters here, so it's
 * obvious at a glance exactly which enum value maps to which class. Adding
 * a third algorithm later means: write the class, add one new enum
 * constant, add one new line to this constructor - nothing else in the
 * aspect or elsewhere needs to change.
 *
 * Deliberately a plain POJO with no @Component here (same reasoning as
 * TokenBucketRateLimiter / SlidingWindowRateLimiter): rate-limiter-core has
 * zero Spring dependency. RateLimiterAutoConfiguration constructs one of
 * these as a Spring @Bean, injecting whichever TokenBucketRateLimiter/
 * SlidingWindowRateLimiter beans end up in the application context.
 */
public class RateLimiterStrategyRegistry {

    private final Map<RateLimitStrategy, RateLimiterStrategy> strategiesByType = new EnumMap<>(RateLimitStrategy.class);

    public RateLimiterStrategyRegistry(TokenBucketRateLimiter tokenBucketRateLimiter,
                                        SlidingWindowRateLimiter slidingWindowRateLimiter) {
        strategiesByType.put(RateLimitStrategy.TOKEN_BUCKET, tokenBucketRateLimiter);
        strategiesByType.put(RateLimitStrategy.SLIDING_WINDOW, slidingWindowRateLimiter);
    }

    public RateLimiterStrategy resolve(RateLimitStrategy strategy) {
        RateLimiterStrategy implementation = strategiesByType.get(strategy);
        if (implementation == null) {
            // Defensive only - unreachable in practice since every enum
            // constant is wired above and the enum can't gain a new value
            // without a compile error here.
            throw new IllegalStateException("No RateLimiterStrategy registered for " + strategy);
        }
        return implementation;
    }
}
