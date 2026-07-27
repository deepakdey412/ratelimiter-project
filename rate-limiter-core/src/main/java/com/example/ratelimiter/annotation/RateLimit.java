package com.example.ratelimiter.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a Spring MVC controller method as rate-limited, tracked per client
 * IP address, using whichever algorithm {@link #strategy()} selects.
 *
 * Example (token bucket, the default):
 *   {@code @RateLimit(limit = 5, window = "1m")}
 * allows 5 requests per minute per IP, where tokens refill continuously
 * (i.e. this is NOT "5 requests, then a hard reset every clock-aligned
 * minute" - see TokenBucket for the refill math).
 *
 * Example (sliding window, opted in explicitly):
 *   {@code @RateLimit(limit = 10, window = "30s", strategy = RateLimitStrategy.SLIDING_WINDOW)}
 * strictly allows at most 10 requests in any trailing 30s window, with no
 * burst allowance beyond the limit - see SlidingWindowCounter.
 *
 * Scope: only method-level use on @RestController methods is supported.
 * Class-level annotation is intentionally not supported yet.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /** Bucket capacity: the max number of requests allowed within {@code window}. */
    long limit();

    /**
     * Refill window, expressed as a number + unit suffix.
     * Supported units: {@code s} (seconds), {@code m} (minutes), {@code h} (hours).
     * Examples: "30s", "1m", "1h".
     */
    String window();

    /**
     * Which algorithm to enforce {@code limit} over {@code window} with.
     * Defaults to TOKEN_BUCKET so any existing @RateLimit usage that doesn't
     * specify this keeps its original Phase 1 behavior unchanged.
     */
    RateLimitStrategy strategy() default RateLimitStrategy.TOKEN_BUCKET;

    /**
     * Which key resolution strategy to use for identifying the caller.
     * Defaults to IP so any existing @RateLimit usage that doesn't specify
     * this keeps its original Phases 1-3 behavior unchanged.
     */
    RateLimitScope scope() default RateLimitScope.IP;

    /**
     * Bean name of a custom RateLimitKeyResolver, only used when
     * {@code scope = RateLimitScope.CUSTOM}. Ignored for other scopes.
     */
    String keyResolverBeanName() default "";
}
