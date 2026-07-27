package com.example.ratelimiter.limiter;

/**
 * Immutable outcome of a single rate-limit check.
 *
 * This used to be a nested class inside TokenBucket (TokenBucket.Result).
 * Now that a second algorithm (SlidingWindowCounter) needs to return the
 * same kind of outcome, it's been pulled out to a shared top-level type so
 * that RateLimiterStrategy has one common return type regardless of which
 * algorithm produced it. No behavior changed - only where the type lives.
 */
public final class RateLimitResult {

    private final boolean allowed;
    private final long retryAfterSeconds;

    private RateLimitResult(boolean allowed, long retryAfterSeconds) {
        this.allowed = allowed;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public static RateLimitResult allowed() {
        return new RateLimitResult(true, 0L);
    }

    public static RateLimitResult denied(long retryAfterSeconds) {
        return new RateLimitResult(false, retryAfterSeconds);
    }

    public boolean isAllowed() {
        return allowed;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
