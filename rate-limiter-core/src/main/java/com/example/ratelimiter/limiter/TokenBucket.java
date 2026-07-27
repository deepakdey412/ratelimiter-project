package com.example.ratelimiter.limiter;

/**
 * A single token bucket for ONE rate-limit key (e.g. one client IP calling
 * one specific endpoint).
 *
 * Token bucket, in plain terms:
 *   - The bucket holds up to `capacity` tokens and starts full.
 *   - Every incoming request costs exactly 1 token.
 *   - Tokens regenerate continuously over time at a fixed rate of
 *     (capacity tokens / windowNanos), rather than all appearing at once
 *     when a window "resets". This is what makes it a token bucket rather
 *     than a naive fixed-window counter.
 *   - Refill is computed lazily, only when a request actually arrives
 *     (by looking at elapsed wall-clock time since the last check). That
 *     means no background thread/scheduler is needed to "reset" anything.
 *
 * Thread-safety: every read and write of mutable state happens inside the
 * synchronized tryConsume() method, so concurrent requests hitting the same
 * key are safely serialized on this bucket's monitor. Different keys use
 * different TokenBucket instances (see TokenBucketRateLimiter), so traffic
 * for one client/endpoint never blocks another.
 */
public class TokenBucket {

    private final long capacity;               // max tokens the bucket can hold (== the configured limit)
    private final double refillTokensPerNano;   // tokens regenerated per nanosecond

    private double availableTokens;             // current token count (kept as a double for precise partial refills)
    private long lastRefillTimestampNanos;

    public TokenBucket(long capacity, long windowNanos) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be > 0");
        }
        if (windowNanos <= 0) {
            throw new IllegalArgumentException("windowNanos must be > 0");
        }
        this.capacity = capacity;
        this.refillTokensPerNano = (double) capacity / (double) windowNanos;
        this.availableTokens = capacity; // start full: allows an initial burst up to `capacity`
        this.lastRefillTimestampNanos = System.nanoTime();
    }

    /**
     * Attempts to consume a single token for the current request.
     *
     * @return RateLimitResult.allowed() if a token was available and has been
     *         consumed, or RateLimitResult.denied(retryAfterSeconds) if the
     *         bucket is empty.
     */
    public synchronized RateLimitResult tryConsume() {
        refill();

        if (availableTokens >= 1.0) {
            availableTokens -= 1.0;
            return RateLimitResult.allowed();
        }

        // Bucket is empty: estimate how long until at least 1 token exists,
        // so callers can send back a helpful Retry-After value.
        double tokensNeeded = 1.0 - availableTokens;
        double secondsUntilNextToken = (tokensNeeded / refillTokensPerNano) / 1_000_000_000.0;
        long retryAfterSeconds = Math.max(1L, (long) Math.ceil(secondsUntilNextToken));
        return RateLimitResult.denied(retryAfterSeconds);
    }

    /** Adds tokens earned since the last refill, capped at `capacity`. */
    private void refill() {
        long now = System.nanoTime();
        long elapsedNanos = now - lastRefillTimestampNanos;
        if (elapsedNanos <= 0) {
            return;
        }
        double tokensEarned = elapsedNanos * refillTokensPerNano;
        if (tokensEarned > 0) {
            availableTokens = Math.min(capacity, availableTokens + tokensEarned);
            lastRefillTimestampNanos = now;
        }
    }
}
